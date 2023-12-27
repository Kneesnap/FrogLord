package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.system.math.Vector2f;

/**
 * This represents a unit of vertex positions, texture coordinate values, and face values which are conceptually grouped together.
 * This could be as small as containing data for a single face, or as large as containing data for all the faces in a mesh.
 * It relies upon data for this group written together as a continuous chunk.
 * Created by Kneesnap on 9/25/2023.
 */
@Getter
public class DynamicMeshDataEntry {
    private final DynamicMesh mesh;
    private int vertexStartIndex = -1;
    private int vertexCount;
    private int texCoordStartIndex = -1;
    private int texCoordCount;
    private int faceStartIndex = -1;
    private int faceCount;

    private static final float[] TEMP_POSITION_ARRAY = new float[3];
    private static final float[] TEMP_TEXCOORD_ARRAY = new float[2];
    private static final int[] TEMP_FACE_ARRAY = new int[6];

    public DynamicMeshDataEntry(DynamicMesh mesh) {
        this(mesh, 0, 0, 0);
    }

    public DynamicMeshDataEntry(DynamicMesh mesh, int vertexCount, int texCoordCount, int faceCount) {
        if (mesh == null)
            throw new NullPointerException("mesh");

        this.mesh = mesh;
        this.vertexCount = vertexCount;
        this.texCoordCount = texCoordCount;
        this.faceCount = faceCount;
    }

    /**
     * Called when the entry is added to a node.
     */
    protected void onAddedToNode() {
        // All of these values are assigned either when the first value is written, or when this is added to the node.
        // This is to ensure we keep the order sorted between entries, allowing binary searches to be possible.
        // This allows for example, looking up a face by the ID which JavaFX reported getting clicked.
        if (this.vertexCount == 0)
            this.vertexStartIndex = this.mesh.getEditableVertices().size() / this.mesh.getPointElementSize();
        if (this.texCoordCount == 0)
            this.texCoordStartIndex = this.mesh.getEditableTexCoords().size() / this.mesh.getTexCoordElementSize();
        if (this.faceCount == 0)
            this.faceStartIndex = this.mesh.getEditableFaces().size() / this.mesh.getFaceElementSize();
    }

    /**
     * Called when the entry is added to a node.
     */
    protected void onRemovedFromNode() {
        // Remove Faces
        // Should run first, so any vertices/texCoords defined here used by this face won't trigger the detector for faces using data we're deleting.
        if (this.faceCount != 0) {
            this.mesh.getEditableFaces().startBulkRemovals();
            for (int i = 0; i < this.faceCount; i++)
                removeFace(i);

            // Done
            this.faceStartIndex = -1;
            this.mesh.getEditableFaces().endBulkRemovals();
        }

        // Remove Vertices
        if (this.vertexCount != 0) {
            this.mesh.getEditableVertices().startBulkRemovals();
            for (int i = 0; i < this.vertexCount; i++)
                removeVertexValue(i);

            // Done
            this.vertexStartIndex = -1;
            this.mesh.getEditableVertices().endBulkRemovals();
        }

        // Remove Tex Coords
        if (this.texCoordCount != 0) {
            this.mesh.getEditableTexCoords().startBulkRemovals();
            for (int i = 0; i < this.texCoordCount; i++)
                removeTexCoordValue(i);

            // Done
            this.texCoordStartIndex = -1;
            this.mesh.getEditableTexCoords().endBulkRemovals();
        }
    }

