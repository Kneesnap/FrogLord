package net.highwayfrogs.editor.games.greatquest.script.action;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.greatquest.script.kcParamType;

/**
 * Represents the 'AI_SETGOAL' kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
@Setter
public class kcActionAISetGoal extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.GOAL_TYPE, "goal");
    private kcGoalType goalType;

    public kcActionAISetGoal() {
        super(kcActionID.AI_SETGOAL);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.goalType = kcGoalType.getType(reader.next().getAsInteger(), false);
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.goalType.ordinal());
    }

    public enum kcGoalType {
        NONE, TIME, ROTATE_X, ROTATE_Y, ROTATE_Z, ROTATE_XYZ, SEEK, ANIMATION, BUTTON_CONTINUE;

        /**
         * Gets the kcGoalType corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return goalType
         */
        public static kcGoalType getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the kcGoalType from value " + value + ".");
            }

            return values()[value];
        }
    }
}