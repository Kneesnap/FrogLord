package net.highwayfrogs.editor.utils.objects;

import lombok.Getter;
import net.highwayfrogs.editor.system.Config.IllegalConfigSyntaxException;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

/**
 * This is a string which represents a value that can be obtained as .
 * Created by Kneesnap on 11/4/2024.
 */
public class StringNode {
    protected String value;
    @Getter protected boolean surroundByQuotes; // Whether the value should be surrounded by double quotes on save or not.

    public StringNode() {
        this(null);
    }

    public StringNode(String value) {
        setAsStringLiteral(value);
    }

    public StringNode(String value, boolean surroundByQuotes) {
        this.value = value;
        this.surroundByQuotes = surroundByQuotes;
    }

    /**
     * Creates a copy of this object.
     * @return nodeCopy
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public StringNode clone() {
        return new StringNode(this.value, this.surroundByQuotes);
    }

    /**
     * Returns true iff the value is equal to null.
     */
    public boolean isNull() {
        return this.value == null;
    }

    /**
     * Marks the string node as null.
     */
    public void setNull() {
        this.value = null;
        this.surroundByQuotes = false;
    }

    /**
     * Gets the node value as a string.
     * @return stringValue
     */
    public String getAsString() {
        return this.value;
    }

    /**
     * Gets the node value as a string.
     * @param fallback the text to return if the string is null/empty.
     * @return stringValueOrFallback
     */
    public String getAsString(String fallback) {
        return this.value != null && this.value.length() > 0 ? this.value : fallback;
    }

    /**
     * Gets the string value, surrounded by quotes if necessary.
     */
    public String getAsStringLiteral() {
        if (!this.surroundByQuotes && !"null".equals(this.value))
            return this.value;

        if (this.value != null) {
            return '"' + this.value
                    .replace("\\", "\\\\")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                    .replace("\"", "\\\"") + '"';
        } else {
            return "null";
        }
    }

    /**
     * Sets the node value as a string.
     * @param newValue the new string value
     */
    public void setAsString(String newValue, boolean surroundByQuotes) {
        this.value = newValue;
        this.surroundByQuotes = surroundByQuotes;
    }

    /**
     * Parses the string literal, and applies it to this node.
     * @param literal the string literal to read
     */
    public void setAsStringLiteral(String literal) {
        if (literal == null || literal.length() < 2 || !literal.startsWith("\"") || !literal.endsWith("\"")) {
            setAsString(literal, false);
        } else {
            parseStringLiteral(literal);
        }
    }

    /**
     * Gets the node value as a boolean.
     * @return boolValue
     * @throws IllegalConfigSyntaxException Thrown if the node data is not a valid boolean.
     */
    public boolean getAsBoolean() {
        if ("true".equalsIgnoreCase(this.value)
                || "yes".equalsIgnoreCase(this.value)
                || "1".equalsIgnoreCase(this.value))
            return true;

        if ("false".equalsIgnoreCase(this.value)
                || "no".equalsIgnoreCase(this.value)
                || "0".equalsIgnoreCase(this.value))
            return false;

        throw new IllegalConfigSyntaxException("Don't know how to interpret '" + this.value + "' as a boolean." + getExtraDebugErrorInfo());
    }

    /**
     * Gets the node value as a boolean, or return a default value if no value is present.
     * @return boolValue
     * @throws IllegalConfigSyntaxException Thrown if the node data is not a valid boolean.
     */
    public boolean getAsBoolean(boolean defaultValue) {
        if (StringUtils.isNullOrWhiteSpace(this.value))
            return defaultValue;

        if ("true".equalsIgnoreCase(this.value)
                || "yes".equalsIgnoreCase(this.value)
                || "1".equalsIgnoreCase(this.value))
            return true;

        if ("false".equalsIgnoreCase(this.value)
                || "no".equalsIgnoreCase(this.value)
                || "0".equalsIgnoreCase(this.value))
            return false;

        throw new IllegalConfigSyntaxException("Don't know how to interpret '" + this.value + "' as a boolean." + getExtraDebugErrorInfo());
    }

    /**
     * Sets the node value to a boolean.
     * @param newValue The new value.
     */
    public void setAsBoolean(boolean newValue) {
        this.value = newValue ? "true" : "false";
        this.surroundByQuotes = false;
    }

    /**
     * Gets the node value as a short.
     * @return shortValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either a short or a hex short.
     */
    public short getAsShort() {
        int value = getAsInteger();
        if (value < Short.MIN_VALUE || value > 0xFFFF)
            throw new IllegalConfigSyntaxException("Value '" + this.value + "' cannot be represented as a 16-bit short number." + getExtraDebugErrorInfo());

        return (short) value;
    }

