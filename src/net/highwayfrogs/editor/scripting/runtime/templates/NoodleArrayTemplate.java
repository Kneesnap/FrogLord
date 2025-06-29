package net.highwayfrogs.editor.scripting.runtime.templates;

import net.highwayfrogs.editor.scripting.runtime.NoodleObjectInstance;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * A Noodle template capable of managing array objects.
 */
public class NoodleArrayTemplate extends NoodleObjectTemplate<Object> {
    public static final NoodleArrayTemplate INSTANCE = new NoodleArrayTemplate();

    private NoodleArrayTemplate() {
        super(Object.class, "Array");
    }

    @Override
    protected void onSetup() {
        addGetter("length",
                (thread, array) -> thread.getStack().pushNumber(Array.getLength(array)));
        addFunction("get",
                (thread, array, args) -> thread.getStack().pushObject(Array.get(array, args[0].getInteger())),
                "index");
        addFunction("set", (thread, array, args) -> {
            int index = args[0].getInteger();
            Object oldValue = setArrayElement(thread, array, index, args[1]);
            NoodlePrimitive result = thread.getStack().pushObject(oldValue);
            removeObjectRef(thread, oldValue); // Proper GC, only after the new value is applied.
            return result;
        }, "index", "value");
        addConstructor((thread, args) -> thread.getStack().pushObject(new NoodlePrimitive[args[0].getInteger()]),
                "length");
    }

    @Override
    protected void onSetupJvmWrapper() {
        // No jvm functions to directly include.
    }

    @Override
    protected boolean isSupported(Object object) {
        return object != null && object.getClass().isArray();
    }

    @Override
    public boolean areObjectContentsEqual(Object array1, Object array2) {
        if (array1 == array2)
            return true;
        if (array1 == null || array2 == null)
            return false;

        int arrayLength = Array.getLength(array1);
        if (arrayLength != Array.getLength(array2))
            return false;

        for (int i = 0; i < arrayLength; i++)
            if (!Objects.equals(Array.get(array1, i), Array.get(array2, i)))
                return false;

        return true;
    }

    @Override
    public void onObjectAddToHeap(NoodleThread<?> thread, Object object, NoodleObjectInstance instance) {
        super.onObjectAddToHeap(thread, object, instance);
        if (object.getClass().getComponentType().isPrimitive())
            return; // Skip primitive arrays.

        // Update GC.
        int arrayLength = Array.getLength(object);
        for (int i = 0; i < arrayLength; i++)
            addObjectRef(thread, Array.get(object, i));
    }

    @Override
    public void onObjectFree(NoodleThread<?> thread, Object object, NoodleObjectInstance instance) {
        super.onObjectFree(thread, object, instance);
        if (object.getClass().getComponentType().isPrimitive())
            return; // Skip primitive arrays.

        // Update GC.
        int arrayLength = Array.getLength(object);
        for (int i = 0; i < arrayLength; i++)
            removeObjectRef(thread, Array.get(object, i));
    }

    private static void addObjectRef(NoodleThread<?> thread, Object object) {
        if (thread == null)
            return;

        if (object instanceof NoodlePrimitive) {
            ((NoodlePrimitive) object).tryIncreaseRefCount();
        } else if (object != null) {
            NoodleObjectInstance objectInstance = thread.getHeap().getObjectInstance(object);
            if (objectInstance == null) {
                NoodleObjectTemplate<?> template = thread.getEngine().getTemplateFromObject(object);
                if (template != null)
                    objectInstance = new NoodleObjectInstance(thread, object, template);
            }

            if (objectInstance != null)
                objectInstance.incrementRefCount();
        }
    }

    private static void removeObjectRef(NoodleThread<?> thread, Object object) {
        if (object instanceof NoodlePrimitive) {
            ((NoodlePrimitive) object).tryDecreaseRefCount();
        } else if (object != null) {
            NoodleObjectInstance objectInstance = thread.getHeap().getObjectInstance(object);
            if (objectInstance != null)
                objectInstance.decrementRefCount();
        }
    }

