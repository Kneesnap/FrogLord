package net.highwayfrogs.editor.scripting.compiler;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.scripting.NoodleScript;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.scripting.NoodleScriptFunction;
import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNode;
import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNodeFunctionDefinition;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodleCachedInclude;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodleMacro;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.NoodlePreprocessor;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstruction;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstructionJump;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstructionJumpPush;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.scripting.tracking.NoodleFileCodeSource;
import net.highwayfrogs.editor.scripting.tracking.NoodleRuntimeCodeSource;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contains information used when compiling a noodle script.
 */
@Getter
public class NoodleCompileContext {
    private final NoodleScriptEngine engine;
    private final NoodlePreprocessor preprocessor;
    private final String scriptText;
    private final NoodleScript targetScript;
    private final List<NoodleToken> tokens = new ArrayList<>();
    @Setter private int currentTokenIndex;
    @Setter private boolean buildCanBreak = false;
    @Setter private boolean buildCanContinue = false;
    @Setter private NoodleNode node;
    private final Map<String, NoodleRuntimeCodeSource> runtimeCodeSources = new HashMap<>();
    private final List<NoodleNode> nodes = new ArrayList<>();
    private final List<NoodleInstruction> instructions = new ArrayList<>();
    private final Map<String, List<NoodleInstruction>> labelJumps = new HashMap<>();
    private final Map<String, Integer> labels = new HashMap<>();
    private final NoodleCallHolder<NoodleScriptFunction> functions = new NoodleCallHolder<>();
    private final Map<NoodleNodeFunctionDefinition, NoodleScriptFunction> functionsByDefinition = new HashMap<>();
    private final NoodleCallHolder<NoodleMacro> macros = new NoodleCallHolder<>();
    private final Map<File, AtomicInteger> includes = new HashMap<>();
    private final List<NoodleCachedInclude> cachedIncludes = new ArrayList<>();

    // Preprocessor data.
    private final Map<File, NoodleFileCodeSource> codeSourcesByFile = new HashMap<>();
    private final Set<NoodleMacro> macrosCurrentlyEvaluating = new HashSet<>();
    private final List<String> mainArgumentNames = new ArrayList<>();

    public NoodleCompileContext(NoodleScriptEngine engine, String scriptText, NoodleScript targetScript) {
        this.engine = engine;
        this.preprocessor = new NoodlePreprocessor(this);
        this.scriptText = scriptText;
        this.targetScript = targetScript;
    }

    /**
     * Reset the context for compiling a new script.
     */
    public void reset() {
        this.currentTokenIndex = 0;
        this.buildCanBreak = false;
        this.buildCanContinue = false;
        this.node = null;
        this.nodes.clear();
        this.instructions.clear();
        this.labels.clear();
        this.labelJumps.clear();
        this.functions.clear();
        this.functionsByDefinition.clear();
        this.macros.clear();
        this.includes.clear();
        this.cachedIncludes.clear();
        this.runtimeCodeSources.clear();
        this.mainArgumentNames.clear();
        this.codeSourcesByFile.clear();

        // Clear preprocessor data too.
        this.macrosCurrentlyEvaluating.clear();
    }

    /**
     * Decrements to the previous token.
     */
    public void decrementToken() {
        this.currentTokenIndex--;
    }

    /**
     * Increment to the next token.
     */
    public int incrementToken() {
        return this.currentTokenIndex++;
    }

    /**
     * Gets the current token.
     */
    public NoodleToken getCurrentToken() {
        return getTokens().get(this.currentTokenIndex);
    }

    /**
     * Peeks at the next token.
     * @return token
     */
    public NoodleToken peekToken() {
        return this.tokens.get(this.currentTokenIndex + 1);
    }

    /**
     * Peeks at a token in the future. Providing '0' will give the current token.
     * @param offset The offset to access the token at.
     * @return token
     */
    public NoodleToken peekToken(int offset) {
        return this.tokens.get(this.currentTokenIndex + offset);
    }

