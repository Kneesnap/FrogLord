package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.SCGameData;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathResult;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils;
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
                getLogger().warning("calculateFixedPointLength() was inaccurate! [Read Length: " + this.length + "/" + DataUtils.fixedPointIntToFloat4Bit(this.length)
                        + ", Calculated Length: " + newLength + "/" + DataUtils.fixedPointIntToFloat4Bit(this.length)
                        + ", Diff: " + diffLength + "/" + DataUtils.fixedPointIntToFloat4Bit(diffLength)
                        + (extraMessage != null && extraMessage.length() > 0 ? ", " + extraMessage : "") + "]");
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
     * @param info The info to calculate with.
     * @return finishPosition
     */
    public abstract FroggerPathResult calculatePosition(FroggerPathInfo info);

    /**
     * Calculate the position along this segment.
     * @param distance The distance along this segment.
     * @return pathResult
     */
    public FroggerPathResult calculatePosition(int distance) {
        FroggerPathInfo fakeInfo = new FroggerPathInfo(this.path.getMapFile());
        fakeInfo.setPath(this.path, this);
        fakeInfo.setSegmentDistance(distance);
        return calculatePosition(fakeInfo);
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
        segmentLengthField.setDisable(true);
        pathPreview.setPathSegmentLengthField(segmentLengthField);
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

    private void updateDisplay(FroggerPathPreview pathPreview) {
        pathPreview.updatePath();
        updateSegmentEntities(pathPreview.getController().getEntityManager());
    }

    private void updateSegmentEntities(FroggerUIMapEntityManager entityManager) {
        int pathIndex = getPath().getPathIndex();
        int segmentIndex = getPathSegmentIndex();
        List<FroggerMapEntity> mapEntities = entityManager.getMap().getEntityPacket().getEntities();
        for (int i = 0; i < mapEntities.size(); i++) {
            FroggerMapEntity entity = mapEntities.get(i);
            FroggerPathInfo pathInfo = entity.getPathInfo();
            if (pathInfo != null && pathInfo.getPathId() == pathIndex && pathInfo.getSegmentId() == segmentIndex)
                entityManager.updateEntityPositionRotation(entity);
        }
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