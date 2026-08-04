package net.highwayfrogs.editor.games.sony.frogger.map.data.path;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.util.converter.NumberStringConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.math.vector.IVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.math.PSXMatrix;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.SCMath;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity.FroggerMapEntityFlag;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack.FroggerGridStackInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the PATH_INFO struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerPathInfo extends SCGameData<FroggerGameInstance> {
    private final FroggerMapFile mapFile;
    private int pathId = -1;
    @Setter private int segmentId;
    @Setter private int segmentDistance;
    private int motionType = FroggerPathMotionType.REPEAT.getFlagBitMask();
    @Setter private int speed = 10; // This is NOT a per-path property, sometimes entities on the same path have different speeds, such as the beavers in ORG1.

    public FroggerPathInfo(FroggerMapFile mapFile) {
        super(mapFile != null ? mapFile.getGameInstance() : null);
        this.mapFile = mapFile;
    }

    @Override
    public void load(DataReader reader) {
        this.pathId = reader.readUnsignedShortAsInt(); // Validated in entity loading logic because we want to identify the entity who loads with this data.
        this.segmentId = reader.readUnsignedShortAsInt();
        this.segmentDistance = reader.readUnsignedShortAsInt();
        this.motionType = reader.readUnsignedShortAsInt();
        warnAboutInvalidBitFlags(this.motionType, FroggerPathMotionType.FLAG_VALIDATION_MASK);
        this.speed = reader.readUnsignedShortAsInt();
        reader.alignRequireEmpty(Constants.INTEGER_SIZE); // Padding.
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.pathId); // Validated in entity saving logic because we want to identify the entity who saves with this data.
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
     * @see #setTotalPathDistance(int, boolean, boolean) for param explanations.
     */
    public void setTotalPathDistance(int totalDistance, boolean applyPathRunnerLogic) {
        setTotalPathDistance(totalDistance, applyPathRunnerLogic, false);
    }

    /**
     * Updates the distance this is along the path. Note this uses total path distance not segment distance.
     * @param totalDistance The total path distance.
     * @param applyPathRunnerLogic If true, repeat/reset behavior will apply.
     * @param treatOneShotAsRepeat Used for Distribute Evenly button to treat the stop flag as repeat for calculations.
     */
    public void setTotalPathDistance(int totalDistance, boolean applyPathRunnerLogic, boolean treatOneShotAsRepeat) {
        if (totalDistance < 0 && !applyPathRunnerLogic)
            throw new IllegalArgumentException("Cannot apply totalPathDistance of " + totalDistance + " to FroggerPathInfo. (Negative values are only allowed when using pathing logic.)");

        FroggerPath path = getPath();
        int fullPathDistance = 0;
        int remainingDistance = totalDistance;
        for (int i = 0; i < path.getSegments().size(); i++) {
            FroggerPathSegment segment = path.getSegments().get(i);
            int segmentLength = segment.getLength();
            fullPathDistance += segmentLength;
            if (totalDistance < 0)
                continue; // Hack to just calculate the path length when the distance is negative.

            if (remainingDistance > segmentLength) {
                remainingDistance -= segmentLength;
            } else { // Found it!
                this.segmentId = i;
                this.segmentDistance = remainingDistance;
                return;
            }
        }

        // If we're still here, we've reached the end of the path.
        if (!applyPathRunnerLogic)
            throw new IllegalArgumentException("Cannot apply a path distance of " + totalDistance + ", as it exceeds the full path length of " + fullPathDistance + ".");

        boolean backwards = testFlag(FroggerPathMotionType.BACKWARDS); // If this is false, we've reached the start of the path.
        int distanceAfterEnd = remainingDistance % fullPathDistance;

        if (testFlag(FroggerPathMotionType.REPEAT) || (treatOneShotAsRepeat && testFlag(FroggerPathMotionType.ONE_SHOT))) {
            // Reset to the start of the path.
            setTotalPathDistance(backwards ? fullPathDistance + distanceAfterEnd : distanceAfterEnd, false);
        } else if (testFlag(FroggerPathMotionType.ONE_SHOT)) {
            this.segmentId = backwards ? 0 : path.getSegments().size() - 1;
            this.segmentDistance = backwards ? 0 : path.getSegments().get(this.segmentId).getLength();
            setFlag(FroggerPathMotionType.FINISHED, true);
        } else { // Bounce.
            setFlag(FroggerPathMotionType.BACKWARDS, !backwards);
            setTotalPathDistance(backwards ? -distanceAfterEnd : fullPathDistance - distanceAfterEnd, false);
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
     * Evaluates the position and rotation for the current path state.
     * @param entity the entity to calculate the path position/rotation for. (Optional, but disables entity-based calculations if omitted)
     * @param position the storage vector for the position, can be null to skip position calculations
     * @param rotation the storage vector for the rotation, can be null to skip rotation calculations
     * @return was successful
     */
    public boolean evaluatePositionAndRotation(FroggerMapEntity entity, Vector3f position, Vector3f rotation) {
        if (position == null && rotation == null)
            throw new NullPointerException("At least one of position or rotation must not be null.");

        FroggerPath path = getPath();
        if (path == null)
            return false; // Invalid path id.

        // Reimplements ENTITY.C/ENTSTRUpdateMovingMOF()
        FroggerPathResult result = path.evaluatePosition(this);

        // Don't rotate if we're aligned to the world.
        SVector pathPosition = result.getPosition();
        if (position != null)
            position.setXYZ(pathPosition.getFloatX(), pathPosition.getFloatY(), pathPosition.getFloatZ());

        // Example: The spinners in VOL1.MAP don't rotate to follow the path despite following it. Also the pink balloons.
        if (entity != null && entity.testFlag(FroggerMapEntityFlag.ALIGN_TO_WORLD)) {
            if (rotation != null)
                rotation.setXYZ(0, 0, 0);
            return true;
        }

        IVector vecZ = new IVector(result.getRotation());
        if (entity != null && entity.testFlag(FroggerMapEntityFlag.PROJECT_ON_LAND)) { // Example: Hedgehogs and many of the jungle entities.
            FroggerMapFilePacketGrid gridPacket = getMapFile().getGridPacket();
            int gridX = gridPacket.getGridXFromWorldX(pathPosition.getX());
            int gridZ = gridPacket.getGridZFromWorldZ(pathPosition.getZ());

            if (gridPacket.isValidGridCoordinate(gridX, gridZ)) {
                FroggerGridStack gridStack = gridPacket.getGridStack(gridX, gridZ);
                FroggerGridStackInfo stackInfo = gridStack != null ? gridStack.getGridStackInfo() : null;
                if (stackInfo != null) {
                    int dx = stackInfo.getXSlope().dotProduct(vecZ) >> 12;
                    int dz = stackInfo.getZSlope().dotProduct(vecZ) >> 12;
                    if (position != null)
                        position.setY(stackInfo.getY() >> 4); // Snap to the ground.
                    vecZ.setX(((stackInfo.getXSlope().getX() * dx) + (stackInfo.getZSlope().getX() * dz)) >> 12);
                    vecZ.setY(((stackInfo.getXSlope().getY() * dx) + (stackInfo.getZSlope().getY() * dz)) >> 12);
                    vecZ.setZ(((stackInfo.getXSlope().getZ() * dx) + (stackInfo.getZSlope().getZ() * dz)) >> 12);
                }
            } else {
                entity.getLogger().warning("I'm located outside of the collision grid, but snap to the grid! This may crash the game!");
            }
        }

        if (rotation == null)
            return true; // Only rotation calculations occur past this point.

        IVector vecX = new IVector();
        if (entity != null && entity.testFlag(FroggerMapEntityFlag.LOCAL_ALIGN)) {
            // I don't think this is entirely correct to how it works in-game, but it captures the spirit of what the code in-game is supposed to do.
            // After the player dies or collects a checkpoint, some entities rotate weirdly. (It might take a few tries) For example,
            // There's one slug in SWP4.MAP which absolutely refuses to cooperate. All the other ones appear right.

            vecX.clear();
            if (Math.abs(vecZ.getFloatX()) > .5F) { // In PSXMatrix, matrix[2][0] = vecZ.x
                vecX.setZ(SCMath.FIXED_POINT_ONE);
            } else {
                vecX.setX(SCMath.FIXED_POINT_ONE);
            }
        } else {
            // ENTITY_BOOK_XZ_PARALLEL_TO_CAMERA applies sprite billboarding, but FrogLord doesn't need to show that.
            IVector.MROuterProduct12(SCMath.GAME_Y_AXIS_POS, vecZ, vecX);
        }


        vecX.normalise();
        IVector vecY = new IVector();
        IVector.MROuterProduct12(vecZ, vecX, vecY);
        PSXMatrix matrix = PSXMatrix.WriteAxesAsMatrix(new PSXMatrix(), vecX, vecY, vecZ);

        rotation.setX((float) matrix.getPitchAngle());
        rotation.setY((float) matrix.getYawAngle());
        rotation.setZ((float) matrix.getRollAngle());
        return true;
    }

    /**
     * Creates a copy of the FroggerPathInfo
     */
    public FroggerPathInfo clone() {
        FroggerPathInfo newPathInfo = new FroggerPathInfo(this.mapFile);
        newPathInfo.pathId = this.pathId;
        newPathInfo.segmentId = this.segmentId;
        newPathInfo.segmentDistance = this.segmentDistance;
        newPathInfo.motionType = this.motionType;
        newPathInfo.speed = this.speed;
        return newPathInfo;
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
                getMapFile().getPathPacket().removeEntityFromPathTracking(entity);
                this.pathId = newPathId;
                getMapFile().getPathPacket().addEntityToPathTracking(entity);
                manager.updateEntityPositionRotation(entity);
                manager.updateEditor(); // Update the entity editor display, update path slider, etc.
            });
            editorGrid.addBoldLabelButton("Or alternatively: ", "Select Path", 25,
                    () -> manager.setSelectedMouseEntity(entity));
        } else { // Otherwise, show it as a selection box!
            editorGrid.addBoldLabelButton("Path " + this.pathId, "Select Path", 25,
                    () -> manager.setSelectedMouseEntity(entity));
        }

        editorGrid.addUnsignedFixedShort("Speed (distance/frame)", this.speed, newSpeed -> {
            this.speed = newSpeed;
            manager.getController().getPathManager().updateEditor(); // Update path speed UI.
        }, 16);

        FroggerPath path = getPath();
        if (path != null) {
            final float distAlongPath = DataUtils.fixedPointIntToFloat4Bit(getTotalPathDistance());
            final float totalPathDist = path.calculateTotalLengthFloat();

            Slider travDistSlider = editorGrid.addDoubleSlider("Travel Distance:", distAlongPath, newValue -> {
                setTotalPathDistance(DataUtils.floatToFixedPointInt4Bit(newValue.floatValue()), false);
                manager.updateEntityPositionRotation(entity);
            }, 0.0, totalPathDist);
            TextField travDistText = editorGrid.addFloatField("", distAlongPath, newValue -> {
                setTotalPathDistance(DataUtils.floatToFixedPointInt4Bit(newValue), false);
                manager.updateEntityPositionRotation(entity);
            }, newValue -> !((newValue < 0.0f) || (newValue > totalPathDist)));
            travDistText.textProperty().bindBidirectional(travDistSlider.valueProperty(), new NumberStringConverter(new DecimalFormat("####0.00")));

            // Show max travel distance.
            TextField txtFieldMaxTravel = editorGrid.addFloatField("(Max. Travel):", totalPathDist);
            txtFieldMaxTravel.setEditable(false);
            txtFieldMaxTravel.setDisable(true);
        } else {
            editorGrid.addBoldLabel("Error: Invalid Path ID!");
            editorGrid.addNormalLabel("Please select a valid path.");
        }

        // Motion Data:
        for (FroggerPathMotionType type : FroggerPathMotionType.values())
            if (type.isEditorCheckBoxShown())
                editorGrid.addCheckBox(type.getLongDisplayName(), testFlag(type), newState -> {
                    setFlag(type, newState);
                    manager.updateEntityPositionRotation(entity); // These flags can control how the entity appears.
                }).setTooltip(FXUtils.createTooltip(type.getTooltipText()));

        FroggerEndOfPathBehavior endOfPathBehavior = FroggerEndOfPathBehavior.getBehavior(this);
        ComboBox<FroggerEndOfPathBehavior> endOfPathSelector = editorGrid.addEnumSelector("End of Path Behavior", endOfPathBehavior, FroggerEndOfPathBehavior.values(), false, newValue -> {
            setFlag(FroggerPathMotionType.ONE_SHOT, newValue.isOneShotFlagSet());
            setFlag(FroggerPathMotionType.REPEAT, newValue.isRepeatFlagSet());
        });
        endOfPathSelector.setConverter(new AbstractStringConverter<>(FroggerEndOfPathBehavior::getDisplayName));
        endOfPathSelector.setCellFactory(listView -> new LazyFXListCell<>(FroggerEndOfPathBehavior::getDisplayName, "Error")
                .setWithoutIndexTooltipHandler(behavior -> behavior != null ? FXUtils.createTooltip(behavior.getTooltipText()) : null));
    }

    /**
     * Creates the tools section of path info editor.
     * In the future this may be moved out of this class, but right now the only tool is related to paths
     * This is separate from setupEditor because we want this section after all entity data fields
     * @param manager The manager managing the display of entities.
     * @param editorGrid The editor grid to build the UI with.
     */
    public void setupEditorTools(FroggerUIMapEntityManager manager, GUIEditorGrid editorGrid) {
        FroggerPath path = getPath();
        if (path != null) {
            // There's only one tool currently, but the button makes more sense to be in its own section
            editorGrid.addSeparator(15);
            editorGrid.addBoldLabel("Tools:",20);
            String distributeText = "Distribute Evenly";
            Button distributeButton = editorGrid.addButton(distributeText, null);
            distributeButton.setTooltip(new Tooltip("Readjust every entity on this path to an even distribution, using this entity as the base."));
            // Reset the confirmation if clicked elsewhere
            distributeButton.focusedProperty().addListener((evt, oldValue, newValue) -> {
                if (!newValue) {
                    distributeButton.setText(distributeText);
                    distributeButton.setStyle("");
                }
            });
            // Create an even distribution of all entities on the current path
            // This button is in the entity section because the currently selected entity will be used as the base
            distributeButton.setOnAction(evt -> {
                if (path.getPathEntities() == null)
                    return;

                // Ask for a double click so users don't accidentally apply this
                if (distributeButton.getText().equals(distributeText)) {
                    distributeButton.setText("Click to Confirm");
                    distributeButton.setStyle("-fx-body-color: #FF7F7F; -fx-text-fill: #4C0707;");
                    return;
                }

                boolean selectedIsBackwards = testFlag(FroggerPathMotionType.BACKWARDS);
                int originalLength = path.calculateTotalLength();
                int totalLength;
                // If the entities reverse direction, that's essentially double the path length since it's out and back
                boolean selectedIsReversed = !(testFlag(FroggerPathMotionType.REPEAT) || testFlag(FroggerPathMotionType.ONE_SHOT));
                if (selectedIsReversed) {
                    totalLength = originalLength * 2;
                } else {
                    totalLength = originalLength;
                }
                int entityCountOnPath = path.getPathEntities().size();

                // The distance value of the currently selected entity
                // (Start the chain where the selected entity is)
                int selectedDistance;
                // Treat backwards and reverse as a double length path, and backward repeat as if it is forwards
                if (selectedIsBackwards && selectedIsReversed) {
                    selectedDistance = (originalLength - getTotalPathDistance()) + originalLength;
                } else {
                    selectedDistance = getTotalPathDistance();
                }

                // Create a copy of the list so entity removal binary search works correctly
                List<FroggerMapEntity> tempEntityList = new ArrayList<>(path.getPathEntities());

                // Order the list so distribute produces a predictable result
                tempEntityList.sort(Comparator.comparingInt(orderEntity -> {
                    FroggerPathInfo orderPathInfo = orderEntity.getPathInfo();
                    int distance = orderPathInfo.getTotalPathDistance();
                    // Treat backwards and reverse as a double length path, and backward repeat as if it is forwards
                    if (orderPathInfo.testFlag(FroggerPathMotionType.BACKWARDS) && !(orderPathInfo.testFlag(FroggerPathMotionType.REPEAT) || orderPathInfo.testFlag(FroggerPathMotionType.ONE_SHOT))) {
                        distance = (originalLength - distance) + originalLength;
                    }
                    // Ensure that the selected entity is always first in the list, so it doesn't move
                    while (distance < selectedDistance) {
                        distance += originalLength * 2;
                    }
                    return distance;
                }));

                // Rearrange every entity on this path in order
                for (int i = 0; i < entityCountOnPath; i++) {
                    FroggerMapEntity sortEntity = tempEntityList.get(i);
                    if (sortEntity != null) {
                        FroggerPathInfo pathData = sortEntity.getPathInfo();
                        // If selected is reversed, apply reverse to everything
                        if (selectedIsReversed) {
                            boolean isEntityBackwards = false;
                            int newSpot = selectedDistance + ((totalLength / entityCountOnPath) * i);
                            // Subtract the path length until the entity is within the maximum length
                            while (newSpot > originalLength) {
                                newSpot -= originalLength;
                                // Keep track of whether we need to make this entity backwards
                                isEntityBackwards = !isEntityBackwards;
                            }
                            // The flags must be correct before we set the distance
                            pathData.setFlag(FroggerPathMotionType.BACKWARDS, isEntityBackwards);
                            pathData.setFlag(FroggerPathMotionType.REPEAT, false);
                            pathData.setFlag(FroggerPathMotionType.ONE_SHOT, false);
                            if (isEntityBackwards) {
                                pathData.setTotalPathDistance(-originalLength + newSpot, true);
                            } else {
                                pathData.setTotalPathDistance(newSpot, true);
                            }
                            // For whatever reason this needs to be set again
                            pathData.setFlag(FroggerPathMotionType.BACKWARDS, isEntityBackwards);
                        } else {
                            // The flags must be correct before we set the distance
                            // Set all backwards as forwards when applying, and then
                            // set all to backwards at the end if selected is also backward
                            pathData.setFlag(FroggerPathMotionType.BACKWARDS, false);
                            pathData.setFlag(FroggerPathMotionType.REPEAT, FroggerEndOfPathBehavior.getBehavior(this).repeatFlagSet);
                            pathData.setFlag(FroggerPathMotionType.ONE_SHOT, FroggerEndOfPathBehavior.getBehavior(this).oneShotFlagSet);
                            int newSpot = selectedDistance + ((totalLength / entityCountOnPath) * i);
                            pathData.setTotalPathDistance(newSpot, true, true);
                            pathData.setFlag(FroggerPathMotionType.BACKWARDS, selectedIsBackwards);
                        }
                        manager.updateEntityPositionRotation(sortEntity);
                    }
                }
                manager.updateEditor();
            });
            // Disable button if only one entity is on the path
            if (path.getPathEntities() != null && path.getPathEntities().size() <= 1) {
                distributeButton.setDisable(true);
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum FroggerPathMotionType {
        ACTIVE(Constants.BIT_FLAG_0, false, "Movement Enabled", "Automatically assigned to all path runners in-game upon creation."), // No point to assign it here. (Runtime Only)
        BACKWARDS(Constants.BIT_FLAG_1, true, "Backwards Direction", "The entity moves in the backwards direction (towards start of path)."), // This is rare, but IS seen in PC retail.
        ONE_SHOT(Constants.BIT_FLAG_2, false, "One Shot (Unused)", "The entity stops moving upon reaching the end of the path.\nNever seen in the original game."),
        REPEAT(Constants.BIT_FLAG_3, false, "Repeat at End (No Reverse)", "Restarts from the start of the path instead of reversing direction."),
        FINISHED(Constants.BIT_FLAG_4, false, "Finished", "Unused by the game, assigned at runtime."); // Doesn't even serve a purpose in the code. (Runtime Only)

        private final int flagBitMask;
        private final boolean editorCheckBoxShown; // If this is false, hide the flag from the user and prevent editing.
        private final String longDisplayName;
        private final String tooltipText;
        public static final int FLAG_VALIDATION_MASK = 0b01110;
    }

    @Getter
    @RequiredArgsConstructor
    private enum FroggerEndOfPathBehavior {
        REVERSE_DIRECTION(false, false, "Reverse", "Reverses direction and continues moving."),
        RESTART(false, true, "Restart", "Teleports to the start of the path."),
        STOP(true, false, "Stop", "Stops moving upon reaching the end.\nThis is not used in the original game.");

        private final boolean oneShotFlagSet;
        private final boolean repeatFlagSet;
        private final String displayName;
        private final String tooltipText;

        private static FroggerEndOfPathBehavior getBehavior(FroggerPathInfo pathInfo) {
            if (pathInfo == null)
                throw new NullPointerException("pathInfo");

            boolean oneShotFlag = pathInfo.testFlag(FroggerPathMotionType.ONE_SHOT);
            boolean repeatFlag = pathInfo.testFlag(FroggerPathMotionType.REPEAT);
            for (int i = 0; i < values().length; i++) {
                FroggerEndOfPathBehavior behavior = values()[i];
                if (oneShotFlag == behavior.oneShotFlagSet && repeatFlag == behavior.repeatFlagSet)
                    return behavior;
            }

            throw new UnsupportedOperationException("Cannot find behavior with oneShotFlag: " + oneShotFlag + ", and repeatFlag: " + repeatFlag + "!");
        }
    }
}