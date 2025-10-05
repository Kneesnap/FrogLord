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
 * Represents the 'CMagicStoneDesc'
 * Loaded by CMagicStone::Init
 * Created by Kneesnap on 8/22/2023.
 */
@Getter
public class CMagicStoneDesc extends CItemDesc {
    private MagicStoneType type = MagicStoneType.NONE;
    private static final int PADDING_VALUES = 8;

    public CMagicStoneDesc(@NonNull kcCResourceGeneric resource) {
        super(resource, kcEntityDescType.MAGIC_STONE);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = MagicStoneType.getType(reader.readInt(), false);
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
        builder.append(padding).append("Magic Stone Type: ").append(this.type).append(Constants.NEWLINE);
    }

    @Override
    public void fromConfig(ILogger logger, Config input) {
        super.fromConfig(logger, input);
        this.type = input.getKeyValueNodeOrError("stoneType").getAsEnumOrError(MagicStoneType.class);

    }

    @Override
    public void toConfig(Config output, kcScriptDisplaySettings settings) {
        super.toConfig(output, settings);
        output.getOrCreateKeyValueNode("stoneType").setAsEnum(this.type);
    }

    /**
     * Sets the magic stone type.
     * @param newType The magic stone type to apply
     */
    @SuppressWarnings("unused") // Available to Noodle scripts.
    public void setType(MagicStoneType newType) {
        if (newType == null)
            throw new NullPointerException("newType");

        this.type = newType;
    }

    public enum MagicStoneType {
        NONE, LIGHT, SLEEP, FIRE, ICE, LIGHTNING, WIND, VORTEX, SPEED, SHRINK;

        /**
         * Gets the MagicStoneType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return magicStoneType
         */
        public static MagicStoneType getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the magic stone type from value " + value + ".");
            }

            return values()[value];
        }
    }
}