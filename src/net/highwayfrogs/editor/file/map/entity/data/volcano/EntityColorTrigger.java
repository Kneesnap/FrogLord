package net.highwayfrogs.editor.file.map.entity.data.volcano;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.TriggerType;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents the "VOL_COLOUR_TRIGGER" struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityColorTrigger extends MatrixData {
    private TriggerType type = TriggerType.BEGIN;
    private VolcanoTriggerColor color = VolcanoTriggerColor.RED;
    private short[] uniqueIds = new short[COLOR_TRIGGER_MAX_IDS];

    private static final int COLOR_TRIGGER_MAX_IDS = 10;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = TriggerType.values()[reader.readUnsignedShortAsInt()];
        this.color = VolcanoTriggerColor.values()[reader.readUnsignedShortAsInt()];

        for (int i = 0; i < uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort((short) this.type.ordinal());
        writer.writeUnsignedShort((short) this.color.ordinal());
        for (short id : uniqueIds)
            writer.writeShort(id);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addEnumSelector("Trigger Type", getType(), TriggerType.values(), false, this::setType);
        editor.addEnumSelector("Color", getColor(), VolcanoTriggerColor.values(), false, this::setColor);
        for (int i = 0; i < getUniqueIds().length; i++) {
            final int tempI = i;
            editor.addShortField("Trigger #" + (i + 1), getUniqueIds()[i], newVal -> getUniqueIds()[tempI] = newVal, null);
        }
    }

    public enum VolcanoTriggerColor {
        RED, BLUE, CYAN, GREEN, ORANGE, PINK, PURPLE, RED_ALTERNATE, WHITE
    }
}
