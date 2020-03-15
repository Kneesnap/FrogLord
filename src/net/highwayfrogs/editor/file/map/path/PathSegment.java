package net.highwayfrogs.editor.file.map.path;

import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.lang.ref.WeakReference;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class PathSegment extends GameObject {
    private PathType type;
    private int length;
    private transient WeakReference<TextField> lengthField;
    private transient Path path;

    public PathSegment(Path path, PathType type) {
        this.path = path;
        this.type = type;
    }

    @Override
    public void load(DataReader reader) {
        this.length = reader.readInt();
        this.loadData(reader);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getType().ordinal());
        writer.writeInt(this.length);
        saveData(writer);
    }

    /**
     * Setup this segment at the end of the given path.
     */
    public abstract void setupNewSegment(MAPFile map);

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
    public abstract PathResult calculatePosition(PathInfo info);

    /**
     * Calculate the position along this segment.
     * @param map      The map containing the path.
     * @param distance The distance along this segment.
     * @return pathResult
     */
    public PathResult calculatePosition(MAPFile map, int distance) {
        PathInfo fakeInfo = new PathInfo();
        fakeInfo.setPath(map, this.path, this);
        fakeInfo.setSegmentDistance(distance);
        fakeInfo.setSpeed(1);
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
     * @param controller The UI controller.
     * @param editor     The editor to setup.
     */
    public void setupEditor(MapUIController controller, GUIEditorGrid editor) {
        editor.addLabel("Type:", getType().name(), 25);
        this.lengthField = new WeakReference<>(editor.addFloatField("Length:", Utils.fixedPointIntToFloat4Bit(getLength()), isAllowLengthEdit() ? newVal -> {
            setLength(Utils.floatToFixedPointShort4Bit(newVal));
            onManualLengthUpdate(controller, editor);
            updateDisplay(controller); // Don't call onUpdate because that will recalculate length.
        } : null, null)); // Read-Only.
    }

    /**
     * Updates the viewer UI when this segment is updated.
     * @param controller The controller
     */
    public void onUpdate(MapUIController controller) {
        recalculateLength();
        updateDisplay(controller);
    }

    private void updateDisplay(MapUIController controller) {
        if (controller != null) {
            Path selectedPath = controller.getPathManager().getSelectedPath();
            if (selectedPath != null)
                controller.getPathManager().updatePath(selectedPath);
            controller.getEntityManager().updateEntities();
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
     * Whether or not editing the length text box is allowed.
     */
    public boolean isAllowLengthEdit() {
        return false;
    }

    /**
     * Called when length is manually updated.
     */
    public void onManualLengthUpdate(MapUIController controller, GUIEditorGrid editor) {

    }
}
