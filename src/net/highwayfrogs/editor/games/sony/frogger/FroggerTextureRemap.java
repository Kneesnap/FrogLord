package net.highwayfrogs.editor.games.sony.frogger;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.LinkedTextureRemap;

/**
 * Represents a texture remap for a Frogger map.
 * Created by Kneesnap on 11/13/2023.
 */
@Getter
public class FroggerTextureRemap extends LinkedTextureRemap<FroggerMapFile> {
    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry) {
        super(instance, fileEntry, FroggerMapFile.class);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry, String name) {
        super(instance, fileEntry, FroggerMapFile.class, name);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry, long loadAddress) {
        super(instance, fileEntry, FroggerMapFile.class, loadAddress);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry, String name, long loadAddress) {
        super(instance, fileEntry, FroggerMapFile.class, name, loadAddress);
    }

    @Override
    public FroggerGameInstance getGameInstance() {
        return (FroggerGameInstance) super.getGameInstance();
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }
}