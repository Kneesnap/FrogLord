package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents "GEN_CHECKPOINT".
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class CheckpointEntity extends MatrixData {
    private int id;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.id = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.id);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("ID", getId(), this::setId, null);
    }
}
