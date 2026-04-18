package net.highwayfrogs.editor.games.sony.shared.pointcloud;

import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.utils.DataUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an Octree containing individual points for a 16-bit short vector coordinate scheme, such as seen in Sony Cambridge games.
 * Created by Kneesnap on 2/21/2026.
 */
public class PointCloudOctree {
    private final PointCloudOctreeNode rootNode;

    public PointCloudOctree() {
        float min = DataUtils.fixedPointShortToFloat4Bit(Short.MIN_VALUE);
        float max = DataUtils.fixedPointShortToFloat4Bit(Short.MIN_VALUE);
        float half = 0;
        this.rootNode = new PointCloudOctreeNode(this, null, 0, min, min, min, max, max, max);

        // The default nodes use a different order than all other nodes, since the highest bit is the "signed" bit.
        // Because of this, the ordering is swapped, since the bit being set does not indicate a larger value, but instead indicates a lower (negative) value.
        this.rootNode.children = new PointCloudOctreeNode[PointCloudOctreeNode.CHILD_COUNT];
        this.rootNode.children[0] = new PointCloudOctreeNode(this, this.rootNode, 1, half, half, half, max, max, max); // Z- Y- X-
        this.rootNode.children[1] = new PointCloudOctreeNode(this, this.rootNode, 1, min, half, half, half, max, max); // Z+ Y- X-
        this.rootNode.children[2] = new PointCloudOctreeNode(this, this.rootNode, 1, half, min, half, max, half, max); // Z- Y+ X-
        this.rootNode.children[3] = new PointCloudOctreeNode(this, this.rootNode, 1, min, min, half, half, half, max); // Z+ Y+ X-
        this.rootNode.children[4] = new PointCloudOctreeNode(this, this.rootNode, 1, half, half, min, max, max, half); // Z- Y- X+
        this.rootNode.children[5] = new PointCloudOctreeNode(this, this.rootNode, 1, min, half, min, half, max, half); // Z+ Y- X+
        this.rootNode.children[6] = new PointCloudOctreeNode(this, this.rootNode, 1, half, min, min, max, half, half); // Z- Y+ X+
        this.rootNode.children[7] = new PointCloudOctreeNode(this, this.rootNode, 1, min, min, min, half, half, half); // Z+ Y+ X+
    }

    /**
     * Inserts a position to the tree
     * @param position the position to insert
     * @return true iff the position was previously not present in the tree and has been added
     */
    public boolean insert(SVector position) {
        return this.rootNode.insert(position);
    }

    /**
     * Removes a position from the tree.
     * @param position the position to remove
     * @return true iff the position was removed from the tree
     */
    public boolean remove(SVector position) {
        return this.rootNode.remove(position);
    }

    /**
     * Finds all positions within the given sphere, and stores them into radius.
     * @param posX the x coordinate of the search sphere
     * @param posY the y coordinate of the search sphere
     * @param posZ the z coordinate of the search sphere
     * @param radius the radius of the search sphere
     */
    public List<SVector> find(float posX, float posY, float posZ, float radius) {
        List<SVector> results = new ArrayList<>();
        this.rootNode.find(results, posX, posY, posZ, radius);
        return results;
    }
}