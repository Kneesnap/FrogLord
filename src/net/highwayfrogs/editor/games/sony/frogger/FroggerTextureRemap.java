package net.highwayfrogs.editor.games.sony.frogger;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.games.sony.shared.LinkedTextureRemap;

/**
 * Represents a texture remap for a Frogger map.
 * Created by Kneesnap on 11/13/2023.
 */
@Getter
public class FroggerTextureRemap extends LinkedTextureRemap<MAPFile> {
    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry) {
        super(instance, fileEntry, MAPFile.class);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry, String name) {
        super(instance, fileEntry, MAPFile.class, name);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry, long loadAddress) {
        super(instance, fileEntry, MAPFile.class, loadAddress);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, FileEntry fileEntry, String name, long loadAddress) {
        super(instance, fileEntry, MAPFile.class, name, loadAddress);
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