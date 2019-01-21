package net.highwayfrogs.editor.file.map.entity.data.swamp;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityCrusher extends MatrixData {
    private short speed;
    private short distance;
    private short direction;
    private short delay;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readShort();
        this.distance = reader.readShort();
        this.direction = reader.readShort();
        this.delay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.speed);
        writer.writeShort(this.distance);
        writer.writeShort(this.direction);
        writer.writeShort(this.delay);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Speed", String.valueOf(getSpeed())));
        table.getItems().add(new NameValuePair("Distance", String.valueOf(getDistance())));
        table.getItems().add(new NameValuePair("Direction", String.valueOf(getDirection())));
        table.getItems().add(new NameValuePair("Delay", String.valueOf(getDelay())));
    }
}
