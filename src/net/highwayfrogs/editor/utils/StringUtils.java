package net.highwayfrogs.editor.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;

/**
 * Contains static utilities for performing operations on strings.
 * Created by Kneesnap on 10/25/2024.
 */
public class StringUtils {
    /**
     * Capitalize every letter after a space.
     * @param sentence The sentence to capitalize.
     * @return capitalized
     */
    public static String capitalize(String sentence) {
        String[] split = sentence.replaceAll("_", " ").split(" ");
        List<String> out = new ArrayList<>();
        for (String s : split)
            out.add(s.length() > 0 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : "");
        return String.join(" ", out);
    }

    /**
     * Format a string safely. If the string is unable to be formatted, an exception will be printed, but not thrown, and the original template returned (with arguments shown separately.)
     * @param template The string to format.
     * @param args The arguments to format it with.
     * @return formattedString
     */
    public static String formatStringSafely(String template, Object... args) {
        if (args == null || args.length == 0)
            return template;

        try {
            return String.format(template, args);
        } catch (IllegalFormatException ife) {
            Utils.handleError(null, ife, false, "Could not format the string/message template '%s'.", template);

            try {
                return template + " (FORMATTING ERROR FOR: [" + Arrays.toString(args) + "])";
            } catch (Throwable th) {
                return template + " (FORMATTING ERROR)";
            }
        }
    }

    /**
     * Tests if a string is alphanumeric or not.
     * @param testString The string to test.
     * @return isAlphanumeric
     */
    public static boolean isAlphanumeric(String testString) {
        for (int i = 0; i < testString.length(); i++)
            if (!Character.isLetterOrDigit(testString.charAt(i)))
                return false;
        return true;
    }

    /**
     * Test if the input string is null or empty.
     * @param input The string to test.
     * @return True if the string is null or empty.
     */
    public static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }

    /**
     * Test if the input string is null or whitespace.
     * @param input The string to test.
     * @return True if the string is null or empty.
     */
    public static boolean isNullOrWhiteSpace(String input) {
        return input == null || input.isEmpty() || input.trim().isEmpty();
    }

    /**
     * Pads the number with 0s as a string.
     * @param baseStr The string to pad.
     * @param targetLength The target string size.
     * @return paddedString
     */
    public static String padStringLeft(String baseStr, int targetLength, char toAdd) {
        StringBuilder prependStr = new StringBuilder();
        while (targetLength > prependStr.length() + baseStr.length())
            prependStr.append(toAdd);
        return prependStr.append(baseStr).toString();
    }

    /**
     * Replace instances of duplicate characters with one of that character.
     * @param str       The string to modify.
     * @param character The character to replace.
     * @return cleanString
     */
    public static String replaceDouble(String str, char character) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char temp = str.charAt(i);
            output.append(temp);
            if (temp == character) // Skip the double characters.
                while (str.length() > i + 1 && str.charAt(i + 1) == character)
                    i++;
        }

        return output.toString();
    }

    /**
     * Removes duplicate spaces from a given string.
     * @param toRemove The string to remove spaces from.
     * @return cleanStr
     */
    public static String removeDuplicateSpaces(String toRemove) {
        return replaceDouble(toRemove, ' ').replaceAll("(\\s+)$", ""); // Removes trailing space.
    }

    /**
     * Returns the string with all non-alphanumeric characters removed.
     * @param input The input to strip.
     * @return strippedStr
     */
    public static String stripAlphanumeric(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char temp = input.charAt(i);
            if (Character.isLetterOrDigit(temp))
                output.append(temp);
        }
        return output.toString();
    }

    /**
     * Trim whitespace at the start of the string.
     * @param input the input string to trim
     * @return trimmedString
     */
    public static String trimStart(String input) {
        if (input == null)
            return null;

        int inputLength = input.length();
        int startIndex = 0;
        while ((startIndex < inputLength) && (input.charAt(startIndex) <= ' '))
            startIndex++;

        return (startIndex > 0) ? input.substring(startIndex) : input;
    }

    /**
     * Trim whitespace at the end of the string.
     * @param input the input string to trim
     * @return trimmedString
     */
    public static String trimEnd(String input) {
        if (input == null)
            return null;

        int len = input.length();
        while ((len > 0) && (input.charAt(len - 1) <= ' '))
            len--;

        return (len < input.length()) ? input.substring(0, len) : input;
    }
}
