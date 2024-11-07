package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
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
    private int repeatCount;

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
        this.repeatCount = reader.next().getAsInteger();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.alarmId);
        writer.write(this.durationMillis);
        writer.write(this.repeatCount);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.alarmId = arguments.useNext().getAsInteger();
        this.durationMillis = (int) (arguments.useNext().getAsFloat() * 1000F);
        StringNode repeatNode = arguments.use(REPEAT_ARGUMENT_NAME);
        this.repeatCount = repeatNode != null ? repeatNode.getAsInteger() : 0;
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsInteger(this.alarmId);
        arguments.createNext().setAsFloat(this.durationMillis / 1000F);
        if (this.repeatCount != 0)
            arguments.getOrCreate(REPEAT_ARGUMENT_NAME).setAsInteger(this.repeatCount);
    }
}
