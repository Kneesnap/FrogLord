package net.highwayfrogs.editor.file.map.entity.data.cave;

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
public class EntityFatFireFly extends MatrixData {
    private int type;
    private SVector target;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.type = reader.readUnsignedShortAsInt();
        reader.readShort();
        this.target = SVector.readWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.type);
        writer.writeUnsignedShort(0);
        this.target.saveWithPadding(writer);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Type", String.valueOf(getType())));
        table.getItems().add(new NameValuePair("Target", target.toString()));
    }
}
