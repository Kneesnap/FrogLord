package net.highwayfrogs.editor.scripting.runtime.templates.aggregate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.scripting.runtime.*;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleTemplateFunction;
import net.highwayfrogs.editor.scripting.runtime.templates.aggregate.UnsavedAggregateTemplate.NoodleAggregateWrapper;
import net.highwayfrogs.editor.utils.lambda.Consumer3;

import java.util.*;
import java.util.function.BiFunction;

/**
 * This is an implementation of an aggregate template, which does not include the saving / loading logic.
 * This is useful for templates such as the clone template which act as aggregates but do not need to save each individual value.
 * An aggregate template is a special kind of template.
 * It operates like an array, but it allows calling the methods of each as if they are a single unit.
 * For example, when we create NPC clones of all players in the cutscene, each player gets their own NPC.
 * However, this would be very annoying for chroniclers to have to iterate through / know they need to iterate through.
 * So, this system allows a single object to be used as an aggregate proxy for multiple underlying objects.
 * In the future, if we ever support arrays, we should allow the array accessor on this template, to handle conflict situations.
 * For example, if you spawn clones, but they all have different locations, using the "location" getter will result in an error.
 */
public abstract class UnsavedAggregateTemplate<TWrappedType> extends NoodleObjectTemplate<NoodleAggregateWrapper<TWrappedType>> {
    private final Map<String, NoodleTemplateFunction<?>> cachedFunctionMappers = new HashMap<>(); // Tracking it ourselves lets us override default "is this function correct" behavior in getInstanceFunction(), we can just always return it regardless of things like argument counts, etc.

    protected UnsavedAggregateTemplate(Class<?> wrappedTypeClass, String name) {
        super((Class<NoodleAggregateWrapper<TWrappedType>>) wrappedTypeClass, name);
    }

    @Override
    protected void onSetup() {
        // Don't need to do any setup.
    }

    @Override
    protected void onSetupJvmWrapper() {
        // Do nothing, we don't have any wrapped functions.
    }

    @Override
    public NoodleTemplateFunction<?> getInstanceFunction(String functionName, int argumentCount) {
        return this.cachedFunctionMappers.computeIfAbsent(functionName, name -> new NoodleTemplateFunctionAggregate<>(name, this));
    }

    @Override
    public BiFunction<NoodleThread<?>, NoodleAggregateWrapper<TWrappedType>, NoodlePrimitive> getGetter(String fieldName) {
        var getter = super.getGetter(fieldName);
        if (getter == null)
            addGetter(fieldName, getter = new NoodleTemplateGetterAggregate<>(fieldName));

        return getter;
    }

    @Override
    public Consumer3<NoodleThread<?>, NoodleAggregateWrapper<TWrappedType>, NoodlePrimitive> getSetter(String fieldName) {
        var setter = super.getSetter(fieldName);
        if (setter == null)
            addSetter(fieldName, setter = new NoodleTemplateSetterAggregate<>(fieldName));

        return setter;
    }

    /**
     * Represents the object containing wrapped values.
     * @param <TWrappedType> The type of the value which gets wrapped.
     */
    @Getter
    public static class NoodleAggregateWrapper<TWrappedType> {
        private transient final NoodleThread<?> thread;
        private final boolean valuesTrackedInHeap;
        private final NoodleObjectTemplate<?> wrappedTemplate;
        private final List<TWrappedType> values;

        private static final String JSON_WRAPPED_TYPE = "wrappedType";
        private static final String JSON_WRAPPED_VALUES = "values";

        public NoodleAggregateWrapper(NoodleThread<?> thread, NoodleObjectTemplate<?> wrappedTemplate, boolean valuesTrackedInHeap, List<TWrappedType> values) {
            this.thread = thread;
            this.valuesTrackedInHeap = valuesTrackedInHeap;
            this.wrappedTemplate = wrappedTemplate;
            this.values = values;
        }

        /**
         * Adds heap references to the values tracked by this wrapper.
         * Should be called when the wrapper is added to the heap and the values here should be registered to the heap too.
         */
        public void addValueHeapReferences() {
            if (this.values == null)
                return;

            for (int i = 0; i < this.values.size(); i++) {
                TWrappedType wrappedObject = this.values.get(i);
                NoodleObjectInstance objectInstance = this.thread.getHeap().getObjectInstance(wrappedObject);
                if (objectInstance == null && this.valuesTrackedInHeap) // Register object in heap if it's not there.
                    objectInstance = new NoodleObjectInstance(this.thread, wrappedObject);

                if (objectInstance != null)
                    objectInstance.incrementRefCount();
            }
        }

