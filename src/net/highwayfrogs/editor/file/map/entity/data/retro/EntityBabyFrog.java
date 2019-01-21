package net.highwayfrogs.editor.file.map.entity.data.retro;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents "ORG_BABY_FROG_DATA", which is the data for the pink frog you can pickup on the retro logs.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class EntityBabyFrog extends MatrixData {
    private short logId; // The id of the log this frog will stand on.
    private short awardedPoints; // The points awarded when collected.

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.logId = reader.readShort();
        this.awardedPoints = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.logId);
        writer.writeShort(this.awardedPoints);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Log ID", String.valueOf(getLogId())));
        table.getItems().add(new NameValuePair("Points", String.valueOf(getAwardedPoints())));
    }
}
