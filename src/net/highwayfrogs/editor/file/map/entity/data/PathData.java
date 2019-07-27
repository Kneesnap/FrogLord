package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Base entity data which holds path data.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@Setter
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
    public void addData(GUIEditorGrid editor) {
        editor.addIntegerField("Path ID", getPathInfo().getPathId(), getPathInfo()::setPathId, null);
        editor.addIntegerField("Segment", getPathInfo().getSegmentId(), getPathInfo()::setSegmentId, null);
        editor.addIntegerField("Speed", getPathInfo().getSpeed(), getPathInfo()::setSpeed, null);
        editor.addIntegerSlider("Distance", getPathInfo().getSegmentDistance(), getPathInfo()::setSegmentDistance, 0,
                getParentEntity().getMap().getPaths().get(getPathInfo().getPathId()).getSegments().get(getPathInfo().getSegmentId()).getLength());
        editor.addCheckBox("Repeat", getPathInfo().isRepeat(), getPathInfo()::setRepeat);
    }
}
