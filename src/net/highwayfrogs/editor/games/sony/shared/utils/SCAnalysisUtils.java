package net.highwayfrogs.editor.games.sony.shared.utils;

import lombok.Data;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.shared.ISCTextureUser;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contains static file analysis tools.
 * Created by Kneesnap on 2/20/2019.
 */
public class SCAnalysisUtils {
    private static List<ISCTextureUser> getAllTextureUsers(SCGameInstance instance) {
        if (instance == null)
            throw new NullPointerException("instance");

        List<ISCTextureUser> textureUsers = new ArrayList<>();

        // Test the game instance itself.
        if (instance instanceof ISCTextureUser)
            textureUsers.add((ISCTextureUser) instance);

        // Test all game files.
        for (SCGameFile<?> gameFile : instance.getMainArchive().getAllFiles())
            if (gameFile instanceof ISCTextureUser)
                textureUsers.add((ISCTextureUser) gameFile);

        return textureUsers;
    }

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
        for (ISCTextureUser textureUser : getAllTextureUsers(instance))
            addTexturesToBitArray(usedTextures, textureUser);

        ILogger logger = instance.getLogger();
        for (VloFile vloArchive : instance.getMainArchive().getAllFiles(VloFile.class)) {
            int unusedTextures = 0;
            for (int i = 0; i < vloArchive.getImages().size(); i++) {
                VloImage image = vloArchive.getImages().get(i);
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

    /**
     * Prints unused textures found in a MWD File.
     * There are some VLOs which frogger can't determine if a texture is used or not, so it will not scan them.
     * Skipped: SKY_LAND textures, FIXE, OPT, LS, GEN, GENM, START, particle textures.
     * @param instance The game instance file to scan.
     */
    @SuppressWarnings("unchecked")
    public static List<SCTextureUsage>[] generateTextureUsageMapping(SCGameInstance instance) {
        if (instance == null)
            throw new NullPointerException("instance");

        List<List<SCTextureUsage>> textureUsagesById = new ArrayList<>();

        for (ISCTextureUser textureUser : getAllTextureUsers(instance)) {
            Set<SCTextureUsage> textureUsages = textureUser.getTextureUsages();
            if (textureUsages == null || textureUsages.isEmpty())
                continue;

            for (SCTextureUsage textureUsage : textureUsages) {
                while (textureUsage.getTextureId() >= textureUsagesById.size())
                    textureUsagesById.add(null);

                List<SCTextureUsage> usagesOfId = textureUsagesById.get(textureUsage.getTextureId());
                if (usagesOfId == null)
                    textureUsagesById.set(textureUsage.getTextureId(), usagesOfId = new ArrayList<>());
                usagesOfId.add(textureUsage);
            }
        }

        return (List<SCTextureUsage>[]) textureUsagesById.toArray(new List[0]);
    }

    @Data
    public static class SCTextureUsage {
        private final WeakReference<ISCTextureUser> textureUser;
        private final short textureId;
        private final String locationDescription;

        public SCTextureUsage(@NonNull ISCTextureUser textureUser, short textureId, String locationDescription) {
            this.textureUser = new WeakReference<>(textureUser);
            this.textureId = textureId;
            this.locationDescription = locationDescription;
        }
    }
}