package net.highwayfrogs.editor.gui.texture.atlas;

import javafx.concurrent.Task;
import javafx.scene.image.WritableImage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.objects.SortedList;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Builds the image for a texture atlas.
 * Textures are built in parallel to achieve maximum speed.
 * Back-ported from ModToolFramework.
 * Caching is utilized to avoid intensive operations for each update.
 * This class has been profiled heavily.
 * TODO: In JavaFX 13+, try converting to PixelBuffer for even better performance (Potentially multithreaded image writing can come back?). Example: <a href="https://foojay.io/today/high-performance-rendering-in-javafx/"/>
 * Created by Kneesnap on 9/23/2023.
 */
@Getter
public class AtlasBuilderTextureSource implements ITextureSource {
    private final TextureAtlas atlas;
    private final boolean enableAwtImage;
    private final boolean enableFxImage;
    private final List<Consumer<BufferedImage>> imageChangeListeners;
    private final Set<DynamicMeshNode> updatedNodes = new HashSet<>();
    private final AsyncTaskWriteTextureState writeTaskState;
    private final List<AsyncTaskWriteTexture> asyncWriteTasks;

    private BufferedImage cachedImage; // Caching the image allows for faster generation.
    private WritableImage cachedFxImage; // Caching the image allows for faster generation.
    @Setter private DynamicMesh mesh;
    private boolean currentlyBuildingTexture;
    private volatile boolean writerThreadShouldPrintTrace;

    private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    private static final ExecutorService ATLAS_BUILDER_THREAD_POOL = Executors.newWorkStealingPool(THREAD_COUNT);
    private static final boolean SINGLE_THREADED_DEBUGGING_ENABLED = false;

    public AtlasBuilderTextureSource(TextureAtlas atlas, boolean enableAwtImage, boolean enableFxImage) {
        if (atlas == null)
            throw new NullPointerException("atlas");

        this.atlas = atlas;
        this.imageChangeListeners = new ArrayList<>();
        this.enableAwtImage = enableAwtImage;
        this.enableFxImage = enableFxImage;
        this.writeTaskState = new AsyncTaskWriteTextureState(this);
        this.asyncWriteTasks = new ArrayList<>(THREAD_COUNT);
    }

    /**
     * Release resources currently held.
     */
    public void releaseResources() {
        if (this.currentlyBuildingTexture) {
            this.writerThreadShouldPrintTrace = true;
            throw new RuntimeException("Cannot release resources while the texture is currently building.");
        }

        this.imageChangeListeners.clear();
        this.updatedNodes.clear();
        this.asyncWriteTasks.forEach(Task::cancel);
        this.asyncWriteTasks.clear();
        this.writeTaskState.clear();
        this.cachedImage = null;
        this.cachedFxImage = null;
    }

    /**
     * Makes a new image containing the texture sheet entries.
     * By default, caching of the image is enabled, to avoid major updates.
     * @return newImage
     */
    public BufferedImage makeNewImage() {
        try {
            this.cachedImage = new BufferedImage(this.atlas.getAtlasWidth(), this.atlas.getAtlasHeight(), BufferedImage.TYPE_INT_ARGB);
        } catch (OutOfMemoryError ex) {
            throw new RuntimeException("Can't create atlas image " + this.atlas.getAtlasWidth() + "x" + this.atlas.getAtlasHeight() + ", as there is not enough heap space.", ex);
        }

        this.cachedFxImage = new WritableImage(this.atlas.getAtlasWidth(), this.atlas.getAtlasHeight());

        writeImageAsyncAndWait(false);
        updateTextureCoordinates(); // Apply the new texture coordinates to the mesh.

        return this.cachedImage;
    }

    @Override
    public boolean hasAnyTransparentPixels(BufferedImage image) {
        return true; // Empty space is transparent.
    }

