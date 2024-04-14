package net.highwayfrogs.editor.games.konami.greatquest.script;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCause;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptListInterim;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcScriptTOC;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceEntityInst;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single script.
 * Scripts are a surprisingly complex system for the simple capabilities they offer.
 * Using a debugger was crucial to understanding how they worked properly.
 * TODO:
 *  - Go over why some hashes aren't reversed in scripts.
 *  - The action sequences can sometimes not show animation name if the animation is in another file. (Eg: 00.dat)
 *  - Still figuring out why SETSEQUENCE doesn't show right. The hashes are seen in resource hashes too. (Some of these like the Vase are in 00.dat) (Seems it's in the TOC..?) "Positive" -> "PositiveActorDesc"
 *  - Still figuring out why actor names don't always resolve. (Could it be names added to the scene dynamically?)
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
@RequiredArgsConstructor
public class kcScript {
    private final List<kcScriptFunction> functions;

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
     * @param toc     The table of contents entry declaring the script.
     */
    public static kcScript loadScript(kcScriptListInterim interim, kcScriptTOC toc) {
        int causeIndex = (int) toc.getCauseStartIndex();

        int totalEffects = 0;
        List<kcScriptFunction> functions = new ArrayList<>();
        for (int i = 0; i < toc.getCauseCount(); i++) {
            interim.setupCauseReader(causeIndex++, 1);
            int mSize = interim.readNextCauseValue();
            int valueCount = ((mSize / Constants.INTEGER_SIZE) - 1); // Subtract 1 to remove 'mSize'.
            interim.setupCauseReader(causeIndex, valueCount);
            causeIndex += valueCount; // For the next cause, start reading at the next position.

            int causeTypeNumber = interim.readNextCauseValue();
            int effectOffset = interim.readNextCauseValue() * Constants.INTEGER_SIZE; // Change from an offset of ints to an offset of bytes.
            int effectCount = interim.readNextCauseValue();
            int causeValueNumber = interim.readNextCauseValue();

            // Read unhandled data.
            List<Integer> unhandledData = null;
            if (interim.hasMoreCauseValues()) {
                unhandledData = new ArrayList<>();
                while (interim.hasMoreCauseValues())
                    unhandledData.add(interim.readNextCauseValue());
            }

            // Find effect.
            int startingEffectIndex = interim.getEffectByOffset(effectOffset);
            if (startingEffectIndex < 0)
                throw new RuntimeException("The index '" + Utils.toHexString(effectOffset) + "' did not correspond to the start of a script effect.");

            // Find start effect.
            // Read effects.
            List<kcScriptEffect> effects = new ArrayList<>();
            for (int j = 0; j < effectCount; j++)
                effects.add(interim.getEffects().get(startingEffectIndex + j).toScriptEffect());

            totalEffects += effects.size();

            // Setup cause.
            kcScriptCauseType causeType = kcScriptCauseType.getCauseType(causeTypeNumber, false);
            if (causeType == null)
                throw new RuntimeException("Failed to find causeType from " + causeTypeNumber + ".");

            kcScriptCause cause = causeType.createNew();

            int optionalArgumentCount = unhandledData != null ? unhandledData.size() : 0;
            if (!cause.validateArgumentCount(optionalArgumentCount))
                throw new RuntimeException("kcScriptCauseType " + causeType + " cannot accept " + optionalArgumentCount + " optional arguments.");

            cause.load(causeValueNumber, unhandledData);
            functions.add(new kcScriptFunction(cause, effects));
        }

        if (totalEffects != toc.getEffectCount())
            System.out.println("Script TOC listed a total of " + toc.getEffectCount() + " script effect(s), but we actually loaded " + totalEffects + ".");

        kcScript newScript = new kcScript(functions);

        // Verify calculated cause types are correct.
        int calculatedCauseTypes = newScript.calculateCauseTypes();
        if (calculatedCauseTypes != toc.getCauseTypes())
            throw new RuntimeException("The kcScript created from the kcScriptTOC has a different calculated cause type value! (kcScriptTOC: " + Utils.toHexString(toc.getCauseTypes()) + ", kcScript: " + Utils.toHexString(calculatedCauseTypes) + ")");

        return newScript;
    }

    /**
     * Represents a unit of code executed as a unit.
     */
    @Getter
    @AllArgsConstructor
    public static class kcScriptFunction {
        @Setter private kcScriptCause cause;
        private final List<kcScriptEffect> effects;

        public kcScriptFunction(kcScriptCause cause) {
            this(cause, new ArrayList<>());
        }

        /**
         * Save the 'cause' data from this function to a list.
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
    }
}