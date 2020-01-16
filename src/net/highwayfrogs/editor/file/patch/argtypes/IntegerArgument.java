package net.highwayfrogs.editor.file.patch.argtypes;

import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Created by Kneesnap on 1/15/2020.
 */
public class IntegerArgument extends PatchArgumentBehavior<Integer> {
    @Override
    public Integer parseString(String text) {
        return text.startsWith("0x") ? Integer.parseInt(text.substring(2), 16) : Integer.parseInt(text);
    }

    @Override
    public boolean isValidString(String text) {
        return Utils.isInteger(text) || Utils.isHexInteger(text);
    }

    @Override
    public String valueToString(Integer number) {
        return String.valueOf(number);
    }

    @Override
    public boolean isCorrectType(Object obj) {
        return obj instanceof Integer;
    }

    @Override
    protected boolean isValidValueInternal(Integer value, PatchArgument argument) {
        return (value >= argument.getMinimumValue()) && (value <= argument.getMaximumValue());
    }

    @Override
    public boolean isTrueValue(PatchValue value) {
        return value.isInteger() && value.getAsInteger() != 0;
    }
}