    @Override
    public BufferedImage makeImage() {
        if (this.cachedImage == null || (this.atlas.getAtlasWidth() != this.cachedImage.getWidth()) || (this.atlas.getAtlasHeight() != this.cachedImage.getHeight()))
            return this.cachedImage = makeNewImage();

        writeImageAsyncAndWait(true);
        updateTextureCoordinates();
        return this.cachedImage;
    }

    private void writeImageAsyncAndWait(boolean writeOnlyChangedImages) {
        if (this.currentlyBuildingTexture) {
            this.writerThreadShouldPrintTrace = true;
            throw new IllegalStateException("Cannot build texture because it seems to already be building?!");
        }

        this.currentlyBuildingTexture = true;

        Graphics2D graphics = null;
        if (this.enableAwtImage) {
            graphics = this.cachedImage.createGraphics();
            graphics.setBackground(new Color(255, 255, 255, 0));
            if (!writeOnlyChangedImages) // Clear image.
                graphics.clearRect(0, 0, this.cachedImage.getWidth(), this.cachedImage.getHeight());
            graphics.setComposite(AlphaComposite.Src); // If we write a transparent image, it will still delete whatever image data is there already.
        }

        this.writeTaskState.setupNextWrite(writeOnlyChangedImages, graphics);
        this.atlas.prepareImageGeneration();

        // Create and submit tasks.
        this.atlas.pushDisableUpdates();

        try {
            if (SINGLE_THREADED_DEBUGGING_ENABLED) {
                AsyncTaskWriteTexture singleTask = new AsyncTaskWriteTexture(this.writeTaskState);
                this.writeTaskState.latch = new CountDownLatch(1);
                singleTask.call();
            } else {
                while (THREAD_COUNT > this.asyncWriteTasks.size())
                    this.asyncWriteTasks.add(new AsyncTaskWriteTexture(this.writeTaskState));

                // Creating the latch of the right count must happen BEFORE the tasks run (as ones that exit immediately will pull the latch immediately.)
                this.writeTaskState.latch = new CountDownLatch(this.asyncWriteTasks.size());

                // Submit work.
                for (int i = 0; i < this.asyncWriteTasks.size(); i++)
                    ATLAS_BUILDER_THREAD_POOL.submit((Callable<?>) this.asyncWriteTasks.get(i));
            }

            // Write textures to the atlas on the main thread.
            this.writeTaskState.writeTextures();
        } finally {
            this.currentlyBuildingTexture = false;
            this.atlas.popDisableUpdates();

            if (this.writerThreadShouldPrintTrace) {
                Utils.printStackTrace();
                this.writerThreadShouldPrintTrace = false;
            }
        }

        // NOTE:
        // We tried to build a large BufferedImage then write it to the WritableImage, but that was significantly slower than just writing directly to the FX image.
        // Even when the BufferedImage was written async it wasn't enough.
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
                if (this.mesh instanceof IPSXShadedMesh) {
                    IPSXShadedMesh shadedMesh = (IPSXShadedMesh) this.mesh;
                    if (shadedMesh.getShadedTextureManager() instanceof PSXMeshShadedTextureManager<?>)
                        ((PSXMeshShadedTextureManager<?>) shadedMesh.getShadedTextureManager()).updateTextureCoordinates(shadeTexture, this.updatedNodes);
                }
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
        if (!this.currentlyBuildingTexture)
            this.fireChangeEvent0(newImage);
    }

    @Getter
    @RequiredArgsConstructor
    private static class AsyncTaskWriteTexture extends Task<Void> implements Callable<Void> {
        private final AsyncTaskWriteTextureState taskState;

        @Override
        public Void call() {
            try {
                AtlasTexture texture;
                while ((texture = this.taskState.getNextTexture()) != null)
                    this.taskState.queueImage(texture);
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "There was an Exception in the async texture update task.");
            } finally {
                this.taskState.latch.countDown();
            }

