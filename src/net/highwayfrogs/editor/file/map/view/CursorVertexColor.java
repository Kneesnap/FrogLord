package net.highwayfrogs.editor.file.map.view;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureSource;
import net.highwayfrogs.editor.file.vlo.GameImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

/**
 * The texture for the cursor.
 * Created by Kneesnap on 12/4/2018.
 */
@Getter
public class CursorVertexColor implements TextureSource {
    private Color bodyColor;
    private Color outlineColor;

    public CursorVertexColor(Color bodyColor, Color outlineColor) {
        this.bodyColor = bodyColor;
        this.outlineColor = outlineColor;
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        BufferedImage image = new BufferedImage(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(getBodyColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.setColor(getOutlineColor());
        graphics.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);

        graphics.dispose();
        return image;
    }

    @Override
    public boolean isOverlay(TextureMap map) {
        return true;
    }

    @Override
    public BigInteger makeIdentifier(TextureMap map) {
        return makeIdentifier(0x0BE1A7, this.bodyColor.getRGB(), this.outlineColor.getRGB());
    }

    @Override
    public GameImage getGameImage(TextureMap map) {
        return null;
    }
}
