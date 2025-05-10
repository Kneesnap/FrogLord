package net.highwayfrogs.editor.games.sony.shared.model.staticmesh;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a part, similar to MR_PART in a static model file.
 * Created by Kneesnap on 5/15/2024.
 */
public class PTStaticPart extends SCSharedGameData {
    @Getter private final PTStaticFile staticFile;
    @Getter private int flags;
    @Getter private short transformId;
    @Getter private short connectedTransformId;
    @Getter private int mimeVectors;
    @Getter private int mimeSkinVectors;
    @Getter private int mimeSkinVertices;
    @Getter private final List<PTStaticPartCel> partCels = new ArrayList<>();
    private transient int partCelStartAddress;

    public static final int FLAG_MIME_ENABLED = Constants.BIT_FLAG_0;
    public static final int FLAG_VALIDATION_MASK = FLAG_MIME_ENABLED;

    public PTStaticPart(PTStaticFile staticFile) {
        super(staticFile != null ? staticFile.getGameInstance() : null);
        this.staticFile = staticFile;
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
        int partCelCount = reader.readUnsignedShortAsInt();
        this.transformId = reader.readUnsignedByteAsShort();
        this.connectedTransformId = reader.readUnsignedByteAsShort();
        this.mimeVectors = reader.readUnsignedShortAsInt();
        this.mimeSkinVectors = reader.readUnsignedShortAsInt();
        this.mimeSkinVertices = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(Constants.SHORT_SIZE); // Padding.
        this.partCelStartAddress = reader.readInt();

        // Setup partcels.
        this.partCels.clear();
        for (int i = 0; i < partCelCount; i++)
            this.partCels.add(new PTStaticPartCel(this));
    }

    /**
     * Reads the partCels from the current position.
     * @param reader the reader to read it from
     */
    void readPartCels(DataReader reader) {
        if (this.partCelStartAddress <= 0)
            throw new RuntimeException("Cannot partCel list, the pointer is invalid.");

        requireReaderIndex(reader, this.partCelStartAddress, "Expected PTStaticPartCel list");
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).load(reader);

        this.partCelStartAddress = -1;
    }

    /**
     * Reads the partCel primitive blocks from the current position.
     * @param reader the reader to read it from
     */
    void readPartCelPrimitiveBlocks(DataReader reader) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).readPrimitiveBlocks(reader);
    }

    /**
     * Reads the partCel collprims from the current position.
     * @param reader the reader to read it from
     */
    void readPartCelCollprims(DataReader reader) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).readCollprims(reader);
    }

    /**
     * Reads the partCel collprim matrices from the current position.
     * @param reader the reader to read it from
     */
    void readPartCelCollprimMatrices(DataReader reader) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).readCollprimMatrices(reader);
    }

    /**
     * Reads the partCel mime vectors from the current position.
     * @param reader the reader to read it from
     */
    void readPartCelMimeVectors(DataReader reader) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).readMimeVectors(reader);
    }

    /**
     * Reads the partCel vectors from the current position.
     * @param reader the reader to read it from
     */
    void readPartCelVectors(DataReader reader) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).readVectors(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);
        writer.writeUnsignedShort(this.partCels.size());
        writer.writeUnsignedByte(this.transformId);
        writer.writeUnsignedByte(this.connectedTransformId);
        writer.writeUnsignedShort(this.mimeVectors);
        writer.writeUnsignedShort(this.mimeSkinVectors);
        writer.writeUnsignedShort(this.mimeSkinVertices);
        writer.writeNull(Constants.SHORT_SIZE); // Padding.
        this.partCelStartAddress = writer.writeNullPointer();
    }

    /**
     * Writes the partCels to the current position.
     * @param writer the writer to write it to
     */
    void writePartCels(DataWriter writer) {
        if (this.partCelStartAddress <= 0)
            throw new RuntimeException("Cannot write partCel list, the pointer is invalid.");

        writer.writeAddressTo(this.partCelStartAddress);
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).save(writer);

        this.partCelStartAddress = -1;
    }

    /**
     * Writes the partCel primitive blocks to the current position.
     * @param writer the writer to write it to
     */
    void writePartCelPrimitiveBlocks(DataWriter writer) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).writePrimitiveBlocks(writer);
    }

    /**
     * Writes the partCel collprims to the current position.
     * @param writer the writer to write it to
     */
    void writePartCelCollprims(DataWriter writer) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).writeCollprims(writer);
    }

    /**
     * Writes the partCel collprim matrices to the current position.
     * @param writer the writer to write it to
     */
    void writePartCelCollprimMatrices(DataWriter writer) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).writeCollprimMatrices(writer);
    }

    /**
     * Writes the partCel mime vectors to the current position.
     * @param writer the writer to write it to
     */
    void writePartCelMimeVectors(DataWriter writer) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).writeMimeVectors(writer);
    }

    /**
     * Writes the partCel vectors to the current position.
     * @param writer the writer to write it to
     */
    void writePartCelVectors(DataWriter writer) {
        for (int i = 0; i < this.partCels.size(); i++)
            this.partCels.get(i).writeVectors(writer);
    }

    /**
     * Gets the index of the part.
     * @return partIndex
     */
    public int getPartIndex() {
        return this.staticFile != null ? this.staticFile.getParts().lastIndexOf(this) : -1;
    }

    /**
     * Gets information used to identify the logger.
     */
    public String getLoggerInfo() {
        return this.staticFile != null ? this.staticFile.getFileDisplayName() + "|Part=" + this.staticFile.getParts().indexOf(this) : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), PTStaticPart::getLoggerInfo, this);
    }
}