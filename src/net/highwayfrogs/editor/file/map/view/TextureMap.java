package net.highwayfrogs.editor.file.map.view;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a texture map.
 * Created by Kneesnap on 11/28/2018.
 */
@Getter
@AllArgsConstructor
public class TextureMap {
    private VLOArchive vloArchive;
    private BufferedImage image;
    private Map<Short, TextureEntry> entryMap;
    @Setter private List<Short> remapList;

    private static final ImageFilterSettings DISPLAY_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setAllowTransparency(true);

    /**
     * Create a new texture map from an existing VLOArchive.
     * @return newTextureMap
     */
    public static TextureMap newTextureMap(MOFFile mofFile) {
        return makeMap(mofFile.getHolder().getVloFile(), null, mofFile.makeVertexColorTextures());
    }

    /**
     * Create a new texture map from an existing VLOArchive.
     * @return newTextureMap
     */
    public static TextureMap newTextureMap(MAPFile mapFile) {
        return makeMap(mapFile.getVlo(), mapFile.getConfig().getRemapTable(mapFile.getFileEntry()), mapFile.makeVertexColorTextures());
    }

    private static TextureMap makeMap(VLOArchive vloSource, List<Short> remapTable, Map<VertexColor, BufferedImage> texMap) {
        int height = vloSource.getImages().stream().mapToInt(GameImage::getFullHeight).max().orElse(0); // Size of largest texture.
        int width = vloSource.getImages().stream().mapToInt(GameImage::getFullWidth).sum(); //Sum of all texture widths.
        width += (texMap.values().stream().mapToInt(BufferedImage::getWidth).sum() / (height / MAPFile.VERTEX_COLOR_IMAGE_SIZE)); // Add vertex colors.
        width += MAPFile.VERTEX_COLOR_IMAGE_SIZE;

        BufferedImage fullImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fullImage.createGraphics();

        Map<Short, TextureEntry> entryMap = new HashMap<>();
        int x = 0;
        for (GameImage image : vloSource.getImages()) {
            BufferedImage copyImage = image.toBufferedImage(DISPLAY_SETTINGS);
            graphics.drawImage(copyImage, x, 0, copyImage.getWidth(), copyImage.getHeight(), null);

            int startX = x + ((image.getFullWidth() - image.getIngameWidth()) / 2);
            int startY = (image.getFullHeight() - image.getIngameHeight()) / 2;
            entryMap.put(image.getTextureId(), TextureEntry.newEntry(startX, startY, image.getIngameWidth(), image.getIngameHeight(), width, height));
            x += image.getFullWidth();
        }

        // Vertex Color Textures.
        int y = 0;
        for (Entry<VertexColor, BufferedImage> entry : texMap.entrySet()) {
            BufferedImage image = entry.getValue();
            graphics.drawImage(image, x, y, image.getWidth(), image.getHeight(), null);
            entry.getKey().setTextureEntry(TextureEntry.newEntry(x + 1, y + 1, image.getWidth() - 2, image.getHeight() - 2, width, height));

            // Condense these things.
            if ((y += image.getHeight()) > height - image.getHeight()) {
                y = 0;
                x += image.getWidth();
            }
        }

        graphics.dispose();
        return new TextureMap(vloSource, fullImage, entryMap, remapTable);
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
    public TextureEntry getEntry(short index) {
        return getEntryMap().get(getRemap(index));
    }

    @Getter
    @Setter
    public static final class TextureEntry {
        private float minU = 0;
        private float maxU = 1;
        private float minV = 0;
        private float maxV = 1;
        private transient BufferedImage cachedImage;

        /**
         * Get this texture's xPosition.
         */
        public double getX(TextureMap map) {
            return minU * map.getImage().getWidth();
        }

        /**
         * Get this texture's yPosition.
         */
        public double getY(TextureMap map) {
            return minV * map.getImage().getHeight();
        }

        /**
         * Get this texture's width.
         */
        public double getWidth(TextureMap map) {
            return (maxU * map.getImage().getWidth()) - getX(map);
        }

        /**
         * Get this texture's height.
         */
        public double getHeight(TextureMap map) {
            return (maxV * map.getImage().getHeight()) - getY(map);
        }

        /**
         * Get this entry as a BufferedImage
         */
        public BufferedImage getImage(TextureMap map) {
            if (this.cachedImage != null)
                return cachedImage;

            int x = (int) getX(map);
            int y = (int) getY(map);
            int width = (int) getWidth(map);
            int height = (int) getHeight(map);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.drawImage(map.getImage(), 0, 0, width, height, x, y, x + width, y + height, null);
            graphics.dispose();
            this.cachedImage = image;
            return image;
        }

        /**
         * Apply this TextureEntry to a MapMesh.
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
         * Create a new TextureEntry for a texture.
         * @return texEntry
         */
        public static TextureEntry newEntry(int startX, int startY, int showWidth, int showHeight, int totalWidth, int totalHeight) {
            TextureEntry entry = new TextureEntry();
            entry.setMinU((float) startX / totalWidth);
            entry.setMaxU((float) (startX + showWidth) / totalWidth);
            entry.setMinV((float) startY / totalHeight);
            entry.setMaxV((float) (startY + showHeight) / totalHeight);
            return entry;
        }
    }
}
