package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import lombok.Setter;
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
    @Setter private int loadAddress;
    private String name;
    private final List<Short> textureIds = new ArrayList<>();

    public TextureRemapArray(SCGameInstance instance) {
        super(instance);
    }

    public TextureRemapArray(SCGameInstance instance, int loadAddress) {
        this(instance, null, loadAddress);
    }

    public TextureRemapArray(SCGameInstance instance, String name) {
        this(instance, name, 0);
    }

    public TextureRemapArray(SCGameInstance instance, String name, int loadAddress) {
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
     * Gets the address this remap starts in the executable.
     */
    public int getReaderIndex() {
        return (int) (this.loadAddress - getConfig().getRamPointerOffset());
    }

    /**
     * Sets the location of this remap to an offset relative to the start of the executable as a file.
     * @param offset The offset from the start of the file.
     */
    public void setFileOffset(int offset) {
        this.loadAddress = (int) (offset + getConfig().getRamPointerOffset());
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
}