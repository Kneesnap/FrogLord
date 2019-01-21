package net.highwayfrogs.editor.file.map.entity.data.cave;

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
public class EntityFrogLight extends MatrixData {
    private int minRadius;
    private int maxRadius;
    private int dieSpeed;
    private int count;
    private int setup;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.minRadius = reader.readInt();
        this.maxRadius = reader.readInt();
        this.dieSpeed = reader.readInt();
        this.count = reader.readInt();
        this.setup = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
        writer.writeInt(this.dieSpeed);
        writer.writeInt(this.count);
        writer.writeInt(this.setup);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Min Radius", String.valueOf(getMinRadius())));
        table.getItems().add(new NameValuePair("Max Radius", String.valueOf(getMaxRadius())));
        table.getItems().add(new NameValuePair("Die Speed", String.valueOf(getDieSpeed())));
        table.getItems().add(new NameValuePair("Count", String.valueOf(getCount())));
        table.getItems().add(new NameValuePair("Setup", String.valueOf(getSetup())));

    }
}
