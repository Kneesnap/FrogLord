package net.highwayfrogs.editor.scripting.compiler;

import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNode;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirective;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstruction;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;

/**
 * Thrown to indicate an error in a Noodle script's syntax during compilation.
 */
public class NoodleSyntaxException extends NoodleCompilerException {
    public NoodleSyntaxException(String message) {
        super(message);
    }

    public NoodleSyntaxException(String message, NoodleCodeLocation codeLocation, Object... args) {
        super(message, codeLocation, args);
    }

    public NoodleSyntaxException(String message, NoodleToken token, Object... args) {
        super(message, token, args);
    }

    public NoodleSyntaxException(String message, NoodleInstruction instruction, Object... args) {
        super(message, instruction, args);
    }

    public NoodleSyntaxException(Throwable cause, String message, NoodleToken token, Object... args) {
        super(cause, message, token, args);
    }

    public NoodleSyntaxException(String message, NoodleNode astNode, Object... args) {
        super(message, astNode, args);
    }

    public NoodleSyntaxException(String message, NoodlePreprocessorDirective directive, Object... args) {
        super(message, directive, args);
    }

    public NoodleSyntaxException(Throwable cause, String message, NoodleCodeLocation codeLocation) {
        super(cause, message, codeLocation);
    }

    public NoodleSyntaxException(Throwable cause, String message, NoodleToken token) {
        super(cause, message, token);
    }

    public NoodleSyntaxException(Throwable cause, String message, NoodleNode astNode) {
        super(cause, message, astNode);
    }

    public NoodleSyntaxException(Throwable cause, String message, NoodleInstruction instruction) {
        super(cause, message, instruction);
    }

    public NoodleSyntaxException(Throwable cause, String message, NoodlePreprocessorDirective directive, Object... args) {
        super(cause, message, directive, args);
    }
}
