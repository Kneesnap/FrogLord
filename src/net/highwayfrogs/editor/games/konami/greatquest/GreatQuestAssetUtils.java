package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntry;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntryStreamAttributes;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.entity.*;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDescType;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.system.Tuple2;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.writer.ArrayReceiver;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.MessageTrackingLogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

/**
 * Contains static utility functions which make exporting/importing Frogger: The Great Quest assets easier.
 * Created by Kneesnap on 11/2/2024.
 */
public class GreatQuestAssetUtils {
    private static final String CONFIG_SECTION_MODELS = "Models";
    private static final String CONFIG_SECTION_SOUND_EFFECTS = "SoundEffects";
    private static final String CONFIG_SECTION_COPY_RESOURCES = "CopyResources";
    private static final String CONFIG_SECTION_DELETE_RESOURCES = "DeleteResources";
    private static final String CONFIG_SECTION_ANIMATIONS = "Animations";
    private static final String CONFIG_SECTION_ACTION_SEQUENCES = "Sequences";
    private static final String CONFIG_SECTION_DIALOG = "Dialog";
    private static final String CONFIG_SECTION_COLLISION_PROXIES = "Collision";
    private static final String CONFIG_SECTION_ENTITY_DESCRIPTIONS = "EntityDescriptions";
    private static final String CONFIG_SECTION_ENTITIES = "Entities";
    private static final String CONFIG_SECTION_SCRIPTS = "Scripts";
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

