package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a texture remap linked to a specific file.
 * Created by Kneesnap on 11/13/2023.
 */
@Getter
public class LinkedTextureRemap<TFile extends SCGameFile<?>> extends TextureRemapArray {
    private final FileEntry fileEntry;
    private final Class<TFile> fileClass;

    public LinkedTextureRemap(SCGameInstance instance, FileEntry fileEntry, Class<TFile> fileClass) {
        this(instance, fileEntry, fileClass, null, 0);
    }

    public LinkedTextureRemap(SCGameInstance instance, FileEntry fileEntry, Class<TFile> fileClass, String name) {
        this(instance, fileEntry, fileClass, name, 0);
    }

    public LinkedTextureRemap(SCGameInstance instance, FileEntry fileEntry, Class<TFile> fileClass, long loadAddress) {
        this(instance, fileEntry, fileClass, null, loadAddress);
    }

    public LinkedTextureRemap(SCGameInstance instance, FileEntry fileEntry, Class<TFile> fileClass, String name, long loadAddress) {
        super(instance, name, loadAddress);
        this.fileEntry = fileEntry;
        this.fileClass = fileClass;
    }

    /**
     * Gets the file linked to this texture remap.
     * @return file
     */
    public TFile getFile() {
        SCGameFile<?> file = getArchive().getFiles().get(this.fileEntry.getResourceId());
        if (file == null)
            return null;

        if (!this.fileClass.isInstance(file))
            throw new ClassCastException("The file was of type " + Utils.getSimpleName(file) + ", but a(n) " + Utils.getSimpleName(this.fileClass) + " was requested.");

        return this.fileClass.cast(file);
    }
}