    /**
     * Gets the node value as a short.
     * @param fallback the number to return if there is no value.
     * @return shortValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either a short or a hex short.
     */
    public short getAsShort(short fallback) {
        int value = getAsInteger(fallback);
        if (value < Short.MIN_VALUE || value > 0xFFFF)
            throw new IllegalConfigSyntaxException("Value '" + this.value + "' cannot be represented as a 16-bit short number." + getExtraDebugErrorInfo());

        return (short) value;
    }

    /**
     * Sets the node value to a short.
     * @param newValue The new value.
     */
    public void setAsShort(short newValue) {
        this.value = Short.toString(newValue);
        this.surroundByQuotes = false;
    }

    /**
     * Gets the node value as an integer.
     * @return intValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
     */
    public int getAsInteger() {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return NumberUtils.parseIntegerAllowHex(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer." + getExtraDebugErrorInfo(), nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer." + getExtraDebugErrorInfo());
    }

    /**
     * Gets the node value as an integer.
     * @param fallback the number to return if there is no value.
     * @return intValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
     */
    public int getAsInteger(int fallback) {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return NumberUtils.parseIntegerAllowHex(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer." + getExtraDebugErrorInfo(), nfe);
            }
        }

        return fallback;
    }

    /**
     * Sets the node value to an integer.
     * @param newValue The new value.
     */
    public void setAsInteger(int newValue) {
        this.value = Integer.toString(newValue);
        this.surroundByQuotes = false;
    }

    /**
     * Gets the node value as an integer.
     * @return intValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
     */
    public int getAsUnsignedInteger() {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return NumberUtils.isPrefixedHexInteger(this.value) ? NumberUtils.parseIntegerAllowHex(this.value) : (int) Long.parseLong(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.", nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer." + getExtraDebugErrorInfo());
    }

    /**
     * Gets the node value as an integer with potentially unsigned bytes.
     * @param fallback the number to return if there is no value.
     * @return intValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
     */
    public int getAsUnsignedInteger(int fallback) {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return NumberUtils.isPrefixedHexInteger(this.value) ? NumberUtils.parseIntegerAllowHex(this.value) : (int) Long.parseLong(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer." + getExtraDebugErrorInfo(), nfe);
            }
        }

        return fallback;
    }

    /**
     * Sets the node value to an unsigned integer.
     * @param newValue The new value.
     */
    public void setAsUnsignedInteger(int newValue) {
        this.value = Long.toString(newValue & 0xFFFFFFFFL);
        this.surroundByQuotes = false;
    }

    /**
     * Gets the node value as a float.
     * @return floatValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as a float.
     */
    public float getAsFloat() {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return Float.parseFloat(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number." + getExtraDebugErrorInfo(), nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number." + getExtraDebugErrorInfo());
    }

    /**
     * Gets the node value as a float.
     * @param fallback the number to return if there is no value.
     * @return floatValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as a float.
     */
    public float getAsFloat(float fallback) {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return Float.parseFloat(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number." + getExtraDebugErrorInfo(), nfe);
            }
        }

        return fallback;
    }

    /**
     * Sets the node value to a float.
     * @param newValue The new value.
     */
    public void setAsFloat(float newValue) {
        this.value = Float.toString(newValue);
        this.surroundByQuotes = false;
    }

    /**
     * Gets the node value as a double.
     * @return doubleValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as a double.
     */
    public double getAsDouble() {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return Double.parseDouble(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number." + getExtraDebugErrorInfo(), nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number." + getExtraDebugErrorInfo());
    }

    /**
     * Gets the node value as a double.
     * @param fallback the number to return if there is no value.
     * @return doubleValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as a double.
     */
    public double getAsDouble(double fallback) {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return Double.parseDouble(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number." + getExtraDebugErrorInfo(), nfe);
            }
        }

        return fallback;
    }

    /**
     * Sets the node value to a double.
     * @param newValue The new value.
     */
    public void setAsDouble(double newValue) {
        this.value = Double.toString(newValue);
        this.surroundByQuotes = false;
    }

    /**
     * Gets the value as an enum.
     * @return enumValue
     * @throws IllegalConfigSyntaxException Thrown if the value was not a valid enum or a valid enum index.
     */
    public <TEnum extends Enum<TEnum>> TEnum getAsEnum(Class<TEnum> enumClass) {
        if (StringUtils.isNullOrEmpty(this.value))
            return null;

        if (NumberUtils.isInteger(this.value)) {
            int enumOrdinal = Integer.parseInt(this.value);
            return enumClass.getEnumConstants()[enumOrdinal];
        }

        try {
            return Enum.valueOf(enumClass, this.value.toUpperCase());
        } catch (Exception e) {
            try {
                return Enum.valueOf(enumClass, this.value);
            } catch (Exception e2) {
                throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpreted as an enum value from " + enumClass.getSimpleName() + "." + getExtraDebugErrorInfo(), e2);
            }
        }
    }

    /**
     * Gets the value as an enum.
     * @return enumValue
     * @throws IllegalConfigSyntaxException Thrown if the value was not a valid enum or a valid enum index.
     */
    public <TEnum extends Enum<TEnum>> TEnum getAsEnumOrError(Class<TEnum> enumClass) {
        TEnum value = getAsEnum(enumClass);
        if (value == null)
            throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpreted as an enum value from " + enumClass.getSimpleName() + "." + getExtraDebugErrorInfo());

        return value;
    }


    /**
     * Gets the value as an enum.
     * @return enumValue
     * @throws IllegalConfigSyntaxException Thrown if the value was not a valid enum or a valid enum index.
     */
    public <TEnum extends Enum<TEnum>> TEnum getAsEnum(TEnum defaultEnum) {
        if (defaultEnum == null)
            throw new NullPointerException("defaultEnum");

        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            if (NumberUtils.isInteger(this.value)) {
                int enumOrdinal = Integer.parseInt(this.value);
                return defaultEnum.getDeclaringClass().getEnumConstants()[enumOrdinal];
            }

            try {
                return Enum.valueOf(defaultEnum.getDeclaringClass(), this.value);
            } catch (Exception e) {
                throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpreted as an enum value from " + defaultEnum.getDeclaringClass().getSimpleName() + "." + getExtraDebugErrorInfo(), e);
            }
        }

        return defaultEnum;
    }

    /**
     * Sets the node value to an enum.
     * @param newValue The new value.
     * @param <TEnum> The enum value type
     */
    public <TEnum extends Enum<TEnum>> void setAsEnum(TEnum newValue) {
        this.value = newValue != null ? newValue.name() : null;
        this.surroundByQuotes = false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + getAsStringLiteral() + "}";
    }

    /**
     * Parses the string literal from an input string.
     * @param input the string to parse
     */
    public void parseStringLiteral(String input) {
        parseStringLiteral(new SequentialStringReader(input), new StringBuilder(), true, false);
    }

    /**
     * Returns extra info for debugging, such as line numbers.
     */
    protected String getExtraDebugErrorInfo() {
        return "";
    }

    /**
     * Parse a string literal from a string.
     * @param reader the reader to read from
     * @param result the result buffer to store temporary stuff within
     * @param includeWhitespace if whitespace should be included
     * @param treatCommaAsEndOfValue if a comma outside a quoted string should be treated as the end of the value
     */
    public void parseStringLiteral(SequentialStringReader reader, StringBuilder result, boolean includeWhitespace, boolean treatCommaAsEndOfValue) {
        result.setLength(0);

        boolean isEscape = false;
        boolean isQuotationString = false;
        boolean isQuotationOpen = false;
        char lastChar = ' '; // The last character is usually whitespace.
        while (reader.hasNext()) {
            char tempChar = reader.read();

            if (result.length() == 0 && !isEscape && tempChar == '"') { // First character.
                isQuotationString = isQuotationOpen = true;
            } else if (isQuotationOpen) {
                if (isEscape) {
                    isEscape = false;
                    if (tempChar == '\\' || tempChar == '"') {
                        result.append(tempChar);
                    } else if (tempChar == 'n') {
                        result.append('\n');
                    } else if (tempChar == 'r') {
                        result.append('\r');
                    } else if (tempChar == 't') {
                        result.append('\t');
                    } else {
                        String displayStr = result.toString();
                        if (displayStr.length() > 16)
                            displayStr = displayStr.substring(0, 16) + "...";

                        throw new RuntimeException("The argument beginning with '" + displayStr + "' contains an invalid escape sequence '\\" + tempChar + "'.");
                    }
                } else if (tempChar == '"') {
                    isQuotationOpen = false;
                    break;
                } else if (tempChar == '\\') {
                    isEscape = true;
                } else {
                    result.append(tempChar);
                }
            } else if (includeWhitespace && Character.isWhitespace(lastChar) && tempChar == '-' && reader.hasNext() && reader.peek() == '-') {
                reader.setIndex(reader.getIndex() - 1);
                break;
            } else if (Character.isWhitespace(tempChar) || (treatCommaAsEndOfValue && tempChar == ',' && result.length() > 0)) {
                if (result.length() > 0) // If there's whitespace and this is at the start, just skip it.
                    break;
            } else {
                result.append(tempChar);
            }

            lastChar = tempChar;
        }

        if (isQuotationOpen) {
            String displayStr = result.toString();
            if (displayStr.length() > 16)
                displayStr = displayStr.substring(0, 16) + "...";

            throw new RuntimeException("The " + Utils.getSimpleName(this) + " beginning with '" + displayStr + "' is never finished/terminated!");
        }

        if (isQuotationString || !"null".contentEquals(result)) {
            this.value = result.toString();
            this.surroundByQuotes = isQuotationString;
        } else {
            this.value = null;
            this.surroundByQuotes = false;
        }
    }
}
