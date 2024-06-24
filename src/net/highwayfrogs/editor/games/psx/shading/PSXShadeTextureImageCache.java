package net.highwayfrogs.editor.games.psx.shading;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.games.psx.polygon.PSXPolygonType;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Contains a cache of images which can be shaded. Used to avoid constant memory allocations.
 * Created by Kneesnap on 6/18/2024.
 */
public class PSXShadeTextureImageCache {
    private final PSXShadeTextureSourceCacheEntry flatShadingCacheEntry = new PSXShadeTextureSourceCacheEntry(this, null, null);
    private final PSXShadeTextureSourceCacheEntry gouraudShadingCacheEntry = new PSXShadeTextureSourceCacheEntry(this, null, null);
    private final Map<ITextureSource, PSXShadeTextureSourceCacheEntry> entriesByTextureSource = new ConcurrentHashMap<>();
    private final List<PSXShadedImageCacheEntry> entryExpirationQueue = new ArrayList<>();

    /**
     * Removes expired entries from existence.
     */
    public void cleanupExpiredEntries() {
        synchronized (this.entryExpirationQueue) {
            while (this.entryExpirationQueue.size() > 0 && System.currentTimeMillis() >= this.entryExpirationQueue.get(0).getExpirationTime()) {
                PSXShadedImageCacheEntry cacheEntry = this.entryExpirationQueue.remove(0);
                cacheEntry.getCacheEntry().cachedEntries.remove(cacheEntry);
                cacheEntry.getCacheEntry().removeIfEmpty();
            }
        }
    }

    private void removeFromExpirationQueue(PSXShadedImageCacheEntry cacheEntry) {
        synchronized (this.entryExpirationQueue) {
            int left = 0, right = this.entryExpirationQueue.size() - 1;

            while (left <= right) {
                int midIndex = (left + right) / 2;
                PSXShadedImageCacheEntry midEntry = this.entryExpirationQueue.get(midIndex);

                if (midEntry.getExpirationTime() > cacheEntry.getExpirationTime()) {
                    right = midIndex - 1;
                } else if (midEntry.getExpirationTime() < cacheEntry.getExpirationTime()) {
                    left = midIndex + 1;
                } else { // Match.
                    // Search to the left to find the index.
                    for (int i = midIndex; i > 0; i--) {
                        PSXShadedImageCacheEntry testEntry = this.entryExpirationQueue.get(i);
                        if (testEntry.getExpirationTime() != cacheEntry.getExpirationTime()) {
                            break;
                        } else if (testEntry == cacheEntry) {
                            this.entryExpirationQueue.remove(i);
                            cacheEntry.getCacheEntry().removeIfEmpty();
                            return;
                        }
                    }

                    // Search to the right to find the index.
                    for (int i = midIndex + 1; i < this.entryExpirationQueue.size(); i++) {
                        PSXShadedImageCacheEntry testEntry = this.entryExpirationQueue.get(i);
                        if (testEntry.getExpirationTime() != cacheEntry.getExpirationTime()) {
                            break;
                        } else if (testEntry == cacheEntry) {
                            this.entryExpirationQueue.remove(i);
                            cacheEntry.getCacheEntry().removeIfEmpty();
                            return;
                        }
                    }

                    break;
                }
            }
        }
    }

    /**
     * Gets the source image which shaded images are created from
     * @param shadeTextureDefinition the shade texture definition to get
     * @return sourceImage or null
     */
    public BufferedImage getSourceImage(PSXShadeTextureDefinition shadeTextureDefinition) {
        // Untextured shade definitions have no source image.
        return shadeTextureDefinition.getPolygonType().isTextured() ? getOrCreateCacheEntry(shadeTextureDefinition).getMainImage() : null;
    }

    /**
     * Adds the target image to the cache
     * @param shadeTextureDefinition the shade texture definition to get
     * @param targetImage the target image to add
     */
    public void addTargetImage(PSXShadeTextureDefinition shadeTextureDefinition, BufferedImage targetImage) {
        getOrCreateCacheEntry(shadeTextureDefinition).addTargetImage(targetImage);
    }

    /**
     * Gets a free target image which shaded images are created from
     * @param shadeTextureDefinition the shade texture definition to get
     * @return targetImage, or null if there aren't any left
     */
    public BufferedImage getTargetImage(PSXShadeTextureDefinition shadeTextureDefinition) {
        PSXShadeTextureSourceCacheEntry entry = getCacheEntry(shadeTextureDefinition);
        return entry != null ? entry.getTargetImage() : null;
    }

    /**
     * Handles when the underlying texture changes.
     * @param shadeTextureDefinition the shade texture definition to update from
     * @param newImage the image to apply
     */
    public void onTextureSourceUpdate(PSXShadeTextureDefinition shadeTextureDefinition, BufferedImage newImage) {
        if (newImage != null) {
            PSXShadeTextureSourceCacheEntry cacheEntry = this.entriesByTextureSource.get(shadeTextureDefinition.getTextureSource());
            if (cacheEntry != null)
                cacheEntry.onTextureSourceUpdate(newImage);
        }
    }

