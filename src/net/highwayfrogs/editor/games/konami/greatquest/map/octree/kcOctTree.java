package net.highwayfrogs.editor.games.konami.greatquest.map.octree;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter.IMultiLineInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.math.kcVector3;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.IPropertyListCreator;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Represents OctTree data.
 * The OctTree has its minimum corner at the tree offset, with its nodes halving in size in each direction (as an OctTree should) every layer.
 * The octree branches (kcOctBranch) represent a tree node which has child nodes (could be leaves or branches).
 * The octree leaves (kcOctLeaf) represent nodes which are not broken down further.
 * The quad branches (kcQuadBranch) represents quad tree links as a replacement for the oct tree branches. (Presumably for vertically finding stuff such as terrain.)
 * TODO:
 *  -> Automatic Generation
 *  -> Remove OctTree traversal maybe? Not sure, it's good for understanding how this works.
 * Created by Kneesnap on 4/20/2024.
 */
@Getter
public class kcOctTree extends GameData<GreatQuestInstance> implements IMultiLineInfoWriter, IPropertyListCreator {
    private final kcOctTreeType treeType;
    private int treeDepth; // maxDimensionE, seems unused by the raw game, but maxDimension = (1 << this).
    private int smallestNodeDepth; // This is the maximum depth for any particular node.
    private final kcVector3 offset = new kcVector3(-8197.429F, -8203.538F, -8195.479F); // Seems to be the same across all levels.
    private final List<kcOctBranch> branches = new ArrayList<>(); // This can have as many or as few entries as necessary. Ordering appears to include child branches immediately, not queued, sorted by index.
    private final List<kcOctLeaf> leaves = new ArrayList<>(); // The first leaf is empty.
    private final List<kcQuadBranch> quadBranches = new ArrayList<>();

    private static final int DEFAULT_ROOT_BRANCH = 1; // Zero is null, so we want the first branch to be one.
    private static final int RUNTIME_VALUE_COUNT = 6;
    public static final short FLAG_IS_LEAF = (short) Constants.BIT_FLAG_15; // 0x8000
    public static final short NULL_LEAF_ID = FLAG_IS_LEAF; // 0x8000, 0x8001 is the first real leaf.

    public kcOctTree(GreatQuestInstance instance, kcOctTreeType treeType, int treeDepth, int smallestNodeDepth) {
        super(instance);
        this.treeType = treeType;
        this.treeDepth = treeDepth;
        this.smallestNodeDepth = smallestNodeDepth;
    }

