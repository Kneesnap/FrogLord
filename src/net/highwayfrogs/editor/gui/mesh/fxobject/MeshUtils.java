package net.highwayfrogs.editor.gui.mesh.fxobject;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains static utilities for writing mesh data to dynamic mesh data entries.
 * Created by Kneesnap on 1/3/2024.
 */
public class MeshUtils {
    /**
     * Search the provided node recursively for a SubScene.
     * @param root node to start searching from
     * @return The identified SubScene 3D group, if there was one.
     */
    public static Group getSubSceneGroup(Parent root) {
        if (root == null)
            return null;

        List<Parent> nodesToVisit = new ArrayList<>();
        nodesToVisit.add(root);

        while (nodesToVisit.size() > 0) {
            Parent parent = nodesToVisit.remove(nodesToVisit.size() - 1);
            for (Node node : parent.getChildrenUnmodifiable()) {
                if (node instanceof SubScene && ((SubScene) node).getRoot() instanceof Group)
                    return (Group) ((SubScene) node).getRoot();
                if (node instanceof Parent)
                    nodesToVisit.add((Parent) node);
            }
        }

        return null;
    }

    /**
     * Creates a box for the given mesh entry centered at the specified position.
     * @param entry   the entry to write mesh data to
     * @param x       the x position to place the center of the box
     * @param y       the y position to place the center of the box
     * @param z       the z position to place the center of the box
     * @param length  the length of the x-axis box dimension
     * @param width   the length of the z-axis box dimension
     * @param height  the length of the y-axis box dimension
     * @param uvIndex the index of the uv to apply to the vertices
     */
    public static void createCenteredBoxWithDimensions(DynamicMeshDataEntry entry, double x, double y, double z, double length, double width, double height, int uvIndex) {
        createBox(entry, x - (length / 2), y - (height / 2), z - (width / 2),
                x + (length / 2), y + (height / 2), z + (width / 2), uvIndex);
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
        final float x0 = (float) Math.min(minX, maxX);
        final float y0 = (float) Math.max(minY, maxY);
        final float z0 = (float) Math.min(minZ, maxZ);
        final float x1 = (float) Math.max(minX, maxX);
        final float y1 = (float) Math.min(minY, maxY);
        final float z1 = (float) Math.max(minZ, maxZ);

        // Add vertices.
        int bottomUpLeftVtx = entry.addVertexValue(x0, y0, z1);
        int bottomUpRightVtx = entry.addVertexValue(x1, y0, z1);
        int bottomDownLeftVtx = entry.addVertexValue(x0, y0, z0);
        int bottomDownRightVtx = entry.addVertexValue(x1, y0, z0);
        int topUpLeftVtx = entry.addVertexValue(x0, y1, z1);
        int topUpRightVtx = entry.addVertexValue(x1, y1, z1);
        int topDownLeftVtx = entry.addVertexValue(x0, y1, z0);
        int topDownRightVtx = entry.addVertexValue(x1, y1, z0);

        // JavaFX uses counter-clockwise winding order.
        entry.addFace(topDownLeftVtx, uvIndex, topUpRightVtx, uvIndex, topUpLeftVtx, uvIndex); // Top Face #1
        entry.addFace(topDownLeftVtx, uvIndex, topDownRightVtx, uvIndex, topUpRightVtx, uvIndex); // Top Face #2
        entry.addFace(bottomUpRightVtx, uvIndex, bottomDownLeftVtx, uvIndex, bottomUpLeftVtx, uvIndex); // Bottom Face #1
        entry.addFace(bottomUpRightVtx, uvIndex, bottomDownRightVtx, uvIndex, bottomDownLeftVtx, uvIndex); // Bottom Face #2
        entry.addFace(bottomUpLeftVtx, uvIndex, topDownLeftVtx, uvIndex, topUpLeftVtx, uvIndex); // Polygon Facing Negative X #1
        entry.addFace(bottomUpLeftVtx, uvIndex, bottomDownLeftVtx, uvIndex, topDownLeftVtx, uvIndex); // Polygon Facing Negative X #2
        entry.addFace(bottomDownRightVtx, uvIndex, topUpRightVtx, uvIndex, topDownRightVtx, uvIndex); // Polygon Facing Positive X #1
        entry.addFace(bottomDownRightVtx, uvIndex, bottomUpRightVtx, uvIndex, topUpRightVtx, uvIndex); // Polygon Facing Positive X #2
        entry.addFace(bottomDownLeftVtx, uvIndex, topDownRightVtx, uvIndex, topDownLeftVtx, uvIndex); // Polygon Facing Negative Z #1
        entry.addFace(bottomDownLeftVtx, uvIndex, bottomDownRightVtx, uvIndex, topDownRightVtx, uvIndex); // Polygon Facing Negative Z #2
        entry.addFace(bottomUpRightVtx, uvIndex, topUpLeftVtx, uvIndex, topUpRightVtx, uvIndex); // Polygon Facing Positive Z #1
        entry.addFace(bottomUpRightVtx, uvIndex, bottomUpLeftVtx, uvIndex, topUpLeftVtx, uvIndex); // Polygon Facing Positive Z #2
    }

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
    private static void addPyramidFaces(DynamicMeshDataEntry entry, boolean invertWinding, int baseTopLeftVtx, int baseTopRightVtx, int baseBottomLeftVtx, int baseBottomRightVtx, int pyramidTipVtx, int uvIndex) {
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