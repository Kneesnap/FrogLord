package net.highwayfrogs.editor.scripting.runtime;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleUtils;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleWrapperTemplate;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;

/**
 * Represents a noodle primitive.
 */
public class NoodlePrimitive {
    private long integerValue;
    private double decimalValue;
    private NoodleObjectInstance objectInstance;
    @Getter private NoodlePrimitiveType primitiveType;

    public NoodlePrimitive() { // Null primitive.
        setNull();
    }

    public NoodlePrimitive(char num) {
        setChar(num);
    }

    public NoodlePrimitive(byte num) {
        setByte(num);
    }

    public NoodlePrimitive(short num) {
        setShort(num);
    }

    public NoodlePrimitive(int num) {
        setInteger(num);
    }

    public NoodlePrimitive(long num) {
        setLong(num);
    }

    public NoodlePrimitive(float num) {
        setFloat(num);
    }

    public NoodlePrimitive(double num) {
        setDouble(num);
    }

    public NoodlePrimitive(boolean boolState) {
        setBoolean(boolState);
    }

    public NoodlePrimitive(Number number) {
        setNumber(number);
    }

    public NoodlePrimitive(NoodlePrimitiveType numberType, long value) {
        setWholeNumber(numberType, value);
    }

    public NoodlePrimitive(NoodlePrimitiveType numberType, double value) {
        setDecimal(numberType, value);
    }

    public NoodlePrimitive(NoodleObjectInstance objectInstance) {
        setObjectReference(objectInstance);
    }

    public NoodlePrimitive(String value) {
        if (value != null) {
            setObjectReference(new NoodleObjectInstance(null, value, NoodleWrapperTemplate.getCachedTemplate(String.class)));
        } else {
            setNull();
        }
    }

    /**
     * Gets the display name of the type.
     */
    public String getTypeDisplayName() {
        if (Objects.requireNonNull(this.primitiveType) == NoodlePrimitiveType.OBJECT_REFERENCE) {
            if (this.objectInstance == null)
                return "Null";

            return this.objectInstance.getTemplate().getName();
        }

        return StringUtils.capitalize(this.primitiveType.name());
    }

    /**
     * Clones the primitive.
     */
    public NoodlePrimitive clone() {
        NoodlePrimitive newPrimitive = new NoodlePrimitive();
        newPrimitive.integerValue = this.integerValue;
        newPrimitive.decimalValue = this.decimalValue;
        newPrimitive.objectInstance = this.objectInstance; // Do not increase ref count here.
        newPrimitive.primitiveType = this.primitiveType;
        return newPrimitive;
    }

    private void reset() {
        this.integerValue = 0;
        this.decimalValue = 0;
        this.objectInstance = null;
    }

    /**
     * Gets the value as a boolean.
     */
    public boolean getBoolean() {
        if (!isBoolean())
            throw new NoodleRuntimeException("Tried to use non-boolean [%s] as if it were a boolean!", this);

        return this.integerValue != 0;
    }

    /**
     * Gets the primitive value as a char.
     */
    public char getChar() {
        if (!isIntegerNumber())
            throw new NoodleRuntimeException("Tried to use non-integer [%s] as if it were a char!", this);
        return (char) this.integerValue;
    }

    /**
     * Gets the primitive value as a byte.
     */
    public byte getByte() {
        if (!isIntegerNumber())
            throw new NoodleRuntimeException("Tried to use non-integer [%s] as if it were a byte!", this);
        return (byte) this.integerValue;
    }

    /**
     * Gets the primitive value as a short.
     */
    public short getShort() {
        if (!isIntegerNumber())
            throw new NoodleRuntimeException("Tried to use non-integer [%s] as if it were a short!", this);
        return (short) this.integerValue;
    }

    /**
     * Gets the primitive value as an int.
     */
    public int getInteger() {
        if (!isIntegerNumber())
            throw new NoodleRuntimeException("Tried to use non-integer [%s] as if it were an integer!", this);
        return (int) this.integerValue;
    }

    /**
     * Gets the primitive value as a long.
     */
    public long getLong() {
        if (!isIntegerNumber())
            throw new NoodleRuntimeException("Tried to use non-integer [%s] as if it were a long!", this);
        return this.integerValue;
    }

    /**
     * Gets the primitive value as a float.
     */
    public float getFloat() {
        if (!isDecimalNumber())
            throw new NoodleRuntimeException("Tried to get non-decimal [%s] as if it were a double!", this);
        return (float) this.decimalValue;
    }

