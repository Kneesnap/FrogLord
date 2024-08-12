package net.highwayfrogs.editor.system.classlist;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a global class registry, containing all classes of a given type which can be saved/loaded.
 * Created by Kneesnap on 7/16/2024.
 */
public class GlobalClassRegistry<TBase> implements Cloneable {
    private final Map<String, ClassEntry<TBase>> entriesByIdentifier = new HashMap<>();
    private final Map<String, ClassEntry<TBase>> entriesByClassName = new HashMap<>();
    private final Map<Class<? extends TBase>, ClassEntry<TBase>> entriesByClass = new HashMap<>();

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public GlobalClassRegistry<TBase> clone() {
        GlobalClassRegistry<TBase> clone = new GlobalClassRegistry<>();
        clone.register(this);
        return clone;
    }

    /**
     * Registers one or more class registrations defined as enums into the registry.
     * Throws an error if any of the enums have already been registered.
     * @param enumValues the value(s) to register
     * @return this
     */
    @SafeVarargs
    public final <TEnum extends Enum<TEnum>> GlobalClassRegistry<TBase> register(IEnumClassRegistryEntrySource<TEnum, TBase>... enumValues) {
        for (int i = 0; i < enumValues.length; i++) {
            IEnumClassRegistryEntrySource<TEnum, ? extends TBase> entry = enumValues[i];
            registerClass(entry.getTargetEntryClass(), entry.getIdentifier());
        }

        return this;
    }

    /**
     * Registers one or more class registration sources into the registry.
     * Throws an error if any of the classes have already been registered.
     * @param sources the source(s) to register the classes from
     * @return this
     */
    @SafeVarargs
    public final GlobalClassRegistry<TBase> register(IClassRegistryEntrySource<? extends TBase>... sources) {
        for (int i = 0; i < sources.length; i++) {
            IClassRegistryEntrySource<? extends TBase> source = sources[i];
            registerClass(source.getTargetEntryClass(), source.getIdentifier());
        }

        return this;
    }

    /**
     * Registers one or more class registration sources into the registry.
     * Throws an error if any of the classes have already been registered.
     * @param sources the source(s) to register the classes from
     * @return this
     */
    public final GlobalClassRegistry<TBase> register(Iterable<IClassRegistryEntrySource<? extends TBase>> sources) {
        for (IClassRegistryEntrySource<? extends TBase> source : sources)
            registerClass(source.getTargetEntryClass(), source.getIdentifier());

        return this;
    }

    /**
     * Registers one or more class registration sources into the registry.
     * Throws an error if any of the classes have already been registered.
     * @param otherGlobalRegistry otherGlobalRegistry another global registry to register the classes from
     * @return this
     */
    public final GlobalClassRegistry<TBase> register(GlobalClassRegistry<? extends TBase> otherGlobalRegistry) {
        for (ClassEntry<? extends TBase> otherClassEntry : otherGlobalRegistry.entriesByClass.values())
            registerClass(otherClassEntry);

        return this;
    }
    
    /**
     * Registers a class as an entry into the registry.
     * Throws an error if either the class or the identifier have already been registered.
     * @param entryClass the class to register
     * @param identifier a unique string identifier to the entry
     * @return this
     * @param <TClass> the class to register (extends the base)
     */
    public <TClass extends TBase> GlobalClassRegistry<TBase> registerClass(Class<TClass> entryClass, String identifier) {
        if (entryClass == null)
            throw new NullPointerException("entryClass");
        if (identifier == null || identifier.isEmpty())
            throw new RuntimeException("Cannot register " + entryClass + " with a null/empty identifier string!");
        if (identifier.length() > IndexedClassRegistry.MAXIMUM_IDENTIFIER_LENGTH)
            throw new RuntimeException("The identifier '" + identifier + "' is too large to use in a class registry.");

        if (this.entriesByIdentifier.containsKey(identifier))
            throw new RuntimeException("An entry is already registered with the identifier '" + identifier + "'.");
        String className = entryClass.getName();
        if (this.entriesByClassName.containsKey(className))
            throw new RuntimeException("The class name '" + className + "' is already registered.");
        if (this.entriesByClass.containsKey(entryClass))
            throw new RuntimeException("The class '" + className + "' is already registered.");

        ClassEntry<TBase> newEntry = new ClassEntry<>(entryClass, identifier);
        this.entriesByIdentifier.put(identifier, newEntry);
        this.entriesByClassName.put(className, newEntry);
        this.entriesByClass.put(entryClass, newEntry);
        return this;
    }

