package net.highwayfrogs.editor.games.tgq.entity;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.kcClassID;

/**
 * Represents the 'CUniqueItemDesc' struct.
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
@Setter
public class CUniqueItemDesc extends CItemDesc {
    private UniqueItemType type;
    private final int[] padUniqueItem = new int[8];

    @Override
    protected int getTargetClassID() {
        return kcClassID.UNIQUE_ITEM.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = UniqueItemType.getItem(reader.readInt(), false);
        for (int i = 0; i < this.padUniqueItem.length; i++)
            this.padUniqueItem[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.type.ordinal());
        for (int i = 0; i < this.padUniqueItem.length; i++)
            writer.writeInt(this.padUniqueItem[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Unique Item Type: ").append(this.type).append(Constants.NEWLINE);
    }

    public enum UniqueItemType {
        NONE, BONE, CLOVER, FAKE_CLOVER, SEED, ENGINE_FUEL, TEMPLE_STATUE,
        SQUARE_ARTIFACT, CIRCLE_ARTIFACT, TRIANGLE_ARTIFACT, BONE_CRUNCHER_STATUE,
        CROWN, RUBY_SHARD, RUBY_SPHERE, RUBY_TEARDROP;

        /**
         * Gets the UniqueItemType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return itemType
         */
        public static UniqueItemType getItem(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the unique item type from value " + value + ".");
            }

            return values()[value];
        }
    }
}
