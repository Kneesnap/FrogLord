package net.highwayfrogs.editor.gui.texture.atlas;

import lombok.Getter;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.SortedList;

import java.util.*;
import java.util.function.ToLongFunction;

/**
 * A texture atlas using an algorithm which places textures using a tree.
 * The texture tree has been designed to support efficient real-time adding/removing of textures.
 * It's based on a normal tree texture packing algorithm, but has been modified to include better performance for arbitrary texture insertion/removal.
 * References:
 * - <a href="https://github.com/juj/RectangleBinPack/blob/master/old/RectangleBinPack.cpp"/>
 * - <a href="http://blackpawn.com/texts/lightmaps/default.html"/>
 * - <a href="https://web.archive.org/web/20180913014836/http://clb.demon.fi:80/projects/rectangle-bin-packing"/>
 * - <a href="http://www.gamedev.net/community/forums/topic.asp?topic_id=392413"/>
 * Created by Kneesnap on 6/19/2024.
 */
public class TreeTextureAtlas extends BasicTextureAtlas<AtlasTexture> {
    private final List<TreeNode> freeSlotsByArea = new ArrayList<>(); // Sorted by area.
    private final List<TreeNode> freeSlotsByAreaHigherWidth = new ArrayList<>(); // Sorted by area, only contains nodes where width >= height
    private final List<TreeNode> freeSlotsByAreaHigherHeight = new ArrayList<>(); // Sorted by area, only contains nodes where height >= width.
    private final List<TreeNode> freeDiagonalSlots = new ArrayList<>(); // Sorted by area.
    private final Map<AtlasTexture, TreeNode> nodesByTexture = new IdentityHashMap<>(); // Sorted by area.
    private TreeNode lastRemovedNode; // This is a cache. If we've just removed a node, we're probably updating it, and it's often good to place the updated texture in the same spot.
    private static final ToLongFunction<TreeNode> SLOT_AREA_CALCULATOR =
            (TreeNode node) -> (long) node.getNodeWidth() * node.getNodeHeight();
    private static final Comparator<TreeNode> SLOTS_BY_AREA_COMPARATOR = Comparator
            .comparingLong(SLOT_AREA_CALCULATOR)
            .thenComparingInt(TreeNode::getY)
            .thenComparingInt(TreeNode::getX);
    private static final ToLongFunction<TreeNode> SLOT_DIAGONAL_AREA_CALCULATOR = TreeNode::getDiagonalSpaceArea;
    private static final Comparator<TreeNode> SLOTS_BY_DIAGONAL_AREA_COMPARATOR = Comparator
            .comparingLong(SLOT_DIAGONAL_AREA_CALCULATOR)
            .thenComparingInt(TreeNode::getY)
            .thenComparingInt(TreeNode::getX);

    public TreeTextureAtlas(int width, int height, boolean allowAutomaticResizing) {
        super(width, height, allowAutomaticResizing, AtlasTexture::new);
    }

    @Override
    protected boolean updatePositions(SortedList<AtlasTexture> sortedTextureList) {
        // Setup root node.
        TreeNode rootNode = new TreeNode(this, null, 0, 0, getAtlasWidth(), getAtlasHeight(), false);
        this.freeDiagonalSlots.clear();
        this.freeSlotsByArea.clear();
        this.freeSlotsByAreaHigherWidth.clear();
        this.freeSlotsByAreaHigherHeight.clear();
        this.freeSlotsByArea.add(rootNode);
        this.freeSlotsByAreaHigherWidth.add(rootNode);
        this.freeSlotsByAreaHigherHeight.add(rootNode);
        return super.updatePositions(sortedTextureList);
    }

    @Override
    protected boolean placeTexture(AtlasTexture texture) {
        if (this.lastRemovedNode != null && this.lastRemovedNode.setTexture(texture)) {
            this.lastRemovedNode = null; // Enough of that.
            return true; // Place updated texture in previous position.
        }

        this.lastRemovedNode = null;

        if (texture.getPaddedWidth() > texture.getPaddedHeight()) {
            if (tryInsertTexture(this.freeSlotsByAreaHigherWidth, SLOT_AREA_CALCULATOR, texture))
                return true;

            // Go over other nodes. (Less likely to contain a match which is why we check the other first.)
            if (tryInsertTexture(this.freeSlotsByAreaHigherHeight, SLOT_AREA_CALCULATOR, texture))
                return true;
        } else if (texture.getPaddedWidth() < texture.getPaddedHeight()) {
            if (tryInsertTexture(this.freeSlotsByAreaHigherHeight, SLOT_AREA_CALCULATOR, texture))
                return true;

            // Go over other nodes. (Less likely to contain a match which is why we check the other first.)
            if (tryInsertTexture(this.freeSlotsByAreaHigherWidth, SLOT_AREA_CALCULATOR, texture))
                return true;
        } else { // paddedWidth == paddedHeight
            if (tryInsertTexture(this.freeSlotsByArea, SLOT_AREA_CALCULATOR, texture))
                return true;
        }

        // There wasn't any open spot, so let's try the diagonals.
        return tryInsertTexture(this.freeDiagonalSlots, SLOT_DIAGONAL_AREA_CALCULATOR, texture);
    }

