package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCChunkedFile.SCFilePacket.PacketSizeType;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a Sony Cambridge game file which is chunked with 4-byte magic headers.
 * Created by Kneesnap on 12/8/2023.
 */
public abstract class SCChunkedFile<TGameInstance extends SCGameInstance> extends SCGameFile<TGameInstance> {
    @Getter private final List<SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance>> filePackets = new ArrayList<>();
    private final Map<String, SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance>> filePacketsByIdentifierString = new HashMap<>();
    private final Map<Integer, SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance>> filePacketsByIdentifierInteger = new HashMap<>();
    @Getter private final boolean enforcePacketOrder;
    @Getter private boolean dataBeenLoaded;

    public SCChunkedFile(TGameInstance instance, boolean enforcePacketOrder) {
        super(instance);
        this.enforcePacketOrder = enforcePacketOrder;
    }

    /**
     * Register a new file packet to the file.
     * @param packet The packet to register.
     */
    public void addFilePacket(SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> packet) {
        if (this.dataBeenLoaded)
            throw new RuntimeException("New file packets cannot be added after data has been loaded.");

        if (packet == null)
            return;

        SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> existingPacket = getFilePacketByIdentifier(packet.getIdentifierString());
        if (existingPacket != null)
            throw new RuntimeException("There was already a packet with the identifier '" + packet.getIdentifierString() + "' registered to this " + Utils.getSimpleName(this) + ".");

        this.filePacketsByIdentifierString.put(packet.getIdentifierString(), packet);
        this.filePacketsByIdentifierInteger.put(packet.getIdentiferInteger(), packet);
        this.filePackets.add(packet);
    }

    /**
     * Get a file packet by its identifier.
     * @param identifier The identifier to lookup a packet by.
     * @return filePacket, may be null
     */
    public SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> getFilePacketByIdentifier(int identifier) {
        return this.filePacketsByIdentifierInteger.get(identifier);
    }

    /**
     * Get a file packet by its identifier.
     * @param identifier The identifier to lookup a packet by.
     * @return filePacket, may be null
     */
    public SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> getFilePacketByIdentifier(String identifier) {
        return this.filePacketsByIdentifierString.get(identifier);
    }

    /**
     * Get the number of active packets.
     */
    public int getActivePacketCount() {
        int count = 0;
        for (int i = 0; i < this.filePackets.size(); i++) {
            SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> packet = this.filePackets.get(i);
            if (packet.isActive())
                count++;
        }

        return count;
    }

