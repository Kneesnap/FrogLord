package net.highwayfrogs.editor.gui.mesh;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * This represents a unit of vertex positions, texture coordinate values, and face values which are conceptually grouped together.
 * This could be as small as containing data for a single face, or as large as containing data for all the faces in a mesh.
 * It relies upon data for this group written together as a continuous chunk.
 * Created by Kneesnap on 9/25/2023.
 */
@Getter
public class DynamicMeshDataEntry {
    @NonNull private final DynamicMesh mesh;
    private DynamicMeshNode meshNode;
    private boolean active = true;
    private int vertexStartIndex = -1;
    private int pendingVertexStartIndex = -1;
    private int pendingVertexCount;
    private int writtenVertexCount;
    private int texCoordStartIndex = -1;
    private int pendingTexCoordStartIndex = -1;
    private int pendingTexCoordCount;
    private int writtenTexCoordCount;
    private int faceStartIndex = -1;
    private int pendingFaceStartIndex = -1;
    private int pendingFaceCount;
    private int writtenFaceCount;

    private static final float[] TEMP_POSITION_ARRAY = new float[3];
    private static final float[] TEMP_TEXCOORD_ARRAY = new float[2];
    private static final int[] TEMP_FACE_ARRAY = new int[6];

    public DynamicMeshDataEntry(DynamicMesh mesh) {
        if (mesh == null)
            throw new NullPointerException("mesh");

        this.mesh = mesh;
    }

    /**
     * Called when the entry is added to a node.
     */
    protected void onAddedToNode(DynamicMeshNode node) {
        // Mark as active.
        this.active = true;
        this.meshNode = node;

        // All of these values are assigned either when the first value is written, or when this is added to the node.
        // This is to ensure we keep the order sorted between entries, allowing binary search.
        // This allows for example, looking up a face by the Face ID which JavaFX reported getting clicked.
        // We don't need to calculate the pending start indices because they're not used for searching.
        if (this.vertexStartIndex == -1 && this.pendingVertexCount == 0)
            this.vertexStartIndex = this.mesh.getEditableVertices().size() / this.mesh.getPointElementSize();
        if (this.texCoordStartIndex == -1 && this.pendingTexCoordCount == 0)
            this.texCoordStartIndex = this.mesh.getEditableTexCoords().size() / this.mesh.getTexCoordElementSize();
        if (this.faceStartIndex == -1 && this.pendingFaceCount == 0)
            this.faceStartIndex = this.mesh.getEditableFaces().size() / this.mesh.getFaceElementSize();
    }

    /**
     * Handle buffered vertices getting written.
     * @param newStartIndex the new face start index to assume
     */
    void onVerticesWritten(int newStartIndex) {
        this.vertexStartIndex = this.pendingVertexStartIndex = newStartIndex;
        this.writtenVertexCount = this.pendingVertexCount;
    }

    /**
     * Handle buffered texCoord data getting written.
     * @param newStartIndex the new texCoord start index to assume
     */
    void onTexCoordsWritten(int newStartIndex) {
        this.texCoordStartIndex = this.pendingTexCoordStartIndex = newStartIndex;
        this.writtenTexCoordCount = this.pendingTexCoordCount;
    }

    /**
     * Handle buffered face data getting written.
     * @param newStartIndex the new face start index to assume
     */
    void onFacesWritten(int newStartIndex) {
        this.faceStartIndex = this.pendingFaceStartIndex = newStartIndex;
        this.writtenFaceCount = this.pendingFaceCount;
    }

    /**
     * Called when the entry is added to a node.
     */
    protected void onRemovedFromNode() {
        this.meshNode = null;

        // Remove Faces
        // Should run first, so any vertices/texCoords defined here used by this face won't trigger the detector for faces using data we're deleting.
        if (this.writtenFaceCount != 0) {
            this.mesh.getEditableFaces().startBatchRemoval();
            for (int i = 0; i < this.writtenFaceCount; i++)
                removeFace(i);

            // Done
            this.faceStartIndex = this.pendingFaceStartIndex = -1;
            this.mesh.getEditableFaces().endBatchRemoval();
        }

        // Remove Vertices
        if (this.writtenVertexCount != 0) {
            this.mesh.getEditableVertices().startBatchRemoval();
            for (int i = 0; i < this.writtenVertexCount; i++)
                removeVertexValue(i);

            // Done
            this.vertexStartIndex = this.pendingVertexStartIndex = -1;
            this.mesh.getEditableVertices().endBatchRemoval();
        }

        // Remove Tex Coords
        if (this.writtenTexCoordCount != 0) {
            this.mesh.getEditableTexCoords().startBatchRemoval();
            for (int i = 0; i < this.writtenTexCoordCount; i++)
                removeTexCoordValue(i);

            // Done
            this.texCoordStartIndex = this.pendingTexCoordStartIndex = -1;
            this.mesh.getEditableTexCoords().endBatchRemoval();
        }

        // Mark this entry as no longer active.
        this.active = false;
    }

