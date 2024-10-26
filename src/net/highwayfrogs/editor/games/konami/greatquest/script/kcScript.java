package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCause;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptListInterim;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptTOC;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 *  -> If it receives a message targeting 'kcCScriptMgr' (44EDFE47), it will broadcast the event globally to all entities listening.
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
 *
 * TODO:
 *  - Go over why some hashes aren't reversed in scripts.
 *  - The action sequences can sometimes not show animation name if the animation is in another file. (Eg: 00.dat)
 *  - Still figuring out why SETSEQUENCE doesn't show right. The hashes are seen in resource hashes too. (Some of these like the Vase are in 00.dat) (Seems it's in the TOC..?) "Positive" -> "PositiveActorDesc"
 *  - Still figuring out why actor names don't always resolve. (Could it be names added to the scene dynamically?)
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public class kcScript extends GameObject<GreatQuestInstance> {
    private final kcScriptList scriptList;
    private final List<kcScriptFunction> functions;

    public kcScript(GreatQuestInstance instance, kcScriptList scriptList, List<kcScriptFunction> functions) {
        super(instance);
        this.scriptList = scriptList;
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
            if (entity.getEntity() == null || entity.getEntity().getScriptIndex() < 0)
                continue;

            kcScript script = scriptList.getScripts().get(entity.getEntity().getScriptIndex());
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
            builder.append("// Function #").append(i + 1).append(":\n");
            this.functions.get(i).toString(builder, settings);
            if (this.functions.get(i).getEffects().size() > 0)
                builder.append('\n');
        }
    }

    /**
     * Loads a kcScript from the interim data model.
     * @param interim The interim data to load from.
     * @param scriptList The script list to add scripts to.
     * @param toc The table of contents entry declaring the script.
     */
    public static kcScript loadScript(kcScriptListInterim interim, kcScriptList scriptList, kcScriptTOC toc) {
        int causeIndex = (int) toc.getCauseStartIndex();

        int totalEffects = 0;
        List<kcScriptFunction> functions = new ArrayList<>();
        kcScript newScript = new kcScript(interim.getGameInstance(), scriptList, functions);
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
            kcScriptCause cause = readCause(interim.getGameInstance(), causeTypeNumber, subCauseTypeNumber, unhandledData);

            // Read effects.
            List<kcScriptEffect> effects = new ArrayList<>();
            kcScriptFunction newFunction = new kcScriptFunction(newScript, cause, effects);

            // Find effect.
            int startingEffectIndex = interim.getEffectByOffset(effectOffset);
            if (startingEffectIndex < 0)
                throw new RuntimeException("The index '" + NumberUtils.toHexString(effectOffset) + "' did not correspond to the start of a script effect.");

            // Read effects.
            for (int j = 0; j < effectCount; j++)
                effects.add(interim.getEffects().get(startingEffectIndex + j).toScriptEffect(newFunction));

            totalEffects += effects.size();
            functions.add(newFunction);
        }

        if (totalEffects != toc.getEffectCount())
            interim.getLogger().warning("Script TOC listed a total of " + toc.getEffectCount() + " script effect(s), but we actually loaded " + totalEffects + ".");

        // Verify calculated cause types are correct.
        int calculatedCauseTypes = newScript.calculateCauseTypes();
        if (calculatedCauseTypes != toc.getCauseTypes())
            throw new RuntimeException("The kcScript created from the kcScriptTOC has a different calculated cause type value! (kcScriptTOC: " + NumberUtils.toHexString(toc.getCauseTypes()) + ", kcScript: " + NumberUtils.toHexString(calculatedCauseTypes) + ")");

        return newScript;
    }

    private static kcScriptCause readCause(GreatQuestInstance gameInstance, int causeTypeNumber, int subCauseTypeNumber, List<Integer> unhandledData) {
        // Setup cause.
        kcScriptCauseType causeType = kcScriptCauseType.getCauseType(causeTypeNumber, false);
        if (causeType == null)
            throw new RuntimeException("Failed to find causeType from " + causeTypeNumber + ".");

        kcScriptCause cause = causeType.createNew(gameInstance);

        int optionalArgumentCount = unhandledData != null ? unhandledData.size() : 0;
        if (!cause.validateArgumentCount(optionalArgumentCount))
            throw new RuntimeException("kcScriptCauseType " + causeType + " cannot accept " + optionalArgumentCount + " optional arguments.");

        cause.load(subCauseTypeNumber, unhandledData);
        return cause;
    }

    /**
     * Represents a unit of code executed as a unit.
     */
    @Getter
    public static class kcScriptFunction extends GameObject<GreatQuestInstance> {
        private final kcScript script;
        @Setter private kcScriptCause cause;
        private final List<kcScriptEffect> effects;

        public kcScriptFunction(kcScript script, kcScriptCause cause) {
            this(script, cause, new ArrayList<>());
        }

        public kcScriptFunction(kcScript script, kcScriptCause cause, List<kcScriptEffect> effects) {
            super(script != null ? script.getGameInstance() : null);
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
         * Gets the chunked file containing this script.
         */
        public GreatQuestChunkedFile getChunkedFile() {
            return getScript() != null ? getScript().getScriptList().getParentFile() : null;
        }
    }
}