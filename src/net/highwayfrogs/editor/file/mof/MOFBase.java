package net.highwayfrogs.editor.file.mof;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents the basic mof file types, static and animated.
 * Created by Kneesnap on 2/25/2019.
 */
@Getter
public abstract class MOFBase extends GameObject {
    private transient MOFHolder holder;

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
        byte[] signature = reader.readBytes(MOFHolder.DUMMY_DATA.length);
        reader.skipInt(); // File length, including header.
        int flags = reader.readInt();
        onLoad(reader, signature);

        // This is done after the file is read, because to generate flags we must know the contents of the file first.
        if (flags != buildFlags())
            throw new RuntimeException("Generated Flags (" + buildFlags() + ") do not match read flags (" + flags + ") in " + getFileEntry().getDisplayName());
        if (!makeSignature().equals(new String(signature)))
            throw new RuntimeException("Generated Signature (" + makeSignature() + ") does not match read signature (" + new String(signature) + ") in " + getFileEntry().getDisplayName() + " (Real Signature Bytes: " + Utils.toByteString(signature) + ")");
    }

    @Override
    public void save(DataWriter writer) {
        int startIndex = writer.getIndex();
        writer.writeStringBytes(makeSignature());
        int sizeAddress = writer.writeNullPointer();
        writer.writeInt(buildFlags());
        onSave(writer);
        writer.writeAddressAt(sizeAddress, writer.getIndex() - startIndex);
    }

    /**
     * Called when it's time to load.
     * @param reader The reader to read data from.
     */
    public abstract void onLoad(DataReader reader, byte[] signature);

    /**
     * Called when it's time to load.
     * @param writer The writer to write data to.
     */
    public abstract void onSave(DataWriter writer);

    /**
     * Builds the flags used for this mof.
     * @return mofFlags
     */
    public abstract int buildFlags();

    /**
     * Creates the signature that goes at the start of this file.
     */
    public abstract String makeSignature();
}
