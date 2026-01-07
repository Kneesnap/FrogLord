package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntry;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntryStreamAttributes;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestAssetBinFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.generic.ILateResourceResolver;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDescType;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.image.ImageUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.MessageTrackingLogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;

/**
 * Contains static utility functions which make exporting/importing Frogger: The Great Quest assets easier.
 * Created by Kneesnap on 11/2/2024.
 */
public class GreatQuestAssetUtils {
    private static final String CONFIG_SECTION_TEXTURES = "Textures";
    private static final String CONFIG_SECTION_MODELS = "Models";
    private static final String CONFIG_SECTION_SOUND_EFFECTS = "SoundEffects";
    public static final String CONFIG_SECTION_COPY_RESOURCES = "CopyResources";
    private static final String CONFIG_SECTION_DELETE_RESOURCES = "DeleteResources";
    private static final String CONFIG_SECTION_ANIMATIONS = "Animations";
    public static final String CONFIG_SECTION_ACTION_SEQUENCES = "Sequences";
    public static final String CONFIG_SECTION_LAUNCHERS = "Launchers";
    public static final String CONFIG_SECTION_DIALOG = "Dialog";
    public static final String CONFIG_SECTION_COLLISION_PROXIES = "Collision";
    public static final String CONFIG_SECTION_ENTITY_DESCRIPTIONS = "EntityDescriptions";
    public static final String CONFIG_SECTION_ENTITIES = "Entities";
    private static final String CONFIG_SECTION_SCRIPTS = "Scripts";
    private static final String CONFIG_SECTION_INCLUDE = "Include";
    private static final String CONFIG_OPTION_CREATE_MODEL_DESC = "CreateModelDesc";

    /**
     * Reads a great quest script group from a config, and applies it to the chunked file.
     * A section named 'Dialog' can contain key-value pairs of dialog strings to add/replace.
     * A section named 'Entities' can contain entity definitions which should be added/set.
     * A section named 'Scripts' can contain entity script definitions.
     * @param workingDirectory the working directory to load other files from. Can be null, but it would disable any features loading external files.
     * @param chunkedFile the file to apply the script group to
     * @param gqsScriptGroup the script group config to apply
     */
    public static void applyGqsScriptGroup(File workingDirectory, GreatQuestChunkedFile chunkedFile, Config gqsScriptGroup) {
        if (chunkedFile == null)
            throw new NullPointerException("chunkedFile");
        if (gqsScriptGroup == null)
            throw new NullPointerException("gqsScriptGroup");
        if (workingDirectory != null && !workingDirectory.isDirectory())
            throw new IllegalArgumentException("The provided workingDirectory was not a directory! " + workingDirectory);

        MessageTrackingLogger logger = new MessageTrackingLogger(chunkedFile.getLogger());
        String sourceName = gqsScriptGroup.getSectionName();
        kcScriptList scriptList = chunkedFile.getScriptList();
        if (scriptList == null)
            throw new RuntimeException(chunkedFile.getDebugName() + " does not have any script data, so we cannot apply " + sourceName + "!");

        // Apply the GQS script group.
        applyGqsScriptGroup(logger, scriptList, gqsScriptGroup, workingDirectory);

        // Print advanced warnings after everything is complete.
        scriptList.printAdvancedWarnings(logger);

        // Show popup.
        logger.showImportPopup(sourceName);
    }

    private static void applyGqsScriptGroup(ILogger logger, kcScriptList scriptList, Config gqsScriptGroup, File workingDirectory) {
        GreatQuestChunkedFile chunkedFile = scriptList.getParentFile();

        List<ILateResourceResolver> lateResolvers = new ArrayList<>();

        // Resource deletion should never impact anything created as part of the gqs file, so it should run first.
        deleteResources(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_DELETE_RESOURCES));

