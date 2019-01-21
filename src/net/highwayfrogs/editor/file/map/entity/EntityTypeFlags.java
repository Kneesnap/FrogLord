package net.highwayfrogs.editor.file.map.entity;

import net.highwayfrogs.editor.Constants;

/**
 * Holds EntityTypeFlags. Must be seperate from the EntityBook enum because otherwise they can't be used in enum constructors.
 * Created by Kneesnap on 8/24/2018.
 */
public class EntityTypeFlags {
    public static final int FLAG_PATH_RUNNER = Constants.BIT_FLAG_0; // Creates a path runner when the map is loaded.
    public static final int FLAG_IMMORTAL = Constants.BIT_FLAG_1;    // Entity that doesn't die when offscreen.
    public static final int FLAG_USE_SCRIPT = Constants.BIT_FLAG_2; // Use a script file supplied by library to control entity. Feature was never implemented in code.
    public static final int FLAG_STATIC = Constants.BIT_FLAG_3; // Never unlink from map group list. Presumably this is for entities that do not move?
    public static final int FLAG_TONGUEABLE = Constants.BIT_FLAG_4; // Can be targeted with tongue.
    public static final int FLAG_NO_ALIGN = Constants.BIT_FLAG_5; // Frog doesn't need to align to the entity when landing. Feature was never implemented in the code
    public static final int FLAG_XZ_PARALLEL_TO_CAMERA = Constants.BIT_FLAG_6; // For 3D sprites, create matrix XZ parallel to camera local XY. I believe this makes it so 2D objects don't look flat.
}