    @Override
    public void load(DataReader reader) {
        int maxTreeSize = reader.readInt();
        this.treeDepth = reader.readInt();
        int smallestNodeSize = reader.readInt();
        this.smallestNodeDepth = reader.readInt();
        reader.skipPointer(); // mpRaycastCallback
        int octantBranchCount = reader.readInt();
        reader.skipPointer(); // Runtime (octantBranchArray)
        int octantLeafCount = reader.readInt();
        reader.skipPointer(); // Runtime (octantLeafArray)
        int quadBranchCount = reader.readInt();
        reader.skipPointer(); // Runtime (quadBranchArray)
        int rootBranch = reader.readInt();
        this.offset.load(reader);
        reader.skipBytesRequireEmpty(Constants.INTEGER_SIZE); // flags (Set to 0 by kcOctTreeInit)
        reader.skipBytes(RUNTIME_VALUE_COUNT * Constants.INTEGER_SIZE);

        if (rootBranch != DEFAULT_ROOT_BRANCH)
            getLogger().warning("Read the default rootBranch as " + rootBranch + ", but " + DEFAULT_ROOT_BRANCH + " was expected.");

        // Validate treeSize/treeDepth.
        if (maxTreeSize != getTreeSize())
            getLogger().severe("The maxTreeSize was not (1 << treeDepth)! (" + maxTreeSize + " != (1 << " + this.treeDepth + "))");
        String failureMessage = validateTreeDepth(this.treeDepth);
        if (failureMessage != null)
            getLogger().severe(failureMessage);

        // Validate smallestNodeSize/smallestNodeDepth.
        if (smallestNodeSize != getSmallestNodeSize())
            getLogger().severe("The smallestNodeSize was not (1 << (treeDepth - smallestNodeDepth))! (" + smallestNodeSize + " != (1 << (" + this.treeDepth + " - " + this.smallestNodeDepth + ")))");
        failureMessage = validateSmallestNodeDepth(this.smallestNodeDepth);
        if (failureMessage != null)
            getLogger().severe(failureMessage);

        // Alignment padding
        reader.alignRequireByte(GreatQuestInstance.PADDING_BYTE_DEFAULT, 16);

        // Read branches.
        this.branches.clear();
        for (int i = 0; i < octantBranchCount; i++) {
            kcOctBranch newBranch = new kcOctBranch(getGameInstance());
            newBranch.load(reader);
            this.branches.add(newBranch);
        }

        // Align to 16 byte boundary
        reader.alignRequireEmpty(16);

        // Read leaves.
        this.leaves.clear();
        for (int i = 0; i < octantLeafCount; i++) {
            kcOctLeaf newLeaf = new kcOctLeaf(getGameInstance());
            newLeaf.load(reader);
            this.leaves.add(newLeaf);
        }

        // Read quad branches.
        this.quadBranches.clear();
        for (int i = 0; i < quadBranchCount; i++) {
            kcQuadBranch newBranch = new kcQuadBranch(getGameInstance());
            newBranch.load(reader);
            this.quadBranches.add(newBranch);
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getTreeSize());
        writer.writeInt(this.treeDepth);
        writer.writeInt(getSmallestNodeSize());
        writer.writeInt(this.smallestNodeDepth);
        writer.writeNullPointer(); // mpRaycastCallback
        writer.writeInt(this.branches.size()); // octantBranchCount
        writer.writeNullPointer(); // Runtime (octantBranchArray)
        writer.writeInt(this.leaves.size()); // octantLeafCount
        writer.writeNullPointer(); // Runtime (octantLeafArray)
        writer.writeInt(this.quadBranches.size()); // quadBranchCount
        writer.writeNullPointer(); // Runtime (quadBranchArray)
        writer.writeInt(DEFAULT_ROOT_BRANCH);
        this.offset.save(writer);
        writer.writeInt(0); // flags
        writer.writeNull(RUNTIME_VALUE_COUNT * Constants.INTEGER_SIZE);

        // Alignment padding
        writer.align(16, GreatQuestInstance.PADDING_BYTE_DEFAULT);

        // Read branches.
        for (int i = 0; i < this.branches.size(); i++)
            this.branches.get(i).save(writer);

        // Align to 16 byte boundary
        writer.align(16);

        // Read leaves.
        for (int i = 0; i < this.leaves.size(); i++)
            this.leaves.get(i).save(writer);

        // Read quad branches.
        for (int i = 0; i < this.quadBranches.size(); i++)
            this.quadBranches.get(i).save(writer);
    }

