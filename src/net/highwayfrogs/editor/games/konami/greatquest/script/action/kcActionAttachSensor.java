package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.generic.kcCResourceGeneric;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
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
    private int focus;

    private static final kcArgument[] BASE_ARGUMENTS = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId");
    private static final kcArgument[] ATTACH_PARTICLE = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId", kcParamType.HASH, "hEffect");
    private static final kcArgument[] ATTACH_LAUNCHER = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId", kcParamType.HASH, "hLaunchData");
    private static final kcArgument[] ATTACH_ATTACK_OR_BUMP = kcArgument.make(kcParamType.ATTACH_ID, "type", kcParamType.BONE_TAG, "boneId", kcParamType.FLOAT, "radius", kcParamType.UNSIGNED_INT, "focus"); // TODO: Special flag option for 'focus'?
    // TODO: What is focus? It's either 0 or 2, and 0 only is seen twice. This is mCollideWith

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
            return ATTACH_ATTACK_OR_BUMP;
        } else {
            return BASE_ARGUMENTS;
        }
    }

    @Override
    public void load(kcParamReader reader) {
        this.attachID = reader.next().getEnum(kcAttachID.values());
        this.boneId.setValue(reader.next().getAsInteger());
        switch (this.attachID) {
            case PARTICLE_EMITTER:
                setParticleEmitterHash(reader.next().getAsInteger());
                break;
            case LAUNCHER:
                setLauncherDataHash(reader.next().getAsInteger());
                break;
            case ATTACK_SENSOR:
            case BUMP_SENSOR:
                this.radius = reader.next().getAsFloat();
                this.focus = reader.next().getAsInteger();
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
                writer.write(this.focus);
                break;
            default:
                throw new RuntimeException("Unsupported kcAttachID: " + this.attachID);
        }
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.attachID = arguments.useNext().getAsEnum(kcAttachID.class);
        this.boneId.fromConfigNode(getExecutor(), getGameInstance(), arguments.useNext(), kcParamType.BONE_TAG);
        switch (this.attachID) {
            case PARTICLE_EMITTER:
                int emitterHash = GreatQuestUtils.getAsHash(arguments.useNext(), -1);
                setParticleEmitterHash(emitterHash);
                break;
            case LAUNCHER:
                int launcherHash = GreatQuestUtils.getAsHash(arguments.useNext(), -1);
                setLauncherDataHash(launcherHash);
                break;
            case ATTACK_SENSOR:
            case BUMP_SENSOR:
                this.radius = arguments.useNext().getAsFloat();
                this.focus = arguments.useNext().getAsInteger();
                break;
            default:
                throw new RuntimeException("Unsupported kcAttachID: " + this.attachID);
        }
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
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
                arguments.createNext().setAsInteger(this.focus);
                break;
            default:
                throw new RuntimeException("Unsupported kcAttachID: " + this.attachID);
        }
    }

    private void setParticleEmitterHash(int particleEmitterHash) {
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, getChunkedFile(), this, this.particleEmitterRef, particleEmitterHash, true);
    }

    private void setLauncherDataHash(int launcherDataHash) {
        GreatQuestUtils.resolveResourceHash(kcCResourceGeneric.class, getChunkedFile(), this, this.launchDataRef, launcherDataHash, true);
    }
}