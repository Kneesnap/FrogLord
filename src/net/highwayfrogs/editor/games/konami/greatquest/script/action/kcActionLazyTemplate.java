package net.highwayfrogs.editor.games.konami.greatquest.script.action;

import net.highwayfrogs.editor.games.konami.greatquest.script.kcActionExecutor;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcParamType;

/**
 * Represents many different kcActions.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionLazyTemplate extends kcActionTemplate {
    private final kcArgument[] arguments;

    public kcActionLazyTemplate(kcActionExecutor executor, kcActionID actionID, kcArgument[] arguments) {
        super(executor, actionID);
        this.arguments = arguments;
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return this.arguments;
    }

    public static final kcArgument[] ADD_POSITION_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.FLOAT, "displacement");
    public static final kcArgument[] ADD_POSITION_XYZ_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");
    public static final kcArgument[] SET_ROTATION_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.ANGLE, "angle");
    public static final kcArgument[] SET_ROTATION_XYZ_ARGUMENTS = kcArgument.make(kcParamType.ANGLE, "xAngle", kcParamType.ANGLE, "yAngle", kcParamType.ANGLE, "zAngle");
    public static final kcArgument[] ADD_ROTATION_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.ANGLE, "angle");
    public static final kcArgument[] ADD_ROTATION_XYZ_ARGUMENTS = kcArgument.make(kcParamType.ANGLE, "xAngle", kcParamType.ANGLE, "yAngle", kcParamType.ANGLE, "zAngle");
    public static final kcArgument[] ROTATE_RIGHT_ARGUMENTS = kcArgument.make(kcParamType.ANGLE, "angle");
    public static final kcArgument[] ROTATE_LEFT_ARGUMENTS = kcArgument.make(kcParamType.ANGLE, "angle");
    public static final kcArgument[] WAIT_ARGUMENTS = kcArgument.make(kcParamType.MILLISECONDS, "timeInSeconds");
    public static final kcArgument[] WAIT_ROTATE_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis");
    public static final kcArgument[] LOOP_ARGUMENTS = kcArgument.make(kcParamType.INT, "loopCount");
    public static final kcArgument[] IMPULSE_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");
    public static final kcArgument[] PROMPT_ARGUMENTS = kcArgument.make(kcParamType.HASH, "promptRes");
    public static final kcArgument[] VARIABLE_SET_ARGUMENTS = kcArgument.make(kcParamType.VARIABLE_ID, "variableId", kcParamType.INT, "value");
    public static final kcArgument[] VARIABLE_ADD_ARGUMENTS = kcArgument.make(kcParamType.VARIABLE_ID, "variableId", kcParamType.INT, "value");
    public static final kcArgument[] WITH_ITEM_ARGUMENTS = kcArgument.make(kcParamType.INVENTORY_ITEM, "item");
    public static final kcArgument[] SAVEPOINT_ARGUMENTS = kcArgument.make(kcParamType.UNSIGNED_INT, "savePointNumber", kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");
}