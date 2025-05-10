package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * The frog on the plinth.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityDataPlinthFrog extends FroggerEntityDataMatrix {
    private int plinthId;

    public FroggerEntityDataPlinthFrog(FroggerMapFile mapFile) {
        super(mapFile);
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
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addSignedIntegerField("Plinth ID", this.plinthId, newId -> newId >= 0 && newId < 9, newId -> this.plinthId = newId)
                .setTooltip(FXUtils.createTooltip("The plinth ID should be the same as the ID of the plinth entity which the frog rests upon.\nThe exception to this is the final gold frog, which uses ID 8."));
    }
}