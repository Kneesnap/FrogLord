package net.highwayfrogs.editor.games.sony.shared.pointcloud;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the Octree.
 * Created by Kneesnap on 2/21/2026.
 */
class PointCloudOctreeNode {
    private final PointCloudOctree tree;
    private final PointCloudOctreeNode parent;
    private final int height;
    private final float minX;
    private final float minY;
    private final float minZ;
    private final float maxX;
    private final float maxY;
    private final float maxZ;
    private final List<SVector> positions = new ArrayList<>(MAX_POSITIONS_PER_NODE);
    PointCloudOctreeNode[] children;

    static final int CHILD_COUNT = 8;
    private static final int MAX_HEIGHT_EXCLUSIVE = 12; // Nodes cannot be smaller than 1.0 x 1.0. (Contains a max of 256 positions (1<<4)*(1<<4) = 16*16)
    private static final int MAX_POSITIONS_PER_NODE = 16;
    private static final int FLAG_X_HIGH = Constants.BIT_FLAG_2;
    private static final int FLAG_Y_HIGH = Constants.BIT_FLAG_1;
    private static final int FLAG_Z_HIGH = Constants.BIT_FLAG_0;

    public PointCloudOctreeNode(PointCloudOctree tree, PointCloudOctreeNode parent, int height, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.tree = tree;
        this.parent = parent;
        this.height = height;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Inserts a position to the tree
     * @param position the position to insert
     * @return true iff the position was previously not present in the tree
     */
    boolean insert(SVector position) {
        if (this.height >= MAX_HEIGHT_EXCLUSIVE - 1 || (this.children == null && this.positions.size() < MAX_POSITIONS_PER_NODE)) {
            if (!this.positions.contains(position))
                return this.positions.add(position);
            return false;
        }

        // Create a new child node.
        if (this.children == null) {
            this.children = new PointCloudOctreeNode[CHILD_COUNT];
            for (int i = 0; i < this.positions.size(); i++)
                insertToChildNode(this.positions.get(i));
            this.positions.clear();
        }

        return insertToChildNode(position);
    }

    private boolean insertToChildNode(SVector position) {
        int index = computeIndex(position);
        if (this.children[index] == null) {
            float centerX = (this.minX + this.maxX) * .5F;
            float centerY = (this.minY + this.maxY) * .5F;
            float centerZ = (this.minZ + this.maxZ) * .5F;
            float minX = this.minX, maxX = centerX;
            float minY = this.minY, maxY = centerY;
            float minZ = this.minZ, maxZ = centerZ;
            if ((index & FLAG_X_HIGH) == FLAG_X_HIGH) {
                minX = centerX;
                maxX = this.maxX;
            }

            if ((index & FLAG_Y_HIGH) == FLAG_Y_HIGH) {
                minY = centerY;
                maxY = this.maxY;
            }

            if ((index & FLAG_Z_HIGH) == FLAG_Z_HIGH) {
                minZ = centerZ;
                maxZ = this.maxZ;
            }

            this.children[index] = new PointCloudOctreeNode(this.tree, this, this.height + 1, minX, minY, minZ, maxX, maxY, maxZ);
        }

        return this.children[index].insert(position);
    }

    /**
     * Removes a position from the tree.
     * @param position the position to remove
     * @return true iff the position was removed from the tree
     */
    boolean remove(SVector position) {
        if (this.children == null || !this.positions.isEmpty()) {
            boolean success = this.positions.remove(position);
            if (success)
                tryMerge();

            return success;
        }

        int index = computeIndex(position);
        PointCloudOctreeNode childNode = this.children[index];
        return childNode != null && childNode.remove(position);
    }

    /**
     * Finds all positions within the given sphere, and stores them into radius.
     * @param results the list to store the results within
     * @param posX the x coordinate of the search sphere
     * @param posY the y coordinate of the search sphere
     * @param posZ the z coordinate of the search sphere
     * @param radius the radius of the search sphere
     */
    public void find(List<SVector> results, float posX, float posY, float posZ, float radius) {
        if (this.children != null) {
            for (int i = 0; i < CHILD_COUNT; i++) {
                PointCloudOctreeNode childNode = this.children[i];
                if (childNode != null && childNode.intersectsSphere(posX, posY, posZ, radius))
                    childNode.find(results, posX, posY, posZ, radius);
            }
        } else {
            double radiusSq = radius * radius;
            for (int i = 0; i < this.positions.size(); i++) {
                SVector position = this.positions.get(i);
                float xDiff = position.getFloatX() - posX;
                float yDiff = position.getFloatY() - posY;
                float zDiff = position.getFloatZ() - posZ;
                double lengthSq = (xDiff * xDiff) + (yDiff * yDiff) + (zDiff * zDiff);
                if (radiusSq >= lengthSq)
                    results.add(position);
            }
        }
    }

    private boolean intersectsSphere(float sphereX, float sphereY, float sphereZ, float radius) {
        return MathUtils.doesSphereIntersectCube(sphereX, sphereY, sphereZ, radius, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    private int computeIndex(SVector position) {
        int nShift = 16 - this.height; // 16 is the size of the position.
        int xBits = (position.getX() >> (nShift - 3)) & FLAG_X_HIGH; // The same bit is tested across each component, despite the difference in shift amount. (We subtract so that building the index is easier, not to test a different bit)
        int yBits = (position.getY() >> (nShift - 2)) & FLAG_Y_HIGH;
        int zBits = (position.getZ() >> (nShift - 1)) & FLAG_Z_HIGH;
        return xBits | yBits | zBits;
    }

    private void tryMerge() {
        if (this.parent == null || !this.positions.isEmpty() || this.parent.parent == null)
            return; // Isn't ready to merge. (The first non-root nodes should never be merged due to their special positions.)

        int otherChildNodes = 0;
        int positionCount = 0;
        boolean hasNestedChildren = false;
        PointCloudOctreeNode remainingNode = null;
        for (int i = 0; i < CHILD_COUNT; i++) {
            PointCloudOctreeNode node = this.parent.children[i];
            if (node == this) {
                this.parent.children[i] = null; // Remove the child node which doesn't have any more positions.
            } else if (node != null) {
                remainingNode = node;
                otherChildNodes++;
                positionCount += node.positions.size();
                if (node.children != null)
                    hasNestedChildren = true;
            }
        }

        if (otherChildNodes > 1 || positionCount > MAX_POSITIONS_PER_NODE || hasNestedChildren)
            return; // Can't merge.

        this.parent.children = null;
        if (remainingNode != null)
            this.parent.positions.addAll(remainingNode.positions);
    }
}
