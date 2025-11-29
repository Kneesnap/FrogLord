package net.highwayfrogs.editor.games.konami.greatquest.script;

/**
 * A registry of different plausible parameter types to scripts.
 * Created by Kneesnap on 6/26/2023.
 */
public enum kcParamType {
    HEX_INTEGER,
    INT16,
    INT,
    UNSIGNED_INT, // int -> long?
    BOOLEAN,
    INVERTED_BOOLEAN,
    FLOAT,
    AXIS, // integer -> 0 = X, 1 = Y, 2 = Z, others = INVALID.
    HASH, // Unsigned int. This should be used when either -1 or 0 will be valid to represent null.
    HASH_NULL_IS_ZERO, // Unsigned int.
    INVENTORY_ITEM, // Unsigned int.
    AI_GOAL_TYPE, // Unsigned int.
    NUMBER_OPERATION,
    CAMERA_PIVOT_PARAM,
    ATTACH_ID, // Unsigned int.
    SOUND, // INT
    MILLISECONDS,
    ANIMATION_TICK,
    TIMESTAMP_TICK,
    DOUBLE_TIMESTAMP_TICK,
    ANGLE,
    VARIABLE_ID,
    ALARM_ID,
    BONE_TAG,
    SPECIAL_ACTIVATION_BIT_MASK
}