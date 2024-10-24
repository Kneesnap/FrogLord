package net.highwayfrogs.editor.scripting.runtime;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;

/**
 * Represents a noodle primitive.
 */
public class NoodlePrimitive {
    private double numberValue;
    private String stringValue;
    private NoodleObjectInstance objectInstance;
    @Getter private NoodlePrimitiveType primitiveType;

    public NoodlePrimitive() { // Null primitive.
        setNull();
    }

    public NoodlePrimitive(String str) {
        setString(str);
    }

    public NoodlePrimitive(double num) {
        setNumber(num);
    }

    public NoodlePrimitive(boolean boolState) {
        this(boolState ? 1D : 0D);
    }

    public NoodlePrimitive(NoodleObjectInstance objectInstance) {
        setObjectReference(objectInstance);
    }

    /**
     * Gets the display name of the type.
     */
    public String getTypeDisplayName() {
        switch (this.primitiveType) {
            case NUMBER:
                return "Number";
            case STRING:
                return "String";
            case OBJECT_REFERENCE:
                if (this.objectInstance == null)
                    return "Null";

                return this.objectInstance.getTemplate().getName();
            default:
                throw new NoodleRuntimeException("Don't know how to get the primitive type %s (of %s) as a string!", this.primitiveType, this);
        }
    }

    /**
     * Clones the primitive.
     */
    public NoodlePrimitive clone() {
        NoodlePrimitive newPrimitive = new NoodlePrimitive();
        newPrimitive.numberValue = this.numberValue;
        newPrimitive.stringValue = this.stringValue;
        newPrimitive.objectInstance = this.objectInstance; // Do not increase ref count here.
        newPrimitive.primitiveType = this.primitiveType;
        return newPrimitive;
    }

    private void reset() {
        this.numberValue = 0;
        this.stringValue = null;
        this.objectInstance = null;
    }

    /**
     * Gets the string value.
     */
    public String getStringValue() {
        if (isNull())
            return null;
        if (!isString())
            throw new NoodleRuntimeException("Tried to get [%s] as a string value!", this);
        return this.stringValue;
    }

    /**
     * Gets the value as a boolean.
     */
    public boolean getBooleanValue() {
        return isNumber() && Math.abs(getNumberValue()) >= .00001D;
    }

    /**
     * Gets the numeric value.
     */
    public double getNumberValue() {
        if (!isNumber())
            throw new NoodleRuntimeException("Tried to get [%s] as a numeric value!", this);
        return this.numberValue;
    }

    /**
     * Gets the numeric value as an integer.
     * Throws if the value does not appear to be an integer.
     */
    public int getIntegerValue() {
        double number = getNumberValue();
        int intNumber = (int) Math.round(number);
        if (!isValidInteger(this.numberValue, intNumber))
            throw new NoodleRuntimeException("Tried to get [%s] as a numeric integer value!", this);
        return intNumber;
    }

    /**
     * Gets the numeric value as an integer.
     */
    public int getAsIntegerValue() {
        return (int) Math.round(getNumberValue());
    }

    /**
     * Tries to increase the refCount for the tracked object reference, if there is one.
     * @return Was successful
     */
    public boolean tryIncreaseRefCount() {
        if (isObjectReference() && this.objectInstance != null) {
            this.objectInstance.incrementRefCount();
            return true;
        }

        return false;
    }

    /**
     * Tries to decrease the refCount for the tracked object reference, if there is one.
     * @return Was successful?
     */
    public boolean tryDecreaseRefCount() {
        if (isObjectReference() && this.objectInstance != null) {
            this.objectInstance.decrementRefCount();
            return true;
        }

        return false;
    }

    /**
     * Called when a thread starts with this primitive as an argument.
     * @param thread The thread which started.
     */
    public void onThreadStartAsArgument(NoodleThread<?> thread) {
        if (isObjectReference() && this.objectInstance != null)
            this.objectInstance.onThreadStartAsArgument(thread);
    }

    /**
     * Gets the object reference.
     */
    public NoodleObjectInstance getObjectReference() {
        if (!isObjectReference())
            throw new NoodleRuntimeException("Tried to get [%s] as a NoodleObjectReference!", this);
        return this.objectInstance;
    }

    /**
     * Gets the object instance corresponding with the given template.
     * @param template The template to get the object with.
     * @param <TType> The type of the object expected.
     * @return objectInstance
     */
    public <TType> TType getOptionalObjectInstance(NoodleObjectTemplate<TType> template) {
        if (!isObjectReference())
            throw new NoodleRuntimeException("Cannot get NoodlePrimitive [%s] as an object reference.", this);

        return this.objectInstance != null ? this.objectInstance.getObjectInstance(template, true) : null;
    }

