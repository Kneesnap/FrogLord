package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Contains map header data for MediEvil maps.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilMapHeaderPacket extends MediEvilMapPacket {
    public static final String IDENTIFIER = "GROF"; // 'FORG' backwards.
    private static final String FILE_TYPE = "MEDIEVIL";
    private static final int VERSION_CODE = 1;
    private static final int LEVEL_STRING_LENGTH = 64;
    private static final String DEFAULT_LEVEL_STRING = "Buzby sucks eggs, and Eastwood smells!";
    private static final int EXPECTED_PACKET_COUNT = 8;

    private String levelString = DEFAULT_LEVEL_STRING;
    private String[] headerIdentifiers = EMPTY_STRING_ARRAY;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public MediEvilMapHeaderPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        reader.verifyString(FILE_TYPE);
        long fileLengthInBytes = reader.readUnsignedIntAsLong();
        int packetCount = reader.readUnsignedShortAsInt();
        int versionCode = reader.readUnsignedShortAsInt();
        this.levelString = reader.readTerminatedStringOfLength(LEVEL_STRING_LENGTH);

        // Read header identifiers.
        int headerListPtr = reader.readInt();
        reader.setIndex(headerListPtr);
        this.headerIdentifiers = new String[packetCount];
        for (int i = 0; i < packetCount; i++)
            this.headerIdentifiers[i] = Utils.toMagicString(reader.readInt());

        // Verify data
        if (versionCode != VERSION_CODE)
            getLogger().warning("File is version " + versionCode + ", but only version " + VERSION_CODE + " is supported!");

        if (fileLengthInBytes != reader.getSize())
            getLogger().warning("The amount of bytes reported by the file '" + getParentFile().getFileDisplayName() + "' was " + fileLengthInBytes + ", but the actual amount was " + reader.getSize() + ".");

        if (packetCount != EXPECTED_PACKET_COUNT)
            getLogger().warning("File has " + packetCount + " packets, but " + EXPECTED_PACKET_COUNT + " were expected.");
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        // TODO: Implement later!
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long sizeInBytes) {
        // TODO: Implement later!
    }
}