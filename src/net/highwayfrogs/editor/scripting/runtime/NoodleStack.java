package net.highwayfrogs.editor.scripting.runtime;

import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Noodle thread's stack.
 * TODO: (FUTURE) Reduce object allocations by never adding / removing from the stack list, but instead just updating existing primitives. Make sure to call reset() on values removed, after decreasing object references.
 */
public class NoodleStack {
    private NoodleThread<?> thread;
    private final List<NoodlePrimitive> stack = new ArrayList<>();

    // TODO: (FUTURE) Make local variables be stack-based. Basically, every function + main() will define the number of local variable slots it will define.
    // TODO: Then, We'll have instructions PUSHLOCAL and SETLOCAL, which we'll give a single number relative to the stack position when the function was called.
    // TODO: Then, we'll have an integer stack which will track stack positions as of the last functions running.
    // TODO: Then, when we return from the function, those values will be popped off of the stack, with the return value being restored afterwards.
    // TODO: This will massively reduce the memory footprint as it will remove hashmap cloning, and allocation in general

    public NoodleStack() {
    }

    public NoodleStack(NoodleThread<?> thread) {
        this.thread = thread;
    }

    /**
     * Gets the number of values in the stack.
     */
    public int size() {
        return this.stack.size();
    }

    /**
     * Pushes an object onto the stack.
     * @param primitive The primitive to push.
     * @return The primitive pushed onto the stack.
     */
    public NoodlePrimitive pushPrimitive(NoodlePrimitive primitive) {
        if (primitive == null)
            primitive = new NoodlePrimitive(); // Null.

        // Increase object usages.
        primitive.tryIncreaseRefCount();

        this.stack.add(primitive);
        return primitive;
    }

    /**
     * Pushes a numeric value onto the stack.
     * @param value The value to push on the stack.
     * @return pushedPrimitive
     */
    public NoodlePrimitive pushNumber(double value) {
        NoodlePrimitive primitive = new NoodlePrimitive(value);
        this.stack.add(primitive);
        return primitive;
    }

    /**
     * Pushes a boolean value onto the stack.
     * @param value The value to push on the stack.
     * @return pushedPrimitive
     */
    public NoodlePrimitive pushBoolean(boolean value) {
        NoodlePrimitive primitive = new NoodlePrimitive(value);
        this.stack.add(primitive);
        return primitive;
    }

    /**
     * Pushes a string value onto the stack.
     * @param value The value to push on the stack.
     * @return pushedPrimitive
     */
    public NoodlePrimitive pushString(String value) {
        NoodlePrimitive primitive = new NoodlePrimitive(value);
        this.stack.add(primitive);
        return primitive;
    }

    /**
     * Pushes an enum value to the stack.
     * @param enumValue The value to push.
     * @return pushedPrimitive
     * @param <E> The enum type.
     */
    public <E extends Enum<E>> NoodlePrimitive pushEnum(E enumValue) {
        return pushString(enumValue != null ? enumValue.name() : null);
    }

    /**
     * Pushes null onto the stack.
     * @return pushedPrimitive
     */
    public NoodlePrimitive pushNull() {
        NoodlePrimitive primitive = new NoodlePrimitive();
        this.stack.add(primitive);
        return primitive;
    }

    /**
     * Pushes a value onto the stack.
     * @param object The object to push on the stack.
     * @return pushedPrimitive
     */
    public NoodlePrimitive pushObject(Object object) {
        return pushObject(object, true);
    }

    /**
     * Pushes a value onto the stack.
     * @param object The object to push on the stack.
     * @param errorIfMissingTemplate If true, an error will be thrown if the object template cannot be resolved. Otherwise, null will be pushed on the stack.
     * @return pushedPrimitive
     */
    public NoodlePrimitive pushObject(Object object, boolean errorIfMissingTemplate) {
        if (object == null)
            return pushNull();
        if (object instanceof String)
            return pushString((String) object);
        if (object instanceof Number)
            return pushNumber(((Number) object).doubleValue());
        if (object instanceof Boolean)
            return pushBoolean((Boolean) object);
        if (object instanceof NoodlePrimitive)
            return pushPrimitive((NoodlePrimitive) object);

        NoodleObjectInstance objectInstance;
        if (object instanceof NoodleObjectInstance) {
            objectInstance = (NoodleObjectInstance) object;
        } else {
            objectInstance = this.thread != null ? this.thread.getHeap().getObjectInstance(object) : null;

            // If there is no object instance, create one.
            if (objectInstance == null) {
                NoodleObjectTemplate<?> template = this.thread != null ? this.thread.getEngine().getTemplateFromObject(object) : null;
                if (template == null) {
                    if (errorIfMissingTemplate)
                        throw new NoodleRuntimeException("The %s passed in is not an object which can be represented in Noodle. (Has no corresponding template)", Utils.getSimpleName(object));

                    // If errorIfMissingTemplate is false, we don't want to error, we're just going to push null on the stack.
                    // objectInstance is currently null, so we don't need to do anything to make this happen.
                } else {
                    objectInstance = new NoodleObjectInstance(this.thread, object, template);
                }
            }
        }

        return pushPrimitive(new NoodlePrimitive(objectInstance));
    }

    /**
     * Pops a NoodlePrimitive from the stack.
     * NOTE: Popping a value off the stack will cause the garbage collector to free it if there are no more references.
     * This means that this method should ONLY be called when the value you're popping is not going back into the thread.
     * @return The popped primitive.
     */
    public NoodlePrimitive popWithGC() {
        if (this.stack.isEmpty())
            throw new NoodleRuntimeException("Cannot pop value from the stack, because the stack is empty.");

        NoodlePrimitive poppedPrimitive = this.stack.remove(this.stack.size() - 1);
        poppedPrimitive.tryDecreaseRefCount();
        return poppedPrimitive;
    }

    /**
     * Pops a NoodlePrimitive from the stack, without garbage collecting.
     * The purpose of this is to allow moving a value from the stack to somewhere else.
     * For example, if you pop a value off the stack, but want to store it in a variable, you do not want to trigger garbage collection.
     * NOTE: THIS MEANS THE CALLER IS RESPONSIBLE FOR CALLING tryDecreaseRefCount() ONCE IT REGISTERS IT IN THE NEW SPOT.
     * @return The popped primitive.
     */
    public NoodlePrimitive popWithoutGC() {
        if (this.stack.isEmpty())
            throw new NoodleRuntimeException("Cannot pop value from the stack, because the stack is empty.");

        return this.stack.remove(this.stack.size() - 1);
    }

    /**
     * Peeks at the top NoodlePrimitive on the stack.
     * @return The peeked primitive.
     */
    public NoodlePrimitive peek() {
        if (this.stack.isEmpty())
            throw new NoodleRuntimeException("Cannot peek value from the stack, because the stack is empty.");

        return this.stack.get(this.stack.size() - 1);
    }
}