    /**
     * Gets the object instance corresponding with the given template.
     * @param template The template to get the object with.
     * @param <TType> The type of the object expected.
     * @return objectInstance
     */
    public <TType> TType getObjectInstance(NoodleObjectTemplate<TType> template) {
        if (!isObjectReference())
            throw new NoodleRuntimeException("Cannot get NoodlePrimitive [%s] as an object reference.", this);

        return this.objectInstance != null ? this.objectInstance.getObjectInstance(template, false) : null;
    }

    /**
     * Gets the object instance corresponding with the given template.
     * Throws an error if the value is null.
     * @param template The template to get the object with.
     * @param <TType> The type of the object expected.
     * @return objectInstance
     */
    public <TType> TType getRequiredObjectInstance(NoodleObjectTemplate<TType> template, String source) {
        if (!isObjectReference())
            throw new NoodleRuntimeException("Cannot get NoodlePrimitive [%s] as an object reference.", this);

        TType objectInstance = this.objectInstance != null ? this.objectInstance.getObjectInstance(template, false) : null;
        if (objectInstance == null)
            throw new NoodleRuntimeException("Expected a(n) %s Noodle object %s, but instead got null.", template.getName(), source);

        return objectInstance;
    }

    /**
     * Gets the string value (if this is a string value) as an enum.
     * @param enumClass The enum class to get the value form.
     * @return enumValue
     */
    public <E extends Enum<E>> E getStringValueAsEnum(Class<E> enumClass) {
        return getStringValueAsEnum(enumClass, false);
    }

    /**
     * Gets the string value (if this is a string value) as an enum.
     * @param enumClass The enum class to get the value form.
     * @return enumValue
     */
    public <E extends Enum<E>> E getStringValueAsEnum(Class<E> enumClass, boolean allowNull) {
        String strValue = getStringValue();
        if (allowNull && (strValue == null || strValue.length() == 0))
            return null; // Null has been intentionally supplied.

        E enumValue = null;

        try {
            enumValue = Enum.valueOf(enumClass, strValue);
        } catch (Throwable th) {
            // Abort!
        }

        if (enumValue == null) // There was an enum name supplied, but it wasn't a valid enum.
            throw new NoodleRuntimeException("Could not get `%s` as a(n) `%s` enum.", strValue, enumClass.getSimpleName());

        return enumValue;
    }

    /**
     * Sets this as a string and sets the string value.
     * @param strValue The string value.
     */
    public void setString(String strValue) {
        if (strValue == null) {
            setNull();
            return;
        }

        reset();
        this.primitiveType = NoodlePrimitiveType.STRING;
        this.stringValue = strValue;
    }

    /**
     * Sets this as a number and sets the numeric value.
     * @param numValue The numeric value.
     */
    public void setNumber(double numValue) {
        reset();
        this.primitiveType = NoodlePrimitiveType.NUMBER;
        this.numberValue = numValue;
    }

    /**
     * Sets this as a number and sets the numeric value.
     * @param objectInstance The object instance.
     */
    public void setObjectReference(NoodleObjectInstance objectInstance) {
        reset();
        this.primitiveType = NoodlePrimitiveType.OBJECT_REFERENCE;
        this.objectInstance = objectInstance;
    }

    /**
     * Sets the NoodlePrimitive as representing null.
     */
    public void setNull() {
        setObjectReference(null);
    }

    /**
     * Tests if the value this primitive represents is a string.
     */
    public boolean isString() {
        return this.primitiveType == NoodlePrimitiveType.STRING;
    }

    /**
     * Tests if the value this primitive represents is a number.
     */
    public boolean isNumber() {
        return this.primitiveType == NoodlePrimitiveType.NUMBER;
    }

    /**
     * Tests if the value this primitive represents is an integer.
     */
    public boolean isInteger() {
        return isNumber() && isValidInteger(this.numberValue);
    }

    /**
     * Tests if the value this primitive represents is a boolean.
     */
    public boolean isBoolean() {
        if (!isInteger())
            return false;

        int intValue = getIntegerValue();
        return (intValue == 0) || (intValue == 1);
    }

    /**
     * Tests if this primitive is a pseudo-pointer to an object instance.
     */
    public boolean isObjectReference() {
        return this.primitiveType == NoodlePrimitiveType.OBJECT_REFERENCE;
    }

