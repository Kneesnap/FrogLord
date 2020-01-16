package net.highwayfrogs.editor.file.patch.argtypes;

import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchValue;

/**
 * Created by Kneesnap on 1/15/2020.
 */
public class StringArgument extends PatchArgumentBehavior<String> {
    @Override
    public String parseString(String text) {
        return (text == null || text.equals("null")) ? "null" : text.substring(1, text.length() - 1);
    }

    @Override
    public boolean isValidString(String text) {
        return text.equals("null") || (text.startsWith("\"") && text.endsWith("\""));
    }

    @Override
    public String valueToString(String string) {
        return string != null ? "\"" + string + "\"" : "null";
    }

    @Override
    public boolean isCorrectType(Object obj) {
        return obj instanceof String;
    }

    @Override
    protected boolean isValidValueInternal(String value, PatchArgument argument) {
        return true;
    }

    @Override
    public boolean isTrueValue(PatchValue value) {
        return value.isString() && !value.getAsString().equals("null");
    }
}
