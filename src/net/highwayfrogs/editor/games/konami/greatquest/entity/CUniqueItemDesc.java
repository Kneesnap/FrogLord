package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'CUniqueItemDesc' struct.
 * Loaded by CUniqueItem::Init
 * Created by Kneesnap on 8/21/2023.
 */
@Getter
public class CUniqueItemDesc extends CItemDesc {
    private UniqueItemType type = UniqueItemType.NONE;
    private static final int PADDING_VALUES = 8;

    public CUniqueItemDesc(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.UNIQUE_ITEM);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = UniqueItemType.getItem(reader.readInt(), false);
        reader.skipBytesRequireEmpty(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.type.ordinal());
        writer.writeNull(PADDING_VALUES * Constants.INTEGER_SIZE);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Unique Item Type: ").append(this.type).append(Constants.NEWLINE);
    }

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        this.type = input.getKeyValueNodeOrError("itemType").getAsEnumOrError(UniqueItemType.class);
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode("itemType").setAsEnum(this.type);
    }

    /**
     * Sets the unique item type.
     * @param newType The unique item type to apply
     */
    @SuppressWarnings("unused") // Available to Noodle scripts.
    public void setType(UniqueItemType newType) {
        if (newType == null)
            throw new NullPointerException("newType");

        this.type = newType;
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