package net.highwayfrogs.editor.file.vlo;

import lombok.Getter;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse.TransparencyFilter;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about what operations should and should not be applied.
 * Created by Kneesnap on 12/1/2018.
 */
@Getter
public class ImageFilterSettings {
    private final ImageState state;
    private boolean trimEdges;
    private boolean allowTransparency;
    private boolean allowFlip;
    private boolean allowScrunch;
    private boolean scaleToMaxSize;
    private final Map<BufferedImage, BufferedImage> renderCache = new HashMap<>();

    public ImageFilterSettings(ImageState state) {
        this.state = state;
    }

    /**
     * Check if this is an imported image.
     * @return isImport
     */
    public boolean isImport() {
        return getState() == ImageState.IMPORT;
    }

    /**
     * Check if this is an exported image.
     * @return isExport
     */
    public boolean isExport() {
        return getState() == ImageState.EXPORT;
    }

    /**
     * Set if we will trim edges or not.
     * @param newState Should we trim edges?
     * @return this
     */
    public ImageFilterSettings setTrimEdges(boolean newState) {
        if (newState != isTrimEdges())
            invalidateRenderCache();
        this.trimEdges = newState;
        return this;
    }

    /**
     * Set if we will show transparency or not.
     * @param newState Should we show transparency?
     * @return this
     */
    public ImageFilterSettings setAllowTransparency(boolean newState) {
        if (newState != isAllowTransparency())
            invalidateRenderCache();
        this.allowTransparency = newState;
        return this;
    }

    /**
     * Set if we will use nearest neighor scaling to make it into a max size image.
     * If we want to add other kinds of scaling, maybe consider replacing this with a system.
     * @param newState Whether or not it should be scaled.
     * @return this
     */
    public ImageFilterSettings setScaleToMaxSize(boolean newState) {
        if (newState != isScaleToMaxSize())
            invalidateRenderCache();
        this.scaleToMaxSize = newState;
        return this;
    }

    /**
     * Set if we will allow flipping the image or not.
     * @param newState Should we allow image flipping?
     * @return this
     */
    public ImageFilterSettings setAllowFlip(boolean newState) {
        if (newState != isAllowFlip())
            invalidateRenderCache();
        this.allowFlip = newState;
        return this;
    }

    /**
     * Set if we will allow scrunching the image or not.
     * @param newState Should we allow scrunching?
     * @return this
     */
    public ImageFilterSettings setAllowScrunch(boolean newState) {
        if (newState != isAllowScrunch())
            invalidateRenderCache();
        this.allowScrunch = newState;
        return this;
    }

    /**
     * Invalidate the render cache, such as when settings change.
     */
    public void invalidateRenderCache() {
        this.renderCache.clear();
    }

    /**
     * Apply filters to a given GameImage.
     * @param gameImage  The image to apply filters to.
     * @param firstImage The raw image to apply filters to.
     * @return filteredImage
     */
    public BufferedImage applyFilters(GameImage gameImage, BufferedImage firstImage) {
        BufferedImage result = this.renderCache.get(firstImage);
        if (result != null)
            return result;

        BufferedImage image = firstImage;
        if (isTrimEdges() && isExport())
            image = ImageWorkHorse.trimEdges(gameImage, image);

        if (isAllowFlip() && !gameImage.testFlag(GameImage.FLAG_HIT_X))
            image = ImageWorkHorse.flipVertically(image);

        if (isAllowScrunch() && gameImage.getParent().isPsxMode())
            image = ImageWorkHorse.scaleWidth(image, isImport() ? (double) gameImage.getWidthMultiplier() : (1D / (double) gameImage.getWidthMultiplier()));

        if (isScaleToMaxSize() && isExport())
            image = ImageWorkHorse.scaleForDisplay(image, GameImage.PSX_FULL_PAGE_WIDTH, true);

        boolean transparencyGoal = isAllowTransparency() && gameImage.testFlag(GameImage.FLAG_BLACK_IS_TRANSPARENT);
        if (transparencyGoal)
            image = ImageWorkHorse.applyFilter(image, new TransparencyFilter());

        this.renderCache.put(firstImage, image);
        return image;
    }

    public enum ImageState {
        IMPORT, EXPORT
    }
}
