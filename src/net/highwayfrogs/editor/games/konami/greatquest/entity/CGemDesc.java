package net.highwayfrogs.editor.games.konami.greatquest.entity;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.kcClassID;

/**
 * Represents the 'CGemDesc' struct.
 * Created by Kneesnap on 8/22/2023.
 */
public class CGemDesc extends CItemDesc {
    private GemType type;
    private final int[] padGem = new int[8];

    @Override
    protected int getTargetClassID() {
        return kcClassID.GEM.getClassId();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = GemType.getType(reader.readInt(), false);
        for (int i = 0; i < this.padGem.length; i++)
            this.padGem[i] = reader.readInt();
    }

    @Override
    public void saveData(DataWriter writer) {
        super.saveData(writer);
        writer.writeInt(this.type.ordinal());
        for (int i = 0; i < this.padGem.length; i++)
            writer.writeInt(this.padGem[i]);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        super.writeMultiLineInfo(builder, padding);
        builder.append(padding).append("Gem Type: ").append(this.type).append(Constants.NEWLINE);
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