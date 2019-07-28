package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

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

    public EntityFallingRock() {
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
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Delay", getDelay(), this::setDelay, null);
        editor.addShortField("Bounces", getBounceCount(), this::setBounceCount, null);
        editor.addIntegerField("Flags", getFlags(), this::setFlags, null);
        editor.addIntegerField("Sound", getSound(), this::setSound, null);
        for (int i = 0; i < getTargets().length; i++) {
            editor.addNormalLabel("Target #" + (i + 1) + ":");
            getTargets()[i].setupEditor(editor);
        }
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

        public void setupEditor(GUIEditorGrid grid) {
            grid.addFloatSVector("Target", getTarget());
            grid.addIntegerField("Time", getTime(), this::setTime, null);
        }
    }
}
