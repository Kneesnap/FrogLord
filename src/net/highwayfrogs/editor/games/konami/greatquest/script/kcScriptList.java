package net.highwayfrogs.editor.games.konami.greatquest.script;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestAssetUtils;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcInterimScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptListInterim;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptTOC;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.PropertyListViewerComponent.PropertyList;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a list of scripts.
 * The logic has been determined by reading kcCScriptMgr::Load() from the PS2 PAL version.
 * Created by Kneesnap on 6/26/2023.
 */
public class kcScriptList extends kcCResource {
    @Getter private final List<kcScript> scripts = new ArrayList<>();
    private transient kcScriptListInterim interim;

    public static final String GLOBAL_SCRIPT_NAME = "scriptdata";
    public static final int GLOBAL_SCRIPT_NAME_HASH = GreatQuestUtils.hash(GLOBAL_SCRIPT_NAME);
    private static final String SCRIPT_FILE_PATH_KEY = "scriptFilePath";
    private static final SavedFilePath SCRIPT_EXPORT_PATH = new SavedFilePath(SCRIPT_FILE_PATH_KEY, "Select the directory to export scripts to");
    private static final SavedFilePath SCRIPT_IMPORT_PATH = new SavedFilePath(SCRIPT_FILE_PATH_KEY, "Select the directory to import scripts from");
    private static final SavedFilePath SCRIPT_GROUP_IMPORT_PATH = new SavedFilePath("gqsScriptFilePath", "Select the script group to import", kcScript.GQS_GROUP_FILE_TYPE);

    public kcScriptList(GreatQuestChunkedFile parentFile) {
        super(parentFile, KCResourceID.RAW);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);

        // Read interim list.
        this.interim = new kcScriptListInterim(getParentFile());
        this.interim.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        loadScriptsFromInterim();
        super.save(writer);

