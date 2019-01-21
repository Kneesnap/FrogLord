package net.highwayfrogs.editor.file.map.entity.data.forest;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityHedgehog extends PathData {
    private int runTime;
    private int rollTime;
    private int runSpeed;
    private int rollSpeed;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.runTime = reader.readUnsignedShortAsInt();
        this.rollTime = reader.readUnsignedShortAsInt();
        this.runSpeed = reader.readUnsignedShortAsInt();
        this.rollSpeed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.runTime);
        writer.writeUnsignedShort(this.rollTime);
        writer.writeUnsignedShort(this.runSpeed);
        writer.writeUnsignedShort(this.rollSpeed);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Run Time", String.valueOf(getRunTime())));
        table.getItems().add(new NameValuePair("Roll Time", String.valueOf(getRollTime())));
        table.getItems().add(new NameValuePair("Run Speed", String.valueOf(getRunSpeed())));
        table.getItems().add(new NameValuePair("Roll Speed", String.valueOf(getRollSpeed())));
    }
}
