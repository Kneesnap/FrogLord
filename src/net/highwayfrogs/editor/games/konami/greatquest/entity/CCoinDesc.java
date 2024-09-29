package net.highwayfrogs.editor.games.konami.greatquest.entity;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'CCoinDesc' struct.
 * Loaded by CCoin::Init.
 * Created by Kneesnap on 8/22/2023.
 */
public class CCoinDesc extends CItemDesc {
    private CoinType type = CoinType.NONE;
    private static final int PADDING_VALUES = 8;

    public CCoinDesc(@NonNull kcCResourceGeneric resource) {
        super(resource);
    }

    @Override
    protected int getTargetClassID() {
        return kcClassID.COIN.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = CoinType.getType(reader.readInt(), false);
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
        builder.append(padding).append("Coin Type: ").append(this.type).append(Constants.NEWLINE);
    }

    public enum CoinType {
        NONE, COPPER, SILVER, GOLD;

        /**
         * Gets the GemType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return gemType
         */
        public static CoinType getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the coin type from value " + value + ".");
            }

            return values()[value];
        }
    }
}