package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityFallingRock extends GameObject {
    private PSXMatrix matrix = new PSXMatrix(); // "Matrix of Entity"
    private FallingRockTarget[] targets = new FallingRockTarget[ROCK_TARGET_COUNT];
    private int delay; // Delay until rock starts moving.
    private short bounceCount; // Number of bounces.
    private int flags; //
    private int sound; // Does this rock have a sound effect?

    public static final int ROCK_TARGET_COUNT = 12;
    public static final int FLAG_TARGETS_RESOLVED = 1; // Believed to be a run-time flag.

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new FallingRockTarget();
            targets[i].load(reader);
        }

        this.delay = reader.readUnsignedShortAsInt();
        this.bounceCount = reader.readUnsignedByteAsShort();
        reader.readByte();
        this.flags = reader.readInt();
        this.sound = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        for (FallingRockTarget target : targets)
            target.save(writer);

        writer.writeUnsignedShort(this.delay);
        writer.writeUnsignedByte(this.bounceCount);
        writer.writeByte(Constants.NULL_BYTE);
        writer.writeInt(this.flags);
        writer.writeInt(this.sound);
    }

    @Getter
    public static final class FallingRockTarget extends GameObject {
        private SVector target; // Target Position.
        private int time; // Time to reach target.

        @Override
        public void load(DataReader reader) {
            this.target = SVector.readWithPadding(reader);
            this.time = reader.readUnsignedShortAsInt();
            reader.readShort();
        }

        @Override
        public void save(DataWriter writer) {
            this.target.saveWithPadding(writer);
            writer.writeUnsignedShort(this.time);
            writer.writeUnsignedShort(0);
        }
    }
}
