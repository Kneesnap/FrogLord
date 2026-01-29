package net.highwayfrogs.editor.games.sony.frogger;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.LinkedTextureRemap;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;

/**
 * Represents a texture remap for a Frogger map.
 * Created by Kneesnap on 11/13/2023.
 */
@Getter
public class FroggerTextureRemap extends LinkedTextureRemap<FroggerMapFile> {
    public FroggerTextureRemap(FroggerGameInstance instance, MWIResourceEntry resourceEntry) {
        super(instance, resourceEntry, FroggerMapFile.class);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, MWIResourceEntry resourceEntry, String name) {
        super(instance, resourceEntry, FroggerMapFile.class, name);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, MWIResourceEntry resourceEntry, long loadAddress) {
        super(instance, resourceEntry, FroggerMapFile.class, loadAddress);
    }

    public FroggerTextureRemap(FroggerGameInstance instance, MWIResourceEntry resourceEntry, String name, long loadAddress) {
        super(instance, resourceEntry, FroggerMapFile.class, name, loadAddress);
    }

    @Override
    protected VloFile resolveVloFile(FroggerMapFile mapFile) {
        return mapFile.getVloFile();
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