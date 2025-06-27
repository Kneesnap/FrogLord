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
                (thread, array, args) -> thread.getStack().pushObject(Array.get(array, args[0].getIntegerValue())),
                "index");
        addFunction("set", (thread, array, args) -> {
            int index = args[0].getIntegerValue();
            Object oldValue = setArrayValue(thread, array, index, args[1]);
            NoodlePrimitive result = thread.getStack().pushObject(oldValue);
            removeObjectRef(thread, oldValue); // Proper GC, only after the new value is applied.
            return result;
        }, "index", "value");
        addConstructor((thread, args) -> thread.getStack().pushObject(new NoodlePrimitive[args[0].getIntegerValue()]),
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

    private static Object setArrayValue(NoodleThread<?> thread, Object array, int index, NoodlePrimitive newValue) {
        if (array == null)
            throw new NullPointerException("array");

        Class<?> arrayClass = array.getClass();
        if (!arrayClass.isArray())
            throw new NoodleRuntimeException("The provided object is not an array!");

        // Bounds check.
        int arrayLength = Array.getLength(array);
        if (index < 0 || index >= arrayLength)
            throw new NoodleRuntimeException("Attempted to assign a value to an array at index %d, but the array only has valid indices between [0, %d).", index, arrayLength);

        Class<?> arrayElementClass = arrayClass.getComponentType();
        if (arrayElementClass.isPrimitive()) {
            if (!newValue.isNumber())
                throw new NoodleRuntimeException("Cannot treat %s as a primitive to assign to the array type %s.", newValue, arrayClass.getSimpleName());

            if (int.class.equals(arrayElementClass)) {
                int oldValue = Array.getInt(array, index);
                Array.setInt(array, index, newValue.getAsIntegerValue());
                return oldValue;
            } else if (float.class.equals(arrayElementClass)) {
                float oldValue = Array.getFloat(array, index);
                Array.setFloat(array, index, (float) newValue.getNumberValue());
                return oldValue;
            } else if (boolean.class.equals(arrayElementClass)) {
                boolean oldValue = Array.getBoolean(array, index);
                Array.setBoolean(array, index, newValue.isTrueValue());
                return oldValue;

            } else if (long.class.equals(arrayElementClass)) {
                long oldValue = Array.getLong(array, index);
                Array.setLong(array, index, newValue.getAsIntegerValue());
                return oldValue;
            } else if (double.class.equals(arrayElementClass)) {
                double oldValue = Array.getDouble(array, index);
                Array.setDouble(array, index, newValue.getNumberValue());
                return oldValue;
            } else if (short.class.equals(arrayElementClass)) {
                short oldValue = Array.getShort(array, index);
                Array.setShort(array, index, (short) newValue.getAsIntegerValue());
                return oldValue;
            } else if (byte.class.equals(arrayElementClass)) {
                byte oldValue = Array.getByte(array, index);
                Array.setByte(array, index, (byte) newValue.getAsIntegerValue());
                return oldValue;
            } else if (char.class.equals(arrayElementClass)) {
                char oldValue = Array.getChar(array, index);
                Array.setChar(array, index, (char) newValue.getAsIntegerValue());
                return oldValue;
            } else {
                throw new NoodleRuntimeException("Unsupported primitive type: '%s'", arrayElementClass.getSimpleName());
            }
        }

        Object newValueObject;
        if (newValue.isString()) {
            newValueObject = newValue.getStringValue();
        } else if (newValue.isObjectReference()) {
            NoodleObjectInstance objectInstance = newValue.getObjectReference();
            newValueObject = objectInstance != null ? objectInstance.getObject() : null;
        } else {
            throw new NoodleRuntimeException("Don't know how to apply %s as an object instance.", newValue);
        }

        Object oldValue = Array.get(array, index);
        Array.set(array, index, newValueObject);
        addObjectRef(thread, newValue);
        return oldValue;
    }
}