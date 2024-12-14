package net.highwayfrogs.editor.file.writer;

import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * A DataReceiver where data is saved to a file.
 * Created by Kneesnap on 8/10/2018.
 */
public class FileReceiver implements DataReceiver {
    private final File targetFile;
    private final ArrayReceiver arrayReceiver;

    public FileReceiver(File file) {
        this.targetFile = file;
        this.arrayReceiver = new ArrayReceiver();
    }

    public FileReceiver(File file, int startingSize) {
        this.targetFile = file;
        this.arrayReceiver = new ArrayReceiver(startingSize);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        this.arrayReceiver.writeByte(value);
    }

    @Override
    public void writeBytes(byte[] values) throws IOException {
        this.arrayReceiver.writeBytes(values);
    }

    @Override
    public void writeBytes(byte[] values, int offset, int amount) throws IOException {
        this.arrayReceiver.writeBytes(values, offset, amount);
    }

    @Override
    public void setIndex(int newIndex) throws IOException {
        this.arrayReceiver.setIndex(newIndex);
    }

    @Override
    public int getIndex() throws IOException {
        return this.arrayReceiver.getIndex();
    }

    @Override
    public void close() {
        this.arrayReceiver.close();
        FileUtils.writeBytesToFile(null, this.targetFile, this.arrayReceiver.toArray(), true);
    }
}