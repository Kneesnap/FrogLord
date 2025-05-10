package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.gui.SelectionMenu;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a texture remap array.
 * Created by Kneesnap on 10/25/2023.
 */
@Getter
public class TextureRemapArray extends SCSharedGameObject {
    private long loadAddress;
    private String name;
    private final List<Short> textureIds = new ArrayList<>();
    private int textureIdSlotsAvailable = -1;

    public TextureRemapArray(SCGameInstance instance) {
        super(instance);
    }

    public TextureRemapArray(SCGameInstance instance, long loadAddress) {
        this(instance, null, loadAddress);
    }

    public TextureRemapArray(SCGameInstance instance, String name) {
        this(instance, name, 0);
    }

    public TextureRemapArray(SCGameInstance instance, String name, long loadAddress) {
        super(instance);
        this.loadAddress = loadAddress;
        this.name = name;
    }

    /**
     * Initialize the number of texture slots available.
     * @param slotsAvailable the number of texture slots available.
     */
    public void initTextureSlotsAvailable(int slotsAvailable) {
        if (this.textureIdSlotsAvailable != -1)
            throw new IllegalStateException("The slots available count has already been initialized.");
        if (slotsAvailable < 0)
            throw new IllegalArgumentException("Cannot apply texture slot value of " + slotsAvailable + ".");

        this.textureIdSlotsAvailable = slotsAvailable;
    }

    /**
     * Gets the remapped texture id.
     * @param localTextureId The local texture id to remap.
     * @return The remapped (global) texture id, or null if this is not in the remap.
     */
    public Short getRemappedTextureId(int localTextureId) {
        return localTextureId >= 0 && localTextureId < this.textureIds.size() ? this.textureIds.get(localTextureId) : null;
    }

    /**
     * Resolves the local remap index to a texture.
     * @param localTextureId the local remap texture ID
     * @param vloArchive If provided, the texture will first attempt to be resolved here before resolving elsewhere.
     * @return gameImageOrNull
     */
    public GameImage resolveTexture(int localTextureId, VLOArchive vloArchive) {
        Short globalTextureId = getRemappedTextureId(localTextureId);
        if (globalTextureId == null)
            return null;

        // First try the one the user supplied.
        if (vloArchive != null) {
            GameImage gameImage = vloArchive.getImageByTextureId(globalTextureId, false);
            if (gameImage != null)
                return gameImage;
        }

        // If all else fails, resolve the texture ID from any VLO we can find it in.
        return getGameInstance().getMainArchive().getImageByTextureId(globalTextureId);
    }

    /**
     * Gets all textures available in the remap.
     * @param vloArchive the vlo archive to prefer to lookup textures from
     * @return textures
     */
    public List<GameImage> getTextures(VLOArchive vloArchive) {
        List<GameImage> images = new ArrayList<>();
        for (int i = 0; i < this.textureIds.size(); i++)
            images.add(resolveTexture(i, vloArchive));

        return images;
    }

    /**
     * Asks the user to choose an image.
     * @param vloArchive the VLO to prefer resolving texture ids from
     * @param allowNull if null is allowed to be selected.
     * @param handler the handler to handle the user's selection.
     */
    public void askUserToSelectImage(VLOArchive vloArchive, boolean allowNull, Consumer<GameImage> handler) {
        List<GameImage> images = getTextures(vloArchive);
        if (images.isEmpty() && !allowNull)
            return; // Nothing to select from.

        if (allowNull)
            images.add(0, null);

        SelectionMenu.promptSelection(getGameInstance(), "Select an image.", handler, images, image -> {
            if (image == null)
                return "No Image";

            String originalName = image.getOriginalName();
            return "#" + image.getLocalImageID() +  " (" + (originalName != null ? originalName + ", " : "") + image.getTextureId() + ")";
        }, image -> image != null ? image.toFXImage(VLOArchive.ICON_EXPORT) : FXUtils.toFXImage(UnknownTextureSource.MAGENTA_INSTANCE.makeImage(), true));
    }

    /**
     * Get the index in the remap where the provided texture id is found.
     * @param globalTextureId the global texture id to find.
     * @return index, or -1 if the texture is not in the remap.
     */
    public int getRemapIndex(short globalTextureId) {
        for (int i = 0; i < this.textureIds.size(); i++)
            if (this.textureIds.get(i) == globalTextureId)
                return i;
        return -1;
    }

    /**
     * Gets the address this remap starts in the executable.
     */
    public int getReaderIndex() {
        return (int) (this.loadAddress - getGameInstance().getRamOffset());
    }

    /**
     * Sets the location of this remap to an offset relative to the start of the executable as a file.
     * @param offset The offset from the start of the file.
     */
    public void setFileOffset(int offset) {
        this.loadAddress = (offset + getGameInstance().getRamOffset());
    }

    /**
     * Gets a string identifying this texture remap
     */
    public String getDebugName() {
        if (this.name != null && !this.name.isEmpty())
            return "Remap[" + this.name + "]";

        return "Remap@" + NumberUtils.toHexString(this.loadAddress) + "/" + NumberUtils.toHexString(getReaderIndex()) + "[" + this.textureIds.size() + "]";
    }

    @Override
    public String toString() {
        return getDebugName();
    }

    /**
     * Creates a remap based on the contents of a VLO archive.
     */
    public static class VLODirectTextureRemapArray extends TextureRemapArray {
        private final VLOArchive vloArchive;

        public VLODirectTextureRemapArray(SCGameInstance instance, VLOArchive vloArchive) {
            super(instance);
            this.vloArchive = vloArchive;
            updateRemapArray();
        }

        /**
         * Update the texture remap id array.
         */
        public void updateRemapArray() {
            List<Short> values = getTextureIds();

            // Remove now unused texture slots.
            while (values.size() > this.vloArchive.getImages().size())
                values.remove(values.size() - 1);

            // Apply texture ids.
            for (int i = 0; i < this.vloArchive.getImages().size(); i++) {
                GameImage image = this.vloArchive.getImages().get(i);
                if (i >= values.size()) {
                    values.add(image.getTextureId());
                } else if (image.getTextureId() != values.get(i)) { // Avoid autoboxing a new short if it matches to begin with.
                    values.set(i, image.getTextureId());
                }
            }
        }
    }
}