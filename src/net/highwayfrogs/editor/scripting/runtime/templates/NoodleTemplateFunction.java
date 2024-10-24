package net.highwayfrogs.editor.scripting.runtime.templates;

import lombok.Getter;
import net.highwayfrogs.editor.scripting.compiler.NoodleCallHolder.INoodleCallable;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.functions.NoodleStaticTemplateFunction;
import net.highwayfrogs.editor.utils.Function3;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a function which is usable inside a template object.
 */
@Getter
public abstract class NoodleTemplateFunction<TObject> implements INoodleCallable {
    private final String name;
    private final Class<TObject> objectClass;
    private final List<String> argumentNames;
    private List<String> optionalArguments;

    public NoodleTemplateFunction(String label, Class<TObject> objectClass, String... argumentNames) {
        this.name = label;
        this.objectClass = objectClass;
        this.argumentNames = new ArrayList<>(Arrays.asList(argumentNames));
    }

    /**
     * Sets the optional arguments for this function.
     * @param optionalArguments The optional arguments to set.
     */
    protected void setOptionalArguments(String... optionalArguments) {
        this.optionalArguments = Arrays.asList(optionalArguments);
    }

    @Override
    public int getOptionalArgumentCount() {
        return this.optionalArguments != null ? this.optionalArguments.size() : 0;
    }

    /**
     * Executes the template function.
     * @param thread The thread to execute under.
     * @param thisRef The reference to the object execution occurs under.
     * @param args The arguments to the function.
     * @return returnValue
     */
    public NoodlePrimitive execute(NoodleThread<?> thread, TObject thisRef, NoodlePrimitive[] args) {
        if (thread == null)
            throw new NullPointerException("thread");

        if (this instanceof NoodleStaticTemplateFunction<?>) {
            if (thisRef != null)
                throw new NoodleRuntimeException("Static noodle function %s.%s was passed a non-null object instance.", this.objectClass.getSimpleName(), getSignature());
        } else {
            if (thisRef == null)
                throw new NoodleRuntimeException("Instance functions cannot run under a null reference. [%s.%s]", this.objectClass.getSimpleName(), getSignature());
        }

        if (thisRef != null && !this.objectClass.isInstance(thisRef))
            throw new NoodleRuntimeException("The template function %s.%s cannot execute as a %s object.", this.objectClass.getSimpleName(), getSignature(), Utils.getSimpleName(thisRef));

        int argumentCount = args != null ? args.length : 0;
        if (argumentCount < getArgumentCount())
            throw new NoodleRuntimeException("Function %s.%s expects %d arguments, but got only %d.", this.objectClass.getSimpleName(), getSignature(), getArgumentCount(), argumentCount);

        return this.executeImpl(thread, thisRef, args);
    }

    /**
     * Executes the template function.
     * @param thread The thread to execute under.
     * @param thisRef The reference to the object execution occurs under.
     * @param args The arguments to the function.
     * @return returnValue
     */
    protected abstract NoodlePrimitive executeImpl(NoodleThread<?> thread, TObject thisRef, NoodlePrimitive[] args);

    /**
     * A noodle template function which can have its logic delegated.
     */
    public static class LazyNoodleTemplateFunction<TObject> extends NoodleTemplateFunction<TObject> {
        private final Function3<NoodleThread<?>, TObject, NoodlePrimitive[], NoodlePrimitive> delegateHandler;

        public LazyNoodleTemplateFunction(String label, Class<TObject> objectClass, Function3<NoodleThread<?>, TObject, NoodlePrimitive[], NoodlePrimitive> delegateHandler, String... argumentNames) {
            super(label, objectClass, getArgumentNames(argumentNames));
            setOptionalArguments(getOptionalArgumentNames(argumentNames));
            this.delegateHandler = delegateHandler;
        }

        @Override
        protected NoodlePrimitive executeImpl(NoodleThread<?> thread, TObject thisRef, NoodlePrimitive[] args) {
            return this.delegateHandler.apply(thread, thisRef, args);
        }

        private static String[] getArgumentNames(String[] argumentNames) {
            int argumentCount = 0;
            for (int i = 0; i < argumentNames.length; i++) {
                String argumentName = argumentNames[i];
                if (!argumentName.startsWith("[") || !argumentName.endsWith("]"))
                    argumentCount++;
            }

            String[] results = new String[argumentCount];
            int resultIndex = 0;
            for (int i = 0; i < argumentNames.length; i++) {
                String argumentName = argumentNames[i];
                if (!argumentName.startsWith("[") || !argumentName.endsWith("]"))
                    results[resultIndex++] = argumentName;
            }

            return results;
        }

        private static String[] getOptionalArgumentNames(String[] argumentNames) {
            int argumentCount = 0;
            for (int i = 0; i < argumentNames.length; i++) {
                String argumentName = argumentNames[i];
                if (argumentName.startsWith("[") && argumentName.endsWith("]"))
                    argumentCount++;
            }

            String[] results = new String[argumentCount];
            int resultIndex = 0;
            for (int i = 0; i < argumentNames.length; i++) {
                String argumentName = argumentNames[i];
                if (argumentName.startsWith("[") && argumentName.endsWith("]"))
                    results[resultIndex++] = argumentName.substring(1, argumentName.length() - 1);
            }

            return results;
        }
    }
}