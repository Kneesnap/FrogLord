package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.config.exe.ThemeBook;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolyTexture;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.MOFPart;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntry;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.vlo.GameImage;

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
     * TODO: Eventually, this should exclude SKY_LAND textures.
     * @param mwd The MWD file to scan.
     */
    public static void findUnusedTextures(MWDFile mwd) {
        FroggerEXEInfo config = mwd.getConfig();

        List<MAPFile> mapFiles = mwd.getFiles(MAPFile.class);
        List<MOFHolder> mofFiles = new ArrayList<>();

        mwd.forEachFile(WADFile.class, wad -> wad.getFiles().forEach(entry -> {
            if (entry.getFile() instanceof MOFHolder)
                mofFiles.add((MOFHolder) entry.getFile());
        }));

        Set<Short> textureIds = new HashSet<>();

        // Populate Level Name ids.
        for (LevelInfo info : config.getArcadeLevelInfo()) {
            int texId = config.getTextureIdFromPointer(info.getLevelNameTextureInGamePointer());
            if (texId >= 0)
                textureIds.add((short) texId);
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
            List<Short> remapTable = mwd.getConfig().getRemapTable(map.getFileEntry());
            if (remapTable == null)
                return; // Failed to get a remap table. It's likely an unused map.

            // Populate animation data.
            for (MAPAnimation animation : map.getMapAnimations())
                for (short texValue : animation.getTextures())
                    textureIds.add(remapTable.get(texValue));

            // Populate face data.
            map.forEachPrimitive(prim -> {
                if (prim instanceof MAPPolyTexture)
                    textureIds.add(remapTable.get(((MAPPolyTexture) prim).getTextureId()));
            });
        });

        // Find unused textures.
        for (ThemeBook themeBook : mwd.getConfig().getThemeLibrary()) {
            if (themeBook.getTheme() == MAPTheme.GENERAL)
                continue; // Contains mostly things we can't check the validity of.

            Set<Short> loggedIds = new HashSet<>(); // Prevents logging both high and low poly versions.
            themeBook.forEachVLO(vloArchive -> {
                for (GameImage image : vloArchive.getImages()) {
                    Short textureId = image.getTextureId();
                    if (!textureIds.contains(textureId) && loggedIds.add(textureId))
                        System.out.println(vloArchive.getFileEntry().getDisplayName() + ": " + image.getLocalImageID() + " / " + textureId);
                }
            });
        }
    }
}