    private boolean tryInsertTexture(List<TreeNode> slots, ToLongFunction<TreeNode> areaCalculator, AtlasTexture texture) {
        long targetArea = (long) texture.getPaddedWidth() * texture.getPaddedHeight();

        // Binary search the list to find the slot we'd like to add it at, then find the start index.
        int startIndex;
        int binarySearchIndex = Utils.binarySearch(slots, targetArea, areaCalculator);
        if (binarySearchIndex >= 0) {
            // We found an exact match for this size, so we're going to find the minimum index which matches the area and resume there.
            startIndex = binarySearchIndex;
            while (startIndex > 0 && areaCalculator.applyAsLong(slots.get(startIndex - 1)) == targetArea)
                startIndex--;
        } else {
            startIndex = -(binarySearchIndex + 1);
        }

        // Try to insert to all the nodes.
        for (int i = startIndex; i < slots.size(); i++) {
            TreeNode node = slots.get(i);
            if (node.setTexture(texture))
                return true; // Successfully inserted texture.
        }

        // Failed to insert the node.
        return false;
    }

    @Override
    protected void freeTexture(AtlasTexture texture) {
        TreeNode node = this.nodesByTexture.remove(texture);
        if (node != null)
            node.setTexture(null);
    }

    /**
     * An atlas texture which is part of the tree algorithm. This is a node in a tree.
     */
    @Getter
    public static class TreeNode {
        private final TreeTextureAtlas textureAtlas;
        private final boolean diagonalNode;
        private final TreeNode parentNode;
        private final int x;
        private final int y;
        private int nodeWidth;
        private int nodeHeight;
        private TreeNode leftChild;
        private TreeNode rightChild;
        private TreeNode diagonalChild;
        private AtlasTexture texture;

        public TreeNode(TreeTextureAtlas atlas, TreeNode parentNode, int x, int y, int nodeWidth, int nodeHeight, boolean diagonalNode) {
            this.textureAtlas = atlas;
            this.diagonalNode = diagonalNode;
            this.parentNode = parentNode;
            this.x = x;
            this.y = y;
            this.nodeWidth = nodeWidth;
            this.nodeHeight = nodeHeight;
        }

        /**
         * Test if a point is contained as part of the area.
         * @param testX The x to test.
         * @param testY The y to test.
         * @return contains
         */
        public boolean contains(int testX, int testY) {
            return testX >= this.x && testX < getEndX()
                    && testY >= this.y && testY < getEndY();
        }

        /**
         * Gets the x edge of this node.
         */
        public int getEndX() {
            return (this.x + this.nodeWidth);
        }

        /**
         * Gets the y edge of this node.
         */
        public int getEndY() {
            return (this.y + this.nodeHeight);
        }

        private boolean canMergeIntoParent() {
            return this.texture == null && this.leftChild == null && this.rightChild == null && this.diagonalChild == null;
        }

        /**
         * Attempt to merge into the parent node.
         */
        boolean tryMergeChildNodes() {
            if (this.texture != null)
                return false; // Can't merge into this.

            if (this.leftChild == null && this.rightChild == null && this.diagonalChild == null)
                return false; // No child nodes to merge.

            // Ensure all child nodes can merge.
            if (this.leftChild != null && !this.leftChild.canMergeIntoParent())
                return false;
            if (this.rightChild != null && !this.rightChild.canMergeIntoParent())
                return false;
            if (this.diagonalChild != null && !this.diagonalChild.canMergeIntoParent())
                return false;

            // Change node width / height.
            removeFromLists();
            int startNodeWidth = this.nodeWidth;
            int startNodeHeight = this.nodeHeight;
            if (this.leftChild != null) {
                this.leftChild.removeFromLists();
                this.nodeWidth = startNodeWidth + this.leftChild.getNodeWidth();
                this.leftChild = null;
            }
            if (this.rightChild != null) {
                this.rightChild.removeFromLists();
                this.nodeHeight = startNodeHeight + this.rightChild.getNodeHeight();
                this.rightChild = null;
            }
            if (this.diagonalChild != null) {
                this.diagonalChild.removeFromLists();
                this.nodeWidth = startNodeWidth + this.diagonalChild.getNodeWidth();
                this.nodeHeight = startNodeHeight + this.diagonalChild.getNodeHeight();
                this.diagonalChild = null;
            }
            addToLists();

            if (this.parentNode != null)
                this.parentNode.tryMergeChildNodes();
            return true;
        }

        /**
         * Gets the width of the diagonal space area.
         */
        public int getDiagonalSpaceWidth() {
            return getNodeWidth() - getEndX();
        }
        /**
         * Gets the height of the diagonal space area.
         */
        public int getDiagonalSpaceHeight() {
            return getNodeHeight() - getEndY();
        }

