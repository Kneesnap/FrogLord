package net.highwayfrogs.editor.file.map.view;

import javafx.scene.paint.PhongMaterial;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a texture map.
 * References:
 * - https://github.com/juj/RectangleBinPack/blob/master/old/RectangleBinPack.cpp
 * - http://blackpawn.com/texts/lightmaps/default.html
 * - https://web.archive.org/web/20180913014836/http://clb.demon.fi:80/projects/rectangle-bin-packing
 * - http://www.gamedev.net/community/forums/topic.asp?topic_id=392413
 * Created by Kneesnap on 11/28/2018.
 */
@Getter
@AllArgsConstructor
public class TextureMap {
    private VLOArchive vloArchive;
    @Setter private List<Short> remapList;
    private PhongMaterial material;
    private TextureTree textureTree;

    private static final int TEXTURE_PAGE_WIDTH = 1024; // The largest VLO is the SWP VLO, on the PS1.
    private static final int TEXTURE_PAGE_HEIGHT = 1024;
    private static final ImageFilterSettings DISPLAY_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setAllowTransparency(true);

    /**
     * Create a new texture map from an existing MOF.
     * @return newTextureMap
     */
    public static TextureMap newTextureMap(MOFHolder mofHolder) {
        return makeMap(mofHolder.getVloFile(), null, mofHolder.asStaticFile().makeVertexColorTextures());
    }

    /**
     * Create a new texture map from an existing VLOArchive.
     * @return newTextureMap
     */
    public static TextureMap newTextureMap(MAPFile mapFile) {
        return makeMap(mapFile.getVlo(), mapFile.getConfig().getRemapTable(mapFile.getFileEntry()), mapFile.makeVertexColorTextures());
    }

    private static TextureMap makeMap(VLOArchive vloSource, List<Short> remapTable, Map<VertexColor, BufferedImage> texMap) {
        return new TextureMap(vloSource, remapTable, null, new TextureTree(vloSource, texMap));
    }

    /**
     * Gets the remap value for a texture.
     * @param index The index to remap
     * @return remap
     */
    public Short getRemap(short index) {
        return this.remapList != null ? this.remapList.get(index) : index;
    }

    /**
     * Get the entry for the tex id.
     * @param index The index.
     * @return entry
     */
    public TextureTreeNode getEntry(short index) {
        return getTextureTree().getEntryMap().get(getRemap(index));
    }

    /**
     * Gets the 3D PhongMaterial (diffuse components only, affected by lighting).
     * @return phongMaterial
     */
    public PhongMaterial getDiffuseMaterial() {
        if (this.material == null)
            this.material = Utils.makeDiffuseMaterial(Utils.toFXImage(getTextureTree().getImage(), false));
        return this.material;
    }

    /**
     * Updates the color data for the tree.
     * @param vertexMap The map in question to update.
     */
    public void updateTreeColorData(Map<VertexColor, BufferedImage> vertexMap) {
        this.textureTree.vertexMap = vertexMap;
        this.textureTree.buildColorMap();
        this.textureTree.updateImage();
        if (this.material != null)
            this.material.setDiffuseMap(Utils.toFXImage(getTextureTree().getImage(), false));
    }

    // Stuff beyond here is heavily based on https://github.com/juj/RectangleBinPack/blob/master/old/RectangleBinPack.cpp (Public Domain)

    @Getter
    public static class TextureTree {
        private final VLOArchive vloSource;
        private Map<VertexColor, BufferedImage> vertexMap;
        private final Map<BigInteger, TextureTreeNode> vertexNodeMap;
        private final int width = TEXTURE_PAGE_WIDTH; // Width of tree.
        private final int height = TEXTURE_PAGE_HEIGHT; // Height of tree.
        private TextureTreeNode rootNode;
        private Map<Short, TextureTreeNode> entryMap = new HashMap<>();
        private BufferedImage image;

        public TextureTree(VLOArchive vloSource, Map<VertexColor, BufferedImage> vertexMap) {
            this.vloSource = vloSource;
            this.vertexMap = vertexMap;
            this.vertexNodeMap = new HashMap<>();
            buildTree(); // Builds the tree contents.
            updateImage(); // Makes the image.
        }

        public TextureTreeNode insert(GameImage image) {
            return insert(getRootNode(), image);
        }

