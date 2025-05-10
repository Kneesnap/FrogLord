package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the 'JUN_OUTRO_FROGPLINTH_DATA' entity data definition in ent_jun.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataOutroPlinth extends FroggerEntityDataMatrix {
    private int id;

    public FroggerEntityDataOutroPlinth(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.id = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.id);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addSignedIntegerField("Plinth ID", this.id, newId -> newId >= 0 && newId < 8, newId -> this.id = newId)
                .setTooltip(FXUtils.createTooltip("There should be 8 plinths in a level, each with their own unique ID.\nThe ID is supposed to correspond to the zone the plinth represents."));
    }
}