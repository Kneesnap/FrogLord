package net.highwayfrogs.editor.games.greatquest.script.action;

import net.highwayfrogs.editor.games.greatquest.script.kcArgument;
import net.highwayfrogs.editor.games.greatquest.script.kcParam;
import net.highwayfrogs.editor.games.greatquest.script.kcParamType;

/**
 * Represents many different kcActions.
 * Created by Kneesnap on 8/24/2023.
 */
public class kcActionLazyTemplate extends kcActionTemplate {
    private final kcArgument[] arguments;

    public kcActionLazyTemplate(kcActionID actionID, kcArgument[] arguments) {
        super(actionID);
        this.arguments = arguments;
    }

    @Override
    public kcArgument[] getArgumentTemplate(kcParam[] arguments) {
        return this.arguments;
    }

    public static final kcArgument[] ADD_POSITION_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.FLOAT, "displacement");
    public static final kcArgument[] ADD_POSITION_XYZ_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");
    public static final kcArgument[] SET_ROTATION_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.FLOAT, "angleInRadians");
    public static final kcArgument[] SET_ROTATION_XYZ_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "xAngleInRadians", kcParamType.FLOAT, "yAngleInRadians", kcParamType.FLOAT, "zAngleInRadians");
    public static final kcArgument[] ADD_ROTATION_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis", kcParamType.FLOAT, "angleInRadians");
    public static final kcArgument[] ADD_ROTATION_XYZ_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "xAngleInRadians", kcParamType.FLOAT, "yAngleInRadians", kcParamType.FLOAT, "zAngleInRadians");
    public static final kcArgument[] ROTATE_RIGHT_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "angleInRadians");
    public static final kcArgument[] ROTATE_LEFT_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "angleInRadians");
    public static final kcArgument[] SET_SEQUENCE_ARGUMENTS = kcArgument.make(kcParamType.HASH, "sequence", kcParamType.BOOLEAN, "interrupt", kcParamType.BOOLEAN, "openBoneChannel");
    public static final kcArgument[] WAIT_ARGUMENTS = kcArgument.make(kcParamType.INT, "time");
    public static final kcArgument[] WAIT_ROTATE_ARGUMENTS = kcArgument.make(kcParamType.AXIS, "axis");
    public static final kcArgument[] LOOP_ARGUMENTS = kcArgument.make(kcParamType.INT, "loopCount");
    public static final kcArgument[] IMPULSE_ARGUMENTS = kcArgument.make(kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");
    public static final kcArgument[] DAMAGE_ARGUMENTS = kcArgument.make(kcParamType.UNSIGNED_INT, "weaponMask", kcParamType.INT, "attackStrength");
    public static final kcArgument[] PROMPT_ARGUMENTS = kcArgument.make(kcParamType.HASH, "promptRes");
    public static final kcArgument[] DIALOG_ARGUMENTS = kcArgument.make(kcParamType.HASH, "dialogRes");
    public static final kcArgument[] SET_ALARM_ARGUMENTS = kcArgument.make(kcParamType.INT, "alarmId", kcParamType.UNSIGNED_INT, "duration", kcParamType.INT, "repeatCount");
    public static final kcArgument[] PLAY_SFX_ARGUMENTS = kcArgument.make(kcParamType.INT, "sound");
    public static final kcArgument[] VARIABLE_SET_ARGUMENTS = kcArgument.make(kcParamType.INT, "variableId", kcParamType.INT, "value");
    public static final kcArgument[] VARIABLE_ADD_ARGUMENTS = kcArgument.make(kcParamType.INT, "variableId", kcParamType.INT, "value");
    public static final kcArgument[] PARTICLE_ARGUMENTS = kcArgument.make(kcParamType.HASH, "particle");
    public static final kcArgument[] WITH_ITEM_ARGUMENTS = kcArgument.make(kcParamType.INVENTORY_ITEM, "item");
    public static final kcArgument[] GIVE_TAKE_ITEM_ARGUMENTS = kcArgument.make(kcParamType.BOOLEAN, "takeItem", kcParamType.INVENTORY_ITEM, "item");
    public static final kcArgument[] GIVE_DAMAGE_ARGUMENTS = kcArgument.make(kcParamType.INT, "attackStrength", kcParamType.UNSIGNED_INT, "weaponMask");
    public static final kcArgument[] SAVEPOINT_ARGUMENTS = kcArgument.make(kcParamType.UNSIGNED_INT, "savePointNumber", kcParamType.FLOAT, "x", kcParamType.FLOAT, "y", kcParamType.FLOAT, "z");
}