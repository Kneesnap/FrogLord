package net.highwayfrogs.editor.file.map.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.cave.EntityFatFireFly;
import net.highwayfrogs.editor.file.map.entity.data.cave.EntityFrogLight;
import net.highwayfrogs.editor.file.map.entity.data.cave.EntityRaceSnail;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityCrack;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityCrocodileHead;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityFallingRock;
import net.highwayfrogs.editor.file.map.entity.data.desert.EntityThermal;
import net.highwayfrogs.editor.file.map.entity.data.forest.*;
import net.highwayfrogs.editor.file.map.entity.data.general.BonusFlyEntity;
import net.highwayfrogs.editor.file.map.entity.data.general.CheckpointEntity;
import net.highwayfrogs.editor.file.map.entity.data.general.TriggerEntity;
import net.highwayfrogs.editor.file.map.entity.data.jungle.EntityEvilPlant;
import net.highwayfrogs.editor.file.map.entity.data.jungle.EntityOutroPlinth;
import net.highwayfrogs.editor.file.map.entity.data.jungle.EntityRopeBridge;
import net.highwayfrogs.editor.file.map.entity.data.retro.EntityBabyFrog;
import net.highwayfrogs.editor.file.map.entity.data.retro.EntityBeaver;
import net.highwayfrogs.editor.file.map.entity.data.retro.EntitySnake;
import net.highwayfrogs.editor.file.map.entity.data.suburbia.EntityDog;
import net.highwayfrogs.editor.file.map.entity.data.suburbia.EntityTurtle;
import net.highwayfrogs.editor.file.map.entity.data.swamp.*;
import net.highwayfrogs.editor.file.map.entity.data.volcano.EntityColorTrigger;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;

import java.util.function.Supplier;

import static net.highwayfrogs.editor.file.map.entity.EntityTypeFlags.*;

