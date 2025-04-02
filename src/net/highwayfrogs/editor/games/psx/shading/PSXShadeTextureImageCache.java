package net.highwayfrogs.editor.games.psx.shading;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.ToIntFunction;

/**
 * The purpose of this image cache is to avoid extra work when working with PSX Shade texture definitions.
 * These operations occur when building/rebuilding texture atlases due to the sheer amount of textures generated for accurate shading.
 * Operations:
 *  - Unnecessary image allocations (& the accompanying garbage collection).
 *  - Generating images from underlying the same underlying ITextureSources over and over.
 * Created by Kneesnap on 6/18/2024.
 */
public class PSXShadeTextureImageCache {
    private final PSXShadeTextureSourceCacheEntry flatShadingCacheEntry = new PSXShadeTextureSourceCacheEntry(this, null, null);
    private final PSXShadeTextureSourceCacheEntry gouraudShadingCacheEntry = new PSXShadeTextureSourceCacheEntry(this, null, null);
    private final Map<ITextureSource, PSXShadeTextureSourceCacheEntry> entriesByTextureSource = new ConcurrentHashMap<>(); // Consider switching this to an IdentityHashMap with a read-write lock, OR Collections.synchronizedMap(IdentityHashMap), because there's a concern about ITextureSources implementing their own equals/hashCode and breaking compatibility.
    private final List<PSXShadedImageCacheEntry> entryExpirationQueue = new ArrayList<>();
    private final BinarySearchIntListHolder<BinarySearchIntListHolder<BinarySearchIntList<PSXShadedImageCacheEntry>>> cachedTargetImages = new BinarySearchIntListHolder<>(null, -1);

