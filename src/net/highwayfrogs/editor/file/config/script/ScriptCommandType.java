package net.highwayfrogs.editor.file.config.script;

import lombok.Getter;
import net.highwayfrogs.editor.file.config.script.format.BankFormatter;
import net.highwayfrogs.editor.file.config.script.format.EnumFormatter;
import net.highwayfrogs.editor.file.config.script.format.ScriptFormatter;

/**
 * A registry of script command types.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
public enum ScriptCommandType {
    WAIT_UNTIL_TIMER(EnumFormatter.FORMAT_REGISTER_TOGGLE, null),
    WAIT_UNTIL_ACTION_FINISHED(),
    WAIT_UNTIL_PATH_END(),
    SET_ACTION((ScriptFormatter) null),
    PLAY_SOUND(BankFormatter.SOUND_INSTANCE),
    RESTART(),
    END(),
    SET_TIMER(EnumFormatter.FORMAT_REGISTER_TOGGLE, null),
    DEVIATE(EnumFormatter.FORMAT_REGISTER_TOGGLE, EnumFormatter.FORMAT_DIRECTION, null, null, null),
    WAIT_DEVIATED(),
    PLAY_RNDSOUND(BankFormatter.SOUND_INSTANCE, null), // sound, chance
    SETLOOP(),
    ENDLOOP(),
    SCRIPT_IF(EnumFormatter.FORMAT_SCRIPT_OPTION, EnumFormatter.FORMAT_CONDITIONS, BankFormatter.SCRIPT_INSTANCE, null),
    BREAKLOOP_IF_TIMER(EnumFormatter.FORMAT_REGISTER_TOGGLE, null),
    PAUSE_ENTITY_ON_PATH(),
    UNPAUSE_ENTITY_ON_PATH(),
    ROTATE(EnumFormatter.FORMAT_DIRECTION, null, null, null),
    WAIT_UNTIL_ROTATED(),
    HOME_IN_ON_FROG(EnumFormatter.FORMAT_REGISTER_TOGGLE, EnumFormatter.FORMAT_REGISTER_IDS, EnumFormatter.FORMAT_REGISTER_IDS),
    RETURN_GOSUB_IF(EnumFormatter.FORMAT_CONDITIONS),
    EJECT_FROG(EnumFormatter.FORMAT_REGISTER_TOGGLE, null, null),
    CHOOSE_RND_CHECKPOINT(),
    APPEAR_ENTITY(),
    DISAPPEAR_ENTITY(),
    START_SCRIPT(BankFormatter.SCRIPT_INSTANCE),
    AWARD_FROG_POINTS((ScriptFormatter) null),
    AWARD_FROG_LIVES((ScriptFormatter) null),
    AWARD_FROG_TIME((ScriptFormatter) null),
    STOP_ROTATE(),
    STOP_DEVIATE(),
    PREPARE_REGISTERS(null, null),
    CLEAR_DEVIATE(),
    RETURN_DEVIATE(EnumFormatter.FORMAT_REGISTER_TOGGLE, EnumFormatter.FORMAT_DIRECTION, EnumFormatter.FORMAT_REGISTER_IDS),
    REGISTER_CALLBACK(EnumFormatter.FORMAT_CALLBACK_IDS, BankFormatter.SCRIPT_INSTANCE, EnumFormatter.FORMAT_CONDITIONS, EnumFormatter.FORMAT_CALLBACK),
    SET_ENTITY_TYPE(EnumFormatter.FORMAT_ENTITY_TYPE),
    PLAY_SOUND_DISTANCE(EnumFormatter.FORMAT_REGISTER_TOGGLE, null, BankFormatter.SOUND_INSTANCE, EnumFormatter.FORMAT_DIRECTION, null),
    PLAY_MOVING_SOUND(BankFormatter.SOUND_INSTANCE, EnumFormatter.FORMAT_REGISTER_TOGGLE, null, null),
    STOP(),
    MUTATE_MESH_COLOR(),
    NO_COLL_CHECKPOINT(),
    COLL_CHECKPOINT(),
    KILL_SAFE_FROG(null, BankFormatter.SOUND_INSTANCE),
    CHANGE_ENTITY_ANIM((ScriptFormatter) null),
    CREATE_3D_SPRITE((ScriptFormatter) null),
    PITCH_BEND_MOVING_SOUND(EnumFormatter.FORMAT_REGISTER_TOGGLE, null, null, null, null, null),
    POP(),
    NO_COLLISION(),
    COLLISION();

    private final ScriptFormatter[] formatters;

    ScriptCommandType(ScriptFormatter... formatters) {
        this.formatters = formatters;
        for (int i = 0; i < this.formatters.length; i++)
            if (this.formatters[i] == null)
                this.formatters[i] = ScriptFormatter.INSTANCE; // Default.
    }

    /**
     * Does this command end a script?
     * @return isFinal
     */
    public boolean isFinalCommand() {
        return this == STOP || this == RESTART || this == END;
    }

    /**
     * Gets the total number of integers this command will use.
     * @return size
     */
    public int getSize() {
        return this.formatters.length + 1;
    }

    /**
     * Gets the amount of arguments this command takes.
     * @return argCount
     */
    public int getArgumentCount() {
        return this.formatters.length;
    }

    /**
     * Gets the ScriptCommandType by its name. Returns null if not found.
     * @param name The name of the type.
     * @return commandType
     */
    public static ScriptCommandType getByName(String name) {
        for (ScriptCommandType type : values())
            if (type.name().equalsIgnoreCase(name))
                return type;
        return null;
    }
}
