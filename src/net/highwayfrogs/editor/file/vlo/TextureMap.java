package net.highwayfrogs.editor.file.vlo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.standard.psx.prims.VertexColor;
import net.highwayfrogs.editor.gui.GUIMain;

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
    private List<Short> remapList;

    /**
     * Create a new texture map from an existing VLOArchive.
     * @param vloSource The source to create the map from.
     * @return newTextureMap
     */
    public static TextureMap newTextureMap(MAPFile mapFile, VLOArchive vloSource, String mapName) {
        Map<VertexColor, BufferedImage> texMap = mapFile.makeVertexColorTextures();

        int height = vloSource.getImages().stream().mapToInt(GameImage::getFullHeight).max().getAsInt(); // Size of largest texture.
        int width = vloSource.getImages().stream().mapToInt(GameImage::getFullWidth).sum(); //Sum of all texture widths.
        width += (texMap.values().stream().mapToInt(BufferedImage::getWidth).sum() / (height / MAPFile.VERTEX_COLOR_IMAGE_SIZE)); // Add vertex colors.
        width += MAPFile.VERTEX_COLOR_IMAGE_SIZE;

        BufferedImage fullImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fullImage.createGraphics();

        Map<Short, TextureEntry> entryMap = new HashMap<>();
        int x = 0;
        for (GameImage image : vloSource.getImages()) {
            BufferedImage copyImage = image.toBufferedImage(false, true, false);
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
        return new TextureMap(vloSource, fullImage, entryMap, GUIMain.EXE_CONFIG.getRemapTable(Utils.stripExtension(mapName)));
    }

    @Getter
    @Setter
    public static final class TextureEntry {
        private float minU = 0;
        private float maxU = 1;
        private float minV = 0;
        private float maxV = 1;

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
