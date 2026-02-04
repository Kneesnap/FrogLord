package net.highwayfrogs.editor.games.sony.medievil.map.packet;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.packet.MediEvilMap2DSplinePacket.MediEvilMap2DSpline;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the 'PCHN' MediEvil map packet.
 * Created by Kneesnap on 2/3/2026.
 */
@Getter
public class MediEvilMapPathChainPacket extends MediEvilMapPacket {
    private final List<MediEvilMapPathChain> pathChains = new ArrayList<>();

    public static final String IDENTIFIER = "NHCP"; // 'PCHN'

    public MediEvilMapPathChainPacket(MediEvilMapFile parentFile) {
        super(parentFile, IDENTIFIER);
    }

    @Override
    protected void loadBody(DataReader reader, int endIndex) {
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Runtime values probably.
        short chainCount = reader.readUnsignedByteAsShort();
        reader.skipByte(); // Padding (garbage)
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

        // Resolve previous/next path chains.
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).resolvePreviousChains();
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).resolveNextChains();
    }

    @Override
    protected void loadBodySecondPass(DataReader reader, int endIndex) {
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).resolvePathSplines();
        for (int i = 0; i < this.pathChains.size(); i++)
            this.pathChains.get(i).resolveCameraSplines();
    }

    @Override
    protected void saveBodyFirstPass(DataWriter writer) {
        writer.writeNull(Constants.SHORT_SIZE); // Runtime values probably.
        writer.writeUnsignedByte((short) this.pathChains.size());
        writer.writeByte(Constants.NULL_BYTE); // Padding (garbage)
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
    }

    public static class MediEvilMapPathChain extends SCGameData<MediEvilGameInstance> {
        private final MediEvilMapPathChainPacket pathChainPacket;
        private short pathGroup;
        private final List<MediEvilMapPathChain> previousChains = new ArrayList<>(); // Must not exceed 8 entries.
        private byte previousChainConnectAB; // Of all the previousChains, which ones are marked start, and which ones are marked end. (Bit flags)
        private final List<MediEvilMapPathChain> nextChains = new ArrayList<>(); // Must not exceed 8 entries.
        private byte nextChainConnectAB; // Of all the nextChains, which ones are marked start, and which ones are marked end. (Bit flags)
        private final List<MediEvilMap2DSpline> pathSplines = new ArrayList<>();
        private final List<MediEvilMap2DSpline> cameraSplines = new ArrayList<>(); // Potentially unused.

        private int tempPreviousChainIdOffset = -1;
        private byte[] tempPreviousChainIds;
        private int tempNextChainIdOffset = -1;
        private byte[] tempNextChainIds;
        private int tempPathSplineIdOffset = -1;
        private int tempCameraSplineIdOffset = -1;
        private byte[] tempPathSplineIds;
        private byte[] tempCameraSplineIds;


        public MediEvilMapPathChain(MediEvilMapPathChainPacket pathChainPacket) {
            super(pathChainPacket.getGameInstance());
            this.pathChainPacket = pathChainPacket;
        }

        /**
         * Gets the ID used to identify this spline when saving/loading.
         * This ID has the ability to change while FrogLord applies changes to files, so it should not be used to identify a spline.
         */
        public int getId() {
            int index = this.pathChainPacket.pathChains.indexOf(this);
            if (index < 0)
                throw new IllegalArgumentException("The referenced " + getClass().getSimpleName() + " is not registered as part of the MediEvilMapFile.");

            return index;
        }

        @Override
        public ILogger getLogger() {
            return new AppendInfoLoggerWrapper(this.pathChainPacket.getLogger(), getClass().getSimpleName() + "[" + this.pathChainPacket.pathChains.indexOf(this) + "]", AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
        }

        @Override
        public void load(DataReader reader) {
            short previousNumber = reader.readUnsignedByteAsShort();
            short nextNumber = reader.readUnsignedByteAsShort();
            short splineCount = reader.readUnsignedByteAsShort();
            this.pathGroup = reader.readUnsignedByteAsShort();

            this.tempPreviousChainIds = new byte[previousNumber];
            this.tempNextChainIds = new byte[nextNumber];
            this.tempCameraSplineIds = new byte[splineCount];
            this.tempPathSplineIds = new byte[splineCount];
            this.tempPreviousChainIdOffset = reader.readInt();
            this.tempNextChainIdOffset = reader.readInt();
            this.tempPathSplineIdOffset = reader.readInt();
            this.tempCameraSplineIdOffset = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            if (this.pathSplines.size() != this.cameraSplines.size())
                throw new IllegalStateException("Expected the number of pathSpline (" + this.pathSplines.size() + ") to match the number of cameraSplines (" + this.cameraSplines.size() + "), but they did not!");

            writer.writeUnsignedByte((short) this.previousChains.size());
            writer.writeUnsignedByte((short) this.nextChains.size());
            writer.writeUnsignedByte((short) this.pathSplines.size());
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
            reader.readBytes(this.tempPreviousChainIds);
        }

        private void savePreviousChainIds(DataWriter writer) {
            if (this.tempPreviousChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save previousChainIds, the pointer " + NumberUtils.toHexString(this.tempPreviousChainIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempPreviousChainIdOffset);
            this.tempPreviousChainIdOffset = -1;
            for (int i = 0; i < this.previousChains.size(); i++)
                writer.writeUnsignedByte((short) this.previousChains.get(i).getId());
        }

        private void resolvePreviousChains() {
            if (this.tempPreviousChainIds == null || this.tempPreviousChainIdOffset != -1)
                throw new RuntimeException("Cannot resolve previous chains, the data was not setup properly.");

            this.previousChains.clear();
            List<MediEvilMapPathChain> pathChains = this.pathChainPacket.getPathChains();
            for (int i = 0; i < this.tempPreviousChainIds.length; i++) {
                int pathChainIndex = (this.tempPreviousChainIds[i] & 0xFF);
                if (pathChainIndex >= pathChains.size())
                    throw new IllegalArgumentException("Invalid pathChainIndex: " + pathChainIndex);

                this.previousChains.add(pathChains.get(pathChainIndex));
            }

            this.tempPreviousChainIds = null;
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
            reader.readBytes(this.tempNextChainIds);
        }

        private void saveNextChainIds(DataWriter writer) {
            if (this.tempNextChainIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save nextChainIds, the pointer " + NumberUtils.toHexString(this.tempNextChainIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempNextChainIdOffset);
            this.tempNextChainIdOffset = -1;
            for (int i = 0; i < this.nextChains.size(); i++)
                writer.writeUnsignedByte((short) this.nextChains.get(i).getId());
        }

        private void resolveNextChains() {
            if (this.tempNextChainIds == null || this.tempNextChainIdOffset != -1)
                throw new RuntimeException("Cannot resolve next chains, the data was not setup properly.");

            this.nextChains.clear();
            List<MediEvilMapPathChain> pathChains = this.pathChainPacket.getPathChains();
            for (int i = 0; i < this.tempNextChainIds.length; i++) {
                int pathChainIndex = (this.tempNextChainIds[i] & 0xFF);
                if (pathChainIndex >= pathChains.size())
                    throw new IllegalArgumentException("Invalid pathChainIndex: " + pathChainIndex);

                this.previousChains.add(pathChains.get(pathChainIndex));
            }

            this.tempNextChainIds = null;
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
            reader.readBytes(this.tempPathSplineIds);
        }

        private void savePathSplineIds(DataWriter writer) {
            if (this.tempPathSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save pathSplineIds, the pointer " + NumberUtils.toHexString(this.tempPathSplineIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempPathSplineIdOffset);
            this.tempPathSplineIdOffset = -1;
            for (int i = 0; i < this.pathSplines.size(); i++)
                writer.writeUnsignedByte((short) this.pathSplines.get(i).getId());
        }

        private void resolvePathSplines() {
            if (this.tempPathSplineIds == null || this.tempPathSplineIdOffset != -1)
                throw new RuntimeException("Cannot resolve path splines, the data was not setup properly.");

            this.pathSplines.clear();
            List<MediEvilMap2DSpline> pathSplines = this.pathChainPacket.getParentFile().getSpline2DPacket().getSplines();
            for (int i = 0; i < this.tempPathSplineIds.length; i++) {
                int pathSplineIndex = (this.tempPathSplineIds[i] & 0xFF);
                if (pathSplineIndex >= pathSplines.size())
                    throw new IllegalArgumentException("Invalid pathSplineIndex: " + pathSplineIndex);

                this.pathSplines.add(pathSplines.get(pathSplineIndex));
            }

            this.tempPathSplineIds = null;
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
            reader.readBytes(this.tempCameraSplineIds);
        }

        private void saveCameraSplineIds(DataWriter writer) {
            if (this.tempCameraSplineIdOffset <= 0) // This is not observed to be 0/empty.
                throw new RuntimeException("Cannot save cameraSplineIds, the pointer " + NumberUtils.toHexString(this.tempCameraSplineIdOffset) + " is invalid.");

            writer.writeAddressTo(this.tempCameraSplineIdOffset);
            this.tempCameraSplineIdOffset = -1;
            for (int i = 0; i < this.cameraSplines.size(); i++)
                writer.writeUnsignedByte((short) this.cameraSplines.get(i).getId());
        }

        private void resolveCameraSplines() {
            if (this.tempCameraSplineIds == null || this.tempCameraSplineIdOffset != -1)
                throw new RuntimeException("Cannot resolve camera splines, the data was not setup properly.");

            this.cameraSplines.clear();
            List<MediEvilMap2DSpline> cameraSplines = this.pathChainPacket.getParentFile().getSpline2DPacket().getSplines();
            for (int i = 0; i < this.tempCameraSplineIds.length; i++) {
                int cameraSplineIndex = (this.tempCameraSplineIds[i] & 0xFF);
                if (cameraSplineIndex >= cameraSplines.size())
                    throw new IllegalArgumentException("Invalid cameraSplineIndex: " + cameraSplineIndex);

                this.cameraSplines.add(cameraSplines.get(cameraSplineIndex));
            }

            this.tempCameraSplineIds = null;
        }
    }
}
