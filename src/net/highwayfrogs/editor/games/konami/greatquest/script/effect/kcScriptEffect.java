package net.highwayfrogs.editor.games.konami.greatquest.script.effect;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceEntityInst;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcEntity3DDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectCamera.kcCameraEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.effect.kcScriptEffectEntity.kcEntityEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcInterimScriptEffect;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript.kcScriptFunction;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptEffectType;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

/**
 * Represents a script effect.
 * Created by Kneesnap on 8/23/2023.
 */
@Getter
public abstract class kcScriptEffect extends GameObject<GreatQuestInstance> implements kcActionExecutor {
    private final kcScriptFunction parentFunction;
    private final kcScriptEffectType effectType;
    private final GreatQuestHash<kcCResourceEntityInst> targetEntityRef;

    public static final String ARGUMENT_ENTITY_RUNNER = "AsEntity";
    public static final String ARGUMENT_EXTERNAL_ENTITY = "ExternalEntity";

    public kcScriptEffect(@NonNull kcScriptFunction parentFunction, kcScriptEffectType effectType) {
        super(parentFunction.getGameInstance());
        this.parentFunction = parentFunction;
        this.effectType = effectType;

        this.targetEntityRef = new GreatQuestHash<>();
        this.targetEntityRef.setNullRepresentedAsZero();
        this.targetEntityRef.setResource(parentFunction.getScript().getEntity(), false);
    }

    /**
     * Gets the target entity, resolving it if the current instance is out of date.
     * @return targetEntity
     */
    public kcCResourceEntityInst getTargetEntity(boolean resolveIfMissing) {
        kcCResourceEntityInst entity = this.targetEntityRef.getResource();
        if (entity != null)
            return entity;

        if (resolveIfMissing) {
            GreatQuestChunkedFile chunkedFile = this.parentFunction.getChunkedFile();
            if (GreatQuestUtils.resolveLevelResourceHash(chunkedFile.getLogger(), kcCResourceEntityInst.class, chunkedFile, this, this.targetEntityRef, this.targetEntityRef.getHashNumber(), false))
                return this.targetEntityRef.getResource();
        }

        return null;
    }

    /**
     * Gets the chunked file which contains the script tree containing this script effect.
     */
    public GreatQuestChunkedFile getChunkedFile() {
        return this.parentFunction != null ? this.parentFunction.getChunkedFile() : null;
    }

    @Override
    public String getName() {
        kcCResourceEntityInst entityInst = getTargetEntity(false);
        if (entityInst == null)
            entityInst = this.parentFunction.getScript().getEntity();

        return entityInst != null ? entityInst.getName() : null;
    }

    /**
     * Gets the effect ID that identifies the behavior which runs with this effect.
     */
    public abstract int getEffectID();

    /**
     * Gets the command name used to uniquely identify the effect action.
     */
    public abstract String getEffectCommandName();

    /**
     * Returns true if the action is applicable to the target entity.
     */
    public abstract boolean isActionApplicableToTarget();

    /**
     * Writes kcParam values to the output list.
     * @param reader The source of kcParam values.
     */
    public abstract void load(kcParamReader reader);

    /**
     * Writes kcParam values to the output list.
     * @param writer The destination to write kcParam values to.
     */
    public abstract void save(kcParamWriter writer);

    /**
     * Loads the action arguments from the arguments provided.
     * @param logger the logger to write information and warnings to
     * @param arguments the arguments to load from
     */
    protected abstract void loadArguments(ILogger logger, OptionalArguments arguments, int lineNumber, String fileName);