    /**
     * Registers a pre-existing class entry into the registry.
     * Throws an error if either the class or the identifier have already been registered.
     * @param newEntry the entry to register
     * @return this
     */
    @SuppressWarnings("unchecked")
    private GlobalClassRegistry<TBase> registerClass(ClassEntry<? extends TBase> newEntry) {
        if (newEntry == null)
            throw new NullPointerException("newEntry");
        
        // Validate tracking by identifier.
        String identifier = newEntry.getIdentifier();
        ClassEntry<TBase> oldEntryByIdentifier = this.entriesByIdentifier.get(identifier);
        if (oldEntryByIdentifier != null) {
            if (oldEntryByIdentifier.equals(newEntry))
                return this; // Skip if already registered.
            
            throw new RuntimeException("An entry is already registered with the identifier '" + identifier + "'.");
        }
        
        // Validate tracking by class.
        Class<? extends TBase> entryClass = newEntry.getEntryClass();
        ClassEntry<TBase> oldEntryByClass = this.entriesByClass.get(entryClass);
        if (oldEntryByClass != null) {
            if (oldEntryByClass.equals(newEntry))
                return this; // Skip if already registered.

            throw new RuntimeException("An entry is already registered for the class '" + Utils.getSimpleName(entryClass) + "'.");
        }

        // Validate tracking by class name.
        String entryClassName = entryClass.getName();
        ClassEntry<TBase> oldEntryByClassName = this.entriesByClassName.get(entryClassName);
        if (oldEntryByClassName != null) {
            if (oldEntryByClassName.equals(newEntry))
                return this; // Skip if already registered.

            throw new RuntimeException("An entry is already registered for the class named '" + entryClassName + "'.");
        }
        
        ClassEntry<TBase> castedEntry = (ClassEntry<TBase>) newEntry; 
        this.entriesByIdentifier.put(identifier, castedEntry);
        this.entriesByClassName.put(entryClassName, castedEntry);
        this.entriesByClass.put(entryClass, castedEntry);
        return this;
    }

    /**
     * Gets the class entry by its class.
     * @param entryClass the entry class to lookup
     * @return classEntry, or null
     */
    public ClassEntry<TBase> getClassEntry(Class<? extends TBase> entryClass) {
        return this.entriesByClass.get(entryClass);
    }

    /**
     * Gets the class entry for an object instance.
     * If the classEntry is not found, an exception is thrown. (Null is returned if the object is explicitly null)
     * @param object the object instance to resolve the classEntry for
     * @return classEntry
     */
    @SuppressWarnings("unchecked")
    public ClassEntry<TBase> requireClassEntry(TBase object) {
        if (object == null)
            return null; // Object is null, so returning null will let us recreate it as null.

        ClassEntry<TBase> classEntry = getClassEntry((Class<? extends TBase>) object.getClass());
        if (classEntry == null)
            throw new RuntimeException("Object " + Utils.getSimpleName(object) + " could not be saved, as its class is not registered in the " + Utils.getSimpleName(this) + ".");

        return classEntry;
    }

    /**
     * Gets the class entry by its identifier or its full class name.
     * @param identifier the identifier to apply
     * @param fullClassName the full class name
     * @return classEntry, or null
     */
    public ClassEntry<TBase> getClassEntry(String identifier, String fullClassName) {
        ClassEntry<TBase> classEntry = this.entriesByClassName.get(fullClassName);
        return classEntry != null ? classEntry : this.entriesByIdentifier.get(identifier);
    }

    /**
     * Represents an object which can provide the information to register a class.
     */
    public interface IClassRegistryEntrySource<TBase> {
        /**
         * Gets the identifier used to identify the class.
         * @return identifier
         */
        String getIdentifier();

        /**
         * Gets the class display name.
         */
        Class<? extends TBase> getTargetEntryClass();
    }

    /**
     * Represents an object which can provide the information to register a class.
     */
    public interface IEnumClassRegistryEntrySource<TEnum extends Enum<TEnum>, TBase> {
        /**
         * Gets the identifier used to identify the class.
         * @return identifier
         */
        @SuppressWarnings("unchecked")
        default String getIdentifier() {
            return ((TEnum) this).name();
        }

        /**
         * Gets the class display name.
         */
        Class<? extends TBase> getTargetEntryClass();
    }

    /**
     * Represents a registry class entry.
     * This can safely be shared across multiple global class registries as it is immutable (excluding constructor cache, which is valid across all instances)
     */
    @RequiredArgsConstructor
    public static class ClassEntry<TBase> {
        @Getter private final Class<? extends TBase> entryClass;
        @Getter private final String identifier;
        private final Map<Class<?>, Constructor<?>> singleArgumentConstructors = new ConcurrentHashMap<>();
        private final Map<Class<?>[], Constructor<?>> doubleArgumentConstructors = new ConcurrentHashMap<>();
        private final Map<Class<?>[], Constructor<?>> tripleArgumentConstructors = new ConcurrentHashMap<>();

