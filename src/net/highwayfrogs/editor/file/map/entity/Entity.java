package net.highwayfrogs.editor.file.map.entity;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the "ENTITY" struct.
 * Created by Kneesnap on 8/24/2018.
 */
public class Entity extends GameObject {
    private short formGridId;
    private short uniqueId;
    private short formBookId; // Form -> Entity ID. Flag 0x8000 = General theme, otherwise current map theme.
    private short flags;
    private int liveEntityPointer;
    private int pathRunnerPointer;
    private int nextEntity;
    private int previousEntity;
    private transient int pointer;

    private static final int FLAG_HIDDEN = 1; // Don't create a live entity while this is set.
    private static final int FLAG_NO_DISPLAY = 2; // Don't display any mesh.
    private static final int FLAG_NO_MOVEMENT = 4; // Don't allow entity movement.
    private static final int FLAG_NO_COLLISION = 8; // Collision does not apply to this entity.
    private static final int FLAG_ALIGN_TO_WORLD = 16; // Entity matrix always aligned to world axes.
    private static final int FLAG_PROJECT_ON_LAND = 32; // Entity position is projected onto the landscape.
    private static final int FLAG_LOCAL_ALIGN = 64; // Entity matrix is calculated locally (Using Y part of entity matrix.)

    @Override
    public void load(DataReader reader) {
        this.formGridId = reader.readShort();
        this.uniqueId = reader.readShort();
        this.formBookId = reader.readShort(); // Later, make this read & save the enum.
        this.flags = reader.readShort();

        this.liveEntityPointer = reader.readInt();
        this.pathRunnerPointer = reader.readInt();
        this.nextEntity = reader.readInt(); // For linked list.
        this.previousEntity = reader.readInt(); // For linked list.


        //TODO: Create Path, if path runner.
        //TODO: Finish. (ResolveMapEntities)
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.formGridId);
        writer.writeShort(this.uniqueId);
        writer.writeShort(this.formBookId);
        writer.writeShort(this.flags);
        writer.writeInt(this.liveEntityPointer);
        writer.writeInt(this.pathRunnerPointer);
        writer.writeInt(this.nextEntity);
        writer.writeInt(this.previousEntity);
    }
}
