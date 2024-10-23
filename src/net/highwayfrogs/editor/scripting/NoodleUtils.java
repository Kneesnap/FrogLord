package net.highwayfrogs.editor.scripting;

import net.highwayfrogs.editor.scripting.compiler.nodes.NoodleNode;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.directives.NoodlePreprocessorDirective;
import net.highwayfrogs.editor.scripting.compiler.tokens.NoodleToken;
import net.highwayfrogs.editor.scripting.instructions.NoodleInstruction;
import net.highwayfrogs.editor.scripting.tracking.NoodleCodeLocation;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;

/**
 * Contains static noodle utilities.
 */
public class NoodleUtils {
    /**
     * Gets the error position text for the given position.
     * @param codeLocation The position to get line information from.
     * @return lineInformation
     */
    public static String getErrorPositionText(NoodleCodeLocation codeLocation) {
        if (codeLocation == null)
            return "<NULL>";

        String displayMessage = codeLocation.getSource() != null ? codeLocation.getSource().getDisplay() : null;
        return "line " + codeLocation.getLineNumber()
                + ", col " + codeLocation.getLinePosition()
                + (displayMessage != null && displayMessage.length() > 0 ? ", " + displayMessage : "");
    }

    /**
     * Gets the error position text for the given token.
     * @param token The token to get the error position from.
     * @return lineInformation
     */
    public static String getErrorPositionText(NoodleToken token) {
        return (token != null ? getErrorPositionText(token.getCodeLocation()) : "<NULL TOKEN>");
    }

    /**
     * Gets the error position text for the given AST node.
     * @param node The node to get the error position from.
     * @return lineInformation
     */
    public static String getErrorPositionText(NoodleNode node) {
        return node != null ? getErrorPositionText(node.getCodeLocation()) : "<NULL NODE>";
    }

    /**
     * Gets the error position text for the given noodle instruction.
     * @param instruction The instruction to get the information from.
     * @return lineInformation
     */
    public static String getErrorPositionText(NoodleInstruction instruction) {
        return instruction != null ? getErrorPositionText(instruction.getCodeLocation()) : "<NULL INSTRUCTION>";
    }

    /**
     * Gets the error position text for the given noodle preprocessor directive.
     * @param directive The directive to get the information from.
     * @return lineInformation
     */
    public static String getErrorPositionText(NoodlePreprocessorDirective directive) {
        return directive != null ? getErrorPositionText(directive.getCodeLocation()) : "<NULL DIRECTIVE>";
    }

    /**
     * Gets the script name from its file name.
     * @param file The file to get the script name from.
     */
    public static String getScriptNameFromFile(File file) {
        return Utils.stripExtension(file.getName());
    }

    /**
     * Converts strings with escaped special characters in them, into strings with special characters in them.
     * @param codeStr The string to convert.
     * @return compiledStr
     */
    public static String codeStringToCompiledString(String codeStr) {
        return codeStr
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }

    /**
     * Converts strings with special characters in them into strings with escaped special characters in them.
     * @param compiledStr The string to convert.
     * @return codeStr
     */
    public static String compiledStringToCodeString(String compiledStr) {
        return compiledStr // Operations should happen in reverse order than codeStringToCompiledString
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\"", "\\\"");
    }
}
