package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.system.Config;

/**
 * Represents the 'CGemDesc' struct.
 * Loaded by CGem::Init
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
public class CGemDesc extends CItemDesc {
    private GemType type = GemType.NONE;
    private static final int PADDING_VALUES = 8;

    public CGemDesc(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.GEM);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = GemType.getType(reader.readInt(), false);
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
        builder.append(padding).append("Gem Type: ").append(this.type).append(Constants.NEWLINE);
    }

    @Override
    public void fromConfig(Config input) {
        super.fromConfig(input);
        this.type = input.getKeyValueNodeOrError("gemType").getAsEnum(GemType.class);
    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode("gemType").setAsEnum(this.type);
    }

    /**
     * Sets the gem type.
     * @param newType The gem type to apply
     */
    @SuppressWarnings("unused") // Available to Noodle scripts.
    public void setType(GemType newType) {
        if (newType == null)
            throw new NullPointerException("newType");

        this.type = newType;
    }

    public enum GemType {
        NONE, AMETHYST, RUBY, DIAMOND, SAPPHIRE, QUARTZ;

        /**
         * Gets the GemType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return gemType
         */
        public static GemType getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the gem type from value " + value + ".");
            }

            return values()[value];
        }
    }
}