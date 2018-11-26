package net.highwayfrogs.editor.file.map.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static net.highwayfrogs.editor.file.map.entity.EntityTypeFlags.*;

/**
 * A registry of all entity types. Akin to the form book.
 * TODO: Associate scripts from the entry here.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@AllArgsConstructor
public enum EntityBook {
    STATIC(FLAG_STATIC), // Create a MOF which does not move or is animated.
    MOVING(FLAG_PATH_RUNNER), // Creates a MOF which moves.
    CHECKPOINT(FLAG_STATIC), // Baby Frogs, Multiplayer Flags.
    DES_FALLING_ROCK(FLAG_IMMORTAL),
    DES_EARTHQUAKE(FLAG_IMMORTAL), // Cut entity.
    DES_THERMAL(FLAG_PATH_RUNNER), // Cut entity.
    DYNAMIC(FLAG_IMMORTAL), // A MOF which moves around the world but is not path-based.
    CAV_WEB(FLAG_STATIC),
    CAV_SPIDER(FLAG_IMMORTAL),
    CAV_FROGGER_LIGHT(FLAG_IMMORTAL),
    ORG_LOG_SNAKE(FLAG_IMMORTAL),
    BONUS_FLY(FLAG_STATIC | FLAG_TONGUEABLE),
    SUB_TURTLE(FLAG_PATH_RUNNER),
    SWP_SQUIRT(FLAG_IMMORTAL), // What is a squirt?
    SWP_CRUSHER(FLAG_IMMORTAL),
    TRIGGER(FLAG_IMMORTAL), // Unsure entirely what this is. Fingers crossed it's a generic trigger that can be used in any level to do things, this would be great for modding.
    ORG_BABY_FROG(FLAG_IMMORTAL),
    DES_SNAKE(FLAG_PATH_RUNNER),
    ORG_BEAVER(FLAG_PATH_RUNNER | FLAG_IMMORTAL),
    DES_VULTURE(FLAG_PATH_RUNNER),
    ORG_FLY(FLAG_IMMORTAL | FLAG_TONGUEABLE),
    ORG_CROC_HEAD(FLAG_IMMORTAL),
    FOR_HIVE(FLAG_IMMORTAL),
    SWP_PRESS(FLAG_IMMORTAL), // What's this?
    CAV_FAT_FIRE_FLY(FLAG_STATIC | FLAG_TONGUEABLE),
    DES_CROC_HEAD(FLAG_IMMORTAL),
    MULTIPOINT(FLAG_STATIC), // Multiplayer flag checkpoint.
    SUB_DOG(FLAG_PATH_RUNNER),
    DES_CRACK(FLAG_IMMORTAL),
    CAV_RACE_SNAIL(FLAG_PATH_RUNNER), // Unused. Appears to be fully present in game files, may work if spawned in-game.
    FALLING_LEAF(FLAG_PATH_RUNNER), // What is this?
    SWAYING_BRANCH(FLAG_IMMORTAL),
    BREAKING_BRANCH(FLAG_IMMORTAL),
    SQUIRREL(FLAG_PATH_RUNNER | FLAG_IMMORTAL),
    HEDGEHOG(FLAG_PATH_RUNNER),
    MOVING_PLATFORM(FLAG_PATH_RUNNER | FLAG_IMMORTAL),
    MOVING_TONGUEABLE(FLAG_PATH_RUNNER | FLAG_TONGUEABLE),
    FIREFLY(FLAG_PATH_RUNNER | FLAG_TONGUEABLE | FLAG_XZ_PARALLEL_TO_CAMERA),
    JUN_PLANT(FLAG_IMMORTAL),
    DES_ROLLING_ROCK(FLAG_PATH_RUNNER | FLAG_IMMORTAL),
    JUN_ROPE_BRIDGE(FLAG_IMMORTAL),
    JUN_HIPPO(FLAG_PATH_RUNNER),
    VOL_FALLING_PLATFORM(FLAG_IMMORTAL),
    DES_TUMBLE_WEED(FLAG_PATH_RUNNER),
    GEN_TOP_LEFT(FLAG_STATIC | FLAG_IMMORTAL), // "Fade Area" Appears to store the position of screen fading?
    GEN_BOTTOM_RIGHT(FLAG_STATIC | FLAG_IMMORTAL),
    GEN_GOLD_FROG(FLAG_STATIC | FLAG_IMMORTAL),
    SWP_RAT(FLAG_IMMORTAL),
    VOL_COLOUR_SWITCH(FLAG_IMMORTAL),
    JUN_OUTRO_DOOR(FLAG_IMMORTAL),
    JUN_STATUE(FLAG_IMMORTAL),
    JUN_PLINTH(FLAG_IMMORTAL),
    JUN_GOLD_FROG(FLAG_IMMORTAL),
    JUN_STONE_FROG(FLAG_IMMORTAL),
    JUN_OUTRO(FLAG_IMMORTAL), // What does this do?
    SWP_SLUG(FLAG_PATH_RUNNER | FLAG_IMMORTAL),
    JUN_BOUNCY_MUSHROOM(FLAG_STATIC), // While technically used, it serves no purpose in-game.
    SUB_LAWNMOWER(FLAG_PATH_RUNNER),
    NUCLEAR_BARREL(FLAG_PATH_RUNNER | FLAG_IMMORTAL);

    private int flags;
}
