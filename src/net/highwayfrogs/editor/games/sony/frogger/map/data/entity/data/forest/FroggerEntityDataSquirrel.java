package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'FOREST_SQUIRREL' definition in ent_for.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataSquirrel extends FroggerEntityDataPathInfo {
    private int turnDuration = 30;

    public FroggerEntityDataSquirrel(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.turnDuration = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.turnDuration);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        // Seems unused.
        TextField squirrelTurnDurationField = editor.addUnsignedFixedShort("Turn Duration (secs)", this.turnDuration, newTurnDuration -> this.turnDuration = newTurnDuration, getGameInstance().getFPS(), 0, 3000);
        squirrelTurnDurationField.setDisable(true);
        squirrelTurnDurationField.setTooltip(FXUtils.createTooltip("Controls how long the squirrel's turn lasts.\nSeems unused in favor of automatically checking when the turn animation is finished."));
    }
}