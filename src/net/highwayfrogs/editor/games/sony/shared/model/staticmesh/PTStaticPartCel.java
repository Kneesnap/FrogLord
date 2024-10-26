package net.highwayfrogs.editor.games.sony.shared.model.staticmesh;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.collprim.PTCollprim;
import net.highwayfrogs.editor.games.sony.shared.model.PTModel;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPrimitiveBlock;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPrimitiveControl;
import net.highwayfrogs.editor.games.sony.shared.model.primitive.PTPrimitiveType;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents an animation partcel
 * Created by Kneesnap on 5/16/2024.
 */
public class PTStaticPartCel extends SCSharedGameData {
    @Getter private final PTStaticPart parentPart;
    @Getter private int flags;
    @Getter private int skinVectorCount; // Number of skin vectors (Both vertices + normals)
    @Getter private int skinVertexCount; // Number of skin vertices (skinVectorCount - skinNormalCount)
    @Getter private final List<SVector> mimeVectors = new ArrayList<>();
    @Getter private final List<SVector> vectors = new ArrayList<>();
    @Getter private final List<PTPrimitiveBlock> primitiveBlocks = new ArrayList<>();
    @Getter private final List<PTModelCollprim> collprims = new ArrayList<>();
    private transient int mimeVectorStartAddress = -1;
    private transient int vectorStartAddress = -1;
    private transient int primitiveBlockStartAddress = -1;
    private transient int collprimListAddress = -1;

    public static final int FLAG_CONTAINS_POLYGONS = Constants.BIT_FLAG_0; // Indicates if there is at least one polygon to draw here. TODO: Auto-calculate.
    public static final int FLAG_ATTACHED_PRIMITIVES = Constants.BIT_FLAG_2;
    public static final int FLAG_VALIDATION_MASK = 0b101;

    public PTStaticPartCel(PTStaticPart parentPart) {
        super(parentPart != null ? parentPart.getGameInstance() : null);
        this.parentPart = parentPart;
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readInt();
        warnAboutInvalidBitFlags(this.flags, FLAG_VALIDATION_MASK);
        this.skinVectorCount = reader.readUnsignedShortAsInt();
        this.skinVertexCount = reader.readUnsignedShortAsInt();
        int primitiveBlockCount = reader.readUnsignedShortAsInt();
        int collprimCount = reader.readUnsignedShortAsInt();

        this.mimeVectorStartAddress = reader.readInt();
        this.vectorStartAddress = reader.readInt();
        this.primitiveBlockStartAddress = reader.readInt();
        this.collprimListAddress = reader.readInt();

        // Setup polygon blocks. (Seems to be after the partcel list)
        this.primitiveBlocks.clear();
        for (int i = 0; i < primitiveBlockCount; i++)
            this.primitiveBlocks.add(new PTPrimitiveBlock(getGameInstance()));

        // Setup collprims. (After blocks? Unclear?)
        this.collprims.clear();
        for (int i = 0; i < collprimCount; i++)
            this.collprims.add(new PTModelCollprim(getGameInstance()));
    }

    /**
     * Reads the primitive blocks from the current position.
     * @param reader the reader to read it from
     */
    void readPrimitiveBlocks(DataReader reader) {
        if (this.primitiveBlockStartAddress <= 0)
            throw new RuntimeException("Cannot primitive block list, the pointer " + NumberUtils.toHexString(this.primitiveBlockStartAddress) + " is invalid.");

        reader.requireIndex(getLogger(), this.primitiveBlockStartAddress, "Expected PTPrimitiveBlock list");
        for (int i = 0; i < this.primitiveBlocks.size(); i++)
            this.primitiveBlocks.get(i).load(reader);

        this.primitiveBlockStartAddress = -1;
    }

