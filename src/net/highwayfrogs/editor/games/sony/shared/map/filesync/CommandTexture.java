package net.highwayfrogs.editor.games.sony.shared.map.filesync;

import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloPadding;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.commandparser.CommandListException;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Represents the "texture" command, specifying a new texture in the texture remap.
 * Created by Kneesnap on 2/9/2026.
 */
public class CommandTexture<TContext extends MapFileSyncLoadContext<?>> extends PostInitializationCommand<TContext> {
    public static final String LABEL = "texture";
    public static final CommandTexture<?> INSTANCE = new CommandTexture<>();

    public CommandTexture() {
        super(LABEL, 1);
    }

    @Override
    public void execute(TContext context, OptionalArguments arguments) throws CommandListException {
        // The remap is updated when the command is run because polygon commands rely on the remap having been updated first.
        // Note that the texture remap is checked after all commands finish running to see if it overflows.
        short loadedTextureId = arguments.useNext().getAsShort(); // NOTE: This may be for a different version, and thus invalid!
        String textureFileName = arguments.hasNext() ? arguments.useNext().getAsString() : null;

        SCGameInstance instance = context.getMapFile().getGameInstance();
        List<Short> textureIds = context.getTextureRemap().getTextureIds();

        // If a texture name is present, it should be the be-all-end-all for identifying the texture, because names work across versions, support custom (new) textures cleanly, and are feasible to change by the user if not.
        if (!StringUtils.isNullOrWhiteSpace(textureFileName)) {
            VloFile mapVlo = context.getVloFile();
            VloImage imageByName = mapVlo != null ? mapVlo.getImageByName(textureFileName) : null;
            if (imageByName != null) {
                textureIds.add(imageByName.getTextureId());
                return;
            }

            // If the image wasn't found in the vlo, try original names.
            Short textureIdByName = instance.getTextureIdByOriginalName(textureFileName);
            if (textureIdByName != null) {
                textureIds.add(textureIdByName);
                return;
            }

            if (mapVlo == null) {
                context.getLogger().severe("No texture could be found which was named '%s'! (Could not create placeholder)", textureFileName);
                textureIds.add((short) -1);
                return;
            }

            // Add a placeholder texture.
            context.getLogger().warning("No texture could be found which was named '%s'! (Creating a placeholder texture...)", textureFileName);
            BufferedImage placeholderImage = UnknownTextureSource.MAGENTA_INSTANCE.makeImage();
            VloImage newTempImage = mapVlo.addImage(textureFileName, placeholderImage, VloPadding.DEFAULT, null, null, false);
            textureIds.add(newTempImage.getTextureId());
            return;
        }

        // Ensure texture ID is applicable to this version of the game.
        SCGameConfig mapFileCfg = context.getGameConfig();
        SCGameConfig realGameCfg = instance.getVersionConfig();
        if (!mapFileCfg.getInternalName().equals(realGameCfg.getInternalName())) {
            context.getLogger().severe("Cannot resolve texture ID %d because the file uses IDs from version '%s', but the game version currently loaded is '%s'!",
                    loadedTextureId, mapFileCfg.getInternalName(), realGameCfg.getInternalName());
            textureIds.add((short) -1);
            return;
        }

        textureIds.add(loadedTextureId);
    }
}
