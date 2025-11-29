package net.highwayfrogs.editor.scripting.runtime.templates;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.scripting.NoodleScriptEngine;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.NoodleThread;
import net.highwayfrogs.editor.scripting.runtime.templates.functions.NoodleConstructor;
import net.highwayfrogs.editor.scripting.runtime.templates.functions.NoodleStaticFunction;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * This object is a helper which allows easily bridging Java code with Noodle.
 * Created by Kneesnap on 10/23/2024.
 */
public class NoodleJvmWrapper<TWrappedType> {
    @Getter private final Class<TWrappedType> wrappedClass;
    private final List<CachedConstructor<TWrappedType>> cachedConstructors = new ArrayList<>();
    private final Set<Constructor<TWrappedType>> registeredConstructors = new HashSet<>();
    private final Map<String, List<CachedMethod>> cachedMethods = new HashMap<>();
    private final Map<String, CachedField> cachedFields = new HashMap<>();
    private final Set<Method> registeredMethods = new HashSet<>();

    private static final NoodlePrimitive[] EMPTY_ARGUMENTS = new NoodlePrimitive[0];

    public NoodleJvmWrapper(@NonNull Class<TWrappedType> wrappedClass) {
        this.wrappedClass = wrappedClass;
    }

    /**
     * Makes the entirety of the class fully accessible in Noodle scripts.
     * @param excludeMethods The names of methods to explicitly exclude
     */
    public void makeFullyAccessible(String... excludeMethods) {
        makeFullyAccessible(null, excludeMethods);
    }

    /**
     * Makes the entirety of the class fully accessible in Noodle scripts.
     * If only methods returning supported objects are included, this method should be run only after all templates have been registered.
     * @param engine If an engine is provided, only methods which return supported objects will be included.
     * @param excludeMethods The names of methods to explicitly exclude
     */
    @SuppressWarnings("unchecked")
    public void makeFullyAccessible(NoodleScriptEngine engine, String... excludeMethods) {
        // Add constructors.
        for (Constructor<?> constructor : this.wrappedClass.getConstructors()) // getConstructors() only includes public methods.
            addConstructor((Constructor<TWrappedType>) constructor);

        // Add static fields. (Includes enum constants)
        for (Field field : this.wrappedClass.getFields())
            this.cachedFields.put(field.getName(), new CachedField(field));

        // Add public methods. (Including inherited ones)
        for (Method method : this.wrappedClass.getMethods()) { // getMethods() only includes public methods.
            if (engine != null) {
                if (!engine.isRepresentable(method.getReturnType()))
                    continue;

                boolean paramsRepresentable = true;
                Class<?>[] paramTypes = method.getParameterTypes();
                for (int i = 0; i < paramTypes.length; i++) {
                    Class<?> paramType = paramTypes[i];
                    if (paramType == null || !engine.isRepresentable(paramType)) {
                        paramsRepresentable = false;
                        break;
                    }
                }

                if (!paramsRepresentable)
                    continue;
            }

            if (excludeMethods != null && Utils.contains(excludeMethods, method.getName()))
                continue;

            if (Object.class.equals(method.getDeclaringClass()))
                continue;

            addFunction(method);
        }
    }

    /**
     * Register a Java class constructor to become accessible within a Noodle template.
     * The method will be searched by checking the public members of the given class (recursively checking its super classes as well)
     * @param paramTypes the arguments to register
     */
    public void addConstructor(Class<?>... paramTypes) {
        Constructor<TWrappedType> constructor;
        try {
            constructor = this.wrappedClass.getConstructor(paramTypes); // This will return public members of super-classes.
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException("No constructor was found in " + this.wrappedClass.getSimpleName() + " with the given parameter types.");
        }

        addConstructor(constructor);
    }

