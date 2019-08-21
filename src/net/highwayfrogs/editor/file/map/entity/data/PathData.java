package net.highwayfrogs.editor.file.map.entity.data;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathInfo.PathMotionType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.EntityManager;
import net.highwayfrogs.editor.utils.Utils;

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
        MAPFile map = getParentEntity().getMap();
        editor.addIntegerField("Speed", getPathInfo().getSpeed(), getPathInfo()::setSpeed, null);
        editor.addIntegerSlider("Distance", getPathInfo().getTotalPathDistance(map),
                distance -> getPathInfo().setTotalPathDistance(getParentEntity().getMap(), distance), 0, getPathInfo().getPath(map).getTotalLength());

        // Motion Data:
        for (PathMotionType type : PathMotionType.values())
            editor.addCheckBox(Utils.capitalize(type.name()), getPathInfo().testFlag(type), newState -> getPathInfo().setFlag(type, newState));
    }

    @Override
    public void addData(EntityManager manager, GUIEditorGrid editor) {
        MAPFile map = getParentEntity().getMap();
        int pathId = getPathInfo().getPathId();
        if (pathId < 0 || pathId >= map.getPaths().size()) { // Invalid path! Show this as a text box.
            editor.addIntegerField("Path ID", pathId, getPathInfo()::setPathId, null);
        } else { // Otherwise, show it as a selection box!
            editor.addBoldLabelButton("Path #" + pathId, "Select Path", 25, () ->
                    manager.getController().getPathManager().promptPath((path, segment, segDistance) -> {
                        getPathInfo().setPath(manager.getMap(), path, segment);
                        getPathInfo().setSegmentDistance(segDistance);
                        manager.updateEntities();
                    }, null));
        }

        super.addData(manager, editor); // Path ID comes before the rest.
    }
}
