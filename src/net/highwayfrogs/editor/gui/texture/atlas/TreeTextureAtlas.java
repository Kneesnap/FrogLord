package net.highwayfrogs.editor.gui.texture.atlas;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas.TreeAtlasTexture;
import net.highwayfrogs.editor.utils.SortedList;

/**
 * A texture atlas using an algorithm which places textures using a tree.
 * This is a backport from ModToolFramework.
 * References:
 * - https://github.com/juj/RectangleBinPack/blob/master/old/RectangleBinPack.cpp
 * - http://blackpawn.com/texts/lightmaps/default.html
 * - https://web.archive.org/web/20180913014836/http://clb.demon.fi:80/projects/rectangle-bin-packing
 * - http://www.gamedev.net/community/forums/topic.asp?topic_id=392413
 * Created by Kneesnap on 9/23/2023.
 */
public class TreeTextureAtlas extends BasicTextureAtlas<TreeAtlasTexture> {
    public TreeTextureAtlas(int width, int height, boolean allowAutomaticResizing) {
        super(width, height, allowAutomaticResizing, TreeAtlasTexture::new);
    }

    @Override
    protected boolean updatePositions(SortedList<TreeAtlasTexture> sortedTextureList) {
        if (sortedTextureList.size() == 0)
            return false; // Nothing to do.

        // Clear relation information.
        for (int i = 0; i < sortedTextureList.size(); i++) {
            TreeAtlasTexture texture = sortedTextureList.get(i);
            texture.leftChild = null;
            texture.rightChild = null;
            texture.nodeWidth = 0;
            texture.nodeHeight = 0;
        }

        // Setup root node.
        TreeAtlasTexture rootNode = sortedTextureList.get(0);
        rootNode.nodeWidth = getAtlasWidth();
        rootNode.nodeHeight = getAtlasHeight();
        rootNode.setPosition(0, 0); // The root should be at the origin to ensure textures are placed correctly.

        // Now, we'll setup positions of all remaining textures by building their tree.
        for (int i = 1; i < sortedTextureList.size(); i++) {
            TreeAtlasTexture texture = sortedTextureList.get(i);
            if (!rootNode.insert(texture))
                return true; // Couldn't be added (no space).
        }

        return false;
    }

    /**
     * An atlas texture which is part of the tree algorithm. This is a node in a tree.
     */
    public static class TreeAtlasTexture extends AtlasTexture {
        @Getter @Setter(AccessLevel.PACKAGE) private TreeAtlasTexture leftChild;
        @Getter @Setter(AccessLevel.PACKAGE) private TreeAtlasTexture rightChild;
        @Getter @Setter(AccessLevel.PACKAGE) private int nodeWidth;
        @Getter @Setter(AccessLevel.PACKAGE) private int nodeHeight;

        public TreeAtlasTexture(TextureAtlas atlas, ITextureSource source) {
            super(atlas, source);
        }

        /**
         * Attempts to insert a texture node into the tree.
         * @param texture The texture to insert.
         * @return Whether there was space to insert the texture node.
         */
        boolean insert(TreeAtlasTexture texture) {
            // First, test if there's room to fit it here.
            if (!canFitTexture(texture, this.nodeWidth, this.nodeHeight))
                return false; // Nope, there isn't enough space.

            // Check existing nodes.
            if (this.leftChild != null && this.leftChild.insert(texture))
                return true;

            if (this.rightChild != null && this.rightChild.insert(texture))
                return true;

            if (this.leftChild != null && this.rightChild != null)
                return false; // Couldn't insert it on either side, and they both exist, therefore there is no room.

            int remainingWidth = (this.nodeWidth - getPaddedWidth());
            int remainingHeight = (this.nodeHeight - getPaddedHeight());

            int leftX, leftY, leftWidth, leftHeight;
            int rightX, rightY, rightWidth, rightHeight;
            if (remainingWidth <= remainingHeight) { // Split the remaining space into two rectangles depending on which remaining dimension is larger.
                leftX = getX() + getPaddedWidth();
                leftY = getY();
                leftWidth = remainingWidth;
                leftHeight = getPaddedHeight();

                rightX = getX();
                rightY = getY() + getPaddedHeight();
                rightWidth = this.nodeWidth;
                rightHeight = remainingHeight;
            } else {
                leftX = getX();
                leftY = getY() + getPaddedHeight();
                leftWidth = getPaddedWidth();
                leftHeight = remainingHeight;

                rightX = getX() + getPaddedWidth();
                rightY = getY();
                rightWidth = remainingWidth;
                rightHeight = this.nodeHeight;
            }

            // With the theoretical nodes we have created, if the texture fits inside, create it as the node.
            if (this.leftChild == null && canFitTexture(texture, leftWidth, leftHeight)) {
                texture.setPosition(leftX, leftY);
                texture.nodeWidth = leftWidth;
                texture.nodeHeight = leftHeight;
                this.leftChild = texture;
                return true;
            }

            if (this.rightChild == null && canFitTexture(texture, rightWidth, rightHeight)) {
                texture.setPosition(rightX, rightY);
                texture.nodeWidth = rightWidth;
                texture.nodeHeight = rightHeight;
                this.rightChild = texture;
                return true;
            }

            // Couldn't find a place to fit the texture anywhere.
            return false;
        }

        private static boolean canFitTexture(AtlasTexture texture, int width, int height) {
            return (texture.getPaddedWidth() <= width) && (texture.getPaddedHeight() <= height);
        }
    }
}