    @Override
    public void load(DataReader reader) {
        // Clear packet read data.
        for (int i = 0; i < this.filePackets.size(); i++)
            this.filePackets.get(i).clearReadWriteData();

        // Read file packet data.
        int packetIndex = 0;
        SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> lastPacket = null;
        while (reader.hasMore() && (!this.enforcePacketOrder || this.filePackets.size() > packetIndex)) {
            int packetReadStartIndex = reader.getIndex();
            int identifier = reader.readInt();

            // Find packet or end.
            int startingPacketIndex = packetIndex;
            SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> packet;

            if (this.enforcePacketOrder) {
                // Get packets in order.
                packet = this.filePackets.get(packetIndex++);
                while (packet.getIdentiferInteger() != identifier && this.filePackets.size() > packetIndex && !packet.isRequired())
                    packet = this.filePackets.get(packetIndex++);
            } else {
                // Get any packet.
                packet = getFilePacketByIdentifier(identifier);
            }

            // If we've found the packet, read it. Otherwise, print we don't recognize it.
            if (packet != null && packet.getIdentiferInteger() == identifier) {
                reader.setIndex(packetReadStartIndex); // Move back so the packet can verify the identifier.
                packet.load(reader);
                lastPacket = packet;
            } else {
                if (this.enforcePacketOrder)
                    packetIndex = startingPacketIndex; // For the next one, let's resume at the same spot.

                // Attempt to skip the section.
                int size = reader.readInt();
                PacketSizeType sizeType;
                if (size >= 0 && (size % 4) == 0 && size <= reader.getRemaining() && (sizeType = getPacketSizeForUnknownChunk(Utils.toMagicString(identifier))) != PacketSizeType.NO_SIZE) {
                    getLogger().warning("Skipping unsupported packet '" + Utils.toMagicString(identifier) + "' (" + size + " bytes).");
                    if (sizeType == PacketSizeType.SIZE_INCLUSIVE) {
                        reader.skipBytes(size - Constants.INTEGER_SIZE * 2);
                    } else {
                        reader.skipBytes(size);
                    }
                } else {
                    SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> nextPacket = lastPacket != null ? lastPacket.findNextPacketWithKnownAddress() : null;

                    if (nextPacket != null) {
                        getLogger().warning("The packet '" + Utils.toMagicString(identifier) + "' could not be identified at " + Utils.toHexString(packetReadStartIndex) + " (did the previous packet finish reading at the right spot?). Attempting to continue from '" + nextPacket.getIdentifierString() + "'.");
                        reader.setIndex(nextPacket.getKnownStartAddress());
                        lastPacket = nextPacket;
                    } else {
                        getLogger().warning("The packet '" + Utils.toMagicString(identifier) + "' could not be identified at " + Utils.toHexString(packetReadStartIndex) + " (did the previous packet finish reading at the right spot?), and due to the questionable byte size (" + size + "), further reading has terminated.");
                        reader.skipBytes(reader.getRemaining());
                    }
                }
            }
        }

        this.dataBeenLoaded = true;

        // Warn if there are missing bytes.
        if (reader.hasMore())
            getLogger().warning("Chunked file reading finished, but it still has " + reader.getRemaining() + " unprocessed bytes.");
    }

    /**
     * Get the type of size for an unknown chunk.
     * @param identifier The identifier to get the size type of.
     * @return packetSizeType
     */
    protected abstract PacketSizeType getPacketSizeForUnknownChunk(String identifier);

    @Override
    public void save(DataWriter writer) {
        // Clear packet read data.
        for (int i = 0; i < this.filePackets.size(); i++)
            this.filePackets.get(i).clearReadWriteData();

        // Write file packet data.
        int startPosition = writer.getIndex();
        for (int i = 0; i < this.filePackets.size(); i++) {
            SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> packet = this.filePackets.get(i);
            if (packet.isActive())
                packet.save(writer);
        }
        int endPosition = writer.getIndex();

        // Write second pass data.
        for (int i = 0; i < this.filePackets.size(); i++) {
            SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> packet = this.filePackets.get(i);
            if (packet.isActive()) {
                writer.jumpTemp(packet.getLastValidWriteBodyAddress());
                packet.saveBodySecondPass(writer, endPosition - startPosition);
                writer.jumpReturn();
            }
        }
    }

    @Override
    public List<Tuple2<String, Object>> createPropertyList() {
        List<Tuple2<String, Object>> list = super.createPropertyList();
        list.add(new Tuple2<>("Active Packet Count", getActivePacketCount()));
        return list;
    }

    /**
     * Represents a packet of data within this file.
     * Data used at the start of a packet which is used by the packet system (behavior shared between different packets) is considered header data.
     * Data used by individual packets which is not shared by the system is considered body data.
     * @param <TFile>         The type of chunked file.
     * @param <TGameInstance> The type of game instance.
     */
    @Getter
    public static abstract class SCFilePacket<TFile extends SCChunkedFile<TGameInstance>, TGameInstance extends SCGameInstance> {
        private final TFile parentFile;
        private final String identifierString;
        private final int identiferInteger;
        private final boolean required;
        private final PacketSizeType sizeType;
        private int lastValidReadHeaderAddress = -1;
        private int lastValidReadBodyAddress = -1;
        private int lastValidReadSize = -1;
        private int lastValidWriteHeaderAddress = -1;
        private int lastValidWriteBodyAddress = -1;
        private int lastValidWriteSize = -1;
        private Logger cachedLogger;

        public SCFilePacket(TFile parentFile, String identifier, boolean required, PacketSizeType sizeType) {
            this.parentFile = parentFile;
            this.identifierString = identifier;
            this.identiferInteger = Utils.makeIdentifier(identifier);
            this.required = required;
            this.sizeType = sizeType;
        }

