package net.highwayfrogs.editor.games.psx.shading;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.psx.math.vector.CVector;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.Texture;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.image.ImageUtils;

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
    private final PSXShadedTextureManager<?> shadedTextureManager;
    @Getter private final List<Consumer<BufferedImage>> imageChangeListeners = new ArrayList<>();
    @Getter private final PSXPolygonType polygonType;
    @Getter private final ITextureSource textureSource;
    @Getter private final CVector[] colors;
    @Getter private final SCByteTextureUV[] textureUVs;
    @Getter private final boolean semiTransparentMode;
    @Getter private final boolean enableModulation;
    @Getter @Setter private boolean debugDrawCornerMarkers;
    private Consumer<BufferedImage> onTextureSourceUpdate;
    @Getter private BufferedImage cachedImage;

    // Some textures (especially ground textures in Frogger) are extremely low res, such as 4x4.
    // When such textures are used with gouraud shading, the 3D renderer doesn't look as accurate.
    // For example, in ORG1, the map edges (under the flat textured road) will have fade-out shading which is significantly more intense than the real game.
    // And, when that is shown next to the (correctly previewed) road shading, there is a clear mismatch.
    // This is caused because of the extremely low resolution of the texture, so we will scale those textures up slightly.
    @Getter private final int textureScaleX;
    @Getter private final int textureScaleY;

    public static final int UNTEXTURED_FLAT_SIZE = 4;
    public static final int UNTEXTURED_GOURAUD_SIZE = 32; // 8 looked mostly good, but it was visually noticeable, and made certain Frogger levels render with less continuous shading. 16 was the desired amount BEFORE we added UNLIT_SHARP texture options for maps, but once it got sharp we had to double it to 32.
    public static final int UNTEXTURED_PADDING_SIZE = 1;
    public static final int GOURAUD_TEXTURE_MINIMUM_SIZE = UNTEXTURED_GOURAUD_SIZE - (2 * UNTEXTURED_PADDING_SIZE);

    public static final String[] QUAD_VERTEX_NAMES = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
    public static final String[] TRI_VERTEX_NAMES = {"1st Corner", "2nd Corner", "3rd Corner", "Padding"};

    public PSXShadeTextureDefinition(PSXShadedTextureManager<?> shadedTextureManager, PSXPolygonType polygonType, ITextureSource textureSource, CVector[] colors, SCByteTextureUV[] textureUVs, boolean semiTransparentMode, boolean enableModulation) {
        this.shadedTextureManager = shadedTextureManager;
        this.polygonType = polygonType;
        this.textureSource = textureSource;
        this.colors = colors;
        this.textureUVs = textureUVs;
        this.semiTransparentMode = semiTransparentMode;
        this.enableModulation = enableModulation;

        // Calculates the texture scaling needed to make the gouraud shading look ok.
        int textureScaleX = 1;
        int textureScaleY = 1;
        if (polygonType.isGouraud() && polygonType.isTextured() && textureSource != null) {
            // Originally this used getUnpaddedWidth()/getUnpaddedHeight(), but this was a BIG mistake.
            // On levels such as Big Boulder Alley in Frogger, there are some images with a width of 10, in-game width of 2.
            // This means in order to reach an unpadded size of 32, the total image size would reach 160x160. All for a single-color image.
            while (GOURAUD_TEXTURE_MINIMUM_SIZE > (textureScaleX * textureSource.getWidth()))
                textureScaleX <<= 1;
            while (GOURAUD_TEXTURE_MINIMUM_SIZE > (textureScaleY * textureSource.getHeight()))
                textureScaleY <<= 1;
        }

        this.textureScaleX = textureScaleX;
        this.textureScaleY = textureScaleY;
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public PSXShadeTextureDefinition(PSXShadeTextureDefinition other) {
        if (other == null)
            throw new NullPointerException("other");

        this.shadedTextureManager = other.shadedTextureManager;
        this.polygonType = other.polygonType;
        this.textureSource = other.textureSource;
        this.semiTransparentMode = other.semiTransparentMode;
        this.enableModulation = other.enableModulation;
        this.textureScaleX = other.textureScaleX;
        this.textureScaleY = other.textureScaleY;

        // Create array copies.
        this.colors = other.colors != null ? new CVector[other.colors.length] : null;
        if (other.colors != null)
            for (int i = 0; i < other.colors.length; i++)
                this.colors[i] = other.colors[i] != null ? other.colors[i].clone() : null;

        this.textureUVs = other.textureUVs != null ? new SCByteTextureUV[other.textureUVs.length] : null;
        if (other.textureUVs != null)
            for (int i = 0; i < other.textureUVs.length; i++)
                this.textureUVs[i] = other.textureUVs[i] != null ? other.textureUVs[i].clone() : null;
    }

    /**
     * Returns true if scaling is applied to the source image to improve the fidelity of the shading approximation.
     */
    public boolean isSourceImageScaled() {
        return this.textureScaleX != 1 || this.textureScaleY != 1;
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
                hash = (31 * hash) + this.colors[i].toRGB();

        // Add UVs
        if (this.textureUVs != null && doSharedUvsMatter())
            for (int i = 0; i < this.textureUVs.length; i++)
                hash = (31 * hash) + this.textureUVs[i].hashCode();

        if (this.semiTransparentMode)
            hash = (31 * hash) + 1;

        if (this.enableModulation)
            hash = (31 * hash) + 1;

        // Add texture.
        if (this.textureSource != null)
            hash = (31 * hash) + System.identityHashCode(this.textureSource);

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
                && (!doSharedUvsMatter() || Arrays.equals(this.textureUVs, other.textureUVs))
                && this.semiTransparentMode == other.semiTransparentMode
                && this.enableModulation == other.enableModulation;
    }

    /**
     * Returns true if UVs should impact the texture generated.
     */
    public boolean doSharedUvsMatter() {
        if (this.polygonType != PSXPolygonType.POLY_GT3 && this.polygonType != PSXPolygonType.POLY_GT4)
            return false;

        return !doAllColorsMatch();
    }

    /**
     * Returns true if all colors match.
     */
    public boolean doAllColorsMatch() {
        if (this.colors == null || this.colors.length == 0)
            return true;

        for (int i = 1; i < this.colors.length; i++)
            if (!Objects.equals(this.colors[0], this.colors[i]))
                return false;

        return true;
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

        return new PSXShadeTextureDefinition(this.shadedTextureManager, this.polygonType, this.textureSource, copyColors, copyUvs, this.semiTransparentMode, this.enableModulation);
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
        this.cachedImage = null;
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
        PSXShadeTextureImageCache imageCache = this.shadedTextureManager != null ? this.shadedTextureManager.getImageCache() : null;
        return imageCache != null ? imageCache.getSourceImage(this) : PSXShadeTextureImageCache.getTextureSourceImage(this, true);
    }

    /**
     * Gets the target image, if there is one cached.
     * @return targetImage or null
     */
    public BufferedImage getTargetImage() {
        BufferedImage sourceImage = getSourceImage();
        if (this.cachedImage != null && sourceImage != null && this.cachedImage.getWidth() == sourceImage.getWidth() && this.cachedImage.getHeight() == sourceImage.getHeight())
            return clearImage(this.cachedImage);

        // Don't add the old cached image to the cache because its size isn't valid.
        PSXShadeTextureImageCache imageCache = this.shadedTextureManager != null ? this.shadedTextureManager.getImageCache() : null;
        return imageCache != null ? clearImage(imageCache.getTargetImage(this)) : null;
    }

    private static BufferedImage clearImage(BufferedImage image) {
        // Prevents images from looking "deep fried"
        int[] array = ImageUtils.getWritablePixelIntegerArray(image);
        if (array != null)
            Arrays.fill(array, 0);

        return image;
    }

    @Override
    public boolean hasAnyTransparentPixels(BufferedImage image) {
        return this.semiTransparentMode || (this.textureSource != null && this.textureSource.hasAnyTransparentPixels(image));
    }

    @Override
    public BufferedImage makeImage() {
        return this.cachedImage = makeImage(getSourceImage(), getTargetImage());
    }

    /**
     * Creates an image usable for rendering a PSX shaded polygon in JavaFX.
     * @param sourceImage The unshaded texture which is applied to the polygon. Polygons which do not use a texture to render will ignore the value and are expected to pass null.
     * @return shadedImage
     */
    public BufferedImage makeImage(BufferedImage sourceImage, BufferedImage targetImage) {
        if (this.polygonType.isTextured() && sourceImage == null)
            return null; // There's no texture available.

        switch (this.polygonType) {
            case POLY_F3:
            case POLY_F4:
                return PSXTextureShader.makeFlatShadedImage(targetImage, getWidth(), getHeight(), this.colors[0], this.semiTransparentMode);
            case POLY_FT3:
            case POLY_FT4:
                return PSXTextureShader.makeTexturedFlatShadedImage(sourceImage, targetImage, this.colors[0], true);
            case POLY_G3:
            case POLY_G4:
                return PSXTextureShader.makeGouraudShadedImage(targetImage, getWidth(), getHeight(), this.colors, this.semiTransparentMode);
            case POLY_GT3:
            case POLY_GT4:
                if (doAllColorsMatch()) {
                    return PSXTextureShader.makeTexturedFlatShadedImage(sourceImage, targetImage, this.colors[0], this.enableModulation);
                } else {
                    return PSXTextureShader.makeTexturedGouraudShadedImage(sourceImage, targetImage, this.textureSource, this.colors, this.textureUVs, this.textureScaleX, this.textureScaleY, this.debugDrawCornerMarkers, this.enableModulation);
                }
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
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
                return this.textureSource != null ? this.textureSource.getWidth() * this.textureScaleX : 0;
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
                return this.textureSource != null ? this.textureSource.getHeight() * this.textureScaleY : 0;
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
                return this.textureSource != null ? this.textureSource.getUpPadding() * this.textureScaleY : 0;
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
                return this.textureSource != null ? this.textureSource.getDownPadding() * this.textureScaleY : 0;
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
                return this.textureSource != null ? this.textureSource.getLeftPadding() * this.textureScaleX : 0;
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
                return this.textureSource != null ? this.textureSource.getRightPadding() * this.textureScaleX : 0;
            default:
                throw new UnsupportedOperationException("The polygon type " + this.polygonType + " is not supported.");
        }
    }

    /**
     * Write the details of the shade texture definition to a String.
     * @param includeHashes if hash information should be included
     * @param includeColorAndUvInfo if color and uv information should be included
     */
    public String toString(boolean includeHashes, boolean includeColorAndUvInfo) {
        StringBuilder builder = new StringBuilder();
        toString(builder, includeHashes, includeColorAndUvInfo);
        return builder.toString();
    }

    /**
     * Write the details of the shade texture definition to a StringBuilder.
     * @param builder The StringBuilder to write the contents to
     * @param includeHashes if hash information should be included
     * @param includeColorAndUvInfo if color and uv information should be included
     */
    public void toString(StringBuilder builder, boolean includeHashes, boolean includeColorAndUvInfo) {
        builder.append("PSXShadeTextureDef{");

        if (includeHashes) {
            builder.append(Integer.toHexString(System.identityHashCode(this)))
                    .append(",")
                    .append(Integer.toHexString(hashCode()))
                    .append("|");
        }

        builder.append(this.polygonType);

        builder.append(",texture=").append(this.textureSource);

        if (includeColorAndUvInfo) {
            builder.append(",colors=[");
            for (int i = 0; i < this.colors.length; i++) {
                if (i > 0)
                    builder.append(',');

                builder.append(NumberUtils.to0PrefixedHexString(this.colors[i].hashCode()));
            }

            builder.append(']');

            builder.append(",textureUvs=[");
            for (int i = 0; i < this.textureUVs.length; i++) {
                if (i > 0)
                    builder.append(',');

                builder.append('[').append(Integer.toHexString(this.textureUVs[i].getU() & 0xFF))
                        .append(',').append(Integer.toHexString(this.textureUVs[i].getV() & 0xFF))
                        .append(']');
            }

            builder.append(']');
        }

        if (isSourceImageScaled())
            builder.append(",scale=[").append(this.textureScaleX).append(",").append(this.textureScaleY).append("]");
        if (this.semiTransparentMode)
            builder.append(",semiTransparent");
        if (this.semiTransparentMode)
            builder.append(",semiTransparent");
        if (this.semiTransparentMode)
            builder.append(",semiTransparent");
        if (this.imageChangeListeners.size() > 0)
            builder.append(",listeners=").append(this.imageChangeListeners.size());

        builder.append('}');
    }

    @Override
    public String toString() {
        return toString(true, false);
    }

    private void onTextureSourceUpdate(BufferedImage newImage) {
        PSXShadeTextureImageCache imageCache = this.shadedTextureManager != null ? this.shadedTextureManager.getImageCache() : null;
        if (imageCache != null)
            imageCache.onTextureSourceUpdate(this, newImage);
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