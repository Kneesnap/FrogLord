package net.highwayfrogs.editor.file.map.view;

import net.highwayfrogs.editor.file.map.MAPFile;
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
 * Represents a texture which was not found.
 * Created by Kneesnap on 3/2/2020.
 */
public class UnknownTextureSource implements TextureSource, ITextureSource {
    private final Color firstColor;
    private final Color secondColor;
    private BufferedImage cachedImage;

    public static final UnknownTextureSource MAGENTA_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.MAGENTA);
    public static final UnknownTextureSource CYAN_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.CYAN);
    public static final UnknownTextureSource GREEN_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.GREEN);

    public UnknownTextureSource(Color firstColor, Color secondColor) {
        this.firstColor = firstColor;
        this.secondColor = secondColor;
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
        return makeIdentifier(0xFA175AFE);
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
        if (this.cachedImage != null)
            return this.cachedImage;

        this.cachedImage = new BufferedImage(MAPFile.VERTEX_COLOR_IMAGE_SIZE, MAPFile.VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = this.cachedImage.createGraphics();
        int xSize = (this.cachedImage.getWidth() / 2);
        int ySize = (this.cachedImage.getHeight() / 2);

        graphics.setColor(this.firstColor);
        graphics.fillRect(0, 0, xSize, ySize);
        graphics.fillRect(xSize, ySize, xSize, ySize);

        graphics.setColor(this.secondColor);
        graphics.fillRect(xSize, 0, xSize, ySize);
        graphics.fillRect(0, ySize, xSize, ySize);

        graphics.dispose();
        return this.cachedImage;
    }

    @Override
    public int getWidth() {
        return MAPFile.VERTEX_COLOR_IMAGE_SIZE;
    }

    @Override
    public int getHeight() {
        return MAPFile.VERTEX_COLOR_IMAGE_SIZE;
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
        // Nothing lol.
    }
}