    /**
     * Tests if this primitive represents null.
     */
    public boolean isNull() {
        return this.primitiveType == NoodlePrimitiveType.OBJECT_REFERENCE && (this.objectInstance == null);
    }

    private static boolean isValidInteger(double numberValue) {
        int intNumber = (int) Math.round(numberValue);
        return isValidInteger(numberValue, intNumber);
    }

    private static boolean isValidInteger(double numberValue, int intNumber) {
        return Math.abs(numberValue - intNumber) <= .00001;
    }

    /**
     * Gets the string of this which can be added to another string.
     * @return addString
     */
    public String getAsString() {
        if (isString())
            return this.stringValue;

        if (isNumber())
            return Utils.doubleToCleanString(this.numberValue);

        if (isObjectReference())
            return this.objectInstance != null && this.objectInstance.getObject() != null ? this.objectInstance.getObject().toString() : "null";

        return super.toString();
    }

    @Override
    public String toString() {
        if (isString()) {
            return "STRING=\"" + NoodleUtils.compiledStringToCodeString(this.stringValue) + "\"";
        } else if (isNumber()) {
            return "NUMBER=" + Utils.doubleToCleanString(this.numberValue);
        } else if (isObjectReference()) {
            return "OBJECT=" + (this.objectInstance != null && this.objectInstance.getObject() != null ? this.objectInstance.getObject().toString() : "null");
        } else {
            throw new NoodleRuntimeException("Don't know how to toString() %s.", this.primitiveType);
        }
    }

    /**
     * Do a value equality test between this and another primitive.
     * @param other The primitive to test against.
     * @return true, iff the value equality check passes.
     */
    public boolean valueEquals(NoodlePrimitive other) {
        if (other == null || other.isNull()) {
            return isNull(); // I don't think this should occur, but regardless, doesn't hurt to test.
        } else if (isNull()) {
            return false; // This is null, and the other one is not, so they must not be equal.
        }

        if (getPrimitiveType() != other.getPrimitiveType()) {
            // Mismatched type.
            return false;
        } else if (!isObjectReference()) {
            // Not an object type, so we can safely use normal equals().
            return equals(other);
        } else {
            @SuppressWarnings("unchecked") NoodleObjectTemplate<Object> template = (NoodleObjectTemplate<Object>) getObjectReference().getTemplate();
            if (template != other.getObjectReference().getTemplate())
                return false; // The objects are different types.

            return template != null && template.areObjectContentsEqual(getObjectReference().getOptionalObjectInstance(Object.class), other.getObjectReference().getOptionalObjectInstance(Object.class));
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NoodlePrimitive))
            return false;

        NoodlePrimitive otherPrim = (NoodlePrimitive) other;
        if (otherPrim.getPrimitiveType() != getPrimitiveType())
            return false; // Make sure match.

        if (isString()) {
            return Objects.equals(this.stringValue, otherPrim.stringValue); // We use Objects.equals so this check is null-safe.
        } else if (isNumber()) {
            return this.numberValue == otherPrim.numberValue;
        } else if (isObjectReference()) {
            return this.objectInstance == otherPrim.objectInstance;
        } else {
            throw new NoodleRuntimeException("Could not test if two %s primitives were equal.", this.primitiveType);
        }
    }

    /**
     * Return if this should be considered "true".
     * @return isTrue
     */
    public boolean isTrueValue() {
        if (isString())
            return this.stringValue != null;

        if (isNumber())
            return this.numberValue != 0D;

        if (isObjectReference())
            return this.objectInstance != null;

        return false;
    }

    /**
     * Gets the argument types as a display string.
     * @param args the argument types to get as a display string.
     * @return argumentDisplayTypes
     */
    public static String getArgumentDisplayTypesAsString(NoodlePrimitive[] args) {
        if (args == null)
            return "";

        StringBuilder builder = new StringBuilder();
        getArgumentDisplayTypesAsString(builder, args);
        return builder.toString();
    }

    /**
     * Gets the argument types as a string for display. Eg: "String, GameInstance, Number"
     * @param builder the builder to write to
     * @param args the argument types to get as a display string.
     */
    public static void getArgumentDisplayTypesAsString(StringBuilder builder, NoodlePrimitive[] args) {
        if (builder == null)
            throw new NullPointerException("builder");
        if (args == null)
            return;

        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                builder.append(", ");

            builder.append(args[i] != null ? args[i].getTypeDisplayName() : "Null");
        }
    }
}