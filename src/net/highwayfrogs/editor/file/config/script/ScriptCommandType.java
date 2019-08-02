package net.highwayfrogs.editor.file.config.script;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A registry of script command types.
 * Created by Kneesnap on 8/1/2019.
 */
@Getter
@AllArgsConstructor
public enum ScriptCommandType {
    WAIT_UNTIL_TIMER(3),
    WAIT_UNTIL_ACTION_FINISHED(1),
    WAIT_UNTIL_PATH_END(1),
    SET_ACTION(2),
    PLAY_SOUND(2),
    RESTART(1),
    END(1),
    SET_TIMER(3),
    DEVIATE(6),
    WAIT_DEVIATED(1),
    PLAY_RNDSOUND(3),
    SETLOOP(1),
    ENDLOOP(1),
    SCRIPT_IF(5),
    BREAKLOOP_IF_TIMER(3),
    PAUSE_ENTITY_ON_PATH(1),
    UNPAUSE_ENTITY_ON_PATH(1),
    ROTATE(5),
    WAIT_UNTIL_ROTATED(1),
    HOME_IN_ON_FROG(4),
    RETURN_GOSUB_IF(2),
    EJECT_FROG(4),
    CHOOSE_RND_CHECKPOINT(1),
    APPEAR_ENTITY(1),
    DISAPPEAR_ENTITY(1),
    START_SCRIPT(2),
    AWARD_FROG_POINTS(2),
    AWARD_FROG_LIVES(2),
    AWARD_FROG_TIME(2),
    STOP_ROTATE(1),
    STOP_DEVIATE(1),
    PREPARE_REGISTERS(3),
    CLEAR_DEVIATE(1),
    RETURN_DEVIATE(4),
    REGISTER_CALLBACK(5),
    SET_ENTITY_TYPE(2),
    PLAY_SOUND_DISTANCE(6),
    PLAY_MOVING_SOUND(5),
    STOP(1),
    MUTATE_MESH_COLOR(1),
    NO_COLL_CHECKPOINT(1),
    COLL_CHECKPOINT(1),
    KILL_SAFE_FROG(3),
    CHANGE_ENTITY_ANIM(2),
    CREATE_3D_SPRITE(2),
    PITCH_BEND_MOVING_SOUND(7),
    POP(1),
    NO_COLLISION(1),
    COLLISION(1);

    private final int size;

    /**
     * Does this command end a script?
     * @return isFinal
     */
    public boolean isFinalCommand() {
        return this == STOP || this == RESTART || this == END;
    }

    /**
     * Gets the amount of arguments this command takes.
     * @return argCount
     */
    public int getArgumentCount() {
        return getSize() - 1;
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
