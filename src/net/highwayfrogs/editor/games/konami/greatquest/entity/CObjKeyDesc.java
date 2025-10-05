package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents the 'CObjKeyDesc' struct.
 * Loaded by CObjKey::Init.
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
public class CObjKeyDesc extends CItemDesc {
    private KeyType type = KeyType.NONE;
    private static final int PADDING_VALUES = 8;

    public CObjKeyDesc(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.OBJ_KEY);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = KeyType.getType(reader.readInt(), false);
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
        builder.append(padding).append("Key Type: ").append(this.type).append(Constants.NEWLINE);
    }

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        this.type = input.getKeyValueNodeOrError("keyType").getAsEnumOrError(KeyType.class);
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode("keyType").setAsEnum(this.type);
    }

    /**
     * Sets the key type.
     * @param newType The key type to apply
     */
    @SuppressWarnings("unused") // Available to Noodle scripts.
    public void setType(KeyType newType) {
        if (newType == null)
            throw new NullPointerException("newType");

        this.type = newType;
    }

    public enum KeyType {
        NONE, DOOR, CHEST, SLICK_WILLY, CLOVER_GATE, FAIRY_TOWN_A, FAIRY_TOWN_B, FAIRY_TOWN_C, TREE_OF_KNOWLEDGE, ENGINE_ROOM;

        /**
         * Gets the KeyType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return keyType
         */
        public static KeyType getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the key type from value " + value + ".");
            }

            return values()[value];
        }
    }
}