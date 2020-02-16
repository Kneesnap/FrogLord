package net.highwayfrogs.editor.file.patch;

import javafx.scene.paint.Color;
import lombok.Getter;

/**
 * Represents a variable value.
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchValue {
    @Getter private PatchArgumentType type;
    private Object value;

    public PatchValue(PatchArgumentType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public boolean isInteger() {
        return this.type == PatchArgumentType.INT;
    }

    public boolean isDecimal() {
        return this.type == PatchArgumentType.DECIMAL;
    }

    public boolean isString() {
        return this.type == PatchArgumentType.STRING;
    }

    public boolean isColor() {
        return this.type == PatchArgumentType.COLOR;
    }

    public boolean isBoolean() {
        return this.type == PatchArgumentType.BOOLEAN;
    }

    public int getAsInteger() {
        if (!isInteger())
            throw new RuntimeException("Cannot get integer for type " + this.type + ". (" + toString() + ")");
        return (Integer) this.value;
    }

    public String getAsString() {
        if (!isString())
            throw new RuntimeException("Cannot get string for type " + this.type + ". (" + toString() + ")");
        return (String) this.value;
    }

    public Color getAsColor() {
        if (!isColor())
            throw new RuntimeException("Cannot get color for type " + this.type + ". (" + toString() + ")");
        return (Color) this.value;
    }

    public double getAsDecimal() {
        if (!isDecimal())
            throw new RuntimeException("Cannot get decimal for type " + this.type + ". (" + toString() + ")");
        return (Double) this.value;
    }

    public boolean getAsBoolean() {
        if (!isBoolean())
            throw new RuntimeException("Cannot get boolean for type " + this.type + ". (" + toString() + ")");
        return (Boolean) this.value;
    }

    public void setInteger(int newValue) {
        this.type = PatchArgumentType.INT;
        this.value = newValue;
    }

    public void setDecimal(double newValue) {
        this.type = PatchArgumentType.DECIMAL;
        this.value = newValue;
    }

    public void setString(String newValue) {
        this.type = PatchArgumentType.STRING;
        this.value = newValue;
    }

    public void setColor(Color newValue) {
        this.type = PatchArgumentType.COLOR;
        this.value = newValue;
    }

    public void setBoolean(Boolean newValue) {
        this.type = PatchArgumentType.BOOLEAN;
        this.value = newValue;
    }

    public void setObject(Object value) {
        if (value instanceof Integer) {
            setInteger((Integer) value);
        } else if (value instanceof Double) {
            setDecimal((Double) value);
        } else if (value instanceof String) {
            setString((String) value);
        } else if (value instanceof Color) {
            setColor((Color) value);
        } else if (value instanceof Boolean) {
            setBoolean((Boolean) value);
        } else {
            throw new RuntimeException("Don't know how to set variable to " + value + ".");
        }
    }

    @Override
    public String toString() {
        return this.type.getBehavior().safeValueToString(this.value);
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof PatchValue) && other.toString().equals(toString());
    }

    /**
     * Parse a string as a patch value. Returns null if it can't be read as a patch value.
     * @param patchStr The value to read.
     * @return patchValue
     */
    public static PatchValue parseStringAsPatchValue(String patchStr) {
        PatchArgumentType type = null;
        for (PatchArgumentType testType : PatchArgumentType.values()) {
            if (testType.getBehavior().isValidString(patchStr)) {
                type = testType;
                break;
            }
        }

        return type != null ? new PatchValue(type, type.getBehavior().parseString(patchStr)) : null;
    }
}
