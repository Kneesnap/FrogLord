package net.highwayfrogs.editor.scripting.compiler;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.NoodleScriptFunction;
import net.highwayfrogs.editor.scripting.compiler.NoodleCallHolder.INoodleCallable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Holds noodle callables
 */
@Getter
public class NoodleCallHolder<TCallable extends INoodleCallable> {
    private final Map<String, List<TCallable>> callablesByName = new HashMap<>();

    /**
     * Clears this holder.
     */
    public void clear() {
        this.callablesByName.clear();
    }

    /**
     * Gets the values stored by this call holder.
     */
    public Collection<List<TCallable>> values() {
        return this.callablesByName.values();
    }

    /**
     * Runs a piece of code for all tracked callables.
     * @param handler The handler to run.
     */
    public void forEach(Consumer<TCallable> handler) {
        for (List<TCallable> list : this.callablesByName.values())
            for (int i = 0; i < list.size(); i++)
                handler.accept(list.get(i));
    }

    /**
     * Returns the number of entries stored.
     */
    public int size() {
        return this.callablesByName.size();
    }

    /**
     * Tests if there is a callable tracked by a given name.
     * @param name The name of the callable to get.
     */
    public boolean hasByName(String name) {
        // Tests if the function is already defined.
        List<TCallable> callables = this.callablesByName.get(name);
        return callables != null && callables.size() > 0;
    }

    /**
     * Tests if there is a callable tracked by a given name and argument count.
     * @param name The name of the callable to get.
     * @param argumentCount The amount of arguments to get.
     */
    public boolean hasByNameAndArgumentCount(String name, int argumentCount) {
        return getByNameAndArgumentCount(name, argumentCount) != null;
    }

    /**
     * Gets a callable by its name and argument count.
     * @param name The name of the callable to get.
     * @param argumentCount The amount of arguments to get.
     * @return callable
     */
    public TCallable getByNameAndArgumentCount(String name, int argumentCount) {
        // Tests if the function is already defined.
        List<TCallable> callables = this.callablesByName.get(name);
        if (callables == null || callables.isEmpty())
            return null;

        // Search to find the matching option.
        // NOTE: Could add binary search here, but we'd also need to sort for insertion.
        for (int i = 0; i < callables.size(); i++) {
            TCallable callable = callables.get(i);
            if (argumentCount >= callable.getArgumentCount() && argumentCount <= callable.getMaximumArgumentCount())
                return callables.get(i);
        }

        // Didn't find a match, return null.
        return null;
    }

    /**
     * Attempts to register the callable.
     * @param callable The callable to register.
     * @return Whether it was successfully registered.
     */
    public boolean registerCallable(TCallable callable) {
        if (callable == null)
            throw new NoodleCompilerException("Cannot register null callable.");
        if (callable.getName() == null)
            throw new NoodleCompilerException("The callable's name was null.");

        // Tests if the function is already defined.
        TCallable foundCallable = getByNameAndArgumentCount(callable.getName(), callable.getArgumentCount());
        if (foundCallable != null)
            return false;

        // Register callable.
        this.callablesByName.computeIfAbsent(callable.getName(), key -> new ArrayList<>()).add(callable);
        return true;
    }

    /**
     * Represents something in noodle which can be called with a variable number of arguments.
     */
    public interface INoodleCallable {
        /**
         * Gets the name of this callable.
         */
        String getName();

        /**
         * Gets the argument count.
         */
        List<String> getArgumentNames();

        /**
         * Gets the argument count.
         */
        default int getArgumentCount() {
            List<String> argumentNames = getArgumentNames();
            return argumentNames != null ? argumentNames.size() : 0;
        }

        /**
         * Gets the optional argument count.
         */
        default int getOptionalArgumentCount() {
            return 0;
        }

        /**
         * Gets the maximum count.
         */
        default int getMaximumArgumentCount() {
            return getArgumentCount() + getOptionalArgumentCount();
        }

        /**
         * Writes the signature of this callable to a string builder.
         * @param builder The builder to write the signature to.
         */
        default void writeSignature(StringBuilder builder) {
            builder.append(getName());

            // Write macro arguments.
            int argumentCount = getArgumentCount();
            if (argumentCount > 0 || (this instanceof NoodleScriptFunction)) {
                builder.append('(');
                for (int i = 0; i < argumentCount; i++) {
                    if (i > 0)
                        builder.append(", ");
                    builder.append(getArgumentNames().get(i));
                }

                builder.append(')');
            }
        }

        /**
         * Gets the callable signature.
         */
        default String getSignature() {
            StringBuilder builder = new StringBuilder();
            writeSignature(builder);
            return builder.toString();
        }
    }
}