package net.highwayfrogs.editor.gui.texture.atlas;

import lombok.Getter;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.SortedList;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds the image for a texture atlas.
 * Back-ported from ModToolFramework.
 * Created by Kneesnap on 9/23/2023.
 */
@Getter
public class AtlasBuilderTextureSource implements ITextureSource {
    private final TextureAtlas atlas;
    private final List<Consumer<BufferedImage>> imageChangeListeners;

    public AtlasBuilderTextureSource(TextureAtlas atlas) {
        if (atlas == null)
            throw new NullPointerException("atlas");

        this.atlas = atlas;
        this.imageChangeListeners = new ArrayList<>();
    }

    @Override
    public BufferedImage makeImage() {
        BufferedImage newImage = new BufferedImage(this.atlas.getAtlasWidth(), this.atlas.getAtlasHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();

        g.setBackground(new Color(255, 255, 255, 0));
        g.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());

        // Draw each image.
        try {
            SortedList<? extends AtlasTexture> sortedTextures = this.atlas.getSortedTextureList();
            for (int i = 0; i < sortedTextures.size(); i++) {
                AtlasTexture texture = sortedTextures.get(i);
                g.drawImage(texture.getImage(), texture.getX() + texture.getLeftPaddingEmpty(), texture.getY() + texture.getUpPaddingEmpty(), texture.getNonEmptyPaddedWidth(), texture.getNonEmptyPaddedHeight(), null);
            }
        } finally {
            g.dispose();
        }

        return newImage;
    }

    @Override
    public int getWidth() {
        return this.atlas.getAtlasWidth();
    }

    @Override
    public int getHeight() {
        return this.atlas.getAtlasHeight();
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
        this.fireChangeEvent0(newImage);
    }
}