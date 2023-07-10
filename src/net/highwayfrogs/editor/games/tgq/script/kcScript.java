package net.highwayfrogs.editor.games.tgq.script;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.tgq.script.kcScriptList.kcScriptEffect;
import net.highwayfrogs.editor.games.tgq.script.kcScriptList.kcScriptTOC;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single script.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
@AllArgsConstructor
public class kcScript {
    private final List<kcScriptFunction> functions;

    /**
     * Gets the number of total effects used by this script.
     */
    public long getEffectCount() {
        return this.functions != null ? this.functions.stream().mapToInt(function -> function.getEffects().size()).sum() : 0;
    }

    /**
     * Writes the script to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings for displaying the output.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        for (int i = 0; i < this.functions.size(); i++) {
            builder.append("// Function #").append(i + 1).append(":\n");
            this.functions.get(i).toString(builder, settings);
            if (this.functions.get(i).effects.size() > 0)
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
            long mSize = interim.readNextCauseValue();
            int valueCount = (int) ((mSize / Constants.INTEGER_SIZE) - 1); // Subtract 1 to remove 'mSize'.
            interim.setupCauseReader(causeIndex, valueCount);
            causeIndex += valueCount; // For the next cause, start reading at the next position.

            long scriptType = interim.readNextCauseValue();
            long effectOffset = interim.readNextCauseValue() * Constants.INTEGER_SIZE; // Change from an offset of ints to an offset of bytes.
            long effectCount = interim.readNextCauseValue();
            long causeType = interim.readNextCauseValue();

            // Read unhandled data.
            List<Long> unhandledData = null;
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
                effects.add(interim.getEffects().get(startingEffectIndex + j));

            totalEffects += effects.size();
            functions.add(new kcScriptFunction(scriptType, causeType, effects, unhandledData));
        }

        if (totalEffects != toc.getEffectCount())
            System.out.println("Script TOC listed a total of " + toc.getEffectCount() + " script effect(s), but we actually loaded " + totalEffects + ".");

        return new kcScript(functions);
    }

    /**
     * Represents a unit of code executed as a unit.
     */
    @Getter
    @AllArgsConstructor
    public static class kcScriptFunction {
        private long scriptType;
        private long causeType;
        private final List<kcScriptEffect> effects;
        private final List<Long> unhandledData;

        /**
         * Save the 'cause' data from this function to a list.
         * @param output           The list to save to.
         * @param effectByteOffset The offset (in bytes) from the start of the effect data which the effect is located at.
         * @return How many values were added.
         */
        public int saveCauseData(List<Long> output, long effectByteOffset) {
            int sizeIndex = output.size();
            output.add(-1L); // Temporary value placeholder, so we can include the true size.

            output.add(this.scriptType);
            output.add(effectByteOffset / Constants.INTEGER_SIZE);
            output.add((long) this.effects.size());
            output.add(this.causeType);

            // Write unhandled data.
            if (this.unhandledData != null && this.unhandledData.size() > 0)
                output.addAll(this.unhandledData);

            // Values added:
            long addedValueCount = output.size() - sizeIndex;
            output.set(sizeIndex, addedValueCount * Constants.INTEGER_SIZE);
            return (int) addedValueCount;
        }

        /**
         * Writes the script function to a string builder.
         * @param builder  The builder to write the function to.
         * @param settings The settings for how to display the output.
         */
        public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
            builder.append("/// Cause Type: ").append(this.causeType).append(", Script Type: ").append(this.scriptType).append("\n");
            if (this.unhandledData != null && this.unhandledData.size() > 0) {
                builder.append("/// Unhandled Data:");
                for (int i = 0; i < this.unhandledData.size(); i++)
                    builder.append(' ').append(this.unhandledData.get(i));

                builder.append('\n');
            }

            builder.append('\n');

            // Write effects.
            for (int i = 0; i < this.effects.size(); i++) {
                this.effects.get(i).toString(builder, settings);
                builder.append('\n');
            }
        }
    }
}
