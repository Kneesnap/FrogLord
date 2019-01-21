package net.highwayfrogs.editor.file.map.entity.data.desert;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityFallingRock extends MatrixData {
    private FallingRockTarget[] targets = new FallingRockTarget[ROCK_TARGET_COUNT];
    private int delay; // Delay until rock starts moving.
    private short bounceCount; // Number of bounces.
    private int flags; //
    private int sound; // Does this rock have a sound effect?

    public static final int ROCK_TARGET_COUNT = 12;
    public static final int FLAG_TARGETS_RESOLVED = Constants.BIT_FLAG_0; // Believed to be a run-time flag.

    @Override
    public void load(DataReader reader) {
        super.load(reader);
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
        super.save(writer);
        for (FallingRockTarget target : targets)
            target.save(writer);

        writer.writeUnsignedShort(this.delay);
        writer.writeUnsignedByte(this.bounceCount);
        writer.writeByte(Constants.NULL_BYTE);
        writer.writeInt(this.flags);
        writer.writeInt(this.sound);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Delay", String.valueOf(getDelay())));
        table.getItems().add(new NameValuePair("Bounces", String.valueOf(getBounceCount())));
        table.getItems().add(new NameValuePair("Flags", String.valueOf(getFlags())));
        table.getItems().add(new NameValuePair("Sound", String.valueOf(getSound())));
        for (int i = 0; i < getTargets().length; i++)
            table.getItems().add(new NameValuePair("Target #" + (i + 1), getTargets()[i].getTime() + " -> " + getTargets()[i].getTarget()));
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
