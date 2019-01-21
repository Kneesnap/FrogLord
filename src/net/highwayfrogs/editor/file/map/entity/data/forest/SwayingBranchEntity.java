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
public class SwayingBranchEntity extends MatrixData {
    private int swayAngle;
    private int swayDuration;
    private int onceOffDelay;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.swayAngle = reader.readUnsignedShortAsInt();
        this.swayDuration = reader.readUnsignedShortAsInt();
        this.onceOffDelay = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.swayAngle);
        writer.writeUnsignedShort(this.swayDuration);
        writer.writeUnsignedShort(this.onceOffDelay);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Sway Angle", String.valueOf(getSwayAngle())));
        table.getItems().add(new NameValuePair("Sway Duration", String.valueOf(getSwayDuration())));
        table.getItems().add(new NameValuePair("Once Off Delay", String.valueOf(getOnceOffDelay())));
    }
}