    /**
     * Reads the collprims from the current position.
     * @param reader the reader to read it from
     */
    void readCollprims(DataReader reader) {
        if (this.collprimListAddress == 0 && this.collprims.isEmpty())
            return; // No collprims available to read.
        if (this.collprimListAddress <= 0)
            throw new RuntimeException("Cannot read collprim list, the pointer " + NumberUtils.toHexString(this.collprimListAddress) + " is invalid.");

        reader.requireIndex(getLogger(), this.collprimListAddress, "Expected PTCollprim list");
        for (int i = 0; i < this.collprims.size(); i++) {
            PTCollprim collprim = this.collprims.get(i);
            collprim.load(reader);
            collprim.applyRadiusToLength();
        }

        this.collprimListAddress = -1;
    }

    /**
     * Reads the collprim matrices from the current position.
     * @param reader the reader to read it from
     */
    void readCollprimMatrices(DataReader reader) {
        for (int i = 0; i < this.collprims.size(); i++)
            this.collprims.get(i).readMatrix(reader);
    }

    /**
     * Reads the mime vectors from the current position.
     * @param reader the reader to read it from
     */
    void readMimeVectors(DataReader reader) {
        if (this.mimeVectorStartAddress <= 0)
            throw new RuntimeException("Cannot read mime vector list, the pointer " + NumberUtils.toHexString(this.mimeVectorStartAddress) + " is invalid.");

        this.mimeVectors.clear();
        reader.requireIndex(getLogger(), this.mimeVectorStartAddress, "Expected mime vector list");
        if ((this.parentPart.getFlags() & PTStaticPart.FLAG_MIME_ENABLED) == PTStaticPart.FLAG_MIME_ENABLED)
            for (int i = 0; i < this.parentPart.getMimeVectors(); i++)
                this.mimeVectors.add(SVector.readWithPadding(reader));

        this.mimeVectorStartAddress = -1;
    }

