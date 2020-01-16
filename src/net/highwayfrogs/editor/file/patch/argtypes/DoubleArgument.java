package net.highwayfrogs.editor.file.patch.argtypes;

import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Created by Kneesnap on 1/15/2020.
 */
public class DoubleArgument extends PatchArgumentBehavior<Double> {
    @Override
    public Double parseString(String text) {
        return Double.parseDouble(text);
    }

    @Override
    public boolean isValidString(String text) {
        return Utils.isNumber(text);
    }

    @Override
    public String valueToString(Double number) {
        return Double.toString(number);
    }

    @Override
    public boolean isCorrectType(Object obj) {
        return obj instanceof Double;
    }

    @Override
    protected boolean isValidValueInternal(Double value, PatchArgument argument) {
        return (value >= argument.getMinimumValue()) && (value <= argument.getMaximumValue());
    }

    @Override
    public boolean isTrueValue(PatchValue value) {
        return value.isDecimal() && value.getAsDecimal() != 0D;
    }
}
