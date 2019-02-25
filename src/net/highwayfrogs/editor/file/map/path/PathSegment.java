package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class PathSegment extends GameObject {
    private PathType type;
    @Setter private int length;

    public PathSegment(PathType type) {
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
    protected abstract Vector calculatePosition(PathInfo info);

    /**
     * Setup a path editor.
     * @param path       The path which owns this segment.
     * @param controller The UI controller.
     * @param editor     The editor to setup.
     */
    public void setupEditor(Path path, MapUIController controller, GUIEditorGrid editor) {
        editor.addEnumSelector("Type", getType(), PathType.values(), false, newType -> {
            if (getType() == newType)
                return;

            path.getSegments().set(path.getSegments().indexOf(this), newType.getMaker().get());
            controller.setupPathEditor();
        });

        editor.addIntegerField("Length", getLength(), this::setLength, null);
    }
}