    /**
     * Gets the primitive value as a double.
     */
    public double getDouble() {
        if (!isDecimalNumber())
            throw new NoodleRuntimeException("Tried to get non-decimal [%s] as if it were a double!", this);
        return this.decimalValue;
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
     * Gets the contents of the primitive as a java Object.
     * @return rawObject
     */
    public Object getAsRawObject() {
        switch (this.primitiveType) {
            case BOOLEAN:
                return getBoolean();
            case CHAR:
                return getChar();
            case BYTE:
                return getByte();
            case SHORT:
                return getShort();
            case INTEGER:
                return getInteger();
            case LONG:
                return getLong();
            case FLOAT:
                return getFloat();
            case DOUBLE:
                return getDouble();
            case OBJECT_REFERENCE:
                return this.objectInstance != null ? this.objectInstance.getObject() : null;
            default:
                throw new NoodleRuntimeException("Cannot get rawObject for %s.");
        }
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
     * Gets the string value of the primitive.
     */
    public String getStringValue() {
        return getOptionalObjectInstance(NoodleWrapperTemplate.getCachedTemplate(String.class));
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
     * Sets the value represented by this primitive object to the provided boolean.
     * @param value The value to apply.
     */
    public void setBoolean(boolean value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.BOOLEAN;
        this.integerValue = value ? 1 : 0;
    }

    /**
     * Sets the value represented by this primitive object to the provided char.
     * @param value The value to apply.
     */
    public void setChar(char value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.CHAR;
        this.integerValue = value;
    }

    /**
     * Sets the value represented by this primitive object to the provided byte.
     * @param value The value to apply.
     */
    public void setByte(byte value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.BYTE;
        this.integerValue = value;
    }

    /**
     * Sets the value represented by this primitive object to the provided short.
     * @param value The value to apply.
     */
    public void setShort(short value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.SHORT;
        this.integerValue = value;
    }

    /**
     * Sets the value represented by this primitive object to the provided int.
     * @param value The value to apply.
     */
    public void setInteger(int value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.INTEGER;
        this.integerValue = value;
    }

    /**
     * Sets the value represented by this primitive object to the provided long.
     * @param value The value to apply.
     */
    public void setLong(long value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.LONG;
        this.integerValue = value;
    }

    /**
     * Sets the value represented by this primitive object to the provided float.
     * @param value The value to apply.
     */
    public void setFloat(float value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.FLOAT;
        this.decimalValue = value;
    }

    /**
     * Sets the value represented by this primitive object to the provided double.
     * @param value The value to apply.
     */
    public void setDouble(double value) {
        reset();
        this.primitiveType = NoodlePrimitiveType.DOUBLE;
        this.decimalValue = value;
    }

    /**
     * Sets the number represented by this primitive object.
     * @param number the number to apply
     */
    public void setNumber(Number number) {
        if (number == null) {
            setObjectReference(null);
            return;
        }

        if (number instanceof Integer) {
            setInteger((Integer) number);
        } else if (number instanceof Long) {
            setLong((Long) number);
        } else if (number instanceof Float) {
            setFloat((Float) number);
        } else if (number instanceof Double) {
            setDouble((Double) number);
        } else if (number instanceof Short) {
            setShort((Short) number);
        } else if (number instanceof Byte) {
            setByte((Byte) number);
        } else {
            throw new NoodleRuntimeException("Unsupported Number type: %s.", Utils.getSimpleName(number));
        }
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
     * Gets the NoodlePrimitive as a decimal number of the largest type.
     */
    public double getDecimal() {
        switch (this.primitiveType) {
            case FLOAT:
                return (float) this.decimalValue;
            case DOUBLE:
                return this.decimalValue;
            default:
                throw new NoodleRuntimeException("Don't know how to interpret %s as a decimal number.", this);
        }
    }

    /**
     * Sets the represented value to be the provided whole number.
     * @param numberType the type of number to track
     * @param value the value to apply
     */
    public void setDecimal(NoodlePrimitiveType numberType, double value) {
        switch (numberType) {
            case FLOAT:
                setFloat((float) value);
                break;
            case DOUBLE:
                setDouble(value);
                break;
            default:
                throw new NoodleRuntimeException("Don't know how to interpret %s as a decimal primitive type.", numberType);
        }
    }

    /**
     * Gets the NoodlePrimitive as a whole number of the largest type.
     */
    public long getWholeNumber() {
        switch (this.primitiveType) {
            case CHAR:
                return (char) this.integerValue;
            case BYTE:
                return (byte) this.integerValue;
            case SHORT:
                return (short) this.integerValue;
            case INTEGER:
                return (int) this.integerValue;
            case LONG:
                return this.integerValue;
            default:
                throw new NoodleRuntimeException("Don't know how to interpret %s as a whole-number.", this);
        }
    }

    /**
     * Sets the represented value to be the provided whole number.
     * @param numberType the type of number to track
     * @param value the value to apply
     */
    public void setWholeNumber(NoodlePrimitiveType numberType, long value) {
        switch (numberType) {
            case CHAR:
                setChar((char) value);
                break;
            case BYTE:
                setByte((byte) value);
                break;
            case SHORT:
                setShort((short) value);
                break;
            case INTEGER:
                setInteger((int) value);
                break;
            case LONG:
                setLong(value);
                break;
            default:
                throw new NoodleRuntimeException("Don't know how to interpret %s as a whole-number primitive type.", numberType);
        }
    }

    /**
     * Sets the NoodlePrimitive as representing null.
     */
    public void setNull() {
        setObjectReference(null);
    }

    /**
     * Tests if the value this primitive represents is a boolean.
     */
    public boolean isBoolean() {
        return this.primitiveType == NoodlePrimitiveType.BOOLEAN;
    }

    /**
     * Tests if the value this primitive represents is an integer type.
     */
    public boolean isIntegerNumber() {
        return this.primitiveType == NoodlePrimitiveType.CHAR || this.primitiveType == NoodlePrimitiveType.BYTE
                || this.primitiveType == NoodlePrimitiveType.SHORT || this.primitiveType == NoodlePrimitiveType.INTEGER
                || this.primitiveType == NoodlePrimitiveType.LONG;
    }

    /**
     * Test if the value this primitive represents is a decimal integer type.
     */
    public boolean isDecimalNumber() {
        return this.primitiveType == NoodlePrimitiveType.FLOAT || this.primitiveType == NoodlePrimitiveType.DOUBLE;
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

    /**
     * Gets the string of this which can be added to another string.
     * @return addString
     */
    public String getAsString() {
        switch (this.primitiveType) {
            case CHAR:
                return String.valueOf(getChar());
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return String.valueOf(this.integerValue);
            case FLOAT:
            case DOUBLE:
                return NumberUtils.doubleToCleanString(this.decimalValue);
            case OBJECT_REFERENCE:
                Object object = this.objectInstance != null ? this.objectInstance.getObject() : null;
                return object != null ? object.toString() : null;
            default:
                throw new NoodleRuntimeException("Don't know how to toString() %s.", this.primitiveType);
        }
    }

    /**
     * Gets the value of this primitive as a java Object.
     * @return addString
     */
    public Object getAsJavaObject() {
        switch (this.primitiveType) {
            case CHAR:
                return getChar();
            case BOOLEAN:
                return getBoolean();
            case BYTE:
                return getByte();
            case SHORT:
                return getShort();
            case INTEGER:
                return getInteger();
            case LONG:
                return getLong();
            case FLOAT:
                return getFloat();
            case DOUBLE:
                return getDouble();
            case OBJECT_REFERENCE:
                return this.objectInstance != null ? this.objectInstance.getObject() : null;
            default:
                throw new NoodleRuntimeException("Don't know how to getAsJavaObject() %s.", this.primitiveType);
        }
    }

    @Override
    public String toString() {
        switch (this.primitiveType) {
            case CHAR:
                return "CHAR='" + getChar() + "'";
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
                return this.primitiveType + "=" + this.integerValue;
            case FLOAT:
            case DOUBLE:
                return this.primitiveType + "=" + this.decimalValue;
            case OBJECT_REFERENCE:
                Object object = this.objectInstance != null ? this.objectInstance.getObject() : null;
                if (object instanceof String) {
                    return "STRING=\"" + NoodleUtils.compiledStringToCodeString((String) object) + "\"";
                } else {
                    return "OBJECT=" + object;
                }
            default:
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

        if (isDecimalNumber()) {
            return this.decimalValue == otherPrim.decimalValue;
        } else if (isIntegerNumber() || isBoolean()) {
            return this.integerValue == otherPrim.integerValue;
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
        if (isBoolean()) {
            return getBoolean();
        } else if (isDecimalNumber()) {
            return Double.isFinite(this.decimalValue) && Math.abs(this.decimalValue) >= 0.5;
        } else if (isIntegerNumber()) {
            return this.integerValue != 0;
        } else if (isObjectReference()) {
            return this.objectInstance != null;
        } else {
            return false;
        }
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