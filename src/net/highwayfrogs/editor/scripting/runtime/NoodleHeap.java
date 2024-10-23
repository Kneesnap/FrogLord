package net.highwayfrogs.editor.scripting.runtime;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScriptFunction;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;

import java.util.*;

/**
 * Represents the Noodle heap.
 */
@Getter
public class NoodleHeap {
    private final NoodleThread<?> thread;
    private final NoodleVariableMap globalVariables;
    private final Stack<NoodleVariableMap> functionVariables = new Stack<>();

    // Object Heap:
    // This contains heap object instances. This should never include null.
    private final List<NoodleObjectInstance> objectPool = new ArrayList<>();

    // Identity hashmaps are rare. We use it because we ONLY want to lookup objects by their reference,
    // NOT their value. EG: If we have two Location objects which .equals() would return true on,
    // they still must be tracked separately in Noodle. It would be very bug-prone if we did not handle it this way.
    // As such, it's important that we track instances by reference, not equality.
    private final IdentityHashMap<Object, NoodleObjectInstance> objectInstances = new IdentityHashMap<>();

    public NoodleHeap(NoodleThread<?> thread) {
        this.thread = thread;
        this.globalVariables = new NoodleVariableMap();
    }

    /**
     * Gets the corresponding NoodleObjectInstance to the supplied object from this heap, if one is present. Null otherwise
     * @param object The object to lookup.
     * @return The corresponding NoodleObjectInstance or null.
     */
    public NoodleObjectInstance getObjectInstance(Object object) {
        if (object instanceof NoodleObjectInstance)
            return (NoodleObjectInstance) object;

        return object != null ? this.objectInstances.get(object) : null;
    }

    /**
     * Gets the corresponding NoodleObjectType for the supplied object from this heap, if one is present.
     * Otherwise, it will determine the type by going through all templates.
     * @param object The object to determine to the template from.
     * @return The corresponding NoodleObjectType, or null.
     */
    @SuppressWarnings("unchecked")
    public <TType> NoodleObjectTemplate<TType> getObjectTemplate(TType object) {
        NoodleObjectInstance instance = getObjectInstance(object);
        if (instance != null && instance.getTemplate() != null) {
            return (NoodleObjectTemplate<TType>) instance.getTemplate();
        } else {
            return(NoodleObjectTemplate<TType>) getThread().getEngine().getTemplateFromObject(object);
        }
    }

    /**
     * Gets the variable stored by its name.
     * Prefers local variables over global variables, but will check both.
     * @param variableName The name of the variable to get.
     * @return variable
     */
    public NoodlePrimitive getVariable(String variableName) {
        if (this.functionVariables.size() > 0) {
            NoodlePrimitive localVariable = this.functionVariables.peek().get(variableName);
            if (localVariable != null)
                return localVariable;
        }

        return this.globalVariables.get(variableName);
    }

    /**
     * Sets the variable stored by its name.
     * Will always prefer local variables over global variables.
     * TODO: FUTURE (Could we require the 'var' keyword in order to declare a variable? It would make this much more explicit.)
     * @param variableName The name of the variable to create.
     * @param primitive The new value to store.
     */
    public void setVariable(String variableName, NoodlePrimitive primitive) {
        if (this.functionVariables.size() > 0) {
            this.functionVariables.peek().put(variableName, primitive);
        } else {
            this.globalVariables.put(variableName, primitive);
        }
    }

    /**
     * Gets a NoodleObjectInstance by its id.
     * @param identifier The id / unique identifier of the object instance.
     * @return objectInstance
     */
    public NoodleObjectInstance getInstanceById(int identifier) {
        if (identifier < 0 || identifier >= this.objectPool.size())
            throw new NoodleRuntimeException("Failed to get NoodleObjectInstance with ID: %d", identifier);
        return this.objectPool.get(identifier);
    }

    /**
     * Gets the identifier of a given NoodleObjectInstance.
     * @param instance The instance to get the identifier of.
     * @return instanceId
     */
    public int getInstanceId(NoodleObjectInstance instance) {
        if (instance.getThread() != this.thread && this.thread.getStatus() != NoodleThreadStatus.ERROR)
            throw new NoodleRuntimeException("Cannot get NoodleObjectInstance ID for instance which is registered to another thread! (Registered to '%s')", instance.getThread());

        int id = this.objectPool.indexOf(instance);
        if (id < 0 && this.thread.getStatus() != NoodleThreadStatus.ERROR)
            throw new NoodleRuntimeException("Cannot get NoodleObjectInstance ID for non-registered instance %s.", instance);
        return id;
    }

    public void pushFunctionContext(NoodleScriptFunction function, NoodlePrimitive[] arguments) {
        // Setup new local variable context.
        NoodleVariableMap newVariableContext = new NoodleVariableMap();
        this.functionVariables.push(newVariableContext);

        // Setup function parameters as local variables.
        for (int i = 0; i < function.getArgumentCount(); i++) {
            String argumentName = function.getArgumentNames().get(i);
            if (arguments.length > i)
                newVariableContext.put(argumentName, arguments[i].clone());
        }

        // Increase ref counts.
        newVariableContext.changeRefCount(true);
    }

    /**
     * Attempts to pop the active function context.
     * @return Whether a function context was popped.
     */
    public boolean popFunctionContext() {
        if (this.functionVariables.isEmpty())
            return false;

        NoodleVariableMap map = this.functionVariables.pop();
        map.changeRefCount(false);
        return true;
    }

    /**
     * Holds a variable set.
     */
    private static class NoodleVariableMap {
        private final Map<String, NoodlePrimitive> variableMap = new HashMap<>();

        /**
         * Gets the noodle primitive value stored for the given variable name.
         * @param name The name to lookup.
         * @return variable value
         */
        public NoodlePrimitive get(String name) {
            return this.variableMap.get(name);
        }

        /**
         * Stores a variable with a given name.
         * @param variableName The name of the variable to store.
         * @param primitive The variable.
         */
        public void put(String variableName, NoodlePrimitive primitive) {
            NoodlePrimitive oldPrimitive = this.variableMap.put(variableName, primitive);
            if (primitive != null) // Run before the oldPrimitive gets decreased in case they are the same.
                primitive.tryIncreaseRefCount();
            if (oldPrimitive != null)
                oldPrimitive.tryDecreaseRefCount();
        }

        /**
         * Changes the ref count of all values tracked within the map.
         * @param shouldIncrease Whether the ref count should be increased or decreased.
         */
        public void changeRefCount(boolean shouldIncrease) {
            for (NoodlePrimitive primitive : this.variableMap.values()) {
                if (primitive == null)
                    continue;

                if (shouldIncrease) {
                    primitive.tryIncreaseRefCount();
                } else {
                    primitive.tryDecreaseRefCount();
                }
            }
        }
    }
}