package net.highwayfrogs.editor.file.map.entity.data.jungle;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents JUN_OUTRO_ENTITY
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class EntityOutroEntity extends MatrixData {
    private OutroTarget[] targets = new OutroTarget[TARGET_COUNT];

    private static final int TARGET_COUNT = 11;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        for (int i = 0; i < this.targets.length; i++) {
            this.targets[i] = new OutroTarget();
            this.targets[i].load(reader);
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (OutroTarget target : this.targets)
            target.save(writer);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        for (int i = 0; i < getTargets().length; i++)
            table.getItems().add(new NameValuePair("Target #" + (i + 1), getTargets()[i].getTime() + " -> " + getTargets()[i].getTarget()));
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
