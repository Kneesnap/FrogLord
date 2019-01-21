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
public class EntityRat extends MatrixData {
    private short speed;
    private SVector startTarget;
    private SVector startRunTarget;
    private SVector endRunTarget;
    private SVector endTarget;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readShort();
        reader.readShort();
        this.startTarget = SVector.readWithPadding(reader);
        this.startRunTarget = SVector.readWithPadding(reader);
        this.endRunTarget = SVector.readWithPadding(reader);
        this.endTarget = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.speed);
        writer.writeUnsignedShort(0);
        this.startTarget.saveWithPadding(writer);
        this.startRunTarget.saveWithPadding(writer);
        this.endRunTarget.saveWithPadding(writer);
        this.endTarget.saveWithPadding(writer);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Speed", String.valueOf(getSpeed())));
        table.getItems().add(new NameValuePair("Start Target", getStartTarget().toString()));
        table.getItems().add(new NameValuePair("Start Run Target", getStartRunTarget().toString()));
        table.getItems().add(new NameValuePair("End Run Target", getEndRunTarget().toString()));
        table.getItems().add(new NameValuePair("End Target", getEndTarget().toString()));
    }
}
