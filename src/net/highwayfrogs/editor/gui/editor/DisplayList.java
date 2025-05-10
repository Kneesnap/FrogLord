package net.highwayfrogs.editor.gui.editor;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import lombok.Getter;

import java.util.*;

/**
 * Represents a list of JavaFX nodes which are grouped together.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public class DisplayList {
    private final Group root;
    private final boolean uniqueGroup;
    private final List<Node> nodes = new ArrayList<>();

    private DisplayList(Group root, boolean uniqueGroup) {
        this.root = root;
        this.uniqueGroup = uniqueGroup;
    }

    /**
     * Gets the number of nodes stored in this list.
     */
    public int size() {
        return this.nodes.size();
    }

    /**
     * Check if there are any nodes in this list.
     * @return true if there are no nodes in this display list, false otherwise.
     */
    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    /**
     * Clears the contents of all nodes in this list.
     */
    public void clear() {
        this.root.getChildren().removeAll(this.nodes);
        this.nodes.clear();
    }

    /**
     * Removes a node from the list.
     * @param node The node to remove.
     */
    public boolean remove(Node node) {
        if (!this.nodes.remove(node))
            return false;

        this.root.getChildren().remove(node);
        return true;
    }

    /**
     * Removes a group of nodes from the list.
     * @param nodes The nodes to remove.
     */
    public void removeAll(List<Node> nodes) {
        this.nodes.removeAll(nodes);
        this.root.getChildren().removeAll(nodes);
    }

    /**
     * Shows all nodes in the display list.
     */
    public void setVisible(boolean visible) {
        for (int i = 0; i < this.nodes.size(); i++)
            this.nodes.get(i).setVisible(visible);
    }

    /**
     * Adds a node to the display list.
     * @param node The node to add.
     */
    public void add(Node node) {
        if (this.nodes.contains(node))
            throw new RuntimeException("The node already exists in the display list.");

        this.nodes.add(node);
        this.root.getChildren().add(node);
    }

    /**
     * Adds a node to the display list using a binary search algorithm.
     * @param node The node to add.
     */
    public void add(Node node, Comparator<? super Node> comparator) {
        if (node == null)
            throw new NullPointerException("node");
        if (comparator == null)
            throw new NullPointerException("comparator");
        if (!this.uniqueGroup)
            throw new UnsupportedOperationException("Cannot add node via binary-search when this is not a unique group.");

        int nodeIndex = Collections.binarySearch(this.root.getChildren(), node, comparator);
        if (this.nodes.contains(node)) // Binary search can often return duplicate indices, such as if sorting by area.
            throw new RuntimeException("The node already exists in the display list.");

        this.nodes.add(node);
        int insertionIndex = nodeIndex >= 0 ? nodeIndex : -(nodeIndex + 1);
        this.root.getChildren().add(insertionIndex, node);
    }

    /**
     * Adds a node to the display list if it is not already contained in the display list.
     * @param node The node to add.
     * @return if the node was added successfully
     */
    public boolean addIfMissing(Node node) {
        if (this.nodes.contains(node))
            return false;

        this.nodes.add(node);
        this.root.getChildren().add(node);
        return true;
    }

    /**
     * Test if the display list contains the given node.
     * @param node the node to test
     * @return true iff the display list contains the node
     */
    public boolean contains(Node node) {
        return this.nodes.contains(node);
    }

    /**
     * Adds a cylindrical representation of a 3D line.
     * @param x0       The x-coordinate defining the start of the line segment.
     * @param y0       The y-coordinate defining the start of the line segment.
     * @param z0       The z-coordinate defining the start of the line segment.
     * @param x1       The x-coordinate defining the end of the line segment.
     * @param y1       The y-coordinate defining the end of the line segment.
     * @param z1       The z-coordinate defining the end of the line segment.
     * @param radius   The radius of the cylinder (effectively the 'width' of the line).
     * @param material The material used to render the line segment.
     * @return The newly created/added cylinder (cylinder primitive only!)
     */
    public Cylinder addLine(double x0, double y0, double z0, double x1, double y1, double z1, double radius, PhongMaterial material) {
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
        line.setMouseTransparent(false);
        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
        add(line);
        return line;
    }

    /**
     * Adds an axis-aligned bounding box.
     * @param minX The minimum x-coordinate.
     * @param minY The minimum y-coordinate.
     * @param minZ The minimum z-coordinate.
     * @param maxX The maximum x-coordinate.
     * @param maxY The maximum y-coordinate.
     * @param maxZ The maximum z-coordinate.
     * @return The newly created/added bounding box
     */
    public Box addBoundingBoxFromMinMax(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, PhongMaterial material, boolean useWireframe) {
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

        return this.addBoundingBoxCenteredWithDimensions(x, y, z, w, h, d, material, useWireframe);
    }

    /**
     * Adds an axis-aligned bounding box.
     * @param x      The x-coordinate defining the center of the box.
     * @param y      The y-coordinate defining the center of the box.
     * @param z      The z-coordinate defining the center of the box.
     * @param width  The width (along x-axis).
     * @param height The height (along y-axis).
     * @param depth  The depth (along z-axis).
     * @return The newly created/added bounding box
     */
    public Box addBoundingBoxCenteredWithDimensions(double x, double y, double z, double width, double height, double depth, PhongMaterial material, boolean useWireframe) {
        Box axisAlignedBoundingBox = new Box(width, height, depth);

        axisAlignedBoundingBox.setMaterial(material);
        axisAlignedBoundingBox.setDrawMode(useWireframe ? DrawMode.LINE : DrawMode.FILL);
        axisAlignedBoundingBox.setCullFace(CullFace.BACK);
        axisAlignedBoundingBox.getTransforms().addAll(new Translate(x, y, z));
        axisAlignedBoundingBox.setMouseTransparent(useWireframe);

        add(axisAlignedBoundingBox);
        return axisAlignedBoundingBox;
    }

    /**
     * Adds a sphere.
     * @param x0           The x-coordinate defining the center of the sphere.
     * @param y0           The y-coordinate defining the center of the sphere.
     * @param z0           The z-coordinate defining the center of the sphere.
     * @param radius       The radius of the cylinder.
     * @param material     The material used to render the sphere.
     * @param useWireframe Whether or not to display the sphere as a wireframe.
     * @return The newly created/added sphere
     */
    public Sphere addSphere(double x0, double y0, double z0, double radius, PhongMaterial material, boolean useWireframe) {
        Sphere sph0 = new Sphere(radius, 8);
        sph0.setMaterial(material);
        sph0.setDrawMode(useWireframe ? DrawMode.LINE : DrawMode.FILL);
        sph0.setCullFace(CullFace.BACK);
        sph0.setMouseTransparent(useWireframe);
        sph0.getTransforms().addAll(new Translate(x0, y0, z0));

        add(sph0);
        return sph0;
    }

    /**
     * Adds a cylinder.
     * @param x            The x-coordinate defining the center of the cylinder.
     * @param y            The y-coordinate defining the center of the cylinder.
     * @param z            The z-coordinate defining the center of the cylinder.
     * @param radius       The radius of the cylinder.
     * @param height       The height of the cylinder.
     * @param material     The material used to render the cylinder.
     * @param useWireframe Whether to display the cylinder as a wireframe.
     * @return The newly created/added cylinder
     */
    public Cylinder addCylinder(double x, double y, double z, double radius, double height, PhongMaterial material, boolean useWireframe) {
        Cylinder cylinder = new Cylinder(radius, height); // TODO: Replace lines with FXyz3D's PolyLines3D or maybe even TexturedMeshes/BezierMeshes
        cylinder.setMaterial(material);
        cylinder.setDrawMode(useWireframe ? DrawMode.LINE : DrawMode.FILL);
        cylinder.setCullFace(CullFace.BACK);
        cylinder.setMouseTransparent(useWireframe);
        cylinder.getTransforms().addAll(new Translate(x, y, z));

        add(cylinder);
        return cylinder;
    }

    /**
     * This class holds render lists.
     */
    public static class RenderListManager {
        @Getter private final Group root;
        private final Set<DisplayList> displayListCache = new HashSet<>();

        public RenderListManager(Group root) {
            this.root = root;
        }

        /**
         * Adds a new display list using the root node.
         */
        public DisplayList createDisplayList() {
            DisplayList newDisplayList = new DisplayList(this.root, false);
            this.displayListCache.add(newDisplayList);
            return newDisplayList;
        }

        /**
         * Adds a new display list with a new Group attached to the root node.
         */
        public DisplayList createDisplayListWithNewGroup() {
            Group newGroup = new Group();
            this.root.getChildren().add(newGroup);

            DisplayList newDisplayList = new DisplayList(newGroup, true);
            this.displayListCache.add(newDisplayList);
            return newDisplayList;
        }

        /**
         * Removes an existing display list.
         * @param displayList The display list to remove.
         */
        public boolean removeDisplayList(DisplayList displayList) {
            if (!this.displayListCache.remove(displayList))
                return false;

            displayList.clear();
            return true;
        }

        /**
         * Clears and removes all display lists.
         */
        public void removeAllDisplayLists() {
            // Clear items from all display lists
            this.clearAllDisplayLists();

            // Clear the cache
            this.displayListCache.clear();
        }

        /**
         * Clears all display lists.
         */
        public void clearAllDisplayLists() {
            // Clear items from each display list
            this.displayListCache.forEach(DisplayList::clear);
        }


        /**
         * Displays som basic stats / debug information.
         */
        public void showDisplayListStats() {
            System.out.println("[ RenderManager - Display Lists ]");
            this.displayListCache.forEach(list -> System.out.println("displayListCache[null] contains " + list.size() + " items"));
        }
    }
}