    /**
     * Reads the vectors from the current position.
     * @param reader the reader to read it from
     */
    void readVectors(DataReader reader) {
        if (this.vectorStartAddress <= 0)
            throw new RuntimeException("Cannot read vector list, the pointer " + NumberUtils.toHexString(this.vectorStartAddress) + " is invalid.");

        // Calculate end index.
        int endIndex = reader.getSize(); // Fallback -> read to end of file.
        int startPartIndex = this.parentPart.getPartIndex();
        if (startPartIndex >= 0) {
            for (int partIndex = startPartIndex; partIndex < this.parentPart.getStaticFile().getParts().size(); partIndex++) {
                PTStaticPart part = this.parentPart.getStaticFile().getParts().get(partIndex);
                if (part.getPartCels().isEmpty())
                    continue;

                boolean foundIndex = false;
                for (int j = (partIndex == startPartIndex) ? getPartCelIndex() + 1 : 0; j < part.getPartCels().size(); j++) {
                    PTStaticPartCel nextPartCel = part.getPartCels().get(j);
                    if (nextPartCel.vectorStartAddress > 0) {
                        endIndex = nextPartCel.vectorStartAddress;
                        foundIndex = true;
                        break;
                    }
                }

                if (foundIndex)
                    break;
            }
        }

        // Read vectors.
        this.vectors.clear();
        reader.requireIndex(getLogger(), this.vectorStartAddress, "Expected vector list");
        while (endIndex > reader.getIndex())
            this.vectors.add(SVector.readWithPadding(reader));

        this.vectorStartAddress = -1;
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.flags);
        writer.writeUnsignedShort(this.skinVectorCount);
        writer.writeUnsignedShort(this.skinVertexCount);
        writer.writeUnsignedShort(this.primitiveBlocks.size());
        writer.writeUnsignedShort(this.collprims.size());
        this.mimeVectorStartAddress = writer.writeNullPointer();
        this.vectorStartAddress = writer.writeNullPointer();
        this.primitiveBlockStartAddress = writer.writeNullPointer();
        this.collprimListAddress = writer.writeNullPointer();
    }

    /**
     * Writes the polygon blocks to the current position.
     * @param writer the writer to write it to
     */
    void writePrimitiveBlocks(DataWriter writer) {
        if (this.primitiveBlockStartAddress <= 0)
            throw new RuntimeException("Cannot write primitive block list, the pointer " + NumberUtils.toHexString(this.primitiveBlockStartAddress) + " is invalid.");

        writer.writeAddressTo(this.primitiveBlockStartAddress);
        for (int i = 0; i < this.primitiveBlocks.size(); i++)
            this.primitiveBlocks.get(i).save(writer);

        this.primitiveBlockStartAddress = -1;
    }

    /**
     * Writes the collprims to the current position.
     * @param writer the writer to write it to
     */
    void writeCollprims(DataWriter writer) {
        if (this.collprimListAddress <= 0)
            throw new RuntimeException("Cannot write collprim list, the pointer " + NumberUtils.toHexString(this.collprimListAddress) + " is invalid.");

        writer.writeAddressTo(this.collprimListAddress);
        for (int i = 0; i < this.collprims.size(); i++)
            this.collprims.get(i).save(writer);

        this.collprimListAddress = -1;
    }

    /**
     * Writes the collprim matrices to the current position.
     * @param writer the writer to write it to
     */
    void writeCollprimMatrices(DataWriter writer) {
        for (int i = 0; i < this.collprims.size(); i++)
            this.collprims.get(i).writeMatrix(writer);
    }

    /**
     * Writes the mime vectors to the current position.
     * @param writer the writer to write it to
     */
    void writeMimeVectors(DataWriter writer) {
        if (this.mimeVectorStartAddress <= 0)
            throw new RuntimeException("Cannot write mime vector list, the pointer " + NumberUtils.toHexString(this.mimeVectorStartAddress) + " is invalid.");

        writer.writeAddressTo(this.mimeVectorStartAddress);
        if ((this.parentPart.getFlags() & PTStaticPart.FLAG_MIME_ENABLED) == PTStaticPart.FLAG_MIME_ENABLED)
            for (int i = 0; i < this.mimeVectors.size(); i++)
                this.mimeVectors.get(i).saveWithPadding(writer);

        this.mimeVectorStartAddress = -1;
    }

    /**
     * Writes the vectors to the current position.
     * @param writer the writer to write it to
     */
    void writeVectors(DataWriter writer) {
        if (this.vectorStartAddress <= 0)
            throw new RuntimeException("Cannot write vector list, the pointer " + NumberUtils.toHexString(this.vectorStartAddress) + " is invalid.");

        writer.writeAddressTo(this.vectorStartAddress);
        for (int i = 0; i < this.vectors.size(); i++)
            this.vectors.get(i).saveWithPadding(writer);

        this.vectorStartAddress = -1;
    }

    /**
     * Gets the index of the part cel.
     * @return partCelIndex
     */
    public int getPartCelIndex() {
        return this.parentPart != null ? this.parentPart.getPartCels().indexOf(this) : -1;
    }

    /**
     * Gets information used to identify the logger.
     */
    public String getLoggerInfo() {
        return (this.parentPart != null ? this.parentPart.getLoggerInfo() + ",PartCel=" + this.parentPart.getPartCels().indexOf(this) : Utils.getSimpleName(this));
    }

    @Override
    public Logger getLogger() {
        return Logger.getLogger(getLoggerInfo());
    }

    /**
     * Gets the vertex position for the vertex ID.
     * @param vertexId the vertex ID to resolve.
     * @return vertex
     */
    public SVector getVertex(PTModel model, int vertexId) {
        for (int i = 0; i < this.primitiveBlocks.size(); i++) {
            PTPrimitiveBlock primitiveBlock = this.primitiveBlocks.get(i);
            if (primitiveBlock.getPrimitiveType() != PTPrimitiveType.CONTROL)
                continue;

            for (int j = 0; j < primitiveBlock.getPrimitives().size(); j++) {
                PTPrimitiveControl control = (PTPrimitiveControl) primitiveBlock.getPrimitives().get(j);
                if (!control.getControlType().isPosition())
                    continue;

                if (vertexId >= control.getTargetTransformIndex() && vertexId < control.getTargetTransformIndex() + control.getVertexCount()) {
                    int localVertexId = vertexId - control.getTargetTransformIndex();
                    SVector vector = control.getVector(model, this, localVertexId);
                    if (vector != null)
                        return vector;
                }
            }
        }

        return SVector.EMPTY;
    }
}