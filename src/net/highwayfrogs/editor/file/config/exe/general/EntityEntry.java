package net.highwayfrogs.editor.file.config.exe.general;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * An entity book entry.
 * Created by Kneesnap on 3/4/2019.
 */
public class EntityEntry extends GameObject {
    private long createFunction;
    private long updateFunction;
    private long killFunction;
    private int flags;
    private int runtimeSize;

    public static final int FLAG_PATH_RUNNER = Constants.BIT_FLAG_0; // Creates a path runner when the map is loaded.
    public static final int FLAG_IMMORTAL = Constants.BIT_FLAG_1;    // Entity that doesn't die when offscreen.
    public static final int FLAG_USE_SCRIPT = Constants.BIT_FLAG_2; // Use a script file supplied by library to control entity. Feature was never implemented in code.
    public static final int FLAG_STATIC = Constants.BIT_FLAG_3; // Never unlink from map group list. Presumably this is for entities that do not move?
    public static final int FLAG_TONGUEABLE = Constants.BIT_FLAG_4; // Can be targeted with tongue.
    public static final int FLAG_NO_ALIGN = Constants.BIT_FLAG_5; // Frog doesn't need to align to the entity when landing. Feature was never implemented in the code
    public static final int FLAG_XZ_PARALLEL_TO_CAMERA = Constants.BIT_FLAG_6; // For 3D sprites, create matrix XZ parallel to camera local XY. I believe this makes it so 2D objects don't look flat.

    @Override
    public void load(DataReader reader) {
        this.createFunction = reader.readUnsignedIntAsLong();
        this.updateFunction = reader.readUnsignedIntAsLong();
        this.killFunction = reader.readUnsignedIntAsLong();
        this.flags = reader.readInt();
        this.runtimeSize = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.createFunction);
        writer.writeUnsignedInt(this.updateFunction);
        writer.writeUnsignedInt(this.killFunction);
        writer.writeInt(this.flags);
        writer.writeInt(this.runtimeSize);
    }
}