    /**
     * Gets the current token, then increments to the next token.
     */
    public NoodleToken getCurrentTokenIncrement() {
        NoodleToken token = getCurrentToken();
        incrementToken();
        return token;
    }

    /**
     * Whether there are more tokens to read.
     * @return hasMoreTokens
     */
    public boolean hasMoreTokens() {
        int tokenCount = this.tokens.size() - 1; // The last token is EOF, so we don't count it.
        return this.currentTokenIndex < tokenCount;
    }

    /**
     * Tests if there are at least x tokens left.
     */
    public boolean hasAtLeastXTokensLeft(int x) {
        return this.tokens.size() - x >= this.currentTokenIndex;
    }

    /**
     * Apply the code labels to the compiled instructions.
     */
    private void applyLabels() {
        for (String labelName : this.labelJumps.keySet()) {
            Integer labelLocation = this.labels.get(labelName);
            if (labelLocation == null)
                throw new NoodleSyntaxException("Referenced an undeclared label `" + labelName + "`.");

            for (NoodleInstruction instruction : this.labelJumps.get(labelName)) {
                if (instruction instanceof NoodleInstructionJump) {
                    ((NoodleInstructionJump) instruction).setJumpPosition(labelLocation);
                } else if (instruction instanceof NoodleInstructionJumpPush) {
                    ((NoodleInstructionJumpPush) instruction).setJumpPosition(labelLocation);
                } else {
                    throw new NoodleSyntaxException("Don't know how to apply label to %s.", instruction, Utils.getSimpleName(instruction));
                }
            }
        }
    }

    /**
     * Applies the freshly compiled context data (such as instructions, labels, etc) to the script.
     */
    public void applyToScript() {
        this.applyLabels();

        // Write instructions to the script.
        this.targetScript.getInstructions().clear();
        this.targetScript.getInstructions().addAll(this.instructions);

        // Write functions to the script.
        for (List<NoodleScriptFunction> functions : this.functions.values())
            functions.sort(Comparator.comparingInt(NoodleScriptFunction::getArgumentCount));

        this.targetScript.getFunctions().clear();
        this.targetScript.getFunctions().getCallablesByName()
                .putAll(this.functions.getCallablesByName());

        // Write labels.
        this.targetScript.getLabels().clear();
        this.targetScript.getLabels().putAll(this.labels);

        // Apply runtime code sources:
        this.targetScript.getCodeSources().clear();
        for (int i = 0; i < this.runtimeCodeSources.size(); i++)
            this.targetScript.getCodeSources().add(null);

        for (NoodleRuntimeCodeSource codeSource : this.runtimeCodeSources.values())
            this.targetScript.getCodeSources().set(codeSource.getIndex(), codeSource);
    }

    /**
     * Gets the code source of a file.
     * @param file The file to get the code source of
     * @return codeSource
     */
    public NoodleFileCodeSource getCodeSource(File file) {
        if (file == null)
            return null;

        NoodleFileCodeSource source = this.codeSourcesByFile.get(file);
        if (source == null) {
            this.cachedIncludes.add(this.preprocessor.getCachedInclude(file));
            this.codeSourcesByFile.put(file, source = new NoodleFileCodeSource(this.targetScript, file));
        }

        return source;
    }

    /**
     * Gets the runtime code location equivalent for a compile-time NoodleCodeLocation.
     * @param oldCodeLocation The compile-time NoodleCodeLocation.
     * @return newNoodleCodeLocation
     */
    public NoodleCodeLocation getRuntimeCodeLocation(NoodleCodeLocation oldCodeLocation) {
        String oldLocationString = oldCodeLocation != null && oldCodeLocation.getSource() != null
                ? oldCodeLocation.getSource().getDisplay() : null;
        NoodleRuntimeCodeSource source = this.runtimeCodeSources.get(oldLocationString);
        if (source == null)
            this.runtimeCodeSources.put(oldLocationString, source = new NoodleRuntimeCodeSource(oldLocationString, this.runtimeCodeSources.size()));

        return new NoodleCodeLocation(source, oldCodeLocation.getLineNumber(), oldCodeLocation.getLinePosition());
    }
}
