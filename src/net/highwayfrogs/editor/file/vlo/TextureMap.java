package net.highwayfrogs.editor.file.vlo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.gui.GUIMain;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static TextureMap newTextureMap(VLOArchive vloSource, String mapName) {
        int height = vloSource.getImages().stream().mapToInt(GameImage::getFullHeight).max().getAsInt(); // Size of largest texture.
        int width = vloSource.getImages().stream().mapToInt(GameImage::getFullWidth).sum(); //Sum of all texture widths.

        BufferedImage fullImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fullImage.createGraphics();

        Map<Short, TextureEntry> entryMap = new HashMap<>();
        int x = 0;
        for (GameImage image : vloSource.getImages()) {
            BufferedImage copyImage = image.toBufferedImage(false, true, true);
            graphics.drawImage(copyImage, x, 0, copyImage.getWidth(), copyImage.getHeight(), null);

            TextureEntry entry = new TextureEntry(image);
            int startX = x + ((image.getFullWidth() - image.getIngameWidth()) / 2);
            int startY = (image.getFullHeight() - image.getIngameHeight()) / 2;

            entry.setMinU((float) ((double) startX / (double) width));
            entry.setMaxU((float) ((double) (startX + image.getIngameWidth()) / (double) width));
            entry.setMinV((float) ((double) startY / (double) height));
            entry.setMaxV((float) ((double) (startY + image.getIngameHeight()) / (double) height));
            entryMap.put(image.getTextureId(), entry);
            x += image.getFullHeight();
        }

        graphics.dispose();
        return new TextureMap(vloSource, fullImage, entryMap, GUIMain.EXE_CONFIG.getRemapTable(Utils.stripExtension(mapName)));
    }

    @Getter
    @Setter
    public static final class TextureEntry {
        private GameImage source;
        private float minU = 0;
        private float maxU = 1;
        private float minV = 0;
        private float maxV = 1;
        private int coordinateId;

        public TextureEntry(GameImage source) {
            this.source = source;
        }
    }
}
