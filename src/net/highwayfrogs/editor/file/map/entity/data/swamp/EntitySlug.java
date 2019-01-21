package net.highwayfrogs.editor.file.map.entity.data.swamp;

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
public class EntitySlug extends PathData {
    private int motionType;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.motionType = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.motionType);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Motion Type", String.valueOf(getMotionType())));
    }
}