    /**
     * Register a Java class constructor to make accessible within a Noodle template.
     * @param constructor the constructor to register
     */
    public void addConstructor(Constructor<TWrappedType> constructor) {
        if (constructor == null)
            throw new NullPointerException("constructor");
        if (!constructor.getDeclaringClass().isAssignableFrom(this.wrappedClass))
            throw new IllegalArgumentException("The constructor for '" + constructor.getDeclaringClass().getSimpleName() + "' cannot be registered to the " + this.wrappedClass.getSimpleName() + " class.");
        if (!this.registeredConstructors.add(constructor))
            throw new IllegalArgumentException("The constructor for '" + constructor.getDeclaringClass().getSimpleName() + "' is already registered.");

        CachedConstructor<TWrappedType> newCachedConstructor = new CachedConstructor<>(constructor);

        // Find the index to insert the method at.
        int i;
        for (i = 0; i < this.cachedConstructors.size(); i++)
            if (this.cachedConstructors.get(i).getParameterCount() > newCachedConstructor.getParameterCount())
                break;

        if (i == this.cachedConstructors.size()) {
            this.cachedConstructors.add(newCachedConstructor);
        } else {
            this.cachedConstructors.add(i, newCachedConstructor);
        }
    }

    /**
     * Register a Java method to make accessible within a Noodle template.
     * The method will be searched by checking the public members of the given class (recursively checking its super classes as well)
     * @param methodName the name of the method to register
     * @param paramTypes the function arguments to register
     */
    public void addFunction(String methodName, Class<?>... paramTypes) {
        Method method;
        try {
            method = this.wrappedClass.getMethod(methodName, paramTypes); // This will return public members of super-classes.
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException("No method named '" + methodName + "' was found in " + this.wrappedClass.getSimpleName() + " with the given parameter types.");
        }

        addFunction(method);
    }

    /**
     * Register a Java method to make accessible within a Noodle template.
     * @param method the method to register
     */
    public void addFunction(Method method) {
        if (method == null)
            throw new NullPointerException("method");
        if (!method.getDeclaringClass().isAssignableFrom(this.wrappedClass))
            throw new IllegalArgumentException("The method '" + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "' cannot be registered to the " + this.wrappedClass.getSimpleName() + " class.");
        if (!this.registeredMethods.add(method))
            throw new IllegalArgumentException("The method '" + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "' is already registered.");

        CachedMethod newCachedMethod = new CachedMethod(method);
        List<CachedMethod> cachedMethods = this.cachedMethods.computeIfAbsent(method.getName(), name -> new ArrayList<>());

        // Find the index to insert the method at.
        int i;
        for (i = 0; i < cachedMethods.size(); i++)
            if (cachedMethods.get(i).getParameterCount() > newCachedMethod.getParameterCount())
                break;

        if (i == cachedMethods.size()) {
            cachedMethods.add(newCachedMethod);
        } else {
            cachedMethods.add(i, newCachedMethod);
        }
    }

