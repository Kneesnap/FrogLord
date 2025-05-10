package net.highwayfrogs.editor.file.map.view;

import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a texture which was not found.
 * Created by Kneesnap on 3/2/2020.
 */
public class UnknownTextureSource implements ITextureSource {
    private final Color firstColor;
    private final Color secondColor;
    private BufferedImage cachedImage;

    public static final UnknownTextureSource MAGENTA_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.MAGENTA);
    public static final UnknownTextureSource CYAN_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.CYAN);
    public static final UnknownTextureSource GREEN_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.GREEN);
    public static final UnknownTextureSource YELLOW_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.YELLOW);
    public static final UnknownTextureSource RED_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.RED);
    public static final UnknownTextureSource ORANGE_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.ORANGE);
    public static final UnknownTextureSource PINK_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.PINK);
    public static final UnknownTextureSource DARK_BLUE_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.BLUE);
    public static final UnknownTextureSource WHITE_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.WHITE);
    public static final UnknownTextureSource GRAY_INSTANCE = new UnknownTextureSource(Color.BLACK, Color.GRAY);
    public static final UnknownTextureSource DARK_GREEN_INSTANCE = new UnknownTextureSource(Color.BLACK, new Color(0x133201));
    public static final UnknownTextureSource BROWN_INSTANCE = new UnknownTextureSource(Color.BLACK, new Color(0x6E260E));
    public static final UnknownTextureSource NEON_GREEN_INSTANCE = new UnknownTextureSource(Color.BLACK, new Color(0x37FBB3));
    public static final UnknownTextureSource LIGHT_YELLOW_INSTANCE = new UnknownTextureSource(Color.BLACK, new Color(0xFFFF80));
    public static final UnknownTextureSource DEEP_BLUE_INSTANCE = new UnknownTextureSource(Color.BLACK, new Color(0x027148));
    public static final UnknownTextureSource LIGHT_PURPLE_INSTANCE = new UnknownTextureSource(Color.BLACK, new Color(0xB332CD));

    public UnknownTextureSource(Color firstColor, Color secondColor) {
        this.firstColor = firstColor;
        this.secondColor = secondColor;
    }

    @Override
    public List<Consumer<BufferedImage>> getImageChangeListeners() {
        return new ArrayList<>();
    }

    @Override
    public BufferedImage makeImage() {
        if (this.cachedImage != null)
            return this.cachedImage;

        this.cachedImage = new BufferedImage(CursorVertexColor.VERTEX_COLOR_IMAGE_SIZE, CursorVertexColor.VERTEX_COLOR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
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
        return CursorVertexColor.VERTEX_COLOR_IMAGE_SIZE;
    }

    @Override
    public int getHeight() {
        return CursorVertexColor.VERTEX_COLOR_IMAGE_SIZE;
    }

    @Override
    public int getUpPadding() {
        return 1;
    }

    @Override
    public int getDownPadding() {
        return 1;
    }

    @Override
    public int getLeftPadding() {
        return 1;
    }

    @Override
    public int getRightPadding() {
        return 1;
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        // Nothing lol.
    }
}