    @Override
    public void writeMultiLineInfo(StringBuilder builder, String padding) {
        builder.append(padding).append("Octree Size: ").append(getTreeSize()).append(" (1 << ").append(this.treeDepth).append(")").append(Constants.NEWLINE);
        builder.append(padding).append("Smallest Node Size: ").append(getSmallestNodeSize()).append(" (1 << (").append(this.treeDepth).append(" - ").append(this.smallestNodeDepth).append("))").append(Constants.NEWLINE);
        this.offset.writePrefixedInfoLine(builder, "Offset", padding);

        String newPadding = padding + " ";
        int branchDigitCount = NumberUtils.getHexDigitCount(this.branches.size());
        builder.append(padding).append("Branches [").append(this.branches.size()).append("]:").append(Constants.NEWLINE);
        for (int i = 0; i < this.branches.size(); i++)
            this.branches.get(i).writePrefixedInfoLine(builder, NumberUtils.to0PrefixedHexString(i, branchDigitCount), newPadding);

        int leafDigitCount = NumberUtils.getHexDigitCount(this.leaves.size());
        builder.append(padding).append("Leaves [").append(this.leaves.size()).append("]:").append(Constants.NEWLINE);
        for (int i = 0; i < this.leaves.size(); i++) {
            kcOctLeaf leaf = this.leaves.get(i);
            if (i == 0 || !leaf.isEmpty())
                leaf.writePrefixedInfoLine(builder, NumberUtils.to0PrefixedHexString(i, leafDigitCount), newPadding);
        }

        int quadBranchDigitCount = NumberUtils.getHexDigitCount(this.quadBranches.size());
        builder.append(padding).append("Quad Branches [").append(this.quadBranches.size()).append("]:").append(Constants.NEWLINE);
        for (int i = 0; i < this.quadBranches.size(); i++) {
            kcQuadBranch branch = this.quadBranches.get(i);
            if (!branch.isEmpty())
                branch.writePrefixedInfoLine(builder, NumberUtils.to0PrefixedHexString(i, quadBranchDigitCount), newPadding);
        }
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList.add("Offset", this.offset);
        propertyList.add("Octree Depth", this.treeDepth);
        propertyList.add("Smallest Node Depth", this.smallestNodeDepth);
        propertyList.add("Branches", this.branches.size());
        propertyList.add("Leaves", this.leaves.size());
        propertyList.add("Quad Branches", this.quadBranches.size());
        return propertyList;
    }

    /**
     * Gets the dimensions (length/width/height) of a cubic representation of the Octree itself/its root node.
     * By the definition of a cube, all sides are the same length, so a single value is returned representing the length of all sides.
     * Referred to as 'maxDimension' in the original debug symbols.
     */
    public int getTreeSize() {
        return 1 << this.treeDepth;
    }

    /**
     * Sets the tree depth (how many layers of child nodes the tree can have).
     * The size of the root cube will be 2^newTreeDepth
     * @param newTreeDepth the new tree depth to apply
     */
    public void setTreeDepth(int newTreeDepth) {
        String failureMessage = validateTreeDepth(newTreeDepth);
        if (failureMessage != null)
            throw new IllegalArgumentException(failureMessage);

        this.treeDepth = newTreeDepth;
    }

    private String validateTreeDepth(int newTreeDepth) {
        if (newTreeDepth <= 0) {
            return "A tree depth of " + newTreeDepth + " is invalid, it must be greater than zero.";
        } else if (this.smallestNodeDepth > newTreeDepth) {
            return "The tree depth (" + newTreeDepth + ") must be at least as large as the depth of the smallest node. (" + this.smallestNodeDepth + ")";
        } else {
            return null;
        }
    }

    /**
     * Gets the dimensions of the smallest node/cube representable by this Octree.
     * By the definition of a cube, all sides are the same length, so a single value is returned representing the length of all sides.
     * Referred to as 'maxResolution' in the original debug symbols.
     */
    public int getSmallestNodeSize() {
        return 1 << (this.treeDepth - this.smallestNodeDepth);
    }

    /**
     * Gets the cube dimensions (length/width/height) for a node at a given depth (distance from the root)
     * By the definition of a cube, all sides are the same length, so a single value is returned representing the length of all sides.
     * @param nodeDepth the distance (depth/height) between the root of the Octree and the node.
     * @return nodeCubeDimensions
     */
    public int getNodeSize(int nodeDepth) {
        if (nodeDepth < 0 || nodeDepth > this.smallestNodeDepth)
            throw new IllegalArgumentException("Invalid nodeDepth: " + nodeDepth + " (Min Depth: 0, Max Depth: " + this.smallestNodeDepth + ")");
        return 1 << (this.treeDepth - nodeDepth);
    }

