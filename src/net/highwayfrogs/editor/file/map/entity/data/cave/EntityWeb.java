package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents a spider web entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityWeb extends MatrixData {
    private short spiderId;

    public EntityWeb(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.spiderId = reader.readShort();
        reader.skipShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.spiderId);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addShortField("Spider ID", getSpiderId(), this::setSpiderId, null);
    }
}