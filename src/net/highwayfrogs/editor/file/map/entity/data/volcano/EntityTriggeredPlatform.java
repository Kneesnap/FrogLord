package net.highwayfrogs.editor.file.map.entity.data.volcano;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityTriggeredPlatform extends PathData {
    private int initialMovement;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.initialMovement = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.initialMovement);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Initial Movement", getInitialMovement(), this::setInitialMovement, null);
    }
}
