package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;
import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * Represents the 'NUMBER' (broadcast number) kcAction.
 * Each kcCEntity contains eight int32 variable slots, indexed by 0 through 7.
 * It may contain an entity argument, suggesting it can access variables on another entity.
 * However, upon further investigation, kcCScriptMgr::FireActorEvent() ignores the script parameter, always using the target entity instead.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionNumber extends kcAction {
    private static final kcArgument[] DEFAULT_ARGUMENTS = kcArgument.make(kcParamType.INT, "number", kcParamType.NUMBER_OPERATION, "operation");
    private static final kcArgument[] WITH_ENTITY_ARGUMENTS = kcArgument.make(kcParamType.INT, "number", kcParamType.NUMBER_OPERATION, "operation", kcParamType.HASH, "entity");
    private int number;
    private NumberOperation operation;
    private int entityHash;

    public kcActionNumber(GreatQuestChunkedFile chunkedFile) {
        super(chunkedFile, kcActionID.NUMBER);
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
            this.entityHash = reader.next().getAsInteger();
            if (this.entityHash != 0)
                getLogger().warning("kcActionNumber had an non-zero entity hash set! (Value: " + NumberUtils.toHexString(this.entityHash) + ") This value is has been determined to be ignored by the retail game!");
        }
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