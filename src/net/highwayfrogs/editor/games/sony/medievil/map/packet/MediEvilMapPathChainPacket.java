package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the 'PCHN' MediEvil map packet.
 * Created by Kneesnap on 2/3/2026.
 */
public class MediEvilMapPathChainPacket extends MediEvilMapPacket {
    private final List<MediEvilMapPathChain> pathChains = new ArrayList<>();
    private byte padding; // It is not clear if this byte is actually used for anything.

    public static final String IDENTIFIER = "NHCP"; // 'PCHN'

    public MediEvilMapPathChainPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Runtime values probably.
        short chainCount = reader.readUnsignedByteAsShort();
        this.padding = reader.readByte();
        int chainDataStartAddress = reader.readInt();

        // Read path chains.
        reader.requireIndex(getLogger(), chainDataStartAddress, "Expected Path chain data");
        this.pathChains.clear();
        for (int i = 0; i < chainCount; i++) {
            MediEvilMapPathChain newPathChain = new MediEvilMapPathChain(this);
            this.pathChains.add(newPathChain);
            newPathChain.load(reader);
        }

        // Read pointer offsets.
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadPathSplineIdOffset(reader);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadCameraSplineIdOffset(reader);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadPreviousChainIdOffset(reader);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadNextChainIdOffset(reader);

