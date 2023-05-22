package net.highwayfrogs.editor.file.map.view;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntry;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntryList;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.editor.MOFController;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.List;
import java.util.*;

/**
 * Represents a texture map.
 * References:
 * - https://github.com/juj/RectangleBinPack/blob/master/old/RectangleBinPack.cpp
 * - http://blackpawn.com/texts/lightmaps/default.html
 * - https://web.archive.org/web/20180913014836/http://clb.demon.fi:80/projects/rectangle-bin-packing
 * - http://www.gamedev.net/community/forums/topic.asp?topic_id=392413
 *
 * Future Ideas:
 * 1. Instead of putting all of the vertex colors in the corner, we could just add them to the tree, though preferably after all of the textures have been added. This would ensure they don't overlap with textures, and it would ensure as many vertex colors as possible are stored. This would also allow for pages of different sizes to be used.
 *
 * Created by Kneesnap on 11/28/2018.
 */
@Getter
public class TextureMap {
    private final VLOArchive vloArchive;
    private final List<Short> remapList;
    private PhongMaterial material;
    private final TextureTree textureTree;
    @Setter private ShadingMode mode;
    private final Map<Short, Set<BigInteger>> mapTextureList = new HashMap<>();
    private final ImageFilterSettings displaySettings = new ImageFilterSettings(ImageState.EXPORT).setAllowTransparency(true); // This is not static because we want it to be gc'd when the TextureMap is.
    private int width;
    private int height;
    @Setter private boolean useModelTextureAnimation;

    // The largest VLO is the SWP VLO, on the PS1. The texture map with the most used space is SUB1.

    private TextureMap(VLOArchive vlo, List<Short> remapList, ShadingMode mode, int width, int height) {
        this.vloArchive = vlo;
        this.remapList = remapList;
        this.textureTree = new TextureTree(this);
        this.mode = mode;
        this.width = width;
        this.height = height;
    }

    /**
     * Create a new texture map from an existing MOF.
     * @return newTextureMap
     */
    public static TextureMap newTextureMap(MOFHolder mofHolder, ShadingMode mode) {
        TextureMap newMap = new TextureMap(mofHolder.getVloFile(), null, mode, 0, 0);
        newMap.setUseModelTextureAnimation(true);
        newMap.updateModel(mofHolder, mode);
        return newMap;
    }

    /**
     * Create a new texture map from an existing VLOArchive.
     * @return newTextureMap
     */
    public static TextureMap newTextureMap(MAPFile mapFile, ShadingMode mode) {
        TextureMap newMap = new TextureMap(mapFile.getVlo(), mapFile.getRemapTable(), mode, 1024, 1024);
        newMap.updateMap(mapFile, mode);
        return newMap;
    }

    public int getWidth() {
        return (int) (this.width * getMode().getWidthMultiplier());
    }

    /**
     * Gets the height of the texture map.
     */
    public int getHeight() {
        return (int) (this.height * getMode().getHeightMultiplier());
    }