    /**
     * Removes expired entries from existence.
     */
    public void cleanupExpiredEntries() {
        synchronized (this.entryExpirationQueue) {
            while (this.entryExpirationQueue.size() > 0 && System.currentTimeMillis() >= this.entryExpirationQueue.get(0).getExpirationTime()) {
                PSXShadedImageCacheEntry cacheEntry = this.entryExpirationQueue.remove(0);
                if (cacheEntry.getParentList() != null)
                    cacheEntry.getParentList().remove(cacheEntry);
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
        return shadeTextureDefinition.getPolygonType().isTextured() ? getOrCreateCacheEntry(shadeTextureDefinition).getSourceImage(shadeTextureDefinition) : null;
    }

    /**
     * Adds the target image to the cache
     * @param shadeTextureDefinition the shade texture definition to get
     * @param targetImage the target image to add
     */
    public void addTargetImage(PSXShadeTextureDefinition shadeTextureDefinition, BufferedImage targetImage) {
        // Do not clone the shade definition, as the cache is actually stored by the shade texture's ITextureSource (which should be reference-equality based), instead of the shade def itself (which can change).
        getOrCreateCacheEntry(shadeTextureDefinition).addTargetImage(targetImage);
    }

    /**
     * Gets a free target image which shaded images are created from
     * @param shadeTextureDefinition the shade texture definition to get
     * @return targetImage, or null if there aren't any left
     */
    public BufferedImage getTargetImage(PSXShadeTextureDefinition shadeTextureDefinition) {
        PSXShadeTextureSourceCacheEntry entry = getCacheEntry(shadeTextureDefinition);
        return entry != null ? entry.getTargetImage(shadeTextureDefinition) : null;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private BinarySearchIntList<PSXShadedImageCacheEntry> getImageCacheForDimensions(int width, int height, boolean createIfMissing) {
        BinarySearchIntListHolder<BinarySearchIntList<PSXShadedImageCacheEntry>> heightList = this.cachedTargetImages.getListByKey(height);
        if (heightList == null) {
            if (!createIfMissing)
                return null;

            heightList = new BinarySearchIntListHolder(this.cachedTargetImages, height);
            heightList = this.cachedTargetImages.addList(heightList);
        }

        BinarySearchIntList<PSXShadedImageCacheEntry> widthList = heightList.getListByKey(width);
        if (widthList == null) {
            if (!createIfMissing)
                return null;

            widthList = new BinarySearchIntListHolder(heightList, width);
            widthList = heightList.addList(widthList);
        }

        return widthList;
    }

    private PSXShadeTextureSourceCacheEntry getOrCreateCacheEntry(PSXShadeTextureDefinition shadeTextureDefinition) {
        PSXShadeTextureSourceCacheEntry cacheEntry = getCacheEntry(shadeTextureDefinition);
        if (cacheEntry != null)
            return cacheEntry;

        ITextureSource textureSource = shadeTextureDefinition.getTextureSource();
        if (textureSource == null)
            throw new RuntimeException("Cannot cached data for a null ITextureSource!");

        BufferedImage mainImage = textureSource.makeImage();
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


    private static class PSXShadeTextureSourceCacheEntry {
        private final ReadWriteLock imageListLock = new ReentrantReadWriteLock();
        @Getter private final PSXShadeTextureImageCache cache;
        @Getter private final ITextureSource textureSource;
        private BufferedImage mainImage;
        private BufferedImage scaledMainImage;
        private volatile WeakReference<BufferedImage> mainImageLastUpdatedFrom;
        private WeakReference<BinarySearchIntList<PSXShadedImageCacheEntry>> imageListRef;

        public PSXShadeTextureSourceCacheEntry(PSXShadeTextureImageCache cache, ITextureSource textureSource, BufferedImage mainImage) {
            this.cache = cache;
            this.textureSource = textureSource;
            this.mainImage = mainImage;
        }

        /**
         * Gets a free target image to write shaded texture data to.
         */
        public BufferedImage getTargetImage(PSXShadeTextureDefinition shadeDef) {
            BinarySearchIntList<PSXShadedImageCacheEntry> cachedEntries = getImageList(shadeDef.getWidth(), shadeDef.getHeight(), false);
            if (cachedEntries == null)
                return null;

            PSXShadedImageCacheEntry imageEntry = cachedEntries.removeLast();
            if (imageEntry != null) {
                this.cache.removeFromExpirationQueue(imageEntry);
                return imageEntry.getImage();
            }

            return null;
        }

        /**
         * Gets the source image for the given shade texture definition
         * @param shadeDef the shade texture definition to get the source image for
         * @return sourceImage
         */
        public BufferedImage getSourceImage(PSXShadeTextureDefinition shadeDef) {
            int textureScaleX = shadeDef.getTextureScaleX();
            int textureScaleY = shadeDef.getTextureScaleY();
            if (textureScaleX != 1 || textureScaleY != 1) {
                if (this.scaledMainImage != null) {
                    return this.scaledMainImage;
                }

                try {
                    this.imageListLock.writeLock().lock();
                    return this.scaledMainImage = ImageWorkHorse.resizeImage(this.mainImage, this.mainImage.getWidth() * textureScaleX, this.mainImage.getHeight() * textureScaleY, true);
                } finally {
                    this.imageListLock.writeLock().unlock();
                }
            }

            try {
                this.imageListLock.readLock().lock();
                return this.mainImage;
            } finally {
                this.imageListLock.readLock().unlock();
            }
        }

        private void removeImageList() {
            try {
                this.imageListLock.writeLock().lock();
                this.imageListRef = null;
                this.scaledMainImage = null;
            } finally {
                this.imageListLock.writeLock().unlock();
            }
        }

        /**
         * Called when the texture source underpinning the PSXShadeTextureDefinition updates.
         * @param newImage the new image to treat as the "source image".
         */
        public void onTextureSourceUpdate(BufferedImage newImage) {
            if (this.mainImageLastUpdatedFrom != null && this.mainImageLastUpdatedFrom.get() == newImage)
                return;

            // Clear all cached images if the size changed.
            if (newImage.getWidth() != this.mainImage.getWidth() || newImage.getHeight() != this.mainImage.getHeight()) {
                removeImageList();
                return;
            }

            // Update image.
            this.mainImageLastUpdatedFrom = new WeakReference<>(newImage);
            this.mainImage = ImageWorkHorse.copyImage(newImage, this.mainImage);
        }

        /**
         * Add a target image to be available for writing.
         * @param image the image to add
         */
        public void addTargetImage(BufferedImage image) {
            BinarySearchIntList<PSXShadedImageCacheEntry> list = getImageList(image.getWidth(), image.getHeight(), true);
            PSXShadedImageCacheEntry newImageEntry = new PSXShadedImageCacheEntry(list, image);
            list.add(newImageEntry);

            synchronized (this.cache.entryExpirationQueue) {
                this.cache.entryExpirationQueue.add(newImageEntry);
            }
        }

        private BinarySearchIntList<PSXShadedImageCacheEntry> getImageList(int width, int height, boolean createIfMissing) {
            boolean shouldApplyImageList = false;
            try {
                this.imageListLock.readLock().lock();

                BinarySearchIntList<PSXShadedImageCacheEntry> imageList;
                if (this.imageListRef == null || (imageList = this.imageListRef.get()) == null) {
                    shouldApplyImageList = true;
                } else if (doesWidthHeightMatch(imageList, width, height)) {
                    return imageList;
                }
            } finally {
                this.imageListLock.readLock().unlock();
            }

            BinarySearchIntList<PSXShadedImageCacheEntry> newList = this.cache.getImageCacheForDimensions(width, height, createIfMissing);

            if (shouldApplyImageList && newList != null) {
                try {
                    this.imageListLock.writeLock().lock();

                    BinarySearchIntList<PSXShadedImageCacheEntry> imageList;
                    if (this.imageListRef == null || (imageList = this.imageListRef.get()) == null || !doesWidthHeightMatch(imageList, width, height)) {
                        this.imageListRef = new WeakReference<>(newList);
                    } else {
                        return imageList;
                    }
                } finally {
                    this.imageListLock.writeLock().unlock();
                }
            }

            return newList;
        }

        private static boolean doesWidthHeightMatch(BinarySearchIntList<PSXShadedImageCacheEntry> list, int width, int height) {
            BinarySearchIntListHolder<BinarySearchIntList<PSXShadedImageCacheEntry>> parent = list.getParent();
            return width == parent.getKey() && height == parent.getParent().getKey();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class PSXShadedImageCacheEntry {
        private final BinarySearchIntList<PSXShadedImageCacheEntry> parentList;
        private final BufferedImage image;
        private final long expirationTime;

        private static final long EXPIRATION_TIME = TimeUnit.SECONDS.toMillis(15);

        public PSXShadedImageCacheEntry(BinarySearchIntList<PSXShadedImageCacheEntry> parentList, BufferedImage image) {
            this.parentList = parentList;
            this.image = image;
            this.expirationTime = System.currentTimeMillis() + EXPIRATION_TIME;
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class BinarySearchIntList<T> {
        protected final BinarySearchIntListHolder<BinarySearchIntList<T>> parent;
        protected final int key;
        protected final List<T> values = new ArrayList<>();
        protected final ReadWriteLock lock = new ReentrantReadWriteLock();

        public static final ToIntFunction<BinarySearchIntList<?>> BINARY_SEARCH_GETTER = BinarySearchIntList::getKey;

        /**
         * Adds a value to the end of the list. No binary searching.
         * @param value the value to add
         */
        public void add(T value) {
            try {
                this.lock.writeLock().lock();
                this.values.add(value);
            } finally {
                this.lock.writeLock().unlock();
            }
        }

        /**
         * Removes a value from the list. No binary searching.
         * @return the value removed
         */
        public T removeLast() {
            try {
                this.lock.writeLock().lock();
                T value = this.values.size() > 0 ? this.values.remove(this.values.size() - 1) : null;
                if (this.values.isEmpty() && this.parent != null)
                    this.parent.remove(this.key);

                return value;
            } finally {
                this.lock.writeLock().unlock();
            }
        }

        /**
         * Removes a value from the list. No binary searching.
         * @param value the value to remove
         */
        public boolean remove(T value) {
            try {
                this.lock.writeLock().lock();
                boolean result = this.values.remove(value);
                if (result && this.values.isEmpty() && this.parent != null)
                    this.parent.remove(this.key);

                return result;
            } finally {
                this.lock.writeLock().unlock();
            }
        }
    }

    @Getter
    private static class BinarySearchIntListHolder<TList extends BinarySearchIntList<?>> extends BinarySearchIntList<TList> {
        public BinarySearchIntListHolder(BinarySearchIntListHolder<BinarySearchIntList<TList>> parent, int key) {
            super(parent, key);
        }

        /**
         * Adds a list.
         * @param list the list to add
         * @return listForKey
         */
        public TList addList(TList list) {
            if (list == null)
                throw new NullPointerException("list");

            try {
                this.lock.writeLock().lock();
                int keyIndex = indexOf(list.getKey());

                // Validate the list hasn't been found/added yet.
                TList result;
                if (keyIndex >= 0) {
                    if ((result = this.values.get(keyIndex)) != null) {
                        return result;
                    } else {
                        this.values.set(keyIndex, list);
                    }
                } else {
                    this.values.add(-(keyIndex + 1), list);
                }

                return list;
            } finally {
                this.lock.writeLock().unlock();
            }
        }

        /**
         * Gets a list by its key, if it is present.
         * @param key the key to get the list by
         * @return list, if found
         */
        public TList getListByKey(int key) {
            try {
                this.lock.readLock().lock();
                int keyIndex = indexOf(key);
                return keyIndex >= 0 ? this.values.get(keyIndex) : null;
            } finally {
                this.lock.readLock().unlock();
            }
        }

        /**
         * Removes a value from the list. No binary searching.
         * @param key the key for the value to remove
         */
        public boolean remove(int key) {
            try {
                this.lock.writeLock().lock();
                int keyIndex = indexOf(key);
                if (keyIndex >= 0) {
                    this.values.remove(keyIndex);
                    if (this.values.isEmpty() && this.parent != null)
                        this.parent.remove(this.key);

                    return true;
                }

                return false;
            } finally {
                this.lock.writeLock().unlock();
            }
        }

        /**
         * Get the index containing the desired node of a binary searchable int list, or the insertion point if there is none.
         * @param key the key to lookup
         * @return index
         */
        public int indexOf(int key) {
            try {
                this.lock.readLock().lock();

                int left = 0;
                int right = this.values.size() - 1;

                while (left <= right) {
                    int midIndex = (left + right) / 2;
                    TList midEntry = this.values.get(midIndex);
                    int midKey = midEntry.getKey();

                    if (key == midKey) {
                        return midIndex;
                    } else if (key > midKey) {
                        left = midIndex + 1;
                    } else {
                        right = midIndex - 1;
                    }
                }

                return -(left + 1);
            } finally {
                this.lock.readLock().unlock();
            }
        }
    }
}