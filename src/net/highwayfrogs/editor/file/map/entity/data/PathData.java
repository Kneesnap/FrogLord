package net.highwayfrogs.editor.file.map.entity.data;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Base entity data which holds path data.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
public class PathData extends EntityData {
    private PathInfo pathInfo = new PathInfo();

    @Override
    public void load(DataReader reader) {
        this.pathInfo.load(reader);
    }

    @Override
    public void save(DataWriter writer) {
        this.pathInfo.save(writer);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Path ID", String.valueOf(getPathInfo().getPathId())));
        table.getItems().add(new NameValuePair("Segment", String.valueOf(getPathInfo().getSegmentId())));
        table.getItems().add(new NameValuePair("Speed", String.valueOf(getPathInfo().getSpeed())));
        table.getItems().add(new NameValuePair("Segment Distance", String.valueOf(getPathInfo().getSegmentDistance())));
        table.getItems().add(new NameValuePair("Motion Type", String.valueOf(getPathInfo().getMotionType())));
    }
}