    /**
     * Adds a new vertex position.
     * TODO: Allow specifying the index to add this at.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return the index into the mesh vertex array which the vertex data starts.
     */
    public int addVertexValue(float x, float y, float z) {
        if (this.vertexCount == 0) {
            this.vertexStartIndex = this.mesh.getEditableVertices().size() / this.mesh.getPointElementSize();
        } else { // TODO: Toss this once proper array handling is added.
            int expectedVertexPos = (this.vertexStartIndex + this.vertexCount) * this.mesh.getPointElementSize();
            if (this.mesh.getEditableVertices().size() != expectedVertexPos)
                throw new RuntimeException("Adding vertex values out of order is not currently supported, but should be before this code is shipped to users."); // TODO
        }

        // Writes values to the array.
        TEMP_POSITION_ARRAY[0] = x;
        TEMP_POSITION_ARRAY[1] = y;
        TEMP_POSITION_ARRAY[2] = z;
        this.mesh.getEditableVertices().addAll(TEMP_POSITION_ARRAY);

        // Update Face References:
        // TODO: Update all the faces which reference a value that comes later to increment their index.
        // TODO: Perhaps we should bulk these changes.

        // Return position data was written to.
        return this.vertexStartIndex + (this.vertexCount++);
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param x             The x coordinate to write.
     */
    public void writeVertexX(int localVtxIndex, float x) {
        this.mesh.getEditableVertices().set(((this.vertexStartIndex + localVtxIndex) * this.mesh.getPointElementSize()), x);
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param y             The y coordinate to write.
     */
    public void writeVertexY(int localVtxIndex, float y) {
        this.mesh.getEditableVertices().set(((this.vertexStartIndex + localVtxIndex) * this.mesh.getPointElementSize()) + 1, y);
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param z             The z coordinate to write.
     */
    public void writeVertexZ(int localVtxIndex, float z) {
        this.mesh.getEditableVertices().set(((this.vertexStartIndex + localVtxIndex) * this.mesh.getPointElementSize()) + 2, z);
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param x             The x coordinate to write.
     * @param y             The y coordinate to write.
     * @param z             The z coordinate to write.
     */
    public void writeVertexXYZ(int localVtxIndex, float x, float y, float z) {
        TEMP_POSITION_ARRAY[0] = x;
        TEMP_POSITION_ARRAY[1] = y;
        TEMP_POSITION_ARRAY[2] = z;

        int vertexElementSize = this.mesh.getPointElementSize(); // 3
        int rawArrayStartIndex = (this.vertexStartIndex + localVtxIndex) * vertexElementSize;
        this.mesh.getEditableVertices().set(rawArrayStartIndex, TEMP_POSITION_ARRAY, 0, vertexElementSize);
    }

    /**
     * Removes a vertex position from the array.
     * @param localVtxIndex The local index of the position to remove.
     */
    public void removeVertexValue(int localVtxIndex) {
        if (localVtxIndex < 0 || localVtxIndex >= this.vertexCount)
            throw new IndexOutOfBoundsException("There is no vertex corresponding to local vertex ID " + localVtxIndex + ". Valid Range: [0, " + this.vertexCount + ").");

        int vertexElementSize = this.mesh.getPointElementSize(); // 3
        int vertexArrayStartIndex = (this.vertexStartIndex + localVtxIndex) * vertexElementSize;
        this.mesh.getEditableVertices().remove(vertexArrayStartIndex, vertexElementSize);
        // TODO: Update all the faces which reference a value that comes later to decrement index. For any face that uses this vertex, throw an exception.
        // TODO: Perhaps we should bulk these changes.
        this.vertexCount--;
    }

    /**
     * Adds a new texture coordinate value.
     * TODO: Allow specifying the index to add this at.
     * @param u The U (horizontal) texture coordinate.
     * @param v The V (vertical) texture coordinate.
     * @return the index into the mesh array which the tex coord data starts.
     */
    public int addTexCoordValue(float u, float v) {
        if (this.texCoordCount == 0) {
            this.texCoordStartIndex = this.mesh.getEditableTexCoords().size() / this.mesh.getTexCoordElementSize();
        } else { // TODO: Toss this once proper array handling is added.
            int expectedPos = (this.texCoordStartIndex + this.texCoordCount) * this.mesh.getTexCoordElementSize();
            if (this.mesh.getEditableTexCoords().size() != expectedPos)
                throw new RuntimeException("Adding tex coord values out of order is not currently supported, but should be before this code is shipped to users."); // TODO
        }

        // Write values to array.
        TEMP_TEXCOORD_ARRAY[0] = u;
        TEMP_TEXCOORD_ARRAY[1] = v;
        this.mesh.getEditableTexCoords().addAll(TEMP_TEXCOORD_ARRAY);

        // Update Face References:
        // TODO: Update all the faces which reference a value that comes later to increment their index (to account for the offset of this new one).
        // TODO: Perhaps we should bulk these changes.

        // Return position data was written to.
        return this.texCoordStartIndex + (this.texCoordCount++);
    }

    /**
     * Adds a new texture coordinate value.
     * TODO: Allow specifying the index to add this at.
     * @param uv The vector containing uv values.
     * @return the index into the mesh array which the tex coord data starts.
     */
    public int addTexCoordValue(Vector2f uv) {
        return addTexCoordValue(uv.getX(), uv.getY());
    }

    /**
     * Writes tex coord data to the tex coord array.
     * @param localTexCoordIndex The index to a tex coord pair saved here.
     * @param u                  The U (horizontal) texture coordinate to write.
     * @param v                  The V (vertical) texture coordinate to write.
     */
    public void writeTexCoordValue(int localTexCoordIndex, float u, float v) {
        TEMP_TEXCOORD_ARRAY[0] = u;
        TEMP_TEXCOORD_ARRAY[1] = v;

        int texCoordElementSize = this.mesh.getTexCoordElementSize(); // 2
        int rawArrayStartIndex = (this.texCoordStartIndex + localTexCoordIndex) * texCoordElementSize;
        this.mesh.getEditableTexCoords().set(rawArrayStartIndex, TEMP_TEXCOORD_ARRAY, 0, texCoordElementSize);
    }

    /**
     * Writes tex coord data to the tex coord array.
     * @param localTexCoordIndex The index to a tex coord pair saved here.
     * @param uv                 The vector containing uv values.
     */
    public void writeTexCoordValue(int localTexCoordIndex, Vector2f uv) {
        writeTexCoordValue(localTexCoordIndex, uv.getX(), uv.getY());
    }

    /**
     * Removes a tex coord from the array.
     * @param localTexCoordIndex The local index of the tex coord to remove.
     */
    public void removeTexCoordValue(int localTexCoordIndex) {
        if (localTexCoordIndex < 0 || localTexCoordIndex >= this.texCoordCount)
            throw new IndexOutOfBoundsException("There is no texCoord corresponding to local texCoord ID " + localTexCoordIndex + ". Range: [0, " + this.texCoordCount + ").");

        int texCoordElementSize = this.mesh.getTexCoordElementSize(); // 2
        int texCoordArrayStartIndex = (this.texCoordStartIndex + localTexCoordIndex) * texCoordElementSize;
        this.mesh.getEditableTexCoords().remove(texCoordArrayStartIndex, texCoordElementSize);
        // TODO: Update all the faces which reference a value that comes later to decrement the reference indices.
        // TODO: Perhaps we should bulk these changes.
        this.texCoordCount--;
    }

    /**
     * Adds a new face referencing existing
     * TODO: Allow specifying the index to add this at.
     * @param meshVertex1   The index to the position of the first vertex.
     * @param meshTexCoord1 The index to the position of the first texture coordinate.
     * @param meshVertex2   The index to the position of the second vertex.
     * @param meshTexCoord2 The index to the position of the second texture coordinate.
     * @param meshVertex3   The index to the position of the third vertex.
     * @param meshTexCoord3 The index to the position of the third texture coordinate.
     * @return the index into the mesh array which the face data starts.
     */
    public int addFace(int meshVertex1, int meshTexCoord1, int meshVertex2, int meshTexCoord2, int meshVertex3, int meshTexCoord3) {
        if (this.faceCount == 0) {
            this.faceStartIndex = this.mesh.getEditableFaces().size() / this.mesh.getFaceElementSize();
        } else { // TODO: Toss this once proper array handling is added.
            int expectedPos = (this.faceStartIndex + this.faceCount) * this.mesh.getFaceElementSize();
            if (this.mesh.getEditableFaces().size() != expectedPos)
                throw new RuntimeException("Adding face values out of order is not currently supported, but should be before this code is shipped to users."); // TODO
        }

        // Write values to array.
        TEMP_FACE_ARRAY[0] = meshVertex1;
        TEMP_FACE_ARRAY[1] = meshTexCoord1;
        TEMP_FACE_ARRAY[2] = meshVertex2;
        TEMP_FACE_ARRAY[3] = meshTexCoord2;
        TEMP_FACE_ARRAY[4] = meshVertex3;
        TEMP_FACE_ARRAY[5] = meshTexCoord3;
        this.mesh.getEditableFaces().addAll(TEMP_FACE_ARRAY);

        // Return position data was written to.
        return this.faceStartIndex + (this.faceCount++);
    }

    /**
     * Writes tex coord data to the tex coord array.
     * @param localFaceIndex The index of the face to update.
     * @param newMeshVertexIndex The vertex position index to apply to the face.
     * @param newMeshTexCoordIndex The texture coordinate index to apply to the face.
     */
    /*public void writeFace(int localFaceIndex, int newMeshVertexIndex, int newMeshTexCoordIndex) {
        // TODO: This is OK and probably doesn't need bulking. (If bulking is on when this occurs sure it'll get bulked, but overall bulking isn't important here.)
        TEMP_FACE_ARRAY[0] = newMeshVertexIndex;
        TEMP_FACE_ARRAY[1] = newMeshTexCoordIndex;

        int rawArrayStartIndex = (this.faceStartIndex + localFaceIndex) * this.mesh.getFaceElementSize();
        this.mesh.getEditableFaces().set(rawArrayStartIndex, TEMP_FACE_ARRAY, 0, 2);
    }*/ // TODO: This fails because there are 6 values to write, not 2.

    /**
     * Removes a face from the array.
     * @param localFaceIndex The local index of the face to remove.
     */
    public void removeFace(int localFaceIndex) {
        if (localFaceIndex < 0 || localFaceIndex >= this.faceCount)
            throw new IndexOutOfBoundsException("There is no face corresponding to local face ID " + localFaceIndex + ". Valid Range: [0, " + this.faceCount + ").");

        int faceElementSize = this.mesh.getFaceElementSize(); // 6 = 3 vertices * (1 vertex ID + 1 texture coordinate)
        int faceArrayStartIndex = (this.texCoordStartIndex + localFaceIndex) * faceElementSize;
        this.mesh.getEditableFaces().remove(faceArrayStartIndex, faceElementSize);
        // TODO: Update all the faces which reference a value that comes later to decrement the reference indices.
        // TODO: Perhaps we should bulk these changes.
        this.faceCount--;
    }
}