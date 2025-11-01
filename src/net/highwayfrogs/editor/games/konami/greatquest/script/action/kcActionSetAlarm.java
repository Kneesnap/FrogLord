package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseTimer;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model.GreatQuestModelMesh;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

/**
 * Represents an action for setting an alarm.
 * Created by Kneesnap on 11/5/2024.
 */
@Getter
public class kcActionSetAlarm extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.ALARM_ID, "alarmId", kcParamType.MILLISECONDS, "duration", kcParamType.INT, "repeatCount");
    private int alarmId;
    private int durationMillis;
    private int intervalCount; // If greater than zero, multiplies the time limit in kcCEntity::SetAlarm(), every time the time limit is reached, it will fire 'IN_PROGRESS'. So, it's the total number of times to run.

    private static final String REPEAT_ARGUMENT_NAME = "Repeat";
    private static final String MAX_ARGUMENT = "MAX";

    public kcActionSetAlarm(kcActionExecutor executor) {
        super(executor, kcActionID.SET_ALARM);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public int getGqsArgumentCount(kcArgument[] argumentTemplates) {
        return super.getGqsArgumentCount(argumentTemplates) - 1;
    }

    @Override
    public void load(kcParamReader reader) {
        this.alarmId = reader.next().getAsInteger();
        this.durationMillis = reader.next().getAsInteger();
        this.intervalCount = reader.next().getAsInteger();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.alarmId);
        writer.write(this.durationMillis);
        writer.write(this.intervalCount);
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        this.alarmId = arguments.useNext().getAsInteger();
        this.durationMillis = (int) (arguments.useNext().getAsFloat() * 1000F);
        StringNode repeatNode = arguments.use(REPEAT_ARGUMENT_NAME);

        if (repeatNode != null) {
            if (MAX_ARGUMENT.equalsIgnoreCase(repeatNode.getAsString())) {
                this.intervalCount = getMaximumRepeatCount(this.durationMillis);
            } else {
                this.intervalCount = repeatNode.getAsInteger() + 1;
            }
        } else {
            this.intervalCount = 0;
        }
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsInteger(this.alarmId);
        arguments.createNext().setAsFloat(this.durationMillis / 1000F);
        if (this.intervalCount == getMaximumRepeatCount(this.durationMillis)) {
            arguments.getOrCreate(REPEAT_ARGUMENT_NAME).setAsString(MAX_ARGUMENT, false);
        } else if (this.intervalCount != 1) {
            arguments.getOrCreate(REPEAT_ARGUMENT_NAME).setAsInteger(this.intervalCount - 1);
        }
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        if (this.intervalCount < 0)
            printWarning(logger, "an invalid repeatCount was provided! (" + (this.intervalCount - 1) + ")");
    }

    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);

        // Ensure there is a cause listening for this alarm.
        if (!data.anyCausesMatch(kcScriptCauseType.TIMER, (kcScriptCauseTimer cause) -> cause.getAlarmId() == this.alarmId))
            printWarning(data.getLogger(), data.getEntityName() + " does not have an " + kcScriptCauseType.TIMER.getDisplayName() + " script cause handling alarm ID " + this.alarmId + ".");
        if (this.durationMillis > 0 && this.intervalCount > getMaximumRepeatCount(this.durationMillis))
            printWarning(data.getLogger(), this.intervalCount + " is greater than the MAX repeat value. (Use --Repeat MAX instead?)");
    }

    /**
     * Alarms are set in kcCEntity::SetAlarm.
     * The game stores the alarm stores the time limit in a uint32.
     * So, the repeatCount must be calculated in order to make the calculated value as high as possible without overflowing the uint32.
     * @param durationMillis the alarm duration, in milliseconds
     * @return maximumRepeatCount
     */
    private static int getMaximumRepeatCount(int durationMillis) {
        // The calculation in is kcCEntity::SetAlarm is (((uint duration * TICKS_PER_SECOND) / 1000) * repeatCount)
        // timeLimit = (((uint duration * TICKS_PER_SECOND) / 1000) * repeatCount)
        // repeatCount = timeLimit / ((duration * TICKS_PER_SECOND) / 1000)
        // Solving for maximum 32-bit value is therefore:
        // repeatCount = uint32.MAX / ((duration * TICKS_PER_SECOND) / 1000)
        if (durationMillis == 0) // Don't allow divide by zero.
            return Integer.MAX_VALUE;

        // This has been tested to work by adding 1 to the result of this function, and observing that it causes repeating to break.
        return (int) (0xFFFFFFFFL / ((durationMillis * GreatQuestModelMesh.TICKS_PER_SECOND) / 1000)); // Even a value of durationMillis=1 will produce a valid value.
    }
}