        /**
         * Create a new instance of the entry class using the given parameters.
         * @param paramType1 the class representing the single constructor argument's type
         * @param param1 the object to pass to the constructor as the single constructor argument
         * @return newObjectInstance
         */
        @SuppressWarnings("unchecked")
        public <TParam> TBase newInstance(Class<? extends TParam> paramType1, TParam param1) {
            Constructor<? extends TBase> constructor = (Constructor<? extends TBase>) this.singleArgumentConstructors.get(paramType1);
            if (constructor == null) {
                constructor = getConstructor(paramType1);
                this.singleArgumentConstructors.put(paramType1, constructor);
            }

            try {
                return constructor.newInstance(param1);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to create instance from constructor " + getSignature(paramType1) + ".");
            }
        }

        /**
         * Create a new instance of the entry class using the given parameters.
         * @param paramType1 the class representing the first constructor argument's type
         * @param param1 the object to pass to the constructor as the first argument
         * @param paramType2 the class representing the second constructor argument's type
         * @param param2 the object to pass to the constructor as the second argument
         * @return newObjectInstance
         */
        @SuppressWarnings("unchecked")
        public <TParam1, TParam2> TBase newInstance(Class<? extends TParam1> paramType1, TParam1 param1, Class<? extends TParam2> paramType2, TParam2 param2) {
            Class<?>[] constructorArguments = {paramType1, paramType2};
            Constructor<? extends TBase> constructor = (Constructor<? extends TBase>) this.doubleArgumentConstructors.get(constructorArguments);
            if (constructor == null) {
                constructor = getConstructor(constructorArguments);
                this.doubleArgumentConstructors.put(constructorArguments, constructor);
            }

            try {
                return constructor.newInstance(param1, param2);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to create instance from constructor " + getSignature(constructorArguments) + ".");
            }
        }

        /**
         * Create a new instance of the entry class using the given parameters.
         * @param paramType1 the class representing the first constructor argument's type
         * @param param1 the object to pass to the constructor as the first argument
         * @param paramType2 the class representing the second constructor argument's type
         * @param param2 the object to pass to the constructor as the second argument
         * @param paramType3 the class representing the third constructor argument's type
         * @param param3 the object to pass to the constructor as the third argument
         * @return newObjectInstance
         */
        @SuppressWarnings("unchecked")
        public <TParam1, TParam2, TParam3> TBase newInstance(Class<? extends TParam1> paramType1, TParam1 param1, Class<? extends TParam2> paramType2, TParam2 param2, Class<? extends TParam3> paramType3, TParam3 param3) {
            Class<?>[] constructorArguments = {paramType1, paramType2, paramType3};
            Constructor<? extends TBase> constructor = (Constructor<? extends TBase>) this.tripleArgumentConstructors.get(constructorArguments);
            if (constructor == null) {
                constructor = getConstructor(constructorArguments);
                this.tripleArgumentConstructors.put(constructorArguments, constructor);
            }

            try {
                return constructor.newInstance(param1, param2, param3);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to create instance from constructor " + getSignature(constructorArguments) + ".");
            }
        }
        
        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ClassEntry))
                return false;

            ClassEntry<?> other = (ClassEntry<?>) object;
            return Objects.equals(this.entryClass, other.entryClass) && Objects.equals(this.identifier, other.identifier);
        }

        @Override
        public String toString() {
            return "ClassEntry{identifier='" + this.identifier + "',entryClass='" + this.entryClass.getName() + "'}";
        }

        @SuppressWarnings("unchecked")
        private Constructor<? extends TBase> getConstructor(Class<?>... argumentTypes) {
            try {
                return this.entryClass.getConstructor(argumentTypes);
            } catch (NoSuchMethodException ex) {
                // Failed to find an exact match. Ignore that since we want to find non-exact matches.
            }

            // Go through each constructor to find one which is acceptable.
            Constructor<?>[] constructors = this.entryClass.getConstructors();
            for (int i = 0; i < constructors.length; i++) {
                Constructor<?> testConstructor = constructors[i];
                if (testConstructor.getParameterCount() != argumentTypes.length)
                    continue; // Wrong count, so skip it.

                // Test if the constructor's arguments can accept the arguments passed.
                boolean constructorValid = true;
                Class<?>[] parameterTypes = testConstructor.getParameterTypes();
                for (int j = 0; j < parameterTypes.length; j++) {
                    if (!parameterTypes[j].isAssignableFrom(argumentTypes[j])) {
                        constructorValid = false;
                        break;
                    }
                }

                if (constructorValid)
                    return (Constructor<? extends TBase>) testConstructor;
            }

            throw new RuntimeException("No constructor found matching " + getSignature(argumentTypes) + ".");
        }

        private String getSignature(Class<?>... argumentTypes) {
            StringBuilder builder = new StringBuilder(this.entryClass.getSimpleName()).append("(");
            for (int i = 0; i < argumentTypes.length; i++) {
                if (i > 0)
                    builder.append(", ");
                builder.append(Utils.getSimpleName(argumentTypes[i]));
            }

            return builder.append(')').toString();
        }
    }
}
