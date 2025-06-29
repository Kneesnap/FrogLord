package net.highwayfrogs.editor.scripting;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.data.GameObject.SharedGameObject;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompiler;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompilerException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.builtins.NoodleBuiltinManager;
import net.highwayfrogs.editor.scripting.functions.*;
import net.highwayfrogs.editor.scripting.runtime.NoodleObjectInstance;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleArrayTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleFileTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleWrapperTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.utils.NoodleLoggerTemplate;
import net.highwayfrogs.editor.scripting.runtime.templates.utils.NoodleStringTemplate;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.math.*;
import net.highwayfrogs.editor.utils.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Represents a runtime for running scripts.
 * TODO: A scripting console would be nice. It should have some commands to show documentation too. #help test
 *  -> Use the tokenizer for highlighting maybe?
 *  -> Have documentation functions. (Allow typing help [class|method], to view available options)
 */
public class NoodleScriptEngine extends SharedGameObject {
    private final String name;
    @Getter private final NoodleBuiltinManager builtinManager = new NoodleBuiltinManager();
    private final List<NoodleObjectTemplate<?>> templates = new ArrayList<>();
    private final Map<Class<?>, List<NoodleObjectTemplate<?>>> templatesByClass = new HashMap<>();
    private final Map<String, NoodleObjectTemplate<?>> templatesByName = new HashMap<>();
    private final Map<String, NoodleFunction> functionMap = new HashMap<>(); // <label, function>
    private final Map<String, NoodlePrimitive> constantMap = new HashMap<>(); // <name, constant>
    @Getter private boolean sealed; // No longer ready for changes.
    private final List<File> allowedDirectories = new ArrayList<>();
    private ILogger logger;

    public NoodleScriptEngine(GameInstance instance, String name) {
        super(instance);
        this.name = name;
    }

    @Override
    public ILogger getLogger() {
        if (this.logger == null) {
            if (this.name != null && this.name.trim().length() > 0) {
                this.logger = new LazyInstanceLogger(getGameInstance(), NoodleScriptEngine::getLoggerInfo, this);
            } else {
                return super.getLogger();
            }
        }

        return this.logger;
    }

    /**
     * Gets info about the logger.
     */
    public String getLoggerInfo() {
        return Utils.getSimpleName(this) + "[" + this.name + "]";
    }
    
    /**
     * Sets up the default runtime features.
     */
    private void setupDefaultFeatures() {
        throwIfSealed();
        this.builtinManager.registerPreprocessorBuiltins();
        registerFunctions();
        registerConstants();
        registerDefaultTemplates();
    }

    /**
     * Seals the engine, disabling all setup functions, and enabling runtime functions.
     */
    public void seal() {
       if (this.sealed)
           throw new IllegalStateException("Cannot seal the engine, as it is already sealed.");

       setupDefaultFeatures();
       this.sealed = true;

       // Register the wrapped Java functions.
       for (int i = 0; i < this.templates.size(); i++)
           this.templates.get(i).setupJvm();
    }

    /**
     * Gets a function by its name.
     * @param functionName The name of the function to get.
     * @return function, null if not found.
     */
    public NoodleFunction getGlobalFunctionByName(String functionName) {
        return this.functionMap.get(functionName);
    }

    /**
     * Gets a constant value by its name.
     * @param constantName The name of the constant value to get.
     * @return constantValue, null if not found.
     */
    public NoodlePrimitive getConstantByName(String constantName) {
        return this.constantMap.get(constantName);
    }

    private void throwIfSealed() {
        if (this.sealed)
            throw new UnsupportedOperationException("This operation cannot be performed after the engine is sealed.");
    }

    /**
     * Registers a new noodle function.
     * @param function The function to register.
     */
    public void addGlobalFunction(NoodleFunction function) {
        this.functionMap.put(function.getLabel(), function);
    }

    /**
     * Adds a constant value to noodle scripts.
     * @param constantName  The name of the constant.
     * @param constantValue The constant value.
     */
    public void addConstant(String constantName, double constantValue) {
        this.constantMap.put(constantName, new NoodlePrimitive(constantValue));
    }

    /**
     * Adds a constant value to noodle scripts.
     * @param constantName  The name of the constant.
     * @param constantValue The constant value.
     */
    public void addConstant(String constantName, String constantValue) {
        this.constantMap.put(constantName, new NoodlePrimitive(constantValue));
    }