    /**
     * Save the arguments of the action to the object.
     * @param logger the logger to write information to
     * @param arguments The object to store the action arguments within
     * @param settings  settings to use to save the arguments as strings
     */
    protected abstract void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings);

    /**
     * Gets the comment (if any) which should be included at the end of the effect line.
     * @return eolComment
     */
    public String getEndOfLineComment() {
        GreatQuestChunkedFile chunkedFile = getChunkedFile();
        if (getTargetEntity(false) == null && !(chunkedFile.getResourceByHash(this.targetEntityRef.getHashNumber()) instanceof kcCResourceEntityInst))
            return "The target entity was not found.";

        return null;
    }

    /**
     * Resolves the target entity hash.
     * @param hash the hash of the target entity
     */
    public boolean setTargetEntityHash(ILogger logger, int hash) {
        GreatQuestChunkedFile chunkedFile = getParentFunction().getScript().getScriptList().getParentFile();
        return GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceEntityInst.class, chunkedFile, this, this.targetEntityRef, hash, false);
    }

    /**
     * Loads the action arguments from the arguments provided.
     * @param arguments the arguments to load from
     */
    public final void loadEffect(ILogger logger, OptionalArguments arguments, int lineNumber, String fileName) {
        // Apply the target entity override before loading the arguments to ensure that the action can access the entity while loading. (Happens for kcActionSetSequence, and anything else which wants to get the actor desc)
        StringNode overrideTargetEntity = arguments.use(ARGUMENT_ENTITY_RUNNER);

        // Restore the default.
        boolean resolvedOverrideEntity = false;
        kcCResourceEntityInst scriptOwner = getParentFunction().getScript().getEntity();
        this.targetEntityRef.setResource(scriptOwner, false);
        if (overrideTargetEntity != null && GreatQuestUtils.resolveLevelResource(logger, overrideTargetEntity, kcCResourceEntityInst.class, getChunkedFile(), this, this.targetEntityRef, false))
            resolvedOverrideEntity = true;

        loadArguments(logger, arguments, lineNumber, fileName);

        // Warn about target entity.
        if (resolvedOverrideEntity && scriptOwner == this.targetEntityRef.getResource()) {
            kcScriptDisplaySettings settings = getChunkedFile() != null ? getChunkedFile().createScriptDisplaySettings() : null;
            OptionalArguments savedEffect = saveEffect(settings);
            kcScriptDisplaySettings.applyGqsSyntaxHashDisplay(savedEffect.getOrCreate(ARGUMENT_ENTITY_RUNNER), settings, this.targetEntityRef);
            logger.warning("The effect '%s' should not include --%s because it already runs as that entity.", savedEffect, ARGUMENT_ENTITY_RUNNER);
        }

        // Print warnings.
        printLoadWarnings(arguments, logger);
    }

    /**
     * Print warnings on the load of the effect.
     * @param arguments the arguments after the load occurred.
     */
    protected void printLoadWarnings(OptionalArguments arguments, ILogger logger) {
        kcCResourceEntityInst targetEntity = getTargetEntity(false);
        boolean externalEntityTarget = arguments.useFlag(ARGUMENT_EXTERNAL_ENTITY);
        arguments.warnAboutUnusedArguments(logger);
        if (targetEntity == null) {
            kcScriptDisplaySettings settings = getChunkedFile() != null ? getChunkedFile().createScriptDisplaySettings() : null;
            if (!externalEntityTarget)
                logger.warning("The effect '%s' targets an entity which was not found.", saveEffect(settings));
        } else if (targetEntity.getInstance() == null || targetEntity.getInstance().getDescription() == null) {
            kcScriptDisplaySettings settings = getChunkedFile() != null ? getChunkedFile().createScriptDisplaySettings() : null;
            logger.warning("The effect '%s' targets an entity (%s) who did not have an entity description!", saveEffect(settings), this.targetEntityRef.getAsString());
        } else if (!isActionApplicableToTarget()) {
            kcScriptDisplaySettings settings = getChunkedFile() != null ? getChunkedFile().createScriptDisplaySettings() : null;
            logger.warning("The effect '%s' targets an entity (%s) which is unable to execute the effect.", saveEffect(settings), this.targetEntityRef.getAsGqsString(settings));
        }
    }

    /**
     * Save the effect data to optional arguments.
     * @param settings settings to use to save the arguments as strings
     */
    public final OptionalArguments saveEffect(kcScriptDisplaySettings settings) {
        OptionalArguments optionalArguments = new OptionalArguments();
        this.saveEffect(settings.getLogger(), optionalArguments, settings);
        return optionalArguments;
    }

    /**
     * Save the effect data to optional arguments.
     * @param arguments The object to store the effect statement within
     * @param settings settings to use to save the arguments as strings
     */
    public final void saveEffect(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        if (logger == null)
            throw new NullPointerException("logger");
        if (arguments == null)
            throw new NullPointerException("arguments");

        arguments.clear();
        arguments.createNext().setAsString(getEffectCommandName(), false);
        saveArguments(logger, arguments, settings);

        // Include the target entity hash if it's not implied.
        if (this.targetEntityRef.getResource() == null || getParentFunction().getScript().getEntity() != this.targetEntityRef.getResource())
            kcScriptDisplaySettings.applyGqsSyntaxHashDisplay(arguments.getOrCreate(ARGUMENT_ENTITY_RUNNER), settings, this.targetEntityRef);
    }

    /**
     * Converts this effect to an interim script effect.
     */
    public kcInterimScriptEffect toInterimScriptEffect() {
        kcInterimScriptEffect scriptEffect = new kcInterimScriptEffect(getChunkedFile());
        scriptEffect.load(this);
        return scriptEffect;
    }

    @Override
    public kcEntity3DDesc getExecutingEntityDescription() {
        kcCResourceEntityInst resEntity = this.targetEntityRef.getResource();
        return (resEntity != null && resEntity.getInstance() != null) ? resEntity.getInstance().getDescription() : null;
    }

    /**
     * Writes the script effect to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings for displaying the output.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("[Target: ");
        builder.append(this.targetEntityRef.getDisplayString(false));
        builder.append("] ");
    }

    /**
     * Attempts to parse a script effect from a line of text in the FrogLord TGQ script syntax.
     * Throws an exception if it cannot be parsed.
     * @param line The line of text to parse
     * @return the parsed script effect
     */
    public static kcScriptEffect parseScriptEffect(ILogger logger, kcScriptFunction function, String line, int lineNumber, String fileName) {
        if (function == null)
            throw new NullPointerException("function");
        if (line == null)
            throw new NullPointerException("line");
        if (line.trim().isEmpty())
            throw new RuntimeException("Cannot parse '" + line + "' as a script effect.");

        try {
            OptionalArguments arguments = OptionalArguments.parse(line);
            String commandName = arguments.useNext().getAsString();
            kcScriptEffect newEffect = createEffectForCommandName(function, commandName);
            if (newEffect == null)
                throw new RuntimeException("The command name '" + commandName + "' is incorrect, make sure it has been spelled correctly.");

            newEffect.loadEffect(logger, arguments, lineNumber, fileName);
            return newEffect;
        } catch (Throwable th) {
            throw new RuntimeException("Failed to parse '" + line + "' in '" + fileName + "' on line " + lineNumber + " as a script effect.", th);
        }
    }

    /**
     * Create a new kcScriptEffect based on the given commandName.
     * If no kcScriptEffect can be created by the given commandName, null will be returned.
     * @param function The script function which the effect is created for
     * @param commandName the name identifying the script effect to create
     * @return newEffect, or null
     */
    public static kcScriptEffect createEffectForCommandName(kcScriptFunction function, String commandName) {
        if (function == null)
            throw new NullPointerException("function");
        if (commandName == null)
            throw new NullPointerException("commandName");

        // Resolve AI effect.
        if (kcScriptEffectAI.EFFECT_COMMAND.equals(commandName))
            return new kcScriptEffectAI(function);

        // Resolve kcActionEffect.
        kcActionID actionID = kcActionID.getActionByCommandName(commandName);
        if (actionID != null)
            return new kcScriptEffectActor(function, actionID);

        // Resolve as camera effect.
        kcCameraEffect cameraEffect = kcCameraEffect.getType(commandName);
        if (cameraEffect != null)
            return new kcScriptEffectCamera(function, cameraEffect);

        // Resolve as entity effect.
        kcEntityEffect entityEffect = kcEntityEffect.getType(commandName);
        if (entityEffect != null) {
            return new kcScriptEffectEntity(function, entityEffect);
        } else {
            return null;
        }
    }
}