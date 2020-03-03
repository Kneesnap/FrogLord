package net.highwayfrogs.editor.file.map.view;

import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureSource;
import net.highwayfrogs.editor.file.vlo.GameImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * Represents a texture which was not found.
 * Created by Kneesnap on 3/2/2020.
 */
public class UnknownTextureSource implements TextureSource {
    private BufferedImage cachedImage;

    public static final UnknownTextureSource INSTANCE = new UnknownTextureSource();
    private static final Color COLOR1 = Color.BLACK;
    private static final Color COLOR2 = Color.MAGENTA;

    private UnknownTextureSource() {
        // Prevent creation, there should only be one instance, the INSTANCE variable.
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        if (this.cachedImage != null)
            return this.cachedImage;

        this.cachedImage = new BufferedImage(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = this.cachedImage.createGraphics();
        int xSize = (this.cachedImage.getWidth() / 2);
        int ySize = (this.cachedImage.getHeight() / 2);

        graphics.setColor(COLOR1);
        graphics.fillRect(0, 0, xSize, ySize);
        graphics.fillRect(xSize, ySize, xSize, ySize);

        graphics.setColor(COLOR2);
        graphics.fillRect(xSize, 0, xSize, ySize);
        graphics.fillRect(0, ySize, xSize, ySize);

        graphics.dispose();
        return this.cachedImage;
    }

    @Override
    public boolean isOverlay(TextureMap map) {
        return true;
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        return makeIdentifier(0xFA175AFE);
    }

    @Override
    public GameImage getGameImage(TextureMap map) {
        return null;
    }
}
