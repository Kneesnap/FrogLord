package net.highwayfrogs.editor.gui.mesh.wrapper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.utils.logging.ILogger;

/**
 * Represents a 3D box part of a DynamicMeshDataEntry.
 * Created by Kneesnap on 1/8/2024.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MeshEntryBox {
    private final DynamicMeshDataEntry entry;
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    // Vertices:
    private int bottomUpLeftVertex;
    private int bottomUpRightVertex;
    private int bottomDownLeftVertex;
    private int bottomDownRightVertex;
    private int topUpLeftVertex;
    private int topUpRightVertex;
    private int topDownLeftVertex;
    private int topDownRightVertex;

    // UVs:
    private int uvTopLeft;
    private int uvTopRight;
    private int uvBottomLeft;
    private int uvBottomRight;

    // Faces:
    private int topFace1;
    private int topFace2;
    private int bottomFace1;
    private int bottomFace2;
    private int polygonFacingNegativeX1;
    private int polygonFacingNegativeX2;
    private int polygonFacingPositiveX1;
    private int polygonFacingPositiveX2;
    private int polygonFacingNegativeZ1;
    private int polygonFacingNegativeZ2;
    private int polygonFacingPositiveZ1;
    private int polygonFacingPositiveZ2;

    private void startVertexUpdates() {
        this.entry.getMesh().getEditableVertices().startBatchingUpdates();
    }

    private void stopVertexUpdates() {
        if (this.entry.getMesh().getEditableVertices().endBatchingUpdates()) {
            if (this.minX > this.maxX)
                getLogger().warning("The MeshEntryBox may not be visible because the minX was greater than the maxX. (MinX: " + this.minX + ", MaxX: " + this.maxX + ")");
            if (this.minY > this.maxY)
                getLogger().warning("The MeshEntryBox may not be visible because the minY was greater than the maxY. (MinY: " + this.minY + ", MaxY: " + this.maxY + ")");
            if (this.minZ > this.maxZ)
                getLogger().warning("The MeshEntryBox may not be visible because the minZ was greater than the maxZ. (MinZ: " + this.minZ + ", MaxZ: " + this.maxZ + ")");
        }
    }

    private ILogger getLogger() {
        return this.entry.getMesh().getLogger();
    }

    /**
     * Update the minimum X coordinate of the box.
     * @param newMinX new minimum X coordinate of the box
     */
    public void setMinX(double newMinX) {
        startVertexUpdates();
        float newX = (float) newMinX;
        this.entry.writeVertexX(this.bottomDownLeftVertex, newX);
        this.entry.writeVertexX(this.bottomUpLeftVertex, newX);
        this.entry.writeVertexX(this.topDownLeftVertex, newX);
        this.entry.writeVertexX(this.topUpLeftVertex, newX);
        this.minX = newMinX;
        stopVertexUpdates();
    }

    /**
     * Update the minimum Y coordinate of the box.
     * @param newMinY new minimum Y coordinate of the box
     */
    public void setMinY(double newMinY) {
        startVertexUpdates();
        float newY = (float) newMinY;
        this.entry.writeVertexY(this.topDownLeftVertex, newY);
        this.entry.writeVertexY(this.topDownRightVertex, newY);
        this.entry.writeVertexY(this.topUpLeftVertex, newY);
        this.entry.writeVertexY(this.topUpRightVertex, newY);
        this.minY = newMinY;
        stopVertexUpdates();
    }

    /**
     * Update the minimum Z coordinate of the box.
     * @param newMinZ new minimum Z coordinate of the box
     */
    public void setMinZ(double newMinZ) {
        startVertexUpdates();
        float newZ = (float) newMinZ;
        this.entry.writeVertexZ(this.bottomDownLeftVertex, newZ);
        this.entry.writeVertexZ(this.bottomDownRightVertex, newZ);
        this.entry.writeVertexZ(this.topDownLeftVertex, newZ);
        this.entry.writeVertexZ(this.topDownRightVertex, newZ);
        this.minZ = newMinZ;
        stopVertexUpdates();
    }

    /**
     * Update the maximum X coordinate of the box.
     * @param newMaxX new maximum X coordinate of the box
     */
    public void setMaxX(double newMaxX) {
        startVertexUpdates();
        float newX = (float) newMaxX;
        this.entry.writeVertexX(this.bottomDownRightVertex, newX);
        this.entry.writeVertexX(this.bottomUpRightVertex, newX);
        this.entry.writeVertexX(this.topDownRightVertex, newX);
        this.entry.writeVertexX(this.topUpRightVertex, newX);
        this.maxX = newMaxX;
        stopVertexUpdates();
    }

    /**
     * Update the maximum Y coordinate of the box.
     * @param newMaxY new maximum Y coordinate of the box
     */
    public void setMaxY(double newMaxY) {
        startVertexUpdates();
        float newY = (float) newMaxY;
        this.entry.writeVertexY(this.bottomDownLeftVertex, newY);
        this.entry.writeVertexY(this.bottomDownRightVertex, newY);
        this.entry.writeVertexY(this.bottomUpLeftVertex, newY);
        this.entry.writeVertexY(this.bottomUpRightVertex, newY);
        this.maxY = newMaxY;
        stopVertexUpdates();
    }

    /**
     * Update the maximum Z coordinate of the box.
     * @param newMaxZ new maximum Z coordinate of the box
     */
    public void setMaxZ(double newMaxZ) {
        startVertexUpdates();
        float newZ = (float) newMaxZ;
        this.entry.writeVertexZ(this.bottomUpLeftVertex, newZ);
        this.entry.writeVertexZ(this.bottomUpRightVertex, newZ);
        this.entry.writeVertexZ(this.topUpLeftVertex, newZ);
        this.entry.writeVertexZ(this.topUpRightVertex, newZ);
        this.maxZ = newMaxZ;
        stopVertexUpdates();
    }

    /**
     * Gets the center X coordinate of the box.
     */
    public double getCenterX() {
        return this.minX + ((this.maxX - this.minX) / 2);
    }

    /**
     * Update the new center of the box.
     * @param newCenterX new center X coordinate of the box
     */
    public void setCenterX(double newCenterX) {
        double widthOver2 = getWidth() / 2;

        startVertexUpdates();
        setMinX(newCenterX - widthOver2);
        setMaxX(newCenterX + widthOver2);
        stopVertexUpdates();
    }

    /**
     * Gets the center Y coordinate of the box.
     */
    public double getCenterY() {
        return this.minY + ((this.maxY - this.minY) / 2);
    }

    /**
     * Update the new center of the box.
     * @param newCenterY new center Y coordinate of the box
     */
    public void setCenterY(double newCenterY) {
        double heightOver2 = getHeight() / 2;

        startVertexUpdates();
        setMinY(newCenterY - heightOver2);
        setMaxY(newCenterY + heightOver2);
        stopVertexUpdates();
    }

    /**
     * Gets the center Z coordinate of the box.
     */
    public double getCenterZ() {
        return this.minZ + ((this.maxZ - this.minZ) / 2);
    }

    /**
     * Update the new center of the box.
     * @param newCenterZ new center Z coordinate of the box
     */
    public void setCenterZ(double newCenterZ) {
        double depthOver2 = getDepth() / 2;

        startVertexUpdates();
        setMinZ(newCenterZ - depthOver2);
        setMaxZ(newCenterZ + depthOver2);
        stopVertexUpdates();
    }

    /**
     * Gets the width of this box.
     */
    public double getWidth() {
        return (this.maxX - this.minX);
    }

    /**
     * Sets the width of this box.
     * @param newWidth the new width to apply
     */
    public void setWidth(double newWidth) {
        if (newWidth < 0 || !Double.isFinite(newWidth))
            throw new IllegalArgumentException("Cannot apply a width which is not a positive finite number. (Got: " + newWidth + ")");

        double centerX = getCenterX();
        double newWidthOver2 = newWidth / 2;
        startVertexUpdates();
        setMinX(centerX - newWidthOver2);
        setMaxX(centerX + newWidthOver2);
        stopVertexUpdates();
    }

    /**
     * Gets the height of this box.
     */
    public double getHeight() {
        return (this.maxY - this.minY);
    }

    /**
     * Sets the height of this box.
     * @param newHeight the new height to apply
     */
    public void setHeight(double newHeight) {
        if (newHeight < 0 || !Double.isFinite(newHeight))
            throw new IllegalArgumentException("Cannot apply a width which is not a positive finite number. (Got: " + newHeight + ")");

        double centerY = getCenterY();
        double newHeightOver2 = newHeight / 2;
        startVertexUpdates();
        setMinY(centerY - newHeightOver2);
        setMaxY(centerY + newHeightOver2);
        stopVertexUpdates();
    }

    /**
     * Gets the depth of this box.
     */
    public double getDepth() {
        return (this.maxZ - this.minZ);
    }

    /**
     * Sets the depth of this box.
     * @param newDepth the new depth to apply
     */
    public void setDepth(double newDepth) {
        if (newDepth < 0 || !Double.isFinite(newDepth))
            throw new IllegalArgumentException("Cannot apply a depth which is not a positive finite number. (Got: " + newDepth + ")");

        double centerZ = getCenterZ();
        double newDepthOver2 = newDepth / 2;
        startVertexUpdates();
        setMinZ(centerZ - newDepthOver2);
        setMaxZ(centerZ + newDepthOver2);
        stopVertexUpdates();
    }

    /**
     * Creates a box for the given mesh entry centered at the specified position.
     * @param entry   the entry to write mesh data to
     * @param x       the x position to place the center of the box
     * @param y       the y position to place the center of the box
     * @param z       the z position to place the center of the box
     * @param width   the length of the x-axis box dimension
     * @param height  the length of the y-axis box dimension
     * @param depth   the length of the z-axis box dimension
     * @param uvIndex the index of the uv to apply to the vertices
     */
    public static void createCenteredBox(DynamicMeshDataEntry entry, double x, double y, double z, double width, double height, double depth, int uvIndex) {
        createBox(entry, x - (width / 2), y - (height / 2), z - (depth / 2),
                x + (width / 2), y + (height / 2), z + (depth / 2), uvIndex, false);
    }

    /**
     * Creates a box for the given mesh entry centered at the specified position.
     * @param entry   the entry to write mesh data to
     * @param x       the x position to place the center of the box
     * @param y       the y position to place the center of the box
     * @param z       the z position to place the center of the box
     * @param width   the length of the x-axis box dimension
     * @param height  the length of the y-axis box dimension
     * @param depth   the length of the z-axis box dimension
     * @param uvTopLeftIndex the index of the texCoord to apply to a top left vertex in a quad
     * @param uvTopRightIndex the index of the texCoord to apply to a top right vertex in a quad
     * @param uvBottomLeftIndex the index of the texCoord to apply to a bottom left vertex in a quad
     * @param uvBottomRightIndex the index of the texCoord to apply to a bottom right vertex in a quad
     */
    public static void createCenteredBox(DynamicMeshDataEntry entry, double x, double y, double z, double width, double height, double depth, int uvTopLeftIndex, int uvTopRightIndex, int uvBottomLeftIndex, int uvBottomRightIndex) {
        createBox(entry, x - (width / 2), y - (height / 2), z - (depth / 2),
                x + (width / 2), y + (height / 2), z + (depth / 2),
                uvTopLeftIndex, uvTopRightIndex, uvBottomLeftIndex, uvBottomRightIndex, false);
    }

    /**
     * Creates a box for the given mesh entry centered at the specified position.
     * @param entry   the entry to write mesh data to
     * @param x       the x position to place the center of the box
     * @param y       the y position to place the center of the box
     * @param z       the z position to place the center of the box
     * @param width   the length of the x-axis box dimension
     * @param height  the length of the y-axis box dimension
     * @param depth   the length of the z-axis box dimension
     * @param uvIndex the index of the uv to apply to the vertices
     * @return object containing information about the created box
     */
    public static MeshEntryBox createCenteredBoxEntry(DynamicMeshDataEntry entry, double x, double y, double z, double width, double height, double depth, int uvIndex) {
        return createBox(entry, x - (width / 2), y - (height / 2), z - (depth / 2),
                x + (width / 2), y + (height / 2), z + (depth / 2), uvIndex, true);
    }

    /**
     * Creates a box for the given mesh entry centered at the specified position.
     * @param entry   the entry to write mesh data to
     * @param x       the x position to place the center of the box
     * @param y       the y position to place the center of the box
     * @param z       the z position to place the center of the box
     * @param width   the length of the x-axis box dimension
     * @param height  the length of the y-axis box dimension
     * @param depth   the length of the z-axis box dimension
     * @param uvTopLeftIndex the index of the texCoord to apply to a top left vertex in a quad
     * @param uvTopRightIndex the index of the texCoord to apply to a top right vertex in a quad
     * @param uvBottomLeftIndex the index of the texCoord to apply to a bottom left vertex in a quad
     * @param uvBottomRightIndex the index of the texCoord to apply to a bottom right vertex in a quad
     * @return object containing information about the created box
     */
    public static MeshEntryBox createCenteredBoxEntry(DynamicMeshDataEntry entry, double x, double y, double z, double width, double height, double depth, int uvTopLeftIndex, int uvTopRightIndex, int uvBottomLeftIndex, int uvBottomRightIndex) {
        return createBox(entry, x - (width / 2), y - (height / 2), z - (depth / 2),
                x + (width / 2), y + (height / 2), z + (depth / 2),
                uvTopLeftIndex, uvTopRightIndex, uvBottomLeftIndex, uvBottomRightIndex, true);
    }

    /**
     * Creates a box for the given mesh entry, conforming to the specified positions
     * @param entry   the entry to write mesh data to
     * @param minX    the x coordinate for the minimum corner of the box
     * @param minY    the y coordinate for the minimum corner of the box
     * @param minZ    the z coordinate for the minimum corner of the box
     * @param maxX    the x coordinate for the maximum corner of the box
     * @param maxY    the y coordinate for the maximum corner of the box
     * @param maxZ    the z coordinate for the maximum corner of the box
     * @param uvIndex the index of the uv to apply to the vertices
     */
    public static void createBox(DynamicMeshDataEntry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int uvIndex) {
        createBox(entry, minX, minY, minZ, maxX, maxY, maxZ, uvIndex, false);
    }

    /**
     * Creates a box for the given mesh entry, conforming to the specified positions
     * @param entry   the entry to write mesh data to
     * @param minX    the x coordinate for the minimum corner of the box
     * @param minY    the y coordinate for the minimum corner of the box
     * @param minZ    the z coordinate for the minimum corner of the box
     * @param maxX    the x coordinate for the maximum corner of the box
     * @param maxY    the y coordinate for the maximum corner of the box
     * @param maxZ    the z coordinate for the maximum corner of the box
     * @param uvIndex the index of the uv to apply to the vertices
     * @return object containing information about the created box
     */
    public static MeshEntryBox createBoxEntry(DynamicMeshDataEntry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int uvIndex) {
        return createBox(entry, minX, minY, minZ, maxX, maxY, maxZ, uvIndex, true);
    }

    /**
     * Creates a box for the given mesh entry, conforming to the specified positions
     * @param entry       the entry to write mesh data to
     * @param minX        the x coordinate for the minimum corner of the box
     * @param minY        the y coordinate for the minimum corner of the box
     * @param minZ        the z coordinate for the minimum corner of the box
     * @param maxX        the x coordinate for the maximum corner of the box
     * @param maxY        the y coordinate for the maximum corner of the box
     * @param maxZ        the z coordinate for the maximum corner of the box
     * @param uvIndex     the index of the uv to apply to the vertices
     * @param createEntry whether a MeshEntryBox should be created representing the box
     * @return object containing information about the created box
     */
    private static MeshEntryBox createBox(DynamicMeshDataEntry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int uvIndex, boolean createEntry) {
        return createBox(entry, minX, minY, minZ, maxX, maxY, maxZ, uvIndex, uvIndex, uvIndex, uvIndex, createEntry);
    }

    /**
     * Creates a box for the given mesh entry, conforming to the specified positions
     * @param entry       the entry to write mesh data to
     * @param minX        the x coordinate for the minimum corner of the box
     * @param minY        the y coordinate for the minimum corner of the box
     * @param minZ        the z coordinate for the minimum corner of the box
     * @param maxX        the x coordinate for the maximum corner of the box
     * @param maxY        the y coordinate for the maximum corner of the box
     * @param maxZ        the z coordinate for the maximum corner of the box
     * @param uvTopLeftIndex the index of the texCoord to apply to a top left vertex in a quad
     * @param uvTopRightIndex the index of the texCoord to apply to a top right vertex in a quad
     * @param uvBottomLeftIndex the index of the texCoord to apply to a bottom left vertex in a quad
     * @param uvBottomRightIndex the index of the texCoord to apply to a bottom right vertex in a quad
     * @param createEntry whether a MeshEntryBox should be created representing the box
     * @return object containing information about the created box
     */
    private static MeshEntryBox createBox(DynamicMeshDataEntry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int uvTopLeftIndex, int uvTopRightIndex, int uvBottomLeftIndex, int uvBottomRightIndex, boolean createEntry) {
        final float x0 = (float) Math.min(minX, maxX);
        final float y0 = (float) Math.max(minY, maxY);
        final float z0 = (float) Math.min(minZ, maxZ);
        final float x1 = (float) Math.max(minX, maxX);
        final float y1 = (float) Math.min(minY, maxY);
        final float z1 = (float) Math.max(minZ, maxZ);

        // Add vertices.
        entry.getMesh().getEditableVertices().startBatchInsertion();
        int bottomUpLeftVtx = entry.addVertexValue(x0, y0, z1);
        int bottomUpRightVtx = entry.addVertexValue(x1, y0, z1);
        int bottomDownLeftVtx = entry.addVertexValue(x0, y0, z0);
        int bottomDownRightVtx = entry.addVertexValue(x1, y0, z0);
        int topUpLeftVtx = entry.addVertexValue(x0, y1, z1);
        int topUpRightVtx = entry.addVertexValue(x1, y1, z1);
        int topDownLeftVtx = entry.addVertexValue(x0, y1, z0);
        int topDownRightVtx = entry.addVertexValue(x1, y1, z0);
        entry.getMesh().getEditableVertices().endBatchInsertion();

        // JavaFX uses counter-clockwise winding order.
        entry.getMesh().getEditableFaces().startBatchInsertion();
        int topFace1 = entry.addFace(topDownLeftVtx, uvBottomLeftIndex, topUpRightVtx, uvTopRightIndex, topUpLeftVtx, uvTopLeftIndex);
        int topFace2 = entry.addFace(topDownLeftVtx, uvBottomLeftIndex, topDownRightVtx, uvBottomRightIndex, topUpRightVtx, uvTopRightIndex);
        int bottomFace1 = entry.addFace(bottomUpRightVtx, uvBottomRightIndex, bottomDownLeftVtx, uvTopLeftIndex, bottomUpLeftVtx, uvBottomLeftIndex);
        int bottomFace2 = entry.addFace(bottomUpRightVtx, uvBottomRightIndex, bottomDownRightVtx, uvTopRightIndex, bottomDownLeftVtx, uvTopLeftIndex);
        int polygonFacingNegativeX1 = entry.addFace(bottomUpLeftVtx, uvBottomLeftIndex, topDownLeftVtx, uvTopRightIndex, topUpLeftVtx, uvTopLeftIndex);
        int polygonFacingNegativeX2 = entry.addFace(bottomUpLeftVtx, uvBottomLeftIndex, bottomDownLeftVtx, uvBottomRightIndex, topDownLeftVtx, uvTopRightIndex);
        int polygonFacingPositiveX1 = entry.addFace(bottomDownRightVtx, uvBottomLeftIndex, topUpRightVtx, uvTopRightIndex, topDownRightVtx, uvTopLeftIndex); // Polygon Facing Positive X #1
        int polygonFacingPositiveX2 = entry.addFace(bottomDownRightVtx, uvBottomLeftIndex, bottomUpRightVtx, uvBottomRightIndex, topUpRightVtx, uvTopRightIndex); // Polygon Facing Positive X #2
        int polygonFacingNegativeZ1 = entry.addFace(bottomDownLeftVtx, uvBottomLeftIndex, topDownRightVtx, uvTopRightIndex, topDownLeftVtx, uvTopLeftIndex); // Polygon Facing Negative Z #1
        int polygonFacingNegativeZ2 = entry.addFace(bottomDownLeftVtx, uvBottomLeftIndex, bottomDownRightVtx, uvBottomRightIndex, topDownRightVtx, uvTopRightIndex); // Polygon Facing Negative Z #2
        int polygonFacingPositiveZ1 = entry.addFace(bottomUpRightVtx, uvBottomLeftIndex, topUpLeftVtx, uvTopRightIndex, topUpRightVtx, uvTopLeftIndex); // Polygon Facing Positive Z #1
        int polygonFacingPositiveZ2 = entry.addFace(bottomUpRightVtx, uvBottomLeftIndex, bottomUpLeftVtx, uvBottomRightIndex, topUpLeftVtx, uvTopRightIndex); // Polygon Facing Positive Z #2
        entry.getMesh().getEditableFaces().endBatchInsertion();

        // Create resulting container with box information.
        if (createEntry) {
            MeshEntryBox newEntry = new MeshEntryBox(entry);

            // Positions
            newEntry.minX = x0;
            newEntry.minY = y1;
            newEntry.minZ = z0;
            newEntry.maxX = x1;
            newEntry.maxY = y0;
            newEntry.maxZ = z1;

            // Vertices
            int vertexStartIndex = entry.getVertexStartIndex();
            newEntry.bottomUpLeftVertex = bottomUpLeftVtx - vertexStartIndex;
            newEntry.bottomUpRightVertex = bottomUpRightVtx - vertexStartIndex;
            newEntry.bottomDownLeftVertex = bottomDownLeftVtx - vertexStartIndex;
            newEntry.bottomDownRightVertex = bottomDownRightVtx - vertexStartIndex;
            newEntry.topUpLeftVertex = topUpLeftVtx - vertexStartIndex;
            newEntry.topUpRightVertex = topUpRightVtx - vertexStartIndex;
            newEntry.topDownLeftVertex = topDownLeftVtx - vertexStartIndex;
            newEntry.topDownRightVertex = topDownRightVtx - vertexStartIndex;

            // UVs
            int uvStartIndex = entry.getTexCoordStartIndex();
            newEntry.uvTopLeft = uvTopLeftIndex - uvStartIndex;
            newEntry.uvTopRight = uvTopRightIndex - uvStartIndex;
            newEntry.uvBottomLeft = uvBottomLeftIndex - uvStartIndex;
            newEntry.uvBottomRight = uvBottomRightIndex - uvStartIndex;

            // Faces:
            int faceStartIndex = entry.getFaceStartIndex();
            newEntry.topFace1 = topFace1 - faceStartIndex;
            newEntry.topFace2 = topFace2 - faceStartIndex;
            newEntry.bottomFace1 = bottomFace1 - faceStartIndex;
            newEntry.bottomFace2 = bottomFace2 - faceStartIndex;
            newEntry.polygonFacingNegativeX1 = polygonFacingNegativeX1 - faceStartIndex;
            newEntry.polygonFacingNegativeX2 = polygonFacingNegativeX2 - faceStartIndex;
            newEntry.polygonFacingPositiveX1 = polygonFacingPositiveX1 - faceStartIndex;
            newEntry.polygonFacingPositiveX2 = polygonFacingPositiveX2 - faceStartIndex;
            newEntry.polygonFacingNegativeZ1 = polygonFacingNegativeZ1 - faceStartIndex;
            newEntry.polygonFacingNegativeZ2 = polygonFacingNegativeZ2 - faceStartIndex;
            newEntry.polygonFacingPositiveZ1 = polygonFacingPositiveZ1 - faceStartIndex;
            newEntry.polygonFacingPositiveZ2 = polygonFacingPositiveZ2 - faceStartIndex;
            return newEntry;
        }

        return null;
    }
}