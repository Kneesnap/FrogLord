package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxAttributes;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntry;
import net.highwayfrogs.editor.games.konami.greatquest.audio.SBRFile.SfxEntryStreamAttributes;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityDescType;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestArchiveFile;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelDesc;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contains static utility functions which make exporting/importing Frogger: The Great Quest assets easier.
 * Created by Kneesnap on 11/2/2024.
 */
public class GreatQuestAssetUtils {
    private static final String CONFIG_SECTION_MODELS = "Models";
    private static final String CONFIG_SECTION_SOUND_EFFECTS = "SoundEffects";
    private static final String CONFIG_SECTION_DIALOG = "Dialog";
    private static final String CONFIG_SECTION_ENTITY_DESCRIPTIONS = "EntityDescriptions";
    private static final String CONFIG_SECTION_ENTITIES = "Entities";
    private static final String CONFIG_SECTION_SCRIPTS = "Scripts";
    private static final String CONFIG_OPTION_CREATE_MODEL_DESC = "CreateModelDesc";

    /**
     * Reads a great quest script group from a config, and applies it to the chunked file.
     * A section named 'Dialog' can contain key-value pairs of dialog strings to add/replace.
     * A section named 'Entities' can contain entity definitions which should be added/set.
     * A section named 'Scripts' can contain entity script definitions.
     * @param chunkedFile the file to apply the script group to
     * @param gqsScriptGroup the script group config to apply
     */
    public static void applyGqsScriptGroup(GreatQuestChunkedFile chunkedFile, Config gqsScriptGroup) {
        if (chunkedFile == null)
            throw new NullPointerException("chunkedFile");
        if (gqsScriptGroup == null)
            throw new NullPointerException("gqsScriptGroup");

        String sourceName = gqsScriptGroup.getSectionName();
        kcScriptList scriptList = chunkedFile.getScriptList();
        if (scriptList == null)
            throw new RuntimeException(chunkedFile.getDebugName() + " does not have any script data, so we cannot apply " + sourceName + "!");

        // Add or replace dialog.
        Config dialogCfg = gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_DIALOG);
        if (dialogCfg != null) {
            for (Entry<String, ConfigValueNode> entry : dialogCfg.getKeyValuePairs().entrySet()) {
                String dialogResName = entry.getKey();
                int dialogResHash = GreatQuestUtils.hash(dialogResName);

                // Get or replace generic resource.
                kcCResourceGeneric generic = chunkedFile.getResourceByHash(dialogResHash);
                if (generic == null) {
                    generic = new kcCResourceGeneric(chunkedFile, kcCResourceGenericType.STRING_RESOURCE);
                    generic.setName(dialogResName, true);
                    chunkedFile.addResource(generic);
                }

                generic.getAsStringResource().setValue(entry.getValue().getAsString());
            }
        }

