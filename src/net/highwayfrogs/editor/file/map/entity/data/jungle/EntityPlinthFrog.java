package net.highwayfrogs.editor.file.map.entity.data.jungle;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * The frog on the plinth.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class EntityPlinthFrog extends MatrixData {
    private int plinthId;

    public EntityPlinthFrog(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.plinthId = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.plinthId);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Plinth ID", getPlinthId(), this::setPlinthId, null);
    }
}