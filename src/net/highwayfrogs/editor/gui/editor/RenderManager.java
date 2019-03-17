package net.highwayfrogs.editor.gui.editor;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.map.path.data.ArcSegment;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.utils.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Manages rendering and rendering resources / assets.
 * Created by AndyEder on 2/21/2019.
 */
@Getter
public class RenderManager
{
    private Group root = null;
    private Map<String, List<Node>> displayListCache = new HashMap<>();

    //>>

    public RenderManager() { }

    /**
     * Sets the root scene graph render node.
     * @param root  The root scene graph node.
     */
    public void setRenderRoot(Group root)
    {
        this.root = root;
    }

    /**
     * Sets the root scene graph render node.
     * @return The root scene graph node.
     */
    public Group getRenderRoot()
    {
        return this.root;
    }

    /**
     * Check to see if the specified display list with the specified ID already exists in the cache.
     * @param listID    The ID (key) for the display list.
     * @return          True if the specified list exists, else false
     */
    public boolean displayListExists(String listID)
    {
        return displayListCache.containsKey(listID);
    }

    /**
     * Adds a new display list with the specified ID.
     * @param listID    The ID (key) for the display list.
     */
    public void addDisplayList(String listID)
    {
        if (!displayListCache.containsKey(listID))
        {
            // Create a new list keyed on the provided ID
            displayListCache.put(listID, new LinkedList<>());
        }
        else
        {
            // A list already exists keyed on the provided ID
            throw new RuntimeException("RenderManager::addDisplayList() - " + listID + " already exists!");
        }
    }

    /**
     * Removes an existing display list with the specified ID.
     * @param listID    The ID (key) for the display list.
     */
    public void removeDisplayList(String listID)
    {
        if (displayListCache.containsKey(listID))
        {
            final List<Node> items = displayListCache.get(listID);

            // Remove items from display and clear the list
            this.root.getChildren().removeAll(items);
            items.clear();

            // Remove from the cache
            displayListCache.remove(listID);
        }
        else
        {
            // The specified list does not exist
            throw new RuntimeException("RenderManager::removeDisplayList() - " + listID + " does not exist!");
        }
    }

    /**
     * Clears and removes all display lists.
     */
    public void removeAllDisplayLists()
    {
        // Clear items from all display lists
        this.clearAllDisplayLists();

        // Clear the cache
        displayListCache.clear();
    }

    /**
     * Clears the specified display list.
     * @param listID    The ID (key) for the display list.
     */
    public void clearDisplayList(String listID)
    {
        if (displayListCache.containsKey(listID))
        {
            final List<Node> items = displayListCache.get(listID);

            // Remove items from display and clear the list
            this.root.getChildren().removeAll(items);
            items.clear();
        }
        else
        {
            // The specified list does not exist
            throw new RuntimeException("RenderManager::clearDisplayList() - " + listID + " does not exist!");
        }
    }

    /**
     * Clears all display lists.
     */
    public void clearAllDisplayLists()
    {
        // Clear items from each display list
        displayListCache.forEach((key, items) ->
        {
            // Remove items from display
            this.root.getChildren().removeAll(items);
            items.clear();
        });
    }

    /**
     * Shows or hides all items in the specified display list.
     * @param listID    The ID of the display list.
     * @param show      True to show items, false to hide.
     */
    public void showDisplayList(String listID, boolean show)
    {
        if (displayListCache.containsKey(listID))
        {
            displayListCache.get(listID).forEach(node -> node.setVisible(show));
        }
        else
        {
            throw new RuntimeException("RenderManager::showDisplayList() - " + listID + " does not exist!");
        }
    }

    /**
     * Adds a bounding box to the specified display list.
     * @param listID    The ID of the display list.
     * @param bbox      The bounding box to add.
     */
    public void addBoundingBox(String listID, Box bbox)
    {
        if (displayListCache.containsKey(listID))
        {
            displayListCache.get(listID).add(bbox);
            this.root.getChildren().add(bbox);
        }
        else
        {
            throw new RuntimeException("RenderManager::addBoundingBox() - " + listID + " does not exist!");
        }
    }

