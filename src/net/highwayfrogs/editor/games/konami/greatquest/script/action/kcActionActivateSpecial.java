package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.entity.kcWaypointDesc;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

import java.util.logging.Logger;

/**
 * Represents the 'ACTIVATE_SPECIAL' kcAction.
 * This appears to be unused.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionActivateSpecial extends kcActionTemplate {
    private static final kcArgument[] ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "shouldActivate", kcParamType.SPECIAL_ACTIVATION_BIT_MASK, "activateMask");

    public kcActionActivateSpecial(kcActionExecutor executor) {
        super(executor, kcActionID.ACTIVATE_SPECIAL);
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return ARGUMENTS;
    }

    @Override
    public void printWarnings(Logger logger, String gqsAction) {
        super.printWarnings(logger, gqsAction);
        if (getExecutor() != null && !(getExecutor().getExecutingEntityDescription() instanceof kcWaypointDesc))
            logger.warning("The action '" + gqsAction + "' will be skipped by the game, since it only works when run as a waypoint entity.");
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