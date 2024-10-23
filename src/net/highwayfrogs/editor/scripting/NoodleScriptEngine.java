package net.highwayfrogs.editor.scripting;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject.SharedGameObject;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompiler;
import net.highwayfrogs.editor.scripting.compiler.NoodleCompilerException;
import net.highwayfrogs.editor.scripting.compiler.preprocessor.builtins.NoodleBuiltinManager;
import net.highwayfrogs.editor.scripting.functions.*;
import net.highwayfrogs.editor.scripting.runtime.NoodlePrimitive;
import net.highwayfrogs.editor.scripting.runtime.NoodleRuntimeException;
import net.highwayfrogs.editor.scripting.runtime.templates.NoodleObjectTemplate;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

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
    private final Map<String, NoodleFunction> functionMap = new HashMap<>(); // <label, function>
    private final Map<String, NoodlePrimitive> constantMap = new HashMap<>(); // <name, constant>
    private Logger logger;

    public NoodleScriptEngine(GameInstance instance, String name) {
        super(instance);
        this.name = name;
    }

    @Override
    public Logger getLogger() {
        if (this.logger == null)
            this.logger = this.name != null && this.name.trim().length() > 0 ? Logger.getLogger("NoodleScriptEngine[" + this.name + "]") : super.getLogger();

        return this.logger;
    }
    
    /**
     * Sets up the default runtime features.
     */
    public void setupDefaultFeatures() {
        this.builtinManager.registerPreprocessorBuiltins();
        registerFunctions();
        registerConstants();
    }

    /**
     * Gets a function by its name.
     * @param functionName The name of the function to get.
     * @return function, null if not found.
     */
    public NoodleFunction getGlobalFunctionByName(String functionName) {
        return functionMap.get(functionName);
    }

    /**
     * Gets a constant value by its name.
     * @param constantName The name of the constant value to get.
     * @return constantValue, null if not found.
     */
    public NoodlePrimitive getConstantByName(String constantName) {
        return constantMap.get(constantName);
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

        for (int i = 0; i < this.templates.size(); i++) {
            NoodleObjectTemplate<?> template = this.templates.get(i);
            if (template.isObjectSupported(object))
                return (NoodleObjectTemplate<? extends T>) template;
        }

        return null;
    }

    /**
     * Gets the object template by the name provided.
     * @param name The name to get the type from.
     * @return objectType
     */
    public NoodleObjectTemplate<?> getTemplateByName(String name) {
        for (int i = this.templates.size() - 1; i >= 0; i--) {
            NoodleObjectTemplate<?> template = this.templates.get(i);
            if (template.getName().equals(name))
                return template;
        }

        return null;
    }

    /**
     * Gets a noodle template of the given type.
     * @param templateClass The type of object the template represents.
     * @return noodleTemplate
     * @param <TType> templateType
     */
    @SuppressWarnings("unchecked")
    public <TType> NoodleObjectTemplate<TType> getTypedTemplate(Class<? extends TType> templateClass) {
        for (int i = this.templates.size() - 1; i >= 0; i--) {
            NoodleObjectTemplate<?> template = this.templates.get(i);
            if (templateClass.equals(template.getWrappedClass()) || (template.getWrappedClass().isAssignableFrom(templateClass) && !templateClass.isAssignableFrom(template.getWrappedClass())))
                return (NoodleObjectTemplate<TType>) template;
        }

        return null;
    }

    /**
     * Registers a template with the scripting engine.
     * @param template the template to register
     */
    public void addTemplate(NoodleObjectTemplate<?> template) {
        if (template == null)
            throw new NullPointerException("template");

        NoodleObjectTemplate<?> oldTemplate = getTemplateByName(template.getName());
        if (oldTemplate != null)
            throw new NoodleRuntimeException("A template named '%s' has already been registered! (%s/%s)", template.getName(), Utils.getSimpleName(template), Utils.getSimpleName(oldTemplate));

        this.templates.add(template);
    }

    /**
     * Loads the script file.
     * @param scriptFile The file to load.
     * @param maker The script object maker.
     */
    public <T extends NoodleScript> T loadScriptFile(File scriptFile, BiFunction<NoodleScriptEngine, String, T> maker) {
        if (scriptFile == null)
            throw new NullPointerException("sourceCodeFile");
        if (maker == null)
            throw new NullPointerException("scriptName");

        String scriptName = Utils.stripExtension(scriptFile.getName());
        return loadScript(scriptFile, scriptName, maker.apply(this, scriptName));
    }

    /**
     * Loads a script from disk.
     */
    public <T extends NoodleScript> T loadScript(File sourceCodeFile, String scriptName, T script) {
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
        
        return script;
    }

    private void registerFunctions() {
        addGlobalFunction(new NDLFunctionGetArgument());
        addGlobalFunction(new NDLFunctionCastToInt());
        addGlobalFunction(new NDLFunctionLogError());
        addGlobalFunction(new NDLFunctionLogInfo());
        addGlobalFunction(new NDLFunctionLogWarning());
        addGlobalFunction(new NDLFunctionPresetPrompt());
        addGlobalFunction(NDLFunctionMakePopup.INSTANCE);

        // Add 'getArgumentCount()' and map it to the 'argumentCount' macro.
        NDLFunctionGetArgumentCount getArgumentCount = new NDLFunctionGetArgumentCount();
        addGlobalFunction(getArgumentCount);
        this.builtinManager.registerSystemMacroBasicFunction("argumentCount", getArgumentCount);
    }

    /**
     * Write a list of all accessible functions (global, instance, static, getter, setter, etc) to the console.
     */
    public void exportFunctionsAndObjectDescriptions() {
        getLogger().info("Global Functions:");
        for (NoodleFunction function : this.functionMap.values())
            getLogger().info(" - " + function.getLabel() + ": " + function.getUsage());
        getLogger().info("");

        getLogger().info("Object Types:");
        for (NoodleObjectTemplate<?> template : this.templates) {
            getLogger().info(" - " + template.getName() + ":");
            template.printFunctionList();
            getLogger().info("");
        }
    }

    private void registerConstants() {
        addConstant("true", 1D);
        addConstant("false", 0D);
        addConstant("null", null);
    }
}