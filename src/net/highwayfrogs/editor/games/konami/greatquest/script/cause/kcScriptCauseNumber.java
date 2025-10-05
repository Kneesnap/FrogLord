package net.highwayfrogs.editor.games.konami.greatquest.script.cause;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionID;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionNumber;
import net.highwayfrogs.editor.games.konami.greatquest.script.action.kcActionNumber.NumberOperation;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScript;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptValidationData;
import net.highwayfrogs.editor.utils.logging.ILogger;
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
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        this.operation = arguments.useNext().getAsEnumOrError(kcScriptCauseNumberOperation.class);
        this.value = arguments.useNext().getAsInteger();
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.operation);
        arguments.createNext().setAsInteger(this.value);
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (this.operation.ordinal() << 24) ^ this.value;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && ((kcScriptCauseNumber) obj).getOperation() == this.operation
                && ((kcScriptCauseNumber) obj).getValue() == this.value;
    }

    @Override
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        builder.append("When a number is received that ");
        builder.append(this.operation.getDisplayAction());
        builder.append(' ').append(this.value);
    }

    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);
        if (!data.anyActionsMatch(kcActionID.NUMBER, this::doesActionMatch))
            printWarning(data.getLogger(), data.getEntityName() + " does not have a " + kcActionID.NUMBER.getFrogLordName() + " effect capable of causing it.");
        // We have confirmed that an entity terminating is still capable of calling SendNumber, and triggering its own script listeners, even if kcScriptCause.isEntityTerminated is true.
    }

    private boolean doesActionMatch(kcActionNumber action) {
        if (action.getOperation() == NumberOperation.ENTITY_VARIABLE) {
            return true; // We don't know what this could be, so we'll assume it matches.
        } else if (action.getOperation() == NumberOperation.LITERAL_NUMBER) {
            return doesValueMatch(action.getNumber());
        } else if (action.getOperation() == NumberOperation.RANDOM) {
            return couldRandomValueMatch(action.getNumber());
        } else {
            return false;
        }
    }

    /**
     * Tests if the value given matches the criteria.
     * @param value the value to test
     */
    public boolean doesValueMatch(int value) {
        switch (this.operation) {
            case NOT_EQUAL_TO:
                return value != this.value;
            case EQUAL_TO:
                return value == this.value;
            case LESS_THAN:
                return value < this.value;
            case GREATER_THAN:
                return value > this.value;
            case LESS_THAN_OR_EQUAL_TO:
                return value <= this.value;
            case GREATER_THAN_OR_EQUAL_TO:
                return value >= this.value;
            default:
                throw new UnsupportedOperationException("Unsupported operation: " + this.operation);
        }
    }

    /**
     * Returns true if a random value between [0, value) could activate this cause.
     * @param upperExclusiveBound the value boundary
     */
    public boolean couldRandomValueMatch(int upperExclusiveBound) {
        switch (this.operation) {
            case NOT_EQUAL_TO:
                return upperExclusiveBound > 1 || this.value != 0;
            case EQUAL_TO:
                return upperExclusiveBound > this.value;
            case LESS_THAN:
                return this.value > 0; // RAND(0, value) can always be zero.
            case GREATER_THAN:
                return upperExclusiveBound > this.value + 1;
            case LESS_THAN_OR_EQUAL_TO:
                return this.value >= 0;  // RAND(0, value) can always be zero.
            case GREATER_THAN_OR_EQUAL_TO:
                return upperExclusiveBound >= this.value + 1;
            default:
                throw new UnsupportedOperationException("Unsupported operation: " + this.operation);
        }
    }


    @Getter
    @AllArgsConstructor
    public enum kcScriptCauseNumberOperation { // the provided number <...> <value>
        NOT_EQUAL_TO("does not equal"),
        EQUAL_TO("is equal to"),
        LESS_THAN("is less than"),
        GREATER_THAN("is greater than"),
        LESS_THAN_OR_EQUAL_TO("is less than or equal to"),
        GREATER_THAN_OR_EQUAL_TO("is greater than or equal to");

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