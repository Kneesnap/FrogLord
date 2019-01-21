package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.entity.TriggerType;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents a trigger entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class TriggerEntity extends MatrixData {
    private TriggerType type;
    private short[] uniqueIds = new short[ENTITY_TYPE_TRIGGER_MAX_IDS];

    public static final int ENTITY_TYPE_TRIGGER_MAX_IDS = 10;
    public static final int BYTE_SIZE = PSXMatrix.BYTE_SIZE + (10 * Constants.SHORT_SIZE) + (2 * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = TriggerType.values()[reader.readInt()];

        for (int i = 0; i < uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.type.ordinal());
        for (short id : uniqueIds)
            writer.writeShort(id);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addEnumSelector("Trigger Type", getType(), TriggerType.values(), false, this::setType);
        for (int i = 0; i < getUniqueIds().length; i++) {
            final int tempIndex = i;
            editor.addShortField("Entity #" + (i + 1), getUniqueIds()[i], newVal -> getUniqueIds()[tempIndex] = newVal, null);
        }
    }
}
