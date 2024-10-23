package net.highwayfrogs.editor.scripting.compiler.preprocessor.builtins;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.scripting.compiler.NoodleCallHolder;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompilerException;
import net.highwayfrogs.editor.scripting.functions.NoodleFunction;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstruction;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstructionCall;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages noodle preprocessor builtin values.
 */
public class NoodleBuiltinManager {
    @Getter private final NoodleCallHolder<NoodleBuiltin> builtins = new NoodleCallHolder<>();
    private final Map<String, NoodleSystemMacro> systemMacros = new HashMap<>();

    /**
     * Registers the registered built-in options.
     */
    public void registerPreprocessorBuiltins() {
        this.builtins.registerCallable(new NoodleBuiltinDefined());
    }

    /**
     * Gets the noodle preprocessor built in by its name and argument count.
     * @param name The name to lookup.
     * @param argumentCount The number of arguments to lookup.
     * @return built-in.
     */
    public NoodleBuiltin getBuiltIn(String name, int argumentCount) {
        return this.builtins.getByNameAndArgumentCount(name, argumentCount);
    }

    /**
     * Registers a system macro.
     * @param systemMacro The system macro to register.
     */
    public void registerSystemMacro(NoodleSystemMacro systemMacro) {
        if (systemMacro == null)
            throw new NullPointerException("systemMacro");

        this.systemMacros.put(systemMacro.getName(), systemMacro);
    }

    /**
     * Registers a system macro.
     * @param macroName The name of the macro.
     * @param functionReplacement The function the macro replaces.
     */
    public void registerSystemMacroBasicFunction(String macroName, NoodleFunction functionReplacement) {
        registerSystemMacro(new NoodleSystemMacroFunctionReplacement(macroName, functionReplacement.getLabel()));
    }

    /**
     * Test if a system macro exists with the given name.
     * @param systemMacroName The name of the system macro to test.
     * @return true, iff there is a system macro with the name.
     */
    public boolean hasSystemMacroNamed(String systemMacroName) {
        return this.systemMacros.containsKey(systemMacroName);
    }

    /**
     * Generates instructions for a system macro.
     * @param systemMacroName The name of the system macro.
     * @param pos The position to generate instructions at.
     * @param output The list to write system macro output instructions into.
     */
    public void generateSystemMacroInstructions(String systemMacroName, NoodleCodeLocation pos, List<NoodleInstruction> output) {
        NoodleSystemMacro macro = this.systemMacros.get(systemMacroName);
        if (macro == null)
            throw new NoodleCompilerException("Cannot generate instructions for unknown system macro: '%s'", pos, systemMacroName);

        macro.writeInstructions(pos, output);
    }

    @Getter
    @RequiredArgsConstructor
    public static abstract class NoodleSystemMacro {
        private final String name;

        /**
         * Writes instructions at the given position.
         * @param pos The position to create the instruction at.
         * @param output The list to write instructions to.
         */
        public abstract void writeInstructions(NoodleCodeLocation pos, List<NoodleInstruction> output);
    }

    /**
     * A macro which replaces the keyword with an empty function call.
     */
    public static class NoodleSystemMacroFunctionReplacement extends NoodleSystemMacro {
        private final String replacementFunctionName;

        public NoodleSystemMacroFunctionReplacement(String name, String replacementFunctionName) {
            super(name);
            this.replacementFunctionName = replacementFunctionName;
        }

        @Override
        public void writeInstructions(NoodleCodeLocation pos, List<NoodleInstruction> output) {
            output.add(new NoodleInstructionCall(pos, this.replacementFunctionName, 0));
        }
    }
}