    /**
     * Sets the element at the given an array index in the array.
     * Note that the garbage collection of the previously removed value must be handled by the calling method.
     * @param thread the thread which the array set occurs under
     * @param array the array object to update
     * @param index the index to set the element at
     * @param newValue the value to apply
     * @return the value removed from the array
     */
    public static Object setArrayElement(NoodleThread<?> thread, Object array, int index, NoodlePrimitive newValue) {
        if (array == null)
            throw new NullPointerException("array");

        Class<?> arrayClass = array.getClass();
        if (!arrayClass.isArray())
            throw new NoodleRuntimeException("The provided object is not an array!");

        // Bounds check.
        int arrayLength = Array.getLength(array);
        if (index < 0 || index >= arrayLength)
            throw new NoodleRuntimeException("Attempted to assign a value to an array at an invalid index: %d. Valid indices are within [0, %d).", index, arrayLength);

        // If this is a NoodlePrimitive[] array, we can simplify our logic.
        if (array instanceof NoodlePrimitive[]) {
            Object oldPrimitive = Array.get(array, index);
            ((NoodlePrimitive[]) array)[index] = newValue;
            addObjectRef(thread, newValue);
            return oldPrimitive;
        }

        Class<?> arrayElementClass = arrayClass.getComponentType();
        if (arrayElementClass.isPrimitive()) {
            if (boolean.class.equals(arrayElementClass)) {
                boolean oldValue = Array.getBoolean(array, index);
                Array.setBoolean(array, index, newValue.getBoolean());
                return oldValue;
            } else if (long.class.equals(arrayElementClass)) {
                long oldValue = Array.getLong(array, index);
                Array.setLong(array, index, newValue.getLong());
                return oldValue;
            } else if (int.class.equals(arrayElementClass)) {
                int oldValue = Array.getInt(array, index);
                Array.setInt(array, index, newValue.getInteger());
                return oldValue;
            } else if (short.class.equals(arrayElementClass)) {
                short oldValue = Array.getShort(array, index);
                Array.setShort(array, index, newValue.getShort());
                return oldValue;
            } else if (byte.class.equals(arrayElementClass)) {
                byte oldValue = Array.getByte(array, index);
                Array.setByte(array, index, newValue.getByte());
                return oldValue;
            } else if (char.class.equals(arrayElementClass)) {
                char oldValue = Array.getChar(array, index);
                Array.setChar(array, index, newValue.getChar());
                return oldValue;
            } else if (float.class.equals(arrayElementClass)) {
                float oldValue = Array.getFloat(array, index);
                Array.setFloat(array, index, newValue.getFloat());
                return oldValue;
            } else if (double.class.equals(arrayElementClass)) {
                double oldValue = Array.getDouble(array, index);
                Array.setDouble(array, index, newValue.getDouble());
                return oldValue;
            } else {
                throw new NoodleRuntimeException("Unsupported primitive type: '%s'", arrayElementClass.getSimpleName());
            }
        }

        // Test the wrapped types.
        Object oldValue = Array.get(array, index);
        if (Boolean.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getBoolean());
        } else if (Long.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getLong());
        } else if (Integer.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getInteger());
        } else if (Short.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getShort());
        } else if (Byte.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getByte());
        } else if (Character.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getChar());
        } else if (Float.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getFloat());
        } else if (Double.class.equals(arrayElementClass)) {
            Array.set(array, index, newValue.getDouble());
        } else if (newValue.isObjectReference()) {
            NoodleObjectInstance objectInstance = newValue.getObjectReference();
            Object newValueObject = objectInstance != null ? objectInstance.getObject() : null;

            Array.set(array, index, newValueObject);
            addObjectRef(thread, newValue);
        } else {
            throw new NoodleRuntimeException("Don't know how to apply %s as an object instance. (%s)", newValue, arrayElementClass.getSimpleName());
        }

        return oldValue;
    }

    /**
     * Loads an array object of Java types from a NoodlePrimitive containing array which MAY contain NoodlePrimitive elements.
     * Any NoodlePrimitive elements in the input array will be unboxed.
     * @param arrayType the array class to create
     * @param input the primitive to interpret
     * @return unboxedArray
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static Object unboxArray(Class<?> arrayType, NoodlePrimitive input) {
        if (arrayType  == null)
            throw new NullPointerException("arrayType");
        if (input == null)
            return null;
        if (!input.isObjectReference())
            throw new NoodleRuntimeException("Tried to obtain an array %s but received a non-array primitive (%s).", arrayType.getSimpleName(), input);

        NoodleObjectInstance objectInstance = input.getObjectReference();
        Object inputObj = objectInstance != null ? objectInstance.getObject() : null;
        if (inputObj == null)
            return null; // Return null as array.

        Class<?> targetElementType = arrayType.getComponentType();
        Class<?> inputObjClass = inputObj.getClass();
        if (!inputObjClass.isArray())
            throw new NoodleRuntimeException("Tried to obtain an array %s but the primitive (%s) was not an array!", arrayType.getSimpleName(), input);
        if (arrayType.isInstance(inputObj))
            return inputObj; // Already unboxed.

        // Attempt to auto-convert object instances.
        int arrayLength = Array.getLength(inputObj);
        Class<?> realElementType = inputObjClass.getComponentType();
        if (targetElementType.isAssignableFrom(realElementType)) {
            Object newArray = Array.newInstance(targetElementType, arrayLength);
            System.arraycopy(inputObj, 0, newArray, 0, arrayLength);
            return newArray;
        }

        // Work with primitives.
        if (inputObj instanceof NoodlePrimitive[]) {
            NoodlePrimitive[] args = (NoodlePrimitive[]) inputObj;
            if (!targetElementType.isPrimitive()) {
                Object newArray = Array.newInstance(inputObjClass.getComponentType(), arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    NoodlePrimitive arg = args[i];
                    NoodleObjectInstance objInstance = arg.isObjectReference() ? arg.getObjectReference() : null;
                    if (objInstance != null) {
                        Array.set(newArray, i, targetElementType.cast(objInstance.getObject()));
                    } else { // Handles number class wrappers too.
                        setArrayElement(null, newArray, i, arg);
                    }
                }

                return newArray;
            } else if (int.class.equals(targetElementType)) {
                int[] array = new int[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getInteger();
                return array;
            } else if (boolean.class.equals(targetElementType)) {
                boolean[] array = new boolean[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getBoolean();
                return array;
            } else if (long.class.equals(targetElementType)) {
                long[] array = new long[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getLong();
                return array;
            } else if (short.class.equals(targetElementType)) {
                short[] array = new short[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getShort();
                return array;
            } else if (byte.class.equals(targetElementType)) {
                byte[] array = new byte[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getByte();
                return array;
            } else if (char.class.equals(targetElementType)) {
                char[] array = new char[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getChar();
                return array;
            } else if (float.class.equals(targetElementType)) {
                float[] array = new float[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getFloat();
                return array;
            } else if (double.class.equals(targetElementType)) {
                double[] array = new double[arrayLength];
                for (int i = 0; i < arrayLength; i++)
                    array[i] = args[i].getDouble();
                return array;
            } else {
                throw new NoodleRuntimeException("Unsupported primitive type: '%s'", targetElementType.getSimpleName());
            }
        }

        throw new NoodleRuntimeException("Tried to obtain an array %s but the primitive (%s) could not be converted to a compatible array!", arrayType.getSimpleName(), input);
    }
}