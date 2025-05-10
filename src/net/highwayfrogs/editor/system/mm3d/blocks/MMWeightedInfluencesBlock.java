package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Represents the weighted influences block.
 * Version: 1.6+
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
@Setter
public class MMWeightedInfluencesBlock extends MMDataBlockBody {
    private MMWeightedPositionType positionType;
    private int positionIndex; // Index into vertex or point array, based on type.
    private int boneJointIndex;
    private MMWeightedInfluenceType influenceType;
    private byte influenceWeight; // 0 - 100.

    public static final byte MAX_INFLUENCE_WEIGHT = (byte) 100;

    public MMWeightedInfluencesBlock(MisfitModel3DObject parent) {
        super(OffsetType.WEIGHTED_INFLUENCES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.positionType = MMWeightedPositionType.getType(reader.readByte());
        this.positionIndex = reader.readInt();
        this.boneJointIndex = reader.readInt();
        this.influenceType = MMWeightedInfluenceType.values()[reader.readByte()];
        this.influenceWeight = reader.readByte();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeByte((byte) this.positionType.getNumber());
        writer.writeInt(this.positionIndex);
        writer.writeInt(this.boneJointIndex);
        writer.writeByte((byte) this.influenceType.ordinal());
        writer.writeByte(this.influenceWeight);
    }

    /**
     * Sets the weight of this influence.
     * @param newWeight The new weight to use. Must be within [0,100].
     */
    public void setInfluenceWeight(int newWeight) {
        if (newWeight < 0 || newWeight > MAX_INFLUENCE_WEIGHT)
            throw new RuntimeException("The specified weight (" + newWeight + "), was not in the range [0, " + MAX_INFLUENCE_WEIGHT + "]!");
        this.influenceWeight = (byte) newWeight;
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