    /**
     * Sets the depth (how far away from the root node) of the smallest (furthest) tree nodes.
     * The size of such a cube will be 2 ^ (treeDepth - newSmallestNodeDepth).
     * A lower depth will result in a larger node, and a larger depth will result in a smaller node.
     * @param newSmallestNodeDepth the new tree depth to apply
     */
    public void setSmallestNodeDepth(int newSmallestNodeDepth) {
        String failureMessage = validateSmallestNodeDepth(newSmallestNodeDepth);
        if (failureMessage != null)
            throw new IllegalArgumentException(failureMessage);

        this.smallestNodeDepth = newSmallestNodeDepth;
    }

    private String validateSmallestNodeDepth(int newSmallestNodeDepth) {
        if (newSmallestNodeDepth <= 0) {
            return "A node depth of " + newSmallestNodeDepth + " is invalid, it must be greater than zero.";
        } else if (newSmallestNodeDepth > this.treeDepth) {
            return "The depth of the smallest node (" + newSmallestNodeDepth + ") can be no larger than the maximum depth of the tree itself. (" + this.treeDepth + ")";
        } else {
            return null;
        }
    }

    /**
     * Reimplementation of kcOctTreeFindContainingLeaf()
     * @param pos the position to search in the OctTree.
     * @return leafObj
     */
    public short findContainingLeaf(Vector3f pos) {
        if (pos == null)
            throw new NullPointerException("pos");

        float posX = pos.getX() - this.offset.getX();
        float posY = pos.getY() - this.offset.getY();
        float posZ = pos.getZ() - this.offset.getZ();
        return findContainingLeafInt(posX, posY, posZ, DEFAULT_ROOT_BRANCH);
    }

    /**
     * Reimplementation of kcOctTreeFindContainingLeafInt()
     * @param posX the x position coordinate to search in the OctTree.
     * @param posY the y position coordinate to search in the OctTree.
     * @param posZ the z position coordinate to search in the OctTree.
     * @param startNode the node id to start searching from
     * @return leafId
     */
    public short findContainingLeafInt(float posX, float posY, float posZ, int startNode) {
        if (startNode <= 0 || startNode > this.branches.size()) // ID of 0 indicates null.
            throw new IllegalArgumentException("The startNode id was " + startNode + ", which was not valid!");

        int dimension = getTreeSize() >> 1;
        int testX = dimension, testY = dimension, testZ = dimension;

        short tempNode;
        int childIndex;
        for (tempNode = (short) startNode; (tempNode & FLAG_IS_LEAF) == 0; tempNode = this.branches.get(tempNode - 1).getChildNumbers()[childIndex]) {
            dimension >>= 1;
            childIndex = 0;

            boolean xHigh = (posX >= testX);
            testX += (xHigh ? dimension : -dimension);
            if (xHigh)
                childIndex |= kcOctBranch.CHILD_INDEX_FLAG_X_HI;

            boolean yHigh = (posY >= testY);
            testY += (yHigh ? dimension : -dimension);
            if (yHigh)
                childIndex |= kcOctBranch.CHILD_INDEX_FLAG_Y_HI;

            boolean zHigh = (posZ >= testZ);
            testY += (zHigh ? dimension : -dimension);
            if (zHigh)
                childIndex |= kcOctBranch.CHILD_INDEX_FLAG_Z_HI;
        }

        return tempNode;
    }

    /**
     * Implementation of 'kcCOctTree::Traverse' and 'kcOctTreeTraverse'
     * @param testCallback the callback to run to test the validity of an oct tree leaf
     * @param actionCallback the callback to run for each found oct tree leaf
     * @param context any context data to pass to the test callback
     */
    public <TContext> void traverse(BiFunction<kcOctTreeTraversalInfo<TContext>, Integer, kcOctTreeStatus> testCallback, BiConsumer<TContext, kcOctLeaf> actionCallback, TContext context) {
        if (testCallback == null)
            throw new NullPointerException("testCallback");
        if (actionCallback == null)
            throw new NullPointerException("actionCallback");

        kcOctTreeTraversalInfo<TContext> traversal = new kcOctTreeTraversalInfo<>(this, testCallback, actionCallback, context);
        traverseInt(traversal, DEFAULT_ROOT_BRANCH);
        traversal.runActionCallbacks();
    }

