package net.highwayfrogs.editor.file.patch.commands;

import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.reference.PatchValueReference;

import java.util.List;

/**
 * Adds an if condition option.
 * Created by Kneesnap on 1/15/2020.
 */
public class PatchCommandIf extends PatchCommand {
    public PatchCommandIf() {
        super("if");
    }

    @Override
    @SuppressWarnings("DuplicateExpressions")
    public void execute(PatchRuntime runtime, List<PatchValueReference> args) {
        PatchValue value = runtime.getVariable(getValueText(args, 0));
        String comparison = getValueText(args, 1);
        PatchValue otherValue = getValue(runtime, args, 2);

        boolean conditionPass;
        switch (comparison) {
            case "==":
                conditionPass = value.toString().equals(otherValue.toString());
                break;
            case "!=":
                conditionPass = !value.toString().equals(otherValue.toString());
                break;
            case ">":
                conditionPass = (value.isDecimal() ? value.getAsDecimal() : value.getAsInteger()) > (otherValue.isDecimal() ? otherValue.getAsDecimal() : otherValue.getAsInteger());
                break;
            case ">=":
                conditionPass = (value.isDecimal() ? value.getAsDecimal() : value.getAsInteger()) >= (otherValue.isDecimal() ? otherValue.getAsDecimal() : otherValue.getAsInteger());
                break;
            case "<":
                conditionPass = (value.isDecimal() ? value.getAsDecimal() : value.getAsInteger()) < (otherValue.isDecimal() ? otherValue.getAsDecimal() : otherValue.getAsInteger());
                break;
            case "<=":
                conditionPass = (value.isDecimal() ? value.getAsDecimal() : value.getAsInteger()) <= (otherValue.isDecimal() ? otherValue.getAsDecimal() : otherValue.getAsInteger());
                break;
            default:
                throw new RuntimeException("Unknown comparison: '" + comparison + "'.");
        }

        if (conditionPass)
            runtime.setExecutionLevel(runtime.getExecutionLevel() + 1);
    }
}
