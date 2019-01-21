package net.highwayfrogs.editor.file.map.entity.data.general;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents a trigger entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class TriggerEntity extends MatrixData {
    private int type;
    private short[] uniqueIds = new short[ENTITY_TYPE_TRIGGER_MAX_IDS];

    public static final int ENTITY_TYPE_TRIGGER_MAX_IDS = 10;
    public static final int BYTE_SIZE = PSXMatrix.BYTE_SIZE + (10 * Constants.SHORT_SIZE) + (2 * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = reader.readInt();

        for (int i = 0; i < uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.type);
        for (short id : uniqueIds)
            writer.writeShort(id);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Type", String.valueOf(getType())));
        for (int i = 0; i < getUniqueIds().length; i++)
            table.getItems().add(new NameValuePair("Entity #" + (i + 1), String.valueOf(getUniqueIds()[i])));
    }
}
