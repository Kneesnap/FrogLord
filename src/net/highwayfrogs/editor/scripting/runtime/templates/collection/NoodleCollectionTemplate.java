package net.highwayfrogs.editor.scripting.runtime.templates.collection;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.runtime.NoodleObjectInstance;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.collection.NoodleCollectionTemplate.NoodleCollection;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Represents a list data structure in Noodle.
 */
public abstract class NoodleCollectionTemplate<TNoodleCollection extends NoodleCollection<?>> extends NoodleObjectTemplate<TNoodleCollection> {
    protected NoodleCollectionTemplate(Class<TNoodleCollection> collectionClass, String collectionName) {
        super(collectionClass, collectionName);
    }

    @Override
    protected void onSetup() {
        addFunction("size", (thread, collection, args) -> thread.getStack().pushNumber(collection.size()));
        addFunction("isEmpty", (thread, collection, args) -> thread.getStack().pushBoolean(collection.isEmpty()));
        addFunction("contains", (thread, collection, args) -> thread.getStack().pushBoolean(collection.contains(args[0])), "value");
        addFunction("indexOf", (thread, collection, args) -> thread.getStack().pushNumber(collection.indexOf(args[0])), "value");

        // Collection modifications:
        addFunction("add", (thread, collection, args) -> thread.getStack().pushBoolean(collection.add(args[0])), "value");
        addFunction("addAll", (thread, collection, args) -> thread.getStack().pushBoolean(collection.addAll(args[0])), "collection");
        addFunction("remove", (thread, collection, args) -> thread.getStack().pushBoolean(collection.remove(args[0])), "value");
        addFunction("removeAll", (thread, collection, args) -> thread.getStack().pushBoolean(collection.removeAll(args[0])), "collection");


        addFunction("clear", (thread, collection, args) -> {
            collection.clear();
            return thread.getStack().pushNull();
        });

        // Add constructors.
        addConstructor((thread, args) -> makeEmptyCollection());
        addConstructor((thread, args) -> {
            TNoodleCollection newCollection = makeEmptyCollection();
            newCollection.addAll(args[0]);
            return newCollection;
        }, "collection");
    }

    @Override
    protected void onSetupJvmWrapper() {
        // Do nothing, we don't have any wrapped functions.
    }

    /**
     * Creates an empty collection.
     */
    public abstract TNoodleCollection makeEmptyCollection();

    @Override
    public void onObjectAddToHeap(NoodleThread<?> thread, TNoodleCollection collection, NoodleObjectInstance instance) {
        super.onObjectAddToHeap(thread, collection, instance);
        collection.addValueHeapReferences();
    }

    @Override
    public void onObjectFree(NoodleThread<?> thread, TNoodleCollection collection, NoodleObjectInstance instance) {
        super.onObjectFree(thread, collection, instance);
        collection.removeValueHeapReferences();
    }

    @Getter
    public static abstract class NoodleCollection<TCollection extends Collection<NoodlePrimitive>> implements Iterable<NoodlePrimitive> {
        private transient final TCollection values;

        protected NoodleCollection(TCollection collection) {
            this.values = collection;
        }

        /**
         * Adds heap references to the values tracked by this list.
         * Should be called when the wrapper is added to the heap and the values here should be registered to the heap too.
         */
        public void addValueHeapReferences() {
            for (NoodlePrimitive value : this.getValues())
                value.tryIncreaseRefCount();
        }

        /**
         * Removes heap references to the values tracked by this list.
         * Should be called when the wrapper is garbage collected from the noodle heap, and the values here are registered to the heap.
         */
        public void removeValueHeapReferences() {
            for (NoodlePrimitive value : this.getValues())
                value.tryDecreaseRefCount();
        }

        /**
         * Get the index of a given value. Returns -1 if not found.
         * @param value The value to find the index of.
         * @return index
         */
        public int indexOf(NoodlePrimitive value) {
            Iterator<NoodlePrimitive> iterator = this.values.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                NoodlePrimitive testValue = iterator.next();
                if (value != null ? value.valueEquals(testValue) : (testValue == null || testValue.isNull()))
                    return index;

                index++;
            }

            return -1; // Not found.
        }

        /**
         * Does this list contain the given value?
         * @param value The value to check if we contain.
         * @return contains
         */
        public boolean contains(NoodlePrimitive value) {
            return indexOf(value) >= 0;
        }

        /**
         * Remove a value from the list.
         * Returns whether the element was removed successfully.
         * @param value The value to remove.
         * @return wasRemoved - Was the value removed.
         */
        public boolean remove(NoodlePrimitive value) {
            if (this.values.remove(value)) {
                value.tryDecreaseRefCount();
                return true;
            }

            return false;
        }

        /**
         * Remove a collection of values from this list.
         * @param values The values to remove.
         */
        public boolean removeAll(Collection<NoodlePrimitive> values) {
            boolean changed = false;
            for (NoodlePrimitive value : values)
                if (remove(value))
                    changed = true;

            return changed;
        }


        /**
         * Add the values in a collection primitive to this list.
         * @param collectionPrimitive The collection primitive to add.
         */
        public boolean removeAll(NoodlePrimitive collectionPrimitive) {
            NoodleCollection<?> otherCollection = collectionPrimitive.getObjectReference().getRequiredObjectInstance(NoodleCollection.class);
            return removeAll(otherCollection.getValues());
        }


        /**
         * Add a value to the list.
         * @param value The value to add.
         */
        public boolean add(NoodlePrimitive value) {
            if (this.values.add(value)) {
                value.tryIncreaseRefCount();
                return true;
            }

            return false;
        }

        /**
         * Add a collection of values to this list.
         * @param values The values to add.
         */
        public boolean addAll(Collection<NoodlePrimitive> values) {
            boolean changed = false;
            for (NoodlePrimitive value : values)
                if (add(value))
                    changed = true;

            return changed;
        }

        /**
         * Add the values in a collection primitive to this list.
         * @param collectionPrimitive The collection primitive to add.
         */
        public boolean addAll(NoodlePrimitive collectionPrimitive) {
            NoodleCollection<?> otherCollection = collectionPrimitive.getObjectReference().getRequiredObjectInstance(NoodleCollection.class);
            return addAll(otherCollection.getValues());
        }

        /**
         * Is this list empty?
         * @return empty
         */
        public boolean isEmpty() {
            return size() == 0;
        }

        /**
         * Returns the number of elements in the list.
         * @return size
         */
        public int size() {
            return this.values.size();
        }

        /**
         * Clear the values.
         */
        public void clear() {
            this.values.forEach(NoodlePrimitive::tryDecreaseRefCount);
            this.values.clear();
        }

        @Override
        public Iterator<NoodlePrimitive> iterator() {
            return this.values.iterator();
        }

        @Override
        public void forEach(Consumer<? super NoodlePrimitive> action) {
            this.values.forEach(action);
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder("[");
            for (NoodlePrimitive value : this.values)
                ret.append(ret.length() > 1 ? ", " : "").append(value);
            return ret.append("]").toString();
        }
    }
}