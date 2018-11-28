package net.highwayfrogs.editor.file.map.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.map.form.FormBook;
import net.highwayfrogs.editor.file.reader.DataReader;
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

    public Entity(MAPFile parentMap) {
        this.map = parentMap;
    }

    private static final int RUNTIME_POINTERS = 4;

    private static final int FLAG_HIDDEN = 1; // Don't create a live entity while this is set.
    private static final int FLAG_NO_DISPLAY = 2; // Don't display any mesh.
    private static final int FLAG_NO_MOVEMENT = 4; // Don't allow entity movement.
    private static final int FLAG_NO_COLLISION = 8; // Collision does not apply to this entity.
    private static final int FLAG_ALIGN_TO_WORLD = 16; // Entity matrix always aligned to world axes.
    private static final int FLAG_PROJECT_ON_LAND = 32; // Entity position is projected onto the landscape.
    private static final int FLAG_LOCAL_ALIGN = 64; // Entity matrix is calculated locally (Using Y part of entity matrix.)

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
}
