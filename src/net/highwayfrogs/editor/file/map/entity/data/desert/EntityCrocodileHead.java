package net.highwayfrogs.editor.file.map.entity.data.desert;

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
public class EntityCrocodileHead extends MatrixData {
    private int riseHeight;
    private int riseSpeed;
    private int snapDelay;
    private int pauseDelay;
    private int snapOrNot;
    private int submergedDelay;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.riseHeight = reader.readUnsignedShortAsInt();
        this.riseSpeed = reader.readUnsignedShortAsInt();
        this.snapDelay = reader.readUnsignedShortAsInt();
        this.pauseDelay = reader.readUnsignedShortAsInt();
        this.snapOrNot = reader.readUnsignedShortAsInt();
        this.submergedDelay = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.riseHeight);
        writer.writeUnsignedShort(this.riseSpeed);
        writer.writeUnsignedShort(this.snapDelay);
        writer.writeUnsignedShort(this.pauseDelay);
        writer.writeUnsignedShort(this.snapOrNot);
        writer.writeUnsignedShort(this.submergedDelay);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Rise Height", String.valueOf(getRiseHeight())));
        table.getItems().add(new NameValuePair("Rise Speed", String.valueOf(getRiseSpeed())));
        table.getItems().add(new NameValuePair("Snap Delay", String.valueOf(getSnapDelay())));
        table.getItems().add(new NameValuePair("Pause Delay", String.valueOf(getPauseDelay())));
        table.getItems().add(new NameValuePair("Should Snap", String.valueOf(getSnapOrNot())));
        table.getItems().add(new NameValuePair("Submerged Delay", String.valueOf(getSubmergedDelay())));
    }
}
