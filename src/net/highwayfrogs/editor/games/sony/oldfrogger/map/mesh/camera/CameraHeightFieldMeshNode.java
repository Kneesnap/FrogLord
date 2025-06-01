package net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.camera;

import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapCameraHeightFieldPacket;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerCameraHeightFieldManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshNode;
import net.highwayfrogs.editor.system.math.Vector2f;

/**
 * Represents the main mesh data in a camera height-field mesh.
 * Created by Kneesnap on 12/30/2023.
 */
public class CameraHeightFieldMeshNode extends DynamicMeshNode {
    private final Vector2f cachedSelectedVertexUv = new Vector2f();
    private final Vector2f cachedUnselectedVertexUv = new Vector2f();
    private DynamicMeshDataEntry mainEntry;

    public CameraHeightFieldMeshNode(CameraHeightFieldMesh mesh) {
        super(mesh);
    }

    @Override
    public CameraHeightFieldMesh getMesh() {
        return (CameraHeightFieldMesh) super.getMesh();
    }

    @Override
    public boolean updateTexCoord(DynamicMeshDataEntry entry, int localTexCoordIndex) {
        if (entry == this.mainEntry) {
            Vector2f localTextureUv;
            int x = getGridX(localTexCoordIndex);
            int z = getGridZ(localTexCoordIndex);
            if (getManager().isVertexSelected(x, z)) {
                localTextureUv = calculateSelectedTextureUv();
            } else {
                localTextureUv = calculateUnselectedTextureUv();
            }

            this.mainEntry.writeTexCoordValue(localTexCoordIndex, localTextureUv);
            return true;
        }

        return false;
    }

    @Override
    public boolean updateVertex(DynamicMeshDataEntry entry, int localVertexIndex) {
        if (entry == this.mainEntry) {
            int x = getGridX(localVertexIndex);
            int z = getGridZ(localVertexIndex);
            OldFroggerMapCameraHeightFieldPacket packet = getMapPacket();
            this.mainEntry.writeVertexXYZ(localVertexIndex, packet.getWorldX(x), packet.getWorldY(x, z), packet.getWorldZ(z));
        }

        return false;
    }

    @Override
    protected void onAddedToMesh() {
        super.onAddedToMesh();
        this.mainEntry = createMeshEntry();
    }

    /**
     * Gets the index of the data type in the mesh array, based on the x and z coordinates.
     * @param gridX x grid coordinate of vertex
     * @param gridZ z grid coordinate of vertex
     * @return meshIndex
     */
    public int getMeshIndex(int gridX, int gridZ) {
        return (gridZ * getMapPacket().getXSquareCount()) + gridX;
    }

    /**
     * Gets the index of the first face for the visual representation of the camera grid position provided.
     * Each visual representation contains two faces (forming a quad). To get the second face index, add 1.
     * @param gridX x coordinate to the camera grid
     * @param gridZ z coordinate to the camera grid
     * @return meshFaceIndex
     */
    public int getFaceIndex(int gridX, int gridZ) {
        return ((gridZ * (getMapPacket().getXSquareCount() - 1)) + gridX) * 2;
    }

    /**
     * Gets the mesh index from the face index.
     * @param meshFaceIndex index into faces array to individual face (not individual element).
     * @return meshIndex
     */
    public int getMeshIndexFromFaceIndex(int meshFaceIndex) {
        if (meshFaceIndex < 0)
            throw new IndexOutOfBoundsException("meshFaceIndex: " + meshFaceIndex);

        // Each vertex has 2 tris shown (forming a quad). So, when converting, account for that.
        int quadFaceIndex = (meshFaceIndex / 2);

        // Faces aren't created for the max vertex in the X & Z direction.
        // However, the mesh index we're creating does account for those, so we account for them.
        return quadFaceIndex + (quadFaceIndex / (getMapPacket().getXSquareCount() - 1));
    }