        private TextureTreeNode insert(TextureTreeNode node, GameImage image) {
            int width = image.getFullWidth();
            int height = image.getFullHeight();

            if (node.getLeft() != null || node.getRight() != null) {
                if (node.getLeft() != null) {
                    TextureTreeNode newNode = insert(node.getLeft(), image);
                    if (newNode != null)
                        return newNode;
                }

                if (node.getRight() != null)
                    return insert(node.getRight(), image);

                return null; // Didn't fit into either sub-tree.
            }

            // This node is a leaf, but can we fit the new rectangle here?
            if (width > node.getWidth() || height > node.getHeight())
                return null; // Nope, there isn't enough space.

            int w = node.getWidth() - width;
            int h = node.getHeight() - height;
            node.setLeft(new TextureTreeNode(this));
            node.setRight(new TextureTreeNode(this));

            TextureTreeNode left = node.getLeft();
            TextureTreeNode right = node.getRight();
            if (w <= h) { // Split the remaining space in the horizontal dimension.
                left.setX(node.getX() + width);
                left.setY(node.getY());
                left.setWidth(w);
                left.setHeight(height);
                right.setX(node.getX());
                right.setY(node.getY() + height);
                right.setWidth(node.getWidth());
                right.setHeight(h);
            } else {
                left.setX(node.getX());
                left.setY(node.getY() + height);
                left.setWidth(width);
                left.setHeight(h);

                right.setX(node.getX() + width);
                right.setY(node.getY());
                right.setWidth(w);
                right.setHeight(node.getHeight());
            }

            // Note that as a result of the above, it can happen that node->left or node->right
            // is now a degenerate (zero area) rectangle. No need to do anything about it,
            // like remove the nodes as "unnecessary" since they need to exist as children of
            // this node (this node can't be a leaf anymore).

            // This node is now a non-leaf, so shrink its area - it now denotes
            // *occupied* space instead of free space. Its children spawn the resulting area of free space.
            node.setWidth(width);
            node.setHeight(height);
            node.setGameImage(image);
            return node;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            getRootNode().toString("", builder, "Root");
            return builder.toString();
        }

        /**
         * Builds this tree from scratch.
         */
        public void buildTree() {
            this.rootNode = new TextureTreeNode(this);
            this.rootNode.setWidth(getWidth());
            this.rootNode.setHeight(getHeight());

            // Build the tree.
            for (int i = 0; i < getVloSource().getImages().size(); i++)
                insert(getVloSource().getImages().get(i));

            buildEntryMap();
            buildColorMap();
        }

        // Rebuilds the entry map.
        private void buildEntryMap() {
            getEntryMap().clear();
            handleNode(getRootNode());
        }

        private void buildColorMap() {
            getVertexNodeMap().clear();

            int minX = getWidth() - ((int) Math.ceil(Math.sqrt(getVertexMap().size())) * MAPFile.VERTEX_COLOR_IMAGE_SIZE);
            int startX = getWidth() - MAPFile.VERTEX_COLOR_IMAGE_SIZE;
            int x = startX;
            int y = getHeight() - MAPFile.VERTEX_COLOR_IMAGE_SIZE;
            for (Entry<VertexColor, BufferedImage> entry : getVertexMap().entrySet()) {
                BufferedImage image = entry.getValue();
                TextureTreeNode vtxNode = TextureTreeNode.newNode(this, x, y, image.getWidth(), image.getHeight());
                vtxNode.setCachedImage(image);
                getVertexNodeMap().put(entry.getKey().makeColorIdentifier(), vtxNode);

                // Condense these things.
                if ((x -= image.getWidth()) < minX) { // Start again at the bottom (move over horizontally) once it clashes with a texture.
                    y -= image.getHeight();
                    x = startX;
                }
            }

            this.vertexMap = null; // No longer needed.
        }

        private void handleNode(TextureTreeNode node) {
            if (node.getGameImage() != null)
                getEntryMap().put(node.getGameImage().getTextureId(), node);

            if (node.getLeft() != null)
                handleNode(node.getLeft());
            if (node.getRight() != null)
                handleNode(node.getRight());
        }

