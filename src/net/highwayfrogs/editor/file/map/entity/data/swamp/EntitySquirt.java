package net.highwayfrogs.editor.file.map.entity.data.swamp;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntitySquirt extends MatrixData {
    private short timeDelay;
    private short dropTime;
    private SVector target;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.timeDelay = reader.readShort();
        this.dropTime = reader.readShort();
        this.target = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.timeDelay);
        writer.writeShort(this.dropTime);
        this.target.saveWithPadding(writer);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Time Delay", String.valueOf(getTimeDelay())));
        table.getItems().add(new NameValuePair("Drop Time", String.valueOf(getDropTime())));
        table.getItems().add(new NameValuePair("Target", getTarget().toString()));
    }
}
