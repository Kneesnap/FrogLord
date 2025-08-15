package net.highwayfrogs.editor.games.sony.shared.utils;

import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.shared.ISCTextureUser;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.util.List;

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
        if (instance == null)
            throw new NullPointerException("instance");

        // Mark textures referenced by files as used.
        IndexBitArray usedTextures = new IndexBitArray();
        for (SCGameFile<?> gameFile : instance.getMainArchive().getAllFiles())
            if (gameFile instanceof ISCTextureUser)
                addTexturesToBitArray(usedTextures, (ISCTextureUser) gameFile);

        // Mark all textures referenced by the game instance as used.
        if (instance instanceof ISCTextureUser)
            addTexturesToBitArray(usedTextures, (ISCTextureUser) instance);

        ILogger logger = instance.getLogger();
        for (VLOArchive vloArchive : instance.getMainArchive().getAllFiles(VLOArchive.class)) {
            int unusedTextures = 0;
            for (int i = 0; i < vloArchive.getImages().size(); i++) {
                GameImage image = vloArchive.getImages().get(i);
                if (usedTextures.getBit(image.getTextureId()))
                    continue;

                if (unusedTextures++ == 0) {
                    logger.info("");
                    logger.info("Unused Images in %s:", vloArchive.getFileDisplayName());
                }

                String imageName = image.getOriginalName();
                if (imageName == null)
                    imageName = SCUtils.C_UNNAMED_IMAGE_PREFIX + image.getTextureId();

                logger.info(" - %s (Local ID %d)", imageName, i);
            }

            if (unusedTextures > 0)
                logger.info(" - Total: %d texture(s)", unusedTextures);
        }
    }

    private static void addTexturesToBitArray(IndexBitArray usedTextures, ISCTextureUser textureUser) {
        if (textureUser == null)
            return;

        List<Short> textureIds = textureUser.getUsedTextureIds();
        if (textureIds == null || textureIds.isEmpty())
            return;

        for (int i = 0; i < textureIds.size(); i++) {
            Short textureId = textureIds.get(i);
            if (textureId != null && textureId >= 0)
                usedTextures.setBit(textureId, true);
        }
    }
}