        /**
         * Updates the image, remaking it if necessary.
         */
        public void updateImage() {
            if (this.image == null) // Gotta make a new one, the old one is invalidated.
                this.image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D graphics = this.image.createGraphics();

            // Writes the images.
            for (short i = 0; i < getVloSource().getImages().size(); i++) {
                GameImage image = getVloSource().getImages().get(i);
                TextureTreeNode node = getEntryMap().get(image.getTextureId());
                if (node == null)
                    continue; // Got nothing!

                graphics.drawImage(image.toBufferedImage(DISPLAY_SETTINGS), node.getX(), node.getY(), node.getWidth(), node.getHeight(), null);
            }

            // Draw the vertex color entries.
            for (Entry<BigInteger, TextureTreeNode> entry : getVertexNodeMap().entrySet()) {
                TextureTreeNode node = entry.getValue();
                graphics.drawImage(node.getCachedImage(), node.getX(), node.getY(), node.getWidth(), node.getHeight(), null);
            }

            graphics.dispose();
        }
    }

    @Getter
    @Setter
    public static class TextureTreeNode {
        private final TextureTree tree;
        private TextureTreeNode left;
        private TextureTreeNode right;
        private int x;
        private int y;
        private int width;
        private int height;
        private GameImage gameImage;
        private BufferedImage cachedImage;

        private static final ImageFilterSettings NODE_DISPLAY_SETTING = new ImageFilterSettings(ImageState.EXPORT)
                .setAllowTransparency(true).setTrimEdges(true).setAllowFlip(true);

        public TextureTreeNode(TextureTree tree) {
            this.tree = tree;
        }

        /**
         * Used when getting tree as a string.
         */
        public void toString(String padding, StringBuilder builder, String prefix) {
            builder.append(padding).append("- ");
            if (prefix != null)
                builder.append(prefix).append(" ");
            builder.append("Node ").append(gameImage != null ? gameImage.getLocalImageID() : -1).append("@[").append(this.x).append(", ").append(this.y)
                    .append(", ").append(this.width).append(", ").append(this.height).append("]").append(Constants.NEWLINE);
            if (getLeft() != null)
                getLeft().toString(padding + " ", builder, "Left");
            if (getRight() != null)
                getRight().toString(padding + " ", builder, "Right");
        }

        public float getMinU() {
            return (float) getStartX() / (float) getTree().getWidth();
        }

        public float getMaxU() {
            return (float) (getStartX() + (getGameImage() != null ? getGameImage().getIngameWidth() : MAPFile.VERTEX_COLOR_IMAGE_SIZE - 2)) / (float) getTree().getWidth();
        }

        public float getMinV() {
            return (float) getStartY() / (float) getTree().getHeight();
        }

        public float getMaxV() {
            return (float) (getStartY() + (getGameImage() != null ? getGameImage().getIngameHeight() : MAPFile.VERTEX_COLOR_IMAGE_SIZE - 2)) / (float) getTree().getHeight();
        }

        private int getStartX() {
            return getX() + (getGameImage() != null ? ((getGameImage().getFullWidth() - getGameImage().getIngameWidth()) / 2) : 1);
        }

        private int getStartY() {
            return getY() + (getGameImage() != null ? ((getGameImage().getFullHeight() - getGameImage().getIngameHeight()) / 2) : 1);
        }

        /**
         * Get this entry's texture image.
         */
        public BufferedImage getImage() {
            return this.cachedImage != null ? this.cachedImage : (this.cachedImage = getGameImage().toBufferedImage(NODE_DISPLAY_SETTING));
        }

        /**
         * Apply this node to a MapMesh.
         * @param mesh      The mesh to apply this entry to.
         * @param vertCount The amount of vertices to add.
         */
        public void applyMesh(FrogMesh mesh, int vertCount) {
            mesh.getTexCoords().addAll(getMinU(), getMinV());
            mesh.getTexCoords().addAll(getMinU(), getMaxV());
            mesh.getTexCoords().addAll(getMaxU(), getMinV());
            if (vertCount == MAPPolygon.QUAD_SIZE)
                mesh.getTexCoords().addAll(getMaxU(), getMaxV());
        }

        /**
         * Makes a new texture node with given data.
         * @return newNode
         */
        public static TextureTreeNode newNode(TextureTree textureTree, int x, int y, int width, int height) {
            TextureTreeNode newNode = new TextureTreeNode(textureTree);
            newNode.setX(x);
            newNode.setY(y);
            newNode.setWidth(width);
            newNode.setHeight(height);
            return newNode;
        }
    }
}
