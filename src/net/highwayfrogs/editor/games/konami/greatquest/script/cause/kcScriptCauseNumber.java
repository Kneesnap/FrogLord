package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

import java.util.List;

/**
 * The way this works is that each kcCEntity has 8 variable slots. Or rather an array of 8 32bit integers.
 * When the 'NUMBER' command is run (the kcAction, not this) any functions which have a cause of kcScriptCauseNumber on the target entity
 * will run a test against the number and run if it succeeds.
 * NOTE: UNLIKE MOST OTHER kcScriptCauses, THE subCauseType is USED TO SPECIFY WHAT OPERATION SHOULD RUN, NOT CORRECTNESS.
 * Created by Kneesnap on 8/18/2023.
 */
@Setter
@Getter
public class kcScriptCauseNumber extends kcScriptCause {
    private kcScriptCauseNumberOperation operation;
    private int value;

    public kcScriptCauseNumber(kcScript script) {
        super(script, kcScriptCauseType.NUMBER, 1, 2);
    }

    public kcScriptCauseNumber(kcScript script, kcScriptCauseNumberOperation operation, int number) {
        this(script);
        this.operation = operation;
        this.value = number;
    }

    @Override
    public void load(int subCauseType, List<Integer> extraValues) {
        this.operation = kcScriptCauseNumberOperation.getOperation(subCauseType, false);
        this.value = extraValues.get(0);
    }

    @Override
    public void save(List<Integer> output) {
        output.add(this.operation.ordinal());
        output.add(this.value);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.operation = arguments.useNext().getAsEnumOrError(kcScriptCauseNumberOperation.class);
        this.value = arguments.useNext().getAsInteger();
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.operation);
        arguments.createNext().setAsInteger(this.value);
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When a number is received that ");
        builder.append(this.operation.getDisplayAction());
        builder.append(' ').append(this.value);
    }


    @Getter
    @AllArgsConstructor
    public enum kcScriptCauseNumberOperation { // the provided number <...> <value>
        DOES_NOT_EQUAL("does not equal"),
        EQUALS("is equal to"),
        LESS_THAN("is less than"),
        GREATER_THAN("is greater than"),
        LESS_THAN_OR_EQUAL("is less than or equal to"),
        GREATER_THAN_OR_EQUAL("is greater than or equal to");

        private final String displayAction;

        /**
         * Gets the kcScriptCauseNumberOperation corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return operation
         */
        public static kcScriptCauseNumberOperation getOperation(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine number operation type from value " + value + ".");
            }

            return values()[value];
        }
    }
}