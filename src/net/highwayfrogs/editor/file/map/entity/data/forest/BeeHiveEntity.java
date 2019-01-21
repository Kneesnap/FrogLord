package net.highwayfrogs.editor.file.map.entity.data.forest;

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
public class BeeHiveEntity extends MatrixData {
    private int releaseDistance;
    private int swarmSpeed;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.releaseDistance = reader.readInt();
        this.swarmSpeed = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.releaseDistance);
        writer.writeInt(this.swarmSpeed);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Release Distance", String.valueOf(getReleaseDistance())));
        table.getItems().add(new NameValuePair("Swarm Speed", String.valueOf(getSwarmSpeed())));
    }
}
