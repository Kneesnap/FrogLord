package net.highwayfrogs.editor.games.sony.shared.utils;

import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntry;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains static file analysis tools.
 * Created by Kneesnap on 2/20/2019.
 */
public class SCAnalysisUtils {
    /**
     * Prints unused textures found in a MWD File.
     * There are some VLOs which frogger can't determine if a texture is used or not, so it will not scan them.
     * Skipped: SKY_LAND textures, FIXE, OPT, LS, GEN, GENM, START, particle textures.
     * @param instance The game instance file to scan.
     */
    public static void findUnusedTextures(SCGameInstance instance) {
        List<FroggerMapFile> mapFiles = instance.getMainArchive().getAllFiles(FroggerMapFile.class);
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
            TextureRemapArray remapTable = map.getTextureRemap();
            if (remapTable == null)
                return; // Failed to get a remap table. It's likely an unused map.

            // Populate animation data.
            for (FroggerMapAnimation animation : map.getAnimationPacket().getAnimations())
                for (short texValue : animation.getTextureIds())
                    textureIds.add(remapTable.getRemappedTextureId(texValue));

            // Populate face data.
            for (FroggerMapPolygon poly : map.getPolygonPacket().getPolygons())
                if (poly.getPolygonType().isTextured())
                    textureIds.add(remapTable.getRemappedTextureId(poly.getTextureId()));
        });

        // Find unused textures.
        if (instance.isFrogger()) {
            for (ThemeBook themeBook : ((FroggerGameInstance) instance).getThemeLibrary()) {
                if (themeBook.getTheme() == FroggerMapTheme.GENERAL)
                    continue; // Contains mostly things we can't check the validity of.

                Set<Short> loggedIds = new HashSet<>(); // Prevents logging both high and low poly versions.
                themeBook.forEachVLO(vloArchive -> {
                    for (GameImage image : vloArchive.getImages()) {
                        Short textureId = image.getTextureId();
                        if (!textureIds.contains(textureId) && loggedIds.add(textureId))
                            System.out.println(vloArchive.getFileDisplayName() + ": " + image.getLocalImageID() + " / " + textureId);
                    }
                });
            }
        }
    }
}