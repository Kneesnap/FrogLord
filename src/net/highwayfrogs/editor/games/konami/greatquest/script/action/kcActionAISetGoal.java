package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'AI_SETGOAL' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionAISetGoal extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.AI_GOAL_TYPE, "goal");
    private kcAISystemGoalType goalType;

    public kcActionAISetGoal(kcActionExecutor executor) {
        super(executor, kcActionID.AI_SETGOAL);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.goalType = kcAISystemGoalType.getType(reader.next().getAsInteger(), false);
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.goalType.ordinal());
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        this.goalType = arguments.useNext().getAsEnumOrError(kcAISystemGoalType.class);
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.goalType);
    }

    public enum kcAISystemGoalType {
        // A goal is selected via MonsterClass::Calculate_Goal every 60 frames.
        // Each of the kcAISystemGoalType types has a value calculated, a "GOAL VALUE".
        // The goal with the highest value is always selected to be the current goal.
        // CanHearTarget is a bit flag, resulting in either 0 or 1.
        // CanSeeTarget is a bit flag, resulting in either 0 or 1.
        SCRIPT, // Can never be selected, always has a value of 0.
        STATIC, // Can never be selected, always has a value of 0.
        FIND, // (CharacterParams.attackGoalPercent * ((CanHearTarget + CanSeeTarget) * 10)). Implemented as MonsterClass::Do_Find(), Attempts to run towards the target, and attack if possible. Will attempt to wander sometimes if it gets stuck.
        FLEE, // (Attack Aggression Counter * 10 + fleeGoalPercent * 10) Implemented as MonsterClass::TransitionTo() "Run00" -> Starts running. -> MonsterClass:Anim_Checks will be skipped when this is set.
        WANDER, // (wanderGoalPercent * 10) Implemented as MonsterClass::Do_Wander(). Picks a random "wander point" up to 15x15 units away in a square, and "Walk00" -> Walks (sometimes aggressively) towards it?
        GUARD, // (guardGoalPercent * 10) Implemented as MonsterClass::Do_Guard(). Seems to try to walk to walk along a waypoint path back and forth until it has an actor target, where it will attempt to chase them. So, if we set the target as a waypoint, it will just keep walking across the path. -> How is this different from when we set waypoint goals?
        DEAD, // 100000 when (this->HitPoints = 0), otherwise 0.
        SLEEP, // (sleepGoalPercent * 20) - (aggresionAttackCounter * 10) - (CanSeeTarget * 10)
        UNKNOWN; // Can never be selected, always has a value of 0. This name was not in GoalNames[] either.

        /**
         * Gets the kcAISystemGoalType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return goalType
         */
        public static kcAISystemGoalType getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the kcAISystemGoalType from value " + value + ".");
            }

            return values()[value];
        }
    }
}