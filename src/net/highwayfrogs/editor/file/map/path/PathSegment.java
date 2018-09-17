package net.highwayfrogs.editor.file.map.path;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
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
        int dataPointer = reader.readInt();

        reader.jumpTemp(dataPointer);
        this.length = reader.readInt();
        this.loadData(reader);
        reader.jumpReturn();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(getType().ordinal());

        writer.jumpTemp(writer.getIndex()); //TODO: Pointer should go right after the path data.
        writer.writeInt(this.length);
        saveData(writer);
        writer.jumpReturn();
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
}
