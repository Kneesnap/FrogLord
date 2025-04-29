package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityFallingRock extends MatrixData {
    private FallingRockTarget[] targets = new FallingRockTarget[ROCK_TARGET_COUNT];
    private int delay; // Delay until rock starts moving.
    private short bounceCount; // Number of bounces.
    private int flags; //
    private int sound; // Does this rock have a sound effect?

    public static final int ROCK_TARGET_COUNT = 12;
    public static final int FLAG_TARGETS_RESOLVED = Constants.BIT_FLAG_0; // Believed to be a run-time flag.

    public EntityFallingRock(FroggerGameInstance instance) {
        super(instance);
        for (int i = 0; i < targets.length; i++)
            targets[i] = new FallingRockTarget();
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        for (FallingRockTarget target : targets)
            target.load(reader);

        this.delay = reader.readUnsignedShortAsInt();
        this.bounceCount = reader.readUnsignedByteAsShort();
        reader.skipByte();
        this.flags = reader.readInt();
        this.sound = getConfig().isAtOrBeforeBuild38() ? -1 : reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (FallingRockTarget target : targets)
            target.save(writer);

        writer.writeUnsignedShort(this.delay);
        writer.writeUnsignedByte(this.bounceCount);
        writer.writeByte(Constants.NULL_BYTE);
        writer.writeInt(this.flags);
        if (!getConfig().isAtOrBeforeBuild38())
            writer.writeInt(this.sound);
    }

    @Getter
    @Setter
    public static final class FallingRockTarget extends GameObject {
        private SVector target = new SVector(); // Target Position.
        private int time; // Time to reach target.

        @Override
        public void load(DataReader reader) {
            this.target = SVector.readWithPadding(reader);
            this.time = reader.readUnsignedShortAsInt();
            reader.skipShort();
        }

        @Override
        public void save(DataWriter writer) {
            this.target.saveWithPadding(writer);
            writer.writeUnsignedShort(this.time);
            writer.writeUnsignedShort(0);
        }
    }
}