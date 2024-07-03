package net.highwayfrogs.editor.games.sony.frogger.map.data.path;

import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.converter.NumberStringConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.Utils;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Represents the PATH_INFO struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerPathInfo extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile mapFile;
    private int pathId;
    @Setter private int segmentId;
    @Setter private int segmentDistance;
    private int motionType = FroggerPathMotionType.REPEAT.getFlagBitMask();
    private int speed = 10;

    public FroggerPathInfo(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.pathId = reader.readUnsignedShortAsInt();
        this.segmentId = reader.readUnsignedShortAsInt();
        this.segmentDistance = reader.readUnsignedShortAsInt();
        this.motionType = reader.readUnsignedShortAsInt();
        warnAboutInvalidBitFlags(this.motionType, FroggerPathMotionType.FLAG_VALIDATION_MASK);
        this.speed = reader.readUnsignedShortAsInt();
        reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.pathId);
        writer.writeUnsignedShort(this.segmentId);
        writer.writeUnsignedShort(this.segmentDistance);
        writer.writeUnsignedShort(this.motionType);
        writer.writeUnsignedShort(this.speed);
        writer.align(Constants.INTEGER_SIZE); // Padding.
    }

    /**
     * Sets the id of the path to use. Resets the path progress.
     * @param newPathId The new path's id.
     */
    public void setPathId(int newPathId) {
        setPathId(newPathId, true);
    }

    /**
     * Sets the id of the path to use.
     * @param newPathId     The new path's id.
     * @param resetDistance Whether to reset the path progress.
     */
    public void setPathId(int newPathId, boolean resetDistance) {
        if (resetDistance) {
            this.segmentDistance = 0; // Start them at the start of the path when switching paths.
            this.segmentId = 0; // Start them at the start of the path when switching paths.
        }
        this.pathId = newPathId;
    }

    /**
     * Sets the new path to use.
     * @param newPath       The new path.
     * @param resetDistance Whether to reset the path progress.
     */
    public void setPath(FroggerPath newPath, boolean resetDistance) {
        int newIndex = newPath.getPathIndex();
        if (newIndex == -1)
            throw new RuntimeException("Cannot setPath, the supplied path was not registered in the supplied map!");
        setPathId(newIndex, resetDistance);
    }

    /**
     * Sets the new path to use.
     * @param newPath The new path.
     * @param segment The new segment to use.
     */
    public void setPath(FroggerPath newPath, FroggerPathSegment segment) {
        setPath(newPath, true);
        setSegmentId(newPath.getSegments().indexOf(segment));
    }

    /**
     * Returns the path object associated with this path info. If the path doesn't exist, return null.
     * @return pathObject
     */
    public FroggerPath getPath() {
        return getPathId() >= 0 && this.mapFile.getPathPacket().getPaths().size() > getPathId() ? this.mapFile.getPathPacket().getPaths().get(getPathId()) : null;
    }

    /**
     * Gets the total path distance this info is at. Note this is total path distance, not segment distance.
     * @return totalPathDistance
     */
    public int getTotalPathDistance() {
        FroggerPath path = getPath();
        int totalDistance = getSegmentDistance(); // Get current segment's distance.
        for (int i = 0; i < getSegmentId(); i++) // Include the distance from all previous segments.
            totalDistance += path.getSegments().get(i).getLength();
        return totalDistance;
    }

    /**
     * Updates the distance this is along the path. Note this uses total path distance not segment distance.
     * @param totalDistance The total path distance.
     */
    public void setTotalPathDistance(int totalDistance) {
        FroggerPath path = getPath();
        for (int i = 0; i < path.getSegments().size(); i++) {
            FroggerPathSegment segment = path.getSegments().get(i);
            if (totalDistance > segment.getLength()) {
                totalDistance -= segment.getLength();
            } else { // Found it!
                this.segmentId = i;
                this.segmentDistance = totalDistance;
                break;
            }
        }
    }

    /**
     * Test if a flag is present.
     * @param type The flag to test.
     * @return hasFlag
     */
    public boolean testFlag(FroggerPathMotionType type) {
        return (this.motionType & type.getFlagBitMask()) == type.getFlagBitMask();
    }

    /**
     * Set the flag state.
     * @param flag     The flag type.
     * @param newState The new state of the flag.
     */
    public void setFlag(FroggerPathMotionType flag, boolean newState) {
        boolean oldState = testFlag(flag);
        if (oldState == newState)
            return; // Prevents the ^ operation from breaking the value.

        if (newState) {
            this.motionType |= flag.getFlagBitMask();
        } else {
            this.motionType ^= flag.getFlagBitMask();
        }
    }

    /**
     * Creates the path info editor.
     * @param manager The manager managing the display of entities.
     * @param entityData The entity data which this path info is found on.
     * @param editorGrid The editor grid to build the UI with.
     */
    public void setupEditor(FroggerUIMapEntityManager manager, FroggerEntityDataPathInfo entityData, GUIEditorGrid editorGrid) {
        FroggerMapFile mapFile = manager.getMap();
        FroggerMapEntity entity = entityData.getParentEntity();
        List<FroggerPath> mapPaths = mapFile.getPathPacket().getPaths();

        if (this.pathId < 0 || this.pathId >= mapPaths.size()) { // Invalid path! Show this as a text box.
            editorGrid.addUnsignedShortField("Path ID", this.pathId, newPathId -> newPathId < mapPaths.size(), newPathId -> {
                this.pathId = newPathId;
                manager.updateEntityPositionRotation(entity);
                manager.updateEditor(); // Update the entity editor display, update path slider, etc.
            });
        } else { // Otherwise, show it as a selection box!
            editorGrid.addBoldLabelButton("Path " + this.pathId, "Select Path", 25, () ->
                    manager.getController().getPathManager().getPathSelector().promptPath((path, segment, segDistance) -> {
                        setPath(path, segment);
                        setSegmentDistance(segDistance);
                        manager.updateEntityPositionRotation(entity);
                        manager.updateEditor(); // Update the entity editor display, update path slider, etc.
                    }, null));
        }

        editorGrid.addUnsignedShortField("Path Speed (???)", this.speed, newSpeed -> this.speed = newSpeed); // TODO: FIXED POINT?

        FroggerPath path = getPath();
        if (path != null) {
            final float distAlongPath = Utils.fixedPointIntToFloat4Bit(getTotalPathDistance());
            final float totalPathDist = path.calculateTotalLengthFloat();

            Slider travDistSlider = editorGrid.addDoubleSlider("Travel Distance:", distAlongPath, newValue -> {
                setTotalPathDistance(Utils.floatToFixedPointInt4Bit(newValue.floatValue()));
                manager.updateEntityPositionRotation(entity);
            }, 0.0, totalPathDist);
            TextField travDistText = editorGrid.addFloatField("", distAlongPath, newValue -> {
                setTotalPathDistance(Utils.floatToFixedPointInt4Bit(newValue));
                manager.updateEntityPositionRotation(entity);
            }, newValue -> !((newValue < 0.0f) || (newValue > totalPathDist)));
            travDistText.textProperty().bindBidirectional(travDistSlider.valueProperty(), new NumberStringConverter(new DecimalFormat("####0.00")));

            // Show max travel distance.
            TextField txtFieldMaxTravel = editorGrid.addFloatField("(Max. Travel):", totalPathDist);
            txtFieldMaxTravel.setEditable(false);
            txtFieldMaxTravel.setDisable(true);
        } else {
            editorGrid.addBoldLabel("Error: Invalid Path ID!");
            editorGrid.addNormalLabel("The path data is not valid until a proper path ID is selected.");
        }

        // Motion Data:
        for (FroggerPathMotionType type : FroggerPathMotionType.values())
            if (type.isAllowEdit())
                editorGrid.addCheckBox(type.getLongDisplayName(), testFlag(type), newState -> setFlag(type, newState));
    }

    @Getter
    @AllArgsConstructor
    public enum FroggerPathMotionType {
        ACTIVE(Constants.BIT_FLAG_0, false, "Movement Enabled"),
        BACKWARDS(Constants.BIT_FLAG_1, false, "Backwards Direction"),
        ONE_SHOT(Constants.BIT_FLAG_2, false, "One Shot"),
        REPEAT(Constants.BIT_FLAG_3, true, "Repeat (When end of path is reached)"),
        FINISHED(Constants.BIT_FLAG_4, false, "Finished Following Path");

        private final int flagBitMask;
        private final boolean allowEdit; // If this isn't true, this flag only exists at runtime.
        private final String longDisplayName;
        public static final int FLAG_VALIDATION_MASK = 0b11111;
    }
}