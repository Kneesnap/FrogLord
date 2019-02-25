package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents the basic mof file types, static and animated.
 * Created by Kneesnap on 2/25/2019.
 */
@Getter
public abstract class MOFBase extends GameObject {
    private MOFHolder holder;

    private byte[] signature;
    private int flags;

    public MOFBase(MOFHolder holder) {
        this.holder = holder;
    }

    /**
     * Gets this file's MWI FileEntry.
     * @return fileEntry
     */
    public FileEntry getFileEntry() {
        return getHolder().getFileEntry();
    }

    @Override
    public final void load(DataReader reader) {
        this.signature = reader.readBytes(MOFHolder.DUMMY_DATA.length);
        reader.skipInt(); // File length, including header.
        this.flags = reader.readInt();
        onLoad(reader);
    }

    @Override
    public final void save(DataWriter writer) {
        writer.writeBytes(getSignature());
        writer.writeNullPointer(); // File size. Maybe in the future we'll set the value to be proper.
        writer.writeInt(this.flags);
        onSave(writer);
    }

    /**
     * Called when it's time to load.
     * @param reader The reader to read data from.
     */
    public abstract void onLoad(DataReader reader);

    /**
     * Called when it's time to load.
     * @param writer The writer to write data to.
     */
    public abstract void onSave(DataWriter writer);

    /**
     * Test if a flag is present.
     * @param flag The flag to test.
     * @return flagPresent
     */
    public boolean testFlag(int flag) {
        return (this.flags & flag) == flag;
    }
}