        // Convert scripts to interim data.
        List<kcScriptTOC> entries = new ArrayList<>();
        List<Integer> statusData = new ArrayList<>();
        List<kcInterimScriptEffect> effects = new ArrayList<>();
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);

            // Create new entry.
            kcScriptTOC newEntry = new kcScriptTOC(script.calculateCauseTypes(), statusData.size(), script.getFunctions().size(), script.getEffectCount());
            entries.add(newEntry);

            // Add 'cause' & 'effect' data.
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);
                function.saveCauseData(statusData, effects.size() * kcInterimScriptEffect.SIZE_IN_BYTES);
                for (int k = 0; k < function.getEffects().size(); k++)
                    effects.add(function.getEffects().get(k).toInterimScriptEffect());
            }
        }

        // Write interim data.
        kcScriptListInterim interim = new kcScriptListInterim(getParentFile(), entries, statusData, effects);
        interim.save(writer);
    }

    /**
     * Loads the scripts from their interim format.
     * This should be called after the other chunks in the file, so that we can identify which scripts belong to which entities.
     */
    public void loadScriptsFromInterim() {
        if (this.interim == null)
            return;

        Map<Integer, kcCResourceEntityInst> entityScriptMapping = new HashMap<>();
        for (kcCResource testChunk : getParentFile().getChunks()) {
            if (!(testChunk instanceof kcCResourceEntityInst))
                continue;

            kcCResourceEntityInst resourceEntity = (kcCResourceEntityInst) testChunk;
            if (resourceEntity.getInstance() == null)
                continue;

            int scriptIndex = resourceEntity.getInstance().getScriptIndex();
            if (scriptIndex >= 0) {
                kcCResourceEntityInst oldEntity = entityScriptMapping.put(scriptIndex, resourceEntity);
                if (oldEntity != null)
                    throw new RuntimeException("There was more than one entity ('" + oldEntity.getName() + "' & '" + resourceEntity.getName() + "') attached to script index " + scriptIndex + "!");
            }
        }

        // Convert interim list to script list.
        this.scripts.clear();
        for (int i = 0; i < this.interim.getEntries().size(); i++)
            this.scripts.add(kcScript.loadScript(this.interim, this, entityScriptMapping.remove(i), this.interim.getEntries().get(i)));

        this.interim = null;
    }

    @Override
    public Image getCollectionViewIcon() {
        return ImageResource.WIN98_TERMINAL_16.getFxImage();
    }

    @Override
    public PropertyList addToPropertyList(PropertyList propertyList) {
        propertyList = super.addToPropertyList(propertyList);
        propertyList.add("Scripts", this.scripts.size());
        return propertyList;
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        MenuItem exportScriptsItem = new MenuItem("Export Scripts");
        contextMenu.getItems().add(exportScriptsItem);
        exportScriptsItem.setOnAction(event -> {
            File scriptFolder = FileUtils.askUserToSelectFolder(getGameInstance(), SCRIPT_EXPORT_PATH);
            if (scriptFolder == null)
                return;

            int functions = 0;
            kcScriptDisplaySettings settings = getParentFile().createScriptDisplaySettings();
            for (int i = 0; i < this.scripts.size(); i++) {
                kcScript script = this.scripts.get(i);
                Config rootNode = script.toConfigNode(settings);
                rootNode.saveTextFile(new File(scriptFolder, rootNode.getSectionName() + "." + kcScript.EXTENSION));
                functions += rootNode.getChildConfigNodes().size();
            }

            getLogger().info("Saved %d script%s containing %d function%s to a folder named '%s'.",
                    this.scripts.size(), (this.scripts.size() != 1 ? "s" : ""), functions, (functions != 1 ? "s" : ""),
                    scriptFolder.getName());
        });

        MenuItem importScriptsItem = new MenuItem("Import Scripts");
        contextMenu.getItems().add(importScriptsItem);
        importScriptsItem.setOnAction(event -> {
            File scriptFolder = FileUtils.askUserToSelectFolder(getGameInstance(), SCRIPT_IMPORT_PATH);
            if (scriptFolder == null)
                return;

            int filesImported = 0;
            for (File file : FileUtils.listFiles(scriptFolder)) {
                if (!file.getName().endsWith(kcScript.EXTENSION)) {
                    getLogger().warning("Skipping %s", file.getName());
                    continue;
                }

                Config scriptCfg = Config.loadConfigFromTextFile(file, false);
                int entityNameHash = GreatQuestUtils.hash(scriptCfg.getSectionName());
                kcCResourceEntityInst entity = getParentFile().getResourceByHash(entityNameHash);
                if (entity == null) {
                    getLogger().warning("Skipping %s, as the entity could not be resolved.", scriptCfg.getSectionName());
                    continue;
                }

                kcEntityInst entityInst = entity.getInstance();
                if (entityInst == null) {
                    getLogger().warning("Skipping %s because the entity is not valid.", entity.getName());
                    continue;
                }

                filesImported++;
                entityInst.addScriptFunctions(this, scriptCfg, scriptCfg.getSectionName(), false);
            }

            getLogger().info("Imported %d scripts.", filesImported);
        });

        MenuItem clearScriptsItem = new MenuItem("Clear Scripts");
        contextMenu.getItems().add(clearScriptsItem);
        clearScriptsItem.setOnAction(event -> {
            if (!FXUtils.makePopUpYesNo("Are you sure you'd like to clear all the scripts in the level?"))
                return;

            // Ensure entities no longer have script indices. (Otherwise they will point to the wrong scripts when we add new ones.)
            for (int i = 0; i < this.scripts.size(); i++) {
                kcScript script = this.scripts.get(i);
                kcCResourceEntityInst entity = script.getEntity();
                if (entity != null && entity.getInstance() != null)
                    entity.getInstance().removeScriptIndex();
            }

            this.scripts.clear();
            getLogger().info("Cleared the script list.");
        });

        // Apply GQS Script Group.
        MenuItem importScriptGroupItem = new MenuItem("Import GQS Script Group");
        contextMenu.getItems().add(importScriptGroupItem);
        importScriptGroupItem.setOnAction(event -> {
            File gqsGroupFile = FileUtils.askUserToOpenFile(getGameInstance(), SCRIPT_GROUP_IMPORT_PATH);
            if (gqsGroupFile == null)
                return;

            getLogger().info("Importing GQS script group '%s'.", gqsGroupFile.getName());
            Config scriptGroupCfg = Config.loadConfigFromTextFile(gqsGroupFile, false);
            GreatQuestAssetUtils.applyGqsScriptGroup(getParentFile(), scriptGroupCfg);
            getLogger().info("Finished importing the gqs.");
        });
    }

    /**
     * Writes the script list to a string builder.
     * @param level The level to find any extra data from.
     * @param builder The builder to write the script to.
     * @param settings The settings used to build the output.
     */
    public void toString(GreatQuestChunkedFile level, StringBuilder builder, kcScriptDisplaySettings settings) {
        for (int i = 0; i < this.scripts.size(); i++) {
            builder.append("// Script #").append(i + 1).append(":\n");
            this.scripts.get(i).toString(level, builder, settings);
            builder.append('\n');
        }
    }

    /**
     * Prints advanced script warnings to the logger.
     * @param logger the logger to print the warnings to
     */
    public void printAdvancedWarnings(ILogger logger) {
        Map<kcCResourceEntityInst, kcScriptValidationData> dataMap = new HashMap<>();
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);
            kcCResourceEntityInst entity = script.getEntity();

            if (entity == null) {
                for (int j = 0; j < script.getFunctions().size(); j++)
                    script.getFunctions().get(j).getCause().printWarning(logger, "the function/script was not attached to a valid entity");

                throw new RuntimeException("Cannot print warnings, there's a script which doesn't have an entity linked! (Line Number: " + script.getFunctions().get(0).getCause().getUserLineNumber());
            }

            kcScriptValidationData functionData = getOrCreateValidationData(logger, dataMap, entity);
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);

                // Add cause.
                if (functionData != null)
                    functionData.getCausesByType().computeIfAbsent(function.getCause().getType(), key -> new ArrayList<>()).add(function.getCause());

                // Add effects.
                for (int k = 0; k < function.getEffects().size(); k++) {
                    kcScriptEffect effect = function.getEffects().get(k);
                    kcAction action = (effect instanceof kcScriptEffectAction) ? ((kcScriptEffectAction) effect).getAction() : null;
                    if (action == null)
                        continue;

                    // Apply it to the target entity, not to the attached script entity.
                    kcScriptValidationData validationData = getOrCreateValidationData(logger, dataMap, effect.getTargetEntityRef().getResource());
                    if (validationData != null)
                        validationData.getActionsByType().computeIfAbsent(action.getActionID(), key -> new ArrayList<>()).add(action);
                }
            }

            // Apply animation sequence checks.
            kcEntityInst entityInst = entity.getInstance();
            kcEntity3DDesc entityDesc = entityInst.getDescription();
            kcActorBaseDesc actorBaseDesc = entityDesc instanceof kcActorBaseDesc ? (kcActorBaseDesc) entityDesc : null;
            if (actorBaseDesc != null) {
                kcCResourceNamedHash sequenceTable = actorBaseDesc.getAnimationSequences();
                if (sequenceTable != null) {
                    List<kcCActionSequence> sequences = sequenceTable.getSequences();
                    for (int j = 0; j < sequences.size(); j++) {
                        kcCActionSequence sequence = sequences.get(j);
                        for (int k = 0; k < sequence.getActions().size(); k++) {
                            kcAction action = sequence.getActions().get(k);

                            // Apply it to the target entity, not to the attached script entity.
                            kcScriptValidationData validationData = getOrCreateValidationData(logger, dataMap, entity);
                            if (validationData != null)
                                validationData.getActionsByType().computeIfAbsent(action.getActionID(), key -> new ArrayList<>()).add(action);
                        }
                    }
                }
            }

        }

        // Print the warnings in order.
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);
            kcScriptValidationData functionCauseData = getOrCreateValidationData(logger, dataMap, script.getEntity());

            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);

                // Add cause.
                if (functionCauseData != null && function.getCause() != null && !function.getCause().isLoadedFromGame())
                    function.getCause().printAdvancedWarnings(functionCauseData);

                // Process effects.
                for (int k = 0; k < function.getEffects().size(); k++) {
                    kcScriptEffect effect = function.getEffects().get(k);
                    kcAction action = (effect instanceof kcScriptEffectAction) ? ((kcScriptEffectAction) effect).getAction() : null;
                    if (action == null || action.isLoadedFromGame())
                        continue;

                    kcScriptValidationData actionData = getOrCreateValidationData(logger, dataMap, effect.getTargetEntityRef().getResource());
                    if (actionData != null)
                        action.printAdvancedWarnings(actionData);
                }
            }
        }
    }

    private static kcScriptValidationData getOrCreateValidationData(ILogger logger, Map<kcCResourceEntityInst, kcScriptValidationData> dataMap, kcCResourceEntityInst entity) {
        if (entity == null)
            return null;

        kcScriptValidationData validationData = dataMap.get(entity);
        if (validationData == null) {
            ILogger tempLogger = new AppendInfoLoggerWrapper(logger, entity.getName(), AppendInfoLoggerWrapper.TEMPLATE_OVERRIDE_AT_ORIGINAL);
            dataMap.put(entity, validationData = new kcScriptValidationData(entity, tempLogger));
        }

        return validationData;
    }
}