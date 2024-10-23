package net.highwayfrogs.editor.scripting.compiler;

import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNode;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirective;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstruction;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents an exception which happened in the Noodle compiler.
 */
public class NoodleCompilerException extends RuntimeException {
    public NoodleCompilerException(String message) {
        super(getMessage(message, null), null);
    }

    public NoodleCompilerException(String message, Object... args) {
        this(getMessage(Utils.formatStringSafely(message, args), null));
    }

    public NoodleCompilerException(String message, NoodleCodeLocation codeLocation, Object... args) {
        this(null, Utils.formatStringSafely(message, args), codeLocation);
    }

    public NoodleCompilerException(String message, NoodleToken token, Object... args) {
        this(null, Utils.formatStringSafely(message, args), token);
    }

    public NoodleCompilerException(String message, NoodleNode astNode, Object... args) {
        this(null, Utils.formatStringSafely(message, args), astNode);
    }

    public NoodleCompilerException(String message, NoodleInstruction instruction, Object... args) {
        this(null, Utils.formatStringSafely(message, args), instruction);
    }

    public NoodleCompilerException(String message, NoodlePreprocessorDirective directive, Object... args) {
        this(null, Utils.formatStringSafely(message, args), directive);
    }

    public NoodleCompilerException(Throwable cause, String message, NoodleCodeLocation codeLocation) {
        super(getMessage(message, cause, codeLocation), cause);
    }

    public NoodleCompilerException(Throwable cause, String message, NoodleToken token) {
        super(getMessage(message, cause, token), cause);
    }

    public NoodleCompilerException(Throwable cause, String message, NoodleNode astNode) {
        super(getMessage(message, cause, astNode), cause);
    }

    public NoodleCompilerException(Throwable cause, String message, NoodleInstruction instruction) {
        super(getMessage(message, cause, instruction), cause);
    }

    public NoodleCompilerException(Throwable cause, String message, NoodleToken token, Object... args) {
        super(getMessage(Utils.formatStringSafely(message, args), cause, token), cause);
    }

    public NoodleCompilerException(Throwable cause, String message, NoodlePreprocessorDirective directive) {
        super(getMessage(message, cause, directive), cause);
    }

    public NoodleCompilerException(Throwable cause, String message, NoodlePreprocessorDirective directive, Object... args) {
        super(getMessage(Utils.formatStringSafely(message, args), cause, directive), cause);
    }

    /**
     * Get the displayed error message.
     * @param message Message for this exception.
     * @param th    Exception
     * @return message
     */
    protected static String getMessage(String message, Throwable th) {
        return th != null && th.getLocalizedMessage() != null ?
                message + " (" + th.getLocalizedMessage() + ")"
                : message;
    }

    protected static String getMessage(String message, Throwable th, NoodleCodeLocation codeLocation) {
        return getMessage(message, th) + (codeLocation != null ? " (At " + NoodleUtils.getErrorPositionText(codeLocation) + ")" : "");
    }

    protected static String getMessage(String message, Throwable th, NoodleToken token) {
        return getMessage(message, th) + (token != null ? " (At " + NoodleUtils.getErrorPositionText(token) + ")" : "");
    }

    protected static String getMessage(String message, Throwable th, NoodleNode node) {
        return getMessage(message, th) + (node != null ? " (At " + NoodleUtils.getErrorPositionText(node) + ")" : "");
    }

    protected static String getMessage(String message, Throwable th, NoodleInstruction instruction) {
        return getMessage(message, th) + (instruction != null ? " (At " + NoodleUtils.getErrorPositionText(instruction) + ")" : "");
    }

    protected static String getMessage(String message, Throwable th, NoodlePreprocessorDirective directive) {
        return getMessage(message, th) + (directive != null ? " (At " + NoodleUtils.getErrorPositionText(directive) + ")" : "");
    }
}