    private PSXShadeTextureSourceCacheEntry getCacheEntry(PSXShadeTextureDefinition shadeTextureDefinition) {
        PSXPolygonType polygonType = shadeTextureDefinition.getPolygonType();
        if (polygonType.isTextured()) {
            return this.entriesByTextureSource.get(shadeTextureDefinition.getTextureSource());
        } else if (polygonType.isGouraud()) {
            return this.gouraudShadingCacheEntry;
        } else if (polygonType.isFlat()) {
            return this.flatShadingCacheEntry;
        } else {
            throw new RuntimeException("Didn't know how to handle polygon type '" + polygonType + "'.");
        }
    }

    private PSXShadeTextureSourceCacheEntry getOrCreateCacheEntry(PSXShadeTextureDefinition shadeTextureDefinition) {
        PSXShadeTextureSourceCacheEntry cacheEntry = getCacheEntry(shadeTextureDefinition);
        if (cacheEntry != null)
            return cacheEntry;

        BufferedImage mainImage = getTextureSourceImage(shadeTextureDefinition);
        ITextureSource textureSource = shadeTextureDefinition.getTextureSource();
        return this.entriesByTextureSource.computeIfAbsent(textureSource, key -> new PSXShadeTextureSourceCacheEntry(this, textureSource, mainImage));
    }

    /**
     * Gets the source image from the PSXShadeTextureDefinition.
     * @param shadeTextureDefinition the shade texture definition to get the source image from
     * @return sourceImage
     */
    public static BufferedImage getTextureSourceImage(PSXShadeTextureDefinition shadeTextureDefinition) {
        ITextureSource textureSource = shadeTextureDefinition.getTextureSource();
        BufferedImage mainImage = textureSource != null ? textureSource.makeImage() : null;

        // Make it larger to ensure the shading is large enough to appear correctly.
        if (mainImage != null) {
            int textureScaleX = shadeTextureDefinition.getTextureScaleX();
            int textureScaleY = shadeTextureDefinition.getTextureScaleY();
            if (textureScaleX != 1 || textureScaleY != 1)
                mainImage = ImageWorkHorse.resizeImage(mainImage, mainImage.getWidth() * textureScaleX, mainImage.getHeight() * textureScaleY, true);
        }

        return mainImage;
    }

    @Getter
    private static class PSXShadeTextureSourceCacheEntry {
        private final PSXShadeTextureImageCache cache;
        private final ITextureSource textureSource;
        private final List<PSXShadedImageCacheEntry> cachedEntries = new ArrayList<>();
        private WeakReference<BufferedImage> mainImageLastUpdatedFrom;
        private BufferedImage mainImage;

        public PSXShadeTextureSourceCacheEntry(PSXShadeTextureImageCache cache, ITextureSource textureSource, BufferedImage mainImage) {
            this.cache = cache;
            this.textureSource = textureSource;
            this.mainImage = mainImage;
        }

        public BufferedImage getTargetImage() {
            PSXShadedImageCacheEntry imageEntry;
            synchronized (this.cachedEntries) {
                imageEntry = this.cachedEntries.size() > 0 ? this.cachedEntries.remove(this.cachedEntries.size() - 1) : null;
                removeIfEmpty();
            }

            if (imageEntry != null)
                this.cache.removeFromExpirationQueue(imageEntry);

            return imageEntry != null ? imageEntry.getImage() : null;
        }

        public void onTextureSourceUpdate(BufferedImage newImage) {
            if (this.mainImageLastUpdatedFrom != null && this.mainImageLastUpdatedFrom.get() == newImage)
                return;

            // Clear all cached images if the size changed.
            if (newImage.getWidth() != this.mainImage.getWidth() || newImage.getHeight() != this.mainImage.getHeight()) {
                synchronized (this.cachedEntries) {
                    this.cachedEntries.forEach(this.cache::removeFromExpirationQueue);
                    this.cachedEntries.clear();
                    removeIfEmpty();
                }

                return;
            }

            // Update image.
            this.mainImageLastUpdatedFrom = new WeakReference<>(newImage);
            this.mainImage = ImageWorkHorse.copyImage(newImage, this.mainImage);
        }

        public void removeIfEmpty() {
            if (this.cachedEntries.isEmpty() && this.textureSource != null)
                this.cache.entriesByTextureSource.remove(this.textureSource, this);
        }

        public void addTargetImage(BufferedImage image) {
            PSXShadedImageCacheEntry newImageEntry = new PSXShadedImageCacheEntry(this, image);
            synchronized (this.cachedEntries) {
                this.cachedEntries.add(newImageEntry);
            }

            synchronized (this.cache.entryExpirationQueue) {
                this.cache.entryExpirationQueue.add(newImageEntry);
            }
        }
    }

    @Getter
    @AllArgsConstructor
    private static class PSXShadedImageCacheEntry {
        private final PSXShadeTextureSourceCacheEntry cacheEntry;
        private final BufferedImage image;
        private final long expirationTime;

        private static final long EXPIRATION_TIME = TimeUnit.SECONDS.toMillis(15);

        public PSXShadedImageCacheEntry(PSXShadeTextureSourceCacheEntry entry, BufferedImage image) {
            this.cacheEntry = entry;
            this.image = image;
            this.expirationTime = System.currentTimeMillis() + EXPIRATION_TIME;
        }
    }
}