        // Add or replace sound effects.
        Config soundEffectsCfg = gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_SOUND_EFFECTS);
        if (soundEffectsCfg != null) {
            SBRFile sbrFile = chunkedFile.getSoundBankFile();
            if (sbrFile == null) {
                chunkedFile.getLogger().warning("Skipping sound file references, as the sound bank file could not be resolved.");
            } else {
                for (String line : soundEffectsCfg.getTextWithoutComments()) {
                    OptionalArguments arguments = OptionalArguments.parse(line);
                    String filePath = arguments.useNext().getAsString();
                    int sfxId = chunkedFile.getGameInstance().getSfxIdFromFullSoundPath(filePath);
                    if (sfxId < 0) {
                        chunkedFile.getLogger().warning("Skipping sound file reference '" + filePath + "', it could not be resolved.");
                        continue;
                    }

                    SfxEntry sfxEntry = null;
                    for (int i = 0; i < sbrFile.getSoundEffects().size(); i++) {
                        SfxEntry tempEntry = sbrFile.getSoundEffects().get(i);
                        if (tempEntry.getSfxId() == sfxId) {
                            sfxEntry = tempEntry;
                            break;
                        }
                    }

                    // Didn't find an entry.
                    if (sfxEntry == null) {
                        SfxEntryStreamAttributes newAttributes = new SfxEntryStreamAttributes(sbrFile);
                        sfxEntry = new SfxEntry(sbrFile, sfxId, newAttributes);
                        sbrFile.getSoundEffects().add(sfxEntry);
                    }

                    SfxAttributes attributes = sfxEntry.getAttributes();
                    attributes.setFlagState(SfxAttributes.FLAG_REPEAT, arguments.useFlag(SfxAttributes.FLAG_NAME_REPEAT));
                    attributes.setFlagState(SfxAttributes.FLAG_VOICE_CLIP, arguments.useFlag(SfxAttributes.FLAG_NAME_VOICE_CLIP));
                    attributes.setFlagState(SfxAttributes.FLAG_MUSIC, arguments.useFlag(SfxAttributes.FLAG_NAME_MUSIC));
                    StringNode priorityNode = arguments.use(SfxAttributes.FLAG_NAME_PRIORITY);
                    if (priorityNode != null)
                        attributes.setPriority(priorityNode.getAsInteger());

                    arguments.warnAboutUnusedArguments(chunkedFile.getLogger());
                }
            }
        }

        // Add missing 3D model references.
        Config modelCfg = gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_MODELS);
        if (modelCfg != null) {
            for (String line : modelCfg.getTextWithoutComments()) {
                OptionalArguments arguments = OptionalArguments.parse(line);
                String filePath = arguments.useNext().getAsString();

                GreatQuestArchiveFile foundFile = chunkedFile.getGameInstance().getMainArchive().getOptionalFileByName(filePath);
                if (foundFile == null) {
                    chunkedFile.getLogger().warning("Skipping model reference '" + filePath + "', it could not be resolved.");
                    continue;
                } else if (!(foundFile instanceof kcModelWrapper)) {
                    chunkedFile.getLogger().warning("Skipping model reference '" + filePath + "', it was not a 3D model.");
                    continue;
                }

                String fileName = foundFile.getFileName();
                kcCResourceModel modelRef = chunkedFile.getResourceByHash(GreatQuestUtils.hash(fileName));
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
                    kcCResourceGeneric genericResource = chunkedFile.getResourceByHash(GreatQuestUtils.hash(modelDescName));
                    if (genericResource == null) {
                        genericResource = new kcCResourceGeneric(chunkedFile);
                        genericResource.setName(modelDescName, true);
                        genericResource.setResourceData(new kcModelDesc(genericResource));
                        chunkedFile.addResource(genericResource);
                    }

                    genericResource.getAsModelDescription().getModelRef().setResource(modelRef, false);
                }

                arguments.warnAboutUnusedArguments(chunkedFile.getLogger());
            }
        }

        // Add/replace entity descriptions.
        Config entityDescriptionsCfg = gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ENTITY_DESCRIPTIONS);
        if (entityDescriptionsCfg != null) {
            for (Config entityDescCfg : entityDescriptionsCfg.getChildConfigNodes()) {
                String entityDescName = entityDescCfg.getSectionName();
                int entityDescNameHash = GreatQuestUtils.hash(entityDescName);
                kcCResourceGeneric entityDesc = chunkedFile.getResourceByHash(entityDescNameHash);
                if (entityDesc == null) {
                    kcEntityDescType descType = entityDescCfg.getKeyValueNodeOrError(kcEntity3DDesc.CONFIG_KEY_DESC_TYPE).getAsEnumOrError(kcEntityDescType.class);

                    entityDesc = new kcCResourceGeneric(chunkedFile);
                    entityDesc.setName(entityDescName, true);
                    entityDesc.setResourceData(descType.createNewInstance(entityDesc));
                    chunkedFile.addResource(entityDesc);
                }

                entityDesc.getAsEntityDescription().fromConfig(entityDescCfg);
            }
        }

        // Add/replace entities.
        Config entityCfg = gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_ENTITIES);
        if (entityCfg != null) {
            // Add all entities first, so it becomes possible to reference other entities defined together.
            for (Config entityInstanceCfg : entityCfg.getChildConfigNodes()) {
                String entityInstName = entityInstanceCfg.getSectionName();
                int entityInstNameHash = GreatQuestUtils.hash(entityInstName);
                kcCResourceEntityInst entity = chunkedFile.getResourceByHash(entityInstNameHash);
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
                int entityInstNameHash = GreatQuestUtils.hash(entityInstName);
                kcCResourceEntityInst entity = chunkedFile.getResourceByHash(entityInstNameHash);
                if (entity == null)
                    throw new RuntimeException("Could not find an entity named '" + entityInstName + "' to load data for.");

                kcEntityInst entityInst = entity.getInstance();
                if (entityInst == null)
                    throw new RuntimeException("The entity instance for '" + entityInstName + "' was null, so we couldn't modify its script.");

                entityInst.fromConfig(entityInstanceCfg);

                // Scripts should load AFTER core entity data.
                Config scriptCfg = entityInstanceCfg.getChildConfigByName(kcEntityInst.CONFIG_SECTION_SCRIPT);
                if (scriptCfg != null)
                    scriptCfgsPerEntity.put(entityInst, scriptCfg);
            }

            // Scripts are loaded last in order to ensure entity data is correct. (Prevents incorrect resolutions and warnings.)
            for (Entry<kcEntityInst, Config> entry : scriptCfgsPerEntity.entrySet())
                entry.getKey().addScriptFunctions(scriptList, entry.getValue(), sourceName, true);
        }

        // Add/replace scripts.
        Config scriptCfg = gqsScriptGroup.getChildConfigByName(CONFIG_SECTION_SCRIPTS);
        if (scriptCfg != null) {
            for (Config entityScriptCfg : scriptCfg.getChildConfigNodes()) {
                for (String entityInstName : entityScriptCfg.getSectionName().split("\\|")) { // Allows multiple entities to be assigned by splitting with the pipe character. I originally wanted comma, but some entity names have commas in them.
                    int entityInstNameHash = GreatQuestUtils.hash(entityInstName);
                    kcCResourceEntityInst entity = chunkedFile.getResourceByHash(entityInstNameHash);
                    if (entity == null)
                        throw new RuntimeException("Couldn't resolve entity named '" + entityInstName + "' to make script modifications to.");

                    kcEntityInst entityInst = entity.getInstance();
                    if (entityInst == null)
                        throw new RuntimeException("The entity instance for '" + entityInstName + "' was null, so we couldn't modify its script.");

                    entityInst.addScriptFunctions(scriptList, entityScriptCfg, sourceName, false);
                }
            }
        }
    }
}
