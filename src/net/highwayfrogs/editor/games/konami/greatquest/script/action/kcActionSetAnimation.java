package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceAnimSet;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

/**
 * Represents the "SET_ANIMATION" kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionSetAnimation extends kcAction {
    private final GreatQuestHash<kcCResourceTrack> trackRef = new GreatQuestHash<>();
    private final kcParam startTime = new kcParam();
    private final kcParam translationTick = new kcParam();
    private kcAnimationMode mode = kcAnimationMode.NO_REPEAT;
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.HASH, "track", kcParamType.ANIMATION_TICK, "startTime", kcParamType.TIMESTAMP_TICK, "transitionTime", kcParamType.INT, "mode");

    private static final String ARGUMENT_START_TIME = "StartTime";
    public static final int DEFAULT_START_TIME = 0x80000000;

    public kcActionSetAnimation(kcActionExecutor executor) {
        super(executor, kcActionID.SET_ANIMATION);
        this.trackRef.setNullRepresentedAsZero();
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        setTrackHash(reader.next().getAsInteger());
        this.startTime.setValue(reader.next());
        this.translationTick.setValue(reader.next());
        this.mode = kcAnimationMode.getType(reader.next().getAsInteger(), false);
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.trackRef.getHashNumber());
        writer.write(this.startTime);
        writer.write(this.translationTick);
        writer.write(this.mode.getValue());
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return 2;
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        StringNode trackNode = arguments.useNext();
        StringNode timestampTick = arguments.useNextIfPresent();

        resolveResource(trackNode, kcCResourceTrack.class, this.trackRef); // Load 'track' parameter.
        if (timestampTick != null) {
            this.translationTick.fromConfigNode(getExecutor(), getGameInstance(), timestampTick, kcParamType.TIMESTAMP_TICK); // Load 'transitionTime' parameter.
        } else {
            this.translationTick.setValue(0);
        }

        // Load 'startTime' parameter.
        StringNode startTime = arguments.use(ARGUMENT_START_TIME);
        if (startTime != null) {
            this.startTime.setValue(Math.round(startTime.getAsFloat() * GreatQuestModelMesh.TICKS_PER_SECOND));
        } else {
            this.startTime.setValue(DEFAULT_START_TIME);
        }

        // Determine the mode based on the flags.
        kcAnimationMode mode = kcAnimationMode.NO_REPEAT;
        for (int i = 0; i < kcAnimationMode.values().length; i++) {
            kcAnimationMode animationMode = kcAnimationMode.values()[i];
            if (animationMode.getFlagName() != null && arguments.useFlag(animationMode.getFlagName())) {
               mode = animationMode;
               break;
            }
        }

        this.mode = kcAnimationMode.getType(mode.getValue(), false);
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.trackRef.applyGqsString(arguments.createNext(), settings); // Save 'track' parameter.
        this.translationTick.toConfigNode(getExecutor(), settings, arguments.createNext(), kcParamType.TIMESTAMP_TICK); // Save 'transitionTime' parameter.

        if (this.mode != null && this.mode.getFlagName() != null)
            arguments.getOrCreate(this.mode.getFlagName());

        // Apply 'startTime' parameter.
        if (this.startTime.getAsInteger() != DEFAULT_START_TIME)
            arguments.getOrCreate(ARGUMENT_START_TIME).setAsFloat(getStartTime());
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        kcCResourceTrack animation = this.trackRef.getResource();
        if (animation == null && this.trackRef.getHashNumber() != 0)
            printWarning(logger, "'" + this.trackRef.getAsGqsString(null) + "' is not a valid animation.");

        kcActorBaseDesc actorDesc = getExecutor() != null ? getExecutor().getExecutingActorBaseDescription() : null;
        kcCResourceAnimSet animSet = actorDesc != null ? actorDesc.getAnimationSet() : null;
        if (animation != null && animSet != null && !animSet.contains(animation))
            printWarning(logger, "the animation '" + animation.getName() + "' was not found in '" + animSet.getName() + "', the animation set configured for '" + actorDesc.getResource().getName() + "'.");
    }

    /**
     * Gets the start time (in seconds)
     * @return startTime
     */
    public float getStartTime() {
        if (this.startTime.getAsInteger() == DEFAULT_START_TIME) {
            return 0F;
        } else {
            return (float) this.startTime.getAsInteger() / GreatQuestModelMesh.TICKS_PER_SECOND;
        }
    }

    /**
     * Sets the hash corresponding to the animation track to play
     * @param trackHash the hash of the track to apply
     */
    public void setTrackHash(int trackHash) {
        GreatQuestUtils.resolveLevelResourceHash(kcCResourceTrack.class, getChunkedFile(), this, this.trackRef, trackHash, false);
    }

    @Getter
    @RequiredArgsConstructor
    public enum kcAnimationMode {
        NO_REPEAT(null, 0),
        REPEAT("Repeat", 2), // This is often applied to the last animation command in a sequence.
        FIRST_IN_SEQUENCE("FirstAnimationInSequence", 65536), // If this is the first animation in a sequence, AND there are more sequences later, set this flag.
        UNKNOWN_MODE("UnknownMode", 36); // Seen in an unused sequence in Dr. Starkenstein's castle. I assume this is probably some kind of unused feature. It's unclear if it was ever implemented.

        private final String flagName;
        private final int value;

        /**
         * Gets the kcAnimationMode corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return kcAnimationMode
         */
        public static kcAnimationMode getType(int value, boolean allowNull) {
            for (int i = 0; i < values().length; i++) {
                kcAnimationMode temp = values()[i];
                if (temp.getValue() == value)
                    return temp;
            }

            if (!allowNull)
                throw new RuntimeException("Couldn't determine the kcAnimationMode from value " + value + ".");

            return null;
        }
    }
}