    /**
     * Removes a bounding box from the specified display list.
     * @param listID    The ID of the display list.
     * @param bbox      The bounding box to remove.
     */
    public void removeBoundingBox(String listID, Box bbox)
    {
        if (displayListCache.containsKey(listID))
        {
            this.root.getChildren().remove(bbox);
            displayListCache.get(listID).remove(bbox);
        }
        else
        {
            throw new RuntimeException("RenderManager::removeBoundingBox() - " + listID + " does not exist!");
        }
    }

    /**
     * Adds an axis-aligned bounding box.
     * @param listID    The display list ID.
     * @param minX      The minimum x-coordinate.
     * @param minY      The minimum y-coordinate.
     * @param minZ      The minimum z-coordinate.
     * @param maxX      The maximum x-coordinate.
     * @param maxY      The maximum y-coordinate.
     * @param maxZ      The maximum z-coordinate.
     * @return          The newly created/added bounding box
     */
    public Box addBoundingBoxFromMinMax(String listID, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, PhongMaterial material, boolean useWireframe)
    {
        if (displayListCache.containsKey(listID))
        {
            final double x0 = Math.min(minX, maxX);
            final double x1 = Math.max(minX, maxX);
            final double y0 = Math.min(minY, maxY);
            final double y1 = Math.max(minY, maxY);
            final double z0 = Math.min(minZ, maxZ);
            final double z1 = Math.max(minZ, maxZ);

            final double x = (x0 + x1) * 0.5;
            final double y = (y0 + y1) * 0.5;
            final double z = (z0 + z1) * 0.5;

            final double w = (x1 - x0);
            final double h = (y1 - y0);
            final double d = (z1 - z0);

            return this.addBoundingBoxCenteredWithDimensions(listID, x, y, z, w, h, d, material, useWireframe);
        }
        else
        {
            throw new RuntimeException("RenderManager::addBoundingBoxFromMinMax() - " + listID + " does not exist!");
        }
    }

    /**
     * Adds an axis-aligned bounding box.
     * @param listID    The display list ID.
     * @param x         The x-coordinate defining the center of the box.
     * @param y         The y-coordinate defining the center of the box.
     * @param z         The z-coordinate defining the center of the box.
     * @param width     The width (along x-axis).
     * @param height    The height (along y-axis).
     * @param depth     The depth (along z-axis).
     * @return          The newly created/added bounding box
     */
    public Box addBoundingBoxCenteredWithDimensions(String listID, double x, double y, double z, double width, double height, double depth, PhongMaterial material, boolean useWireframe)
    {
        if (displayListCache.containsKey(listID))
        {
            Box axisAlignedBoundingBox = new Box(width, height, depth);

            axisAlignedBoundingBox.setMaterial(material);
            axisAlignedBoundingBox.setDrawMode(useWireframe ? DrawMode.LINE : DrawMode.FILL);
            axisAlignedBoundingBox.setCullFace(CullFace.BACK);
            axisAlignedBoundingBox.getTransforms().addAll(new Translate(x, y, z));
            axisAlignedBoundingBox.setMouseTransparent(true);

            displayListCache.get(listID).add(axisAlignedBoundingBox);
            this.root.getChildren().add(axisAlignedBoundingBox);

            return axisAlignedBoundingBox;
        }
        else
        {
            throw new RuntimeException("RenderManager::addBoundingBoxCenteredWithDimensions() - " + listID + " does not exist!");
        }
    }