    /**
     * Gets the object type of the supplied object.
     * @param object The object to get the type from.
     * @return objectType
     */
    @SuppressWarnings("unchecked")
    public <T> NoodleObjectTemplate<? extends T> getTemplateFromObject(T object) {
        if (object == null)
            return null;

        Class<?> tempClass = object.getClass();
        if (tempClass.isPrimitive() || String.class.equals(tempClass))
            return null;

        if (tempClass.isArray())
            return (NoodleObjectTemplate<? extends T>) NoodleArrayTemplate.INSTANCE;

        while (tempClass != null && !Object.class.equals(tempClass)) {
            List<NoodleObjectTemplate<?>> templates = this.templatesByClass.get(tempClass);
            if (templates != null) {
                for (int i = 0; i < templates.size(); i++) {
                    NoodleObjectTemplate<?> template = templates.get(i);
                    if (template.isObjectSupported(object))
                        return (NoodleObjectTemplate<? extends T>) template;
                }
            }

            tempClass = tempClass.getSuperclass();
        }

        return null;
    }

    /**
     * Gets the object template by the name provided.
     * @param name The name to get the type from.
     * @return objectType
     */
    public NoodleObjectTemplate<?> getTemplateByName(String name) {
        return this.templatesByName.get(name);
    }

    /**
     * Registers automatic wrapped templates for each of the provided classes.
     * @param wrappedClasses the classes to wrap & register
     */
    public void addWrapperTemplates(Class<?>... wrappedClasses) {
        for (int i = 0; i < wrappedClasses.length; i++)
            addTemplate(new NoodleWrapperTemplate<>(wrappedClasses[i]));
    }

    /**
     * Registers a template with the scripting engine.
     * @param template the template to register
     */
    public void addTemplate(NoodleObjectTemplate<?> template) {
        throwIfSealed();
        if (template == null)
            throw new NullPointerException("template");

        String templateName = template.getName();
        if (templateName == null)
            throw new NullPointerException("template.getName()");

        Class<?> templateWrappedClass = template.getWrappedClass();
        if (templateWrappedClass == null)
            throw new NullPointerException("template.getWrappedClass()");

        NoodleObjectTemplate<?> oldTemplate = getTemplateByName(templateName);
        if (oldTemplate != null)
            throw new NoodleRuntimeException("A template named '%s' has already been registered! (%s/%s)", templateName, Utils.getSimpleName(template), Utils.getSimpleName(oldTemplate));

        this.templatesByName.put(templateName, template);
        this.templates.add(template);
        this.templatesByClass.computeIfAbsent(templateWrappedClass, key -> new ArrayList<>()).add(template);
        template.setup();
    }

    private void throwIfNotSealed() {
        if (!this.sealed)
            throw new UnsupportedOperationException("This operation cannot be performed until the engine is sealed.");
    }

    /**
     * Loads the script file.
     * @param scriptFile The file to load.
     */
    public NoodleScript loadScriptFile(File scriptFile, boolean whitelistScriptFolder) {
        throwIfNotSealed();
        if (scriptFile == null)
            throw new NullPointerException("sourceCodeFile");

        String scriptName = FileUtils.stripExtension(scriptFile.getName());
        return loadScript(scriptFile, scriptName, new NoodleScript(this, scriptName), whitelistScriptFolder);
    }

    /**
     * Loads the script file.
     * @param scriptFile The file to load.
     * @param maker The script object maker.
     */
    public <T extends NoodleScript> T loadScriptFile(File scriptFile, BiFunction<NoodleScriptEngine, String, T> maker, boolean whitelistScriptFolder) {
        throwIfNotSealed();
        if (scriptFile == null)
            throw new NullPointerException("sourceCodeFile");
        if (maker == null)
            throw new NullPointerException("scriptName");

        String scriptName = FileUtils.stripExtension(scriptFile.getName());
        return loadScript(scriptFile, scriptName, maker.apply(this, scriptName), whitelistScriptFolder);
    }

    /**
     * Loads a script from disk.
     */
    public <T extends NoodleScript> T loadScript(File sourceCodeFile, String scriptName, T script, boolean whitelistScriptFolder) {
        throwIfNotSealed();
        if (sourceCodeFile == null)
            throw new NullPointerException("sourceCodeFile");
        if (scriptName == null)
            throw new NullPointerException("scriptName");
        if (script == null)
            throw new NullPointerException("script");
        if (!sourceCodeFile.exists())
            throw new NoodleCompilerException("Tried to load script `%s`, which could not be found!", sourceCodeFile.getName());

        // Track the file where the source code came from.
        script.setSourceFile(sourceCodeFile);

        // Compile the script.
        try {
            NoodleCompiler.compileScript(this, sourceCodeFile, script);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Failed to compile script: '%s'.", scriptName);
            return null;
        }

        if (whitelistScriptFolder)
            addWhitelistedFolder(script.getScriptFolder());

        return script;
    }

