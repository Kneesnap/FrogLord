package net.highwayfrogs.editor.file.config.script.format;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptParseException;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.editor.ScriptEditorController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * The base script formatter.
 * Created by Kneesnap on 8/1/2019.
 */
public class ScriptFormatter {
    public static final ScriptFormatter INSTANCE = new ScriptFormatter();

    /**
     * Convert a number into the option to display.
     * @param number The number to convert.
     * @return displayStr
     */
    public String numberToString(FroggerGameInstance instance, int number) {
        if (number == 65535)
            return "TIME_INFINITE";
        return String.valueOf(number);
    }

    /**
     * Parses the string into a number. Can be constants.
     * @param str The string to parse.
     * @return useNumber
     */
    public int stringToNumber(FroggerGameInstance instance, String str) {
        if (str.equalsIgnoreCase("TIME_INFINITE"))
            return 65535;
        return Integer.parseInt(str);
    }

    /**
     * Creates an editor node for this formatter.
     * @param command The command.
     * @param index   The argument index.
     * @return editorNode
     */
    public Node makeEditor(FroggerGameInstance instance, ScriptEditorController controller, ScriptCommand command, int index) {
        TextField field = new TextField(numberToString(instance, command.getArguments()[index]));
        Utils.setHandleKeyPress(field, newValue -> {
            try {
                command.getArguments()[index] = stringToNumber(instance, newValue);
                controller.updateCodeDisplay();
                return true;
            } catch (ScriptParseException spe) {
                Utils.makeErrorPopUp(null, spe, false);
                return false;
            }
        }, null);

        return field;
    }

    /**
     * Gets the style to display with the text.
     * @return textStyle
     */
    public String getTextStyle() {
        return "-fx-fill: RED;-fx-font-weight:normal;";
    }
}