package net.highwayfrogs.editor.utils.objects;

import lombok.Getter;
import net.highwayfrogs.editor.system.Config.IllegalConfigSyntaxException;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;

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
        this.value = value;
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

        throw new IllegalConfigSyntaxException("Don't know how to interpret '" + this.value + "' as a boolean.");
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
     * Gets the node value as an integer.
     * @return intValue
     * @throws IllegalConfigSyntaxException Thrown if the value is not formatted as either an integer or a hex integer.
     */
    public int getAsInteger() {
        if (!StringUtils.isNullOrWhiteSpace(this.value)) {
            try {
                return NumberUtils.isHexInteger(this.value) ? NumberUtils.parseHexInteger(this.value) : Integer.parseInt(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.", nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.");
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
                return NumberUtils.isHexInteger(this.value) ? NumberUtils.parseHexInteger(this.value) : Integer.parseInt(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.", nfe);
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
                return NumberUtils.isHexInteger(this.value) ? NumberUtils.parseHexInteger(this.value) : (int) Long.parseLong(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.", nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.");
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
                return NumberUtils.isHexInteger(this.value) ? NumberUtils.parseHexInteger(this.value) : (int) Long.parseLong(this.value);
            } catch (NumberFormatException nfe) {
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid integer.", nfe);
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
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.", nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.");
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
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.", nfe);
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
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.", nfe);
            }
        }

        throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.");
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
                throw new IllegalConfigSyntaxException("Value '" + this.value + "' is not a valid number.", nfe);
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
            return Enum.valueOf(enumClass, this.value);
        } catch (Exception e) {
            throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpreted as an enum value from " + enumClass.getSimpleName() + ".", e);
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
            throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpretted as an enum value from " + enumClass.getSimpleName() + ".");

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
                throw new IllegalConfigSyntaxException("The value '" + this.value + "' could not be interpreted as an enum value from " + defaultEnum.getDeclaringClass().getSimpleName() + ".", e);
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
}