        applyStringResources(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_DIALOG));

        // Should apply before scripts/entities.
        applySoundEffects(workingDirectory, chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_SOUND_EFFECTS), logger);

        // Should occur before resource copying, so that any resources can resolve the model/collision references.
        applyModelReferences(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_MODELS), logger);
        applyCollisionProxies(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_COLLISION_PROXIES));
        copyResources(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_COPY_RESOURCES), logger);
        deleteResources(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_DELETE_RESOURCES), logger);
        updateAnimationSets(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ANIMATIONS), logger);
        applyActionSequences(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ACTION_SEQUENCES), logger);

        // This should occur after resource copying to ensure it can resolve resources. Copied resources shouldn't reference entity descriptions since entity instances (a resource which is not expected to be copied) are the only resource to resolve entity descriptions.
        // This should also happen before entity instances are applied.
        List<kcWaypointDesc> waypoints = applyEntityDescriptions(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ENTITY_DESCRIPTIONS));

        // Run before scripts, but after entity descriptions.
        applyEntityInstances(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ENTITIES), scriptList, logger);
        applyScripts(chunkedFile, gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_SCRIPTS), scriptList, logger);

        // Finish resolving waypoint descriptions. (Happens after entities are created, so the next/prev entities can be resolved successfully.
        if (waypoints != null)
            waypoints.forEach(kcWaypointDesc::resolvePendingWaypointEntities);

        // Print advanced warnings after everything is complete.
        scriptList.printAdvancedWarnings(logger);
        gqsScriptGroup.recursivelyWarnAboutUnusedData(logger);

        // Show popup.
        logger.showPopup("successfully", "Imported '%s' [problem=with ][summary].", sourceName);
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

    private static void applySoundEffects(File workingDirectory, GreatQuestChunkedFile chunkedFile, Config soundEffectsCfg, ILogger logger) {
        if (soundEffectsCfg == null)
            return;

        String sourceName = soundEffectsCfg.getRootNode().getSectionName();
        SBRFile sbrFile = chunkedFile.getSoundBankFile();
        if (sbrFile == null) {
            logger.warning("Skipping sound file references in %s, as the sound bank file could not be resolved.", sourceName);
        } else {
            for (String line : soundEffectsCfg.getTextWithoutComments()) {
                OptionalArguments arguments = OptionalArguments.parse(line);
                String filePath = arguments.useNext().getAsString();
                int sfxId = chunkedFile.getGameInstance().getSfxIdFromFullSoundPath(filePath);

                SfxEntry sfxEntry = null;
                if (sfxId < 0) {
                    if (!arguments.has(SfxEntry.FLAG_NAME_IMPORT)) { // If we're importing, then we create a new sound instead!
                        logger.warning("Skipping sound file reference '%s' in %s, it could not be resolved.", filePath, sourceName);
                        continue;
                    }

                    // Continue by building a new entry.
                    sfxId = chunkedFile.getGameInstance().useNextFreeSoundIdSlot();
                    chunkedFile.getGameInstance().getModData().setUserSfxFullPath(sfxId, filePath);
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
                    SfxEntryStreamAttributes newAttributes = new SfxEntryStreamAttributes(sbrFile);
                    sfxEntry = new SfxEntry(sbrFile, sfxId, newAttributes);
                    sbrFile.getSoundEffects().add(sfxEntry);
                }

                try {
                    sfxEntry.applySettings(workingDirectory, arguments);
                    arguments.warnAboutUnusedArguments(logger);
                } catch (Throwable th) {
                    Utils.handleError(logger, th, false, "Failed to configure the sound '%s'.", filePath);
                }
            }
        }
    }

    private static void copyResources(GreatQuestChunkedFile chunkedFile, Config copyResourceCfg, ILogger logger) {
        if (copyResourceCfg == null)
            return;

        String sourceName = copyResourceCfg.getRootNode().getSectionName();
        List<Tuple2<kcCResource, byte[]>> queuedResources = new ArrayList<>();
        for (Config resourceList : copyResourceCfg.getChildConfigNodes()) {
            String sourceFilePath = resourceList.getSectionName();
            GreatQuestArchiveFile sourceFile = chunkedFile.getGameInstance().getMainArchive().getOptionalFileByName(sourceFilePath);
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
                ArrayReceiver receiver = new ArrayReceiver();
                DataWriter writer = new DataWriter(receiver);
                sourceResource.save(writer);
                writer.closeReceiver();
                byte[] rawData = receiver.toArray();

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

                // Queue the resource to have its data loaded.
                queuedResources.add(new Tuple2<>(newResource, rawData));
            }
        }

        // Loading should occur after all resources have been created, so that referenced resources will resolve.
        for (Tuple2<kcCResource, byte[]> pair : queuedResources)
            pair.getA().loadFromRawBytes(pair.getB());
    }

    private static void deleteResources(GreatQuestChunkedFile chunkedFile, Config deleteResourceCfg, ILogger logger) {
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


    private static void applyModelReferences(GreatQuestChunkedFile chunkedFile, Config modelCfg, ILogger logger) {
        if (modelCfg == null)
            return;

        String sourceName = modelCfg.getRootNode().getSectionName();
        for (String line : modelCfg.getTextWithoutComments()) {
            OptionalArguments arguments = OptionalArguments.parse(line);
            String filePath = arguments.useNext().getAsString();

            GreatQuestArchiveFile foundFile = chunkedFile.getGameInstance().getMainArchive().getOptionalFileByName(filePath);
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

    private static void applyCollisionProxies(GreatQuestChunkedFile chunkedFile, Config collisionCfg) {
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

            proxyDesc.fromConfig(collisionProxyDescCfg);
        }
    }

    private static void updateAnimationSets(GreatQuestChunkedFile chunkedFile, Config resourceHashTableCfg, ILogger logger) {
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

    private static void applyActionSequences(GreatQuestChunkedFile chunkedFile, Config resourceHashTableCfg, ILogger logger) {
        if (resourceHashTableCfg == null)
            return;

        for (Config hashTableCfg : resourceHashTableCfg.getChildConfigNodes()) {
            String hashTableName = hashTableCfg.getSectionName() + kcCResourceNamedHash.NAME_SUFFIX;

            kcCResourceNamedHash namedHashTable = GreatQuestUtils.findLevelResourceByName(chunkedFile, hashTableName, kcCResourceNamedHash.class);
            if (namedHashTable == null) {
                namedHashTable = new kcCResourceNamedHash(chunkedFile);
                namedHashTable.setName(hashTableName, true);
                chunkedFile.addResource(namedHashTable);
            }

            namedHashTable.addSequencesFromConfigNode(hashTableCfg, logger);
        }
    }

    private static List<kcWaypointDesc> applyEntityDescriptions(GreatQuestChunkedFile chunkedFile, Config entityDescriptionsCfg) {
        if (entityDescriptionsCfg == null)
            return Collections.emptyList();

        List<kcWaypointDesc> waypoints = new ArrayList<>();
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

            entityDesc.fromConfig(entityDescCfg);

            // Return waypoints.
            if (entityDesc instanceof kcWaypointDesc)
                waypoints.add((kcWaypointDesc) entityDesc);
        }

        return waypoints;
    }

    private static void applyEntityInstances(GreatQuestChunkedFile chunkedFile, Config entityCfg, kcScriptList scriptList, ILogger logger) {
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

            kcEntityInst entityInst = entity.fromConfig(entityInstanceCfg);

            // Scripts should load AFTER core entity data.
            Config scriptCfg = entityInstanceCfg.getChildConfigByName(kcEntityInst.CONFIG_SECTION_SCRIPT);
            if (scriptCfg != null)
                scriptCfgsPerEntity.put(entityInst, scriptCfg);
        }

        // Scripts are loaded last in order to ensure entity data is correct. (Prevents incorrect resolutions and warnings.)
        for (Entry<kcEntityInst, Config> entry : scriptCfgsPerEntity.entrySet())
            entry.getKey().addScriptFunctions(logger, scriptList, entry.getValue(), sourceName, true);
    }

    private static void applyScripts(GreatQuestChunkedFile chunkedFile, Config scriptCfg, kcScriptList scriptList, ILogger logger) {
        if (scriptCfg == null)
            return;

        String sourceName = scriptCfg.getRootNode().getSectionName();
        for (Config entityScriptCfg : scriptCfg.getChildConfigNodes()) {
            for (String entityInstName : entityScriptCfg.getSectionName().split("\\|")) { // Allows multiple entities to be assigned by splitting with the pipe character. I originally wanted comma, but some entity names have commas in them.
                kcCResourceEntityInst entity = chunkedFile.getResourceByName(entityInstName, kcCResourceEntityInst.class);
                if (entity == null)
                    throw new RuntimeException("Couldn't resolve entity named '" + entityInstName + "' to make script modifications to.");

                kcEntityInst entityInst = entity.getInstance();
                if (entityInst == null)
                    throw new RuntimeException("The entity instance for '" + entityInstName + "' was null, so we couldn't modify its script.");

                entityInst.addScriptFunctions(logger, scriptList, entityScriptCfg, sourceName, false);
            }
        }
    }
}
