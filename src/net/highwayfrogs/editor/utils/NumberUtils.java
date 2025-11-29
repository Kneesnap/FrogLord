package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.Constants;

/**
 * Contains static utilities for working with numbers.
 * Created by Kneesnap on 10/25/2024.
 */
public class NumberUtils {
    private static final double BASE_16_LOGARITHM_CONVERTER = Math.log10(16);

    /**
     * Test if a string is an integer.
     * @param str The string to test.
     * @return isInteger
     */
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty() || str.equals("-"))
            return false;

        int startIndex = str.startsWith("-") ? 1 : 0;
        for (int i = startIndex; i < str.length(); i++)
            if (Character.digit(str.charAt(i), 10) < 0)
                return false;
        return true;
    }

    /**
     * Test if a string is a hex number.
     * @param str The string to test.
     * @return isHexInteger
     */
    public static boolean isPrefixedHexInteger(String str) {
        return str != null && str.startsWith("0x") && isHexInteger(str);
    }

    /**
     * Test if a string is a hex number.
     * @param str The string to test.
     * @return isHexInteger
     */
    public static boolean isHexInteger(String str) {
        if (str == null || str.isEmpty())
            return false;

        int startIndex = str.startsWith("0x") ? "0x".length() : 0;
        for (int i = startIndex; i < str.length(); i++)
            if (!isHexDigit(str.charAt(i)))
                return false;

        return true;
    }

    /**
     * Tests if the given character is a valid hexadecimal digit.
     * @param input the character to test
     * @return true iff the character is a hexadecimal digit
     */
    public static boolean isHexDigit(char input) {
        return (input >= '0' && input <= '9') || (input >= 'a' && input <= 'f') || (input >= 'A' && input <= 'F');
    }

    /**
     * Parse a hex integer string into a 32 bit signed integer.
     * @param str The string to parse.
     * @return parsedNumber
     */
    public static int parseIntegerAllowHex(String str) {
        // We need to use Long.parseLong() since Integer.parseInt() will not accept values larger than 0x80000000.
        return str.startsWith("0x") ? parseHexInteger(str) : Integer.parseInt(str);
    }

    /**
     * Parse a hex integer string into a 32 bit signed integer, which may optionally be prefixed with "0x".
     * @param input The string to parse.
     * @return parsedNumber
     */
    public static int parseHexInteger(String input) {
        if (input.startsWith("0x"))
            input = input.substring(2);

        // We need to use Long.parseLong() since Integer.parseInt() will not accept values larger than 0x80000000.
        long value = Long.parseLong(input, 16);
        if (value > 0xFFFFFFFFL || value < -0xFFFFFFFFL)
            throw new ArithmeticException("The value: " + value + " (" + Long.toHexString(value) + ") cannot be represented as a 32-bit integer. (Too large/small!)");

        return (int) value;
    }

    /**
     * Test if a string is a signed short.
     * @param str The string to test.
     * @return isSignedShort
     */
    public static boolean isSignedShort(String str) {
        if (!isInteger(str))
            return false;

        int intTest = Integer.parseInt(str);
        return intTest >= Short.MIN_VALUE && intTest <= Short.MAX_VALUE;
    }

    /**
     * Test if a string is an unsigned byte.
     * @param str The string to test.
     * @return isUnsignedByte
     */
    public static boolean isUnsignedByte(String str) {
        if (!isInteger(str))
            return false;

        int intTest = Integer.parseInt(str);
        return intTest >= 0 && intTest <= 0xFF;
    }

    /**
     * Is the input string a valid integer or decimal number?
     * @param input The input to test.
     * @return isNumber
     */
    public static boolean isNumber(String input) {
        boolean hasDecimal = false;
        boolean validNumber = false;
        for (int i = 0; i < input.length(); i++) {
            char test = input.charAt(i);
            if (test == '-' && i == 0)
                continue; // Allow negative indicator.

            if (test == '.') {
                if (!hasDecimal) {
                    hasDecimal = true;
                    continue;
                } else {
                    return false; // Multiple decimal = invalid number.
                }
            }

            if (!Character.isDigit(test))
                return false; // Character isn't a digit, so it can't be a number.

            validNumber = true;
        }

        return validNumber;
    }

    /**
     * Turn an integer into a hex string.
     * 255 -> 0xFF
     * @param value The value to convert.
     * @return hexString
     */
    public static String toHexString(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    /**
     * Turn a long into a hex string.
     * 255 -> 0xFF
     * @param value The value to convert.
     * @return hexString
     */
    public static String toHexString(long value) {
        return "0x" + Long.toHexString(value).toUpperCase();
    }

    /**
     * Pads the number with 0s as a string.
     * @param number The number to pad.
     * @param digits The number of digits to include.
     * @return paddedString
     */
    public static String padNumberString(int number, int digits) {
        int usedDigits = (number == 0) ? 1 : (int) Math.log10(number) + 1;

        StringBuilder prependStr = new StringBuilder();
        for (int i = 0; i < (digits - usedDigits); i++)
            prependStr.append("0");

        return prependStr.toString() + number;
    }

    /**
     * Returns a value as a hex string with leading 0s included.
     * @param value The value to get as a hex string.
     * @return hexString
     */
    public static String to0PrefixedHexString(short value) {
        return StringUtils.padStringLeft(Integer.toHexString(value & 0xFFFF).toUpperCase(), Constants.SHORT_SIZE * 2, '0');
    }

    /**
     * Returns a value as a hex string with leading 0s included.
     * @param value The value to get as a hex string.
     * @return hexString
     */
    public static String to0PrefixedHexString(int value) {
        return StringUtils.padStringLeft(Integer.toHexString(value).toUpperCase(), 8, '0');
    }

    /**
     * Returns a value as a hex string with leading 0s included.
     * @param value The value to get as a hex string.
     * @return hexString
     */
    public static String to0PrefixedHexString(int value, int digitCount) {
        return StringUtils.padStringLeft(Integer.toHexString(value).toUpperCase(), digitCount, '0');
    }

    /**
     * Returns a value as a hex string with leading 0s included.
     * @param value The value to get as a hex string.
     * @return hexString
     */
    public static String to0PrefixedHexStringLower(int value) {
        return StringUtils.padStringLeft(Integer.toHexString(value).toLowerCase(), 8, '0');
    }

    /**
     * Gets the clean string of a double value.
     * Examples:
     * - 12.34 -> "12.34"
     * - 12.0  -> "12"
     * @param doubleVal The value to get the string of.
     * @return numString
     */
    public static String doubleToCleanString(double doubleVal) {
        return ((int) doubleVal == doubleVal) ? String.valueOf((int) doubleVal) : String.valueOf(doubleVal);
    }

    /**
     * Gets the number of digits for a number.
     * @param number The number to get digits for.
     * @return digitCount
     */
    public static int getDigitCount(int number) {
        return (int) Math.max(Math.log10(number), 0) + 1;
    }

    /**
     * Gets the number of digits for a number.
     * @param number The number to get digits for.
     * @return digitCount
     */
    public static int getHexDigitCount(int number) {
        return (int) Math.max(Math.log10(number) / BASE_16_LOGARITHM_CONVERTER, 0) + 1;
    }
}
