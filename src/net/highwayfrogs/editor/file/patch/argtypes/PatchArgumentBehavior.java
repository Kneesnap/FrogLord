package net.highwayfrogs.editor.file.patch.argtypes;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.gui.editor.PatchController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Base patch argument behavior.
 * Created by Kneesnap on 1/15/2020.
 */
public abstract class PatchArgumentBehavior<T> {

    /**
     * Parse a string value of this type.
     * @param text The text to try parsing.
     * @return parsedValue
     */
    public abstract T parseString(String text);

    /**
     * Test if a given string is a valid argument of this type.
     * @param text The text to test.
     * @return isValidArgument
     */
    public abstract boolean isValidString(String text);

    /**
     * Turns a value to a string.
     * @param obj The value to convert.
     * @return string
     */
    @SuppressWarnings("unchecked")
    public String safeValueToString(Object obj) {
        return isCorrectType(obj) ? valueToString((T) obj) : null;
    }

    /**
     * Turns a value to a string.
     * @param object The value to convert.
     * @return string
     */
    public abstract String valueToString(T object);

    /**
     * Test if an object is the proper type.
     * @param obj The object to test.
     * @return isProperType
     */
    public abstract boolean isCorrectType(Object obj);

    /**
     * Test if a value is valid for a given argument type.
     * @param value    The value to test.
     * @param argument The argument with restrictions.
     * @return isValid
     */
    @SuppressWarnings("unchecked")
    public boolean isValidValue(Object value, PatchArgument argument) {
        return isCorrectType(value) && isValidValueInternal((T) value, argument);
    }

    /**
     * Test if a value is valid for a given argument type.
     * @param value    The value to test.
     * @param argument The argument with restrictions.
     * @return isValid
     */
    protected abstract boolean isValidValueInternal(T value, PatchArgument argument);

    /**
     * Test if a value is a proper true value.
     * @param value The value to test.
     * @return isTrueValue
     */
    public abstract boolean isTrueValue(PatchValue value);

    /**
     * Create a JavaFX editor node for data of this argument type.
     * @param controller The controller the editor will be placed in.
     * @param argument   The argument to create an editor for.
     * @param variable   The variable to edit.
     * @return editorNode
     */
    public Node createEditor(PatchController controller, PatchArgument argument, PatchValue variable) {
        TextField field = new TextField(variable.toString());
        Utils.setHandleKeyPress(field, newValue -> {
            PatchArgumentBehavior<?> behavior = argument.getType().getBehavior();
            if (!behavior.isValidString(newValue))
                return false;

            Object resultValue = behavior.parseString(newValue);
            if (!behavior.isValidValue(resultValue, argument))
                return false;

            variable.setObject(resultValue);
            return true;
        }, controller::updatePatchDisplay);

        return field;
    }
}
