package net.highwayfrogs.editor.games.sony.frogger.utils;

import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
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
}
