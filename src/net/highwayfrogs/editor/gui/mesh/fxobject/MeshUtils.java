package net.highwayfrogs.editor.gui.mesh.fxobject;

import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;

/**
 * Contains static utilities for writing mesh data to dynamic mesh data entries.
 * Created by Kneesnap on 1/3/2024.
 */
public class MeshUtils {
    /**
     * Connects vertices to form a pyramid for the given mesh data entry.
     * For the purposes of variable names and concepts, is assumed that the hypothetical camera forward vector is parallel to the vector the pyramid is facing with its vertex.
     * The faces are added in the following order:
     * - Bottom Right Triangle of Base
     * - Top Left Triangle of Base
     * - Pyramid Triangle w/ Normal Facing Upward (From the perspective of a pyramid aligned to the Z axis pointing in the positive direction)
     * - Pyramid Triangle w/ Normal Downward Upward (From the perspective of a pyramid aligned to the Z axis pointing in the positive direction)
     * - Pyramid Triangle w/ Normal Towards Positive Z (From the perspective of a pyramid aligned to the Z axis pointing in the positive direction)
     * - Pyramid Triangle w/ Normal Towards Negative Z (From the perspective of a pyramid aligned to the Z axis pointing in the positive direction)
     * @param entry              entry to add the pyramid faces to.
     * @param invertWinding      if the winding order should be inverted
     * @param baseTopLeftVtx     the top left vertex id of the base from the perspective of the hypothetical camera
     * @param baseTopRightVtx    the top right vertex id of the base from the perspective of the hypothetical camera
     * @param baseBottomLeftVtx  the bottom left vertex id of the base from the perspective of the hypothetical camera
     * @param baseBottomRightVtx the bottom right vertex id of the base from the perspective of the hypothetical camera
     * @param pyramidTipVtx      the vertex id representing the tip of the pyramid
     * @param uvIndex            the index of the texCoord to apply to all faces.
     */
    public static void addPyramidFaces(DynamicMeshDataEntry entry, boolean invertWinding, int baseTopLeftVtx, int baseTopRightVtx, int baseBottomLeftVtx, int baseBottomRightVtx, int pyramidTipVtx, int uvIndex) {
        // If the pyramid has height less than zero, the pyramid is facing the inverse direction.
        // And as such, the winding order should be inverted.
        // JavaFX uses counter-clockwise winding order.
        if (invertWinding) {
            // Flip winding order.
            entry.addFace(baseBottomLeftVtx, uvIndex, baseTopRightVtx, uvIndex, baseTopLeftVtx, uvIndex); // Base Face #1
            entry.addFace(baseBottomLeftVtx, uvIndex, baseBottomRightVtx, uvIndex, baseTopRightVtx, uvIndex); // Base Face #2
            entry.addFace(baseTopLeftVtx, uvIndex, baseTopRightVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Up
            entry.addFace(baseBottomRightVtx, uvIndex, baseBottomLeftVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Down
            entry.addFace(baseBottomLeftVtx, uvIndex, baseTopLeftVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Positive Z
            entry.addFace(baseTopRightVtx, uvIndex, baseBottomRightVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Negative Z
        } else {
            entry.addFace(baseBottomLeftVtx, uvIndex, baseTopLeftVtx, uvIndex, baseTopRightVtx, uvIndex); // Base Face #1
            entry.addFace(baseBottomLeftVtx, uvIndex, baseTopRightVtx, uvIndex, baseBottomRightVtx, uvIndex); // Base Face #2
            entry.addFace(baseTopRightVtx, uvIndex, baseTopLeftVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Up
            entry.addFace(baseBottomLeftVtx, uvIndex, baseBottomRightVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Down
            entry.addFace(baseTopLeftVtx, uvIndex, baseBottomLeftVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Positive Z
            entry.addFace(baseBottomRightVtx, uvIndex, baseTopRightVtx, uvIndex, pyramidTipVtx, uvIndex); // Pyramid Side Facing Negative Z
        }
    }

    /**
     * Creates a pyramid aligned to the x-axis, with the base centered at the given position
     * @param entry   the entry to write mesh data to
     * @param x       the x coordinate representing the center of the pyramid base
     * @param y       the y coordinate representing the center of the pyramid base
     * @param z       the z coordinate representing the center of the pyramid base
     * @param length  the length of the pyramid base (z-axis)
     * @param width   the width of the pyramid base (y-axis)
     * @param height  the height of the pyramid (x-axis)
     * @param uvIndex the index of the uv to apply to the vertices
     */
    public static void createXAxisPyramid(DynamicMeshDataEntry entry, double x, double y, double z, double length, double width, double height, int uvIndex) {
        final double lengthOver2 = Math.abs(length / 2);
        final double widthOver2 = Math.abs(width / 2);
        float baseMinY = (float) (y - widthOver2);
        float baseMaxY = (float) (y + widthOver2);
        float baseMinZ = (float) (z - lengthOver2);
        float baseMaxZ = (float) (z + lengthOver2);
        float baseX = (float) x;
        float tipX = (float) (x + height);

        // Add vertices.
        // The names here assume a hypothetical a positive height, with a camera facing positive, aligned to the X axis.
        int baseTopLeftVtx = entry.addVertexValue(baseX, baseMaxY, baseMaxZ);
        int baseTopRightVtx = entry.addVertexValue(baseX, baseMaxY, baseMinZ);
        int baseBottomLeftVtx = entry.addVertexValue(baseX, baseMinY, baseMaxZ);
        int baseBottomRightVtx = entry.addVertexValue(baseX, baseMinY, baseMinZ);
        int pyramidTipVtx = entry.addVertexValue(tipX, (float) y, (float) z);

        addPyramidFaces(entry, height < 0, baseTopLeftVtx, baseTopRightVtx, baseBottomLeftVtx, baseBottomRightVtx, pyramidTipVtx, uvIndex);
    }

    /**
     * Creates a pyramid aligned to the y-axis, with the base centered at the given position
     * @param entry   the entry to write mesh data to
     * @param x       the x coordinate representing the center of the pyramid base
     * @param y       the y coordinate representing the center of the pyramid base
     * @param z       the z coordinate representing the center of the pyramid base
     * @param length  the length of the pyramid base (x-axis)
     * @param width   the width of the pyramid base (z-axis)
     * @param height  the height of the pyramid (y-axis)
     * @param uvIndex the index of the uv to apply to the vertices
     */
    public static void createYAxisPyramid(DynamicMeshDataEntry entry, double x, double y, double z, double length, double width, double height, int uvIndex) {
        final double lengthOver2 = Math.abs(length / 2);
        final double widthOver2 = Math.abs(width / 2);
        float baseMinX = (float) (x - lengthOver2);
        float baseMaxX = (float) (x + lengthOver2);
        float baseMinZ = (float) (z - widthOver2);
        float baseMaxZ = (float) (z + widthOver2);
        float baseY = (float) y;
        float tipY = (float) (y - height);

        // Add vertices.
        // The names here assume a hypothetical a positive height, with a camera facing positive, aligned to the y-axis.
        int baseTopLeftVtx = entry.addVertexValue(baseMinX, baseY, baseMinZ);
        int baseTopRightVtx = entry.addVertexValue(baseMaxX, baseY, baseMinZ);
        int baseBottomLeftVtx = entry.addVertexValue(baseMinX, baseY, baseMaxZ);
        int baseBottomRightVtx = entry.addVertexValue(baseMaxX, baseY, baseMaxZ);
        int pyramidTipVtx = entry.addVertexValue((float) x, tipY, (float) z);

        addPyramidFaces(entry, height > 0, baseTopLeftVtx, baseTopRightVtx, baseBottomLeftVtx, baseBottomRightVtx, pyramidTipVtx, uvIndex);
    }

    /**
     * Creates a pyramid aligned to the z-axis, with the base centered at the given position
     * @param entry   the entry to write mesh data to
     * @param x       the x coordinate representing the center of the pyramid base
     * @param y       the y coordinate representing the center of the pyramid base
     * @param z       the z coordinate representing the center of the pyramid base
     * @param length  the length of the pyramid base (x-axis)
     * @param width   the width of the pyramid base (y-axis)
     * @param height  the height of the pyramid (z-axis)
     * @param uvIndex the index of the uv to apply to the vertices
     */
    public static void createZAxisPyramid(DynamicMeshDataEntry entry, double x, double y, double z, double length, double width, double height, int uvIndex) {
        final double lengthOver2 = Math.abs(length / 2);
        final double widthOver2 = Math.abs(width / 2);
        float baseMinX = (float) (x - lengthOver2);
        float baseMaxX = (float) (x + lengthOver2);
        float baseMinY = (float) (y - widthOver2);
        float baseMaxY = (float) (y + widthOver2);
        float baseZ = (float) z;
        float tipZ = (float) (z + height);

        // Add vertices.
        // The names here assume a hypothetical a positive height, with a camera facing positive, aligned to the z-axis.
        int baseTopLeftVtx = entry.addVertexValue(baseMinX, baseMinY, baseZ);
        int baseTopRightVtx = entry.addVertexValue(baseMaxX, baseMinY, baseZ);
        int baseBottomLeftVtx = entry.addVertexValue(baseMinX, baseMaxY, baseZ);
        int baseBottomRightVtx = entry.addVertexValue(baseMaxX, baseMaxY, baseZ);
        int pyramidTipVtx = entry.addVertexValue((float) x, (float) y, tipZ);

        addPyramidFaces(entry, height < 0, baseTopLeftVtx, baseTopRightVtx, baseBottomLeftVtx, baseBottomRightVtx, pyramidTipVtx, uvIndex);
    }
}