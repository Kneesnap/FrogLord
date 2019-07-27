package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
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
        editor.addIntegerField("Path ID", getPathInfo().getPathId(), pathId -> {
            getPathInfo().setSegmentDistance(0); // Start them at the start of the path when switching paths.
            getPathInfo().setSegmentId(0); // Start them at the start of the path when switching paths.
            getPathInfo().setPathId(pathId);
        }, null);

        editor.addIntegerField("Speed", getPathInfo().getSpeed(), getPathInfo()::setSpeed, null);
        editor.addCheckBox("Repeat", getPathInfo().isRepeat(), getPathInfo()::setRepeat);

        Path path = getParentEntity().getMap().getPaths().get(getPathInfo().getPathId());
        int startValue = getPathInfo().getSegmentDistance();
        for (int i = 0; i < getPathInfo().getSegmentId(); i++)
            startValue += path.getSegments().get(i).getLength();

        editor.addIntegerSlider("Distance", startValue, distance -> {
            for (int i = 0; i < path.getSegments().size(); i++) {
                PathSegment segment = path.getSegments().get(i);
                if (distance >= segment.getLength()) {
                    distance -= segment.getLength();
                } else { // Found it!
                    getPathInfo().setSegmentId(i);
                    getPathInfo().setSegmentDistance(distance);
                    break;
                }
            }
        }, 0, path.getTotalLength());
    }
}
