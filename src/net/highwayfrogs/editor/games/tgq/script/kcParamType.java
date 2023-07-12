package net.highwayfrogs.editor.games.tgq.script;

/**
 * A registry of different plausible parameter types to scripts.
 * Created by Kneesnap on 6/26/2023.
 */
public enum kcParamType {
    ANY, // Can be anything. Generally represented as a hex integer because of the ambiguity.
    INT,
    UNSIGNED_INT, // int -> long?
    BOOLEAN,
    FLOAT,
    AXIS, // integer -> 0 = X, 1 = Y, 2 = Z, others = INVALID.
    HASH, // Unsigned int.
    INVENTORY_ITEM, // Unsigned int.
    PARTICLE, // Unsigned int.
    ATTACH_ID, // Unsigned int.
}
