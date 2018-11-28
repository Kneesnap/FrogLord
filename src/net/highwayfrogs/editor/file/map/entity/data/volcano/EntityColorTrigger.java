package net.highwayfrogs.editor.file.map.entity.data.volcano;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the "VOL_COLOUR_TRIGGER" struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityColorTrigger extends GameObject {
    private PSXMatrix matrix = new PSXMatrix();
    private int type;
    private int color;
    private short[] uniqueIds = new short[COLOR_TRIGGER_MAX_IDS];

    public static final int TYPE_FREEZE = 0;
    public static final int TYPE_REVERSE = 1;
    public static final int TYPE_START = 2;

    private static final int COLOR_TRIGGER_MAX_IDS = 10;

    @Override
    public void load(DataReader reader) {
        this.matrix.load(reader);
        this.type = reader.readUnsignedShortAsInt();
        this.color = reader.readUnsignedShortAsInt();

        for (int i = 0; i < uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        this.matrix.save(writer);
        writer.writeUnsignedShort(this.type);
        writer.writeUnsignedShort(this.color);
        for (short id : uniqueIds)
            writer.writeShort(id);
    }
}
