package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'CMagicStoneDesc'
 * Loaded by CMagicStone::Init
 * Created by Kneesnap on 8/22/2023.
 */
public class CMagicStoneDesc extends CItemDesc {
    private MagicStoneType type = MagicStoneType.NONE;
    private static final int PADDING_VALUES = 8;

    public CMagicStoneDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.MAGIC_STONE.getClassId();
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