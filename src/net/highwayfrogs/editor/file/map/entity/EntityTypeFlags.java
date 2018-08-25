package net.highwayfrogs.editor.file.map.entity;

/**
 * Holds EntityTypeFlags. Must be seperate from the EntityBook enum because otherwise they can't be used in enum constructors.
 * Created by Kneesnap on 8/24/2018.
 */
public class EntityTypeFlags {
    public static final int FLAG_PATH_RUNNER = 1; // Creates a path runner when the map is loaded.
    public static final int FLAG_IMMORTAL = 2;    // Entity that doesn't die when offscreen.
    public static final int FLAG_USE_SCRIPT = 4; // Use a script file supplied by library to control entity. Feature was never implemented in code.
    public static final int FLAG_STATIC = 8; // Never unlink from map group list. Presumably this is for entities that do not move?
    public static final int FLAG_TONGUEABLE = 16; // Can be targeted with tongue.
    public static final int FLAG_NO_ALIGN = 32; // Frog doesn't need to align to the entity when landing. Feature was never implemented in the code
    public static final int FLAG_XZ_PARALLEL_TO_CAMERA = 64; // For 3D sprites, create matrix XZ parallel to camera local XY. I believe this makes it so 2D objects don't look flat.
}
