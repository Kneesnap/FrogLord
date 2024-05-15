package net.highwayfrogs.editor.games.sony.shared.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFilePacket;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the header packet in a Sony Cambridge map file. (v1999)
 * Created by Kneesnap on 5/12/2024.
 */
@Getter
public class SCMapHeaderPacket<TGameInstance extends SCGameInstance> extends SCMapFilePacket<SCMapFile<TGameInstance>, TGameInstance> {
    public static final String IDENTIFIER = "FORG";
    private static final int VERSION_CODE = 0;

    private int sectionId;
    private String[] headerIdentifiers = EMPTY_STRING_ARRAY;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    public SCMapHeaderPacket(SCMapFile<TGameInstance> parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        long fileLengthInBytes = reader.readUnsignedIntAsLong();
        int packetCount = reader.readUnsignedShortAsInt();
        int versionCode = reader.readUnsignedShortAsInt();
        this.sectionId = reader.readInt();

        // Read header identifiers.
        int headerListPtr = reader.readInt();
        if (headerListPtr != reader.getIndex())
            throw new RuntimeException("SCMapHeaderPacket expected header list at " + Utils.toHexString(reader.getIndex()) + ", but was actually at " + Utils.toHexString(headerListPtr));

        this.headerIdentifiers = new String[packetCount];
        for (int i = 0; i < packetCount; i++)
            this.headerIdentifiers[i] = Utils.toMagicString(reader.readInt());

        // Verify data
        if (versionCode != VERSION_CODE)
            getLogger().warning("File is version " + versionCode + ", but only version " + VERSION_CODE + " is supported!");

        if (fileLengthInBytes != reader.getSize())
            getLogger().warning("The amount of bytes reported by the file '" + getParentFile().getFileDisplayName() + "' was " + fileLengthInBytes + ", but the actual amount was " + reader.getSize() + ".");
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeNullPointer(); // fileLengthInBytes
        writer.writeUnsignedShort(getParentFile().getActivePacketCount()); // Packet Count
        writer.writeUnsignedShort(VERSION_CODE); // Version code.
        writer.writeInt(this.sectionId); // Section ID.
        int headerListPtrAddress = writer.writeNullPointer();
        writer.writeAddressTo(headerListPtrAddress);
        for (int i = 0; i < getParentFile().getFilePackets().size(); i++) {
            SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> filePacket = getParentFile().getFilePackets().get(i);
            if (filePacket.isActive())
                writer.writeInt(filePacket.getIdentifierInteger());
        }
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long sizeInBytes) {
        super.saveBodySecondPass(writer, sizeInBytes);
        writer.writeUnsignedInt(sizeInBytes);
    }
}