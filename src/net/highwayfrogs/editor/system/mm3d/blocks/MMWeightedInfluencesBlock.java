package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Represents the weighted influences block.
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
@Setter
public class MMWeightedInfluencesBlock extends MMDataBlockBody {
    private MMWeightedPositionType positionType;
    private long positionIndex; // Index into vertex or point array, based on type.
    private long boneJointIndex;
    private MMWeightedInfluenceType influenceType;
    private byte influenceWeight; // 0 - 100.

    public MMWeightedInfluencesBlock(MisfitModel3DObject parent) {
        super(OffsetType.WEIGHTED_INFLUENCES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.positionType = MMWeightedPositionType.getType(reader.readByte());
        this.positionIndex = reader.readUnsignedIntAsLong();
        this.boneJointIndex = reader.readUnsignedIntAsLong();
        this.influenceType = MMWeightedInfluenceType.values()[reader.readByte()];
        this.influenceWeight = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte((byte) this.positionType.getNumber());
        writer.writeUnsignedInt(this.positionIndex);
        writer.writeUnsignedInt(this.boneJointIndex);
        writer.writeByte((byte) this.influenceType.ordinal());
        writer.writeByte(this.influenceWeight);
    }

    /**
     * Sets the weight of this influence.
     * @param newWeight The new weight to use. Must be within [0,100].
     */
    public void setInfluenceWeight(byte newWeight) {
        if (newWeight < 0 || newWeight > 100)
            throw new RuntimeException("The specified weight (" + newWeight + "), was not in the range [0, 100]!");
        this.influenceWeight = newWeight;
    }

    @Getter
    @AllArgsConstructor
    public enum MMWeightedPositionType {
        VERTEX(0),
        POINT(2);

        private final int number;

        public static MMWeightedPositionType getType(int number) {
            for (MMWeightedPositionType type : values())
                if (type.getNumber() == number)
                    return type;
            return null;
        }
    }

    public enum MMWeightedInfluenceType {
        CUSTOM, // Manually entered by user.
        AUTOMATIC,
        REMAINDER
    }
}
