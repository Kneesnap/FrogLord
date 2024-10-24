package net.highwayfrogs.editor.scripting.runtime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Objects;

/**
 * Represents a noodle object instance.
 * Object instances go into a pool we call the heap, but it's not actually a heap.
 * These can also be stored on the stack, but only sometimes, and we should generally avoid that, opting to use a reference instead.
 */
@Getter
@NoArgsConstructor
public class NoodleObjectInstance {
    private NoodleThread<?> thread;
    private Object object;
    private NoodleObjectTemplate<?> template;
    private int refCount;

    public NoodleObjectInstance(NoodleThread<?> thread, Object object) {
        this.thread = thread;
        setObject(object);
    }

    public NoodleObjectInstance(NoodleThread<?> thread, Object object, NoodleObjectTemplate<?> objectTemplate) {
        this.thread = thread;
        setObject(object, objectTemplate);
    }

    /**
     * Called when a thread starts with this primitive as an argument.
     * @param thread The thread which started.
     */
    public void onThreadStartAsArgument(NoodleThread<?> thread) {
        if (this.thread == null)
            this.thread = thread;

        incrementRefCount();
    }

    /**
     * Sets the object wrapped by this instance.
     * Throws an error if the object cannot be represented in Noodle.
     * @param newObject The object to apply.
     */
    public void setObject(Object newObject) {
        if (newObject == null) // Null is not allowed. If you want a NoodlePrimitive to be null, you set the NoodleObjectInstance to be null instead.
            throw new NoodleRuntimeException("NoodleObjectInstance cannot track a null object.");

        if (this.thread == null)
            throw new NoodleRuntimeException("The NoodleObjectInstance does not have a thread, so we can't resolve the template for the provided object.");

        NoodleObjectTemplate<?> template = this.thread.getEngine().getTemplateFromObject(newObject);
        if (template == null)
            throw new NoodleRuntimeException("The %s passed in is not an object which can be represented in Noodle. (Has no corresponding template)", Utils.getSimpleName(newObject));

        setObject(newObject, template);
    }

    /**
     * Gets the object template generically, so it can be used without type validation.
     */
    @SuppressWarnings({"unchecked"})
    public NoodleObjectTemplate<Object> getObjectTemplate() {
        return (NoodleObjectTemplate<Object>) this.template;
    }

    /**
     * Sets the object wrapped by this instance.
     * Throws an error if the object cannot be represented in Noodle.
     * @param newObject The object to apply.
     * @param template The template supporting the new object.
     */
    protected void setObject(Object newObject, NoodleObjectTemplate<?> template) {
        if (newObject == null) // Null is not allowed. If you want a NoodlePrimitive to be null, you set the NoodleObjectInstance to be null instead.
            throw new NoodleRuntimeException("NoodleObjectInstance cannot track a null object.");

        if (template == null || !template.isObjectSupported(newObject))
            throw new NoodleRuntimeException("The %s passed in is not an object which can be represented as %s.", Utils.getSimpleName(newObject), template != null ? template.getName() : "null");

        Object oldObject = this.object;
        this.object = newObject;
        this.template = template;

        // Update thread tracking of object instance.
        if (this.thread != null && this.refCount > 0 && oldObject != newObject) {
            if (oldObject != null && !this.thread.getHeap().getObjectInstances().remove(oldObject, this))
                throw new NoodleRuntimeException("The NoodleObjectInstance's old object was not registered to the NoodleObjectInstance.");

            NoodleObjectInstance otherInstance = this.thread.getHeap().getObjectInstances().put(newObject, this);
            if (otherInstance != null)
                throw new NoodleRuntimeException("The NoodleObjectInstance's new object was already registered to another NoodleObjectInstance.");
        }
    }

    /**
     * Gets the object instance as the given type.
     * @param typeClass The class of the type to use to get the object instance.
     * @param <TType> The type of the object instance to get.
     * @return objectInstance
     */
    public <TType> TType getOptionalObjectInstance(Class<TType> typeClass) {
        if (typeClass == null)
            throw new NoodleRuntimeException("Cannot get object reference with null type class.");
        if (this.object == null)
            return null;
        if (!typeClass.isInstance(this.object))
            return null;

        return typeClass.cast(this.object);
    }

    /**
     * Gets the object instance as the given type, or throw an exception if the object is null or cannot be cast to the type.
     * @param typeClass The class of the type to use to get the object instance.
     * @param <TType> The type of the object instance to get.
     * @return typedObjectInstance
     */
    public <TType> TType getRequiredObjectInstance(Class<TType> typeClass) {
        if (typeClass == null)
            throw new NoodleRuntimeException("Cannot get object reference with null type class.");
        if (this.object == null || !typeClass.isInstance(this.object))
            throw new NoodleRuntimeException("Expected an object reference of type %s/%s, but got %s instead.", Utils.getSimpleName(typeClass), Utils.getSimpleName(this.object));

        return typeClass.cast(this.object);
    }

    /**
     * Gets the object instance as the given type.
     * @param template The template to use to get the object instance.
     * @param <TType> The type of the object instance to get.
     * @return objectInstance
     */
    public <TType> TType getObjectInstance(NoodleObjectTemplate<TType> template, boolean allowBadTypes) {
        if (template == null)
            throw new NoodleRuntimeException("Cannot get object reference with null template.");
        if (this.object == null)
            return null;

        if (!template.getWrappedClass().isInstance(this.object)) {
            if (allowBadTypes)
                return null;
            throw new NoodleRuntimeException("Expected a(n) %s/%s Noodle object, but instead got a(n) '%s'.", template.getName(), Utils.getSimpleName(template.getWrappedClass()), Utils.getSimpleName(this.object));
        }

        return template.getWrappedClass().cast(this.object);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NoodleObjectInstance && Objects.equals(this.object, ((NoodleObjectInstance) other).object);
    }

    /**
     * Increments the number of active usages.
     */
    public void incrementRefCount() {
        if (this.refCount++ != 0)
            return;

        // Add this object instance to the thread heap.
        if (this.thread != null) {
            NoodleHeap heap = this.thread.getHeap();

            NoodleObjectInstance registeredInstance = heap.getObjectInstance(this.object);
            if (registeredInstance != null)
                throw new NoodleRuntimeException("Tried to register a new NoodleObjectInstance for a(n) %s object which already had a NoodleObjectInstance registered??", this.template);

            // Register new instance, since this primitive isn't registered.
            heap.getObjectInstances().put(this.object, this);
            heap.getObjectPool().add(this);

            // Call hook.
            getObjectTemplate().onObjectAddToHeap(this.thread, this.object, this);
        }
    }

    /**
     * Decreases the number of usages.
     */
    public void decrementRefCount() {
        if (this.refCount <= 0)
            throw new NoodleRuntimeException("refCount of NoodleObjectInstance cannot decrease below zero! (This error should not occur.)");

        if (--this.refCount > 0)
            return;

        // Free the object from the heap.
        if (this.thread != null) {
            NoodleHeap heap = this.thread.getHeap();

            // Remove this object from the heap.
            if (!heap.getObjectPool().remove(this))
                throw new NoodleRuntimeException("The NoodleObjectInstance was not registered in the heap.");
            if (!heap.getObjectInstances().remove(this.object, this))
                throw new NoodleRuntimeException("The NoodleObjectInstance's object was not registered properly to the instance.");

            // Call object free hook.
            getObjectTemplate().onObjectFree(this.thread, this.object, this);
        }
    }
}