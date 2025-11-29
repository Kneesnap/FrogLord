package net.highwayfrogs.editor.games.konami.greatquest.script;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.*;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCause;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcInterimScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptListInterim;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptTOC;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.propertylist.PropertyListNode;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.AppendInfoLoggerWrapper;

import java.util.*;

/**
 * Represents a list of scripts.
 * The logic has been determined by reading kcCScriptMgr::Load() from the PS2 PAL version.
 * Created by Kneesnap on 6/26/2023.
 */
public class kcScriptList extends kcCResource {
    @Getter private final List<kcScript> scripts = new ArrayList<>();
    private transient kcScriptListInterim interim;

    public static final String GLOBAL_SCRIPT_NAME = "scriptdata";

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
        List<kcScriptEffect> currentFunctionEffects = new ArrayList<>();
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);

            // Create new entry.
            kcScriptTOC newEntry = new kcScriptTOC(script.calculateCauseTypes(), statusData.size(), script.getFunctions().size(), script.getEffectCount());
            entries.add(newEntry);

            // Add 'cause' & 'effect' data.
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);
                function.saveCauseData(statusData, effects.size() * kcInterimScriptEffect.SIZE_IN_BYTES);

                // Save the effects in an order to make the effects run in roughly the effect list order.
                currentFunctionEffects.clear();
                currentFunctionEffects.addAll(function.getEffects());
                kcScriptListInterim.reverseExecutionOrder(currentFunctionEffects);
                for (int k = 0; k < currentFunctionEffects.size(); k++)
                    effects.add(currentFunctionEffects.get(k).toInterimScriptEffect());
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
    public void addToPropertyList(PropertyListNode propertyList) {
        super.addToPropertyList(propertyList);
        propertyList.add("Scripts", this.scripts.size());
    }

    @Override
    public void setupRightClickMenuItems(ContextMenu contextMenu) {
        super.setupRightClickMenuItems(contextMenu);

        // Apply GQS Script Group.
        MenuItem importScriptGroupItem = new MenuItem("Import GQS File");
        contextMenu.getItems().add(importScriptGroupItem);
        importScriptGroupItem.setOnAction(event -> getParentFile().askUserToImportGqsFile());
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

                throw new RuntimeException("Cannot print warnings, there's a script which doesn't have an entity linked! (Line Number: " + script.getFunctions().get(0).getCause().getUserLineNumber() + ")");
            }

            kcScriptValidationData functionData = getOrCreateValidationData(logger, dataMap, entity);
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);

                // Add cause.
                if (functionData != null)
                    functionData.addCause(function.getCause());

                // Add effects.
                for (int k = 0; k < function.getEffects().size(); k++) {
                    kcScriptEffect effect = function.getEffects().get(k);
                    kcAction action = (effect instanceof kcScriptEffectAction) ? ((kcScriptEffectAction) effect).getAction() : null;
                    if (action == null)
                        continue;

                    // Apply it to the target entity, not to the attached script entity.
                    kcScriptValidationData validationData = getOrCreateValidationData(logger, dataMap, effect.getTargetEntity(true));
                    if (validationData != null)
                        validationData.addAction(action);
                    if (functionData != null)
                        functionData.addSendNumberToOwner(action);
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
                            if (validationData != null) {
                                validationData.addAction(action);
                                validationData.addSendNumberToOwner(action);
                            }
                        }
                    }
                }
            }
        }

        // Print the warnings in order.
        Set<kcScriptCause> seenCauses = new HashSet<>();
        for (int i = 0; i < this.scripts.size(); i++) {
            kcScript script = this.scripts.get(i);
            kcScriptValidationData functionCauseData = getOrCreateValidationData(logger, dataMap, script.getEntity());

            seenCauses.clear();
            for (int j = 0; j < script.getFunctions().size(); j++) {
                kcScriptFunction function = script.getFunctions().get(j);
                kcScriptCause cause = function.getCause();

                // Add cause.
                if (functionCauseData != null && cause != null && !cause.isLoadedFromGame())
                    cause.printAdvancedWarnings(functionCauseData);

                // This shouldn't really ever occur because kcScript.addFunctionsFromConfigNode() should handle it first.
                // But, this has been added as a safety check, just in case we ever load functions through some other way such as a gui.
                if (functionCauseData != null && cause != null && !seenCauses.add(cause))
                    cause.printWarning(logger, functionCauseData.getEntityName() + " contains multiple functions using the same cause! Only one of them will run! (Cause: '" + cause.getAsGqsStatement() + "')");

                if (function.getEffects().isEmpty() && cause != null && !cause.isLoadedFromGame()) {
                    logger.warning("The function on line %d in %s is pointless because it has no commands (kcScriptEffects)!",
                            cause.getUserLineNumber(), cause.getUserImportSource());
                    continue;
                }

                // Process effects.
                for (int k = 0; k < function.getEffects().size(); k++) {
                    kcScriptEffect effect = function.getEffects().get(k);
                    kcAction action = (effect instanceof kcScriptEffectAction) ? ((kcScriptEffectAction) effect).getAction() : null;
                    if (action == null || action.isLoadedFromGame())
                        continue;

                    kcScriptValidationData actionData = getOrCreateValidationData(logger, dataMap, effect.getTargetEntity(true));
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

    /**
     * Removes a script.
     * @param scriptIndex the index of the script to remove
     * @return removedScript
     */
    public kcScript removeScript(int scriptIndex) {
        if (scriptIndex < 0 || scriptIndex >= this.scripts.size())
            throw new IllegalArgumentException("Invalid scriptIndex: " + scriptIndex);

        kcScript script = this.scripts.remove(scriptIndex);

        // Update script indices.
        for (kcCResource resource : getParentFile().getChunks()) {
            if (!(resource instanceof kcCResourceEntityInst))
                continue;

            kcEntityInst entityInst = ((kcCResourceEntityInst) resource).getInstance();
            if (entityInst == null)
                continue;

            if (entityInst.getScriptIndex() == scriptIndex) {
                entityInst.removeScriptIndex();
            } else if (entityInst.getScriptIndex() > scriptIndex) {
                entityInst.decrementScriptIndex();
            }
        }

        return script;
    }
}