    /**
     * Register the functions tracked by this wrapper to the noodle template.
     * If there are any conflicts with functions previously defined, any impacted methods will be skipped.
     * @param template the template to register the functions to
     */
    public void registerFunctions(NoodleObjectTemplate<TWrappedType> template) {
        if (template == null)
            throw new NullPointerException("template");

        // Register constructors:
        int lastArgumentCount = -1;
        for (int i = 0; i < this.cachedConstructors.size(); i++) {
            CachedConstructor<TWrappedType> cachedConstructor = this.cachedConstructors.get(i);
            int tempArgumentCount = cachedConstructor.getParameterCount();

            if (lastArgumentCount != tempArgumentCount) {
                template.addStaticFunction(new NoodleJvmConstructor<>(this, tempArgumentCount));
                lastArgumentCount = tempArgumentCount;
            }
        }

        // Register static fields.
        for (Entry<String, CachedField> entry : this.cachedFields.entrySet()) {
            CachedField field = entry.getValue();
            if (!field.isStaticallyAccessible())
                continue;

            template.addStaticGetter(entry.getKey(),
                    thread -> thread.getStack().pushObject(field.get(null)));
            if (field.isAllowSetter())
                template.addStaticSetter(entry.getKey(),
                        (thread, value) -> field.set(null, value.getAsRawObject()));
        }

        // Register methods:
        for (Entry<String, List<CachedMethod>> entry : this.cachedMethods.entrySet()) {
            String methodName = entry.getKey();
            List<CachedMethod> methods = entry.getValue();

            // Static methods.
            lastArgumentCount = -1;
            for (int i = 0; i < methods.size(); i++) {
                CachedMethod cachedMethod = methods.get(i);
                int tempArgumentCount = cachedMethod.getParameterCount();
                if (!cachedMethod.isStaticMethod())
                    continue;

                if (lastArgumentCount != tempArgumentCount) {
                    template.addStaticFunctionIfMissing(new NoodleJvmStaticFunction<>(this, methodName, tempArgumentCount));
                    lastArgumentCount = tempArgumentCount;
                }
            }

            // Instance methods.
            lastArgumentCount = -1;
            for (int i = 0; i < methods.size(); i++) {
                CachedMethod cachedMethod = methods.get(i);
                if (cachedMethod.isStaticMethod())
                    continue;

                int tempArgumentCount = cachedMethod.getParameterCount();

                if (lastArgumentCount != tempArgumentCount) {
                    template.addFunctionIfMissing(new NoodleJvmTemplateFunction<>(this, methodName, tempArgumentCount));
                    lastArgumentCount = tempArgumentCount;
                }
            }

            // Add corresponding accessors for 'X' in the case of 'getX()' and 'setX()'.
            if (methods.size() > 0 && methodName.length() >= 4 && (methodName.charAt(1) == 'e') && (methodName.charAt(2) == 't') && Character.isUpperCase(methodName.charAt(3)) && (methodName.charAt(0) == 's' || methodName.charAt(0) == 'g')) {
                String fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                CachedMethod method = methods.get(0);

                // Create a getter binding.
                if (method.getParameterCount() == 0 && methodName.charAt(0) == 'g' && template.getGetter(fieldName) == null)
                    template.addGetter(fieldName, (thread, object) -> thread.getStack().pushObject(executeMethod(methodName, object, EMPTY_ARGUMENTS)));

                // Create a setter binding.
                if (method.getParameterCount() == 1 && methodName.charAt(0) == 's' && template.getSetter(fieldName) == null)
                    template.addSetter(fieldName, (thread, object, newValue) -> executeMethod(methodName, object, new NoodlePrimitive[] {newValue}));
            }

            // Add corresponding accessor for 'X' in the case of 'isX()'.
            if (methods.size() > 0 && methodName.length() >= 3 && methodName.charAt(0) == 'i' && methodName.charAt(1) == 's' && Character.isUpperCase(methodName.charAt(2))) {
                String fieldName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                CachedMethod method = methods.get(0);

                // Create a getter binding.
                if (method.getParameterCount() == 0 && template.getGetter(fieldName) == null)
                    template.addGetter(fieldName, (thread, object) -> thread.getStack().pushObject(executeMethod(methodName, object, EMPTY_ARGUMENTS)));
            }
        }
    }

    private Object executeMethod(String methodName, TWrappedType thisRef, NoodlePrimitive[] args) {
        if (args == null)
            throw new NullPointerException("args");

        List<CachedMethod> methods = this.cachedMethods.get(methodName);
        if (methods == null)
            throw new NoodleRuntimeException("The method %s.%s(%s) is not tracked", this.wrappedClass.getSimpleName(), methodName, NoodlePrimitive.getArgumentDisplayTypesAsString(args));

        CachedMethod bestMethod = getBestMatch(methods, args);
        if (bestMethod == null)
            throw new NoodleRuntimeException("Could not find a method executable as '%s.%s(%s)'.", this.wrappedClass.getSimpleName(), methodName, NoodlePrimitive.getArgumentDisplayTypesAsString(args));

        // Execute the function.
        Object[] javaArguments = getInvocationArguments(bestMethod.getParameterTypes(), args);
        try {
            return bestMethod.getExecutable().invoke(thisRef, javaArguments);
        } catch (Throwable th) {
            throw new NoodleRuntimeException(th, "Encountered an error while executing %s.%s(%s)", this.wrappedClass.getSimpleName(), methodName, NoodlePrimitive.getArgumentDisplayTypesAsString(args));
        }
    }

