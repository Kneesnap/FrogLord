package net.highwayfrogs.editor.file.map.entity.data.jungle;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;

/**
 * Represents JUN_OUTRO_ENTITY
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class EntityOutroEntity extends MatrixData {
    private OutroTarget[] targets;

    public EntityOutroEntity(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.targets = new OutroTarget[getConfig().isAtOrBeforeBuild51() ? 10 : 11];
        for (int i = 0; i < this.targets.length; i++) {
            OutroTarget newTarget = new OutroTarget();
            newTarget.load(reader);
            this.targets[i] = newTarget;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (OutroTarget target : this.targets)
            target.save(writer);
    }

    @Override
    public void addData(EntityManager manager, GUIEditorGrid editor) {
        super.addData(manager, editor);
        for (int i = 0; i < getTargets().length; i++) {
            editor.addNormalLabel("Target #" + (i + 1) + ":");
            getTargets()[i].setupEditor(editor, manager.getController());
        }
    }

    @Setter
    @Getter // Represents JUN_OUTRO_TARGETS
    public static final class OutroTarget extends GameObject {
        private SVector target = new SVector(); // Target position.
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

        public void setupEditor(GUIEditorGrid grid, MapUIController controller) {
            grid.addFloatSVector("Target", getTarget(), controller);
            grid.addIntegerField("Time", getTime(), this::setTime, null);
        }
    }
}