package net.highwayfrogs.editor.file.map.view;

import net.highwayfrogs.editor.file.map.view.TextureMap.TextureEntry;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents a polygon which has a VertexColor.
 * Created by Kneesnap on 12/1/2018.
 */
public interface VertexColor {

    /**
     * Set the cached vertex color texture information.
     * @param entry Vertex color information.
     */
    public void setTextureEntry(TextureEntry entry);

    /**
     * Make the displayed texture on an image.
     * @param image    The image.
     * @param graphics The graphics to write to.
     */
    public void makeTexture(BufferedImage image, Graphics2D graphics);
}
