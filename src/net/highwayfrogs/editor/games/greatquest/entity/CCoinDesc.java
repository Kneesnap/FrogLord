package net.highwayfrogs.editor.games.greatquest.entity;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.kcClassID;

/**
 * Represents the 'CCoinDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
public class CCoinDesc extends CItemDesc {
    private CoinType type;
    private final int[] padCoin = new int[8];

    @Override
    protected int getTargetClassID() {
        return kcClassID.COIN.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = CoinType.getType(reader.readInt(), false);
        for (int i = 0; i < this.padCoin.length; i++)
            this.padCoin[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.type.ordinal());
        for (int i = 0; i < this.padCoin.length; i++)
            writer.writeInt(this.padCoin[i]);
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