    /**
     * Implementation of 'kcOctTreeTraverseInt'
     * @param traversal the context to traverse with
     * @param branchId the id of the branch to traverse from
     */
    public void traverseInt(kcOctTreeTraversalInfo<?> traversal, int branchId) {
        kcOctTreeStatus status = traversal.runTestCallback(branchId);
        if (status == kcOctTreeStatus.OUT) {
            return;
        }

        if ((branchId & FLAG_IS_LEAF) == FLAG_IS_LEAF) {
            traversal.addLeaf(branchId);
            return;
        }

        if (status == kcOctTreeStatus.DONE) {
            traverseWithoutTest(traversal, branchId);
            return;
        }

        float startX = traversal.getNodeOrigin().getX(),
                startY = traversal.getNodeOrigin().getY(),
                startZ = traversal.getNodeOrigin().getZ();

        kcOctBranch branch = this.branches.get(branchId - 1);
        float newDimension = traversal.getNodeDimension() * .5F;
        for (int i = 0; i < kcOctBranch.CHILD_NODE_COUNT; i++) {
            traversal.getNodeOrigin().setXYZ(startX, startY, startZ);
            traversal.setNodeDimension(newDimension);
            if ((i & kcOctBranch.CHILD_INDEX_FLAG_X_HI) != 0)
                traversal.getNodeOrigin().setX(traversal.getNodeOrigin().getX() + newDimension);
            if ((i & kcOctBranch.CHILD_INDEX_FLAG_Y_HI) != 0)
                traversal.getNodeOrigin().setY(traversal.getNodeOrigin().getY() + newDimension);
            if ((i & kcOctBranch.CHILD_INDEX_FLAG_Z_HI) != 0)
                traversal.getNodeOrigin().setZ(traversal.getNodeOrigin().getZ() + newDimension);
            traverseInt(traversal, branch.getChildNumbers()[i]);
        }
    }

    /**
     * Implementation of 'kcOctTreeTraverseWithoutTest'
     * This function appears broken in the original
     * @param traversal the context to traverse with
     * @param branchId the id of the branch to traverse from
     */
    public void traverseWithoutTest(kcOctTreeTraversalInfo<?> traversal, int branchId) {
        if (traversal == null)
            throw new NullPointerException("traversal");
        if (branchId <= 0 || branchId > this.branches.size()) // ID of 0 indicates null.
            throw new IllegalArgumentException("The branchId was " + branchId + ", which was not valid!");

        if ((branchId & FLAG_IS_LEAF) != 0) {
            traversal.addLeaf(branchId);
            return;
        }

        float startX = traversal.getNodeOrigin().getX(),
                startY = traversal.getNodeOrigin().getY(),
                startZ = traversal.getNodeOrigin().getZ();

        kcOctBranch branch = this.branches.get(branchId - 1);

        float newDimension = traversal.getNodeDimension() * .5F;
        for (int i = 0; i < kcOctBranch.CHILD_NODE_COUNT; i++) {
            traversal.getNodeOrigin().setXYZ(startX, startY, startZ);
            traversal.setNodeDimension(newDimension);
            if ((i & kcOctBranch.CHILD_INDEX_FLAG_X_HI) != 0)
                traversal.getNodeOrigin().setX(traversal.getNodeOrigin().getX() + newDimension);

            // The following appear broken (and probably are), but it's what the original game does soo.
            if ((i & kcOctBranch.CHILD_INDEX_FLAG_Y_HI) != 0)
                traversal.getNodeOrigin().setX(traversal.getNodeOrigin().getX() + newDimension);
            if ((i & kcOctBranch.CHILD_INDEX_FLAG_Z_HI) != 0)
                traversal.getNodeOrigin().setX(traversal.getNodeOrigin().getX() + newDimension);
            traverseWithoutTest(traversal, branch.getChildNumbers()[i]);
        }
    }

    /**
     * Represents the 'OTTEST' enum.
     */
    public enum kcOctTreeStatus {
        OUT, IN, DONE;
    }
}