package net.highwayfrogs.editor.file.map.entity.data.jungle;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityRopeBridge extends MatrixData {
    private int fallDelay;
    private int hopsBefore;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fallDelay = reader.readUnsignedShortAsInt();
        this.hopsBefore = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.fallDelay);
        writer.writeUnsignedShort(this.hopsBefore);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Fall Delay", getFallDelay(), this::setFallDelay, null);
        editor.addIntegerField("Hops Before", getHopsBefore(), this::setHopsBefore, null);
    }
}