    private Object invokeConstructor(NoodlePrimitive[] args) {
        CachedConstructor<TWrappedType> bestConstructor = getBestMatch(this.cachedConstructors, args);
        if (bestConstructor == null)
            throw new NoodleRuntimeException("Could not find a constructor executable as '%s(%s)'.", this.wrappedClass.getSimpleName(), NoodlePrimitive.getArgumentDisplayTypesAsString(args));

        // Invoke the constructor.
        Object[] javaArguments = getInvocationArguments(bestConstructor.getParameterTypes(), args);
        try {
            return bestConstructor.getExecutable().newInstance(javaArguments);
        } catch (Throwable th) {
            throw new NoodleRuntimeException(th, "Encountered an error while invoking 'new %s(%s)'", this.wrappedClass.getSimpleName(), NoodlePrimitive.getArgumentDisplayTypesAsString(args));
        }
    }

    private String[] getArgumentNames(String methodName, int argumentCount) {
        List<CachedMethod> methods = this.cachedMethods.get(methodName);
        if (methods == null)
            throw new NoodleRuntimeException("There are no functions registered for %s.%s(%d args).", this.wrappedClass.getSimpleName(), methodName, argumentCount);

        return getArgumentNames(methods, argumentCount);
    }

    private String[] getArgumentNames(List<? extends CachedExecutable<?>> executables, int argumentCount) {
        // Populate the default arguments.
        Set<String> seenNames = new HashSet<>();
        StringBuilder nameBuilder = new StringBuilder();
        String[] arguments = new String[argumentCount];
        for (int i = 0; i < arguments.length; i++) {
            // Add each unique method parameter name.
            seenNames.clear();
            for (int j = 0; j < executables.size(); j++) {
                CachedExecutable<?> testExecutable = executables.get(j);
                if (testExecutable.getParameterCount() != argumentCount)
                    continue; // Wrong number of parameters.

                String paramName = testExecutable.getParameterNames()[i];
                if (paramName == null || !seenNames.add(paramName))
                    continue;

                if (nameBuilder.length() > 0)
                    nameBuilder.append('|');
                nameBuilder.append(paramName);
            }

            arguments[i] = nameBuilder.toString();
            nameBuilder.setLength(0);
        }

        return arguments;
    }

    private static <TExecutable extends CachedExecutable<?>> TExecutable getBestMatch(List<TExecutable> executables, NoodlePrimitive[] args) {
        TExecutable bestExecutable = null;
        Class<?>[] bestParams = null;
        for (int i = 0; i < executables.size(); i++) {
            TExecutable testMethod = executables.get(i);
            Class<?>[] executableParams = testMethod.getParameterTypes();
            if (executableParams.length != args.length)
                continue; // Wrong number of parameters.

            boolean betterMatch = true;
            for (int j = 0; j < executableParams.length; j++) {
                Class<?> testParamType = executableParams[j];
                Class<?> bestParamType = bestParams != null ? bestParams[j] : null;

                // Ensure the method accepts the given value.
                if (!isPrimitiveAtLeastAsSpecific(testParamType, bestParamType, args[j])) {
                    betterMatch = false;
                    break;
                }
            }

            if (betterMatch) {
                bestExecutable = testMethod;
                bestParams = executableParams;
            }
        }

        return bestExecutable;
    }

    private enum JvmPrimitiveGroup {
        BOOLEAN,
        INTEGER,
        FLOAT,
        REFERENCE_TYPE;

        public static JvmPrimitiveGroup getType(Class<?> clazz) {
            if (clazz == null)
                return null;

            if (!clazz.isPrimitive()) {
                return REFERENCE_TYPE;
            } else if (boolean.class.equals(clazz)) {
                return BOOLEAN;
            } else if (float.class.equals(clazz) || double.class.equals(clazz)) {
                return FLOAT;
            } else if (byte.class.equals(clazz) || short.class.equals(clazz) || int.class.equals(clazz) || long.class.equals(clazz) || char.class.equals(clazz)) {
                return INTEGER;
            } else {
                return null;
            }
        }
    }

