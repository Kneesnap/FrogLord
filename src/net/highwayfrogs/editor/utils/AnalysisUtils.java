package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntry;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains static file analysis tools.
 * Created by Kneesnap on 2/20/2019.
 */
public class AnalysisUtils {
    /**
     * Prints unused textures found in a MWD File.
     * There are some VLOs which frogger can't determine if a texture is used or not, so it will not scan them.
     * Skipped: SKY_LAND textures, FIXE, OPT, LS, GEN, GENM, START, particle textures.
     * @param instance The game instance file to scan.
     */
    public static void findUnusedTextures(SCGameInstance instance) {
        List<MAPFile> mapFiles = instance.getMainArchive().getAllFiles(MAPFile.class);
        List<MOFHolder> mofFiles = new ArrayList<>();

        instance.getMainArchive().forEachFile(WADFile.class, wad -> wad.getFiles().forEach(entry -> {
            if (entry.getFile() instanceof MOFHolder)
                mofFiles.add((MOFHolder) entry.getFile());
        }));

        Set<Short> textureIds = new HashSet<>();

        // Populate Level Name ids.
        if (instance.isFrogger()) {
            for (LevelInfo info : ((FroggerGameInstance) instance).getArcadeLevelInfo()) {
                int texId = instance.getTextureIdFromPointer(info.getLevelNameTextureInGamePointer());
                if (texId >= 0)
                    textureIds.add((short) texId);
            }
        }

        // Populate MOF Ids.
        mofFiles.forEach(holder -> {
            MOFFile mof = holder.asStaticFile();

            // Populate MOF animation.
            for (MOFPart part : mof.getParts())
                for (MOFPartPolyAnim partPolyAnim : part.getPartPolyAnims())
                    for (MOFPartPolyAnimEntry entry : partPolyAnim.getEntryList().getEntries())
                        textureIds.add((short) entry.getImageId());

            // Populate MOF face textures.
            mof.forEachPolygon(poly -> {
                if (poly instanceof MOFPolyTexture)
                    textureIds.add(((MOFPolyTexture) poly).getImageId());
            });
        });

        // Populate MAP ids.
        mapFiles.forEach(map -> {
            List<Short> remapTable = map.getRemapTable();
            if (remapTable == null)
                return; // Failed to get a remap table. It's likely an unused map.

            // Populate animation data.
            for (MAPAnimation animation : map.getMapAnimations())
                for (short texValue : animation.getTextures())
                    textureIds.add(remapTable.get(texValue));

            // Populate face data.
            for (MAPPolygon poly : map.getAllPolygons())
                if (poly instanceof MAPPolyTexture)
                    textureIds.add(remapTable.get(((MAPPolyTexture) poly).getTextureId()));
        });

        // Find unused textures.
        if (instance.isFrogger()) {
            for (ThemeBook themeBook : ((FroggerGameInstance) instance).getThemeLibrary()) {
                if (themeBook.getTheme() == MAPTheme.GENERAL)
                    continue; // Contains mostly things we can't check the validity of.

                Set<Short> loggedIds = new HashSet<>(); // Prevents logging both high and low poly versions.
                themeBook.forEachVLO(vloArchive -> {
                    for (GameImage image : vloArchive.getImages()) {
                        Short textureId = image.getTextureId();
                        if (!textureIds.contains(textureId) && loggedIds.add(textureId))
                            System.out.println(vloArchive.getIndexEntry().getDisplayName() + ": " + image.getLocalImageID() + " / " + textureId);
                    }
                });
            }
        }
    }
}