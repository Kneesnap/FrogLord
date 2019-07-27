package net.highwayfrogs.editor.file.map.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathResult;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.IVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;

/**
 * Represents the "ENTITY" struct.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@Setter
public class Entity extends GameObject {
    private int formGridId;
    private int uniqueId;
    private FormEntry formEntry;
    private int flags;
    private EntityData entityData;
    private EntityScriptData scriptData;

    private transient int loadScriptDataPointer;
    private transient int loadReadLength;
    private transient MAPFile map;

    private static final int RUNTIME_POINTERS = 4;
    private static final IVector GAME_Y_AXIS_POS = new IVector(0, 0x1000, 0);

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

    public Entity(MAPFile file, FormEntry formEntry) {
        this(file);
        setFormBook(formEntry);
    }

    @Override
    public void load(DataReader reader) {
        this.formGridId = reader.readUnsignedShortAsInt();
        this.uniqueId = reader.readUnsignedShortAsInt();
        this.formEntry = getConfig().getMapFormEntry(map.getTheme(), reader.readUnsignedShortAsInt());
        this.flags = reader.readUnsignedShortAsInt();
        reader.skipBytes(RUNTIME_POINTERS * Constants.POINTER_SIZE);

        this.loadScriptDataPointer = reader.getIndex();

        this.entityData = EntityData.makeData(getConfig(), getFormEntry());
        if (this.entityData != null)
            this.entityData.load(reader);

        this.scriptData = EntityScriptData.makeData(getConfig(), getFormEntry());
        if (this.scriptData != null)
            scriptData.load(reader);

        this.loadReadLength = reader.getIndex() - this.loadScriptDataPointer;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.formGridId);
        writer.writeUnsignedShort(this.uniqueId);
        writer.writeUnsignedShort(this.formEntry.getMapFormId());
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
        return getEntityData() instanceof PathData ? ((PathData) getEntityData()).getPathInfo() : null;
    }

    /**
     * Get any PSXMatrix owned by this entity, if it has any.
     * @return psxMatrix
     */
    public PSXMatrix getMatrixInfo() {
        return getEntityData() instanceof MatrixData ? ((MatrixData) getEntityData()).getMatrix() : null;
    }

    /**
     * Get the x, y, z position of this entity.
     * @param map The map file this entity presides in.
     * @return position
     */
    public float[] getPosition(float[] position, MAPFile map) {
        PSXMatrix matrix = getMatrixInfo();
        if (matrix != null) {
            int[] pos = matrix.getTransform();
            position[0] = Utils.fixedPointIntToFloat20Bit(pos[0]);
            position[1] = Utils.fixedPointIntToFloat20Bit(pos[1]);
            position[2] = Utils.fixedPointIntToFloat20Bit(pos[2]);
            position[3] = (float) matrix.getYawAngle();
            position[4] = (float) matrix.getPitchAngle();
            position[5] = (float) matrix.getRollAngle();
            return position;
        }

        PathInfo pathInfo = getPathInfo();
        if (pathInfo != null) {
            Path path = map.getPaths().get(pathInfo.getPathId());
            PathResult result = path.evaluatePosition(pathInfo);

            IVector vec_x = new IVector();
            IVector vec_z = result.getRotation();
            vec_x.outerProduct12(GAME_Y_AXIS_POS, vec_z);
            vec_x.normalise();

            IVector vec_y = new IVector();
            vec_y.outerProduct12(vec_z, vec_x);

            matrix = new PSXMatrix();
            matrix.getMatrix()[0][0] = (short) vec_x.getX();
            matrix.getMatrix()[1][0] = (short) vec_x.getY();
            matrix.getMatrix()[2][0] = (short) vec_x.getZ();
            matrix.getMatrix()[0][1] = (short) vec_y.getX();
            matrix.getMatrix()[1][1] = (short) vec_y.getY();
            matrix.getMatrix()[2][1] = (short) vec_y.getZ();
            matrix.getMatrix()[0][2] = (short) vec_z.getX();
            matrix.getMatrix()[1][2] = (short) vec_z.getY();
            matrix.getMatrix()[2][2] = (short) vec_z.getZ();

            Vector endVec = result.getPosition();
            position[0] = endVec.getFloatX();
            position[1] = endVec.getFloatY();
            position[2] = endVec.getFloatZ();
            position[3] = (float) matrix.getYawAngle();
            position[4] = (float) matrix.getPitchAngle();
            position[5] = (float) matrix.getRollAngle();
            return position;
        }

        throw new UnsupportedOperationException("Tried to get the position of an entity without position data!");
    }

    /**
     * Set this entity's form book.
     * @param newEntry This entities new form book.
     */
    public void setFormBook(FormEntry newEntry) {
        Class<?> oldScriptClass = EntityScriptData.getScriptDataClass(getConfig(), this.formEntry);
        Class<?> newScriptClass = EntityScriptData.getScriptDataClass(getConfig(), newEntry);
        if (this.formEntry == null || !Objects.equals(newScriptClass, oldScriptClass))
            this.scriptData = EntityScriptData.makeData(getConfig(), newEntry);

        Class<?> oldEntityDataClass = EntityData.getEntityClass(getConfig(), this.formEntry);
        Class<?> newEntityDataClass = EntityData.getEntityClass(getConfig(), newEntry);
        if (this.formEntry == null || !Objects.equals(newEntityDataClass, oldEntityDataClass)) {
            PSXMatrix oldMatrix = getMatrixInfo(); // Call before setting entityData to null.
            PathInfo oldPath = getPathInfo();
            this.entityData = EntityData.makeData(getConfig(), newEntry);

            if (this.entityData instanceof MatrixData && oldMatrix != null)
                ((MatrixData) this.entityData).setMatrix(oldMatrix);

            if (this.entityData instanceof PathData && oldPath != null)
                ((PathData) this.entityData).setPathInfo(oldPath);
        }

        this.formEntry = newEntry;
    }
}