        // Dialog is independent of all else, so it happens early.
        applyStringResources(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_DIALOG));

        // Should apply before scripts/entities.
        applyTextures(chunkedFile, logger, workingDirectory, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_TEXTURES));
        applySoundEffects(chunkedFile, logger, workingDirectory, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_SOUND_EFFECTS));

        // Should occur before resource copying, so that any copied resources can resolve the model/collision references.
        applyModelReferences(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_MODELS));
        applyCollisionProxies(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_COLLISION_PROXIES)); // Before entity descriptions, and before resource copies, so these can be resolved in both.
        copyResources(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_COPY_RESOURCES), lateResolvers);

        // Should occur after resource copying, so any resources done here can resolve the copied resources.
        updateAnimationSets(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ANIMATIONS));
        List<kcCResourceNamedHash> sequenceTables = applyActionSequences(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ACTION_SEQUENCES));

        // This should occur after resource copying to ensure it can resolve resources. Copied resources shouldn't reference entity descriptions since entity instances (a resource which is not expected to be copied) are the only resource to resolve entity descriptions.
        // This should also happen before entity instances are applied.
        applyEntityDescriptions(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ENTITY_DESCRIPTIONS), lateResolvers);
        applyLauncherParams(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_LAUNCHERS)); // Must run after particle emitter params (entity descriptions) and .vtx references are imported, but before scripts.

        // Run before scripts, but after entity descriptions.
        applyEntityInstances(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ENTITIES), scriptList);
        applyScripts(chunkedFile, logger, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_SCRIPTS), scriptList);

        // Warn about unused sequences. (Likely indicates a typo.)
        warnAboutUnusedSequences(chunkedFile, logger, sequenceTables);

        // Some resources can't fully resolve when they are loaded. (Happens after entities are created, so the next/prev entities can be resolved successfully.
        // Some examples include waypoint descriptions referencing entity instances, or [CopyResources] requiring descriptions not yet created.
        // Resolve all remaining resolves. This may show warnings if certain things are not resolved.
        for (int i = 0; i < lateResolvers.size(); i++)
            lateResolvers.get(i).resolvePendingResources(logger);

        // Must be done before the unused test is performed to ensure this section isn't seen as unused.
        Config includeCfg = gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_INCLUDE);
        List<String> includeFilePaths = includeCfg != null ? includeCfg.getTextWithoutComments() : null;

        // Warn if any bits were unused.
        gqsScriptGroup.recursivelyWarnAboutUnusedData(logger);

        // Include other gqs files.
        if (includeFilePaths != null) {
            for (int i = 0; i < includeFilePaths.size(); i++) {
                String filePath = includeFilePaths.get(i);
                File file = new File(workingDirectory, filePath);
                if (!file.exists() || !file.isFile()) {
                    logger.severe("Cannot include the file '%s', as it does not exist.", file);
                    continue;
                }

                Config includedGqsConfig = Config.loadConfigFromTextFile(file, false);
                applyGqsScriptGroup(logger, scriptList, includedGqsConfig, file.getParentFile());
            }
        }
    }

    private static void applyStringResources(GreatQuestChunkedFile chunkedFile, Config dialogCfg) {
        if (dialogCfg == null)
            return;

        for (Entry<String, ConfigValueNode> entry : dialogCfg.getKeyValuePairs().entrySet()) {
            String dialogResName = entry.getKey();

            // Get or replace generic resource.
            kcCResourceGeneric generic = chunkedFile.getGenericResourceByName(dialogResName, kcCResourceGenericType.STRING_RESOURCE);
            if (generic == null) {
                generic = new kcCResourceGeneric(chunkedFile, kcCResourceGenericType.STRING_RESOURCE);
                generic.setName(dialogResName, true);
                chunkedFile.addResource(generic);
            }

            generic.getAsStringResource().setValue(entry.getValue().getAsString());
        }
    }

    // Must run after particle params are loaded, and after vtx models are imported, but before scripts.
    private static void applyLauncherParams(GreatQuestChunkedFile chunkedFile, ILogger logger, Config launcherParamsCfg) {
        if (launcherParamsCfg == null)
            return;

        for (Config launcherParamCfg : launcherParamsCfg.getChildConfigNodes()) {
            String launcherParamName = launcherParamCfg.getSectionName();
            kcCResourceGeneric generic = GreatQuestUtils.findLevelResourceByName(chunkedFile, launcherParamName, kcCResourceGeneric.class);
            if (generic == null) {
                generic = new kcCResourceGeneric(chunkedFile);
                generic.setName(launcherParamName, true);
                generic.setResourceData(new LauncherParams(generic));
                chunkedFile.addResource(generic);
            }

            LauncherParams launcherParams = generic.getAsLauncherParams();
            if (launcherParams == null)
                throw new RuntimeException("Found a resource named '" + launcherParamName + "', which was expected to be launcher params, but was actually a(n) " + generic.getResourceType() + ".");

            launcherParams.fromConfig(logger, launcherParamCfg);
        }
    }

    private static final String FLAG_NAME_DELETE = "Delete";
    private static void applySoundEffects(GreatQuestChunkedFile chunkedFile, ILogger logger, File workingDirectory, Config soundEffectsCfg) {
        if (soundEffectsCfg == null)
            return;
        if (chunkedFile.getGameInstance().getSoundChunkFile() == null)
            throw new IllegalStateException("Cannot apply sound changes, because the sound files were not found/loaded in a folder named SOUND\\ found in the directory holding 'data.bin'!");

        String sourceName = soundEffectsCfg.getRootNode().getSectionName();
        SBRFile sbrFile = chunkedFile.getSoundBankFile();
        if (sbrFile == null) {
            logger.warning("Skipping sound file references in %s, as the sound bank file could not be resolved.", sourceName);
            return;
        }

        for (String line : soundEffectsCfg.getTextWithoutComments()) {
            OptionalArguments arguments = OptionalArguments.parse(line);
            String filePath = arguments.useNext().getAsString();
            int sfxId = chunkedFile.getGameInstance().getSfxIdFromFullSoundPath(filePath);
            boolean deleteEntry = arguments.useFlag(FLAG_NAME_DELETE);

            SfxEntry sfxEntry = null;
            if (sfxId < 0) {
                if (!arguments.has(SfxEntry.FLAG_NAME_IMPORT)) { // If we're importing, then we create a new sound instead!
                    logger.warning("Skipping sound file reference '%s' in %s, it could not be resolved.", filePath, sourceName);
                    continue;
                }

                // Continue by building a new entry.
                sfxId = chunkedFile.getGameInstance().useNextFreeSoundIdSlot();
                chunkedFile.getGameInstance().getSoundModData().setUserSfxFullPath(sfxId, filePath);
            } else {
                // Find the entry using this ID.
                for (int i = 0; i < sbrFile.getSoundEffects().size(); i++) {
                    SfxEntry tempEntry = sbrFile.getSoundEffects().get(i);
                    if (tempEntry.getSfxId() == sfxId) {
                        sfxEntry = tempEntry;
                        break;
                    }
                }
            }

            // Didn't find an entry.
            if (sfxEntry == null) {
                if (deleteEntry)
                    continue;

                SfxEntryStreamAttributes newAttributes = new SfxEntryStreamAttributes(sbrFile);
                sfxEntry = new SfxEntry(sbrFile, sfxId, newAttributes);
                sbrFile.getSoundEffects().add(sfxEntry);
            } else if (deleteEntry) {
                if (!sfxEntry.remove())
                    logger.warning("Failed to remove SfxEntry named '%s'.", sfxEntry.getExportFileName());

                continue;
            }

            try {
                sfxEntry.applySettings(workingDirectory, arguments);
                arguments.warnAboutUnusedArguments(logger);
            } catch (Throwable th) {
                Utils.handleError(logger, th, false, "Failed to configure the sound '%s'.", filePath);
            }
        }
    }

    private static void copyResources(GreatQuestChunkedFile chunkedFile, ILogger logger, Config copyResourceCfg, List<ILateResourceResolver> lateResolvers) {
        if (copyResourceCfg == null)
            return;

        String sourceName = copyResourceCfg.getRootNode().getSectionName();
        List<Tuple2<kcCResource, byte[]>> queuedResources = new ArrayList<>();
        for (Config resourceList : copyResourceCfg.getChildConfigNodes()) {
            String sourceFilePath = resourceList.getSectionName();
            GreatQuestArchiveFile sourceFile = chunkedFile.getGameInstance().getMainArchive().getOptionalFileByPath(sourceFilePath);
            if (sourceFile == null) {
                logger.warning("Skipping resource copy for '%s' in %s, as the chunked file could not be found.", sourceFilePath, sourceName);
                continue;
            } else if (!(sourceFile instanceof GreatQuestChunkedFile)) {
                logger.warning("Skipping resource copy for '%s' in %s, as the specified file was not a chunk file! (%s)", sourceFilePath, sourceName, Utils.getSimpleName(sourceFile));
                continue;
            }

            GreatQuestChunkedFile sourceChunkFile = (GreatQuestChunkedFile) sourceFile;

            // Find resources.
            for (String resourceId : resourceList.getTextWithoutComments()) {
                kcCResource sourceResource = sourceChunkFile.getResourceByName(resourceId, null);
                if (sourceResource == null) {
                    logger.warning("Skipping resource copy for %s from %s in %s, as the resource was not found.", resourceId, sourceChunkFile.getFilePath(), sourceName);
                    continue;
                }

                // Save the resource data.
                byte[] rawData = sourceResource.writeDataToByteArray();

                // Firstly, create the new resources in the destination.
                kcCResource newResource = chunkedFile.getResourceByHash(sourceResource.getHash());
                if (newResource == null) {
                    newResource = chunkedFile.createResource(sourceResource.getChunkType(), rawData, sourceResource.getChunkIdentifier());
                    newResource.getSelfHash().setHash(sourceResource.getHash());
                    chunkedFile.addResource(newResource);
                }

                newResource.setName(sourceResource.getName(), sourceResource.isHashBasedOnName());
                if (sourceResource.getSelfHash().getOriginalString() != null)
                    newResource.getSelfHash().setOriginalString(sourceResource.getSelfHash().getOriginalString());

                // Check if this resource may need to have things resolved later.
                if (newResource instanceof ILateResourceResolver) {
                    lateResolvers.add((ILateResourceResolver) newResource);
                } else if (newResource instanceof kcCResourceGeneric && ((kcCResourceGeneric) newResource).getResourceData() instanceof ILateResourceResolver) {
                    lateResolvers.add((ILateResourceResolver) ((kcCResourceGeneric) newResource).getResourceData());
                }

                // Queue the resource to have its data loaded.
                queuedResources.add(new Tuple2<>(newResource, rawData));
            }
        }

        // Loading should occur after all resources have been created, so that referenced resources will resolve.
        for (Tuple2<kcCResource, byte[]> pair : queuedResources)
            pair.getA().loadFromRawBytes(pair.getB());
    }

    private static void deleteResources(GreatQuestChunkedFile chunkedFile, ILogger logger, Config deleteResourceCfg) {
        if (deleteResourceCfg == null)
            return;

        String sourceName = deleteResourceCfg.getRootNode().getSectionName();
        for (String resourceName : deleteResourceCfg.getTextWithoutComments()) {
            kcCResource resource = chunkedFile.getResourceByName(resourceName, null);
            if (resource == null) {
                // Don't warn since gqs scripts are often applied multiple times.
                // logger.warning("Skipping resource deletion for " + NumberUtils.toHexString(resourceHash) + "/'" + resourceName + "' in " + sourceName + ", as the chunked file could not be found.");
                continue;
            }

            try {
                chunkedFile.removeResource(resource);
            } catch (Throwable th) {
                Utils.handleError(logger, th, false, "Failed to remove resource %s/'%s' in %s.", resource.getHashAsHexString(), resourceName, sourceName);
            }
        }
    }

    private static String getCodeLocation(Config config, ConfigValueNode node) {
        return kcScript.getCodeLocation(node.getOriginalLineNumber(), config != null ? config.getRootNode().getSectionName() : null, true);
    }

    private static final String CONFIG_OPTION_TEXTURE_IMPORT = "Import";
    private static final String CONFIG_OPTION_TEXTURE_DELETE = "Delete";
    private static final String CONFIG_OPTION_TEXTURE_RESIZE = "Resize";
    private static void applyTextures(GreatQuestChunkedFile chunkedFile, ILogger logger, File workingDirectory, Config textureCfg) {
        if (textureCfg == null)
            return;

        GreatQuestAssetBinFile binFile = chunkedFile.getGameInstance().getMainArchive();
        for (ConfigValueNode node : textureCfg.getTextNodes()) {
            String textLine = node.getAsString();
            if (StringUtils.isNullOrWhiteSpace(textLine))
                continue;

            OptionalArguments arguments = OptionalArguments.parse(textLine);
            StringNode binFilePathNode = arguments.useNextIfPresent();
            if (binFilePathNode == null) {
                logger.warning("Skipping '%s'%s because it does not include a file path.", textLine, getCodeLocation(textureCfg, node));
                continue;
            }

            // Parse arguments.
            boolean shouldDelete = arguments.useFlag(CONFIG_OPTION_TEXTURE_DELETE);
            StringNode importPath = arguments.use(CONFIG_OPTION_TEXTURE_IMPORT);
            StringNode resizeDimensions = arguments.use(CONFIG_OPTION_TEXTURE_RESIZE);
            arguments.warnAboutUnusedArguments(logger);

            // Find file.
            String binFilePath = binFilePathNode.getAsString();
            String localChunkName = GreatQuestUtils.getFileNameFromPath(binFilePath);
            GreatQuestArchiveFile originalFileFoundByPath = binFile.getOptionalFileByPath(binFilePath);
            GreatQuestImageFile gqImageFile;
            if (originalFileFoundByPath != null) {
                if (!(originalFileFoundByPath instanceof GreatQuestImageFile)) {
                    logger.severe("Skipping texture '%s'%s because it wasn't actually a texture!", localChunkName, getCodeLocation(textureCfg, node));
                    continue;
                }

                gqImageFile = (GreatQuestImageFile) originalFileFoundByPath;
            } else if (importPath == null) {
                logger.severe("Skipping texture '%s'%s because it does not exist, and --%s was not included!", localChunkName, getCodeLocation(textureCfg, node), CONFIG_OPTION_TEXTURE_IMPORT);
                continue;
            } else {
                gqImageFile = new GreatQuestImageFile(chunkedFile.getGameInstance());
                gqImageFile.init(binFilePath, null);
            }

            // Attempt to delete the image file.
            if (shouldDelete) {
                if (originalFileFoundByPath != null) {
                    // Remove all references to this path.
                    kcCResourceTexture textureReference = chunkedFile.getResourceByName(localChunkName, kcCResourceTexture.class);
                    if (textureReference != null && binFilePath.equalsIgnoreCase(textureReference.getFullPath()))
                        chunkedFile.removeResource(textureReference);

                    // Remove from main archive.
                    chunkedFile.getMainArchive().removeFile(originalFileFoundByPath);
                }

                continue;
            }

            // Attempt to import the image.
            if (importPath != null) {
                String relativeFilePath = importPath.getAsString();
                File imageFile = new File(workingDirectory, relativeFilePath);
                if (!imageFile.exists() || !imageFile.isFile()) {
                    logger.severe("Skipping texture '%s'%s because '%s' did not exist/could not be imported!", localChunkName, getCodeLocation(textureCfg, node), relativeFilePath);
                    continue;
                }

                BufferedImage image = FileUtils.openImageFile(logger, imageFile);
                if (image == null)
                    continue;

                gqImageFile.setImage(image);
            }

            if (resizeDimensions != null) {
                String rawDimensions = resizeDimensions.getAsString();
                int newWidth = 0, newHeight = 0;
                int xPosition = rawDimensions.indexOf('x');
                if (xPosition > 0 && NumberUtils.isInteger(rawDimensions.substring(0, xPosition)))
                    newWidth = Integer.parseInt(rawDimensions.substring(0, xPosition));
                if (xPosition < rawDimensions.length() - 1 && NumberUtils.isInteger(rawDimensions.substring(xPosition + 1)))
                    newHeight = Integer.parseInt(rawDimensions.substring(xPosition + 1));

                if (newWidth <= 0 || newHeight <= 0) {
                    logger.severe("Skipping texture resize of '%s'%s because '%s' was not formatted properly!", localChunkName, getCodeLocation(textureCfg, node), rawDimensions);
                } else if (newWidth != gqImageFile.getWidth() || newHeight != gqImageFile.getHeight()) {
                    gqImageFile.setImage(ImageUtils.resizeImage(gqImageFile.getImage(), newWidth, newHeight, true));
                }
            }

            // Register image file, if it wasn't previously found.
            if (gqImageFile != originalFileFoundByPath)
                chunkedFile.getMainArchive().addFile(gqImageFile);

            // Create/update texture description.
            kcCResourceTexture textureRef = chunkedFile.getResourceByName(localChunkName, kcCResourceTexture.class);
            if (textureRef == null) {
                textureRef = new kcCResourceTexture(chunkedFile);
                textureRef.setName(localChunkName, true);
                chunkedFile.addResource(textureRef);
            }

            textureRef.setFullPath(binFilePath, false);
        }
    }

    private static void applyModelReferences(GreatQuestChunkedFile chunkedFile, ILogger logger, Config modelCfg) {
        if (modelCfg == null)
            return;

        String sourceName = modelCfg.getRootNode().getSectionName();
        for (String line : modelCfg.getTextWithoutComments()) {
            OptionalArguments arguments = OptionalArguments.parse(line);
            String filePath = arguments.useNext().getAsString();

            GreatQuestArchiveFile foundFile = chunkedFile.getGameInstance().getMainArchive().getOptionalFileByPath(filePath);
            if (foundFile == null) {
                logger.warning("Skipping model reference '%s' in %s, it could not be resolved.", filePath, sourceName);
                continue;
            } else if (!(foundFile instanceof kcModelWrapper)) {
                logger.warning("Skipping model reference '%s' in %s, it was not a 3D model.", filePath, sourceName);
                continue;
            }

            String fileName = foundFile.getFileName();
            kcCResourceModel modelRef = GreatQuestUtils.findLevelResourceByName(chunkedFile, fileName, kcCResourceModel.class);
            if (modelRef == null) {
                modelRef = new kcCResourceModel(chunkedFile);
                modelRef.setName(fileName, true);
                modelRef.setFullPath(filePath, true);
                chunkedFile.addResource(modelRef);
            }

            // Create/update the relevant modelDesc.
            StringNode modelDescNameNode = arguments.use(CONFIG_OPTION_CREATE_MODEL_DESC);
            if (modelDescNameNode != null) {
                String modelDescName = modelDescNameNode.getAsString();
                kcCResourceGeneric genericResource = GreatQuestUtils.findLevelResourceByName(chunkedFile, modelDescName, kcCResourceGeneric.class);
                if (genericResource == null) {
                    genericResource = new kcCResourceGeneric(chunkedFile);
                    genericResource.setName(modelDescName, true);
                    genericResource.setResourceData(new kcModelDesc(genericResource));
                    chunkedFile.addResource(genericResource);
                }

                kcModelDesc modelDesc = genericResource.getAsModelDescription();
                if (modelDesc == null)
                    throw new RuntimeException("Found a resource named '" + modelDescName + "', which was expected to be a entity description, but was actually a(n) " + genericResource.getResourceType() + ".");

                modelDesc.getModelRef().setResource(modelRef, false);
            }

            arguments.warnAboutUnusedArguments(logger);
        }
    }

    private static void applyCollisionProxies(GreatQuestChunkedFile chunkedFile, ILogger logger, Config collisionCfg) {
        if (collisionCfg == null)
            return;

        for (Config collisionProxyDescCfg : collisionCfg.getChildConfigNodes()) {
            String collisionProxyDescName = collisionProxyDescCfg.getSectionName();
            kcCResourceGeneric collisionDesc = GreatQuestUtils.findLevelResourceByName(chunkedFile, collisionProxyDescName, kcCResourceGeneric.class);
            if (collisionDesc == null) {
                kcProxyDescType descType = collisionProxyDescCfg.getKeyValueNodeOrError(kcProxyDesc.CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcProxyDescType.class);

                collisionDesc = new kcCResourceGeneric(chunkedFile);
                collisionDesc.setName(collisionProxyDescName, true);
                collisionDesc.setResourceData(descType.createNewInstance(collisionDesc));
                chunkedFile.addResource(collisionDesc);
            }

            kcProxyDesc proxyDesc = collisionDesc.getAsProxyDescription();
            if (proxyDesc == null)
                throw new RuntimeException("Found a resource named '" + collisionProxyDescName + "', which was expected to be a entity description, but was actually a(n) " + collisionDesc.getResourceType() + ".");

            proxyDesc.fromConfig(logger, collisionProxyDescCfg);
        }
    }

    private static void updateAnimationSets(GreatQuestChunkedFile chunkedFile, ILogger logger, Config resourceHashTableCfg) {
        if (resourceHashTableCfg == null)
            return;

        for (Config animationSetCfg : resourceHashTableCfg.getChildConfigNodes()) {
            String animationSetName = animationSetCfg.getSectionName();
            kcCResourceAnimSet animationSet = GreatQuestUtils.findLevelResourceByName(chunkedFile, animationSetName, kcCResourceAnimSet.class);

            // Try to find the animation set by auto-adding the prefix.
            if (animationSet == null && !animationSetName.endsWith(kcCResourceAnimSet.NAME_SUFFIX)) {
                animationSetName += kcCResourceAnimSet.NAME_SUFFIX;
                animationSet = GreatQuestUtils.findLevelResourceByName(chunkedFile, animationSetName, kcCResourceAnimSet.class);
            }

            // Create a new animation set.
            if (animationSet == null) {
                animationSet = new kcCResourceAnimSet(chunkedFile);
                animationSet.setName(animationSetName, true);
                chunkedFile.addResource(animationSet);
            }

            // Add the animations.
            for (String animationName : animationSetCfg.getTextWithoutComments()) {
                kcCResourceTrack animation = GreatQuestUtils.findLevelResourceByName(chunkedFile, animationName, kcCResourceTrack.class);
                if (animation == null) {
                    logger.warning("Could not find animation named '%s'.", animationName);
                    continue;
                }

                if (!animationSet.contains(animation) && !animationSet.addAnimation(animation))
                    logger.warning("Failed to add animation '%s' to '%s'.", animation.getName(), animationSet.getName());
            }
        }
    }

    private static List<kcCResourceNamedHash> applyActionSequences(GreatQuestChunkedFile chunkedFile, ILogger logger, Config resourceHashTableCfg) {
        if (resourceHashTableCfg == null)
            return Collections.emptyList();

        List<kcCResourceNamedHash> entitySequences = new ArrayList<>();
        for (Config hashTableCfg : resourceHashTableCfg.getChildConfigNodes()) {
            String hashTableName = hashTableCfg.getSectionName() + kcCResourceNamedHash.NAME_SUFFIX;

            kcCResourceNamedHash namedHashTable = GreatQuestUtils.findLevelResourceByName(chunkedFile, hashTableName, kcCResourceNamedHash.class);
            if (namedHashTable == null) {
                namedHashTable = new kcCResourceNamedHash(chunkedFile);
                namedHashTable.setName(hashTableName, true);
                chunkedFile.addResource(namedHashTable);
            }

            namedHashTable.addSequencesFromConfigNode(hashTableCfg.getSectionName(), hashTableCfg, logger);
            if (!entitySequences.contains(namedHashTable))
                entitySequences.add(namedHashTable);
        }

        return entitySequences;
    }

    private static void applyEntityDescriptions(GreatQuestChunkedFile chunkedFile, ILogger logger, Config entityDescriptionsCfg, List<ILateResourceResolver> lateResolvers) {
        if (entityDescriptionsCfg == null)
            return;

        for (Config entityDescCfg : entityDescriptionsCfg.getChildConfigNodes()) {
            String entityDescName = entityDescCfg.getSectionName();
            kcCResourceGeneric generic = GreatQuestUtils.findLevelResourceByName(chunkedFile, entityDescName, kcCResourceGeneric.class);
            if (generic == null) {
                kcEntityDescType descType = entityDescCfg.getKeyValueNodeOrError(kcEntity3DDesc.CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcEntityDescType.class);

                generic = new kcCResourceGeneric(chunkedFile);
                generic.setName(entityDescName, true);
                generic.setResourceData(descType.createNewInstance(generic));
                chunkedFile.addResource(generic);
            }

            kcEntity3DDesc entityDesc = generic.getAsEntityDescription();
            if (entityDesc == null)
                throw new RuntimeException("Found a resource named '" + entityDescName + "', which was expected to be a entity description, but was actually a(n) " + generic.getResourceType() + ".");

            entityDesc.fromConfig(logger, entityDescCfg);

            // If the entity description has things to resolve later, add it. (Waypoints, actor base descriptions)
            if (entityDesc instanceof ILateResourceResolver)
                lateResolvers.add((ILateResourceResolver) entityDesc);
        }
    }

    private static void applyEntityInstances(GreatQuestChunkedFile chunkedFile, ILogger logger, Config entityCfg, kcScriptList scriptList) {
        if (entityCfg == null)
            return;

        // Add all entities first, so it becomes possible to reference other entities defined together.
        String sourceName = entityCfg.getRootNode().getSectionName();
        for (Config entityInstanceCfg : entityCfg.getChildConfigNodes()) {
            String entityInstName = entityInstanceCfg.getSectionName();
            kcCResourceEntityInst entity = chunkedFile.getResourceByName(entityInstName, kcCResourceEntityInst.class);
            if (entity == null) {
                entity = new kcCResourceEntityInst(chunkedFile);
                entity.setName(entityInstName, true);
                entity.setInstance(new kcEntity3DInst(entity));
                chunkedFile.addResource(entity);
            }
        }

        // Load entity data.
        Map<kcEntityInst, Config> scriptCfgsPerEntity = new HashMap<>();
        for (Config entityInstanceCfg : entityCfg.getChildConfigNodes()) {
            String entityInstName = entityInstanceCfg.getSectionName();
            kcCResourceEntityInst entity = chunkedFile.getResourceByName(entityInstName, kcCResourceEntityInst.class);
            if (entity == null)
                throw new RuntimeException("Could not find an entity named '" + entityInstName + "' to load data for.");

            kcEntityInst entityInst = entity.fromConfig(logger, entityInstanceCfg);

            // Scripts should load AFTER core entity data.
            Config scriptCfg = entityInstanceCfg.getChildConfigByName(kcEntityInst.CONFIG_SECTION_SCRIPT);
            if (scriptCfg != null)
                scriptCfgsPerEntity.put(entityInst, scriptCfg);
        }

        // Scripts are loaded last in order to ensure entity data is correct. (Prevents incorrect resolutions and warnings.)
        for (Entry<kcEntityInst, Config> entry : scriptCfgsPerEntity.entrySet())
            entry.getKey().addScriptFunctions(logger, scriptList, entry.getValue(), sourceName, true, false);
    }

    private static void applyScripts(GreatQuestChunkedFile chunkedFile, ILogger logger, Config scriptCfg, kcScriptList scriptList) {
        if (scriptCfg == null)
            return;

        String sourceName = scriptCfg.getRootNode().getSectionName();
        for (Config entityScriptCfg : scriptCfg.getChildConfigNodes()) {
            String[] entityNames = entityScriptCfg.getSectionName().split("\\|");
            for (String entityInstName : entityNames) { // Allows multiple entities to be assigned by splitting with the pipe character. I originally wanted comma, but some entity names have commas in them.
                kcCResourceEntityInst entity = chunkedFile.getResourceByName(entityInstName, kcCResourceEntityInst.class);
                if (entity == null)
                    throw new RuntimeException("Couldn't resolve entity named '" + entityInstName + "' to make script modifications to.");

                kcEntityInst entityInst = entity.getInstance();
                if (entityInst == null)
                    throw new RuntimeException("The entity instance for '" + entityInstName + "' was null, so we couldn't modify its script.");

                entityInst.addScriptFunctions(logger, scriptList, entityScriptCfg, sourceName, false, entityNames.length > 1);
            }
        }
    }

    @SuppressWarnings("ExtractMethodRecommender")
    private static void warnAboutUnusedSequences(GreatQuestChunkedFile chunkedFile, ILogger logger, List<kcCResourceNamedHash> sequenceTables) {
        if (sequenceTables == null || sequenceTables.isEmpty())
            return;

        // Find all sequence tables actually used.
        Set<kcCResourceNamedHash> usedSequenceTables = new HashSet<>();
        for (kcCResource resource : chunkedFile.getChunks()) {
            if (!(resource instanceof kcCResourceGeneric))
                continue;

            kcEntity3DDesc entityDesc = ((kcCResourceGeneric) resource).getAsEntityDescription();
            if (!(entityDesc instanceof kcActorBaseDesc))
                continue;

            kcCResourceNamedHash sequenceTable = ((kcActorBaseDesc) entityDesc).getAnimationSequences();
            if (sequenceTable != null)
                usedSequenceTables.add(sequenceTable);
        }

        // Test each sequence table.
        for (int i = 0; i < sequenceTables.size(); i++) {
            kcCResourceNamedHash namedHash = sequenceTables.get(i);
            if (!usedSequenceTables.contains(namedHash))
                logger.warning("The action sequence table '%s' is never used. This may indicate a typo in the sequence definition.", namedHash.getName());
        }
    }
}
