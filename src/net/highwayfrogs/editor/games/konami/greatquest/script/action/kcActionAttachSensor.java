package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric.kcCResourceGenericType;
import net.highwayfrogs.editor.games.konami.greatquest.proxy.kcProxyDesc.kcCollisionGroup;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'ATTACH' and 'ATTACH_SENSOR' types.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionAttachSensor extends kcAction {
    private kcAttachID attachID;
    private final kcParam boneId = new kcParam();
    private final GreatQuestHash<kcCResourceGeneric> particleEmitterRef = new GreatQuestHash<>(); // kcParticleEmitterParam The hashes should be represented as -1 when not present.
    private final GreatQuestHash<kcCResourceGeneric> launchDataRef = new GreatQuestHash<>(); // LauncherParams The hashes should be represented as -1 when not present.
    private float radius;
    private int collideWith;

    private static final kcArgument[] BASE_ARGUMENTS = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId");
    private static final kcArgument[] ATTACH_PARTICLE = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId", kcParamType.HASH, "hEffect");
    private static final kcArgument[] ATTACH_LAUNCHER = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId", kcParamType.HASH, "hLaunchData");
    private static final kcArgument[] ATTACH_ATTACK_OR_BUMP = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId", kcParamType.FLOAT, "radius", kcParamType.UNSIGNED_INT, "collideWith");
    private static final kcArgument[] ATTACH_SENSOR = kcArgument.make(kcParamType.BONE_TAG, "boneId", kcParamType.FLOAT, "radius", kcParamType.UNSIGNED_INT, "collideWith");

    public kcActionAttachSensor(kcActionExecutor executor, kcActionID action) {
        super(executor, action);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        if (arguments == null || arguments.length == 0 || arguments[0] == null)
            return BASE_ARGUMENTS;

        int firstArg = arguments[0].getAsInteger();

        // CCharacter::OnCommand
        if (firstArg == kcAttachID.PARTICLE_EMITTER.ordinal()) {
            return ATTACH_PARTICLE;
        } else if (firstArg == kcAttachID.LAUNCHER.ordinal()) {
            return ATTACH_LAUNCHER;
        } else if (firstArg == kcAttachID.ATTACK_SENSOR.ordinal() || firstArg == kcAttachID.BUMP_SENSOR.ordinal()) {
            return (getActionID() == kcActionID.ATTACH_SENSOR) ? ATTACH_SENSOR : ATTACH_ATTACK_OR_BUMP;
        } else {
            return BASE_ARGUMENTS;
        }
    }

    @Override
    public void load(kcParamReader reader) {
        this.attachID = reader.next().getEnum(kcAttachID.values());
        this.boneId.setValue(reader.next());
        switch (this.attachID) {
            case PARTICLE_EMITTER:
                setParticleEmitterHash(getLogger(), reader.next().getAsInteger());
                break;
            case LAUNCHER:
                setLauncherDataHash(getLogger(), reader.next().getAsInteger());
                break;
            case ATTACK_SENSOR:
            case BUMP_SENSOR:
                this.radius = reader.next().getAsFloat();
                this.collideWith = reader.next().getAsInteger();
                break;
            default:
                throw new RuntimeException("Unsupported kcAttachID: " + this.attachID);
        }
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.attachID.ordinal());
        writer.write(this.boneId);
        switch (this.attachID) {
            case PARTICLE_EMITTER:
                writer.write(this.particleEmitterRef.getHashNumber());
                break;
            case LAUNCHER:
                writer.write(this.launchDataRef.getHashNumber());
                break;
            case ATTACK_SENSOR:
            case BUMP_SENSOR:
                writer.write(this.radius);
                writer.write(this.collideWith);
                break;
            default:
                throw new RuntimeException("Unsupported kcAttachID: " + this.attachID);
        }
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        int argumentCount = super.getGqsArgumentCount(argumentTemplates);
        return (argumentTemplates == ATTACH_ATTACK_OR_BUMP || argumentTemplates == ATTACH_SENSOR) ? argumentCount - 1 : argumentCount;
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        this.attachID = getActionID() == kcActionID.ATTACH_SENSOR ? kcAttachID.ATTACK_SENSOR : arguments.useNext().getAsEnum(kcAttachID.class);
        this.boneId.fromConfigNode(logger, getExecutor(), getGameInstance(), arguments.useNext(), kcParamType.BONE_TAG);
        switch (this.attachID) {
            case PARTICLE_EMITTER:
                resolveResource(logger, arguments.useNext(), kcCResourceGenericType.PARTICLE_EMITTER_PARAM, this.particleEmitterRef);
                break;
            case LAUNCHER:
                resolveResource(logger, arguments.useNext(), kcCResourceGenericType.LAUNCHER_DESCRIPTION, this.launchDataRef);
                break;
            case ATTACK_SENSOR:
            case BUMP_SENSOR:
                this.radius = arguments.useNext().getAsFloat();
                this.collideWith = kcCollisionGroup.getValueFromArguments(arguments);
                break;
            default:
                throw new RuntimeException("Unsupported kcAttachID: " + this.attachID);
        }
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        if (getActionID() != kcActionID.ATTACH_SENSOR)
            arguments.createNext().setAsEnum(this.attachID);
        this.boneId.toConfigNode(getExecutor(), settings, arguments.createNext(), kcParamType.BONE_TAG);
        switch (this.attachID) {
            case PARTICLE_EMITTER:
                this.particleEmitterRef.applyGqsString(arguments.createNext(), settings);
                break;
            case LAUNCHER:
                this.launchDataRef.applyGqsString(arguments.createNext(), settings);
                break;
            case ATTACK_SENSOR:
            case BUMP_SENSOR:
                arguments.createNext().setAsFloat(this.radius);
                kcCollisionGroup.addFlags(this.collideWith, arguments);
                break;
            default:
                throw new RuntimeException("Unsupported kcAttachID: " + this.attachID);
        }
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);

        if (this.attachID == kcAttachID.ATTACK_SENSOR || this.attachID == kcAttachID.BUMP_SENSOR) {
            if (this.radius <= 0 || !Float.isFinite(this.radius))
                printWarning(logger, "the provided radius (" + this.radius + "), and not a positive number");
            if (this.collideWith == 0)
                printWarning(logger, "no collision groups were specified");
        }
    }

    private void setParticleEmitterHash(ILogger logger, int particleEmitterHash) {
        GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceGenericType.PARTICLE_EMITTER_PARAM, getChunkedFile(), this, this.particleEmitterRef, particleEmitterHash, true);
    }

    private void setLauncherDataHash(ILogger logger, int launcherDataHash) {
        GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceGenericType.LAUNCHER_DESCRIPTION, getChunkedFile(), this, this.launchDataRef, launcherDataHash, true);
    }
}