package net.highwayfrogs.editor.file.map.entity.data;

import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathInfo.PathMotionType;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.ui.mapeditor.EntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.text.DecimalFormat;

/**
 * Base entity data which holds path data.
 * Created by Kneesnap on 1/20/2019.
 */
@Getter
@Setter
public class PathData extends EntityData {
    private PathInfo pathInfo = new PathInfo();

    public PathData(FroggerGameInstance instance) {
        super(instance);
    }

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

        final float distAlongPath = Utils.fixedPointIntToFloat4Bit(getPathInfo().getTotalPathDistance(map));
        final float totalPathDist = Utils.fixedPointIntToFloat4Bit(getPathInfo().getPath(map).getTotalLength());

        Slider travDistSlider = editor.addDoubleSlider("Travel Distance:", distAlongPath, newValue -> getPathInfo().setTotalPathDistance(getParentEntity().getMap(), Utils.floatToFixedPointInt4Bit(newValue.floatValue())), 0.0, totalPathDist);
        TextField travDistText = editor.addFloatField("", distAlongPath, newValue -> getPathInfo().setTotalPathDistance(getParentEntity().getMap(), Utils.floatToFixedPointInt4Bit(newValue)), newValue -> !((newValue < 0.0f) || (newValue > totalPathDist)));
        travDistText.textProperty().bindBidirectional(travDistSlider.valueProperty(), new NumberStringConverter(new DecimalFormat("####0.00")));

        TextField txtFieldMaxTravel = editor.addFloatField("(Max. Travel):", totalPathDist);
        txtFieldMaxTravel.setEditable(false);
        txtFieldMaxTravel.setDisable(true);

        // Motion Data:
        for (PathMotionType type : PathMotionType.values())
            if (type.isAllowEdit())
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
                        manager.showEntityInfo(getParentEntity()); // Update the entity editor display, update path slider, etc.
                    }, null));
        }

        super.addData(manager, editor); // Path ID comes before the rest.
    }
}