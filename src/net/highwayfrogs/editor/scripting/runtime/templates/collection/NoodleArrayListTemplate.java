package net.highwayfrogs.editor.scripting.runtime.templates.collection;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.templates.collection.NoodleArrayListTemplate.NoodleArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list data structure in Noodle.
 */
public class NoodleArrayListTemplate extends NoodleCollectionTemplate<NoodleArrayList> {
    public NoodleArrayListTemplate(NoodleScriptEngine engine) {
        super(engine, NoodleArrayList.class, "ArrayList");
    }

    @Override
    protected void onSetup() {
        super.onSetup();

        // ArrayList specific.
        addFunction("get", (thread, list, args) -> thread.getStack().pushPrimitive(list.get(args[0].getIntegerValue())), "index");

        addFunction("add", (thread, list, args) -> {
            list.add(args[0].getIntegerValue(), args[1]);
            return thread.getStack().pushNull();
        }, "index", "value");

        addFunction("set", (thread, list, args) -> {
            NoodlePrimitive oldValue = list.set(args[0].getIntegerValue(), args[1], true);
            NoodlePrimitive stackValue = thread.getStack().pushPrimitive(oldValue);
            oldValue.tryDecreaseRefCount(); // Remove the temporary ref count which prevented GC'ing the removed value before it was added to the stack.
            return stackValue;
        }, "index", "value");

        addFunction("removeAt", (thread, list, args) -> {
            NoodlePrimitive removedValue = list.remove(args[0].getIntegerValue(), true);
            NoodlePrimitive stackValue = thread.getStack().pushPrimitive(removedValue);
            removedValue.tryDecreaseRefCount(); // Remove the temporary ref count which prevented GC'ing the removed value before it was added to the stack.
            return stackValue;
        }, "index");
    }

    @Override
    public NoodleArrayList makeEmptyCollection() {
        return new NoodleArrayList();
    }

    // The full class path is written out since IntelliJ keeps automatically "optimizing" (deleting) the import statement necessary for this to work for some reason.
    @Getter
    public static class NoodleArrayList extends net.highwayfrogs.editor.scripting.runtime.templates.collection.NoodleCollectionTemplate.NoodleCollection<List<NoodlePrimitive>> {
        public NoodleArrayList() {
            super(new ArrayList<>());
        }

        @Override
        public int indexOf(NoodlePrimitive value) {
            for (int i = 0; i < getValues().size(); i++) {
                NoodlePrimitive testValue = getValues().get(i);
                if (value != null ? value.valueEquals(testValue) : (testValue == null || testValue.isNull()))
                    return i;
            }

            return -1;
        }

        /**
         * Set a value at the given index. Index must be in range of list.
         * @param index Index to set the value at.
         * @param value Value to set.
         * @param increaseRefCount When true, it will increase the ref count to avoid early garbage collection. UNDERSTAND WHAT YOU'RE DOING HERE, GETTING THIS WRONG WILL BREAK THINGS.
         * @return replaced - Old Value.
         */
        public NoodlePrimitive set(int index, NoodlePrimitive value, boolean increaseRefCount) {
            if (getValues().size() != index) // Allow setting to the max index value.
                validateIndex(index);

            // Update the underlying list.
            NoodlePrimitive oldValue = getValues().set(index, value);

            // Technically, we want to decrease the ref count from removing the old object from the list, then increase it when it gets added to the stack. HOWEVER, I've simplified it with equivalent logic.
            if (!increaseRefCount)
                oldValue.tryDecreaseRefCount();

            value.tryIncreaseRefCount();
            return oldValue;
        }

        /**
         * Insert a value into the list at the index.
         * @param index The index to insert the value at.
         * @param value The value to insert.
         */
        public void add(int index, NoodlePrimitive value) {
            if (getValues().size() != index) // Allow setting to the max index value.
                validateIndex(index);

            value.tryIncreaseRefCount();
            getValues().add(index, value);
        }

        /**
         * Remove a value from the list by its index.
         * @param index The index to remove.
         * @param increaseRefCount When true, it will increase the ref count to avoid early garbage collection. UNDERSTAND WHAT YOU'RE DOING HERE, GETTING THIS WRONG WILL BREAK THINGS.
         * @return removedValue - If there was one.
         */
        public NoodlePrimitive remove(int index, boolean increaseRefCount) {
            validateIndex(index);

            NoodlePrimitive removedValue = getValues().remove(index);

            // Technically, we want to decrease the ref count from removing the old object from the list, then increase it when it gets added to the stack. HOWEVER, I've simplified it with equivalent logic.
            if (!increaseRefCount)
                removedValue.tryDecreaseRefCount();

            return getValues().remove(index);
        }

        /**
         * Return the value at the given index.
         * @param index The index of the element to get.
         * @return value
         */
        public NoodlePrimitive get(int index) {
            validateIndex(index);
            return getValues().get(index);
        }

        private void validateIndex(int index) {
            if (index < 0 || index >= getValues().size())
                throw new NoodleRuntimeException("Invalid index %d from the list.%s", index, (index >= 0 ? " (List Size: " + getValues().size() + ")" : ""));
        }
    }
}