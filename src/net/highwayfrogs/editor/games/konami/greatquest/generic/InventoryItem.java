package net.highwayfrogs.editor.games.konami.greatquest.generic;

/**
 * Represents the '_InventoryItems' enum.
 * Created by Kneesnap on 10/30/2024.
 */
public enum InventoryItem {
    NONE, // 00 / 00
    STONE_FIRE, // 01 / 01
    STONE_ICE, // 02 / 02
    STONE_SPEED, // 03 / 03
    STONE_SHRINK, // 04 / 04
    STONE_LIGHTNING, // 05 / 05
    COIN_G, // 06 / 06
    COIN_S, // 07 / 07
    COIN_B, // 08 / 08
    GEM_D, // 09 / 09
    GEM_S, // 10 / 0A
    GEM_R, // 11 / 0B
    GEM_A, // 12 / 0C
    DOOR_KEY, // 13 / 0D
    CHEST_KEY, // 14 / 0E
    BONE, // 15 / 0F
    CHECKPOINT, // 16 / 10
    SEED, // 17 / 11
    EXTRA_BIN_19, // 18 / 12
    EXTRA_BIN_20, // 19 / 13

    // Start of bitwise items.
    MAP_00, // 20 / 14
    MAP_01, // 21 / 15
    MAP_02, // 22 / 16
    MAP_03, // 23 / 17
    MAP_04, // 24 / 18
    MAP_05, // 25 / 19
    MAP_06, // 26 / 1A
    MAP_07, // 27 / 1B
    MAP_08, // 28 / 1C
    MAP_09, // 29 / 1D
    MAP_10, // 30 / 1E
    MAP_11, // 31 / 1F
    MAP_12, // 32 / 20
    MAP_13, // 33 / 21
    MAP_14, // 34 / 22
    MAP_15, // 35 / 23
    MAP_16, // 36 / 24
    MAP_17, // 37 / 25
    MAP_18, // 38 / 26
    MAP_19, // 39 / 27
    MAP_20, // 40 / 28
    MAP_21, // 41 / 29
    MAP_22, // 42 / 2A
    MAP_23, // 43 / 2B
    HONEY_POT, // 44 / 2C
    ENGINE_ROOM_KEY, // 45 / 2D
    MAYOR_HOUSE_KEY, // 46 / 2E
    CLOVER_GATE_KEY, // 47 / 2F
    CLOVER, // 48 / 30
    FAKE_CLOVER, // 49 / 31
    FAIRY_TOWN_KEY_1, // 50 / 32
    FAIRY_TOWN_KEY_2, // 51 / 33
    FAIRY_TOWN_KEY_3, // 52 / 34
    TREE_KEY, // 53 / 35
    ENGINE_FUEL, // 54 / 36
    SHRUNK_BONE_CRUSHER, // 55 / 37
    ENGINE_KEY, // 56 / 38
    STATUE, // 57 / 39
    SQUARE_ARTIFACT, // 58 / 3A
    CIRCLE_ARTIFACT, // 59 / 3B
    TRIANGLE_ARTIFACT, // 60 / 3C
    CROWN, // 61 / 3D
    GRIM_BITE, // 62 / 3E
    RUBY_SHARD, // 63 / 3F
    RUBY_SPHERE, // 64 / 40
    RUBY_TEARDROP; // 65 / 41
    
    public static final InventoryItem MAP_LAST = MAP_23;
}
