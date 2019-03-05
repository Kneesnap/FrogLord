package net.highwayfrogs.editor.file.config.exe.general;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * An entity book entry.
 * Created by Kneesnap on 3/4/2019.
 */
public class EntityEntry extends GameObject {
    private long createFunction;
    private long updateFunction;
    private long killFunction;
    private int flags;
    private int runtimeSize;

    @Override
    public void load(DataReader reader) {
        this.createFunction = reader.readUnsignedIntAsLong();
        this.updateFunction = reader.readUnsignedIntAsLong();
        this.killFunction = reader.readUnsignedIntAsLong();
        this.flags = reader.readInt();
        this.runtimeSize = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.createFunction);
        writer.writeUnsignedInt(this.updateFunction);
        writer.writeUnsignedInt(this.killFunction);
        writer.writeInt(this.flags);
        writer.writeInt(this.runtimeSize);
    }
}