    /**
     * Gets the remap value for a texture.
     * @param index The index to remap
     * @return remap
     */
    public Short getRemap(short index) {
        return this.remapList != null && this.remapList.size() > index && index >= 0 ? this.remapList.get(index) : index;
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
     * @param sourceMap The map in question to update the tree with.
     */
    public void updateTree(Map<BigInteger, TextureSource> sourceMap) {
        this.textureTree.rebuildTree(sourceMap);
        if (this.material == null)
            this.material = getDiffuseMaterial();

        Image image = Utils.toFXImage(getTextureTree().getImage(), false);
        this.material.setDiffuseMap(image);
        this.material.setSpecularMap(image); // Fixes polygon lighting.
    }

    /**
     * Updates this map texture map.
     * @param mapFile The map file to update for.
     * @param newMode The shading mode to use.
     */
    public void updateMap(MAPFile mapFile, ShadingMode newMode) {
        if (newMode != null)
            this.mode = newMode;
        updateTree(createSourceMap(mapFile));
    }

    /**
     * Updates this model texture map.
     * @param mof     The model to update for.
     * @param newMode The shading mode to use.
     */
    public void updateModel(MOFHolder mof, ShadingMode newMode) {
        if (newMode != null)
            this.mode = newMode;

        boolean oldModelTextureState = this.useModelTextureAnimation;
        this.useModelTextureAnimation = false; // Makes sure animated textures are properly applied.
        Map<BigInteger, TextureSource> sourceMap = createSourceMap(mof);

        // Dynamic resizing to keep it small.
        int totalArea = 0;
        double toBase2 = Math.log10(10) / Math.log10(2);
        final int vertexArea = (MAPFile.VERTEX_COLOR_IMAGE_SIZE * MAPFile.VERTEX_COLOR_IMAGE_SIZE);
        for (TextureSource source : sourceMap.values()) {
            GameImage gameImage = source.getGameImage(this);

            if (gameImage != null) {
                totalArea += (gameImage.getFullWidth() * gameImage.getFullHeight());
            } else {
                totalArea += vertexArea;
            }
        }

        int newSize = Utils.power(2, (int) (Math.log10(Math.sqrt(totalArea * 2)) * toBase2) + 1); // One size up, because it won't be stored completely optimally.
        this.width = (int) (newSize / getMode().getWidthMultiplier());
        this.height = (int) (newSize / getMode().getHeightMultiplier());

        // Build the tree.
        updateTree(sourceMap);
        if (oldModelTextureState)
            this.useModelTextureAnimation = true; // Enables the use of animated textures.
    }

    /**
     * Creates a texture source map for a map.
     */
    private Map<BigInteger, TextureSource> createSourceMap(MAPFile map) {
        // Calculate how many of each are used.
        this.mapTextureList.clear();
        for (MAPPolygon poly : map.getAllPolygons()) {
            if (poly instanceof MAPPolyTexture) {
                MAPPolyTexture polyTex = (MAPPolyTexture) poly;
                this.mapTextureList.computeIfAbsent(polyTex.getTextureId(), key -> new HashSet<>()).add(polyTex.makeIdentifier(this));
            }
        }

        // Calculate the polygon data.
        Map<BigInteger, TextureSource> texMap = new HashMap<>();
        Set<Short> visitedTextures = new HashSet<>();
        for (MAPPolygon poly : map.getAllPolygons()) {
            BigInteger id = poly.makeIdentifier(this);
            if (!texMap.containsKey(id))
                texMap.put(id, poly);

            if (poly instanceof MAPPolyTexture && poly.isOverlay(this)) {
                MAPPolyTexture polyTex = (MAPPolyTexture) poly;
                if (visitedTextures.add(polyTex.getTextureId())) {
                    GameImage image = polyTex.getGameImage(this);
                    id = image.makeIdentifier(this);
                    if (!texMap.containsKey(id))
                        texMap.put(id, image);
                }
            }
        }

        texMap.put(UnknownTextureSource.INSTANCE.makeIdentifier(this), UnknownTextureSource.INSTANCE);
        texMap.put(MapMesh.CURSOR_COLOR.makeIdentifier(this), MapMesh.CURSOR_COLOR);
        texMap.put(MapMesh.ANIMATION_COLOR.makeIdentifier(this), MapMesh.ANIMATION_COLOR);
        texMap.put(MapMesh.INVISIBLE_COLOR.makeIdentifier(this), MapMesh.INVISIBLE_COLOR);
        texMap.put(MapMesh.GRID_COLOR.makeIdentifier(this), MapMesh.GRID_COLOR);
        texMap.put(MapMesh.REMOVE_FACE_COLOR.makeIdentifier(this), MapMesh.REMOVE_FACE_COLOR);
        texMap.put(MapMesh.GENERAL_SELECTION.makeIdentifier(this), MapMesh.GENERAL_SELECTION);
        return texMap;
    }

    /**
     * Creates a texture source map for the model.
     */
    private Map<BigInteger, TextureSource> createSourceMap(MOFHolder mof) {
        Map<BigInteger, TextureSource> texMap = new HashMap<>();

        Set<Short> visitedTextures = new HashSet<>();
        for (MOFPolygon poly : mof.asStaticFile().getAllPolygons()) {
            BigInteger id = poly.makeIdentifier(this);
            if (!texMap.containsKey(id))
                texMap.put(id, poly);

            if (poly instanceof MOFPolyTexture && poly.isOverlay(this)) {
                MOFPolyTexture polyTex = (MOFPolyTexture) poly;
                if (visitedTextures.add(polyTex.getImageId())) {
                    GameImage image = polyTex.getGameImage(this);
                    if (image == null)
                        continue;

                    id = image.makeIdentifier(this);
                    if (!texMap.containsKey(id))
                        texMap.put(id, image);
                }
            }
        }

        // Add texture animations to the map as well.
        for (MOFPart part : mof.asStaticFile().getParts()) {
            for (MOFPartPolyAnimEntryList entryList : part.getPartPolyAnimLists()) {
                for (MOFPartPolyAnimEntry entry : entryList.getEntries()) {
                    if (visitedTextures.add((short) entry.getImageId())) {
                        GameImage image = mof.getMWD().getImageByTextureId(entry.getImageId());
                        if (image == null)
                            continue;

                        BigInteger id = image.makeIdentifier(this);
                        if (!texMap.containsKey(id))
                            texMap.put(id, image);
                    }
                }
            }
        }

        texMap.put(UnknownTextureSource.INSTANCE.makeIdentifier(this), UnknownTextureSource.INSTANCE);
        texMap.put(MOFController.ANIMATION_COLOR.makeIdentifier(this), MOFController.ANIMATION_COLOR);
        texMap.put(MOFController.CANT_APPLY_COLOR.makeIdentifier(this), MOFController.CANT_APPLY_COLOR);
        return texMap;
    }

    /**
     * Gets the TextureTreeNode for a given TextureSource, if found.
     */
    public TextureTreeNode getNode(TextureSource source) {
        return this.textureTree.getAccessMap().get(source.makeIdentifier(this));
    }

    // Stuff beyond here is heavily based on https://github.com/juj/RectangleBinPack/blob/master/old/RectangleBinPack.cpp (Public Domain)

    @Getter
    public static class TextureTree {
        private final TextureMap parentMap;
        private final Map<BigInteger, TextureTreeNode> accessMap;
        private int width; // Width of tree.
        private int height; // Height of tree.
        private TextureTreeNode rootNode;
        private BufferedImage image;

        public TextureTree(TextureMap parentMap) {
            this.parentMap = parentMap;
            this.accessMap = new HashMap<>();
        }

        /**
         * Rebuilds the texture tree.
         */
        public void rebuildTree(Map<BigInteger, TextureSource> sourceMap) {
            this.width = getParentMap().getWidth();
            this.height = getParentMap().getHeight();

            this.rootNode = new TextureTreeNode(this);
            this.rootNode.setWidth(getWidth());
            this.rootNode.setHeight(getHeight());

            this.accessMap.clear();

            int minX = getWidth() - MAPFile.VERTEX_COLOR_IMAGE_SIZE; // Our goal is to start in the bottom right corner, and grow out.
            int minY = getHeight() - MAPFile.VERTEX_COLOR_IMAGE_SIZE;
            int x = minX;
            int y = minY;

            List<TextureEntry> images = new ArrayList<>();
            for (BigInteger key : sourceMap.keySet()) {
                TextureSource source = sourceMap.get(key);

                BufferedImage image = source.makeTexture(getParentMap());
                if (image == null)
                    continue;

                if (source.isOverlay(getParentMap())) {
                    this.accessMap.put(key, TextureTreeNode.newNode(this, x, y, image.getWidth(), image.getHeight(), image));

                    if (y > minY) {
                        y -= image.getHeight();
                    } else {
                        x += image.getWidth();
                    }

                    if (minY >= y && x >= getWidth()) { // We've reached the end of the cycle, time to reset.
                        minX -= image.getWidth();
                        minY -= image.getHeight();
                        x = minX;
                        y = getHeight() - MAPFile.VERTEX_COLOR_IMAGE_SIZE;
                    }
                } else {
                    images.add(new TextureEntry(key, source, image));
                }
            }

            // Now, we have to build the actual "tree" part of the texture tree.
            // This data structure seems to work best when nodes are added in descending size, so the first thing we'll do is get them in descending size.
            images.sort(Comparator.comparingInt(entry -> -(entry.getImage().getWidth() * entry.getImage().getHeight())));

            // Now, we'll add them.
            for (TextureEntry entry : images) {
                GameImage gameImage = entry.getSource().getGameImage(getParentMap());
                if (gameImage == null)
                    throw new RuntimeException("TextureSource returned null GameImage. " + entry.getSource());

                TextureTreeNode newNode = insert(gameImage);
                if (newNode != null) {
                    newNode.setImage(entry.getImage());
                    this.accessMap.put(entry.getId(), newNode);
                }
            }

            updateImage();
        }

        @Getter
        @AllArgsConstructor
        private static class TextureEntry {
            private BigInteger id;
            private TextureSource source;
            private BufferedImage image;
        }

        private TextureTreeNode insert(GameImage image) {
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
         * Updates the image, remaking it if necessary.
         */
        public void updateImage() {
            if (this.image == null || (this.image.getWidth() != getWidth() || this.image.getHeight() != getHeight())) // Gotta make a new one, the old one is invalidated.
                this.image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D graphics = this.image.createGraphics();
            graphics.setBackground(new Color(255, 255, 255, 0));
            graphics.clearRect(0, 0, this.image.getWidth(), this.image.getHeight());

            // Draw each node.
            for (TextureTreeNode node : this.accessMap.values())
                graphics.drawImage(node.getImage(), node.getX(), node.getY(), node.getWidth(), node.getHeight(), null);

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
        private BufferedImage image;

        public TextureTreeNode(TextureTree tree) {
            this.tree = tree;
        }

        /**
         * Gets this node (and subsequent nodes) returned as a display string.
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
        public static TextureTreeNode newNode(TextureTree textureTree, int x, int y, int width, int height, BufferedImage image) {
            TextureTreeNode newNode = new TextureTreeNode(textureTree);
            newNode.x = x;
            newNode.y = y;
            newNode.width = width;
            newNode.height = height;
            newNode.image = image;
            return newNode;
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ShadingMode {
        NO_SHADING("None", 1, 1),
        OVERLAY_SHADING("Overlay", 1, 1),
        MIXED_SHADING("Mixed", 2, 2), // Works to create a middle-ground between accurate and low quality.
        EVERYTHING("Texture", 4, 4);

        private final String name;
        private final double widthMultiplier;
        private final double heightMultiplier;
    }

    /**
     * Represents something which creates a texture that goes into the TextureTree.
     * Created by Kneesnap on 2/25/2020.
     */
    public interface TextureSource {
        /**
         * Creates the texture which should be put into the texture map.
         */
        BufferedImage makeTexture(TextureMap map);

        /**
         * Tests if the source creates an overlay texture, or an actual texture.
         */
        boolean isOverlay(TextureMap map);

        /**
         * Creates a hash code identifier which should match other textures that would look exactly the same, but not match others.
         */
        BigInteger makeIdentifier(TextureMap map);

        default BigInteger makeIdentifier(int... colors) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < colors.length; i++)
                sb.append(Utils.padStringLeft(Integer.toHexString(colors[i]).toUpperCase(), Constants.INTEGER_SIZE, '0'));

            return new BigInteger(sb.toString(), 16);
        }

        /**
         * Gets the GameImage this source represents, if it represents one.
         */
        GameImage getGameImage(TextureMap map);

        /**
         * Called when a mesh using this TextureSource is setup.
         */
        default void onMeshSetup(FrogMesh mesh) {
            // Do nothing, by default.
        }

        /**
         * Get the node associated with this source, if it exists.
         * @param map The map to get the node from.
         */
        default TextureTreeNode getTreeNode(TextureMap map) {
            return map.getNode(this);
        }
    }
}
