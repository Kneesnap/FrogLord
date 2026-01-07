package net.highwayfrogs.editor.games.sony.shared.mof2.mesh;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData.SCSharedGameData;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBox;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the "MR_PARTCEL" struct.
 * Created by Kneesnap on 8/25/2018.
 */
public class MRMofPartCel extends SCSharedGameData {
    @Getter private transient final MRMofPart parent;
    @Getter private final List<SVector> vertices = new ArrayList<>();
    @Getter private final List<SVector> normals = new ArrayList<>(); // This is the same on all partcels, ie it always matches the static partcel.
    @Getter @Setter private MRMofBoundingBox boundingBox; // This is used for collision purposes, unless it's an animated MOF, where it will use the model bounding box instead.

    @Getter(AccessLevel.PACKAGE) private transient int vertexPointer = -1;
    private transient int normalPointer = -1;
    private transient int boundingBoxPointer = -1;

    public static final int SIZE_IN_BYTES = 4 * Constants.INTEGER_SIZE;

    public MRMofPartCel(MRMofPart parent) {
        super(parent.getGameInstance());
        this.parent = parent;
    }

    /**
     * Gets the ID of this partCel.
     */
    public int getPartCelID() {
        return Utils.getLoadingIndex(this.parent.getPartCels(), this);
    }

    @Override
    public ILogger getLogger() {
        return new AppendInfoLoggerWrapper(this.parent.getLogger(), "partCel=" + getPartCelID(), AppendInfoLoggerWrapper.TEMPLATE_COMMA_SEPARATOR);
    }

    @Override
    public void load(DataReader reader) {
        this.vertexPointer = reader.readInt();
        this.normalPointer = reader.readInt();
        this.boundingBoxPointer = reader.readInt();
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // Unused.
    }

    @Override
    public void save(DataWriter writer) {
        boolean vertexPointerSet = (this.vertexPointer >= 0);
        boolean normalPointerSet = (this.normalPointer >= 0);
        if (!vertexPointerSet || !normalPointerSet)
            throw new RuntimeException("Vertex/Normal vector buffers have not been saved yet, so the MRMofPartCel header cannot be saved.");

        writer.writeInt(this.vertexPointer);
        writer.writeInt(this.normalPointer);
        this.boundingBoxPointer = writer.writeNullPointer();
        writer.writeNull(Constants.INTEGER_SIZE);

        // Clear data.
        this.vertexPointer = -1;
        this.normalPointer = -1;
    }

    /**
     * Read vertex data from the vector buffer.
     * @param vertexCount The number of vertices to read.
     * @param context context shared when loading all mof parts, used to share partCel data across different mof parts.
     */
    void loadVertices(int vertexCount, MRStaticMofDataContext context) {
        if (this.vertexPointer < 0)
            throw new RuntimeException("Cannot loadVertices for MrMofPartCel when the partCel header has not been read yet.");

        int vertexPointer = this.vertexPointer;
        this.vertexPointer = -1;

        this.vertices.clear();
        context.getPartCelVectors().copyElementsFromBuffer(this.vertices, vertexPointer, vertexCount);
    }

    /**
     * Writes vertex data to the writer.
     * @param writer The writer to save the vertex data to.
     * @param context context shared when saving all mof parts, used to share partCel data across different mof parts.
     */
    void saveVertices(DataWriter writer, MRStaticMofDataContext context) {
        if (this.vertexPointer >= 0)
            throw new RuntimeException("Cannot saveVertices for MrMofPartCel when the partCel header already has a vertexPointer set.");

        this.vertexPointer = context.getPartCelVectors().saveElementBuffer(writer, this.vertices);
    }

    /**
     * Read normal data from the vector buffer.
     * @param normalCount The number of normals to read.
     * @param context context shared when loading all mof parts, used to share partCel data across different mof parts.
     */
    void loadNormals(int normalCount, MRStaticMofDataContext context) {
        if (this.normalPointer < 0)
            throw new RuntimeException("Cannot loadNormals for MrMofPartCel when the partCel header has not been read yet.");

        int normalPointer = this.normalPointer;
        this.normalPointer = -1;

        this.normals.clear();
        context.getPartCelVectors().copyElementsFromBuffer(this.normals, normalPointer, normalCount);
    }

    /**
     * Writes normal data to the writer.
     * @param writer The writer to save the normal data to.
     * @param context context shared when saving all mof parts, used to share partCel data across different mof parts.
     */
    void saveNormals(DataWriter writer, MRStaticMofDataContext context) {
        if (this.normalPointer >= 0)
            throw new RuntimeException("Cannot saveNormals for MrMofPartCel when the partCel header already has a normalPointer set.");

        this.normalPointer = context.getPartCelVectors().saveElementBuffer(writer, this.normals);
    }

    /**
     * Read bounding box data from the reader.
     * @param reader The reader to read the bounding box from.
     * @param previousBoundingBoxes A registry of previously used bounding box pointers
     */
    void loadBoundingBox(DataReader reader, Map<Integer, MRMofBoundingBox> previousBoundingBoxes) {
        if (this.boundingBoxPointer < 0) {
            throw new RuntimeException("Cannot loadBoundingBox for MrMofPartCel when the partCel header has not been read yet.");
        } else if (this.boundingBoxPointer == 0) {
            // There's no bounding box!
            this.boundingBox = null;
            this.boundingBoxPointer = -1;
            return;
        }

        int boundingBoxPointer = this.boundingBoxPointer;
        this.boundingBoxPointer = -1;

        // Get cached bounding box, or read it if it's new.
        this.boundingBox = previousBoundingBoxes != null ? previousBoundingBoxes.get(boundingBoxPointer) : null;
        if (this.boundingBox == null) {
            requireReaderIndex(reader, boundingBoxPointer, "Expected boundingBoxPointer");
            this.boundingBox = new MRMofBoundingBox();
            if (previousBoundingBoxes != null)
                previousBoundingBoxes.put(reader.getIndex(), this.boundingBox);

            this.boundingBox.load(reader);
        }
    }

    /**
     * Writes the bounding box (if there is one) to the writer.
     * @param writer The writer to save the bounding box to.
     * @param previousBoundingBoxes A registry of bounding boxes previously written (reusable pointers)
     */
    void saveBoundingBox(DataWriter writer, Map<MRMofBoundingBox, Integer> previousBoundingBoxes, boolean incompleteMof) {
        // Write the bounding box, or find where it has already been written.
        Integer boundingBoxWriteIndex = previousBoundingBoxes != null ? previousBoundingBoxes.get(this.boundingBox) : null;
        if (this.boundingBox != null && boundingBoxWriteIndex == null) {
            boundingBoxWriteIndex = writer.getIndex();
            if (previousBoundingBoxes != null)
                previousBoundingBoxes.put(this.boundingBox, boundingBoxWriteIndex);

            this.boundingBox.save(writer);
        } else if (this.boundingBox == null) {
            boundingBoxWriteIndex = 0;
        }

        if (this.boundingBoxPointer > 0) { // Ready to write to the header.
            // Write to the header.
            if (!incompleteMof)
                writer.writeIntAtPos(this.boundingBoxPointer, boundingBoxWriteIndex);
            this.boundingBoxPointer = -1;
        } else { // Not ready to write to the header.
            // Prepare the value for the header when it gets written.
            this.boundingBoxPointer = boundingBoxWriteIndex;
        }
    }
}
