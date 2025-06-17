package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTrack;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcAction;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionLazyTemplate;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionSetAnimation;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionSetAnimation.kcAnimationMode;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcCActionSequence;

/**
 * Manages playback of an action sequence.
 * Created by Kneesnap on 11/8/2024.
 */
public class GreatQuestActionSequencePlayback {
    @Getter @NonNull private final GreatQuestModelMesh modelMesh;
    @Getter private kcCActionSequence activeSequence;
    @Getter private SequenceStatus status = SequenceStatus.FINISHED;
    private int actionIndex;
    private int loopsLeft;
    private boolean hasLoopBeenApplied;

    public GreatQuestActionSequencePlayback(@NonNull GreatQuestModelMesh modelMesh) {
        this.modelMesh = modelMesh;
    }

    /**
     * Restarts the sequence from the start.
     */
    public void restart() {
        if (this.activeSequence != null)
            setSequence(this.activeSequence);
    }

    /**
     * Sets the sequence currently played, stopping any existing one, ticking it as well.
     * @param newSequence the sequence to set as active.
     */
    public void setSequenceAndTick(kcCActionSequence newSequence) {
        kcCActionSequence oldSequence = this.activeSequence;
        setSequence(newSequence);
        if (oldSequence != null && newSequence == null)
            this.modelMesh.setActiveAnimation(null);

        tick();
        this.modelMesh.tickAnimation(0);
    }

    /**
     * Sets the sequence currently played, stopping any existing one.
     * @param newSequence the sequence to set as active.
     */
    public void setSequence(kcCActionSequence newSequence) {
        this.activeSequence = newSequence;
        this.status = SequenceStatus.CONTINUE;
        this.actionIndex = 0;
        this.loopsLeft = 0;
        this.hasLoopBeenApplied = false;
    }

    /**
     * Ticks the sequence.
     */
    public void tick() {
        if (this.status == SequenceStatus.FINISHED)
            return;

        if (this.status == SequenceStatus.WAIT_FOR_ANIMATION && !this.modelMesh.isPlayingAnimation())
            this.status = SequenceStatus.CONTINUE;

        while (this.status == SequenceStatus.CONTINUE)
            processNextAction();
    }

    /**
     * Processes the next sequence action.
     * Returns true if the next action should be processed immediately.
     */
    private void processNextAction() {
        if (this.activeSequence == null) {
            this.status = SequenceStatus.FINISHED;
            return;
        }

        // Reached end of script?
        if (this.actionIndex >= this.activeSequence.getActions().size()) {
            if (this.loopsLeft-- > 0) {
                this.actionIndex = 0;
                this.status = SequenceStatus.CONTINUE;
                return;
            }

            this.status = SequenceStatus.FINISHED;
            return;
        }

        kcAction action = this.activeSequence.getActions().get(this.actionIndex++);
        switch (action.getActionID()) {
            case SET_ANIMATION: // 3249
                kcCResourceTrack animation = ((kcActionSetAnimation) action).getTrackRef().getResource();
                boolean repeat = ((kcActionSetAnimation) action).getMode() == kcAnimationMode.REPEAT;
                float startTime = ((kcActionSetAnimation) action).getStartTime();
                this.modelMesh.setActiveAnimation(animation, repeat);
                this.modelMesh.setAnimationTick(startTime * GreatQuestModelMesh.TICKS_PER_SECOND);
                break;
            case WAIT_ANIMATION: // 559
                this.status = SequenceStatus.WAIT_FOR_ANIMATION;
                break;
            case LOOP: // 3
                if (!this.hasLoopBeenApplied) {
                    this.loopsLeft = ((kcActionLazyTemplate) action).getArguments().get(0).getAsInteger();
                    this.hasLoopBeenApplied = true;
                }

                break;
            default:
                if (action.getActionID().isEnableForActionSequences()) {
                    // These actions are valid for action sequences, but may not have been implemented in FrogLord, likely due to the base game not using them.
                    this.activeSequence.getLogger().warning("Wanted to run '%s', but FrogLord does not support this action!", action.getAsGqsStatement());
                } else {
                    this.activeSequence.getLogger().warning("Wanted to run '%s', but that action is not supported in an action sequence!", action.getAsGqsStatement());
                }

                break;
        }
    }

    public enum SequenceStatus {
        CONTINUE,
        WAIT_FOR_ANIMATION,
        FINISHED
    }
}
