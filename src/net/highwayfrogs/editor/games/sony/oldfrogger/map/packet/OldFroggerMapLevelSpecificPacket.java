package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerReactionType;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;

/**
 * Contains level-specific data.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapLevelSpecificPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "GAME";
    public static final int LEVEL_TIMER_COUNT = 10;
    public static final int GENERIC_LEVEL_DATA_INTEGER_COUNT = 243;
    private int froggerStartX; // TODO: These should be fixed point, but the fixed point MTF struct is on another branch right now.
    private int froggerStartY;
    private int froggerStartZ;
    private OldFroggerReactionType defaultReactionType = OldFroggerReactionType.Nothing; // Default reaction type for level (Used if no rectangles are hit)
    private final int[] defaultReactionData = new int[3]; // Default reaction data.
    private final int[] levelTimers = new int[LEVEL_TIMER_COUNT]; // Based on difficulty settings.
    private short preCalculatedLowestLandscapeHeight;
    private final long[] genericLevelData = new long[GENERIC_LEVEL_DATA_INTEGER_COUNT];

    public OldFroggerMapLevelSpecificPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        this.froggerStartX = reader.readInt();
        this.froggerStartY = reader.readInt();
        this.froggerStartZ = reader.readInt();
        this.defaultReactionType = OldFroggerReactionType.values()[reader.readUnsignedShortAsInt()];
        for (int i = 0; i < this.defaultReactionData.length; i++)
            this.defaultReactionData[i] = reader.readUnsignedShortAsInt();
        for (int i = 0; i < this.levelTimers.length; i++)
            this.levelTimers[i] = reader.readUnsignedShortAsInt();
        this.preCalculatedLowestLandscapeHeight = reader.readShort();
        reader.skipShort(); // Padding
        for (int i = 0; i < this.genericLevelData.length; i++)
            this.genericLevelData[i] = reader.readUnsignedIntAsLong();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(this.froggerStartX);
        writer.writeInt(this.froggerStartY);
        writer.writeInt(this.froggerStartZ);
        writer.writeUnsignedShort(this.defaultReactionType != null ? this.defaultReactionType.ordinal() : 0);
        for (int i = 0; i < this.defaultReactionData.length; i++)
            writer.writeUnsignedShort(this.defaultReactionData[i]);
        for (int i = 0; i < this.levelTimers.length; i++)
            writer.writeUnsignedShort(this.levelTimers[i]);
        writer.writeShort(this.preCalculatedLowestLandscapeHeight);
        writer.writeShort((short) 0); // Padding
        for (int i = 0; i < this.genericLevelData.length; i++)
            writer.writeUnsignedInt(this.genericLevelData[i]);
    }
}