        /**
         * Gets the logger used for this chunked file packet.
         */
        public Logger getLogger() {
            if (this.cachedLogger != null)
                return this.cachedLogger;

            return this.cachedLogger = Logger.getLogger(this.parentFile.getFileDisplayName() + "|" + this.identifierString);
        }

        /**
         * Clear the last valid read & write data.
         */
        public void clearReadWriteData() {
            this.lastValidReadHeaderAddress = -1;
            this.lastValidReadBodyAddress = -1;
            this.lastValidReadSize = -1;
            this.lastValidWriteHeaderAddress = -1;
            this.lastValidWriteBodyAddress = -1;
            this.lastValidWriteSize = -1;
        }

        /**
         * Test if this packet has a size.
         */
        public boolean hasSize() {
            return this.sizeType == PacketSizeType.SIZE_INCLUSIVE || this.sizeType == PacketSizeType.SIZE_EXCLUSIVE;
        }

        /**
         * Test if this packet is active.
         * If this packet is active, it ensures it will be saved.
         * Required packets are always saved.
         */
        public boolean isActive() {
            return isRequired();
        }

        /**
         * Reads packet data.
         * @param reader The reader to read data from.
         */
        public void load(DataReader reader) {
            this.lastValidReadHeaderAddress = reader.getIndex();
            reader.verifyString(this.identifierString);

            int knownStartAddress = getKnownStartAddress();
            if (knownStartAddress >= 0 && knownStartAddress != this.lastValidReadHeaderAddress)
                throw new RuntimeException(Utils.getSimpleName(this) + ".getKnownStartAddress() returned " + Utils.toHexString(knownStartAddress) + ", but the address reading actually started from was " + Utils.toHexString(this.lastValidReadHeaderAddress) + ".");

            // Read the data size, if size is set.
            if (hasSize()) {
                this.lastValidReadSize = reader.readInt();
            } else {
                this.lastValidReadSize = -1;
            }

            // Prepare to read data body.
            this.lastValidReadBodyAddress = reader.getIndex();

            // Verify packet read the correct amount of data.
            int expectedEndPosition = -1;
            if (this.sizeType == PacketSizeType.SIZE_EXCLUSIVE) {
                expectedEndPosition = this.lastValidReadBodyAddress + this.lastValidReadSize;
            } else if (this.sizeType == PacketSizeType.SIZE_INCLUSIVE) {
                expectedEndPosition = this.lastValidReadHeaderAddress + this.lastValidReadSize;
            }

            // Read data body.
            boolean threwError = false;
            try {
                this.loadBody(reader, expectedEndPosition);
            } catch (Throwable th) {
                threwError = true;
                getLogger().warning("An error occurred while reading the '" + getIdentifierString() + "' packet data.");
                th.printStackTrace();
            }

            // Automatically align to the next section.
            reader.align(4);

            if (expectedEndPosition >= 0 && expectedEndPosition != reader.getIndex()) {
                getLogger().warning("Didn't end at the right position for '" + getIdentifierString() + "'. (Expected: " + Utils.toHexString(expectedEndPosition) + ", Actual: " + Utils.toHexString(reader.getIndex()) + ")");
                reader.setIndex(expectedEndPosition);
            } else if (threwError) {
                // Find the next packet to resume reading from.
                SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> bestNextPacket = findNextPacketWithKnownAddress();

                if (bestNextPacket != null) {
                    getLogger().warning("Failed while reading packet '" + getIdentifierString() + "' at " + Utils.toHexString(this.lastValidReadHeaderAddress) + ". Attempting to continue from '" + bestNextPacket.getIdentifierString() + "'.");
                    reader.setIndex(bestNextPacket.getKnownStartAddress());
                } else {
                    getLogger().warning("Failed in an unrecoverable manner when reading '" + getIdentifierString() + "' at " + Utils.toHexString(this.lastValidReadHeaderAddress) + ".");
                    reader.skipBytes(reader.getRemaining());
                }
            }
        }

