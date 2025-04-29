package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents JUN_OUTRO_ENTITY
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityDataOutro extends FroggerEntityDataMatrix {
    private final OutroTarget[] targets;

    public FroggerEntityDataOutro(FroggerMapFile mapFile) {
        super(mapFile);
        this.targets = new OutroTarget[getConfig().isAtOrBeforeBuild51() ? 10 : 11];
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        for (int i = 0; i < this.targets.length; i++) {
            OutroTarget newTarget = new OutroTarget();
            newTarget.load(reader);
            this.targets[i] = newTarget;
        }
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        for (int i = 0; i < this.targets.length; i++)
            this.targets[i].save(writer);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);
        for (int i = 0; i < this.targets.length; i++) {
            editor.addNormalLabel("Target #" + (i + 1) + ":");
            this.targets[i].setupEditor(editor, manager.getController());
        }
    }

    @Getter // Represents JUN_OUTRO_TARGETS
    public static final class OutroTarget extends GameObject {
        private final SVector target = new SVector(); // Target position.
        private int time = 150; // Time to reach target. (Unused, the game just hardcodes 30)

        @Override
        public void load(DataReader reader) {
            this.target.loadWithPadding(reader);
            this.time = reader.readInt();
        }

        @Override
        public void save(DataWriter writer) {
            this.target.saveWithPadding(writer);
            writer.writeInt(this.time);
        }

        public void setupEditor(GUIEditorGrid grid, FroggerMapMeshController controller) {
            grid.addFloatSVector("Target", this.target, controller);
            grid.addFixedInt("Time (Unused)", this.time, newTime -> this.time = newTime, 30);
        }
    }
}