        // Read data.
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadPathSplineIds(reader);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadCameraSplineIds(reader);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadPreviousChainIds(reader);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).loadNextChainIds(reader);
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeNull(Constants.SHORT_SIZE); // Runtime values probably.
        writer.writeUnsignedByte((short) this.pathChains.size());
        writer.writeByte(this.padding);
        int chainDataStartAddress = writer.writeNullPointer();

        // Write path chains.
        writer.writeAddressTo(chainDataStartAddress);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).save(writer);

        // Write pointer offsets.
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).savePathSplineIdOffset(writer);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).saveCameraSplineIdOffset(writer);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).savePreviousChainIdOffset(writer);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).saveNextChainIdOffset(writer);

        // Write data.
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).savePathSplineIds(writer);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).saveCameraSplineIds(writer);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).savePreviousChainIds(writer);
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).saveNextChainIds(writer);
    }

    @Override
    public void clear() {
        this.pathChains.clear();
        this.padding = (byte) 0x00;
    }

    public static class MediEvilMapPathChain extends SCGameData<MediEvilGameInstance> {
        private final MediEvilMapPathChainPacket pathChainPacket;
        private short pathGroup;
        private byte[] previousChainIds; // TODO: Replace these with object references.
        private byte previousChainConnectAB; // "Connect to start/end"
        private byte[] nextChainIds; // TODO: Replace these with object references.
        private byte nextChainConnectAB; // "Connect to start/end"
        private byte[] pathSplineIds; // TODO: Replace these with object references.
        private byte[] cameraSplineIds; // TODO: Replace these with object references.

        private int tempPreviousChainIdOffset = -1;
        private int tempNextChainIdOffset = -1;
        private int tempPathSplineIdOffset = -1;
        private int tempCameraSplineIdOffset = -1;


        public MediEvilMapPathChain(MediEvilMapPathChainPacket pathChainPacket) {
            super(pathChainPacket.getGameInstance());
            this.pathChainPacket = pathChainPacket;
        }

        @Override
        public void load(DataReader reader) {
            short previousNumber = reader.readUnsignedByteAsShort();
            short nextNumber = reader.readUnsignedByteAsShort();
            short splineCount = reader.readUnsignedByteAsShort();
            this.pathGroup = reader.readUnsignedByteAsShort();

            this.previousChainIds = new byte[previousNumber];
            this.nextChainIds = new byte[nextNumber];
            this.cameraSplineIds = new byte[splineCount];
            this.pathSplineIds = new byte[splineCount];
            this.tempPreviousChainIdOffset = reader.readInt();
            this.tempNextChainIdOffset = reader.readInt();
            this.tempPathSplineIdOffset = reader.readInt();
            this.tempCameraSplineIdOffset = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            if (this.pathSplineIds.length != this.cameraSplineIds.length)
                throw new IllegalStateException("Expected the number of pathSplineIds (" + this.pathSplineIds.length + ") to match the number of cameraSplineIds (" + this.cameraSplineIds.length + "), but they did not!");

            writer.writeUnsignedByte((short) this.previousChainIds.length);
            writer.writeUnsignedByte((short) this.nextChainIds.length);
            writer.writeUnsignedByte((short) this.pathSplineIds.length);
            writer.writeUnsignedByte(this.pathGroup);

            this.tempPreviousChainIdOffset = writer.writeNullPointer();
            this.tempNextChainIdOffset = writer.writeNullPointer();
            this.tempPathSplineIdOffset = writer.writeNullPointer();
            this.tempCameraSplineIdOffset = writer.writeNullPointer();
        }

        private void loadPreviousChainIdOffset(DataReader reader) {
            if (this.tempPreviousChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load previousChainIdOffset, the pointer " + NumberUtils.toHexString(this.tempPreviousChainIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempPreviousChainIdOffset, "Expected previousChainIdOffset");
            this.tempPreviousChainIdOffset = reader.readInt();
            this.previousChainConnectAB = reader.readByte();
            reader.alignRequireByte((byte) 0xFF, Constants.INTEGER_SIZE);
        }

        private void savePreviousChainIdOffset(DataWriter writer) {
            if (this.tempPreviousChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save previousChainIdOffset, the pointer " + NumberUtils.toHexString(this.tempPreviousChainIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempPreviousChainIdOffset);
            this.tempPreviousChainIdOffset = writer.writeNullPointer();
            writer.writeByte(this.previousChainConnectAB);
            writer.align(Constants.INTEGER_SIZE, (byte) 0xFF);
        }

        private void loadPreviousChainIds(DataReader reader) {
            if (this.tempPreviousChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load previousChainIds, the pointer " + NumberUtils.toHexString(this.tempPreviousChainIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempPreviousChainIdOffset, "Expected previousChainIds");
            this.tempPreviousChainIdOffset = -1;
            reader.readBytes(this.previousChainIds);
        }

        private void savePreviousChainIds(DataWriter writer) {
            if (this.tempPreviousChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save previousChainIds, the pointer " + NumberUtils.toHexString(this.tempPreviousChainIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempPreviousChainIdOffset);
            this.tempPreviousChainIdOffset = -1;
            writer.writeBytes(this.previousChainIds);
        }

        private void loadNextChainIdOffset(DataReader reader) {
            if (this.tempNextChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load nextChainIdOffset, the pointer " + NumberUtils.toHexString(this.tempNextChainIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempNextChainIdOffset, "Expected nextChainIdOffset");
            this.tempNextChainIdOffset = reader.readInt();
            this.nextChainConnectAB = reader.readByte();
            reader.alignRequireByte((byte) 0xFF, Constants.INTEGER_SIZE);
        }

        private void saveNextChainIdOffset(DataWriter writer) {
            if (this.tempNextChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save nextChainIdOffset, the pointer " + NumberUtils.toHexString(this.tempNextChainIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempNextChainIdOffset);
            this.tempNextChainIdOffset = writer.writeNullPointer();
            writer.writeByte(this.nextChainConnectAB);
            writer.align(Constants.INTEGER_SIZE, (byte) 0xFF);
        }

        private void loadNextChainIds(DataReader reader) {
            if (this.tempNextChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load nextChainIds, the pointer " + NumberUtils.toHexString(this.tempNextChainIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempNextChainIdOffset, "Expected nextChainIds");
            this.tempNextChainIdOffset = -1;
            reader.readBytes(this.nextChainIds);
        }

        private void saveNextChainIds(DataWriter writer) {
            if (this.tempNextChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save nextChainIds, the pointer " + NumberUtils.toHexString(this.tempNextChainIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempNextChainIdOffset);
            this.tempNextChainIdOffset = -1;
            writer.writeBytes(this.nextChainIds);
        }

        private void loadPathSplineIdOffset(DataReader reader) {
            if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load pathSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempPathSplineIdOffset, "Expected pathSplineIdOffset");
            this.tempPathSplineIdOffset = reader.readInt();
        }

        private void savePathSplineIdOffset(DataWriter writer) {
            if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save pathSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempPathSplineIdOffset);
            this.tempPathSplineIdOffset = writer.writeNullPointer();
        }

        private void loadPathSplineIds(DataReader reader) {
            if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load pathSplineIds, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempPathSplineIdOffset, "Expected pathSplineIds");
            this.tempPathSplineIdOffset = -1;
            reader.readBytes(this.pathSplineIds);
        }

        private void savePathSplineIds(DataWriter writer) {
            if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save pathSplineIds, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempPathSplineIdOffset);
            this.tempPathSplineIdOffset = -1;
            writer.writeBytes(this.pathSplineIds);
        }

        private void loadCameraSplineIdOffset(DataReader reader) {
            if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load cameraSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempCameraSplineIdOffset, "Expected cameraSplineIdOffset");
            this.tempCameraSplineIdOffset = reader.readInt();
        }

        private void saveCameraSplineIdOffset(DataWriter writer) {
            if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save cameraSplineIdOffset, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempCameraSplineIdOffset);
            this.tempCameraSplineIdOffset = writer.writeNullPointer();
        }

        private void loadCameraSplineIds(DataReader reader) {
            if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot load cameraSplineIds, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

            requireReaderIndex(reader, this.tempCameraSplineIdOffset, "Expected cameraSplineIds");
            this.tempCameraSplineIdOffset = -1;
            reader.readBytes(this.cameraSplineIds);
        }

        private void saveCameraSplineIds(DataWriter writer) {
            if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save cameraSplineIds, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempCameraSplineIdOffset);
            this.tempCameraSplineIdOffset = -1;
            writer.writeBytes(this.cameraSplineIds);
        }
    }
}
