package net.highwayfrogs.editor.scripting;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.scripting.compiler.NoodleCallHolder;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstruction;
import net.highwayfrogs.editor.scripting.tracking.NoodleRuntimeCodeSource;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.util.*;

/**
 * Represents a noodle script.
 */
@Getter
public class NoodleScript {
    private final NoodleScriptEngine engine;
    private final String name; // The name of this script.
    @Setter private File sourceFile; // The file containing the source code to this script.
    @Setter private Config config;
    private final List<NoodleRuntimeCodeSource> codeSources = new ArrayList<>();
    private final List<NoodleInstruction> instructions = new ArrayList<>(); // An ordered list of all instructions in this script.
    private final Map<String, Integer> labels = new HashMap<>();
    private final NoodleCallHolder<NoodleScriptFunction> functions = new NoodleCallHolder<>();
    public static final String CONFIG_CHILD_NAME = "Config";

    public NoodleScript(NoodleScriptEngine engine, String scriptName) {
        this.engine = engine;
        this.name = scriptName;
        this.config = new Config(scriptName);
    }

    /**
     * Prints the disassembled script to the console.
     */
    @SuppressWarnings("unused")
    public void printDisassembly(ILogger logger) {
        StringBuilder builder = new StringBuilder();
        disassemble(builder, true);
        logger.info(String.format("Script '%s':%n%s", getName(), builder));
    }

    /**
     * Print the instructions this script has.
     */

    public void disassemble(StringBuilder builder, boolean includeLineNumbers) {
        int goalDigits = NumberUtils.getDigitCount(getInstructions().size());

        // Get labels.
        List<String> labels = new ArrayList<>(this.labels.keySet());
        labels.sort(Comparator.comparingInt(this.labels::get));

        // Get functions.
        List<NoodleScriptFunction> allFunctions = new ArrayList<>();
        this.functions.values().forEach(allFunctions::addAll);
        allFunctions.sort(Comparator.comparingInt(NoodleScriptFunction::getStartAddress));

        // Write instructions.
        String nextLabelName = labels.size() > 0 ? labels.get(0) : null;
        int nextLabelPos = labels.size() > 0 ? this.labels.get(nextLabelName) : -1;
        for (int i = 0; i < this.instructions.size(); i++) {
            NoodleInstruction instruction = this.instructions.get(i);

            boolean canWriteNewLine = (i > 0);

            // Write labels.
            while (nextLabelName != null && nextLabelPos == i) {
                if (canWriteNewLine) {
                    builder.append(Constants.NEWLINE);
                    canWriteNewLine = false;
                }

                builder.append(nextLabelName).append(":").append(Constants.NEWLINE);

                labels.remove(0);
                nextLabelName = labels.size() > 0 ? labels.get(0) : null;
                nextLabelPos = labels.size() > 0 ? this.labels.get(nextLabelName) : -1;
            }

            // Write function labels.
            while (allFunctions.size() > 0 && i == allFunctions.get(0).getStartAddress()) {
                NoodleScriptFunction scriptFunction = allFunctions.remove(0);

                if (canWriteNewLine) {
                    builder.append(Constants.NEWLINE);
                    canWriteNewLine = false;
                }

                scriptFunction.writeSignature(builder, false);
                builder.append(":").append(Constants.NEWLINE);
            }

            // Write assembly instruction to line.
            if (includeLineNumbers)
                builder.append(NumberUtils.padNumberString(i, goalDigits)).append(": ");

            instruction.toString(builder, this);
            builder.append(Constants.NEWLINE);
        }
    }

    /**
     * Turn this into an empty script for loading again.
     */
    public void clearScript() {
        this.config = new Config(this.name);
        this.instructions.clear();
        this.codeSources.clear();
        this.labels.clear();
        this.functions.clear();
    }

    /**
     * Get a function accessible by this script.
     * Will not search parent scripts, due to errors.
     * @param functionName The name of the function to find.
     * @param argumentCount The number of arguments the function accepts.
     * @return function
     */
    public NoodleScriptFunction getFunctionByName(String functionName, int argumentCount) {
        return this.functions.getByNameAndArgumentCount(functionName, argumentCount);
    }

    /**
     * Gets the function containing the given address, if there is one.
     * @param address The address to find the function at.
     * @return scriptFunction, or null.
     */
    public NoodleScriptFunction getFunctionContainingAddress(int address) {
        for (List<NoodleScriptFunction> functions : this.functions.values()) {
            for (int i = 0; i < functions.size(); i++) {
                NoodleScriptFunction function = functions.get(i);
                if (function.contains(address))
                    return function;
            }
        }

        return null;
    }

    /**
     * Gets the folder which the script resides within.
     */
    public File getScriptFolder() {
        if (this.sourceFile == null)
            return null;

        return this.sourceFile.getParentFile();
    }

    @Override
    public String toString() {
        return Utils.getSimpleName(this) + "{name=" + this.name + ",instructions=" + this.instructions.size() + ",functions=" + this.functions.size() + "}";
    }
}