        /**
         * Gets the area taken up by the diagonal space.
         */
        public long getDiagonalSpaceArea() {
            return (long) getDiagonalSpaceWidth() * getDiagonalSpaceHeight();
        }

        /**
         * Attempts to insert a texture node into the tree.
         * @param texture The texture to insert.
         * @return Whether there was space to insert the texture node.
         */
        boolean setTexture(AtlasTexture texture) {
            if (texture == this.texture)
                return true; // Already set.

            // First, test if there's room to fit it here.
            if (texture != null && !canFitTexture(texture, getNodeWidth(), getNodeHeight()))
                return false; // Nope, there isn't enough space.

            // Clear out the old texture.
            if (this.texture != null) {
                this.texture = null;
                // Shouldn't be in the lists, so we don't need to remove them.

                if (texture == null) { // There's no texture we intend to apply right now.
                    addToLists();
                    tryMergeChildNodes();
                    return true;
                }
            } else {
                removeFromLists();
            }

            // Apply texture.
            texture.setPosition(getX(), getY());
            this.texture = texture;

            // Create new nodes.
            int texturePaddedWidth = texture.getPaddedWidth();
            int texturePaddedHeight = texture.getPaddedHeight();
            int diagonalWidth = (getNodeWidth() - texturePaddedWidth);
            int diagonalHeight = (getNodeHeight() - texturePaddedHeight);

            // Create nodes using remaining free space.
            int leftX = getX() + texturePaddedWidth;
            int leftY = getY();
            if (this.leftChild == null && diagonalWidth > 0 && texturePaddedHeight > 0) {
                this.leftChild = new TreeNode(this.textureAtlas, this, leftX, leftY, diagonalWidth, texturePaddedHeight, false);
                this.leftChild.addToLists();
            }

            int rightX = getX();
            int rightY = getY() + texturePaddedHeight;
            if (this.rightChild == null && texturePaddedWidth > 0 && diagonalHeight > 0) {
                this.rightChild = new TreeNode(this.textureAtlas, this, rightX, rightY, texturePaddedWidth, diagonalHeight, false);
                this.rightChild.addToLists();
            }

            if (this.diagonalChild == null && diagonalWidth > 0 && diagonalHeight > 0) {
                this.diagonalChild = new TreeNode(this.textureAtlas, this, leftX, rightY, diagonalWidth, diagonalHeight, true);
                this.diagonalChild.addToLists();
            }

            return true;
        }

        private void addToLists() {
            if (isDiagonalNode()) {
                if (getDiagonalSpaceArea() > 0)
                    tryAddToList(this.textureAtlas.freeDiagonalSlots, SLOTS_BY_DIAGONAL_AREA_COMPARATOR);
            } else {
                if (this.nodeWidth >= this.nodeHeight)
                    tryAddToList(this.textureAtlas.freeSlotsByAreaHigherWidth, SLOTS_BY_AREA_COMPARATOR);
                if (this.nodeWidth <= this.nodeHeight)
                    tryAddToList(this.textureAtlas.freeSlotsByAreaHigherHeight, SLOTS_BY_AREA_COMPARATOR);

                tryAddToList(this.textureAtlas.freeSlotsByArea, SLOTS_BY_AREA_COMPARATOR);
            }
        }

        private void tryAddToList(List<TreeNode> nodes, Comparator<TreeNode> comparator) {
            int index = Collections.binarySearch(nodes, this, comparator);
            if (index >= 0)
                throw new RuntimeException("TreeNode is already in the list!");
            nodes.add(-(index + 1), this);
        }

        private void removeFromLists() {
            if (isDiagonalNode()) {
                if (getDiagonalSpaceArea() > 0)
                    tryRemoveFromList(this.textureAtlas.freeDiagonalSlots, SLOTS_BY_DIAGONAL_AREA_COMPARATOR);
            } else {
                if (this.nodeWidth >= this.nodeHeight)
                    tryRemoveFromList(this.textureAtlas.freeSlotsByAreaHigherWidth, SLOTS_BY_AREA_COMPARATOR);
                if (this.nodeWidth <= this.nodeHeight)
                    tryRemoveFromList(this.textureAtlas.freeSlotsByAreaHigherHeight, SLOTS_BY_AREA_COMPARATOR);

                tryRemoveFromList(this.textureAtlas.freeSlotsByArea, SLOTS_BY_AREA_COMPARATOR);
            }
        }

        private void tryRemoveFromList(List<TreeNode> nodes, Comparator<TreeNode> comparator) {
            int index = Collections.binarySearch(nodes, this, comparator);
            if (index < 0)
                throw new RuntimeException("TreeNode was not found in the list!");
            if (nodes.remove(index) != this)
                throw new RuntimeException("Removed the wrong TreeNode somehow!!!?");
        }

        private static boolean canFitTexture(AtlasTexture texture, int width, int height) {
            return (texture.getPaddedWidth() <= width) && (texture.getPaddedHeight() <= height);
        }
    }
}