    private void registerFunctions() {
        addGlobalFunction(NDLFunctionGetArgument.INSTANCE);
        addGlobalFunction(NDLFunctionCastToInt.INSTANCE);
        addGlobalFunction(NDLFunctionLogError.INSTANCE);
        addGlobalFunction(NDLFunctionLogInfo.INSTANCE);
        addGlobalFunction(NDLFunctionLogWarning.INSTANCE);
        addGlobalFunction(NDLFunctionPresetPrompt.INSTANCE);
        addGlobalFunction(NDLFunctionMakePopup.INSTANCE);

        // Functions which we also register as macros.
        registerGlobalFunctionAndBuiltinMacro("argumentCount", NDLFunctionGetArgumentCount.INSTANCE);
    }

    private void registerGlobalFunctionAndBuiltinMacro(String macroName, NoodleFunction function) {
        addGlobalFunction(function);
        this.builtinManager.registerSystemMacroBasicFunction(macroName, function);
    }

    private void registerDefaultTemplates() {
        // A bunch of classes which are generally useful, and good to expose to any script.
        // For the interface/abstract classes added here, they are a temporary measure so that isRepresentable() can return true.
        // Consider revisiting them at a later date once the scripting system has been used more.
        addTemplate(NoodleArrayTemplate.INSTANCE);
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(Collection.class));
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(List.class));
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(ArrayList.class));
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(Set.class));
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(HashSet.class));
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(Map.class));
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(HashMap.class));
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(Arrays.class));
        addTemplate(NoodleLoggerTemplate.INSTANCE);
        addTemplate(NoodleStringTemplate.INSTANCE);
        addTemplate(NoodleFileTemplate.INSTANCE);

        // Various utility components of FrogLord.
        addWrapperTemplates(Matrix4x4f.class, Vector2f.class, Vector3f.class, Vector4f.class, Quaternion.class);
        addWrapperTemplates(AudioUtils.class, ColorUtils.class, DataUtils.class, FileUtils.class, MathUtils.class,
                NumberUtils.class, StringUtils.class, TimeUtils.class, Utils.class);
        addTemplate(NoodleWrapperTemplate.getCachedTemplate(Config.class));

        // DataReader/DataWriter is considered not very helpful to Noodle, as its data types are somewhat bizarre.
        // Perhaps we should have a way to create them, and pass them to functions, but not necessarily use them.
    }

    /**
     * Write a list of all accessible functions (global, instance, static, getter, setter, etc) to the console.
     */
    public void exportFunctionsAndObjectDescriptions() {
        getLogger().info("Global Functions:");
        for (NoodleFunction function : this.functionMap.values())
            getLogger().info(" - %s: %s", function.getLabel(), function.getUsage());
        getLogger().info("");

        getLogger().info("Object Types:");
        for (NoodleObjectTemplate<?> template : this.templates) {
            getLogger().info(" - %s:", template.getName());
            template.printFunctionList();
            getLogger().info("");
        }
    }

    /**
     * Returns true iff the given object can be represented in Noodle.
     * @param object The object to test
     * @return true iff the given object can be represented in a Noodle script.
     */
    public boolean isRepresentable(Object object) {
        return object == null || isRepresentable(object.getClass());
    }

    /**
     * Returns true iff the given class can for certain be represented in Noodle.
     * @param testClass The class to test
     * @return true iff the given class can be represented in a Noodle script.
     */
    public boolean isRepresentable(Class<?> testClass) {
        if (testClass == null)
            throw new NullPointerException("testClass");

        if (testClass.isPrimitive())
            return true;

        if (testClass.equals(Void.class) || testClass.equals(Boolean.class) || Number.class.isAssignableFrom(testClass) || String.class.equals(testClass) || testClass.equals(NoodlePrimitive.class) || testClass.equals(NoodleObjectInstance.class))
            return true;

        return this.templatesByClass.get(testClass) != null;
    }

    /**
     * Adds a directory to the whitelisted file paths where a script can modify.
     * @param file the file path to add
     */
    public File addWhitelistedFolder(File file) {
        if (file == null)
            throw new NullPointerException("file");
        if (!file.isDirectory())
            throw new IllegalArgumentException("The file provided was not a directory!");

        File canonicalFile;

        try {
            canonicalFile = file.getCanonicalFile();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot resolve '" + file + "' to a canonical file path.", ex);
        }

        if (!this.allowedDirectories.contains(canonicalFile))
            this.allowedDirectories.add(canonicalFile);

        return canonicalFile;
    }

    /**
     * Test if the provided file is within the allowed directory paths.
     * @param file the file to test
     * @return isWhitelistedFilePath
     */
    public boolean isWhitelistedFilePath(File file) {
        if (file == null)
            throw new NullPointerException("file");

        for (int i = 0; i < this.allowedDirectories.size(); i++) {
            File testDir = this.allowedDirectories.get(i);
            if (FileUtils.isFileWithinParent(file, testDir))
                return true;
        }

        return false;
    }

    private void registerConstants() {
        addConstant("true", 1D);
        addConstant("false", 0D);
        addConstant("null", null);
    }
}