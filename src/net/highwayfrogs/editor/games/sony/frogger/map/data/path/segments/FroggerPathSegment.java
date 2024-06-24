package net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.FroggerMapConfig;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
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
import net.highwayfrogs.editor.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.logging.Logger;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class FroggerPathSegment extends SCGameData<FroggerGameInstance> {
    private final FroggerPath path;
    private final FroggerPathSegmentType type;
    private int length;
    private transient WeakReference<TextField> lengthField;

    public FroggerPathSegment(FroggerPath path, FroggerPathSegmentType type) {
        super(path.getGameInstance());
        this.path = path;
        this.type = type;
    }

    @Override
    public void load(DataReader reader) {
        FroggerMapConfig mapConfig = this.path.getMapFile().getMapConfig();

        if (!mapConfig.isOldPathFormat())
            this.length = reader.readInt();
        this.loadData(reader);
        if (mapConfig.isOldPathFormat())
            recalculateLength();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getType().ordinal());
        if (!this.path.getMapFile().getMapConfig().isOldPathFormat())
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
    public Logger getLogger() {
        return Logger.getLogger(getLoggerInfo());
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
    public abstract void recalculateLength();

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
        this.lengthField = new WeakReference<>(editor.addFloatField("Segment Length:", Utils.fixedPointIntToFloat4Bit(getLength()), isAllowLengthEdit() ? newVal -> {
            setLength(Utils.floatToFixedPointShort4Bit(newVal));
            onManualLengthUpdate(pathPreview, editor);
            updateDisplay(pathPreview); // Don't call onUpdate because that will recalculate length.
        } : null, null)); // Read-Only.
    }

    /**
     * Updates the viewer UI when this segment is updated.
     * @param pathPreview the path preview to update the path for
     */
    public void onUpdate(FroggerPathPreview pathPreview) {
        recalculateLength();
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
     * @param newLength The segment length
     */
    @SuppressWarnings("ConstantConditions")
    public void setLength(int newLength) {
        this.length = newLength;
        if (this.lengthField != null && this.lengthField.get() != null)
            this.lengthField.get().setText(String.valueOf(Utils.fixedPointIntToFloat4Bit(newLength)));
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