    /**
     * Gets the x coordinate in the camera grid from the mesh array index.
     * @param meshIndex mesh index to calculate the coordinate from
     * @return x coordinate
     */
    public int getGridX(int meshIndex) {
        if (meshIndex < 0 || meshIndex >= (getMapPacket().getXSquareCount() * getMapPacket().getZSquareCount()))
            throw new IndexOutOfBoundsException("meshIndex: " + meshIndex);

        return meshIndex % getMapPacket().getXSquareCount();
    }

    /**
     * Gets the z coordinate in the camera grid from the mesh array index.
     * @param meshIndex mesh index to calculate the coordinate from
     * @return z coordinate
     */
    public int getGridZ(int meshIndex) {
        if (meshIndex < 0 || meshIndex >= (getMapPacket().getXSquareCount() * getMapPacket().getZSquareCount()))
            throw new IndexOutOfBoundsException("meshIndex: " + meshIndex);

        return meshIndex / getMapPacket().getXSquareCount();
    }

    private DynamicMeshDataEntry createMeshEntry() {
        DynamicMeshDataEntry entry = new DynamicMeshDataEntry(getMesh());

        // Calculate default uv (representing unselected vertex).
        Vector2f unselectedTextureUv = calculateUnselectedTextureUv();
        Vector2f selectedTextureUv = calculateSelectedTextureUv();

        // Add vertices.
        int xSquareCount = getMapPacket().getXSquareCount();
        int zSquareCount = getMapPacket().getZSquareCount();
        for (int z = 0; z < zSquareCount; z++) {
            float zPos = getMapPacket().getWorldZ(z);
            for (int x = 0; x < xSquareCount; x++) {
                float xPos = getMapPacket().getWorldX(x);
                float yPos = getMapPacket().getWorldY(x, z);
                entry.addVertexValue(xPos, yPos, zPos);

                // Add the UV for the vertex.
                entry.addTexCoordValue(getManager().isVertexSelected(x, z) ? selectedTextureUv : unselectedTextureUv);
            }
        }

        // Setup faces.
        for (int z = 0; z < zSquareCount - 1; z++) {
            for (int x = 0; x < xSquareCount - 1; x++) {
                int baseIndex = getMeshIndex(x, z);

                // Calculate UV indices.
                int uvBottomLeftIndex = entry.getPendingTexCoordStartIndex() + baseIndex;
                int uvBottomRightIndex = uvBottomLeftIndex + 1;
                int uvTopLeftIndex = uvBottomLeftIndex + getMapPacket().getXSquareCount();
                int uvTopRightIndex = uvTopLeftIndex + 1;

                // Calculate vertices.
                int vtxBottomLeftIndex = entry.getPendingVertexStartIndex() + baseIndex;
                int vtxBottomRightIndex = vtxBottomLeftIndex + 1;
                int vtxTopLeftIndex = vtxBottomLeftIndex + getMapPacket().getXSquareCount();
                int vtxTopRightIndex = vtxTopLeftIndex + 1;

                // JavaFX uses counter-clockwise winding order.
                entry.addFace(vtxBottomLeftIndex, uvBottomLeftIndex, vtxTopRightIndex, uvTopRightIndex, vtxTopLeftIndex, uvTopLeftIndex);
                entry.addFace(vtxBottomLeftIndex, uvBottomLeftIndex, vtxBottomRightIndex, uvBottomRightIndex, vtxTopRightIndex, uvTopRightIndex);
            }
        }

        addUnlinkedEntry(entry);
        return entry;
    }

