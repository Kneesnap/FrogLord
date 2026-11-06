package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.NonNull;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.patch.GamePatch;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides the patches bundled with FrogLord for Frogger, and loads external patch files.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerPatchLibrary {
    private static List<GamePatch> bundledPatches;

    private static final String PATCH_FOLDER = "patches";
    private static final ILogger LOGGER = ClassNameLogger.getLogger(null, FroggerPatchLibrary.class);

    /**
     * Gets the patches bundled with FrogLord, loading them from the resource folder on first use.
     * A bundled patch which fails to load is logged and left out.
     * @return bundledPatches
     */
    public static synchronized List<GamePatch> getBundledPatches() {
        if (bundledPatches != null)
            return bundledPatches;

        List<GamePatch> patches = new ArrayList<>();
        for (URL patchLocation : FileUtils.getInternalResourceFilesInDirectory(SCGameType.FROGGER.getEmbeddedResourceURL(PATCH_FOLDER), true)) {
            try (InputStream inputStream = patchLocation.openStream()) {
                GamePatch patch = new GamePatch();
                patch.loadPatchFromConfig(FileUtils.getFileNameWithoutExtension(patchLocation), new Config(inputStream));
                patches.add(patch);
            } catch (Throwable th) {
                LOGGER.throwing("FroggerPatchLibrary", "getBundledPatches", new RuntimeException("Failed to load '" + patchLocation + "'.", th));
            }
        }

        bundledPatches = Collections.unmodifiableList(patches);
        return bundledPatches;
    }

    /**
     * Gets a bundled patch by its identifier, the stem of its file name.
     * @param id the patch identifier, such as "fps"
     * @return bundledPatch, or null if there is none with that identifier
     */
    public static GamePatch getPatchById(String id) {
        if (id == null)
            return null;

        for (GamePatch patch : getBundledPatches())
            if (id.equalsIgnoreCase(patch.getId()))
                return patch;

        return null;
    }

    /**
     * Gets the bundled patches whose versions list contains the given version.
     * @param internalName the internal name of the game version, such as "pc-retail-v3.0e"
     * @return compatiblePatches
     */
    public static List<GamePatch> getPatchesForVersion(String internalName) {
        List<GamePatch> results = new ArrayList<>();
        for (GamePatch patch : getBundledPatches())
            if (patch.isCompatibleWithVersion(internalName))
                results.add(patch);

        return results;
    }

    /**
     * Loads a patch from a file outside of FrogLord. Its identifier is the stem of the file name.
     * @param patchFile the patch file to load
     * @return loadedPatch
     */
    public static GamePatch loadExternalPatch(@NonNull File patchFile) {
        GamePatch patch = new GamePatch();
        patch.loadPatchFromConfig(FileUtils.stripSingleExtension(patchFile.getName()), new Config(FileUtils.readLinesFromFile(patchFile)));
        return patch;
    }
}
