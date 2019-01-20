package net.highwayfrogs.editor.file.map.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.data.MatrixEntity;
import net.highwayfrogs.editor.file.map.entity.data.PathEntity;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.map.form.FormBook;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the "ENTITY" struct.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
public class Entity extends GameObject {
    private int formGridId;
    private int uniqueId;
    private FormBook formBook;
    private int flags;
    private GameObject entityData;
    private EntityScriptData scriptData;

    private transient int loadScriptDataPointer;
    private transient int loadReadLength;
    private transient MAPFile map;

    private static final int RUNTIME_POINTERS = 4;

    public static final int FLAG_HIDDEN = Constants.BIT_FLAG_0; // Don't create a live entity while this is set.
    public static final int FLAG_NO_DISPLAY = Constants.BIT_FLAG_1; // Don't display any mesh.
    public static final int FLAG_NO_MOVEMENT = Constants.BIT_FLAG_2; // Don't allow entity movement.
    public static final int FLAG_NO_COLLISION = Constants.BIT_FLAG_3; // Collision does not apply to this entity.
    public static final int FLAG_ALIGN_TO_WORLD = Constants.BIT_FLAG_4; // Entity matrix always aligned to world axes.
    public static final int FLAG_PROJECT_ON_LAND = Constants.BIT_FLAG_5; // Entity position is projected onto the landscape.
    public static final int FLAG_LOCAL_ALIGN = Constants.BIT_FLAG_6; // Entity matrix is calculated locally (Using Y part of entity matrix.)

    public Entity(MAPFile parentMap) {
        this.map = parentMap;
    }

    @Override
    public void load(DataReader reader) {
        this.formGridId = reader.readUnsignedShortAsInt();
        this.uniqueId = reader.readUnsignedShortAsInt();
        this.formBook = FormBook.getFormBook(map.getTheme(), reader.readUnsignedShortAsInt());
        this.flags = reader.readUnsignedShortAsInt();
        reader.readBytes(RUNTIME_POINTERS * Constants.POINTER_SIZE);

        this.loadScriptDataPointer = reader.getIndex();
        if (formBook.getEntity().getScriptDataMaker() != null) {
            this.entityData = formBook.getEntity().getScriptDataMaker().get();
            this.entityData.load(reader);
        }

        if (formBook.getScriptDataMaker() != null) {
            this.scriptData = formBook.getScriptDataMaker().get();
            this.scriptData.load(reader);
        }

        this.loadReadLength = reader.getIndex() - this.loadScriptDataPointer;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.formGridId);
        writer.writeUnsignedShort(this.uniqueId);
        writer.writeUnsignedShort(getFormBook().getRawId());
        writer.writeUnsignedShort(this.flags);
        writer.writeNull(RUNTIME_POINTERS * Constants.POINTER_SIZE);
        if (this.entityData != null)
            this.entityData.save(writer);
        if (this.scriptData != null)
            this.scriptData.save(writer);
    }

    /**
     * Test if this entity has a particular flag.
     * @param flag The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(int flag) {
        return (this.flags & flag) == flag;
    }

    /**
     * Get any PathInfo owned by this entity, if it has any.
     * @return pathInfo
     */
    public PathInfo getPathInfo() {
        if (getEntityData() instanceof PathEntity)
            return ((PathEntity) getEntityData()).getPathInfo();

        if (getEntityData() instanceof PathInfo)
            return (PathInfo) getEntityData();

        return null;
    }

    /**
     * Get any PSXMatrix owned by this entity, if it has any.
     * @return psxMatrix
     */
    public PSXMatrix getMatrixInfo() {
        if (getEntityData() instanceof MatrixEntity)
            return ((MatrixEntity) getEntityData()).getMatrix();

        if (getEntityData() instanceof PSXMatrix)
            return (PSXMatrix) getEntityData();

        return null;
    }
}
