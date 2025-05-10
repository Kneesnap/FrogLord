package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents "GEN_CHECKPOINT" from ent_gen.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataCheckpoint extends FroggerEntityDataMatrix {
    private int id; // This is overwritten by the game itself.

    public FroggerEntityDataCheckpoint(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.id = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.id);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        TextField checkpointIdField = editor.addUnsignedShortField("Unused ID", this.id, newId -> newId >= 0 && newId < 5, newId -> this.id = newId);
        checkpointIdField.setTooltip(FXUtils.createTooltip("Each checkpoint has its own ID. Older maps have it baked into the map file.\nThe final game automatically assigns these IDs at runtime, so they do not need to be manually assigned."));
        checkpointIdField.setDisable(true);
    }
}