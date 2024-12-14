package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Contains map header data for MediEvil maps.
 * Created by Kneesnap on 3/8/2024.
 */
@Getter
public class MediEvilMapHeaderPacket extends MediEvilMapPacket implements IPropertyListCreator {
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
        this.levelString = reader.readNullTerminatedFixedSizeString(LEVEL_STRING_LENGTH);

        // Read header identifiers.
        int headerListPtr = reader.readInt();
        if (headerListPtr != reader.getIndex())
            throw new RuntimeException("MediEvilMapHeaderPacket expected header list at " + NumberUtils.toHexString(reader.getIndex()) + ", but was actually at " + NumberUtils.toHexString(headerListPtr));

        this.headerIdentifiers = new String[packetCount];
        for (int i = 0; i < packetCount; i++)
            this.headerIdentifiers[i] = Utils.toIdentifierString(reader.readInt());

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
        writer.writeStringBytes(FILE_TYPE);
        writer.writeNullPointer(); // fileLengthInBytes
        writer.writeUnsignedShort(getParentFile().getActivePacketCount()); // Packet Count
        writer.writeUnsignedShort(VERSION_CODE); // Version code.
        writer.writeNullTerminatedFixedSizeString(this.levelString, LEVEL_STRING_LENGTH);
        int headerListPtrAddress = writer.writeNullPointer();
        writer.writeAddressTo(headerListPtrAddress);
        for (int i = 0; i < getParentFile().getFilePackets().size(); i++) {
            SCFilePacket<? extends SCChunkedFile<MediEvilGameInstance>, MediEvilGameInstance> filePacket = getParentFile().getFilePackets().get(i);
            if (filePacket.isActive())
                writer.writeInt(filePacket.getIdentifierInteger());
        }
    }

    @Override
    protected void saveBodySecondPass(DataWriter writer, long sizeInBytes) {
        super.saveBodySecondPass(writer, sizeInBytes);
        writer.skipBytes(FILE_TYPE.length());
        writer.writeUnsignedInt(sizeInBytes);
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Level String", this.levelString);
        return propertyList;
    }
}