    /**
     * Adds a cylindrical representation of a 3D line.
     * @param listID    The display list ID.
     * @param x0        The x-coordinate defining the start of the line segment.
     * @param y0        The y-coordinate defining the start of the line segment.
     * @param z0        The z-coordinate defining the start of the line segment.
     * @param x1        The x-coordinate defining the end of the line segment.
     * @param y1        The y-coordinate defining the end of the line segment.
     * @param z1        The z-coordinate defining the end of the line segment.
     * @param radius    The radius of the cylinder (effectively the 'width' of the line).
     * @param material  The material used to render the line segment.
     * @param showStart Whether or not to display a sphere at the start of the line segment.
     * @param showEnd   Whether or not to display a sphere at the end of the line segment.
     * @return          The newly created/added cylinder (cylinder primitive only!)
     */
    public Cylinder addLineSegment(String listID, double x0, double y0, double z0, double x1, double y1, double z1, double radius, PhongMaterial material, boolean showStart, boolean showEnd)
    {
        if (displayListCache.containsKey(listID))
        {
            final Point3D yAxis = new Point3D(0.0, 1.0, 0.0);
            final Point3D p0 = new Point3D(x0, y0, z0);
            final Point3D p1 = new Point3D(x1, y1, z1);
            final Point3D diff = p1.subtract(p0);
            final double length = diff.magnitude();

            final Point3D mid = p1.midpoint(p0);
            final Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

            final Point3D axisOfRotation = diff.crossProduct(yAxis);
            final double angle = Math.acos(diff.normalize().dotProduct(yAxis));
            final Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

            Cylinder line = new Cylinder(radius, length, 3);
            line.setMaterial(material);
            line.setDrawMode(DrawMode.FILL);
            line.setCullFace(CullFace.BACK);
            line.setMouseTransparent(true);
            line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
            displayListCache.get(listID).add(line);
            this.root.getChildren().add(line);

            if (showStart)
            {
                Sphere sphStart = addSphere(listID, x0, y0, z0, radius * 5.0, Utils.makeSpecialMaterial(Color.GREEN), false);
            }
            if (showEnd)
            {
                Sphere sphEnd = addSphere(listID, x1, y1, z1, radius * 5.0, Utils.makeSpecialMaterial(Color.RED), false);
            }

            return line;
        }
        else
        {
            throw new RuntimeException("RenderManager::addLine() - " + listID + " does not exist!");
        }
    }

    /**
     * Adds a sphere.
     * @param listID        The display list ID.
     * @param x0            The x-coordinate defining the center of the sphere.
     * @param y0            The y-coordinate defining the center of the sphere.
     * @param z0            The z-coordinate defining the center of the sphere.
     * @param radius        The radius of the cylinder.
     * @param material      The material used to render the sphere.
     * @param useWireframe  Whether or not to display the sphere as a wireframe.
     * @return              The newly created/added sphere
     */
    public Sphere addSphere(String listID, double x0, double y0, double z0, double radius, PhongMaterial material, boolean useWireframe)
    {
        if (displayListCache.containsKey(listID))
        {
            Sphere sph0 = new Sphere(radius, 8);
            sph0.setMaterial(material);
            sph0.setDrawMode(useWireframe ? DrawMode.LINE : DrawMode.FILL);
            sph0.setCullFace(CullFace.BACK);
            sph0.setMouseTransparent(true);
            sph0.getTransforms().addAll(new Translate(x0, y0, z0));

            displayListCache.get(listID).add(sph0);
            this.root.getChildren().add(sph0);

            return sph0;
        }
        else
        {
            throw new RuntimeException("RenderManager::addSphere() - " + listID + " does not exist!");
        }
    }

