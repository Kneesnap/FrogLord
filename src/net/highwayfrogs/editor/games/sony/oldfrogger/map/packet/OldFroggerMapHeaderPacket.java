package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the header packet in a pre-recode Frogger map.
 * Created by Kneesnap on 12/8/2023.
 */
@Getter
public class OldFroggerMapHeaderPacket extends OldFroggerMapPacket {
    public static final String IDENTIFIER = "FROG";
    private static final String VERSION = "1.02";
    private static final int COMMENT_LENGTH = 64;
    private static final String DEFAULT_COMMENT = "Hey! Crunchy crunchy frog, yeah!";
    private static final int EXPECTED_PACKET_COUNT = 8;
    private static final long EXPECTED_MOF_ADDRESS = 0xFFFFFFFFL;

    private String comment = DEFAULT_COMMENT;

    public OldFroggerMapHeaderPacket(OldFroggerMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        long fileLengthInBytes = reader.readUnsignedIntAsLong();
        int packetCount = reader.readUnsignedShortAsInt();
        reader.skipShort(); // Padding
        reader.verifyString(VERSION);
        this.comment = reader.readNullTerminatedFixedSizeString(COMMENT_LENGTH);

        // Read header pointers.
        long levelSpecificAddr = reader.readUnsignedIntAsLong();
        long graphicalAddr = reader.readUnsignedIntAsLong();
        int mofsAddr = reader.readInt(); // This should be uint32, but it seems we have an issue with this.
        long formsAddr = reader.readUnsignedIntAsLong();
        long entitiesAddr = reader.readUnsignedIntAsLong();
        long zonesAddr = reader.readUnsignedIntAsLong();
        long splinesAddr = reader.readUnsignedIntAsLong();

        // Verify data
        if (fileLengthInBytes != reader.getSize())
            getLogger().warning("The amount of bytes reported by the file '%s' was %d, but the actual amount was %d.", getParentFile().getFileDisplayName(), fileLengthInBytes, reader.getSize());

        if ((mofsAddr & 0xFFFFFFFFL) != EXPECTED_MOF_ADDRESS)
            getLogger().warning("MofsAddress was %s, but %s was expected.",  NumberUtils.toHexString(mofsAddr), NumberUtils.toHexString(EXPECTED_MOF_ADDRESS));
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeInt(0); // File size in bytes (overwrite later)
        writer.writeUnsignedShort(EXPECTED_PACKET_COUNT);
        writer.writeUnsignedShort(0); // Padding
        writer.writeStringBytes(VERSION);
        writer.writeNullTerminatedFixedSizeString(this.comment, COMMENT_LENGTH);

        writer.writeUnsignedInt(0); // levelSpecificAddr
        writer.writeUnsignedInt(0); // graphicalAddr
        writer.writeUnsignedInt(EXPECTED_MOF_ADDRESS); // mofsAddr
        writer.writeUnsignedInt(0); // formsAddr
        writer.writeUnsignedInt(0); // entitiesAddr
        writer.writeUnsignedInt(0); // zonesAddr
        writer.writeUnsignedInt(0); // splinesAddr
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long sizeInBytes) {
        writer.writeUnsignedInt(sizeInBytes); // Write file length.
        writer.skipBytes(Constants.SHORT_SIZE); // packet count
        writer.skipBytes(Constants.SHORT_SIZE); // padding
        writer.skipBytes(VERSION.length());
        writer.skipBytes(COMMENT_LENGTH);

        // Write values.
        writer.writeUnsignedInt(getParentFile().getLevelSpecificDataPacket().getLastValidReadHeaderAddress());
        writer.writeUnsignedInt(getParentFile().getGraphicalHeaderPacket().getLastValidReadHeaderAddress());
        writer.skipBytes(Constants.INTEGER_SIZE); // mofsAddr
        writer.writeUnsignedInt(getParentFile().getFormInstancePacket().getLastValidReadHeaderAddress());
        writer.writeUnsignedInt(getParentFile().getEntityMarkerPacket().getLastValidReadHeaderAddress());
        writer.writeUnsignedInt(getParentFile().getZonePacket().getLastValidReadHeaderAddress());
        writer.writeUnsignedInt(getParentFile().getPathPacket().getLastValidReadHeaderAddress());
    }
}