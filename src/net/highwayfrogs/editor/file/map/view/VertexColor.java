package net.highwayfrogs.editor.file.map.view;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureTreeNode;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

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
    public void makeTexture(BufferedImage image, Graphics2D graphics, boolean raw);

    /**
     * Make this texture.
     * @return newTexture
     */
    default BufferedImage makeTexture() {
        return makeTexture(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, false);
    }

    /**
     * Make this texture.
     * @return newTexture
     */
    default BufferedImage makeTexture(int width, int height, boolean raw) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        makeTexture(image, graphics, raw);
        graphics.dispose();
        return image;
    }

    /**
     * Gets this node value from the TextureMap
     * @param map The map to get the node from.
     * @return node
     */
    public default TextureTreeNode getTreeNode(TextureMap map) {
        return map.getTextureTree().getVertexNodeMap().get(makeColorIdentifier());
    }

    public BigInteger makeColorIdentifier();

    public default BigInteger makeColorIdentifier(String prefix, int... colors) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i : colors)
            sb.append(Utils.padStringLeft(Integer.toHexString(i).toUpperCase(), Constants.INTEGER_SIZE, '0'));

        return new BigInteger(sb.toString(), 16);
    }
}