    /**
     * Adds a list of paths to the specified display list (collection of line, arc and spline segments).
     * @param listID            The display list ID.
     * @param pathList          The paths to realise.
     * @param materialLine      The material to use for rendering line segments.
     * @param materialArc       The material to use for rendering arc segments.
     * @param materialSpline    The material to use for rendering spline segments.
     */
    public void addPaths(String listID, List<Path> pathList, PhongMaterial materialLine, PhongMaterial materialArc, PhongMaterial materialSpline)
    {
        if (displayListCache.containsKey(listID))
        {
            // We will use pathInfo to 'step' along the paths and to build the geometry
            PathInfo pathInfo = new PathInfo();

            // Track indices (ID's) of paths and segments so that we can feed them in via the PathInfo object
            int pathIndex = 0;
            int segmentIndex;

            double x0, y0, z0;
            double x1, y1, z1;

            for (Path path : pathList)
            {
                segmentIndex = 0;

                for (PathSegment segment : path.getSegments())
                {
                    pathInfo.setPathId(pathIndex);
                    pathInfo.setSegmentId(segmentIndex);

                    if (segment.getType() == PathType.LINE)
                    {
                        pathInfo.setSegmentDistance(0);
                        Vector vec0 = path.evaluatePosition(pathInfo);

                        pathInfo.setSegmentDistance(segment.getLength());
                        Vector vec1 = path.evaluatePosition(pathInfo);

                        x0 = vec0.getFloatX();
                        y0 = vec0.getFloatY();
                        z0 = vec0.getFloatZ();

                        x1 = vec1.getFloatX();
                        y1 = vec1.getFloatY();
                        z1 = vec1.getFloatZ();

                        this.addLineSegment(listID, x0, y0, z0, x1, y1, z1, 0.20, materialLine, true, true);
                    }
                    else if (segment.getType() == PathType.ARC)
                    {
                        final int stepSize = Math.min(32, segment.getLength());
                        final int numSteps = 1 + (segment.getLength() / stepSize);

                        for (int step=0; step<numSteps; ++step)
                        {
                            pathInfo.setSegmentDistance(step * stepSize);
                            Vector vec0 = path.evaluatePosition(pathInfo);

                            pathInfo.setSegmentDistance(Math.min((step + 1) * stepSize, segment.getLength()));
                            Vector vec1 = path.evaluatePosition(pathInfo);

                            x0 = vec0.getFloatX();
                            y0 = vec0.getFloatY();
                            z0 = vec0.getFloatZ();

                            x1 = vec1.getFloatX();
                            y1 = vec1.getFloatY();
                            z1 = vec1.getFloatZ();

                            if (!((x0 == x1) && (y0 == y1) && (z0 == z1)))
                            {
                                this.addLineSegment(listID, x0, y0, z0, x1, y1, z1, 0.20, materialArc, false, false);
                            }
                        }
                    }
                    else if (segment.getType() == PathType.SPLINE)
                    {
                        final int stepSize = Math.min(32, segment.getLength());
                        final int numSteps = 1 + (segment.getLength() / stepSize);

                        for (int step=0; step<numSteps; ++step)
                        {
                            pathInfo.setSegmentDistance(step * stepSize);
                            Vector vec0 = path.evaluatePosition(pathInfo);

                            pathInfo.setSegmentDistance(Math.min((step + 1) * stepSize, segment.getLength()));
                            Vector vec1 = path.evaluatePosition(pathInfo);

                            x0 = vec0.getFloatX();
                            y0 = vec0.getFloatY();
                            z0 = vec0.getFloatZ();

                            x1 = vec1.getFloatX();
                            y1 = vec1.getFloatY();
                            z1 = vec1.getFloatZ();

                            if (!((x0 == x1) && (y0 == y1) && (z0 == z1)))
                            {
                                this.addLineSegment(listID, x0, y0, z0, x1, y1, z1, 0.20, materialSpline, false, false);
                            }
                        }
                    }

                    ++segmentIndex;
                }

                ++pathIndex;
            }
        }
        else
        {
            throw new RuntimeException("RenderManager::addPaths() - " + listID + " does not exist!");
        }
    }

    /**
     * Displays som basic stats / debug information.
     */
    public void showDisplayListStats()
    {
        System.out.println("[ RenderManager - Display Lists ]");
        displayListCache.forEach((key, val) -> System.out.println("displayListCache[" + key + "] contains " + val.size() + " items"));
    }
}
