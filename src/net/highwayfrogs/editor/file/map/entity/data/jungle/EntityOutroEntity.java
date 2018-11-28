package net.highwayfrogs.editor.file.map.entity.data.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents JUN_OUTRO_ENTITY
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class EntityOutroEntity extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private OutroTarget[] targets = new OutroTarget[TARGET_COUNT];

    private static final int TARGET_COUNT = 11;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        for (int i = 0; i < this.targets.length; i++) {
            this.targets[i] = new OutroTarget();
            this.targets[i].load(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        for (OutroTarget target : this.targets)
            target.save(writer);
    }

    @Getter // Represents JUN_OUTRO_TARGETS
    public static final class OutroTarget extends GameObject {
        private SVector target; // Target position.
        private int time; // Time to reach target.

        @Override
        public void load(DataReader reader) {
            this.target = SVector.readWithPadding(reader);
            this.time = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            this.target.saveWithPadding(writer);
            writer.writeInt(this.time);
        }
    }
}