        /**
         * Removes heap references to the values tracked by this wrapper.
         * Should be called when the wrapper is garbage collected from the noodle heap, and the values here are registered to the heap.
         */
        public void removeValueHeapReferences() {
            if (this.values == null)
                return;

            for (int i = 0; i < this.values.size(); i++) {
                TWrappedType wrappedObject = this.values.get(i);
                NoodleObjectInstance objectInstance = this.thread.getHeap().getObjectInstance(wrappedObject);
                if (objectInstance != null)
                    objectInstance.decrementRefCount();
            }
        }

        /**
         * Get the values in a form which does NOT allow adding to removing values, but can still be iterated.
         * Use the appropriate methods instead for proper garbage collection tracking.
         * @return values
         */
        public Collection<TWrappedType> getValues() {
            return this.values;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof NoodleAggregateWrapper))
                return false;

            // Verify match.
            NoodleAggregateWrapper<TWrappedType> otherWrapper = (NoodleAggregateWrapper<TWrappedType>) other;
            return Objects.equals(this.wrappedTemplate, otherWrapper.wrappedTemplate) && Objects.equals(this.values, otherWrapper.values);
        }

        /**
         * Adds a value to the aggregate and handles any garbage collection.
         * @param value The value to add.
         */
        public void add(TWrappedType value) {
            if (this.values.contains(value))
                return;

            this.values.add(value);

            // Find the wrapper in the heap.
            NoodleObjectInstance wrapperObjectRef = this.thread.getHeap().getObjectInstance(this);
            if (wrapperObjectRef == null)
                return; // Wrapper is not registered in the heap, so the new value also shouldn't be added to the heap.

            // Find the value in the heap.
            NoodleObjectInstance valueObjectRef = this.thread.getHeap().getObjectInstance(value);
            if (valueObjectRef == null && this.valuesTrackedInHeap) // Create new object if not exist.
                valueObjectRef = new NoodleObjectInstance(this.thread, value);

            // Consider this as another heap reference, but only if the object reference has been found or created.
            if (valueObjectRef != null)
                valueObjectRef.incrementRefCount();
        }

        /**
         * Removes a value from the aggregate and handles any garbage collection.
         * @param value The value to remove.
         */
        public void remove(TWrappedType value) {
            if (!this.values.remove(value))
                return;

            NoodleObjectInstance wrapperObjectRef = this.thread.getHeap().getObjectInstance(this);
            if (wrapperObjectRef == null)
                return; // Wrapper is not registered in the heap, so this shouldn't impact the heap.

            NoodleObjectInstance valueObjectRef = this.thread.getHeap().getObjectInstance(value);
            if (valueObjectRef != null)
                valueObjectRef.decrementRefCount();
        }

        /**
         * Clear the contents of this aggregate wrapper.
         * @param freeHeapReferences Whether the objects should have their heap refCount decremented, and be potentially garbage collected.
         */
        public void clear(boolean freeHeapReferences) {
            NoodleObjectInstance wrapperObjectRef = this.thread.getHeap().getObjectInstance(this);
            if (!freeHeapReferences || wrapperObjectRef == null) {
                this.values.clear();
                return; // Wrapper is not registered in the heap, so this shouldn't impact the heap.
            }

            for (int i = 0; i < this.values.size(); i++) {
                NoodleObjectInstance valueObjectRef = this.thread.getHeap().getObjectInstance(this.values.get(i));
                if (valueObjectRef != null)
                    valueObjectRef.decrementRefCount();
            }

            this.values.clear();
        }
    }

    @AllArgsConstructor
    private static class NoodleTemplateGetterAggregate<TWrappedType> implements BiFunction<NoodleThread<?>, NoodleAggregateWrapper<TWrappedType>, NoodlePrimitive> {
        private final String fieldName;

        @Override
        public NoodlePrimitive apply(NoodleThread<?> thread, NoodleAggregateWrapper<TWrappedType> wrapper) {
            NoodleObjectTemplate<TWrappedType> template = (NoodleObjectTemplate<TWrappedType>) wrapper.getWrappedTemplate();

            var getter = template.getGetter(this.fieldName);
            if (getter == null)
                throw new NoodleRuntimeException("The Noodle object type '%s' does not support getting the field named '%s'.", template.getName(), this.fieldName);

            NoodlePrimitive returnValue = null;
            for (int i = 0; i < wrapper.values.size(); i++) {
                int oldStackSize = thread.getStack().size();
                NoodlePrimitive singleResult = getter.apply(thread, wrapper.values.get(i));
                if (thread.getStatus() != NoodleThreadStatus.RUNNING)
                    throw new NoodleRuntimeException("Noodle aggregate getter '%s.%s' put the thread into %s state, but aggregate objects do not support this.", template.getName(), this.fieldName, thread.getStatus());

                if (thread.getStack().size() == oldStackSize + 1)
                    singleResult = thread.getStack().popWithoutGC();

                // Manage return values.
                if (i > 0) {
                    if (!Objects.equals(returnValue, singleResult))
                        throw new NoodleRuntimeException("This is actually a group of multiple '%s' objects, so there is no way to get a single value for '%s' unless all the values match.", template.getName(), this.fieldName);
                    if (singleResult != null)
                        singleResult.tryDecreaseRefCount(); // Free stack reference.
                } else {
                    returnValue = singleResult;
                }
            }

            NoodlePrimitive valuePushedToStack = thread.getStack().pushObject(returnValue);
            if (returnValue != null)
                returnValue.tryDecreaseRefCount(); // Remove the stack reference of the value.
            return valuePushedToStack;
        }
    }

    @AllArgsConstructor
    private static class NoodleTemplateSetterAggregate<TWrappedType> implements Consumer3<NoodleThread<?>, NoodleAggregateWrapper<TWrappedType>, NoodlePrimitive> {
        private final String fieldName;

        @Override
        public void accept(NoodleThread<?> thread, NoodleAggregateWrapper<TWrappedType> wrapper, NoodlePrimitive newValue) {
            NoodleObjectTemplate<TWrappedType> template = (NoodleObjectTemplate<TWrappedType>) wrapper.getWrappedTemplate();

            var setter = template.getSetter(this.fieldName);
            if (setter == null)
                throw new NoodleRuntimeException("The Noodle object type '%s' does not support setting the field named '%s'.", template.getName(), this.fieldName);

            for (int i = 0; i < wrapper.values.size(); i++) {
                setter.accept(thread, wrapper.values.get(i), newValue);
                if (thread.getStatus() != NoodleThreadStatus.RUNNING)
                    throw new NoodleRuntimeException("Noodle aggregate setter '%s.%s' put the thread into %s state, but aggregate objects do not support this.", template.getName(), this.fieldName, thread.getStatus());
            }
        }
    }

    private static class NoodleTemplateFunctionAggregate<TWrappedType> extends NoodleTemplateFunction<NoodleAggregateWrapper<TWrappedType>> {
        public NoodleTemplateFunctionAggregate(String label, NoodleObjectTemplate<NoodleAggregateWrapper<TWrappedType>> template) {
            super(label, template.getWrappedClass());
        }

        @Override
        protected NoodlePrimitive executeImpl(NoodleThread<?> thread, NoodleAggregateWrapper<TWrappedType> thisRef, NoodlePrimitive[] args) {
            NoodleObjectTemplate<TWrappedType> template = (NoodleObjectTemplate<TWrappedType>)  thisRef.getWrappedTemplate();
            final String label = getName();

            boolean returnValuesMatch = true;
            NoodlePrimitive returnValue = null;
            for (int i = 0; i < thisRef.values.size(); i++) {
                int oldStackSize = thread.getStack().size();
                NoodlePrimitive singleResult = template.executeFunction(thisRef.values.get(i), thread, label, args);
                if (thread.getStatus() != NoodleThreadStatus.RUNNING)
                    throw new NoodleRuntimeException("Noodle aggregate function call '%s.%s' put the thread into %s state, but aggregate functions do not support this.", template.getName(), label, thread.getStatus());

                if (thread.getStack().size() == oldStackSize + 1)
                    singleResult = thread.getStack().popWithoutGC();

                // Manage return values.
                if (i > 0) {
                    if (returnValuesMatch && !Objects.equals(returnValue, singleResult))
                        returnValuesMatch = false;

                    if (singleResult != null)
                        singleResult.tryDecreaseRefCount();
                } else {
                    returnValue = singleResult;
                }
            }

            // If all return values match, return the value as a return value.
            NoodlePrimitive valuePushedToStack = thread.getStack().pushObject(returnValuesMatch ? returnValue : null);
            if (returnValue != null) // Free the first value that used to be on the stack, so it gets free'd from the heap.
                returnValue.tryDecreaseRefCount();
            return valuePushedToStack;
        }
    }
}