    /**
     * Updates the mesh index information for the faces representing the given camera height grid position.
     * @param x x position in the camera height grid to update.
     * @param z z position in the camera height grid to update.
     */
    public void updateFace(int x, int z) {
        int baseIndex = getMeshIndex(x, z);
        int faceIndex = this.mainEntry.getCurrentFaceStartIndex() + getFaceIndex(x, z);

        // Calculate UV indices.
        int uvBottomLeftIndex = this.mainEntry.getCurrentTexCoordStartIndex() + baseIndex;
        int uvBottomRightIndex = uvBottomLeftIndex + 1;
        int uvTopLeftIndex = uvBottomLeftIndex + getMapPacket().getXSquareCount();
        int uvTopRightIndex = uvTopLeftIndex + 1;

        // Calculate vertices.
        int vtxBottomLeftIndex = this.mainEntry.getCurrentVertexStartIndex() + baseIndex;
        int vtxBottomRightIndex = vtxBottomLeftIndex + 1;
        int vtxTopLeftIndex = vtxBottomLeftIndex + getMapPacket().getXSquareCount();
        int vtxTopRightIndex = vtxTopLeftIndex + 1;

        // JavaFX uses counter-clockwise winding order.
        getMesh().getEditableFaces().startBatchingUpdates();
        this.mainEntry.writeFace(faceIndex++, vtxBottomLeftIndex, uvBottomLeftIndex, vtxTopRightIndex, uvTopRightIndex, vtxTopLeftIndex, uvTopLeftIndex);
        this.mainEntry.writeFace(faceIndex, vtxBottomLeftIndex, uvBottomLeftIndex, vtxBottomRightIndex, uvBottomRightIndex, vtxTopRightIndex, uvTopRightIndex);
        getMesh().getEditableFaces().endBatchingUpdates();
    }

    /**
     * Updates the position of the vertex on the mesh at the given camera height grid position.
     * @param x x position in the camera height grid to update.
     * @param z z position in the camera height grid to update.
     */
    public void updateVertex(int x, int z) {
        OldFroggerMapCameraHeightFieldPacket packet = getMapPacket();
        this.mainEntry.writeVertexXYZ(getMeshIndex(x, z), packet.getWorldX(x), packet.getWorldY(x, z), packet.getWorldZ(z));
    }

    /**
     * Updates the local texture coordinate for the texture coordinate at the given camera grid position.
     * @param x x position in the camera height grid to update.
     * @param z z position in the camera height grid to update.
     */
    public void updateTexCoord(int x, int z) {
        Vector2f localTextureUv;
        if (getManager().isVertexSelected(x, z)) {
            localTextureUv = calculateSelectedTextureUv();
        } else {
            localTextureUv = calculateUnselectedTextureUv();
        }

        this.mainEntry.writeTexCoordValue(getMeshIndex(x, z), localTextureUv);
    }

