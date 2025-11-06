package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestHash;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceAnimSet;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcActorBaseDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;
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
    private int modeFlags;
    private static final kcArgument[] SEQUENCE_ARGUMENTS = kcArgument.make(kcParamType.HASH, "track", kcParamType.ANIMATION_TICK, "startTime", kcParamType.TIMESTAMP_TICK, "transitionTime", kcParamType.INT, "mode");
    private static final kcArgument[] SCRIPT_ARGUMENTS = kcArgument.make(kcParamType.HASH, "track", kcParamType.INT, "mode", kcParamType.ANIMATION_TICK, "startTime", kcParamType.TIMESTAMP_TICK, "transitionTime");

    private static final String ARGUMENT_START_TIME = "StartTime";
    public static final int DEFAULT_START_TIME = 0x80000000;

    public kcActionSetAnimation(kcActionExecutor executor) {
        super(executor, kcActionID.SET_ANIMATION);
        this.trackRef.setNullRepresentedAsZero();
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        if (isPartOfActionSequence()) {
            return SEQUENCE_ARGUMENTS;
        } else {
            return SCRIPT_ARGUMENTS;
        }
    }

    @Override
    public void load(kcParamReader reader) {
        setTrackHash(getLogger(), reader.next().getAsInteger());
        if (isPartOfActionSequence()) {
            this.startTime.setValue(reader.next());
            this.translationTick.setValue(reader.next());
            this.modeFlags = reader.next().getAsInteger();
        } else {
            this.modeFlags = reader.next().getAsInteger();
            this.startTime.setValue(reader.next());
            this.translationTick.setValue(reader.next());
        }

        warnAboutInvalidBitFlags(this.modeFlags, kcAnimationModeFlag.FLAG_VALIDATION_MASK, getAsGqsStatement());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.trackRef.getHashNumber());
        if (isPartOfActionSequence()) {
            writer.write(this.startTime);
            writer.write(this.translationTick);
            writer.write(this.modeFlags);
        } else {
            writer.write(this.modeFlags);
            writer.write(this.startTime);
            writer.write(this.translationTick);
        }
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return 2;
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        StringNode trackNode = arguments.useNext();
        StringNode timestampTick = arguments.useNextIfPresent();

        resolveResource(logger, trackNode, kcCResourceTrack.class, this.trackRef); // Load 'track' parameter.
        if (timestampTick != null) {
            this.translationTick.fromConfigNode(logger, getExecutor(), getGameInstance(), timestampTick, kcParamType.TIMESTAMP_TICK); // Load 'transitionTime' parameter.
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
        this.modeFlags = kcAnimationModeFlag.getValueFromArguments(arguments);
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        this.trackRef.applyGqsString(arguments.createNext(), settings); // Save 'track' parameter.
        this.translationTick.toConfigNode(getExecutor(), settings, arguments.createNext(), kcParamType.TIMESTAMP_TICK); // Save 'transitionTime' parameter.

        // Apply 'startTime' parameter.
        if (this.startTime.getAsInteger() != DEFAULT_START_TIME)
            arguments.getOrCreate(ARGUMENT_START_TIME).setAsFloat(getStartTime());

        // Apply mode flags.
        kcAnimationModeFlag.addFlags(this.modeFlags, arguments);
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
     * Tests if this action is part of an action sequence.
     */
    public boolean isPartOfActionSequence() {
        return getExecutor() instanceof kcCActionSequence;
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
    public void setTrackHash(ILogger logger, int trackHash) {
        GreatQuestUtils.resolveLevelResourceHash(logger, kcCResourceTrack.class, getChunkedFile(), this, this.trackRef, trackHash, false);
    }

    /**
     * Test if the given flag is set.
     * @param flag the flag to test
     * @return true iff the flag is set
     */
    public boolean hasFlag(kcAnimationModeFlag flag) {
        return (this.modeFlags & flag.getValue()) == flag.getValue();
    }

    @Getter
    @RequiredArgsConstructor
    public enum kcAnimationModeFlag {
        BROKEN("Broken", Constants.BIT_FLAG_0), // This breaks rendering of animated entities.
        REPEAT("Repeat", Constants.BIT_FLAG_1), // 2, This is often applied to the last animation command in a sequence.
        REVERSE("Reverse", Constants.BIT_FLAG_16), // 65536, See kcCAnimCtl::SetMode. Seems to cut off all bits higher than this one too.
        REVERSE_ON_COMPLETE("ReverseOnComplete", 36); // Seen in an unused sequence in Dr. Starkenstein's castle. It seems like the '4' part is what causes this, I can't confirm that 32 does anything. But for consistency with the original, I'll keep it as 36.

        private final String flagName;
        private final int value;

        private static final int FLAG_VALIDATION_MASK;
        static {
            int mask = 0;
            for (kcAnimationModeFlag flag : values())
                mask |= flag.getValue();

            FLAG_VALIDATION_MASK = mask;
        }

        /**
         * Add flags to the arguments for the corresponding entity flags.
         * @param value The value to determine which flags to apply from
         * @param arguments The arguments to add the flags to
         */
        public static void addFlags(int value, OptionalArguments arguments) {
            // Write flags.
            for (int i = 0; i < values().length; i++) {
                kcAnimationModeFlag flag = values()[i];
                if ((value & flag.getValue()) == flag.getValue()) {
                    arguments.getOrCreate(flag.getFlagName());
                    value &= ~flag.getValue();
                }
            }

            if (value != 0)
                Utils.getLogger().warning("kcAnimationModeFlag.addFlags() skipped some bits! " + NumberUtils.toHexString(value));
        }

        /**
         * Consume optional flag arguments to build a value containing the same flags as specified by the arguments.
         * @param arguments The arguments to create the value from.
         * @return flagArguments
         */
        public static int getValueFromArguments(OptionalArguments arguments) {
            int value = 0;
            for (int i = 0; i < values().length; i++) {
                kcAnimationModeFlag flag = values()[i];
                if (arguments.useFlag(flag.getFlagName()))
                    value |= flag.getValue();
            }

            return value;
        }
    }
}