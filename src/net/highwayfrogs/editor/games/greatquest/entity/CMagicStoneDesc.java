package net.highwayfrogs.editor.games.greatquest.entity;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.greatquest.kcClassID;

/**
 * Represents the 'CMagicStoneDesc'
 * Created by Kneesnap on 8/22/2023.
 */
public class CMagicStoneDesc extends CItemDesc {
    private MagicStoneType type;
    private final int[] padMagicStone = new int[8];

    @Override
    protected int getTargetClassID() {
        return kcClassID.MAGIC_STONE.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = MagicStoneType.getType(reader.readInt(), false);
        for (int i = 0; i < this.padMagicStone.length; i++)
            this.padMagicStone[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.type.ordinal());
        for (int i = 0; i < this.padMagicStone.length; i++)
            writer.writeInt(this.padMagicStone[i]);
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