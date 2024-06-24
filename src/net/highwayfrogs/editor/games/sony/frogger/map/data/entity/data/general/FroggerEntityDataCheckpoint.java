package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents "GEN_CHECKPOINT" from ent_gen.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataCheckpoint extends FroggerEntityDataMatrix {
    private int id;

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
        editor.addUnsignedShortField("ID", this.id, newId -> newId >= 0 && newId < 5, newId -> this.id = newId);
    }
}