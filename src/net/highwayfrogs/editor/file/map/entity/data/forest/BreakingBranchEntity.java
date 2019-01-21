package net.highwayfrogs.editor.file.map.entity.data.forest;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents "FOREST_BREAKING_BRANCH".
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class BreakingBranchEntity extends MatrixData {
    private int breakDelay;
    private int fallSpeed;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.breakDelay = reader.readUnsignedShortAsInt();
        this.fallSpeed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.breakDelay);
        writer.writeUnsignedShort(this.fallSpeed);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Break Delay", String.valueOf(getBreakDelay())));
        table.getItems().add(new NameValuePair("Fall Speed", String.valueOf(getFallSpeed())));
    }
}
