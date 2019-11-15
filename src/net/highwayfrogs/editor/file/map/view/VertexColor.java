package net.highwayfrogs.editor.file.map.view;

import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Represents a polygon which has a VertexColor.
 * Created by Kneesnap on 12/1/2018.
 */
public interface VertexColor {
    /**
     * Make the displayed texture on an image.
     * @param image    The image.
     * @param graphics The graphics to write to.
     */
    public void makeTexture(BufferedImage image, Graphics2D graphics);

    /**
     * Make this texture.
     * @return newTexture
     */
    default BufferedImage makeTexture() {
        BufferedImage image = new BufferedImage(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        makeTexture(image, graphics);
        graphics.dispose();
        return image;
    }

    /**
     * Gets this node value from the TextureMap
     * @param map The map to get the node from.
     * @return node
     */
    public default TextureTreeNode getTreeNode(TextureMap map) {
        return map.getTextureTree().getVertexNodeMap().get(this);
    }
}
