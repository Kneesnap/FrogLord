package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import javafx.geometry.Point3D;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseInputState;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.LazyInstanceLogger;

import java.util.List;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class FroggerPathSegment extends SCGameData<FroggerGameInstance> {
    private final FroggerPath path;
    private final FroggerPathSegmentType type;
    private int length;

    public FroggerPathSegment(FroggerPath path, FroggerPathSegmentType type) {
        super(path.getGameInstance());
        this.path = path;
        this.type = type;
    }

    @Override
    public void load(DataReader reader) {
        if (!this.path.isOldPathFormatEnabled())
            this.length = reader.readInt();
        this.loadData(reader);

        // Handle length.
        if (this.path.isOldPathFormatEnabled()) {
            recalculateLength(null);
        } else {
            int newLength = calculateFixedPointLength();
            int diffLength = newLength - this.length;
            if (Math.abs(diffLength) > getIncorrectLengthTolerance()) {
                String extraMessage = getCalculatedIncorrectLengthString();
                getLogger().warning("calculateFixedPointLength() was inaccurate! [Read Length: %d/%f, Calculated Length: %d/%f, Diff: %d/%f%s]",
                        this.length, DataUtils.fixedPointIntToFloat4Bit(this.length),
                        newLength, DataUtils.fixedPointIntToFloat4Bit(newLength),
                        diffLength, DataUtils.fixedPointIntToFloat4Bit(diffLength),
                        extraMessage != null && extraMessage.length() > 0 ? ", " + extraMessage : "");
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getType().ordinal());
        if (!this.path.isOldPathFormatEnabled())
            writer.writeInt(this.length);
        saveData(writer);
    }

    /**
     * Gets the index of the path segment within the path.
     */
    public int getPathSegmentIndex() {
        return this.path != null ? this.path.getSegments().lastIndexOf(this) : -1;
    }

    /**
     * Gets information about the logger.
     */
    public String getLoggerInfo() {
        return this.path != null ? this.path.getLoggerInfo() + "|FroggerPathSegment{" + getPathSegmentIndex() + "|" + this.type + "}" : Utils.getSimpleName(this);
    }

    @Override
    public ILogger getLogger() {
        return new LazyInstanceLogger(getGameInstance(), FroggerPathSegment::getLoggerInfo, this);
    }

    /**
     * Helper method to copy this segment to a new segment
     */
    public FroggerPathSegment clone(FroggerPath newPath) {
        byte[] rawData = writeDataToByteArray();
        FroggerPathSegment newSegment = this.type.makeNew(newPath);
        newPath.getSegments().add(newSegment);

        DataReader reader = new DataReader(new ArraySource(rawData));
        reader.skipInt(); // Skip the type.
        newSegment.load(reader);

        return newSegment;
    }

    /**
     * Helper method to move the segment according to the delta. Used with the "Move All" control.
     */
    public abstract void moveDelta(SVector delta);

    /**
     * Helper method to reverse the segment so its end-point becomes its start point.
     */
    public abstract void flip();

    /**
     * Setup this segment at the end of the given path.
     */
    public abstract void setupNewSegment();

    /**
     * Load segment specific data.
     * @param reader Data source.
     */
    protected abstract void loadData(DataReader reader);

    /**
     * Write segment specific data.
     * @param writer the receiver to write data to.
     */
    protected abstract void saveData(DataWriter writer);

    /**
     * Calculate the position after a path is completed.
     * @param segmentDistance The segment distance to calculate.
     * @return finishPosition
     */
    public abstract FroggerPathResult calculatePosition(int segmentDistance);

    /**
     * Calculate the position after a path is completed.
     * @param pathInfo The info to calculate with.
     * @return finishPosition
     */
    public FroggerPathResult calculatePosition(FroggerPathInfo pathInfo) {
        if (pathInfo == null)
            throw new NullPointerException("pathInfo");

        return calculatePosition(pathInfo.getSegmentDistance());
    }

    /**
     * Recalculates the length of this segment.
     */
    public void recalculateLength(FroggerPathPreview pathPreview) {
        setLength(pathPreview, calculateFixedPointLength());
    }

    /**
     * Calculates the segment length in fixed point form.
     */
    public abstract int calculateFixedPointLength();

    /**
     * Called to warn that we've calculated an incorrect segment length.
     */
    protected abstract String getCalculatedIncorrectLengthString();

    /**
     * Gets the amount of allowed inaccuracy in calculated paths before throwing a warning.
     */
    protected abstract int getIncorrectLengthTolerance();

    /**
     * Gets the start position of this segment.
     * @return startPosition
     */
    public abstract SVector getStartPosition();

    /**
     * Setup a path editor.
     * @param pathPreview The path preview.
     * @param editor The editor to setup.
     */
    public void setupEditor(FroggerPathPreview pathPreview, GUIEditorGrid editor) {
        editor.addLabel("Type:", getType().name(), 25);
        TextField segmentLengthField = editor.addFloatField("Segment Length:", DataUtils.fixedPointIntToFloat4Bit(getLength()), isAllowLengthEdit() ? newVal -> {
            setLength(pathPreview, DataUtils.floatToFixedPointShort4Bit(newVal));
            onManualLengthUpdate(pathPreview, editor);
            updateDisplay(pathPreview); // Don't call onUpdate because that will recalculate length.
        } : null, null); // Read-Only.
        pathPreview.setPathSegmentLengthField(segmentLengthField);

        editor.addButton("Flip", () -> {
            flip();
            onUpdate(pathPreview);
        });
    }

    /**
     * Updates the viewer UI when this segment is updated.
     * @param pathPreview the path preview to update the path for
     */
    public void onUpdate(FroggerPathPreview pathPreview) {
        recalculateLength(pathPreview);
        if (pathPreview != null)
            updateDisplay(pathPreview);
    }

    public void updateDisplay(FroggerPathPreview pathPreview) {
        pathPreview.updatePath();
        updateSegmentEntities(pathPreview.getController().getEntityManager());
    }

    private void updateSegmentEntities(FroggerUIMapEntityManager entityManager) {
        int segmentIndex = getPathSegmentIndex();
        List<FroggerMapEntity> pathEntities = getPath().getPathEntities();
        for (int i = 0; i < pathEntities.size(); i++) {
            FroggerMapEntity entity = pathEntities.get(i);
            FroggerPathInfo pathInfo = entity.getPathInfo();
            if (pathInfo != null && pathInfo.getSegmentId() == segmentIndex)
                entityManager.updateEntityPositionRotation(entity);
        }
    }

    /**
     * Allows using the 'Select' button to update a position.
     * @param pathPreview the pathPreview to update
     * @param vector the vector to reposition
     * @param bits the number bits of precision.
     */
    protected void selectPathPosition(FroggerPathPreview pathPreview, Vector vector, int bits, Runnable onFinish) {
        FroggerMapMeshController frogController = pathPreview.getController();
        frogController.getBakedGeometryManager().getPolygonSelector().activate(polygon -> {
            InputManager inputManager = pathPreview.getController().getInputManager();
            if (inputManager.isKeyPressed(KeyCode.CONTROL)) {
                MouseInputState mouseState = inputManager.getMouseTracker().getMouseState();
                Point3D intersectedPoint = new Point3D(mouseState.getIntersectedPoint().getX(), mouseState.getIntersectedPoint().getY(), mouseState.getIntersectedPoint().getZ());
                Point3D newWorldPos = mouseState.getIntersectedNode().localToScene(intersectedPoint);
                vector.setFloatX(Math.round(newWorldPos.getX() / 4F) * 4F, bits);
                vector.setFloatY(Math.round(newWorldPos.getY() / 4F) * 4F, bits);
                vector.setFloatZ(Math.round(newWorldPos.getZ() / 4F) * 4F, bits);
            } else {
                Vector3f centerOfPolygon = polygon.getCenterOfPolygon(null);
                vector.setFloatX(centerOfPolygon.getX(), bits);
                vector.setFloatY(centerOfPolygon.getY(), bits);
                vector.setFloatZ(centerOfPolygon.getZ(), bits);
            }

            onUpdate(pathPreview);
            frogController.getPathManager().updateEditor();
            if (onFinish != null)
                onFinish.run();
        }, null);
    }

    /**
     * Sets the length of this segment.
     * @param newFixedPointSegmentLength The new segment length in fixed point form
     */
    @SuppressWarnings("ConstantConditions")
    public void setLength(FroggerPathPreview pathPreview, int newFixedPointSegmentLength) {
        this.length = newFixedPointSegmentLength;

        if (pathPreview != null) {
            // Update length field.
            pathPreview.getPathSegmentLengthField().setText(String.valueOf(DataUtils.fixedPointIntToFloat4Bit(newFixedPointSegmentLength)));

            // Update main path length display.
            if (pathPreview.getPathManager().getSelectedValue() == getPath())
                pathPreview.getPathManager().getFullPathLengthField().setText(Float.toString(getPath().calculateTotalLengthFloat()));
        }
    }

    /**
     * Whether editing the length text box is allowed.
     */
    public boolean isAllowLengthEdit() {
        return false;
    }

    /**
     * Called when length is manually updated.
     */
    public void onManualLengthUpdate(FroggerPathPreview pathPreview, GUIEditorGrid editor) {

    }
}