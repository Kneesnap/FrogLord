package net.highwayfrogs.editor.gui.texture.atlas;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.SortedList;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Builds the image for a texture atlas.
 * Back-ported from ModToolFramework.
 * Caching is utilized to avoid intensive operations for each update.
 * Created by Kneesnap on 9/23/2023.
 */
@Getter
public class AtlasBuilderTextureSource implements ITextureSource {
    private final TextureAtlas atlas;
    private final List<Consumer<BufferedImage>> imageChangeListeners;
    private final Set<DynamicMeshNode> updatedNodes = new HashSet<>();
    private BufferedImage cachedImage; // Caching the image allows for faster generation.
    @Setter private DynamicMesh mesh;

    public AtlasBuilderTextureSource(TextureAtlas atlas) {
        if (atlas == null)
            throw new NullPointerException("atlas");

        this.atlas = atlas;
        this.imageChangeListeners = new ArrayList<>();
    }

    /**
     * Makes a new image containing the texture sheet entries.
     * By default, caching of the image is enabled, to avoid major updates.
     * @return newImage
     */
    public BufferedImage makeNewImage() {
        BufferedImage newImage = new BufferedImage(this.atlas.getAtlasWidth(), this.atlas.getAtlasHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();

        g.setBackground(new Color(255, 255, 255, 0));
        g.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());

        // Draw each image.
        try {
            this.atlas.prepareImageGeneration();
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
    public boolean hasAnyTransparentPixels(BufferedImage image) {
        return true; // Empty space is transparent.
    }

    @Override
    public BufferedImage makeImage() {
        if (this.cachedImage == null || (this.atlas.getAtlasWidth() != this.cachedImage.getWidth()) || (this.atlas.getAtlasHeight() != this.cachedImage.getHeight())) {
            this.cachedImage = makeNewImage();

            // Ensure image cache is okay.
            SortedList<? extends AtlasTexture> sortedTextures = this.atlas.getSortedTextureList();
            for (int i = 0; i < sortedTextures.size(); i++)
                sortedTextures.get(i).onTextureWrittenToAtlas();

            updateTextureCoordinates();
            return this.cachedImage;
        }

        Graphics2D g = this.cachedImage.createGraphics();

        g.setBackground(new Color(255, 255, 255, 0));
        g.setComposite(AlphaComposite.Src); // If we write a transparent image, it will still delete whatever image data is there already.

        // Update each image marked for update.
        try {
            this.atlas.prepareImageGeneration();
            SortedList<? extends AtlasTexture> sortedTextures = this.atlas.getSortedTextureList();
            for (int i = 0; i < sortedTextures.size(); i++) {
                AtlasTexture texture = sortedTextures.get(i);
                if (texture.isAtlasCachedImageInvalid()) {
                    g.drawImage(texture.getImage(), texture.getX() + texture.getLeftPaddingEmpty(), texture.getY() + texture.getUpPaddingEmpty(), texture.getNonEmptyPaddedWidth(), texture.getNonEmptyPaddedHeight(), null);
                    texture.onTextureWrittenToAtlas();
                }
            }
        } finally {
            g.dispose();
        }

        updateTextureCoordinates();
        return this.cachedImage;
    }

    private void updateTextureCoordinates() {
        if (this.mesh == null || this.mesh.getEditableTexCoords() == null || this.mesh.getEditableTexCoords().size() == 0)
            return;

        this.mesh.getEditableTexCoords().startBatchingUpdates();
        boolean foundInvalidTexCoords = false;
        SortedList<? extends AtlasTexture> sortedTextures = this.atlas.getSortedTextureList();
        for (int i = 0; i < sortedTextures.size(); i++) {
            AtlasTexture texture = sortedTextures.get(i);
            if (!texture.isMeshTextureCoordsInvalid())
                continue;

            foundInvalidTexCoords = true;

            // Update texture coordinates for this texture.
            if (texture.getTextureSource() instanceof PSXShadeTextureDefinition) {
                PSXShadeTextureDefinition shadeTexture = (PSXShadeTextureDefinition) texture.getTextureSource();
                if (this.getMesh().getShadedTextureManager() != null)
                    this.mesh.getShadedTextureManager().updateTextureCoordinates(shadeTexture, this.updatedNodes);
            }

            // Mark the texture coordinates as up to date.
            texture.onTextureUvsUpdated();
        }

        // Update the other non-shaded polygon texture coordinates.
        if (foundInvalidTexCoords) {
            this.mesh.updateNonShadedPolygonTexCoords(this.updatedNodes);
            this.updatedNodes.clear();
        }

        this.mesh.getEditableTexCoords().endBatchingUpdates();
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