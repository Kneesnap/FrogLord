package net.highwayfrogs.editor.games.sony.frogger.utils;

import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.utils.DynamicMeshObjExporter;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;

/**
 * Contains static utilities specifically for Frogger.
 * Created by Kneesnap on 5/2/2025.
 */
public class FroggerUtils {
    /**
     * Exports a map file to wavefront obj.
     * @param map The map to export.
     * @param directory The directory to export it to.
     */
    public static void exportMapToObj(FroggerMapFile map, File directory) {
        if (map == null)
            throw new NullPointerException("map");
        if (directory == null)
            throw new NullPointerException("directory");
        if (!directory.isDirectory())
            throw new IllegalArgumentException("The provided destination was not a directory!");

        FroggerMapMesh mapMesh = new FroggerMapMesh(map);
        String cleanName = FileUtils.stripExtension(map.getFileDisplayName());
        DynamicMeshObjExporter.exportMeshToObj(map.getLogger(), mapMesh, directory, cleanName, true);
    }

    /**
     * Gets the Frogger map theme which the WAD corresponds to.
     * Returns null if the game is not Frogger, or there is no map theme.
     * @return froggerMapTheme
     */
    public static FroggerMapTheme getFroggerMapTheme(WADFile wadFile) {
        if (wadFile == null)
            return null;
        if (!wadFile.getGameInstance().isFrogger())
            return null;

        ThemeBook themeBook = null;
        for (ThemeBook book : ((FroggerGameInstance) wadFile.getGameInstance()).getThemeLibrary()) {
            if (book != null && book.isEntry(wadFile)) {
                themeBook = book;
                if (themeBook.getTheme() != null)
                    break;
            }
        }

        return themeBook != null && themeBook.getTheme() != null
                ? themeBook.getTheme() : FroggerMapTheme.getTheme(wadFile.getFileDisplayName());
    }

    /**
     * Checks if this map is a low-poly file.
     * Unfortunately, nothing distinguishes the files besides where you can access them from and the names.
     * @return isLowPolyMode
     */
    public static boolean isLowPolyMode(SCGameFile<?> gameFile) {
        return gameFile != null && gameFile.getGameInstance().isFrogger() && gameFile.getGameInstance().isPC()
                && (gameFile.getFileDisplayName().contains("_WIN95") || gameFile.getFileDisplayName().equals("OPTIONSL.WAD"));
    }

    /**
     * Checks if the provided file is used for multiplayer gameplay.
     * Unfortunately, nothing distinguishes the files besides where you can access them from and the file names themselves.
     * @return isMultiplayer
     */
    public static boolean isMultiplayerFile(SCGameFile<?> gameFile, FroggerMapTheme mapTheme) {
        return gameFile != null && mapTheme != null && gameFile.getGameInstance().isFrogger()
                && gameFile.getFileDisplayName().contains(mapTheme.getInternalName() + "M");
    }
}