    /**
     * Adds a new vertex position.
     * @param position the position vector to add
     * @return the index into the mesh vertex array which the vertex data starts.
     */
    public int addVertexValue(Vector3f position) {
        return addVertexValue(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Adds a new vertex position.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param z The z coordinate.
     * @return the index into the mesh vertex array which the vertex data starts.
     */
    public int addVertexValue(float x, float y, float z) {
        return insertVertexValue(this.writtenVertexCount, x, y, z);
    }

    /**
     * Adds a new vertex position.
     * @param localIndex The local index to insert the index at.
     * @param x          The x coordinate.
     * @param y          The y coordinate.
     * @param z          The z coordinate.
     * @return the index into the mesh vertex array which the vertex data starts.
     */
    public int insertVertexValue(int localIndex, float x, float y, float z) {
        if (!this.active)
            throw new IllegalStateException("Cannot insert a new vertex while the entry is not active.");
        if (localIndex < 0 || localIndex > this.writtenVertexCount)
            throw new IllegalArgumentException("The local vertex index " + localIndex + " is not a valid index to insert a new vertex value.");

        // If applicable, save the start index.
        if (this.vertexStartIndex == -1)
            this.vertexStartIndex = this.mesh.getEditableVertices().size() / this.mesh.getPointElementSize();
        if (this.pendingVertexStartIndex == -1)
            this.pendingVertexStartIndex = this.mesh.getEditableVertices().pendingSize() / this.mesh.getPointElementSize();

        // Writes values to the array.
        TEMP_POSITION_ARRAY[0] = x;
        TEMP_POSITION_ARRAY[1] = y;
        TEMP_POSITION_ARRAY[2] = z;

        // Should occur before an update is triggered.
        int pendingLocalIndex = (localIndex == this.writtenVertexCount ? this.pendingVertexCount : localIndex);
        this.pendingVertexCount++;

        // Determine insertion position, and if we're at the end of the array.
        int insertPos = (this.vertexStartIndex + localIndex) * this.mesh.getPointElementSize();

        // Write potentially batched data.
        if (this.mesh.getEditableVertices().addAll(insertPos, TEMP_POSITION_ARRAY)) {
            this.writtenVertexCount++;
            this.mesh.updateEntryVerticesAfterWrite();
        }

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableVertices().applyToFxArray();

        // Return position data was written to.
        return this.pendingVertexStartIndex + pendingLocalIndex;
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param x             The x coordinate to write.
     */
    public void writeVertexX(int localVtxIndex, float x) {
        if (!this.active)
            throw new IllegalStateException("Cannot write vertex data while the entry is not active.");
        if (localVtxIndex < 0 || localVtxIndex >= this.writtenVertexCount)
            throw new IllegalArgumentException("The local vertex index " + localVtxIndex + " is not available to write, and thus we cannot write data to it. (Pending: " + this.pendingVertexCount + ", Written: " + this.writtenVertexCount + ")");

        this.mesh.getEditableVertices().set(((this.vertexStartIndex + localVtxIndex) * this.mesh.getPointElementSize()), x);

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableVertices().applyToFxArray();
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param y             The y coordinate to write.
     */
    public void writeVertexY(int localVtxIndex, float y) {
        if (!this.active)
            throw new IllegalStateException("Cannot write vertex data while the entry is not active.");
        if (localVtxIndex < 0 || localVtxIndex >= this.writtenVertexCount)
            throw new IllegalArgumentException("The local vertex index " + localVtxIndex + " is not available to write, and thus we cannot write data to it. (Pending: " + this.pendingVertexCount + ", Written: " + this.writtenVertexCount + ")");

        this.mesh.getEditableVertices().set(((this.vertexStartIndex + localVtxIndex) * this.mesh.getPointElementSize()) + 1, y);

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableVertices().applyToFxArray();
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param z             The z coordinate to write.
     */
    public void writeVertexZ(int localVtxIndex, float z) {
        if (!this.active)
            throw new IllegalStateException("Cannot write vertex data while the entry is not active.");
        if (localVtxIndex < 0 || localVtxIndex >= this.writtenVertexCount)
            throw new IllegalArgumentException("The local vertex index " + localVtxIndex + " is not available to write, and thus we cannot write data to it. (Pending: " + this.pendingVertexCount + ", Written: " + this.writtenVertexCount + ")");

        this.mesh.getEditableVertices().set(((this.vertexStartIndex + localVtxIndex) * this.mesh.getPointElementSize()) + 2, z);

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableVertices().applyToFxArray();
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param x             The x coordinate to write.
     * @param y             The y coordinate to write.
     * @param z             The z coordinate to write.
     */
    public void writeVertexXYZ(int localVtxIndex, float x, float y, float z) {
        if (!this.active)
            throw new IllegalStateException("Cannot write vertex data while the entry is not active.");
        if (localVtxIndex < 0 || localVtxIndex >= this.writtenVertexCount)
            throw new IllegalArgumentException("The local vertex index " + localVtxIndex + " is not available to write, and thus we cannot write data to it. (Pending: " + this.pendingVertexCount + ", Written: " + this.writtenVertexCount + ")");

        TEMP_POSITION_ARRAY[0] = x;
        TEMP_POSITION_ARRAY[1] = y;
        TEMP_POSITION_ARRAY[2] = z;

        int vertexElementSize = this.mesh.getPointElementSize(); // 3
        int rawArrayStartIndex = (this.vertexStartIndex + localVtxIndex) * vertexElementSize;
        this.mesh.getEditableVertices().set(rawArrayStartIndex, TEMP_POSITION_ARRAY, 0, vertexElementSize);

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableVertices().applyToFxArray();
    }

    /**
     * Writes vertex position data to a vertex.
     * @param localVtxIndex The index to a vertex saved here.
     * @param position the position vector to write
     */
    public void writeVertexXYZ(int localVtxIndex, Vector3f position) {
        writeVertexXYZ(localVtxIndex, position.getX(), position.getY(), position.getZ());
    }

    /**
     * Removes a vertex position from the array.
     * @param localVtxIndex The local index of the position to remove.
     */
    public void removeVertexValue(int localVtxIndex) {
        if (!this.active)
            throw new IllegalStateException("Cannot remove vertex data while the entry is not active.");
        if (localVtxIndex < 0 || localVtxIndex >= this.writtenVertexCount)
            throw new IndexOutOfBoundsException("There is no written vertex corresponding to local vertex ID " + localVtxIndex + ". Valid Range: [0, " + this.pendingVertexCount + "/" + this.writtenVertexCount + ").");

        int vertexElementSize = this.mesh.getPointElementSize(); // 3
        int vertexArrayStartIndex = (this.vertexStartIndex + localVtxIndex) * vertexElementSize;
        if (this.mesh.getEditableVertices().isQueuedForRemoval(vertexArrayStartIndex))
            return; // Already queued for removal.

        // Mark the vertex as pending removal.
        this.pendingVertexCount--;

        // Remove vertex.
        if (this.mesh.getEditableVertices().remove(vertexArrayStartIndex, vertexElementSize)) {
            this.writtenVertexCount--;
            this.mesh.updateEntryVerticesAfterWrite();
        }

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableVertices().applyToFxArray();
    }

    /**
     * Gets the index which vertex position data starts in the full mesh array.
     * @param localVertexIndex local index of vertex managed by this entry
     * @return vertexMeshArrayIndex
     */
    public int getVertexMeshArrayIndex(int localVertexIndex) {
        if (localVertexIndex < 0 || localVertexIndex >= this.writtenVertexCount)
            throw new ArrayIndexOutOfBoundsException("Invalid local vertex ID: " + localVertexIndex);

        int vertexElementSize = this.mesh.getPointElementSize(); // 3
        return (this.vertexStartIndex + localVertexIndex) * vertexElementSize;
    }

    /**
     * Adds a new texture coordinate value.
     * @param uv The vector containing uv values.
     * @return the index into the mesh array which the tex coord data starts.
     */
    public int addTexCoordValue(Vector2f uv) {
        return addTexCoordValue(uv.getX(), uv.getY());
    }

    /**
     * Adds a new texture coordinate value.
     * @param u The U (horizontal) texture coordinate.
     * @param v The V (vertical) texture coordinate.
     * @return the index into the mesh array which the tex coord data starts.
     */
    public int addTexCoordValue(float u, float v) {
        return insertTexCoordValue(this.writtenTexCoordCount, u, v);
    }

    /**
     * Adds a new texture coordinate value.
     * @param localIndex The local index
     * @param uv         The vector containing uv values.
     * @return the index into the mesh array which the tex coord data starts.
     */
    public int insertTexCoordValue(int localIndex, Vector2f uv) {
        return insertTexCoordValue(localIndex, uv.getX(), uv.getY());
    }

    /**
     * Adds a new texture coordinate value.
     * @param localIndex The local index
     * @param u          The U (horizontal) texture coordinate.
     * @param v          The V (vertical) texture coordinate.
     * @return the index into the mesh array which the tex coord data starts.
     */
    public int insertTexCoordValue(int localIndex, float u, float v) {
        if (!this.active)
            throw new IllegalStateException("Cannot insert new texCoord data while the entry is not active.");
        if (localIndex < 0 || localIndex > this.writtenTexCoordCount)
            throw new IllegalArgumentException("The local texCoord index " + localIndex + " is not a valid index to insert a new texCoord value.");

        // If applicable, save the start index.
        if (this.texCoordStartIndex == -1)
            this.texCoordStartIndex = this.mesh.getEditableTexCoords().size() / this.mesh.getTexCoordElementSize();
        if (this.pendingTexCoordStartIndex == -1)
            this.pendingTexCoordStartIndex = this.mesh.getEditableTexCoords().pendingSize() / this.mesh.getTexCoordElementSize();

        // Write values to array.
        TEMP_TEXCOORD_ARRAY[0] = u;
        TEMP_TEXCOORD_ARRAY[1] = v;

        // Should occur before an update is triggered.
        int pendingLocalIndex = (localIndex == this.writtenTexCoordCount ? this.pendingTexCoordCount : localIndex);
        this.pendingTexCoordCount++;

        // Determine array position & write.
        int insertPos = (this.texCoordStartIndex + localIndex) * this.mesh.getTexCoordElementSize();
        if (this.mesh.getEditableTexCoords().addAll(insertPos, TEMP_TEXCOORD_ARRAY)) {
            this.writtenTexCoordCount++;
            this.mesh.updateEntryTexCoordsAfterWrite();
        }

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableTexCoords().applyToFxArray();

        // Return position data was written to.
        return this.pendingTexCoordStartIndex + pendingLocalIndex;
    }

    /**
     * Writes tex coord data to the tex coord array.
     * @param localTexCoordIndex The index to a tex coord pair saved here.
     * @param u                  The U (horizontal) texture coordinate to write.
     * @param v                  The V (vertical) texture coordinate to write.
     */
    public void writeTexCoordValue(int localTexCoordIndex, float u, float v) {
        if (!this.active)
            throw new IllegalStateException("Cannot write texCoord data while the entry is not active.");
        if (localTexCoordIndex < 0 || localTexCoordIndex >= this.writtenTexCoordCount)
            throw new IllegalArgumentException("The local texCoord index " + localTexCoordIndex + " is not valid, and thus we cannot write data to it.");

        TEMP_TEXCOORD_ARRAY[0] = u;
        TEMP_TEXCOORD_ARRAY[1] = v;

        int texCoordElementSize = this.mesh.getTexCoordElementSize(); // 2
        int rawArrayStartIndex = (this.texCoordStartIndex + localTexCoordIndex) * texCoordElementSize;
        this.mesh.getEditableTexCoords().set(rawArrayStartIndex, TEMP_TEXCOORD_ARRAY, 0, texCoordElementSize);

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableTexCoords().applyToFxArray();
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
     * Update all faces using the given texCoordUvIndex to use the new one.
     * @param oldTextureUvIndex the old uv texCoord index to replace
     * @param newTextureUvIndex the new replacement uv texCoord index
     */
    public void updateTextureIndex(int oldTextureUvIndex, int newTextureUvIndex) {
        if (oldTextureUvIndex < 0 || newTextureUvIndex < 0)
            return; // Invalid textures, so skip em.

        boolean anyUpdated = false;
        for (int i = 0; i < this.writtenFaceCount; i++) {
            int texCoordBaseIndex = (this.faceStartIndex + i) * this.mesh.getFaceElementSize() + this.mesh.getVertexFormat().getTexCoordIndexOffset();
            for (int j = 0; j < 3; j++) {
                int localTexCoordVtxIndex = texCoordBaseIndex + (j * this.mesh.getVertexFormat().getVertexIndexSize());
                if (this.mesh.getEditableFaces().get(localTexCoordVtxIndex) == oldTextureUvIndex) {
                    this.mesh.getEditableFaces().set(localTexCoordVtxIndex, newTextureUvIndex);
                    anyUpdated = true;
                }
            }
        }

        // Trigger an update. (This will occur after all batched changes are ready)
        if (anyUpdated)
            this.mesh.getEditableFaces().applyToFxArray();
    }

    /**
     * Removes a tex coord from the array.
     * @param localTexCoordIndex The local index of the tex coord to remove.
     */
    public void removeTexCoordValue(int localTexCoordIndex) {
        if (!this.active)
            throw new IllegalStateException("Cannot remove texCoord data while the entry is not active.");
        if (localTexCoordIndex < 0 || localTexCoordIndex >= this.writtenTexCoordCount)
            throw new IllegalArgumentException("The local texCoord index " + localTexCoordIndex + " is not valid, and thus we cannot remove it. Range: [0, " + this.pendingTexCoordCount + "/" + this.writtenTexCoordCount + ").");

        int texCoordElementSize = this.mesh.getTexCoordElementSize(); // 2
        int texCoordArrayStartIndex = (this.texCoordStartIndex + localTexCoordIndex) * texCoordElementSize;
        if (this.mesh.getEditableTexCoords().isQueuedForRemoval(texCoordArrayStartIndex))
            return; // Already queued for removal.

        // This should occur before the update so the new value is accessible.
        this.pendingTexCoordCount--;

        // Remove from array.
        if (this.mesh.getEditableTexCoords().remove(texCoordArrayStartIndex, texCoordElementSize)) {
            this.writtenTexCoordCount--;
            this.mesh.updateEntryTexCoordsAfterWrite();
        }

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableTexCoords().applyToFxArray();
    }

    /**
     * Gets the index which texture coordinate data starts in the full mesh array.
     * @param localTexCoordIndex local index of texCoord managed by this entry
     * @return texCoordMeshArrayIndex
     */
    public int getTexCoordMeshArrayIndex(int localTexCoordIndex) {
        if (localTexCoordIndex < 0 || localTexCoordIndex >= this.writtenTexCoordCount)
            throw new ArrayIndexOutOfBoundsException("Invalid local texCoord ID: " + localTexCoordIndex);

        int texCoordElementSize = this.mesh.getTexCoordElementSize(); // 2
        return (this.texCoordStartIndex + localTexCoordIndex) * texCoordElementSize;
    }

    /**
     * Adds a new face referencing existing vertex and texCoord data.
     * @param meshVertex1   The index to the position of the first vertex.
     * @param meshTexCoord1 The index to the position of the first texture coordinate.
     * @param meshVertex2   The index to the position of the second vertex.
     * @param meshTexCoord2 The index to the position of the second texture coordinate.
     * @param meshVertex3   The index to the position of the third vertex.
     * @param meshTexCoord3 The index to the position of the third texture coordinate.
     * @return the index into the mesh array which the face data starts.
     */
    public int addFace(int meshVertex1, int meshTexCoord1, int meshVertex2, int meshTexCoord2, int meshVertex3, int meshTexCoord3) {
        return insertFace(this.writtenFaceCount, meshVertex1, meshTexCoord1, meshVertex2, meshTexCoord2, meshVertex3, meshTexCoord3);
    }

    /**
     * Inserts a new face referencing existing vertex and texCoord data.
     * @param localIndex    The local index to insert the face
     * @param meshVertex1   The index to the position of the first vertex.
     * @param meshTexCoord1 The index to the position of the first texture coordinate.
     * @param meshVertex2   The index to the position of the second vertex.
     * @param meshTexCoord2 The index to the position of the second texture coordinate.
     * @param meshVertex3   The index to the position of the third vertex.
     * @param meshTexCoord3 The index to the position of the third texture coordinate.
     * @return the index into the mesh array which the face data starts.
     */
    public int insertFace(int localIndex, int meshVertex1, int meshTexCoord1, int meshVertex2, int meshTexCoord2, int meshVertex3, int meshTexCoord3) {
        if (!this.active)
            throw new IllegalStateException("Cannot insert polygon face data while the entry is not active.");
        if (localIndex < 0 || localIndex > this.writtenFaceCount)
            throw new IllegalArgumentException("The local face index " + localIndex + " is not a valid index to insert a new face.");

        // If applicable, save the start index.
        if (this.faceStartIndex == -1)
            this.faceStartIndex = this.mesh.getEditableFaces().size() / this.mesh.getFaceElementSize();
        if (this.pendingFaceStartIndex == -1)
            this.pendingFaceStartIndex = this.mesh.getEditableFaces().pendingSize() / this.mesh.getFaceElementSize();

        // Validate face values.
        validateFace(meshVertex1, meshTexCoord1, 1);
        validateFace(meshVertex2, meshTexCoord2, 2);
        validateFace(meshVertex3, meshTexCoord3, 3);

        // Write values to array.
        TEMP_FACE_ARRAY[0] = meshVertex1;
        TEMP_FACE_ARRAY[1] = meshTexCoord1;
        TEMP_FACE_ARRAY[2] = meshVertex2;
        TEMP_FACE_ARRAY[3] = meshTexCoord2;
        TEMP_FACE_ARRAY[4] = meshVertex3;
        TEMP_FACE_ARRAY[5] = meshTexCoord3;

        // Should occur before an update is triggered.
        int pendingLocalIndex = (localIndex == this.writtenFaceCount ? this.pendingFaceCount : localIndex);
        this.pendingFaceCount++;

        // Write mesh data.
        int insertPos = (this.faceStartIndex + localIndex) * this.mesh.getFaceElementSize();
        if (this.mesh.getEditableFaces().addAll(insertPos, TEMP_FACE_ARRAY)) {
            this.writtenFaceCount++;
            this.mesh.updateEntryFacesAfterWrite();
        }

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableFaces().applyToFxArray();

        // Return position data was written to.
        return this.pendingFaceStartIndex + pendingLocalIndex;
    }

    /**
     * Overwrites a face-data referencing vertex and texCoord arrays.
     * @param localFaceIndex The local index to insert the face
     * @param meshVertex1    The index to the position of the first vertex.
     * @param meshTexCoord1  The index to the position of the first texture coordinate.
     * @param meshVertex2    The index to the position of the second vertex.
     * @param meshTexCoord2  The index to the position of the second texture coordinate.
     * @param meshVertex3    The index to the position of the third vertex.
     * @param meshTexCoord3  The index to the position of the third texture coordinate.
     */
    public void writeFace(int localFaceIndex, int meshVertex1, int meshTexCoord1, int meshVertex2, int meshTexCoord2, int meshVertex3, int meshTexCoord3) {
        if (!this.active)
            throw new IllegalStateException("Cannot write polygon face data while the entry is not active.");
        if (localFaceIndex < 0 || localFaceIndex >= this.writtenFaceCount)
            throw new IllegalArgumentException("The local face index " + localFaceIndex + " is not a valid index to overwrite face data.");

        // Calculate indices.
        int faceElementSize = this.mesh.getFaceElementSize(); // 6
        int faceArrayStartIndex = (this.faceStartIndex + localFaceIndex) * faceElementSize;

        // Write values to array.
        TEMP_FACE_ARRAY[0] = (meshVertex1 > Integer.MIN_VALUE) ? meshVertex1 :  this.mesh.getEditableFaces().get(faceArrayStartIndex);
        TEMP_FACE_ARRAY[1] = (meshTexCoord1 > Integer.MIN_VALUE) ? meshTexCoord1 : this.mesh.getEditableFaces().get(faceArrayStartIndex + 1);
        TEMP_FACE_ARRAY[2] = (meshVertex2 > Integer.MIN_VALUE) ? meshVertex2 : this.mesh.getEditableFaces().get(faceArrayStartIndex + 2);
        TEMP_FACE_ARRAY[3] = (meshTexCoord2 > Integer.MIN_VALUE) ? meshTexCoord2 : this.mesh.getEditableFaces().get(faceArrayStartIndex + 3);
        TEMP_FACE_ARRAY[4] = (meshVertex3 > Integer.MIN_VALUE) ? meshVertex3 : this.mesh.getEditableFaces().get(faceArrayStartIndex + 4);
        TEMP_FACE_ARRAY[5] = (meshTexCoord3 > Integer.MIN_VALUE) ? meshTexCoord3 : this.mesh.getEditableFaces().get(faceArrayStartIndex + 5);

        // Validate face values.
        validateFace(TEMP_FACE_ARRAY[0], TEMP_FACE_ARRAY[1], 1);
        validateFace(TEMP_FACE_ARRAY[2], TEMP_FACE_ARRAY[3], 2);
        validateFace(TEMP_FACE_ARRAY[4], TEMP_FACE_ARRAY[5], 3);

        // Write face data.
        this.mesh.getEditableFaces().set(faceArrayStartIndex, TEMP_FACE_ARRAY, 0, faceElementSize);

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableFaces().applyToFxArray();
    }

    private void validateFace(int meshVertexIndex, int texCoordIndex, int localVertexIndex) {
        int vertexCount = (this.mesh.getEditableVertices().pendingSize() / this.mesh.getPointElementSize());
        if (meshVertexIndex >= vertexCount || meshVertexIndex < 0)
            throw new IllegalStateException("Invalid mesh vertex index (" + meshVertexIndex + ") for vertex " + localVertexIndex + ". There are only " + vertexCount + " total vertices!");

        int texCoordCount = (this.mesh.getEditableTexCoords().pendingSize() / this.mesh.getTexCoordElementSize());
        if (texCoordIndex >= texCoordCount || texCoordIndex < 0)
            throw new IllegalStateException("Invalid mesh texCoord index (" + texCoordIndex + ") for texCoord " + localVertexIndex + ". There are only " + texCoordCount + " total texCoords!");
    }

    /**
     * Writes tex coord data to the tex coord array.
     * @param localFaceIndex       The index of the face to update.
     * @param newMeshVertexIndex   The vertex position index to apply to the face.
     * @param newMeshTexCoordIndex The texture coordinate index to apply to the face.
     */
    public void writeFace(int localFaceIndex, int faceVertexIndex, int newMeshVertexIndex, int newMeshTexCoordIndex) {
        if (!this.active)
            throw new IllegalStateException("Cannot write polygon face data while the entry is not active.");
        if ((localFaceIndex != Integer.MIN_VALUE && localFaceIndex < 0) || localFaceIndex >= this.writtenFaceCount)
            throw new IllegalArgumentException("The local face index " + localFaceIndex + " has no face, and thus we cannot write polygon face data to it.");
        if ((localFaceIndex != Integer.MIN_VALUE && faceVertexIndex < 0) || faceVertexIndex >= 3)
            throw new IllegalArgumentException("The provided face vertex ID was " + faceVertexIndex + ", but there are only 3 vertices per face.");

        // Get values to write.
        int rawArrayStartIndex = (this.faceStartIndex + localFaceIndex) * this.mesh.getFaceElementSize() + (faceVertexIndex * this.mesh.getVertexFormat().getVertexIndexSize());
        TEMP_FACE_ARRAY[0] = (newMeshVertexIndex > Integer.MIN_VALUE) ? newMeshVertexIndex : this.mesh.getEditableFaces().get(rawArrayStartIndex);
        TEMP_FACE_ARRAY[1] = (newMeshTexCoordIndex > Integer.MIN_VALUE) ? newMeshTexCoordIndex : this.mesh.getEditableFaces().get(rawArrayStartIndex + 1);

        // Write raw data.
        this.mesh.getEditableFaces().set(rawArrayStartIndex, TEMP_FACE_ARRAY, 0, 2);

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableFaces().applyToFxArray();
    }

    /**
     * Removes a face from the array.
     * @param localFaceIndex The local index of the face to remove.
     */
    public void removeFace(int localFaceIndex) {
        if (!this.active)
            throw new IllegalStateException("Cannot remove face data while the entry is not active.");
        if (localFaceIndex < 0 || localFaceIndex >= this.writtenFaceCount)
            throw new IndexOutOfBoundsException("There is no written face corresponding to local face ID " + localFaceIndex + ", thus we cannot remove it. Valid Range: [0, " + this.writtenFaceCount + "/" + this.writtenFaceCount + ").");

        int faceElementSize = this.mesh.getFaceElementSize(); // 6 = 3 vertices * (1 vertex ID + 1 texture coordinate)
        int faceArrayStartIndex = (this.faceStartIndex + localFaceIndex) * faceElementSize;
        if (this.mesh.getEditableFaces().isQueuedForRemoval(faceArrayStartIndex))
            return; // Already queued for removal.

        // Update before removal.
        this.pendingFaceCount--;

        // Remove face data.
        if (this.mesh.getEditableFaces().remove(faceArrayStartIndex, faceElementSize)) {
            this.writtenFaceCount--;
            this.mesh.updateEntryFacesAfterWrite();
        }

        // Trigger an update. (If batching is enabled, this will occur after all changes are ready)
        this.mesh.getEditableFaces().applyToFxArray();
    }

    /**
     * Gets the index which face index data starts in the full mesh array.
     * @param localFaceIndex local index of vertex managed by this entry
     * @return faceMeshArrayIndex
     */
    public int getFaceMeshArrayIndex(int localFaceIndex) {
        if (localFaceIndex < 0 || localFaceIndex >= this.writtenFaceCount)
            throw new ArrayIndexOutOfBoundsException("Invalid local face ID: " + localFaceIndex);

        int faceElementSize = this.mesh.getFaceElementSize(); // 6 = 3 vertices * (1 vertex ID + 1 texture coordinate)
        return (this.faceStartIndex + localFaceIndex) * faceElementSize;
    }

    /**
     * Gets the current vertex array start position.
     * If the internal start index is invalid, the start index will be calculated.
     */
    public int getCurrentVertexStartIndex() {
        return this.vertexStartIndex >= 0 ? this.vertexStartIndex : (this.mesh.getEditableVertices().size() / this.mesh.getPointElementSize());
    }

    /**
     * Gets the current texCoord array start position for the existing
     * If the internal start index is invalid, the start index will be calculated.
     */
    public int getCurrentTexCoordStartIndex() {
        return this.texCoordStartIndex >= 0 ? this.texCoordStartIndex : (this.mesh.getEditableTexCoords().size() / this.mesh.getTexCoordElementSize());
    }

    /**
     * Gets the current face array start position.
     * If the internal start index is invalid, the start index will be calculated.
     */
    public int getCurrentFaceStartIndex() {
        return this.faceStartIndex >= 0 ? this.faceStartIndex : (this.mesh.getEditableFaces().size() / this.mesh.getFaceElementSize());
    }

    /**
     * Gets the hypothetical vertex array start position which will exist once all queued operations were to be performed.
     * If the start index is invalid, the start index will be calculated as the end of the hypothetical array.
     */
    public int getPendingVertexStartIndex() {
        return this.pendingVertexStartIndex >= 0 ? this.pendingVertexStartIndex : (this.mesh.getEditableVertices().pendingSize() / this.mesh.getPointElementSize());
    }

    /**
     * Gets the hypothetical texCoord array start position which will exist once all queued operations were to be performed.
     * If the start index is invalid, the start index will be calculated as the end of the hypothetical array.
     */
    public int getPendingTexCoordStartIndex() {
        return this.pendingTexCoordStartIndex >= 0 ? this.pendingTexCoordStartIndex : (this.mesh.getEditableTexCoords().pendingSize() / this.mesh.getTexCoordElementSize());
    }

    /**
     * Gets the hypothetical face writer array position which will exist once all queued operations were to be performed.
     * If the start index is invalid, the start index will be calculated as the end of the hypothetical array.
     */
    public int getPendingFaceStartIndex() {
        return this.pendingFaceStartIndex >= 0 ? this.pendingFaceStartIndex : (this.mesh.getEditableFaces().pendingSize() / this.mesh.getFaceElementSize());
    }

    /**
     * Print debug information about this entry.
     */
    public void printDebugInformation(ILogger logger, String leftPadding) {
        logger.info(leftPadding + "Mesh Entry (" + Utils.getSimpleName(this) + " for " + Utils.getSimpleName(this.mesh) + "):");
        logger.info(leftPadding + "Faces [Start: " + this.faceStartIndex + ", Written: " + this.writtenFaceCount + ", Pending: " + this.pendingFaceCount
                + ", Array Size: " + this.mesh.getEditableFaces().size() + "/" + this.mesh.getEditableFaces().pendingSize() + "/" + this.mesh.getFaces().size() + "]");
        logger.info(leftPadding + "TexCoords [Start: " + this.texCoordStartIndex + ", Written: " + this.writtenTexCoordCount + ", Pending: " + this.pendingTexCoordCount
                + ", Array Size: " + this.mesh.getEditableTexCoords().size() + "/" + this.mesh.getEditableTexCoords().pendingSize() + "/" + this.mesh.getTexCoords().size() + "]");
        logger.info(leftPadding + "Vertex [Start: " + this.vertexStartIndex + ", Written: " + this.writtenVertexCount + ", Pending: " + this.pendingVertexCount
                + ", Array Size: " + this.mesh.getEditableVertices().size() + "/" + this.mesh.getEditableVertices().pendingSize() + "/" + this.mesh.getPoints().size() + "]");
    }
}