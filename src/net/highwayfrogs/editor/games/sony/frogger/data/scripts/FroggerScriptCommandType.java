package net.highwayfrogs.editor.games.sony.frogger.data.scripts;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.format.FroggerScriptEnumFormatter;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.format.FroggerScriptFormatter;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.format.FroggerScriptNameBankFormatter;
import net.highwayfrogs.editor.games.sony.frogger.data.scripts.format.FroggerScriptSoundFormatter;

/**
 * A registry of script command types.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
public enum FroggerScriptCommandType {
    WAIT_UNTIL_TIMER(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, null),
    WAIT_UNTIL_ACTION_FINISHED(),
    WAIT_UNTIL_PATH_END(),
    SET_ACTION((FroggerScriptFormatter) null),
    PLAY_SOUND(FroggerScriptSoundFormatter.INSTANCE),
    RESTART(),
    END(),
    SET_TIMER(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, null),
    DEVIATE(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, FroggerScriptEnumFormatter.FORMAT_DIRECTION, null, null, null),
    WAIT_DEVIATED(),
    PLAY_RNDSOUND(FroggerScriptSoundFormatter.INSTANCE, null), // sound, chance
    SETLOOP(),
    ENDLOOP(),
    SCRIPT_IF(FroggerScriptEnumFormatter.FORMAT_SCRIPT_OPTION, FroggerScriptEnumFormatter.FORMAT_CONDITIONS, FroggerScriptNameBankFormatter.SCRIPT_INSTANCE, null),
    BREAKLOOP_IF_TIMER(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, null),
    PAUSE_ENTITY_ON_PATH(),
    UNPAUSE_ENTITY_ON_PATH(),
    ROTATE(FroggerScriptEnumFormatter.FORMAT_DIRECTION, null, null, null),
    WAIT_UNTIL_ROTATED(),
    HOME_IN_ON_FROG(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, FroggerScriptEnumFormatter.FORMAT_REGISTER_IDS, FroggerScriptEnumFormatter.FORMAT_REGISTER_IDS),
    RETURN_GOSUB_IF(FroggerScriptEnumFormatter.FORMAT_CONDITIONS),
    EJECT_FROG(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, null, null),
    CHOOSE_RND_CHECKPOINT(),
    APPEAR_ENTITY(),
    DISAPPEAR_ENTITY(),
    START_SCRIPT(FroggerScriptNameBankFormatter.SCRIPT_INSTANCE),
    AWARD_FROG_POINTS((FroggerScriptFormatter) null),
    AWARD_FROG_LIVES((FroggerScriptFormatter) null),
    AWARD_FROG_TIME((FroggerScriptFormatter) null),
    STOP_ROTATE(),
    STOP_DEVIATE(),
    PREPARE_REGISTERS(null, null),
    CLEAR_DEVIATE(),
    RETURN_DEVIATE(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, FroggerScriptEnumFormatter.FORMAT_DIRECTION, FroggerScriptEnumFormatter.FORMAT_REGISTER_IDS),
    REGISTER_CALLBACK(FroggerScriptEnumFormatter.FORMAT_CALLBACK_IDS, FroggerScriptNameBankFormatter.SCRIPT_CALLBACK_INSTANCE, FroggerScriptEnumFormatter.FORMAT_CONDITIONS, FroggerScriptEnumFormatter.FORMAT_CALLBACK),
    SET_ENTITY_TYPE(FroggerScriptEnumFormatter.FORMAT_ENTITY_TYPE),
    PLAY_SOUND_DISTANCE(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, null, FroggerScriptSoundFormatter.INSTANCE, FroggerScriptEnumFormatter.FORMAT_DIRECTION, null),
    PLAY_MOVING_SOUND(FroggerScriptSoundFormatter.INSTANCE, FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, null, null),
    STOP(),
    MUTATE_MESH_COLOR(),
    NO_COLL_CHECKPOINT(),
    COLL_CHECKPOINT(),
    KILL_SAFE_FROG(null, FroggerScriptSoundFormatter.INSTANCE),
    CHANGE_ENTITY_ANIM((FroggerScriptFormatter) null),
    CREATE_3D_SPRITE((FroggerScriptFormatter) null),
    PITCH_BEND_MOVING_SOUND(FroggerScriptEnumFormatter.FORMAT_REGISTER_TOGGLE, null, null, null, null, null),
    POP(),
    NO_COLLISION(),
    COLLISION();

    private final FroggerScriptFormatter[] formatters;

    FroggerScriptCommandType(FroggerScriptFormatter... formatters) {
        this.formatters = formatters;
        for (int i = 0; i < this.formatters.length; i++)
            if (this.formatters[i] == null)
                this.formatters[i] = FroggerScriptFormatter.INSTANCE; // Default.
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
     * Gets the FroggerScriptCommandType by its name. Returns null if not found.
     * @param name The name of the type.
     * @return commandType
     */
    public static FroggerScriptCommandType getByName(String name) {
        for (FroggerScriptCommandType type : values())
            if (type.name().equalsIgnoreCase(name))
                return type;
        return null;
    }
}