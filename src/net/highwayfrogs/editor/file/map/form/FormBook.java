package net.highwayfrogs.editor.file.map.form;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.map.entity.EntityBook;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.map.entity.script.ScriptButterflyData;
import net.highwayfrogs.editor.file.map.entity.script.ScriptNoiseData;
import net.highwayfrogs.editor.file.map.entity.script.sky.ScriptBalloonData;
import net.highwayfrogs.editor.file.map.entity.script.sky.ScriptHeliumBalloon;
import net.highwayfrogs.editor.file.map.entity.script.swamp.ScriptBobbingWasteData;
import net.highwayfrogs.editor.file.map.entity.script.swamp.ScriptNuclearBarrelData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptHawkData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptHelicopterData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptMechanismData;
import net.highwayfrogs.editor.file.map.entity.script.volcano.ScriptPlatform2Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static net.highwayfrogs.editor.file.map.form.FormBookFlags.*;

/**
 * A registry of all forms.
 * TODO: Delete after commit.
 * Created by Kneesnap on 8/24/2018.
 */
@Getter
@AllArgsConstructor
public enum FormBook {
    GEN_CHECKPOINT_1(MAPTheme.GENERAL, 0, EntityBook.CHECKPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_CHECKPOINT_2(MAPTheme.GENERAL, 1, EntityBook.CHECKPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_CHECKPOINT_3(MAPTheme.GENERAL, 2, EntityBook.CHECKPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_CHECKPOINT_4(MAPTheme.GENERAL, 3, EntityBook.CHECKPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_CHECKPOINT_5(MAPTheme.GENERAL, 4, EntityBook.CHECKPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_BONUS_SCORE(MAPTheme.GENERAL, 5, EntityBook.STATIC, null),
    GEN_BONUS_TIME(MAPTheme.GENERAL, 6, EntityBook.STATIC, null),
    GEN_BONUS_LIFE(MAPTheme.GENERAL, 7, EntityBook.STATIC, null),
    GEN_BONUS_FROG(MAPTheme.GENERAL, CODE_NULL_OFFSET, EntityBook.STATIC, null),
    GEN_BONUS_FLY_GRE(MAPTheme.GENERAL, CODE_NULL_OFFSET, EntityBook.BONUS_FLY, FLAG_NO_MODEL | FLAG_NO_ROTATION_SNAPPING | FLAG_DONT_RESET_ON_DEATH, null), // Believe the FLAG_NO_ROTATION_SNAPPING was by accident, they used a flag from a completely different file, with a similar name to not resetting on checkpoint.
    GEN_LADYFROG(MAPTheme.GENERAL, CODE_NULL_OFFSET, EntityBook.STATIC, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_MULTIPOINT_1(MAPTheme.GENERAL, 8, EntityBook.MULTIPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_MULTIPOINT_2(MAPTheme.GENERAL, 9, EntityBook.MULTIPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_MULTIPOINT_3(MAPTheme.GENERAL, 10, EntityBook.MULTIPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_MULTIPOINT_4(MAPTheme.GENERAL, 11, EntityBook.MULTIPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_MULTIPOINT_5(MAPTheme.GENERAL, 12, EntityBook.MULTIPOINT, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_STAT_COL_BLOCK(MAPTheme.GENERAL, 13, EntityBook.STATIC, null),
    GEN_STAT_DEATH_BLOCK(MAPTheme.GENERAL, 14, EntityBook.STATIC, null),
    GEN_TOPLEFT(MAPTheme.GENERAL, 13, EntityBook.GEN_TOP_LEFT, null),
    GEN_BOTTOMRIGHT(MAPTheme.GENERAL, 13, EntityBook.GEN_BOTTOM_RIGHT, null),
    GEN_GOLD_FROG(MAPTheme.GENERAL, 15, EntityBook.GEN_GOLD_FROG, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    GEN_STAT_WATER_FALL_BLOCK(MAPTheme.GENERAL, 13, EntityBook.STATIC, null),
    GEN_STAT_FALL_BLOCK(MAPTheme.GENERAL, 13, EntityBook.STATIC, null),

    CAV_GLOWWORM(MAPTheme.CAVE, 0, EntityBook.STATIC, null),
    CAV_FIREFLY(MAPTheme.CAVE, 0, EntityBook.FIREFLY, FLAG_DONT_FADE_COLOR | FLAG_NO_MODEL, null),
    CAV_SPIDER(MAPTheme.CAVE, 0, EntityBook.MOVING, null),
    CAV_BAT(MAPTheme.CAVE, 1, EntityBook.MOVING, null),
    CAV_ROCKFALLFLOOR(MAPTheme.CAVE, 2, EntityBook.STATIC, null),
    CAV_ROCKBLOCK(MAPTheme.CAVE, 3, EntityBook.STATIC, null),
    CAV_ROPEBRIDGE(MAPTheme.CAVE, 4, EntityBook.STATIC, null),
    CAV_FROGGERLIGHT(MAPTheme.CAVE, 0, EntityBook.CAV_FROGGER_LIGHT, FLAG_NO_MODEL, null),
    CAV_STAT_COBWEB(MAPTheme.CAVE, 5, EntityBook.STATIC, null),
    CAV_VAMP_BAT(MAPTheme.CAVE, 6, EntityBook.MOVING, null),
    CAV_STAT_STONEBRIDGE(MAPTheme.CAVE, 7, EntityBook.STATIC, null),
    CAV_STAT_CRYSTALS(MAPTheme.CAVE, 8, EntityBook.STATIC, null),
    CAV_BAT_FLOCK(MAPTheme.CAVE, 9, EntityBook.STATIC, null),
    CAV_LAVADROP(MAPTheme.CAVE, 0, EntityBook.STATIC, null),
    CAV_SNAIL(MAPTheme.CAVE, 10, EntityBook.MOVING, null),
    CAV_SLIME(MAPTheme.CAVE, 11, EntityBook.STATIC, null),
    CAV_STAT_ROCKBLOCK2(MAPTheme.CAVE, 12, EntityBook.STATIC, null),
    CAV_STAT_WEBWALL(MAPTheme.CAVE, 13, EntityBook.STATIC, null),
    CAV_FAT_FIRE_FLY(MAPTheme.CAVE, EntityBook.CAV_FAT_FIRE_FLY, FLAG_NO_MODEL, null),
    CAV_RACESNAIL(MAPTheme.CAVE, 14, EntityBook.CAV_RACE_SNAIL, null),
    DES_VULTURE(MAPTheme.DESERT, 0, EntityBook.DES_VULTURE, null),
    DES_LIZARD(MAPTheme.DESERT, 1, EntityBook.MOVING, null),
    DES_STAT_BALLCACTUS(MAPTheme.DESERT, 2, EntityBook.STATIC, null),
    DES_STAT_CACTUS(MAPTheme.DESERT, 3, EntityBook.STATIC, null),
    DES_SNAKE(MAPTheme.DESERT, 4, EntityBook.DES_SNAKE, null),
    DES_TUMBLEWEED(MAPTheme.DESERT, 5, EntityBook.DES_TUMBLE_WEED, null),
    DES_FALLING_ROCK(MAPTheme.DESERT, 6, EntityBook.DES_FALLING_ROCK, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    DES_EARTHQUAKE(MAPTheme.DESERT, 0, EntityBook.DES_EARTH_QUAKE, null),
    DES_HOLE1(MAPTheme.DESERT, 7, EntityBook.STATIC, null),
    DES_HOLE2(MAPTheme.DESERT, 8, EntityBook.STATIC, null),
    DES_CRACK(MAPTheme.DESERT, 9, EntityBook.DES_CRACK, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    DES_BEETLE(MAPTheme.DESERT, 10, EntityBook.MOVING, null),
    DES_BISON(MAPTheme.DESERT, 11, EntityBook.MOVING, null),
    DES_THERMAL(MAPTheme.DESERT, 12, EntityBook.DES_THERMAL, null),
    DES_STAT_ROCKFORM(MAPTheme.DESERT, 13, EntityBook.STATIC, null),
    DES_STAT_ARCH(MAPTheme.DESERT, 14, EntityBook.STATIC, null),
    DES_STAT_ROCKS(MAPTheme.DESERT, 15, EntityBook.STATIC, null),
    DES_STAT_ROCKS2(MAPTheme.DESERT, 16, EntityBook.STATIC, null),
    DES_BISONCLOUD(MAPTheme.DESERT, 17, EntityBook.STATIC, null),
    DES_SALAMANDER(MAPTheme.DESERT, 18, EntityBook.STATIC, null),
    DES_STAT_CLIFFBRANCH(MAPTheme.DESERT, 19, EntityBook.STATIC, null),
    DES_STAT_COWSKULL(MAPTheme.DESERT, 20, EntityBook.STATIC, null),
    DES_STAT_BALLCACTUS2(MAPTheme.DESERT, 21, EntityBook.STATIC, null),
    DES_STAT_CACTUS2(MAPTheme.DESERT, 22, EntityBook.STATIC, null),
    DES_STAT_CLIFFBRANCH2(MAPTheme.DESERT, 23, EntityBook.STATIC, null),
    DES_STAT_BONES(MAPTheme.DESERT, 24, EntityBook.STATIC, null),
    DES_FALL_ROCKROLL(MAPTheme.DESERT, 25, EntityBook.STATIC, null),
    DES_BISONNOISE(MAPTheme.DESERT, 1, EntityBook.STATIC, FLAG_NO_MODEL, null),
    DES_BIRD1(MAPTheme.DESERT, 26, EntityBook.MOVING, null),
    DES_BIRD2(MAPTheme.DESERT, 27, EntityBook.MOVING, null),
    DES_STAT_CACTUS3(MAPTheme.DESERT, 28, EntityBook.STATIC, null),
    DES_STAT_BALLCACTUS3(MAPTheme.DESERT, 29, EntityBook.STATIC, null),
    DES_STAT_BALLCACTUS4(MAPTheme.DESERT, 30, EntityBook.STATIC, null),
    DES_STAT_CACTUS4(MAPTheme.DESERT, 31, EntityBook.STATIC, null),
    DES_LIZARD_NOISE(MAPTheme.DESERT, 1, EntityBook.STATIC, FLAG_NO_MODEL, null),
    DES_BUTTERFLY(MAPTheme.DESERT, 32, EntityBook.MOVING_TONGUEABLE, ScriptButterflyData::new),
    DES_CROCHEAD(MAPTheme.DESERT, 33, EntityBook.DES_CROC_HEAD, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    DES_ROLLINGROCK(MAPTheme.DESERT, 6, EntityBook.DES_ROLLING_ROCK, null),

    FOR_WOODPECKER(MAPTheme.FOREST, 0, EntityBook.STATIC, null),
    FOR_JAY(MAPTheme.FOREST, 1, EntityBook.MOVING, null),
    FOR_FALLINGLEAVES(MAPTheme.FOREST, 2, EntityBook.STATIC, null),
    FOR_SWAYINGBRANCH(MAPTheme.FOREST, 3, EntityBook.FOR_SWAYING_BRANCH, FLAG_NO_ROTATION_SNAPPING | FLAG_NO_ENTITY_ANGLE, null),
    FOR_SQUIRREL(MAPTheme.FOREST, 4, EntityBook.FOR_SQUIRREL, null),
    FOR_OWL(MAPTheme.FOREST, 5, EntityBook.MOVING, null),
    FOR_SWARM(MAPTheme.FOREST, 6, EntityBook.STATIC, null),
    FOR_HIVE(MAPTheme.FOREST, 7, EntityBook.FOR_HIVE, null),
    FOR_STAT_BRACHTFUNGI(MAPTheme.FOREST, 8, EntityBook.STATIC, null),
    FOR_STAT_TREESTUMP(MAPTheme.FOREST, 9, EntityBook.STATIC, null),
    FOR_STAT_BIGTREE(MAPTheme.FOREST, 10, EntityBook.STATIC, null),
    FOR_BREAKINGBRANCH(MAPTheme.FOREST, 11, EntityBook.FOR_BREAKING_BRANCH, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    FOR_STAT_SAFEBRANCH(MAPTheme.FOREST, 12, EntityBook.STATIC, null),
    FOR_STAT_TREESTUMP2(MAPTheme.FOREST, 13, EntityBook.STATIC, null),
    FOR_STAT_TREESTUMP3(MAPTheme.FOREST, 14, EntityBook.STATIC, null),
    FOR_STAT_BREAKINGBRANCH(MAPTheme.FOREST, 15, EntityBook.STATIC, null),
    FOR_STAT_BUSH(MAPTheme.FOREST, 16, EntityBook.STATIC, null),
    FOR_STAT_SMALLTREE(MAPTheme.FOREST, 17, EntityBook.STATIC, null),
    FOR_STAT_SAFEBRANCH2(MAPTheme.FOREST, 18, EntityBook.STATIC, null),
    FOR_STAT_TOADSTOOL(MAPTheme.FOREST, 19, EntityBook.STATIC, null),
    FOR_STAT_MUSHROOM(MAPTheme.FOREST, 20, EntityBook.STATIC, null),
    FOR_STAT_DEADBRANCH(MAPTheme.FOREST, 21, EntityBook.STATIC, null),
    FOR_STAT_FALLENTREE(MAPTheme.FOREST, 22, EntityBook.STATIC, null),
    FOR_STAT_SMALLTREE2(MAPTheme.FOREST, 23, EntityBook.STATIC, null),
    FOR_STAT_SMALLTREE3(MAPTheme.FOREST, 24, EntityBook.STATIC, null),
    FOR_STAT_SWAYINGBRANCH(MAPTheme.FOREST, 25, EntityBook.FOR_SWAYING_BRANCH, null),
    FOR_STAT_TREETOP(MAPTheme.FOREST, 26, EntityBook.STATIC, null),
    FOR_STAT_BRACHTFUNGI2(MAPTheme.FOREST, 27, EntityBook.STATIC, null),
    FOR_STAT_BRACHTFUNGI3(MAPTheme.FOREST, 28, EntityBook.STATIC, null),
    FOR_STAT_BRACHTFUNGI4(MAPTheme.FOREST, 29, EntityBook.STATIC, null),
    FOR_HEDGEHOG(MAPTheme.FOREST, 30, EntityBook.FOR_HEDGEHOG, null),
    FOR_SWAN(MAPTheme.FOREST, 31, EntityBook.MOVING, null),
    FOR_STAT_ORCHID(MAPTheme.FOREST, 32, EntityBook.STATIC, null),
    FOR_STAT_DAISY(MAPTheme.FOREST, 33, EntityBook.STATIC, null),
    FOR_LEAF(MAPTheme.FOREST, 34, EntityBook.MOVING, null),
    FOR_STAT_TREESTUMP1(MAPTheme.FOREST, 0, EntityBook.STATIC, null),
    FOR_RIVER_NOISE(MAPTheme.FOREST, 14, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),

    ARN_FALLING_TREE(MAPTheme.JUNGLE, 0, EntityBook.STATIC, null),
    ARN_SCORPION(MAPTheme.JUNGLE, 0, EntityBook.MOVING, null),
    ARN_STAT_BOULDER(MAPTheme.JUNGLE, 0, EntityBook.STATIC, null),
    ARN_STAT_DOG(MAPTheme.JUNGLE, 1, EntityBook.STATIC, null),
    ARN_STAT_DOG2(MAPTheme.JUNGLE, 2, EntityBook.STATIC, null),
    ARN_STAT_FALLTREESTUMP(MAPTheme.JUNGLE, 0, EntityBook.STATIC, null),
    ARN_STAT_FROG(MAPTheme.JUNGLE, 3, EntityBook.STATIC, null),
    ARN_STAT_ROCK(MAPTheme.JUNGLE, 0, EntityBook.STATIC, null),
    ARN_STAT_ROCKS(MAPTheme.JUNGLE, 0, EntityBook.STATIC, null),
    ARN_STAT_ROCKS2(MAPTheme.JUNGLE, 0, EntityBook.STATIC, null),
    ARN_STAT_STUMP(MAPTheme.JUNGLE, 0, EntityBook.STATIC, null),
    JUN_BIRD(MAPTheme.JUNGLE, 4, EntityBook.MOVING, null),
    JUN_BIRD1(MAPTheme.JUNGLE, 5, EntityBook.MOVING, null),
    JUN_BIRD2(MAPTheme.JUNGLE, 6, EntityBook.MOVING, null),
    JUN_BOAR(MAPTheme.JUNGLE, 7, EntityBook.MOVING, null),
    JUN_BUTTERFLY(MAPTheme.JUNGLE, 8, EntityBook.MOVING_TONGUEABLE, ScriptButterflyData::new),
    JUN_DRAGONFLY(MAPTheme.JUNGLE, 9, EntityBook.MOVING, null),
    JUN_EEL(MAPTheme.JUNGLE, 10, EntityBook.MOVING, null),
    JUN_FLAMINGO(MAPTheme.JUNGLE, 11, EntityBook.MOVING, null),
    JUN_HUMBIRD(MAPTheme.JUNGLE, 12, EntityBook.MOVING, null),
    JUN_MONKEY(MAPTheme.JUNGLE, 13, EntityBook.MOVING, null),
    JUN_PARROT(MAPTheme.JUNGLE, 14, EntityBook.MOVING, null),
    JUN_PARROT2(MAPTheme.JUNGLE, 15, EntityBook.MOVING, null),
    JUN_PARROT3(MAPTheme.JUNGLE, 16, EntityBook.MOVING, null),
    JUN_POCUPINE(MAPTheme.JUNGLE, 17, EntityBook.MOVING, null),
    JUN_PLANT(MAPTheme.JUNGLE, 18, EntityBook.JUN_PLANT, null),
    JUN_PIRANHA(MAPTheme.JUNGLE, 19, EntityBook.MOVING, null),
    JUN_RHINO(MAPTheme.JUNGLE, 20, EntityBook.MOVING, null),
    JUN_SCORPION(MAPTheme.JUNGLE, 0, EntityBook.MOVING, null),
    JUN_STAT_TREECOC2(MAPTheme.JUNGLE, 21, EntityBook.STATIC, null),
    JUN_STAT_TREE2(MAPTheme.JUNGLE, 22, EntityBook.STATIC, null),
    JUN_STAT_TREECOC(MAPTheme.JUNGLE, 23, EntityBook.STATIC, null),
    JUN_STAT_SUNFLOWER(MAPTheme.JUNGLE, 24, EntityBook.STATIC, null),
    JUN_STAT_TREE(MAPTheme.JUNGLE, 25, EntityBook.STATIC, null),
    JUN_STAT_TREEFAT2(MAPTheme.JUNGLE, 26, EntityBook.STATIC, null),
    JUN_STAT_TREEFAT(MAPTheme.JUNGLE, 27, EntityBook.STATIC, null),
    JUN_STAT_TREE4(MAPTheme.JUNGLE, 28, EntityBook.STATIC, null),
    JUN_STAT_TREE3(MAPTheme.JUNGLE, 29, EntityBook.STATIC, null),
    JUN_STAT_FLOWER(MAPTheme.JUNGLE, 30, EntityBook.STATIC, null),
    JUN_STAT_FLOWER2(MAPTheme.JUNGLE, 31, EntityBook.STATIC, null),
    JUN_STAT_FLOWER3(MAPTheme.JUNGLE, 32, EntityBook.STATIC, null),
    JUN_STAT_FLOWER4(MAPTheme.JUNGLE, 33, EntityBook.STATIC, null),
    JUN_STAT_ORCHID(MAPTheme.JUNGLE, 34, EntityBook.STATIC, null),
    JUN_TOUCAN(MAPTheme.JUNGLE, 35, EntityBook.MOVING, null),
    JUN_TURTLE(MAPTheme.JUNGLE, 36, EntityBook.MOVING, null),
    JUN_LOG(MAPTheme.JUNGLE, 37, EntityBook.MOVING, null),
    JUN_ROPEBRIDGE(MAPTheme.JUNGLE, 38, EntityBook.JUN_ROPE_BRIDGE, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH | FLAG_UNIT_FORM, null),
    JUN_CROCODILE(MAPTheme.JUNGLE, 39, EntityBook.MOVING, null),
    JUN_HIPPO(MAPTheme.JUNGLE, 40, EntityBook.JUN_HIPPO, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    JUN_CRACK(MAPTheme.JUNGLE, 41, EntityBook.DES_CRACK, null),
    JUN_BANANATREE(MAPTheme.JUNGLE, 42, EntityBook.STATIC, null),
    JUN_PINEAPPLETREE(MAPTheme.JUNGLE, 43, EntityBook.STATIC, null),
    JUN_OVERHANG(MAPTheme.JUNGLE, 44, EntityBook.STATIC, null),
    JUN_FLOATINGTREES(MAPTheme.JUNGLE, 45, EntityBook.MOVING, null),
    JUN_ROPEBRIDGE2(MAPTheme.JUNGLE, 46, EntityBook.JUN_ROPE_BRIDGE, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    JUN_STAT_PALM(MAPTheme.JUNGLE, 47, EntityBook.STATIC, null),
    JUN_BOUNCY_MUSH(MAPTheme.JUNGLE, 48, EntityBook.JUN_BOUNCY_MUSHROOM, null),
    JUN_WATER_NOISE(MAPTheme.JUNGLE, CODE_NULL_OFFSET, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),
    JUN_OUTRO(MAPTheme.JUNGLE, CODE_NULL_OFFSET, EntityBook.JUN_OUTRO, FLAG_NO_MODEL, null),
    JUN_OUTRO_DOOR(MAPTheme.JUNGLE, 49, EntityBook.JUN_OUTRO_DOOR, null),
    JUN_OUTRO_STATUE(MAPTheme.JUNGLE, CODE_NULL_OFFSET, EntityBook.JUN_STATUE, FLAG_NO_MODEL, null),
    JUN_OUTRO_PLINTH(MAPTheme.JUNGLE, 50, EntityBook.JUN_PLINTH, null),
    JUN_OUTRO_GOLD_DOOR(MAPTheme.JUNGLE, 49, EntityBook.JUN_OUTRO_DOOR, null),
    JUN_OUTRO_GOLD_FROG(MAPTheme.JUNGLE, 15, EntityBook.JUN_GOLD_FROG, null),
    JUN_STONE_FROG(MAPTheme.JUNGLE, 51, EntityBook.JUN_STONE_FROG, null),
    JUN_LOG3(MAPTheme.JUNGLE, 52, EntityBook.MOVING, null),

    ORG_BULL_DOZER(MAPTheme.ORIGINAL, 0, EntityBook.MOVING, null),
    ORG_CAR_BLUE(MAPTheme.ORIGINAL, 1, EntityBook.MOVING, null),
    ORG_CAR_PURPLE(MAPTheme.ORIGINAL, 2, EntityBook.MOVING, null),
    ORG_LOG_SMALL(MAPTheme.ORIGINAL, 3, EntityBook.MOVING, FLAG_DONT_CENTER_Z, null),
    ORG_LOG_MEDIUM(MAPTheme.ORIGINAL, 4, EntityBook.MOVING, FLAG_DONT_CENTER_Z, null),
    ORG_LOG_LARGE(MAPTheme.ORIGINAL, 5, EntityBook.MOVING, FLAG_DONT_CENTER_Z, null),
    ORG_SNAKE(MAPTheme.ORIGINAL, 6, EntityBook.MOVING, null),
    ORG_LORRY(MAPTheme.ORIGINAL, 7, EntityBook.MOVING, null),
    ORG_TRUCK_GREEN(MAPTheme.ORIGINAL, 8, EntityBook.MOVING, null),
    ORG_TRUCK_RED(MAPTheme.ORIGINAL, 9, EntityBook.MOVING, null),
    ORG_CROCODILE(MAPTheme.ORIGINAL, 10, EntityBook.MOVING, null),
    ORG_HOME_FROG(MAPTheme.ORIGINAL, 10, EntityBook.STATIC, null),
    ORG_FLY(MAPTheme.ORIGINAL, 12, EntityBook.ORG_FLY, FLAG_NO_MODEL, null),
    ORG_BEAVER(MAPTheme.ORIGINAL, 13, EntityBook.ORG_BEAVER, null),
    ORG_BABYFROG(MAPTheme.ORIGINAL, 14, EntityBook.ORG_BABY_FROG, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    ORG_CROCHEAD(MAPTheme.ORIGINAL, 15, EntityBook.ORG_CROC_HEAD, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    ORG_LOGSNAKE(MAPTheme.ORIGINAL, 6, EntityBook.ORG_LOG_SNAKE, null),
    ORG_ROADNOISE(MAPTheme.ORIGINAL, 0, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),
    ORG_RIVERNOISE(MAPTheme.ORIGINAL, 0, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),
    ORG_TURTLE(MAPTheme.ORIGINAL, 16, EntityBook.SUB_TURTLE, null),
    ORG_TURTLE2(MAPTheme.ORIGINAL, 17, EntityBook.SUB_TURTLE, null),
    ORG_TURTLE3(MAPTheme.ORIGINAL, 18, EntityBook.SUB_TURTLE, null),
    ORG_STAT_NETTLES(MAPTheme.ORIGINAL, 19, EntityBook.STATIC, null),
    ORG_CAR_ORANGE(MAPTheme.ORIGINAL, 20, EntityBook.MOVING, null),
    ORG_GOLD_FROG(MAPTheme.ORIGINAL, 15, EntityBook.ORG_BABY_FROG, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),

    ARN_STAT_DOG_REAL(MAPTheme.RUINED, 0, EntityBook.STATIC, null),
    ARN_STAT_DOG2_REAL(MAPTheme.RUINED, 1, EntityBook.STATIC, null),
    ARN_STAT_FROG_REAL(MAPTheme.RUINED, 2, EntityBook.STATIC, null),

    SWP_OIL_DRUM(MAPTheme.SWAMP, 0, EntityBook.MOVING, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SWP_BOX(MAPTheme.SWAMP, 1, EntityBook.STATIC, FLAG_THICK, null),
    SWP_RAT(MAPTheme.SWAMP, 2, EntityBook.SWP_RAT, null),
    SWP_STAT_MOUND(MAPTheme.SWAMP, 0, EntityBook.STATIC, FLAG_NO_MODEL, null),
    SWP_STAT_SUNKCAR(MAPTheme.SWAMP, 3, EntityBook.STATIC, null),
    SWP_NEWSPAPER(MAPTheme.SWAMP, 4, EntityBook.MOVING, FLAG_THICK | FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SWP_NEWSPAPER_TORN(MAPTheme.SWAMP, 5, EntityBook.STATIC, FLAG_THICK, null),
    SWP_STAT_PIPE(MAPTheme.SWAMP, 6, EntityBook.STATIC, null),
    SWP_STAT_FLUME(MAPTheme.SWAMP, 0, EntityBook.STATIC, FLAG_NO_MODEL, null),
    SWP_RACOON(MAPTheme.SWAMP, 7, EntityBook.MOVING, null),
    SWP_STAT_DEADTREE(MAPTheme.SWAMP, 8, EntityBook.STATIC, null),
    SWP_STAT_DEADTREE1(MAPTheme.SWAMP, 9, EntityBook.STATIC, null),
    SWP_STAT_LOG(MAPTheme.SWAMP, 10, EntityBook.STATIC, null),
    SWP_STAT_LITTER(MAPTheme.SWAMP, 11, EntityBook.STATIC, FLAG_THICK, null),
    SWP_STAT_LITTER2(MAPTheme.SWAMP, 12, EntityBook.STATIC, FLAG_THICK, null),
    SWP_PALLET(MAPTheme.SWAMP, 13, EntityBook.MOVING, FLAG_THICK, null),
    SWP_OIL(MAPTheme.SWAMP, 14, EntityBook.MOVING, null),
    SWP_WASTE_BARREL(MAPTheme.SWAMP, 15, EntityBook.MOVING, null),
    SWP_NUCLEAR_BARREL(MAPTheme.SWAMP, 16, EntityBook.NUCLEAR_BARREL, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, ScriptNuclearBarrelData::new),
    SWP_STAT_PIPE_STR(MAPTheme.SWAMP, 17, EntityBook.STATIC, null),
    SWP_ST_PIPE_BIG_STR(MAPTheme.SWAMP, 18, EntityBook.STATIC, null),
    SWP_STAT_PIPE_HOLE(MAPTheme.SWAMP, 19, EntityBook.STATIC, null),
    SWP_STAT_MARSH(MAPTheme.SWAMP, 20, EntityBook.STATIC, null),
    SWP_CRUSHER(MAPTheme.SWAMP, 21, EntityBook.SWP_CRUSHER, null),
    SWP_STAT_WEIR(MAPTheme.SWAMP, 22, EntityBook.STATIC, null),
    SWP_SQUIRT(MAPTheme.SWAMP, 23, EntityBook.SWP_SQUIRT, null),
    SWP_STAT_PIPE_CURVED(MAPTheme.SWAMP, 24, EntityBook.STATIC, null),
    SWP_ST_PIPE_BIG_CURVE(MAPTheme.SWAMP, 25, EntityBook.STATIC, null),
    SWP_STAT_PIPE_SMALL_STR(MAPTheme.SWAMP, 26, EntityBook.STATIC, null),
    SWP_STAT_PIPE_SMALL_CUR(MAPTheme.SWAMP, 27, EntityBook.STATIC, null),
    SWP_SLUG(MAPTheme.SWAMP, 28, EntityBook.SWP_SLUG, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SWP_MUTANT_FISH(MAPTheme.SWAMP, 29, EntityBook.MOVING, null),
    SWP_STAT_WASTE_BARREL(MAPTheme.SWAMP, 30, EntityBook.DYNAMIC, ScriptBobbingWasteData::new),
    SWP_PIPE_MESH(MAPTheme.SWAMP, 31, EntityBook.STATIC, null),
    SWP_STAT_FRIDGE(MAPTheme.SWAMP, 32, EntityBook.STATIC, null),
    SWP_STAT_TYRE(MAPTheme.SWAMP, 33, EntityBook.STATIC, null),
    SWP_CHEMICAL_BARREL(MAPTheme.SWAMP, 30, EntityBook.MOVING, null),
    SWP_CRUSHER2(MAPTheme.SWAMP, 34, EntityBook.SWP_CRUSHER, null),
    SWP_PRESS(MAPTheme.SWAMP, 35, EntityBook.SWP_PRESS, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SWP_BIRD1(MAPTheme.SWAMP, 36, EntityBook.MOVING, null),
    SWP_WATER_NOISE(MAPTheme.SWAMP, 0, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),
    SWP_WEIR_NOISE(MAPTheme.SWAMP, 0, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new), // These last two labels may not be accurate.
    SWP_RECYCLE_BIN_NOISE(MAPTheme.SWAMP, 0, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),

    SKY_JET1(MAPTheme.SKY, 0, EntityBook.MOVING, null),
    SKY_JET3(MAPTheme.SKY, 1, EntityBook.MOVING, null),
    SKY_BIPLANE1(MAPTheme.SKY, 2, EntityBook.MOVING, null),
    SKY_BIPLANE2(MAPTheme.SKY, 3, EntityBook.MOVING, null),
    SKY_HELICOPTER(MAPTheme.SKY, 4, EntityBook.DYNAMIC, ScriptHelicopterData::new),
    SKY_BIRD1(MAPTheme.SKY, 5, EntityBook.MOVING, null),
    SKY_BIRD2(MAPTheme.SKY, 6, EntityBook.MOVING, null),
    SKY_BIRD3(MAPTheme.SKY, 7, EntityBook.MOVING, null),
    SKY_BIRD4(MAPTheme.SKY, 8, EntityBook.MOVING, null),
    SKY_LITTLE_BIRD(MAPTheme.SKY, 9, EntityBook.MOVING, null),
    SKY_POPPING_BIRD(MAPTheme.SKY, 10, EntityBook.MOVING, null),
    SKY_RUBBER_BALLOON1(MAPTheme.SKY, 11, EntityBook.DYNAMIC, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, ScriptBalloonData::new),
    SKY_RUBBER_BALLOON2(MAPTheme.SKY, 12, EntityBook.DYNAMIC, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, ScriptBalloonData::new),
    SKY_HELIUM_BALLOON3(MAPTheme.SKY, 13, EntityBook.DYNAMIC, FLAG_NO_ROTATION_SNAPPING | FLAG_NO_ENTITY_ANGLE | FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, ScriptHeliumBalloon::new),
    SKY_BIPLANE_BANNER1(MAPTheme.SKY, 14, EntityBook.STATIC, null),
    SKY_CLOUD_PLATFORM1(MAPTheme.SKY, 0, EntityBook.STATIC, FLAG_NO_MODEL, null),
    SKY_CLOUD1(MAPTheme.SKY, 15, EntityBook.STATIC, null),
    SKY_CLOUD2(MAPTheme.SKY, 16, EntityBook.STATIC, null),
    SKY_CLOUD3(MAPTheme.SKY, 17, EntityBook.STATIC, null),
    SKY_HAWK(MAPTheme.SKY, 18, EntityBook.STATIC, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, ScriptHawkData::new),
    SKY_BIRDHAWK(MAPTheme.SKY, 19, EntityBook.STATIC, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SKY_TORNADO_OBJECT(MAPTheme.SKY, 20, EntityBook.MOVING, null),
    SKY_FLOCKING_BIRD(MAPTheme.SKY, 21, EntityBook.MOVING, null),
    SKY_BIRD_SMALL(MAPTheme.SKY, 22, EntityBook.MOVING, null),
    SKY_CLD_PATCH(MAPTheme.SKY, 23, EntityBook.STATIC, null),
    SKY_CLOUDPLATFORM(MAPTheme.SKY, 15, EntityBook.MOVING, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SKY_SQUADRON(MAPTheme.SKY, 24, EntityBook.MOVING_PLATFORM, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SKY_FROGGERS_MAGICAL_POPPING_BALLOON(MAPTheme.SKY, 25, EntityBook.TRIGGER, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),

    SUB_LOG(MAPTheme.SUBURBIA, 0, EntityBook.MOVING, null),
    SUB_TURTLE(MAPTheme.SUBURBIA, 1, EntityBook.MOVING, null),
    SUB_STAT_BIG_FENCE(MAPTheme.SUBURBIA, 2, EntityBook.STATIC, null),
    SUB_STAT_SMALL_FENCE(MAPTheme.SUBURBIA, 3, EntityBook.STATIC, null),
    SUB_STAT_SMALL_FLOWERS(MAPTheme.SUBURBIA, 4, EntityBook.STATIC, null),
    SUB_STAT_BIG_FLOWERS(MAPTheme.SUBURBIA, 5, EntityBook.STATIC, null),
    SUB_STAT_SMALL_JETTY(MAPTheme.SUBURBIA, 6, EntityBook.STATIC, null),
    SUB_STAT_DOG_KENNEL(MAPTheme.SUBURBIA, 7, EntityBook.STATIC, null),
    SUB_STAT_TUNNEL(MAPTheme.SUBURBIA, 8, EntityBook.STATIC, null),
    SUB_STAT_LILLY(MAPTheme.SUBURBIA, 9, EntityBook.STATIC, null),
    SUB_LILLYPAD(MAPTheme.SUBURBIA, 10, EntityBook.STATIC, FLAG_NO_ROTATION_SNAPPING | FLAG_NO_ENTITY_ANGLE | FLAG_THICK, null),
    SUB_HEDGEHOG(MAPTheme.SUBURBIA, 11, EntityBook.MOVING, null),
    SUB_STAT_START_JETTY(MAPTheme.SUBURBIA, 10, EntityBook.STATIC, null),
    SUB_STAT_FENCE_POST(MAPTheme.SUBURBIA, 12, EntityBook.STATIC, null),
    SUB_TRUCK(MAPTheme.SUBURBIA, 13, EntityBook.MOVING, null),
    SUB_CAR(MAPTheme.SUBURBIA, 14, EntityBook.MOVING, null),
    SUB_LORRY(MAPTheme.SUBURBIA, 15, EntityBook.MOVING, null),
    SUB_PEDDLEBOAT(MAPTheme.SUBURBIA, 16, EntityBook.MOVING, null),
    SUB_SWAN(MAPTheme.SUBURBIA, 17, EntityBook.MOVING, null),
    SUB_LAWN_MOWER(MAPTheme.SUBURBIA, 18, EntityBook.SUB_LAWNMOWER, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SUB_DOG(MAPTheme.SUBURBIA, 19, EntityBook.SUB_DOG, null),
    SUB_STAT_DANDYLION(MAPTheme.SUBURBIA, 20, EntityBook.STATIC, null),
    SUB_STAT_STONE(MAPTheme.SUBURBIA, 21, EntityBook.STATIC, null),
    SUB_STAT_ROCKS(MAPTheme.SUBURBIA, 22, EntityBook.STATIC, null),
    SUB_STAT_STOPSIGN(MAPTheme.SUBURBIA, 23, EntityBook.STATIC, null),
    SUB_STAT_BULLRUSH(MAPTheme.SUBURBIA, 24, EntityBook.STATIC, null),
    SUB_STAT_TLIGHT(MAPTheme.SUBURBIA, 25, EntityBook.STATIC, null),
    SUB_STAT_GRASS(MAPTheme.SUBURBIA, 26, EntityBook.STATIC, null),
    SUB_FISH(MAPTheme.SUBURBIA, 27, EntityBook.MOVING, null),
    SUB_FISH3(MAPTheme.SUBURBIA, 28, EntityBook.MOVING, null),
    SUB_BUTTERFLY(MAPTheme.SUBURBIA, 29, EntityBook.MOVING_TONGUEABLE, ScriptButterflyData::new),
    SUB_LAWNMOWERNOISE(MAPTheme.SUBURBIA, 30, EntityBook.DYNAMIC, FLAG_NO_MODEL, null),
    SUB_BUTTERFLY2(MAPTheme.SUBURBIA, 30, EntityBook.MOVING_TONGUEABLE, ScriptButterflyData::new),
    SUB_BUTTERFLY3(MAPTheme.SUBURBIA, 31, EntityBook.MOVING_TONGUEABLE, ScriptButterflyData::new),
    SUB_STAT_BULLRUSH2(MAPTheme.SUBURBIA, 32, EntityBook.STATIC, null),
    SUB_STAT_DAISY(MAPTheme.SUBURBIA, 33, EntityBook.STATIC, null),
    SUB_STAT_WEED(MAPTheme.SUBURBIA, 34, EntityBook.STATIC, null),
    SUB_STAT_WEED2(MAPTheme.SUBURBIA, 35, EntityBook.STATIC, null),
    SUB_STAT_WEED3(MAPTheme.SUBURBIA, 36, EntityBook.STATIC, null),
    SUB_STAT_TREEFAT(MAPTheme.SUBURBIA, 37, EntityBook.STATIC, null),
    SUB_STAT_TREEFAT2(MAPTheme.SUBURBIA, 38, EntityBook.STATIC, null),
    SUB_STAT_ORCHID(MAPTheme.SUBURBIA, 39, EntityBook.STATIC, null),
    SUB_STAT_TREE(MAPTheme.SUBURBIA, 40, EntityBook.STATIC, null),
    SUB_STAT_TREE2(MAPTheme.SUBURBIA, 41, EntityBook.STATIC, null),
    SUB_STAT_TREE3(MAPTheme.SUBURBIA, 42, EntityBook.STATIC, null),
    SUB_STAT_TREE4(MAPTheme.SUBURBIA, 43, EntityBook.STATIC, null),
    SUB_STAT_SHED(MAPTheme.SUBURBIA, 44, EntityBook.STATIC, null),
    SUB_BIRD4(MAPTheme.SUBURBIA, 45, EntityBook.MOVING, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SUB_SNAKE(MAPTheme.SUBURBIA, 46, EntityBook.MOVING, null),
    SUB_CROCODILE(MAPTheme.SUBURBIA, 47, EntityBook.MOVING, null),
    SUB_SMALL_BIRD(MAPTheme.SUBURBIA, 48, EntityBook.MOVING, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SUB_LOG3(MAPTheme.SUBURBIA, 49, EntityBook.MOVING, null),
    SUB_CLOUD_PLATFORM(MAPTheme.SUBURBIA, 50, EntityBook.MOVING, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    SUB_ROAD_NOISE(MAPTheme.SUBURBIA, 15, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),
    SUB_WATER_NOISE(MAPTheme.SUBURBIA, 23, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),
    SUB_CAR_BLUE(MAPTheme.SUBURBIA, 51, EntityBook.MOVING, null),
    SUB_CAR_PURPLE(MAPTheme.SUBURBIA, 52, EntityBook.MOVING, null),
    SUB_CAR_BLUE_ALTERNATE(MAPTheme.SUBURBIA, 53, EntityBook.MOVING, null),

    VOL_BURNING_LOG(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_TREETOPS(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_FIREBALLS(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_SPLASH(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_SPURT_PLATFORMS(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_BLACK_LAVA(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_ASH_GYSER(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_CRACK(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_BUBBLEUP(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_TOPS_EXPLOSIONS(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_TREE_FALL_BURN(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_TRAPPED_ANIMAL1(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_TRAPPED_ANIMAL2(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_DEBRIS1(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_DEBRIS2(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_STAT_TREESTUMP(MAPTheme.VOLCANO, 0, EntityBook.STATIC, null),
    VOL_STAT_ROCKS(MAPTheme.VOLCANO, 1, EntityBook.STATIC, null),
    VOL_STAT_ROCKS2(MAPTheme.VOLCANO, 2, EntityBook.STATIC, null),
    VOL_SWITCH(MAPTheme.VOLCANO, 3, EntityBook.TRIGGER, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    VOL_PLATFORM1(MAPTheme.VOLCANO, 4, EntityBook.MOVING, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    VOL_PLATFORM2(MAPTheme.VOLCANO, 5, EntityBook.MOVING_PLATFORM, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, ScriptPlatform2Data::new),
    VOL_FALLING_PLATFORM(MAPTheme.VOLCANO, 6, EntityBook.VOL_FALLING_PLATFORM, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH | FLAG_THICK, null),
    VOL_MECHANISM(MAPTheme.VOLCANO, 7, EntityBook.DYNAMIC, ScriptMechanismData::new),
    VOL_FURNACE_PLATFORM(MAPTheme.VOLCANO, 8, EntityBook.DYNAMIC, ScriptMechanismData::new),
    VOL_LAVA_SPRAY(MAPTheme.VOLCANO, 9, EntityBook.MOVING, null),
    VOL_SPINNER(MAPTheme.VOLCANO, 10, EntityBook.MOVING, null),
    VOL_COLOR_TRIGGER(MAPTheme.VOLCANO, 3, EntityBook.VOL_COLOUR_SWITCH, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    VOL_COG_NOISE(MAPTheme.VOLCANO, 11, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new),
    VOL_PLATFORM3(MAPTheme.VOLCANO, 12, EntityBook.MOVING_PLATFORM, FLAG_DONT_RESET_ON_CHECKPOINT | FLAG_DONT_RESET_ON_DEATH, null),
    VOL_LAVA_NOISE(MAPTheme.VOLCANO, 10, EntityBook.STATIC, FLAG_NO_MODEL, ScriptNoiseData::new);

    private MAPTheme theme;
    private int id;
    private EntityBook entity;
    private int flags;
    private Supplier<EntityScriptData> scriptDataMaker;

    private static final int FLAG_GENERAL = 0x8000; // or Constants.BIT_FLAG_15
    private static final Map<MAPTheme, List<FormBook>> themeFormsBooks = new HashMap<>();

    FormBook(MAPTheme theme, EntityBook entity, Supplier<EntityScriptData> maker) {
        this(theme, 0, entity, maker);
    }

    FormBook(MAPTheme theme, EntityBook entity, int flags, Supplier<EntityScriptData> maker) {
        this(theme, 0, entity, flags, maker);
    }

    FormBook(MAPTheme theme, int id, EntityBook entity, Supplier<EntityScriptData> maker) {
        this(theme, id, entity, 0, maker);
    }

    /**
     * Gets the MOF id.
     * @return mofId
     */
    public int getId() {
        if (this.id == CODE_NULL_OFFSET)
            return 0;
        boolean useGeneral = theme == MAPTheme.GENERAL || (this == FOR_RIVER_NOISE || this == ORG_GOLD_FROG || this == JUN_OUTRO_GOLD_FROG);
        MAPTheme usedTheme = useGeneral ? MAPTheme.GENERAL : getTheme();
        return this.id + usedTheme.getFormOffset();
    }

    /**
     * Gets the id used in the files.
     * @return rawId
     */
    public int getRawId() {
        int rawId = getBooks(getTheme()).indexOf(this);
        if (getTheme() == MAPTheme.GENERAL)
            rawId |= FLAG_GENERAL;
        return rawId;
    }

    /**
     * Get all the FormBooks for a specific theme.
     * @param theme The theme to get form books for.
     * @return formBooks
     */
    public static List<FormBook> getBooks(MAPTheme theme) {
        if (!themeFormsBooks.containsKey(theme)) {
            List<FormBook> books = new ArrayList<>();
            for (FormBook book : values())
                if (book.getTheme() == theme)
                    books.add(book);
            themeFormsBooks.put(theme, books);
        }

        return themeFormsBooks.get(theme);
    }

    /**
     * Get the form book for the given formBookId and MapTheme.
     * @param mapTheme   The map theme the form belongs to.
     * @param formBookId The form id in question. Normally passed to ENTITY_GET_FORM_BOOK as en_form_book_id.
     * @return formBook
     */
    public static FormBook getFormBook(MAPTheme mapTheme, int formBookId) {
        if ((formBookId & FLAG_GENERAL) == FLAG_GENERAL)
            mapTheme = MAPTheme.GENERAL;
        return getBooks(mapTheme).get(formBookId & (FLAG_GENERAL - 1));
    }
}
