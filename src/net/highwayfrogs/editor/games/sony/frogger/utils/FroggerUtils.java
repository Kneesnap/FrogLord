package net.highwayfrogs.editor.games.sony.frogger.utils;

import javafx.scene.control.Alert;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.utils.DynamicMeshObjExporter;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * Gets all instances of Frogger except the provided game instance.
     * @param instance the instance to search from
     * @return froggerInstances, if there are any.
     */
    public static List<FroggerGameInstance> getAllFroggerInstancesExcept(GameInstance instance) {
        List<FroggerGameInstance> instances = Collections.emptyList();
        for (GameInstance testInstance : FrogLordApplication.getActiveGameInstances()) {
            if (!(testInstance instanceof FroggerGameInstance) || testInstance == instance)
                continue;

            if (instances.isEmpty())
                instances = new ArrayList<>();
            instances.add((FroggerGameInstance) testInstance);
        }

        return instances;
    }

    /**
     * Gets the only other active instance of Frogger which is not the provided game instance.
     * @param instance the instance to search from
     * @return froggerInstance, if there is one.
     */
    public static FroggerGameInstance getOtherFroggerInstanceOrWarnUser(GameInstance instance) {
        FroggerGameInstance foundInstance = null;
        for (GameInstance testInstance : FrogLordApplication.getActiveGameInstances()) {
            if (!(testInstance instanceof FroggerGameInstance) || testInstance == instance)
                continue;

            if (foundInstance != null) {
                FXUtils.makePopUp("There is more than one copy of Frogger open at once,\n so FrogLord is unable to choose which version to convert the map to.", Alert.AlertType.ERROR);
                return null;
            }

            foundInstance = (FroggerGameInstance) testInstance;
        }

        if (foundInstance == null) {
            FXUtils.makePopUp("Please open a copy of Frogger to target.", Alert.AlertType.ERROR);
            return null;
        }

        return foundInstance;
    }
}
