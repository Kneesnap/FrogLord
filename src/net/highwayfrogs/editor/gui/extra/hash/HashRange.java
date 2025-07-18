package net.highwayfrogs.editor.gui.extra.hash;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;

/**
 * Represents a range of allowed hash values.
 * Created by Kneesnap on 5/28/2025.
 */
@RequiredArgsConstructor
public class HashRange {
    @Getter @NonNull private final HashRangeType rangeType;
    @Getter private final int minValue;
    @Getter private final int maxValue;

    /**
     * Tests if the given value is found within the provided range.
     * @param value the range to test
     * @return true iff the value is within the range
     */
    public boolean isInRange(int value) {
        if (this.maxValue >= this.minValue) {
            return value >= this.minValue && this.maxValue >= value;
        } else {
            return value >= this.minValue || value <= this.maxValue;
        }
    }

    /**
     * Returns true iff the minimum value matches the maximum value.
     */
    public boolean isMinValueSameAsMaxValue() {
        return this.minValue == this.maxValue;
    }

    /**
     * Gets the singular range value, if the range represents a single value.
     * @return singleValue
     */
    public int getSingleValue() {
        if (!isMinValueSameAsMaxValue())
            throw new RuntimeException("The minimum value (" + this.minValue + ") did not match the maximum value (" + this.maxValue + ")!");

        return this.minValue;
    }

    /**
     * Gets the next value in the range, or -1 if there is none.
     * @param currentValue the current value to increment from. if -1 is provided, the first value will be provided.
     * @return nextValue
     */
    public int getNextValue(int currentValue) {
        if (currentValue < 0)
            return this.minValue;

        if (this.maxValue >= this.minValue) {
            if (currentValue >= this.maxValue || currentValue < this.minValue)
                return -1;

            return currentValue + 1;
        } else {
            if (currentValue >= this.maxValue && currentValue < this.minValue)
                return -1;

            return (currentValue + 1) % this.rangeType.getMaximumValue();
        }
    }

    /**
     * Parses the range from a line of text
     * @param input the line of text to parse the range from
     * @param rangeType the type of range to parse
     * @return parsedRange, or null
     */
    public static HashRange parseRange(String input, HashRangeType rangeType) {
        if (rangeType == null)
            throw new NullPointerException("rangeType");
        if (StringUtils.isNullOrWhiteSpace(input))
            throw new NullPointerException("input");

        String[] split = input.trim().split("-");
        if (split.length != 1 && split.length != 2)
            throw new IllegalArgumentException("The text '" + input + "' cannot be interpreted as a range.");

        for (int i = 0; i < split.length; i++)
            if (!NumberUtils.isInteger(split[i]))
                throw new IllegalArgumentException("Cannot interpret '" + split[i] + "' (part of '" + input + "') as a number!");

        int[] values = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            int value = Integer.parseInt(split[i]);
            if (value < 0 || value >= rangeType.getMaximumValue())
                throw new IllegalArgumentException("The value '" + value + "' (part of '" + input + "') must be within the range of [0, " + rangeType.getMaximumValue() + ").");

            values[i] = value;
        }

        if (values.length == 1) {
            return new HashRange(rangeType, values[0], values[0]);
        } else {
            return new HashRange(rangeType, values[0], values[1]);
        }
    }

    @Override
    public String toString() {
        return "HashRange{" + this.rangeType + ","
                + (this.minValue != this.maxValue ? this.minValue + "-" + this.maxValue : this.minValue) + "}";
    }

    @Getter
    @RequiredArgsConstructor
    public enum HashRangeType {
        PSYQ(FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE),
        MSVC(FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE);

        private final int maximumValue;
    }
}
