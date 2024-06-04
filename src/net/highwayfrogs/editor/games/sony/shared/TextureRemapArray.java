package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameObject.SCSharedGameObject;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a texture remap array.
 * Created by Kneesnap on 10/25/2023.
 */
@Getter
public class TextureRemapArray extends SCSharedGameObject {
    @Setter private long loadAddress;
    private String name;
    private final List<Short> textureIds = new ArrayList<>();

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
     * Gets the remapped texture id.
     * @param localTextureId The local texture id to remap.
     * @return The remapped (global) texture id, or null if this is not in the remap.
     */
    public Short getRemappedTextureId(int localTextureId) {
        return localTextureId >= 0 && localTextureId < this.textureIds.size() ? this.textureIds.get(localTextureId) : null;
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

        return "Remap@" + Utils.toHexString(this.loadAddress) + "/" + Utils.toHexString(getReaderIndex()) + "[" + this.textureIds.size() + "]";
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