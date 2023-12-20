package net.highwayfrogs.editor.games.sony.shared.shading;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This defines all the information necessary for the PlayStation texture shading recreation system to shade a texture.
 * Created by Kneesnap on 12/19/2023.
 */
public final class PSXShadeTextureDefinition implements ITextureSource {
    @Getter private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    @Getter private final PSXPolygonType polygonType;
    @Getter private final ITextureSource textureSource;
    @Getter private final CVector[] colors;
    @Getter private final SCByteTextureUV[] textureUVs;
    private Consumer<BufferedImage> onTextureSourceUpdate;
    private BufferedImage cachedImage;

    public static final int UNTEXTURED_IMAGE_FLAT_DIMENSIONS = 8;
    public static final int UNTEXTURED_IMAGE_GOURAUD_DIMENSIONS = 16;
    public static final int UNTEXTURED_IMAGE_PADDING_DIMENSIONS = 1;

    public PSXShadeTextureDefinition(PSXPolygonType polygonType, ITextureSource textureSource, CVector[] colors, SCByteTextureUV[] textureUVs) {
        this.polygonType = polygonType;
        this.textureSource = textureSource;
        this.colors = colors;
        this.textureUVs = textureUVs;
    }

    /**
     * Gets the number of vertices this polygon has.
     */
    public int getVerticeCount() {
        return getPolygonType().getVerticeCount();
    }

    /**
     * Check if this polygon is textured.
     */
    public boolean isTextured() {
        return getPolygonType().isTextured();
    }

    @Override
    public int hashCode() {
        int hash = getPolygonType().ordinal();

        // Add colors.
        if (this.colors != null)
            for (int i = 0; i < this.colors.length; i++)
                hash *= (this.colors[i].toRGB() + 1);

        // Add UVs
        if (this.textureUVs != null)
            for (int i = 0; i < this.textureUVs.length; i++)
                hash *= (this.textureUVs[i].hashCode() + 1);

        // Add texture.
        if (this.textureSource != null)
            hash ^= this.textureSource.hashCode();

        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PSXShadeTextureDefinition))
            return false;

        PSXShadeTextureDefinition other = (PSXShadeTextureDefinition) object;
        return this.polygonType == other.polygonType
                && Objects.equals(this.textureSource, other.textureSource)
                && Arrays.equals(this.colors, other.colors)
                && Arrays.equals(this.textureUVs, other.textureUVs);
    }

    /**
     * Called when this object is setup to be used long-term and track changes to the underlying texture source.
     */
    public void onRegister() {
        if (this.onTextureSourceUpdate != null)
            return; // Already registered.

        // Setup texture source listener.
        if (this.polygonType.isTextured() && this.textureSource != null) {
            this.onTextureSourceUpdate = this::onTextureSourceUpdate;
            this.textureSource.getImageChangeListeners().add(this.onTextureSourceUpdate);
        }
    }

    /**
     * Called when this object is no longer necessary and can be released.
     */
    public void onDispose() {
        if (this.onTextureSourceUpdate != null && this.textureSource != null)
            this.textureSource.getImageChangeListeners().remove(this.onTextureSourceUpdate);
    }

    /**
     * Gets or creates the source image (From the underlying texture source), if there is one.
     * @return sourceImage
     */
    public BufferedImage getSourceImage() {
        if (this.cachedImage != null)
            return this.cachedImage;

        return this.cachedImage = (this.textureSource != null ? this.textureSource.makeImage() : null);
    }

    @Override
    public BufferedImage makeImage() {
        if (this.polygonType.isTextured() && getSourceImage() == null)
            return null; // There's no texture available.

        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
                return PSXTextureShader.makeFlatShadedImage(getWidth(), getHeight(), this.colors[0]);
            case POLY_FT3:
            case POLY_FT4:
                return PSXTextureShader.makeTexturedFlatShadedImage(getSourceImage(), this.colors[0]);
            case POLY_G3:
            case POLY_G4:
                return PSXTextureShader.makeGouraudShadedImage(getWidth(), getHeight(), this.colors);
            case POLY_GT3:
            case POLY_GT4:
                return PSXTextureShader.makeTexturedGouraudShadedImage(getSourceImage(), this.colors, this.textureUVs);
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    @Override
    public int getWidth() {
        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
                return UNTEXTURED_IMAGE_FLAT_DIMENSIONS;
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_IMAGE_GOURAUD_DIMENSIONS;
            case POLY_FT3:
            case POLY_FT4:
            case POLY_GT3:
            case POLY_GT4:
                return this.textureSource != null ? this.textureSource.getWidth() : 0;
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    @Override
    public int getHeight() {
        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
                return UNTEXTURED_IMAGE_FLAT_DIMENSIONS;
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_IMAGE_GOURAUD_DIMENSIONS;
            case POLY_FT3:
            case POLY_FT4:
            case POLY_GT3:
            case POLY_GT4:
                return this.textureSource != null ? this.textureSource.getHeight() : 0;
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    @Override
    public int getUpPadding() {
        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
            case POLY_FT3:
            case POLY_FT4:
            case POLY_GT3:
            case POLY_GT4:
                return this.textureSource != null ? this.textureSource.getUpPadding() : 0;
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    @Override
    public int getDownPadding() {
        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
            case POLY_FT3:
            case POLY_FT4:
            case POLY_GT3:
            case POLY_GT4:
                return this.textureSource != null ? this.textureSource.getDownPadding() : 0;
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    @Override
    public int getLeftPadding() {
        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
            case POLY_FT3:
            case POLY_FT4:
            case POLY_GT3:
            case POLY_GT4:
                return this.textureSource != null ? this.textureSource.getLeftPadding() : 0;
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    @Override
    public int getRightPadding() {
        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_IMAGE_PADDING_DIMENSIONS;
            case POLY_FT3:
            case POLY_FT4:
            case POLY_GT3:
            case POLY_GT4:
                return this.textureSource != null ? this.textureSource.getRightPadding() : 0;
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    private void onTextureSourceUpdate(BufferedImage newImage) {
        this.cachedImage = newImage;
        fireChangeEvent(makeImage()); // This image should now update due to the change in the underlying image.
    }

    @Override
    public void fireChangeEvent(BufferedImage newImage) {
        fireChangeEvent0(newImage);
    }
}