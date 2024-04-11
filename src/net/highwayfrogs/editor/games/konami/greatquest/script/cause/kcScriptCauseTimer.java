package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;

import java.util.List;

/**
 * Runs when an alarm rings. (When a certain amount of time passes since the timer started)
 * Created by Kneesnap on 8/17/2023.
 */
public class kcScriptCauseTimer extends kcScriptCause {
    private kcScriptCauseTimerState timerState;
    private int alarmId;

    public kcScriptCauseTimer() {
        super(kcScriptCauseType.TIMER, 1);
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.timerState = kcScriptCauseTimerState.getTimerState(subCauseType, false);
        this.alarmId = extraValues.get(0);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.timerState.ordinal());
        output.add(this.alarmId);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        switch (this.timerState) {
            case UNUSED_0:
                builder.append("Never (Alarm #").append(this.alarmId).append(" was probably supposed to trigger this, but can't)");
                break;
            case IN_PROGRESS:
                builder.append("When alarm #").append(this.alarmId).append(" is in progress");
                break;
            case FINISHED:
                builder.append("When alarm #").append(this.alarmId).append(" expires");
                break;
            default:
                throw new RuntimeException("Unexpected timer state " + this.timerState + ".");
        }

    }

    @Getter
    @AllArgsConstructor
    public enum kcScriptCauseTimerState {
        UNUSED_0, IN_PROGRESS, FINISHED;

        /**
         * Gets the kcScriptCauseTimerState corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return timerState
         */
        public static kcScriptCauseTimerState getTimerState(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine action type from value " + value + ".");
            }

            return values()[value];
        }
    }
}