/**
 * A registry of all entity types. Akin to the form book.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@AllArgsConstructor
public enum EntityBook {
    STATIC(FLAG_STATIC, PSXMatrix::new), // Create a MOF which does not move or is animated.
    MOVING(FLAG_PATH_RUNNER, PathInfo::new), // Creates a MOF which moves.
    CHECKPOINT(FLAG_STATIC, CheckpointEntity::new), // Baby Frogs, Multiplayer Flags.
    DES_FALLING_ROCK(FLAG_IMMORTAL, EntityFallingRock::new),
    DES_EARTHQUAKE(FLAG_IMMORTAL, null), // Cut entity.
    DES_THERMAL(FLAG_PATH_RUNNER, EntityThermal::new), // Cut entity.
    DYNAMIC(FLAG_IMMORTAL, PSXMatrix::new), // A MOF which moves around the world but is not path-based.
    CAV_FROGGER_LIGHT(FLAG_IMMORTAL, EntityFrogLight::new),
    ORG_LOG_SNAKE(FLAG_IMMORTAL, EntitySnake::new),
    BONUS_FLY(FLAG_STATIC | FLAG_TONGUEABLE, BonusFlyEntity::new),
    SUB_TURTLE(FLAG_PATH_RUNNER, EntityTurtle::new),
    SWP_SQUIRT(FLAG_IMMORTAL, EntitySquirt::new), // What is a squirt?
    SWP_CRUSHER(FLAG_IMMORTAL, EntityCrusher::new),
    TRIGGER(FLAG_IMMORTAL, TriggerEntity::new), // Unsure entirely what this is. Fingers crossed it's a generic trigger that can be used in any level to do things, this would be great for modding.
    ORG_BABY_FROG(FLAG_IMMORTAL, EntityBabyFrog::new),
    DES_SNAKE(FLAG_PATH_RUNNER, PathInfo::new), // I think
    ORG_BEAVER(FLAG_PATH_RUNNER | FLAG_IMMORTAL, EntityBeaver::new),
    DES_VULTURE(FLAG_PATH_RUNNER, PathInfo::new), // I think
    ORG_FLY(FLAG_IMMORTAL | FLAG_TONGUEABLE, PSXMatrix::new), // This one is rather odd. The code suggests it uses a struct called ORG_BONUS_FLY, identical to GEN_BONUS_FLY. However, neither the demo files nor the retail files seem to have this.
    ORG_CROC_HEAD(FLAG_IMMORTAL, PSXMatrix::new), // Appears to have used the struct ORG_CROC_HEAD, but in the demo + retail it does not have a struct...
    FOR_HIVE(FLAG_IMMORTAL, BeeHiveEntity::new),
    SWP_PRESS(FLAG_IMMORTAL, EntityPress::new), // What's this?
    CAV_FAT_FIRE_FLY(FLAG_STATIC | FLAG_TONGUEABLE, EntityFatFireFly::new),
    DES_CROC_HEAD(FLAG_IMMORTAL, EntityCrocodileHead::new),
    MULTIPOINT(FLAG_STATIC, CheckpointEntity::new), // Multiplayer flag checkpoint.
    SUB_DOG(FLAG_PATH_RUNNER, EntityDog::new),
    DES_CRACK(FLAG_IMMORTAL, EntityCrack::new),
    CAV_RACE_SNAIL(FLAG_PATH_RUNNER, EntityRaceSnail::new), // Unused. Appears to be fully present in game files, may work if spawned in-game.
    FALLING_LEAF(FLAG_PATH_RUNNER, FallingLeafEntity::new), // What is this?
    SWAYING_BRANCH(FLAG_IMMORTAL, SwayingBranchEntity::new),
    BREAKING_BRANCH(FLAG_IMMORTAL, BreakingBranchEntity::new),
    SQUIRREL(FLAG_PATH_RUNNER | FLAG_IMMORTAL, EntitySquirrel::new),
    HEDGEHOG(FLAG_PATH_RUNNER, EntityHedgehog::new),
    MOVING_PLATFORM(FLAG_PATH_RUNNER | FLAG_IMMORTAL, PathInfo::new), // I think
    MOVING_TONGUEABLE(FLAG_PATH_RUNNER | FLAG_TONGUEABLE, PathInfo::new), // I think
    FIREFLY(FLAG_PATH_RUNNER | FLAG_TONGUEABLE | FLAG_XZ_PARALLEL_TO_CAMERA, PSXMatrix::new), // I think.
    JUN_PLANT(FLAG_IMMORTAL, EntityEvilPlant::new),
    DES_ROLLING_ROCK(FLAG_PATH_RUNNER | FLAG_IMMORTAL, PathInfo::new), // I think
    JUN_ROPE_BRIDGE(FLAG_IMMORTAL, EntityRopeBridge::new),
    JUN_HIPPO(FLAG_PATH_RUNNER, PathInfo::new), // I think
    VOL_FALLING_PLATFORM(FLAG_IMMORTAL, PSXMatrix::new), // I think
    DES_TUMBLE_WEED(FLAG_PATH_RUNNER, PathInfo::new), // I think
    GEN_TOP_LEFT(FLAG_STATIC | FLAG_IMMORTAL, PSXMatrix::new), // "Fade Area" Appears to store the position of screen fading? I think
    GEN_BOTTOM_RIGHT(FLAG_STATIC | FLAG_IMMORTAL, PSXMatrix::new), // I think
    GEN_GOLD_FROG(FLAG_STATIC | FLAG_IMMORTAL, PSXMatrix::new),
    SWP_RAT(FLAG_IMMORTAL, EntityRat::new),
    VOL_COLOUR_SWITCH(FLAG_IMMORTAL, EntityColorTrigger::new),
    JUN_OUTRO_DOOR(FLAG_IMMORTAL, PSXMatrix::new), // I think
    JUN_STATUE(FLAG_IMMORTAL, PSXMatrix::new), // I think
    JUN_PLINTH(FLAG_IMMORTAL, EntityOutroPlinth::new),
    JUN_GOLD_FROG(FLAG_IMMORTAL, PSXMatrix::new), // I think
    JUN_STONE_FROG(FLAG_IMMORTAL, PSXMatrix::new), // I think
    JUN_OUTRO(FLAG_IMMORTAL, EntityOutroPlinth::new), // What does this do?
    SWP_SLUG(FLAG_PATH_RUNNER | FLAG_IMMORTAL, EntitySlug::new),
    JUN_BOUNCY_MUSHROOM(FLAG_STATIC, PSXMatrix::new), // While technically used, it serves no purpose in-game. I think.
    SUB_LAWNMOWER(FLAG_PATH_RUNNER, PathInfo::new), // I think
    NUCLEAR_BARREL(FLAG_PATH_RUNNER | FLAG_IMMORTAL, PathInfo::new); // I think

    private int flags;
    private Supplier<GameObject> scriptDataMaker;
}