    /**
     * Test if the given NoodlePrimitive is at least as specialized as the previously chosen target.
     * @param currTarget The current parameter type
     * @param prevTarget The previous parameter type which the current one must be a better match than
     * @param value the value to test
     * @return true iff the current parameter is at least as specific to the given primitive as the previous one.
     */
    private static boolean isPrimitiveAtLeastAsSpecific(Class<?> currTarget, Class<?> prevTarget, NoodlePrimitive value) {
        if (value == null || value.isNull()) {
            return !currTarget.isPrimitive();
        } else if (!currTarget.isPrimitive()) {
            Object refObject = value.getAsRawObject(); // Autoboxes primitives when appropriate.
            if (!currTarget.isInstance(refObject))
                return false;

            // Prefer more explicitly typed options.
            return (prevTarget == null) || !prevTarget.isInstance(refObject) || prevTarget.isAssignableFrom(currTarget);
        } else {
            JvmPrimitiveGroup currTargetGroup = JvmPrimitiveGroup.getType(currTarget);
            switch (currTargetGroup) {
                case BOOLEAN:
                    return value.isBoolean();
                case FLOAT:
                    return value.isDecimalNumber();
                case INTEGER:
                    return value.isIntegerNumber();
                default:
                    throw new NoodleRuntimeException("Unsupported PrimitiveType for %s.", value);
            }
        }
    }

    private static Object castValue(Class<?> target, NoodlePrimitive input) {
        if (input == null || input.isNull()) {
            if (target.isPrimitive())
                throw new NoodleRuntimeException("Cannot return null for '%s', since it is a primitive!", target.getSimpleName());

            return null;
        } else if (String.class.equals(target)) {
            return input.getAsString();
        } else if (target.isPrimitive()) {
            return getPrimitiveAsObject(target, input);
        } else if (target.isArray()) {
            return NoodleArrayTemplate.unboxArray(target, input);
        } else {
            if (!input.isObjectReference()) {
                Object rawObject = input.getAsRawObject();
                if (rawObject != null)
                    return rawObject;

                throw new NoodleRuntimeException("Tried to obtain the %s as a(n) %s, but the primitive was not an object reference!", input, target);
            }

            return input.getObjectReference().getRequiredObjectInstance(target);
        }
    }

    private static Object getPrimitiveAsObject(Class<?> target, NoodlePrimitive input) {
        if (!target.isPrimitive()) {
            if (Object.class.equals(target)) { // Generic types could be anything, so allow more complex options.
                return input.getAsRawObject();
            } else if (Integer.class.equals(target)) {
                return input.getInteger();
            } else if (Float.class.equals(target)) {
                return input.getFloat();
            } else if (Boolean.class.equals(target)) {
                return input.getBoolean();
            } else if (Long.class.equals(target)) {
                return input.getLong();
            } else if (Double.class.equals(target)) {
                return input.getDouble();
            } else if (Short.class.equals(target)) {
                return input.getShort();
            } else if (Byte.class.equals(target)) {
                return input.getByte();
            } else if (Character.class.equals(target)) {
                return input.getChar();
            } else {
                // isPrimitive() is false so it might be valid for this to not be a number.
                return null;
            }
        }

        // isPrimitive() is true, so this is expected to work.
        if (int.class.equals(target)) {
            return input.getInteger();
        } else if (float.class.equals(target)) {
            return input.getFloat();
        } else if (boolean.class.equals(target)) {
            return input.isTrueValue();
        } else if (long.class.equals(target)) {
            return input.getLong();
        } else if (double.class.equals(target)) {
            return input.getDouble();
        } else if (short.class.equals(target)) {
            return input.getShort();
        } else if (byte.class.equals(target)) {
            return input.getByte();
        } else if (char.class.equals(target)) {
            return input.getChar();
        } else {
            throw new NoodleRuntimeException("Unsupported primitive type: '%s' for %s.", target.getSimpleName(), input);
        }
    }

    private static Object[] getInvocationArguments(Class<?>[] parameterTypes, NoodlePrimitive[] args) {
        if (parameterTypes.length != args.length)
            throw new NoodleRuntimeException("parameterTypeCount (%d) did not match the provided argument count (%d).", parameterTypes.length, args.length);

        Object[] tempArguments = new Object[args.length];
        for (int i = 0; i < tempArguments.length; i++)
            tempArguments[i] = castValue(parameterTypes[i], args[i]);

        return tempArguments;
    }

    private static class NoodleJvmTemplateFunction<TObject> extends NoodleTemplateFunction<TObject> {
        private final NoodleJvmWrapper<TObject> jvmWrapper;

