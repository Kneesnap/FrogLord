package net.highwayfrogs.editor.file.map.view;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureSource;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The texture for the cursor.
 * Created by Kneesnap on 12/4/2018.
 */
@Getter
public class CursorVertexColor implements TextureSource, ITextureSource {
    private final Color bodyColor;
    private final Color outlineColor;

    public static final int VERTEX_COLOR_IMAGE_SIZE = 12;

    public CursorVertexColor(Color bodyColor, Color outlineColor) {
        this.bodyColor = bodyColor;
        this.outlineColor = outlineColor;
    }

    @Override
    public BufferedImage makeTexture(TextureMap map) {
        return makeImage();
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

    @Override
    public List<Consumer<BufferedImage>> getImageChangeListeners() {
        return new ArrayList<>();
    }

    @Override
    public BufferedImage makeImage() {
        BufferedImage image = new BufferedImage(VERTEX_COLOR_IMAGE_SIZE, VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(getBodyColor());
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        graphics.setColor(getOutlineColor());
        graphics.drawRect(0, 0, image.getWidth() - 1, image.getHeight() - 1);

        graphics.dispose();
        return image;
    }

    @Override
    public int getWidth() {
        return VERTEX_COLOR_IMAGE_SIZE;
    }

    @Override
    public int getHeight() {
        return VERTEX_COLOR_IMAGE_SIZE;
    }

    @Override
    public int getUpPadding() {
        return 0;
    }

    @Override
    public int getDownPadding() {
        return 0;
    }

    @Override
    public int getLeftPadding() {
        return 0;
    }

    @Override
    public int getRightPadding() {
        return 0;
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        // Do nothing, this doesn't change. (For now...?)
    }
}