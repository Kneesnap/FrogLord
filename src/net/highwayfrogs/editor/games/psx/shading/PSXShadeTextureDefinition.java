package net.highwayfrogs.editor.games.psx.shading;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;

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
    @Getter private final boolean semiTransparentMode;
    @Getter private final boolean includeLastPixel;
    private Consumer<BufferedImage> onTextureSourceUpdate;
    private BufferedImage cachedImage;

    public static final int UNTEXTURED_FLAT_SIZE = 8;
    public static final int UNTEXTURED_GOURAUD_SIZE = 16;
    public static final int UNTEXTURED_PADDING_SIZE = 1;

    public static final String[] QUAD_VERTEX_NAMES = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
    public static final String[] TRI_VERTEX_NAMES = {"1st Corner", "2nd Corner", "3rd Corner"};

    public PSXShadeTextureDefinition(PSXPolygonType polygonType, ITextureSource textureSource, CVector[] colors, SCByteTextureUV[] textureUVs, boolean semiTransparentMode, boolean includeLastPixel) {
        this.polygonType = polygonType;
        this.textureSource = textureSource;
        this.colors = colors;
        this.textureUVs = textureUVs;
        this.semiTransparentMode = semiTransparentMode;
        this.includeLastPixel = includeLastPixel;
    }

    /**
     * Tests if modulation should be enabled for the shade definition.
     */
    public boolean isModulated() {
        if (this.colors == null || this.colors.length == 0)
            return false;

        return this.colors[0].testFlag(CVector.FLAG_MODULATION)
                || (!this.colors[0].isCodeValid() && getPolygonType().isGouraud());
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

    /**
     * Gets display names for each vertex.
     * Flip vertically only applies to UVs, not colors or vertices.
     * @return vertexNames
     */
    public String[] getVertexNames() {
        switch (getVerticeCount()) {
            case 3:
                return TRI_VERTEX_NAMES;
            case 4:
                return QUAD_VERTEX_NAMES;
            default:
                throw new UnsupportedOperationException("Don't have vertex names for a face with " + getVerticeCount() + " vertices.");
        }
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

        if (this.semiTransparentMode)
            hash *= 5;

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
                && Arrays.equals(this.textureUVs, other.textureUVs)
                && this.semiTransparentMode == other.semiTransparentMode;
    }

    /**
     * Creates a clone of this texture definition.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public PSXShadeTextureDefinition clone() {
        CVector[] copyColors = this.colors != null ? new CVector[this.colors.length] : null;
        if (this.colors != null)
            for (int i = 0; i < this.colors.length; i++)
                copyColors[i] = this.colors[i] != null ? this.colors[i].clone() : null;

        SCByteTextureUV[] copyUvs = this.textureUVs != null ? new SCByteTextureUV[this.textureUVs.length] : null;
        if (this.textureUVs != null)
            for (int i = 0; i < this.textureUVs.length; i++)
                copyUvs[i] = this.textureUVs[i] != null ? this.textureUVs[i].clone() : null;

        return new PSXShadeTextureDefinition(this.polygonType, this.textureSource, copyColors, copyUvs, this.semiTransparentMode, this.includeLastPixel);
    }

    /**
     * Called when this object is created to be used long-term and track changes to the underlying texture source.
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
        if (this.onTextureSourceUpdate != null && this.textureSource != null) {
            this.textureSource.getImageChangeListeners().remove(this.onTextureSourceUpdate);
            this.onTextureSourceUpdate = null;
        }
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
    public boolean hasAnyTransparentPixels(BufferedImage image) {
        return this.semiTransparentMode || hasAnyTransparentPixelsImpl(image);
    }

    @Override
    public BufferedImage makeImage() {
        if (this.polygonType.isTextured() && getSourceImage() == null)
            return null; // There's no texture available.

        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
                return applyImagePostFx(PSXTextureShader.makeFlatShadedImage(getWidth(), getHeight(), this.colors[0]));
            case POLY_FT3:
            case POLY_FT4:
                return applyImagePostFx(PSXTextureShader.makeTexturedFlatShadedImage(getSourceImage(), this.colors[0]));
            case POLY_G3:
            case POLY_G4:
                return applyImagePostFx(PSXTextureShader.makeGouraudShadedImage(getWidth(), getHeight(), this.colors));
            case POLY_GT3:
            case POLY_GT4:
                return applyImagePostFx(PSXTextureShader.makeTexturedGouraudShadedImage(getSourceImage(), this.textureSource, this.colors, this.textureUVs, this.includeLastPixel));
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    private BufferedImage applyImagePostFx(BufferedImage image) {
        if (this.semiTransparentMode) {
            // Reduce opacity / alpha.
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    if ((rgb & 0xFF000000L) > 0)
                        image.setRGB(x, y, (rgb & 0x00FFFFFF) | 0x80000000);
                }
            }
        }

        return image;
    }

    @Override
    public int getWidth() {
        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
                return UNTEXTURED_FLAT_SIZE;
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_GOURAUD_SIZE;
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
                return UNTEXTURED_FLAT_SIZE;
            case POLY_G3:
            case POLY_G4:
                return UNTEXTURED_GOURAUD_SIZE;
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
                return UNTEXTURED_PADDING_SIZE;
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
                return UNTEXTURED_PADDING_SIZE;
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
                return UNTEXTURED_PADDING_SIZE;
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
                return UNTEXTURED_PADDING_SIZE;
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

    /**
     * Shaded untextured gouraud images generated by FrogLord include a small amount of padding.
     * This padding prevents the edges of polygons from bleeding a nearby texture on the texture sheet.
     * Sony Cambridge games usually seem to do this automatically when creating VLOs.
     * However, since untextured polygons don't have UVs, this is used instead.
     * @param texture The texture which the texCoord corresponds to.
     * @param textureSource The source of the texture which the texCoord corresponds to.
     * @param localUv The texture coordinate to update.
     * @return appliedShadingPadding
     */
    public static boolean tryApplyUntexturedShadingPadding(Texture texture, ITextureSource textureSource, Vector2f localUv) {
        if (!(textureSource instanceof PSXShadeTextureDefinition))
            return false;

        PSXShadeTextureDefinition shadeTexture = (PSXShadeTextureDefinition) textureSource;
        if (shadeTexture.getPolygonType().isTextured())
            return false;

        float minValue = (float) texture.getLeftPadding() / texture.getPaddedWidth();
        float maxValue = (float) (texture.getLeftPadding() + texture.getWidthWithoutPadding()) / texture.getPaddedWidth();
        if (localUv.getX() == 0F)
            localUv.setX(minValue);
        if (localUv.getX() == 1F)
            localUv.setX(maxValue);
        if (localUv.getY() == 0F)
            localUv.setY(minValue);
        if (localUv.getY() == 1F)
            localUv.setY(maxValue);

        return true;
    }
}