        /**
         * Find the next registered packet with a known address.
         * Helpful for error recovery when the size of a chunk is not known.
         */
        public SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> findNextPacketWithKnownAddress() {
            for (int i = this.parentFile.getFilePackets().indexOf(this) + 1; i < this.parentFile.getFilePackets().size(); i++) {
                SCFilePacket<? extends SCChunkedFile<TGameInstance>, TGameInstance> tempPacket = this.parentFile.getFilePackets().get(i);
                if (tempPacket.getKnownStartAddress() >= 0)
                    return tempPacket;
            }

            return null;
        }

        /**
         * Reads the packet body data from the reader.
         * @param reader   The reader to read body data from.
         * @param endIndex The reader index which indicates the end of the data. This value is -1 if the data end position is unknown.
         */
        protected abstract void loadBody(DataReader reader, int endIndex);

        /**
         * Writes this packet to the writer.
         * @param writer The writer to write data to.
         */
        public void save(DataWriter writer) {
            this.lastValidWriteHeaderAddress = writer.getIndex();
            writer.writeInt(this.identiferInteger);
            int sizeAddress = hasSize() ? writer.writeNullPointer() : -1;

            // Write body data.
            this.lastValidWriteBodyAddress = writer.getIndex();
            try {
                this.saveBodyFirstPass(writer);
            } catch (Throwable th) {
                String errorMessage = "An error occurred while saving the '" + getIdentifierString() + "' packet data.";
                getLogger().throwing("SCChunkedFile", "save", new RuntimeException(errorMessage));
                throw new RuntimeException(th);
            }

            // Automatically align to the next section.
            writer.align(4);

            // Write size, if there is one.
            int sizeValue = -1;
            if (this.sizeType == PacketSizeType.SIZE_EXCLUSIVE) {
                sizeValue = writer.getIndex() - this.lastValidReadBodyAddress;
            } else if (this.sizeType == PacketSizeType.SIZE_INCLUSIVE) {
                sizeValue = writer.getIndex() - this.lastValidReadHeaderAddress;
            }

            if (sizeValue >= 0 && hasSize()) {
                writer.jumpTemp(sizeAddress);
                writer.writeInt(sizeValue);
                writer.jumpReturn();
            }
        }

        /**
         * Writes the core body data to the writer.
         * @param writer The writer to write body data to.
         */
        protected abstract void saveBodyFirstPass(DataWriter writer);

        /**
         * Writes the secondary body data to the writer.
         * This is for data which is unknown until after the first pass completes.
         * @param writer          The writer to write body data to.
         * @param fileSizeInBytes The size of the file after the first pass.
         */
        protected void saveBodySecondPass(DataWriter writer, long fileSizeInBytes) {
            // By default, do nothing.
        }

        /**
         * Gets the address the chunk is known to start at. Negative values indicate that the start is not currently known.
         * This function is only defined for reading data, and is unlikely to provide the right data in the write step.
         * @return knownStartAddress
         */
        public int getKnownStartAddress() {
            return -1;
        }

        public enum PacketSizeType {
            NO_SIZE,
            SIZE_INCLUSIVE, // Includes the header size.
            SIZE_EXCLUSIVE, // Does not include the header size.
        }
    }

    /**
     * Represents a file packet which is unimplemented, and just reads/stores the packet in full.
     */
    @Getter
    public static class DummyFilePacket<TFile extends SCChunkedFile<TGameInstance>, TGameInstance extends SCGameInstance> extends SCFilePacket<TFile, TGameInstance> {
        private byte[] rawData;

        public DummyFilePacket(TFile parentFile, String identifier, boolean required, PacketSizeType sizeType) {
            super(parentFile, identifier, required, sizeType);
        }

        @Override
        protected void loadBody(DataReader reader, int endIndex) {
            this.rawData = reader.readBytes(endIndex - reader.getIndex());
        }

        @Override
        protected void saveBodyFirstPass(DataWriter writer) {
            if (this.rawData != null)
                writer.writeBytes(this.rawData);
        }
    }
}