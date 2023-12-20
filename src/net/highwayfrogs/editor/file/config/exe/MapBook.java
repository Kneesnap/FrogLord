package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.exe.pc.PCMapBook;
import net.highwayfrogs.editor.file.config.exe.psx.PSXMapBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerTextureRemap;
import net.highwayfrogs.editor.utils.Utils;

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
    public abstract WADFile getWad(MAPFile map);

    /**
     * Add a texture remap for a level.
     * @param instance      The game instance to add the remap to.
     * @param mapResourceId The id of the map file.
     * @param remapPointer  The runtime pointer address to the remap.
     * @param lowPoly       If win95 low poly mode is enabled.
     */
    protected static void addRemap(FroggerGameInstance instance, int mapResourceId, int remapPointer, boolean lowPoly) {
        if (mapResourceId <= 0 || remapPointer <= 0)
            return; // Invalid.

        FileEntry entry = instance.getResourceEntryByID(mapResourceId);
        if (entry == null) {
            System.out.println("WARNING: Couldn't find map with resource ID " + mapResourceId);
            return;
        }

        String name = entry.hasFullFilePath() ? "txl_" + Utils.stripExtension(entry.getDisplayName()).toLowerCase(Locale.ROOT) + (lowPoly ? "_win95" : "") : null;
        instance.addRemap(new FroggerTextureRemap(instance, entry, name, remapPointer));
    }
}