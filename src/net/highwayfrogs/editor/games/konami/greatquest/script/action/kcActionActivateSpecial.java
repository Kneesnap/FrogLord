package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.entity.kcWaypointDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamReader;
import net.highwayfrogs.editor.games.konami.greatquest.script.interim.kcParamWriter;
import net.highwayfrogs.editor.games.konami.greatquest.script.*;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.objects.OptionalArguments;

/**
 * Represents the 'ACTIVATE_SPECIAL' kcAction.
 * This appears to be unused.
 * Created by Kneesnap on 8/24/2023.
 */
@Getter
public class kcActionActivateSpecial extends kcAction {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "shouldActivate", kcParamType.SPECIAL_ACTIVATION_BIT_MASK, "activateMask");
    private boolean shouldActivate;
    @NonNull private kcSpecialActivationMask activationMask = kcSpecialActivationMask.NONE;

    public kcActionActivateSpecial(kcActionExecutor executor) {
        super(executor, kcActionID.ACTIVATE_SPECIAL);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void load(kcParamReader reader) {
        this.shouldActivate = reader.next().getAsBoolean();
        this.activationMask = reader.next().getEnum(kcSpecialActivationMask.values());
    }

    @Override
    public void save(kcParamWriter writer) {
        writer.write(this.shouldActivate);
        writer.write(this.activationMask);
    }

    @Override
    protected void loadArguments(ILogger logger, OptionalArguments arguments) {
        this.activationMask = arguments.useNext().getAsEnumOrError(kcSpecialActivationMask.class);
        this.shouldActivate = arguments.useNext().getAsBoolean();
    }

    @Override
    protected void saveArguments(ILogger logger, OptionalArguments arguments, kcScriptDisplaySettings settings) {
        arguments.createNext().setAsEnum(this.activationMask);
        arguments.createNext().setAsBoolean(this.shouldActivate);
    }

    @Override
    public void printWarnings(ILogger logger) {
        super.printWarnings(logger);
        if (getExecutor() != null && !(getExecutor().getExecutingEntityDescription() instanceof kcWaypointDesc))
            printWarning(logger, "it only works when run as a waypoint entity.");
    }

    public enum kcSpecialActivationMask {
        NONE,
        ENTITIES,
        TERRAIN,
        BOTH;

        /**
         * Gets the kcSpecialActivationMask corresponding to the provided value.
         * @param value     The value to lookup.
         * @param allowNull If null is allowed.
         * @return goalType
         */
        public static kcSpecialActivationMask getType(int value, boolean allowNull) {
            if (value < 0 || value >= values().length) {
                if (allowNull)
                    return null;

                throw new RuntimeException("Couldn't determine the kcSpecialActivationMask from value " + value + ".");
            }

            return values()[value];
        }
    }
}