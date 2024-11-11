package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityDescType;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;

import java.util.Map.Entry;

/**
 * Contains static utility functions which make exporting/importing Frogger: The Great Quest assets easier.
 * Created by Kneesnap on 11/2/2024.
 */
public class GreatQuestAssetUtils {
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

        kcScriptList scriptList = chunkedFile.getScriptList();
        if (scriptList == null)
            throw new RuntimeException(chunkedFile.getDebugName() + " does not have any script data, so we cannot apply " + gqsScriptGroup.getSectionName() + "!");

        // Add or replace dialog.
        Config dialogCfg = gqsScriptGroup.getChildConfigByName("Dialog");
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

        // Add/replace entity descriptions.
        Config entityDescriptionsCfg = gqsScriptGroup.getChildConfigByName("EntityDescriptions");
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
        Config entityCfg = gqsScriptGroup.getChildConfigByName("Entities");
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
            }
        }

        // Add/replace scripts.
        Config scriptCfg = gqsScriptGroup.getChildConfigByName("Scripts");
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

                    entityInst.addScriptFunctions(scriptList, entityScriptCfg, gqsScriptGroup.getSectionName());
                }
            }
        }
    }
}
