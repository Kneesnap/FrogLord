package net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.path;

import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.oldfrogger.OldFroggerGameInstance;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.entity.OldFroggerMapEntity;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapPathPacket;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapPathPacket.OldFroggerMapPath;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.text.DecimalFormat;

/**
 * Represents pathing data for old Frogger's entities.
 * Created by Kneesnap on 12/14/2023.
 */
@Getter
public class OldFroggerPathData extends SCGameData<OldFroggerGameInstance> {
    private int pathId;
    private int splineId;
    private int splinePosition;
    private MotionType motionType = MotionType.Restart;

    public OldFroggerPathData(OldFroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        this.pathId = reader.readUnsignedShortAsInt();
        this.splineId = reader.readUnsignedShortAsInt();
        this.splinePosition = reader.readUnsignedShortAsInt();
        reader.skipBytesRequireEmpty(1); // Padding
        this.motionType = MotionType.values()[reader.readUnsignedByteAsShort()];
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.pathId);
        writer.writeUnsignedShort(this.splineId);
        writer.writeUnsignedShort(this.splinePosition);
        writer.writeNull(1); // Padding
        writer.writeUnsignedByte((short) (this.motionType != null ? this.motionType.ordinal() : 0));
    }

    /**
     * Gets the path which this data is currently attached to.
     * @param map The map file containing paths.
     * @return path or null if there is none.
     */
    public OldFroggerMapPath getPath(OldFroggerMapFile map) {
        if (map == null)
            return null;

        OldFroggerMapPathPacket pathPacket = map.getPathPacket();
        if (pathPacket == null)
            return null;

        if (this.pathId < 0 || this.pathId >= pathPacket.getPaths().size())
            return null;

        return pathPacket.getPaths().get(this.pathId);
    }

    /**
     * Gets the total path distance this info is at. Note this is total path distance, not segment distance.
     * @param map The map to get the path from.
     * @return totalPathDistance
     */
    public int getTotalPathDistance(OldFroggerMapFile map) {
        OldFroggerMapPath path = getPath(map);
        if (path == null)
            throw new RuntimeException("Failed to find the current path reference.");

        int totalDistance = this.splinePosition; // Get current segment's distance.
        for (int i = 0; i < this.splineId; i++) // Include the distance from all previous segments.
            totalDistance += path.getSplines().get(i).calculateLength();
        return totalDistance;
    }

    /**
     * Updates the distance this is along the path. Note this uses total path distance not segment distance.
     * @param map           The map this info belongs to.
     * @param totalDistance The total path distance.
     */
    public void setTotalPathDistance(OldFroggerMapFile map, int totalDistance) {
        OldFroggerMapPath path = getPath(map);
        if (path == null)
            throw new RuntimeException("Failed to find the current path reference.");

        for (int i = 0; i < path.getSplines().size(); i++) {
            OldFroggerSpline spline = path.getSplines().get(i);
            int splineLength = spline.calculateLength();
            if (totalDistance > splineLength) {
                totalDistance -= splineLength;
            } else { // Found it!
                this.splineId = i;
                this.splinePosition = totalDistance;
                break;
            }
        }
    }

    /**
     * Sets up a path data editor.
     * @param manager The manager to setup the editor for.
     * @param entity  The entity to update elements for.
     * @param editor  The editor to create ui elements for.
     */
    public void setupEditor(OldFroggerEntityManager manager, OldFroggerMapEntity entity, GUIEditorGrid editor) {
        final float distAlongPath = Utils.fixedPointIntToFloat4Bit(getTotalPathDistance(manager.getMap()));
        final float totalPathDist = Utils.fixedPointIntToFloat4Bit(getPath(manager.getMap()).getTotalLength());

        Slider travDistSlider = editor.addDoubleSlider("Travel Distance:", distAlongPath, newValue -> {
            setTotalPathDistance(manager.getMap(), Utils.floatToFixedPointInt4Bit(newValue.floatValue()));
            manager.updateEntityPositionRotation(entity);
        }, 0.0, totalPathDist);

        TextField travDistText = editor.addFloatField("", distAlongPath, newValue -> {
            setTotalPathDistance(manager.getMap(), Utils.floatToFixedPointInt4Bit(newValue));
            manager.updateEntityPositionRotation(entity);
        }, newValue -> !((newValue < 0.0f) || (newValue > totalPathDist)));
        travDistText.textProperty().bindBidirectional(travDistSlider.valueProperty(), new NumberStringConverter(new DecimalFormat("####0.00")));

        TextField txtFieldMaxTravel = editor.addFloatField("(Max. Travel):", totalPathDist);
        txtFieldMaxTravel.setEditable(false);
        txtFieldMaxTravel.setDisable(true);

        // Motion Data:
        editor.addEnumSelector("Motion Type", this.motionType, MotionType.values(), false, newMotionType -> this.motionType = newMotionType);

        if (this.pathId < 0 || this.pathId >= manager.getMap().getPathPacket().getPaths().size()) { // Invalid path! Show this as a text box.
            editor.addIntegerField("Path ID", this.pathId, newPathId -> {
                this.pathId = newPathId;
                manager.updateEntityPositionRotation(entity);
            }, newPathId -> newPathId >= 0 && newPathId < manager.getMap().getPathPacket().getPaths().size());
        } else { // Otherwise, show it as a selection box!
            editor.addIntegerField("Path ID", this.pathId, newPathId -> {
                this.pathId = newPathId;
                manager.updateEntityPositionRotation(entity);
            }, newPathId -> newPathId >= 0 && newPathId < manager.getMap().getPathPacket().getPaths().size());

            // TODO: Bring back path selection.
            /*
            editor.addBoldLabelButton("Path #" + pathId, "Select Path", 25, () ->
                    controller.getPathManager().promptPath((path, segment, segDistance) -> {
                        setPath(manager.getMap(), path, segment);
                        setSegmentDistance(segDistance);
                        manager.updateEntityPositionRotation(entity);
                        manager.showEntityInfo(getParentEntity()); // Update the entity editor display, update path slider, etc.
                    }, null));*/
        }
    }

    /**
     * Contains the available motion types.
     */
    public enum MotionType {
        Die,
        Reverse,
        Restart,
        Reverse_Back
    }
}