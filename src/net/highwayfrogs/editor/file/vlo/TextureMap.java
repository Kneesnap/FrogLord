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
        int height = vloSource.getImages().stream().mapToInt(GameImage::getIngameHeight).max().getAsInt(); // Size of largest texture.
        int width = vloSource.getImages().stream().mapToInt(GameImage::getIngameWidth).sum(); //Sum of all texture widths.

        BufferedImage fullImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = fullImage.createGraphics();

        Map<Short, TextureEntry> entryMap = new HashMap<>();
        int x = 0;
        for (GameImage image : vloSource.getImages()) {
            BufferedImage copyImage = image.toBufferedImage(true, true, true);
            graphics.drawImage(copyImage, x, 0, copyImage.getWidth(), copyImage.getHeight(), null);

            TextureEntry entry = new TextureEntry(image);
            entry.setMinU((float) ((double) x / (double) width));
            entry.setMaxU((float) ((double) (x + image.getIngameWidth()) / (double) width));
            entry.setMaxV((float) ((double) copyImage.getHeight() / (double) height));
            entryMap.put(image.getTextureId(), entry);
            x += image.getIngameWidth();
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
