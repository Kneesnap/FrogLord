package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.BonusFlyType;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Data for a entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class BonusFlyEntity extends MatrixData {
    private BonusFlyType type; // Score type.

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = BonusFlyType.values()[reader.readInt()];
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.type.ordinal());
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addEnumSelector("Fly Type", getType(), BonusFlyType.values(), false, this::setType);
    }
}
