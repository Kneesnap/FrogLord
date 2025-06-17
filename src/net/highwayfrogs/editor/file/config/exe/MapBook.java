package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerTextureRemap;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.utils.FileUtils;

import java.util.Locale;
import java.util.function.Function;

/**
 * A general mapbook struct.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class MapBook extends ExeStruct {
    public MapBook(FroggerGameInstance instance) {
        super(instance);
    }

    /**
     * Sets up texture remaps for maps tracked by this map book.
     */
    public abstract void addTextureRemaps(FroggerGameInstance instance);

    /**
     * Check if this map book is dummied.
     * @return isDummy
     */
    public abstract boolean isDummy();

    /**
     * Execute something depending on which MapBook type this is.
     * @param pcHandler  The PC handler.
     * @param psxHandler The psx handler.
     * @return result
     */
    public abstract <T> T execute(Function<PCMapBook, T> pcHandler, Function<PSXMapBook, T> psxHandler);

    /**
     * Gets the wad file for a given map.
     * @param map The map to get the wad file for.
     * @return wadFile
     */
    public abstract WADFile getLevelWad(FroggerMapFile map);

    /**
     * Gets the wad file for a given map.
     * @param map The map to get the wad file for.
     * @return wadFile
     */
    public abstract WADFile getThemeWad(FroggerMapFile map);

    /**
     * Add a texture remap for a level.
     * @param instance      The game instance to add the remap to.
     * @param mapResourceId The id of the map file.
     * @param remapPointer  The runtime pointer address to the remap.
     * @param lowPoly       If win95 low poly mode is enabled.
     */
    protected static void addRemap(FroggerGameInstance instance, int mapResourceId, long remapPointer, boolean lowPoly) {
        if (mapResourceId <= 0 || remapPointer <= 0)
            return; // Invalid.

        MWIResourceEntry entry = instance.getResourceEntryByID(mapResourceId);
        if (entry == null) {
            instance.getLogger().warning("Couldn't find map with resource ID: %d.", mapResourceId);
            return;
        }

        String name = entry.hasFullFilePath() ? "txl_" + FileUtils.stripExtension(entry.getDisplayName()).toLowerCase(Locale.ROOT) : null;
        instance.addRemap(new FroggerTextureRemap(instance, entry, name, remapPointer));
    }
}