package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents the 'NUMBER' (broadcast number) kcAction.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionNumber extends kcAction {
    private static final kcArgument[] DEFAULT_ARGUMENTS = kcArgument.make(kcParamType.INT, "number", kcParamType.NUMBER_OPERATION, "operation");
    private static final kcArgument[] WITH_ENTITY_ARGUMENTS = kcArgument.make(kcParamType.INT, "number", kcParamType.NUMBER_OPERATION, "operation", kcParamType.HASH, "entity");
    private int number;
    private NumberOperation operation;
    private int entityHash;

    public kcActionNumber() {
        super(kcActionID.NUMBER);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        if (arguments != null && arguments.length > 2 && arguments[1].getAsInteger() == NumberOperation.ENTITY_VARIABLE.ordinal()) {
            return WITH_ENTITY_ARGUMENTS;
        } else {
            return DEFAULT_ARGUMENTS;
        }
    }

    @Override
    public void load(kcParamReader reader) {
        this.number = reader.next().getAsInteger();
        this.operation = NumberOperation.getType(reader.next().getAsInteger(), false);
        if (this.operation == NumberOperation.ENTITY_VARIABLE)
            this.entityHash = reader.next().getAsInteger();
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.number);
        writer.write(this.operation.ordinal());
        if (this.operation == NumberOperation.ENTITY_VARIABLE)
            writer.write(this.entityHash);
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