            return null;
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class AsyncTaskWriteTextureState {
        private final AtlasBuilderTextureSource atlasBuilder;
        private final List<AtlasTexture> texturesReadyToWrite = new ArrayList<>();
        private CountDownLatch latch;
        private boolean onlyWriteUpdatedTextures;
        private Graphics awtGraphics;
        private int index;

        /**
         * Clears resources from the previous write task.
         */
        public void clear() {
            this.texturesReadyToWrite.clear();
            this.latch = null;
            if (this.awtGraphics != null)
                this.awtGraphics.dispose();
            this.awtGraphics = null;
            this.index = 0;
        }

        /**
         * Setup the task state for the next execution.
         * @param onlyWriteUpdatedTextures whether to only write updated textures
         * @param awtGraphics the awt graphics object, if there is one
         */
        public void setupNextWrite(boolean onlyWriteUpdatedTextures, Graphics awtGraphics) {
            clear();
            this.onlyWriteUpdatedTextures = onlyWriteUpdatedTextures;
            this.awtGraphics = awtGraphics;
        }

        /**
         * Wait for the textures
         */
        public void writeTextures() {
            while (this.latch.getCount() > 0 || this.texturesReadyToWrite.size() > 0) {
                if (!this.tryWriteNextImage()) {
                    AtlasTexture nextTexture = getNextTexture();
                    if (nextTexture != null)
                        this.queueImage(nextTexture);
                }
            }
        }

        /**
         * Create the texture image to write to the atlas, and queues it to be written.
         * @param texture the texture to create the image for
         */
        public void queueImage(AtlasTexture texture) {
            BufferedImage awtImage = texture.getImage(); // Ensures the image is ready.
            if (this.atlasBuilder.isEnableAwtImage()) { // BufferedImage can be written async.
                this.awtGraphics.drawImage(awtImage, texture.getX() + texture.getLeftPaddingEmpty(), texture.getY() + texture.getUpPaddingEmpty(), texture.getNonEmptyPaddedWidth(), texture.getNonEmptyPaddedHeight(), null);
                texture.onTextureWrittenToAtlas();
            }

            // NOTE: It is (TECHNICALLY) possible to update the FX image async, and it does yield a marginal performance boost.
            // HOWEVER, once we do that, it randomly causes the 3D scene to not respond to mouse input.
            // Also, it causes the texture sheet ImageView to throw heaps of errors when viewed.
            // So, we skip writing it here.

            synchronized (this.texturesReadyToWrite) {
                this.texturesReadyToWrite.add(texture);
            }
        }

        /**
         * Attempts to write the next shaded image to the texture atlas image(s).
         * @return true iff there was an image written
         */
        public boolean tryWriteNextImage() {
            AtlasTexture texture;
            synchronized (this.texturesReadyToWrite) {
                if (this.texturesReadyToWrite.isEmpty())
                    return false;

                texture = this.texturesReadyToWrite.remove(0);
            }

            // The AWT image is written in part of queueImage(), because it is safe to write async.
            // The FX image is NOT safe to write async, so it is written here (on the main thread).
            if (this.atlasBuilder.isEnableFxImage()) {
                BufferedImage awtImage = texture.getImage(); // Gets the cached image.
                ImageWorkHorse.writeBufferedImageToFxImage(awtImage, this.atlasBuilder.cachedFxImage, texture.getX() + texture.getLeftPaddingEmpty(), texture.getY() + texture.getUpPaddingEmpty());
                texture.onTextureWrittenToAtlas();
            }

            return true;
        }

        /**
         * Gets the next texture to write, if there is one.
         * @return nextTexture
         */
        public AtlasTexture getNextTexture() {
            final SortedList<? extends AtlasTexture> sortedTextures = this.atlasBuilder.getAtlas().getSortedTextureList();
            synchronized (sortedTextures) {
                while (this.index < sortedTextures.size()) {
                    AtlasTexture texture = sortedTextures.get(this.index++);
                    if (!this.onlyWriteUpdatedTextures || texture.isAtlasCachedImageInvalid())
                        return texture;
                }
            }

            return null;
        }
    }
}