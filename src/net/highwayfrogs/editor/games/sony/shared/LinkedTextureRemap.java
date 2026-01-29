package net.highwayfrogs.editor.games.sony.shared;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents a texture remap linked to a specific file.
 * Created by Kneesnap on 11/13/2023.
 */
@Getter
public class LinkedTextureRemap<TFile extends SCGameFile<?>> extends TextureRemapArray {
    private final MWIResourceEntry resourceEntry;
    private final Class<TFile> fileClass;

    public LinkedTextureRemap(SCGameInstance instance, MWIResourceEntry resourceEntry, Class<TFile> fileClass) {
        this(instance, resourceEntry, fileClass, null, 0);
    }

    public LinkedTextureRemap(SCGameInstance instance, MWIResourceEntry resourceEntry, Class<TFile> fileClass, String name) {
        this(instance, resourceEntry, fileClass, name, 0);
    }

    public LinkedTextureRemap(SCGameInstance instance, MWIResourceEntry resourceEntry, Class<TFile> fileClass, long loadAddress) {
        this(instance, resourceEntry, fileClass, null, loadAddress);
    }

    public LinkedTextureRemap(SCGameInstance instance, MWIResourceEntry resourceEntry, Class<TFile> fileClass, String name, long loadAddress) {
        super(instance, name, loadAddress);
        this.resourceEntry = resourceEntry;
        this.fileClass = fileClass;

        TFile file = getFile();
        if (file != null) {
            VloFile vloFile = resolveVloFile(file);
            if (vloFile != null)
                setVloFileDefinition(vloFile.getIndexEntry());
        }
    }

    /**
     * Gets the vlo file holding textures for this remap.
     * @param file the file to resolve for
     * @return vloFile, if known/exists
     */
    protected VloFile resolveVloFile(TFile file) {
        return null;
    }

    /**
     * Gets the file linked to this texture remap.
     * @return file
     */
    public TFile getFile() {
        SCGameFile<?> file = getArchive().getFiles().get(this.resourceEntry.getResourceId());
        if (file == null)
            return null;

        if (!this.fileClass.isInstance(file))
            throw new ClassCastException("The file was of type " + Utils.getSimpleName(file) + ", but a(n) " + Utils.getSimpleName(this.fileClass) + " was requested.");

        return this.fileClass.cast(file);
    }
}