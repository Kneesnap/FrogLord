package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * A single part of the path. When saved, this is broken up by <type,offset> -> segment data
 * Created by Kneesnap on 8/22/2018.
 */
@Getter
public abstract class PathSegment extends GameObject {
    private PathType type;
    private int length;

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
    protected abstract SVector calculatePosition(PathInfo info);
}
