package net.highwayfrogs.editor.games.sony.shared.model.primitive;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.model.PTModel;
import net.highwayfrogs.editor.games.sony.shared.model.PTPartInstanceData;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticPartCel;

/**
 * Represents a control primitive.
 * Created by Kneesnap on 5/17/2024.
 */
@Getter
public class PTPrimitiveControl extends SCSharedGameData implements IPTPrimitive {
    private PTPrimitiveControlType controlType = PTPrimitiveControlType.POSITION_STATIC;
    private int sourceVertexIndex;
    private int targetTransformIndex;
    private int vertexCount;

    public PTPrimitiveControl(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public PTPrimitiveType getPrimitiveType() {
        return PTPrimitiveType.CONTROL;
    }

    @Override
    public void load(DataReader reader) {
        this.controlType = PTPrimitiveControlType.values()[reader.readUnsignedShortAsInt()];
        this.sourceVertexIndex = reader.readUnsignedShortAsInt();
        this.targetTransformIndex = reader.readUnsignedShortAsInt();
        this.vertexCount = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.controlType.ordinal());
        writer.writeUnsignedShort(this.sourceVertexIndex);
        writer.writeUnsignedShort(this.targetTransformIndex);
        writer.writeUnsignedShort(this.vertexCount);
    }

    /**
     * Gets the vector corresponding to the provided ID.
     * @param model the model to get the vector from.
     * @param partCel the partcel which the vertex belongs to.
     * @param vectorId the id of the vector
     * @return vector
     */
    public SVector getVector(PTModel model, PTStaticPartCel partCel, int vectorId) {
        PTPartInstanceData partInstanceData = model.getPartData().get(partCel.getParentPart().getPartIndex());
        int vertexOffset = partCel.getParentPart().getMimeVectors() + partCel.getParentPart().getMimeSkinVectors();

        switch (this.controlType) {
            case POSITION_MIME:
                return partCel.getMimeVectors().get(this.sourceVertexIndex + vectorId);
            case POSITION_MIMESKIN: // Rotate Mime & skin vertices
                return partInstanceData.getVectorBlock()[this.sourceVertexIndex + vectorId];
            case POSITION_SKIN: // Rotate skin vertices
                return partInstanceData.getVectorBlock()[this.sourceVertexIndex + vertexOffset + vectorId];
            case POSITION_STATIC: // Rotate static vertices
                return partCel.getVectors().get(this.sourceVertexIndex + vectorId);
            case NORMAL_MIME: // Mime normal.
                return partCel.getMimeVectors().get(vectorId);
            case NORMAL_MIMESKIN: // Mime & skinned normal.
                return partInstanceData.getVectorBlock()[vectorId];
            case NORMAL_SKIN: // Skinned normal
                return partInstanceData.getVectorBlock()[vertexOffset + vectorId];
            case NORMAL_STATIC: // Static normal
                return partCel.getVectors().get(this.sourceVertexIndex + vectorId);
            default:
                throw new RuntimeException("Unsupported control type: " + this.controlType);
        }
    }

    public enum PTPrimitiveControlType {
        POSITION_MIME, // 0, MediEvil 2
        POSITION_MIMESKIN, // 1
        POSITION_SKIN, // 2, Moon Warrior, MediEvil II
        POSITION_STATIC, // 3, Moon Warrior, MediEvil II
        NORMAL_MIME, // 4, MediEvil II
        NORMAL_MIMESKIN, // 5, MediEvil II
        NORMAL_SKIN, // 6, Moon Warrior, MediEvil II
        NORMAL_STATIC; // 7, Moon Warrior, MediEvil II

        /**
         * Returns true iff the type represents positional data.
         */
        public boolean isPosition() {
            return this == POSITION_MIME || this == POSITION_MIMESKIN || this == POSITION_SKIN || this == POSITION_STATIC;
        }
    }
}