        public NoodleJvmTemplateFunction(NoodleJvmWrapper<TObject> jvmWrapper, String methodName, int argumentCount) {
            super(methodName, jvmWrapper.getWrappedClass(), jvmWrapper.getArgumentNames(methodName, argumentCount));
            this.jvmWrapper = jvmWrapper;
        }

        @Override
        protected NoodlePrimitive executeImpl(NoodleThread<?> thread, TObject thisRef, NoodlePrimitive[] args) {
            return thread.getStack().pushObject(this.jvmWrapper.executeMethod(getName(), thisRef, args), false);
        }
    }

    private static class NoodleJvmStaticFunction<TObject> extends NoodleStaticFunction<TObject> {
        private final NoodleJvmWrapper<TObject> jvmWrapper;

        public NoodleJvmStaticFunction(NoodleJvmWrapper<TObject> jvmWrapper, String methodName, int argumentCount) {
            super(methodName, jvmWrapper.getWrappedClass(), jvmWrapper.getArgumentNames(methodName, argumentCount));
            this.jvmWrapper = jvmWrapper;
        }

        @Override
        protected NoodlePrimitive executeImpl(NoodleThread<?> thread, NoodlePrimitive[] args) {
            return thread.getStack().pushObject(this.jvmWrapper.executeMethod(getName(), null, args), false);
        }
    }

    private static class NoodleJvmConstructor<TObject> extends NoodleConstructor<TObject> {
        private final NoodleJvmWrapper<TObject> jvmWrapper;

        public NoodleJvmConstructor(NoodleJvmWrapper<TObject> jvmWrapper, int argumentCount) {
            super(jvmWrapper.getWrappedClass(), jvmWrapper.getArgumentNames(jvmWrapper.cachedConstructors, argumentCount));
            this.jvmWrapper = jvmWrapper;
        }

        @Override
        protected NoodlePrimitive executeImpl(NoodleThread<?> thread, NoodlePrimitive[] args) {
            return thread.getStack().pushObject(this.jvmWrapper.invokeConstructor(args));
        }
    }

    @Getter
    private static class CachedField {
        private final String fieldName;
        private final Field field;
        private final boolean allowSetter;
        private final boolean staticallyAccessible;

        public CachedField(Field field) {
            this.fieldName = field.getName();
            this.field = field;
            this.allowSetter = !Modifier.isFinal(field.getModifiers());
            this.staticallyAccessible = Modifier.isStatic(field.getModifiers());
        }

        public Object get(Object instance) {
            try {
                return this.field.get(instance);
            } catch (IllegalAccessException iae) {
                throw new NoodleRuntimeException(iae, "Failed to get the value of field %s for instance %s.", this.fieldName, instance);
            }
        }

        public void set(Object instance, Object value) {
            try {
                this.field.set(instance, value);
            } catch (IllegalAccessException iae) {
                throw new NoodleRuntimeException(iae, "Failed to set the value of field %s for instance %s to %s.", this.fieldName, instance, value);
            }
        }
    }

    @Getter
    private static class CachedExecutable<TExecutable extends Executable> {
        private final TExecutable executable;
        private final Class<?>[] parameterTypes;
        private final String[] parameterNames;

        public CachedExecutable(TExecutable executable) {
            this.executable = executable;
            this.parameterTypes = executable.getParameterTypes();
            this.parameterNames = getParameterNames(executable);
        }

        /**
         * Represents the number of parameters accepted.
         */
        public int getParameterCount() {
            return this.parameterTypes.length;
        }

        private static String[] getParameterNames(Executable executable) {
            List<String> parameterNames = new ArrayList<>();
            for (Parameter parameter : executable.getParameters())
                parameterNames.add(parameter.isNamePresent() ? parameter.getName() : null);

            return parameterNames.toArray(new String[0]);
        }
    }

    @Getter
    private static class CachedMethod extends CachedExecutable<Method> {
        private final boolean staticMethod;

        public CachedMethod(Method method) {
            super(method);
            this.staticMethod = Modifier.isStatic(method.getModifiers());
        }
    }

    @Getter
    private static class CachedConstructor<TWrappedType> extends CachedExecutable<Constructor<TWrappedType>> {
        public CachedConstructor(Constructor<TWrappedType> constructor) {
            super(constructor);
        }
    }
}
