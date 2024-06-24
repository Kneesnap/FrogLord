package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents a spider web entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataWeb extends FroggerEntityDataMatrix {
    private int spiderId;

    public FroggerEntityDataWeb(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.spiderId = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.spiderId);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedShortField("Spider ID", this.spiderId, newSpiderId -> this.spiderId = newSpiderId);
    }
}