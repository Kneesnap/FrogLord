package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCause;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptListInterim;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptTOC;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.FileUtils.BrowserFileType;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.*;

/**
 * Represents a single script.
 * Scripts are a surprisingly complex system for the simple capabilities they offer.
 * Using a debugger was crucial to understanding how they worked properly.
 *
 * The way the script flow works is as follows:
 * kcCScriptMgr::OnCommand:
 *  -> This entity receives messages which indicate a trigger has fired, which trigger, what entity it has fired for, etc.
 *   - ?/0x00 - _kcMsgID
 *   - ?/0x04 - filter
 *   - 0/0x08 - cause (kcScriptCauseType)
 *   - 1/0x0C - subCause (The sub cause type can differ in meaning/usage per kcScriptCauseType)
 *   - 2/0x10 - Script Index
 *   - 3/0x14 - Target Entity Hash
 *   - 4/0x18 - Hash? This might be the hash of the message sender.
 *   - 5/0x1C - Script Cause Argument 0 (Optional)
 *   - 6/0x20 - Script Cause Argument 1 (Optional)
 *
 *  -> If it receives a message targeting 'kcCScriptMgr' (44EDFE47), it will send/broadcast the event globally to all entities listening.
 *   -> This is done by executing kcCScriptMgr::ProcessGlobalScript.
 *   -> That function will go through all scripts, skipping any which do not report having the provided cause type, via a bit mask check.
 *   -> Then, for each script cause in each script, it will check that the cause type & sub cause type match the expected values.
 *   -> Finally, all script causes which matched this criteria will have their effects run immediately.
 *   -> Because the only globally broadcasted script causes are LEVEL and EVENT, it seems they forgot to actually include proper checks.
 *   -> Sooo, it seems like the EVENT script cause will trigger regardless of if the event hash matches the expected value or not.
 *
 *  -> Otherwise, it will find and execute all functions in the target entity's script which have their "cause" met by the provided arguments.
 *   -> It will find the target entity by the target entity hash, and then obtain the entity's script.
 *   -> For the entity's script, it will test the causes to see if their cause criteria match. If they do, then the effects are executed.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public class kcScript extends GameObject<GreatQuestInstance> {
    private final kcScriptList scriptList;
    private final List<kcScriptFunction> functions;
    private final kcCResourceEntityInst entity;

    public static final String CONFIG_FIELD_SCRIPT_BEHAVIOR = "behavior";
    public static final String CONFIG_FIELD_SCRIPT_CAUSE = "cause";
    public static final String EXTENSION = "gqs";
    public static final BrowserFileType GQS_GROUP_FILE_TYPE = new BrowserFileType("Great Quest Script Group", EXTENSION);

    public kcScript(GreatQuestInstance instance, kcScriptList scriptList, kcCResourceEntityInst entity, List<kcScriptFunction> functions) {
        super(instance);
        this.scriptList = scriptList;
        this.entity = entity;
        this.functions = functions;
    }

    /**
     * Gets the number of total effects used by this script.
     */
    public int getEffectCount() {
        return this.functions != null ? this.functions.stream().mapToInt(function -> function.getEffects().size()).sum() : 0;
    }

    /**
     * Calculate an integer with bit flags set for every cause type active in this script.
     * @return causeTypeInteger
     */
    public int calculateCauseTypes() {
        int value = 0;
        for (int i = 0; i < this.functions.size(); i++) {
            kcScriptFunction function = this.functions.get(i);
            value |= function.getCause().getType().getValue();
        }

        return value;
    }

    /**
     * Gets a list of all attached entities.
     * @param level The chunked file containing level data.
     * @return attachedEntities
     */
    public List<kcCResourceEntityInst> getAttachedEntities(GreatQuestChunkedFile level) {
        if (level == null)
            return Collections.emptyList();

        List<kcCResourceEntityInst> entities = new ArrayList<>();
        kcScriptList scriptList = level.getScriptList();
        for (int i = 0; i < level.getChunks().size(); i++) {
            kcCResource resource = level.getChunks().get(i);
            if (!(resource instanceof kcCResourceEntityInst))
                continue;

            kcCResourceEntityInst entity = (kcCResourceEntityInst) resource;
            if (entity.getInstance() == null || entity.getInstance().getScriptIndex() < 0)
                continue;

            kcScript script = scriptList.getScripts().get(entity.getInstance().getScriptIndex());
            if (script == this)
                entities.add(entity);
        }

        return entities;
    }

    /**
     * Writes the script to a string builder.
     * @param level    The level file to search for entity data from. Can be null
     * @param builder  The builder to write the script to.
     * @param settings The settings for displaying the output.
     */
    public void toString(GreatQuestChunkedFile level, StringBuilder builder, kcScriptDisplaySettings settings) {
        // Write attached entities.
        if (level != null) {
            builder.append("/// Attached Entities: ");

            List<kcCResourceEntityInst> entities = getAttachedEntities(level);
            if (entities.size() > 0) {
                for (int i = 0; i < entities.size(); i++) {
                    kcCResourceEntityInst entityInst = entities.get(i);
                    if (i > 0)
                        builder.append(", ");
                    builder.append('"').append(entityInst.getName()).append('"');
                }
            } else {
                builder.append("None");
            }

            builder.append("\n\n");
        }

        for (int i = 0; i < this.functions.size(); i++) {
            kcScriptFunction function = this.functions.get(i);

            builder.append("// Function #").append(i + 1).append(" (");
            if (function.getCause() == null || function.getCause().isLoadedFromGame() || StringUtils.isNullOrWhiteSpace(function.getCause().getUserImportSource())) {
                builder.append("Loaded from Game");
            } else {
                builder.append(function.getCause().getUserImportSource());
            }

            if (function.getCause() != null && function.getCause().getUserLineNumber() > 0)
                builder.append(", Line #").append(function.getCause().getUserLineNumber());

            builder.append("):\n");
            function.toString(builder, settings);
            if (function.getEffects().size() > 0)
                builder.append('\n');
        }
    }

    /**
     * Saves this script to a Config node.
     * @param settings The settings to save the script with
     * @return configNode
     */
    public Config toConfigNode(ILogger logger, kcScriptDisplaySettings settings) {
        if (logger == null)
            throw new NullPointerException("logger");

        Config result = new Config(this.entity != null ? this.entity.getName() : "Unused_" + this.scriptList.getScripts().indexOf(this));
        for (int i = 0; i < this.functions.size(); i++)
            result.addChildConfig(this.functions.get(i).toConfigNode(logger, settings));

        return result;
    }

    /**
     * Adds all function definitions found in a Config node.
     * @param baseConfigNode The config node to add functions from
     * @param sourceName The source name (usually a file name) representing where the scripts came from.
     */
    public void addFunctionsFromConfigNode(ILogger logger, Config baseConfigNode, String sourceName) {
        if (logger == null)
            throw new NullPointerException("logger");
        if (baseConfigNode == null)
            throw new NullPointerException("baseConfigNode");

        Map<kcScriptCause, List<kcScriptFunction>> functionsByCause = new HashMap<>();
        for (int i = 0; i < this.functions.size(); i++) {
            kcScriptFunction function = this.functions.get(i);
            functionsByCause.computeIfAbsent(function.getCause(), key -> new ArrayList<>()).add(function);
        }

        Map<kcScriptCause, kcScriptCause> addedCauses = new HashMap<>();
        for (Config nestedFunction : baseConfigNode.getChildConfigNodes()) {
            kcScriptFunction newFunction = new kcScriptFunction(this, null);

            // Load the script function.
            try {
                newFunction.loadFromConfigNode(nestedFunction, logger);
            } catch (Throwable th) {
                // We must print exception causes, because if we don't, they won't be visible to the user.
                String entityName = getEntity() != null ? "'" + getEntity().getName() + "'" : "null";
                Utils.handleError(logger, th, false, "Failed to load script function for entity %s from '%s'.\n%s", entityName, sourceName, Utils.getOrderedErrorMessagesString(th));
                continue; // Skip registration.
            }

            String newFunctionUserImportSource = newFunction.getCause() != null ? newFunction.getCause().getUserImportSource() : null;
            GQSFunctionBehavior behavior = nestedFunction.getOrDefaultKeyValueNode(CONFIG_FIELD_SCRIPT_BEHAVIOR).getAsEnum(GQSFunctionBehavior.ADD);
            if (behavior == GQSFunctionBehavior.DELETE) {
                if (!newFunction.getEffects().isEmpty())
                    throw new RuntimeException("Function in '" + sourceName + "' used behavior " + behavior + ", but also had " + newFunction.getEffects().size() + " effects!");

                List<kcScriptFunction> functionsToRemove = functionsByCause.remove(newFunction.getCause());
                if (functionsToRemove != null && !this.functions.removeAll(functionsToRemove))
                    throw new RuntimeException("Function in '" + sourceName + "' used behavior " + behavior + " and tried to delete " + functionsToRemove.size() + " functions with the cause '" + newFunction.getCause().getAsGqsStatement() + "', but failed!");
            } else if (behavior == GQSFunctionBehavior.REPLACE) { // Replace means to override ALL functions with the cause (regardless of where they came from), replacing them with the new target. If there are no matching functions, the new function will still be added.
                List<kcScriptFunction> functionsToRemove = functionsByCause.remove(newFunction.getCause());
                if (functionsToRemove != null && !this.functions.removeAll(functionsToRemove))
                    throw new RuntimeException("Function in '" + sourceName + "' used behavior " + behavior + " and tried to replace " + functionsToRemove.size() + " functions with the cause '" + newFunction.getCause().getAsGqsStatement() + "', but failed!");

                this.functions.add(newFunction);
                functionsByCause.computeIfAbsent(newFunction.getCause(), key -> new ArrayList<>()).add(newFunction);
            } else if (behavior == GQSFunctionBehavior.ADD) { // Add means to replace any function with the same cause previously imported from the same file, otherwise to add it. This will avoid overriding functions from other files/the vanilla game.
                List<kcScriptFunction> functionsToReplace = functionsByCause.get(newFunction.getCause());
                kcScriptFunction functionToReplace = getFunctionToReplace(functionsToReplace, newFunctionUserImportSource, behavior);

                if (functionToReplace != null) {
                    newFunction = functionToReplace;
                    functionToReplace.loadFromConfigNode(nestedFunction, logger);
                } else {
                    // Add the new function.
                    this.functions.add(newFunction);
                    functionsByCause.computeIfAbsent(newFunction.getCause(), key -> new ArrayList<>()).add(newFunction);
                }

                kcScriptCause oldMatchingCause = addedCauses.putIfAbsent(newFunction.getCause(), newFunction.getCause());
                if (oldMatchingCause != null) // I never bothered to figure out the exact resolution order for which function will be seen first, but only the first function seen to have an applicable cause will be run.
                    logger.warning("The function in %s on line %d reuses 'cause=%s', which was previously used on line %d.",
                            sourceName, newFunction.getCause().getUserLineNumber(), newFunction.getCause().getAsGqsStatement(), oldMatchingCause.getUserLineNumber());
            }
        }
    }

    private static kcScriptFunction getFunctionToReplace(List<kcScriptFunction> functionsToReplace, String newFunctionUserImportSource, GQSFunctionBehavior behavior) {
        if (functionsToReplace == null)
            return null;

        kcScriptFunction functionToReplace = null;
        for (int i = 0; i < functionsToReplace.size(); i++) {
            kcScriptFunction function = functionsToReplace.get(i);
            kcScriptCause cause = function.getCause();

            if (!cause.isLoadedFromGame() && Objects.equals(cause.getUserImportSource(), newFunctionUserImportSource)) {
                if (functionToReplace != null) {
                    throw new RuntimeException("Cannot use " + behavior + " behavior when there are multiple functions using cause '" + cause.getAsGqsStatement() + "' in '" + newFunctionUserImportSource + "'.");
                } else { // Found the right function!
                    functionToReplace = function;
                }
            }
        }

        return functionToReplace;
    }

    /**
     * Loads a kcScript from the interim data model.
     * @param interim The interim data to load from.
     * @param scriptList The script list to add scripts to.
     * @param entity The entity which the script belongs to.
     * @param toc The table of contents entry declaring the script.
     */
    public static kcScript loadScript(kcScriptListInterim interim, kcScriptList scriptList, kcCResourceEntityInst entity, kcScriptTOC toc) {
        int causeIndex = (int) toc.getCauseStartIndex();

        int totalEffects = 0;
        List<kcScriptFunction> functions = new ArrayList<>();
        kcScript newScript = new kcScript(interim.getGameInstance(), scriptList, entity, functions);
        for (int i = 0; i < toc.getCauseCount(); i++) {
            interim.setupCauseReader(causeIndex++, 1);
            int mSize = interim.readNextCauseValue();
            int valueCount = ((mSize / Constants.INTEGER_SIZE) - 1); // Subtract 1 to remove 'mSize'.
            interim.setupCauseReader(causeIndex, valueCount);
            causeIndex += valueCount; // For the next cause, start reading at the next position.

            int causeTypeNumber = interim.readNextCauseValue();
            int effectOffset = interim.readNextCauseValue() * Constants.INTEGER_SIZE; // Change from an offset of ints to an offset of bytes.
            int effectCount = interim.readNextCauseValue();
            int subCauseTypeNumber = interim.readNextCauseValue();

            // Read unhandled data.
            List<Integer> unhandledData = null;
            if (interim.hasMoreCauseValues()) {
                unhandledData = new ArrayList<>();
                while (interim.hasMoreCauseValues())
                    unhandledData.add(interim.readNextCauseValue());
            }

            // Read cause.
            kcScriptCause cause = readCause(newScript, causeTypeNumber, subCauseTypeNumber, unhandledData);

            // Read effects.
            List<kcScriptEffect> effects = new ArrayList<>();
            kcScriptFunction newFunction = new kcScriptFunction(newScript, cause, effects);
            cause.setParentFunction(newFunction);

            // Find effect.
            int startingEffectIndex = interim.getEffectByOffset(effectOffset);
            if (startingEffectIndex < 0)
                throw new RuntimeException("The index '" + NumberUtils.toHexString(effectOffset) + "' did not correspond to the start of a script effect.");

            // Read effects.
            for (int j = 0; j < effectCount; j++)
                effects.add(interim.getEffects().get(startingEffectIndex + j).toScriptEffect(newFunction));

            // Modify the effect order to show in an order more closely resembling the order which the game will run the effects.
            kcScriptListInterim.reverseExecutionOrder(effects);

            totalEffects += effects.size();
            functions.add(newFunction);
        }

        if (totalEffects != toc.getEffectCount())
            interim.getLogger().warning("Script TOC listed a total of %d script effect(s), but we actually loaded %d.", toc.getEffectCount(), totalEffects);

        // Verify calculated cause types are correct.
        int calculatedCauseTypes = newScript.calculateCauseTypes();
        if (calculatedCauseTypes != toc.getCauseTypes())
            throw new RuntimeException("The kcScript created from the kcScriptTOC has a different calculated cause type value! (kcScriptTOC: " + NumberUtils.toHexString(toc.getCauseTypes()) + ", kcScript: " + NumberUtils.toHexString(calculatedCauseTypes) + ")");

        return newScript;
    }

    private static kcScriptCause readCause(kcScript script, int causeTypeNumber, int subCauseTypeNumber, List<Integer> unhandledData) {
        // Setup cause.
        kcScriptCauseType causeType = kcScriptCauseType.getCauseType(causeTypeNumber, false);
        if (causeType == null)
            throw new RuntimeException("Failed to find causeType from " + causeTypeNumber + ".");

        kcScriptCause cause = causeType.createNew(script);

        int optionalArgumentCount = unhandledData != null ? unhandledData.size() : 0;
        if (!cause.validateArgumentCount(optionalArgumentCount))
            throw new RuntimeException("kcScriptCauseType " + causeType + " cannot accept " + optionalArgumentCount + " optional arguments.");

        cause.onRead();
        cause.load(subCauseTypeNumber, unhandledData);
        return cause;
    }

    /**
     * Represents a unit of code executed as a unit.
     */
    @Getter
    public static class kcScriptFunction extends GameObject<GreatQuestInstance> {
        private final kcScript script;
        private kcScriptCause cause;
        private final List<kcScriptEffect> effects;

        public kcScriptFunction(@NonNull kcScript script, kcScriptCause cause) {
            this(script, cause, new ArrayList<>());
        }

        public kcScriptFunction(@NonNull kcScript script, kcScriptCause cause, List<kcScriptEffect> effects) {
            super(script.getGameInstance());
            this.script = script;
            this.cause = cause;
            this.effects = effects;
        }

        /**
         * Save the "cause" data from this function to a list.
         * @param output           The list to save to.
         * @param effectByteOffset The offset (in bytes) from the start of the effect data which the effect is located at.
         * @return How many values were added.
         */
        public int saveCauseData(List<Integer> output, int effectByteOffset) {
            int sizeIndex = output.size();
            output.add(-1); // Temporary value placeholder, so we can include the true size.

            output.add(this.cause.getType().getValue());
            output.add(effectByteOffset / Constants.INTEGER_SIZE);
            output.add(this.effects.size());
            this.cause.save(output);

            // Values added:
            int addedValueCount = output.size() - sizeIndex;
            output.set(sizeIndex, addedValueCount * Constants.INTEGER_SIZE);
            return addedValueCount;
        }

        /**
         * Writes the script function to a string builder.
         * @param builder  The builder to write the function to.
         * @param settings The settings for how to display the output.
         */
        public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
            builder.append("/// Cause: ");
            this.cause.toString(builder, settings);
            builder.append('\n');

            // Write effects.
            for (int i = 0; i < this.effects.size(); i++) {
                kcScriptEffect effect = this.effects.get(i);
                effect.toString(builder, settings);
                builder.append('\n');
            }
        }

        /**
         * Saves this script to a Config node.
         * @param settings The settings to save the script with
         * @return configNode
         */
        public Config toConfigNode(ILogger logger, kcScriptDisplaySettings settings) {
            OptionalArguments tempArguments = new OptionalArguments();
            StringBuilder builder = new StringBuilder();

            // Create new config node.
            Config result = new Config("Function");
            result.setSectionComment(this.cause.toString(settings));

            // Set cause.
            this.cause.save(tempArguments, settings);
            result.getOrCreateKeyValueNode(CONFIG_FIELD_SCRIPT_CAUSE).setAsString(tempArguments.toString());

            // Add script effects.
            tempArguments.clear();
            for (int i = 0; i < this.effects.size(); i++) {
                kcScriptEffect effect = this.effects.get(i);
                effect.saveEffect(logger, tempArguments, settings);
                tempArguments.toString(builder);
                result.getInternalText().add(new ConfigValueNode(builder.toString(), effect.getEndOfLineComment()));
                builder.setLength(0);
            }

            return result;
        }

        /**
         * Loads this function loaded from a Config node.
         * @param config The config containing the function definition
         */
        public void loadFromConfigNode(Config config, ILogger logger) {
            if (config == null)
                throw new NullPointerException("config");

            // Set cause.
            String rawScriptCause = config.getKeyValueNodeOrError(CONFIG_FIELD_SCRIPT_CAUSE).getAsString();
            this.cause = kcScriptCause.parseScriptCause(logger, this, rawScriptCause, config.getOriginalLineNumber(), config.getRootNode().getSectionName());

            // Add script effects.
            this.effects.clear();

            String fileName = config.getRootNode().getSectionName();
            List<ConfigValueNode> textNodes = config.getTextNodes();
            for (int i = 0; i < textNodes.size(); i++) {
                ConfigValueNode node = textNodes.get(i);
                String textLine = node != null ? node.getAsStringLiteral() : null;
                if (!StringUtils.isNullOrWhiteSpace(textLine))
                    this.effects.add(kcScriptEffect.parseScriptEffect(logger, this, textLine, config.getOriginalLineNumber() + i, fileName));
            }
        }

        /**
         * Gets the chunked file containing this script.
         */
        public GreatQuestChunkedFile getChunkedFile() {
            return getScript() != null ? getScript().getScriptList().getParentFile() : null;
        }
    }
}