    /**
     * Gets the map file which mesh data comes from.
     */
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }

    /**
     * Gets the UI manager responsible for managing this mesh.
     */
    public OldFroggerCameraHeightFieldManager getManager() {
        return getMesh().getManager();
    }

    /**
     * Gets the camera height-field map packet containing the data represented by this mesh.
     */
    public OldFroggerMapCameraHeightFieldPacket getMapPacket() {
        return getMap().getCameraHeightFieldPacket();
    }

    /**
     * Calculates the UV of a selected vertex.
     * @return selectedTextureUv
     */
    public Vector2f calculateSelectedTextureUv() {
        Vector2f selectedTextureUv = getMesh().getSelectedTextureBlending().calculateUvForColor2(this.cachedSelectedVertexUv);
        return getMesh().getTextureAtlas().getUV(getMesh().getSelectedTexture(), selectedTextureUv);
    }

    /**
     * Calculates the UV of an unselected vertex.
     * @return unselectedTextureUv
     */
    public Vector2f calculateUnselectedTextureUv() {
        Vector2f unselectedTextureUv = getMesh().getSelectedTextureBlending().calculateUvForColor1(this.cachedUnselectedVertexUv);
        return getMesh().getTextureAtlas().getUV(getMesh().getSelectedTexture(), unselectedTextureUv);
    }

    /**
     * Updates teh face data of all faces.
     * Should be called only if a change occurs which impacts the IDs of face vertex data.
     */
    public void updateAllFaces() {
        int xCount = getMapPacket().getXSquareCount();
        int zCount = getMapPacket().getZSquareCount();

        // Update faces.
        getMesh().getEditableFaces().startBatchingUpdates();
        for (int z = 0; z < zCount - 1; z++)
            for (int x = 0; x < xCount - 1; x++)
                updateFace(x, z);

        // Update faces together.
        getMesh().getEditableFaces().endBatchingUpdates();
    }

    /**
     * Handles the change of the size of the camera height-field grid.
     * @param oldX old number of squares in the x direction
     * @param oldZ old number of squares in the z direction
     * @param newX new number of squares in the x direction
     * @param newZ new number of squares in the z direction
     */
    public void onGridSizeChange(int oldX, int oldZ, int newX, int newZ) {
        if (newX < 0)
            throw new IllegalArgumentException("The x count cannot be less than zero! (Provided: " + newX + ")");
        if (newZ < 0)
            throw new IllegalArgumentException("The z count cannot be less than zero! (Provided: " + newZ + ")");

        if (oldX == newX && oldZ == newZ)
            return; // There's no difference between the old & new sizes.

        // Calculate the old & new specifications for the mesh.
        int oldQuadCount = ((oldX - 1) * (oldZ - 1));
        int newQuadCount = ((newX - 1) * (newZ - 1));
        int oldFaceCount = (oldQuadCount * 2);
        int newFaceCount = (newQuadCount * 2);
        int oldMeshIndexCount = (oldX * oldZ);
        int newMeshIndexCount = (newX * newZ);

        // Validate mesh state.
        // We do not use batch updates for the insertions / removals up there because we are likely to add / remove vertices and thus break .
        if (getMesh().getEditableVertices().isAnyBatchModeEnabled() || getMesh().getEditableTexCoords().isAnyBatchModeEnabled() || getMesh().getEditableFaces().isAnyBatchModeEnabled())
            throw new IllegalStateException("The mesh has a batch operation mode enabled, but updates to inserted values occur, which could break the array data.");

        // Update the buffer size for face mesh data.
        if (newFaceCount > oldFaceCount) {
            // If the size of the grid increased, add empty data to reach the correct size.
            getMesh().getEditableFaces().startBatchInsertion();

            // Insert dummy faces. (They'll be updated later in this method)
            for (int i = oldFaceCount; i < newFaceCount; i++)
                this.mainEntry.addFace(0, 0, 0, 0, 0, 0);

            // Finish batching.
            getMesh().getEditableFaces().endBatchInsertion();
        } else if (newFaceCount < oldFaceCount) {
            // If the size of the grid shrunk, remove the unused entries.
            getMesh().getEditableFaces().startBatchRemoval();

            // Remove faces.
            for (int i = newFaceCount; i < oldFaceCount; i++)
                this.mainEntry.removeFace(i);

            // Finish batching.
            getMesh().getEditableFaces().endBatchRemoval();
        }

        // Update the contents of the face buffer.
        // Do this before we update vertices and tex coords, so it doesn't trigger the warning about how the face array references data that got removed.
        updateAllFaces();

        // Update buffer size for vertex positions and texture UVs.
        if (newMeshIndexCount > oldMeshIndexCount) {
            // If the size of the grid increased, add empty data to reach the correct size.
            getMesh().getEditableVertices().startBatchInsertion();
            getMesh().getEditableTexCoords().startBatchInsertion();

            // Insert dummy values. (They'll be updated later in this method)
            for (int i = oldMeshIndexCount; i < newMeshIndexCount; i++) {
                this.mainEntry.addVertexValue(0, 0, 0);
                this.mainEntry.addTexCoordValue(0F, 0F);
            }

            // Finish batching
            getMesh().getEditableVertices().endBatchInsertion();
            getMesh().getEditableTexCoords().endBatchInsertion();
        } else if (newMeshIndexCount < oldMeshIndexCount) {
            // If the size of the grid shrunk, remove the unused entries.
            getMesh().getEditableVertices().startBatchRemoval();
            getMesh().getEditableTexCoords().startBatchRemoval();

            // Remove values.
            for (int i = newMeshIndexCount; i < oldMeshIndexCount; i++) {
                this.mainEntry.removeVertexValue(i);
                this.mainEntry.removeTexCoordValue(i);
            }

            // Finish batching.
            getMesh().getEditableVertices().endBatchRemoval();
            getMesh().getEditableTexCoords().endBatchRemoval();
        }

        // Update the contents of the buffers now that they are sized properly.
        updateVertices();
        updateTexCoords();
    }
}