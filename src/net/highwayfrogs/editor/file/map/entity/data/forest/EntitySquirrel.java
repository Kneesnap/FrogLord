package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntitySquirrel extends PathData {
    private int turnDuration;

    public EntitySquirrel(FroggerGameInstance instance) {
        super(instance);
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
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Turn Duration", getTurnDuration(), this::setTurnDuration, null);
    }
}