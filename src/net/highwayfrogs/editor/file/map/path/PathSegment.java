package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.utils.Utils;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class PathSegment extends GameObject {
    private PathType type;
    @Setter private int length;
    private boolean allowLengthEdit;

    public PathSegment(PathType type, boolean allowLengthEdit) {
        this.type = type;
        this.allowLengthEdit = allowLengthEdit;
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
    protected abstract PathResult calculatePosition(PathInfo info);

    /**
     * Recalculates the length of this segment.
     */
    public abstract void recalculateLength();

    /**
     * Setup a path editor.
     * @param path       The path which owns this segment.
     * @param controller The UI controller.
     * @param editor     The editor to setup.
     */
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        editor.addLabel("Type:", getType().name(), 25);
        editor.addFloatField("Length:", Utils.fixedPointIntToFloat4Bit(getLength()), isAllowLengthEdit() ? newVal -> setLength(Utils.floatToFixedPointShort4Bit(newVal)) : null, null); // Read-Only.
    }

    /**
     * Updates the viewer UI when this segment is updated.
     * @param controller The controller
     */
    public void onUpdate(MapUIController controller) {
        recalculateLength();
        controller.getController().updatePathDisplay();
        controller.getController().resetEntities();
    }
}
