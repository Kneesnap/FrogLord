package net.highwayfrogs.editor.file.map.entity.data.volcano;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerEntityTriggerType;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the "VOL_COLOUR_TRIGGER" struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityColorTrigger extends MatrixData {
    private FroggerEntityTriggerType type = FroggerEntityTriggerType.BEGIN;
    private VolcanoTriggerColor color = VolcanoTriggerColor.RED;
    private short[] uniqueIds = new short[COLOR_TRIGGER_MAX_IDS];

    private static final int COLOR_TRIGGER_MAX_IDS = 10;

    public EntityColorTrigger(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = FroggerEntityTriggerType.values()[reader.readUnsignedShortAsInt()];
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

    public enum VolcanoTriggerColor {
        RED, BLUE, CYAN, GREEN, ORANGE, PINK, PURPLE, RED_ALTERNATE, WHITE
    }
}