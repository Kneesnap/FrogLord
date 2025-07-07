package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseNumber;
import net.highwayfrogs.editor.games.konami.greatquest.script.cause.kcScriptCauseType;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'NUMBER' (send number) kcAction.
 * Each kcCEntity contains eight int32 variable slots, indexed by 0 through 7.
 * It may contain an entity argument, suggesting it can access variables on another entity.
 * However, upon further investigation, kcCScriptMgr::FireActorEvent() ensures that the script entity is always where the variables are obtained from.
 * In other words, when using the --AsEntity flag to send a number as another entity, it will use the variable value from the script entity.
 * This behavior can be observed by looking at BarrelInst004 in The Goblin Fort on PC.
 *  -> There are three barrels, and 004 sits on top of the other two.
 *  -> When either 002 or 003 are broken, they will send their number of 0 to break the top barrel (004)
 *  -> Upon receiving the number 0 from 002 or 003, it will also break, and mark the variables in 002/003 to be 10, so the next breakage will only move the top barrel up.
 *  -> Thus, if you break 004 then break 002/003, a glitch will occur where the top barrel will appear to break twice. This is only possible if it's using the variables as described.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionNumber extends kcAction {
    private static final kcArgument[] DEFAULT_ARGUMENTS = kcArgument.make(kcParamType.INT, "number", kcParamType.NUMBER_OPERATION, "operation");
    private static final kcArgument[] WITH_ENTITY_ARGUMENTS = kcArgument.make(kcParamType.INT, "number", kcParamType.NUMBER_OPERATION, "operation", kcParamType.HASH_NULL_IS_ZERO, "entity");
    private int number;
    private NumberOperation operation;

    public kcActionNumber(kcActionExecutor executor) {
        super(executor, kcActionID.NUMBER);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        if (arguments != null && arguments.length > 2 && arguments[1].getAsInteger() == NumberOperation.ENTITY_VARIABLE.ordinal() && arguments[2].getAsInteger() != 0) {
            return WITH_ENTITY_ARGUMENTS; // This does nothing, so we will always hide it unless we have a reason to show it.
        } else {
            return DEFAULT_ARGUMENTS;
        }
    }

    @Override
    public void load(kcParamReader reader) {
        this.number = reader.next().getAsInteger();
        this.operation = NumberOperation.getType(reader.next().getAsInteger(), false);
        if (this.operation == NumberOperation.ENTITY_VARIABLE) {
            int entityHash = reader.next().getAsInteger();
            if (entityHash != 0)
                getLogger().warning("kcActionNumber had an non-zero entity hash set! (Value: %08X) This value is has been determined to be ignored by the retail game!", entityHash);
        }
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.number);
        writer.write(this.operation.ordinal());
        if (this.operation == NumberOperation.ENTITY_VARIABLE)
            writer.write(0);
    }

    @Override
    protected void loadArguments(OptionalArguments arguments) {
        this.operation = arguments.useNext().getAsEnumOrError(NumberOperation.class);
        this.number = arguments.useNext().getAsInteger();
    }

    @Override
    protected void saveArguments(OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.operation);
        arguments.createNext().setAsInteger(this.number);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        if (this.operation == NumberOperation.RANDOM && this.number < 1)
            printWarning(logger, "the RANDOM operation requires a number greater than zero!");
        if (this.operation == NumberOperation.ENTITY_VARIABLE && (this.number < 0 || this.number > 7))
            printWarning(logger, this.number + " is not a valid entity variable id!");
    }

    @Override
    public void printAdvancedWarnings(kcScriptValidationData data) {
        super.printAdvancedWarnings(data);

        if (!data.anyCausesMatch(kcScriptCauseType.NUMBER, (kcScriptCauseNumber cause) -> true)) {
            printWarning(data.getLogger(), data.getEntityName() + " does not have an " + kcScriptCauseType.NUMBER.getDisplayName() + " script cause for handling numbers sent to it by " + getActionID().getFrogLordName() + "!");
            return;
        }

        // Ensure there is a cause listening for this number.
        if (this.operation == NumberOperation.LITERAL_NUMBER) {
            if (!data.anyCausesMatch(kcScriptCauseType.NUMBER, (kcScriptCauseNumber cause) -> cause.doesValueMatch(this.number)))
                printWarning(data.getLogger(), data.getEntityName() + " does not have an " + kcScriptCauseType.NUMBER.getDisplayName() + " script cause handling number " + this.number + ".");
        } else if (this.operation == NumberOperation.RANDOM) {
            if (!data.anyCausesMatch(kcScriptCauseType.NUMBER, (kcScriptCauseNumber cause) -> cause.couldRandomValueMatch(this.number)))
                printWarning(data.getLogger(), data.getEntityName() + " does not have an " + kcScriptCauseType.NUMBER.getDisplayName() + " script cause handling any of the numbers between 0 and " + this.number + ".");
        }
    }

    public enum NumberOperation {
        LITERAL_NUMBER, ENTITY_VARIABLE, RANDOM;

        /**
         * Gets the NumberOperation corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return numberOperation
         */
        public static NumberOperation getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the NumberOperation from value " + value + ".");
            }

            return values()[value];
        }
    }
}