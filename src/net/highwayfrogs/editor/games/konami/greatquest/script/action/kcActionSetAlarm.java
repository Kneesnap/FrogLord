package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseTimer;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;
import net.highwayfrogs.editor.utils.objects.StringNode;

/**
 * Represents an action for setting an alarm.
 * Created by Kneesnap on 11/5/2024.
 */
@Getter
public class kcActionSetAlarm extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.ALARM_ID, "alarmId", kcParamType.MILLISECONDS, "duration", kcParamType.INT, "intervalCount");
    private int alarmId;
    private int durationMillis;
    private int intervalCount; // If greater than zero, multiplies the time limit in kcCEntity::SetAlarm(), every time the time limit is reached, it will fire 'IN_PROGRESS'. So, it's the total number of times to run.

    private static final String REPEAT_ARGUMENT_NAME = "Repeat";

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
    protected void loadArguments(OptionalArguments arguments) {
        this.alarmId = arguments.useNext().getAsInteger();
        this.durationMillis = (int) (arguments.useNext().getAsFloat() * 1000F);
        StringNode repeatNode = arguments.use(REPEAT_ARGUMENT_NAME);
        this.intervalCount = repeatNode != null ? repeatNode.getAsInteger() + 1 : 0;
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsInteger(this.alarmId);
        arguments.createNext().setAsFloat(this.durationMillis / 1000F);
        if (this.intervalCount != 0)
            arguments.getOrCreate(REPEAT_ARGUMENT_NAME).setAsInteger(this.intervalCount - 1);
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
    }
}