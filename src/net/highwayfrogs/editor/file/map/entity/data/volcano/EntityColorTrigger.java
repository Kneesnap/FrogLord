package net.highwayfrogs.editor.file.map.entity.data.volcano;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents the "VOL_COLOUR_TRIGGER" struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityColorTrigger extends MatrixData {
    private int type;
    private int color;
    private short[] uniqueIds = new short[COLOR_TRIGGER_MAX_IDS];

    public static final int TYPE_FREEZE = 0;
    public static final int TYPE_REVERSE = 1;
    public static final int TYPE_START = 2;

    private static final int COLOR_TRIGGER_MAX_IDS = 10;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = reader.readUnsignedShortAsInt();
        this.color = reader.readUnsignedShortAsInt();

        for (int i = 0; i < uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.type);
        writer.writeUnsignedShort(this.color);
        for (short id : uniqueIds)
            writer.writeShort(id);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Type", String.valueOf(getType())));
        table.getItems().add(new NameValuePair("Color", String.valueOf(getColor())));

        for (int i = 0; i < getUniqueIds().length; i++)
            table.getItems().add(new NameValuePair("Trigger #" + (i + 1), String.valueOf(getUniqueIds()[i])));
    }
}
