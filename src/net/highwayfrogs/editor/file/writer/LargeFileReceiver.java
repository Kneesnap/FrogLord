package net.highwayfrogs.editor.file.writer;

import lombok.SneakyThrows;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Writes data to large files without storing it in memory.
 * Created by Kneesnap on 8/24/2023.
 */
public class LargeFileReceiver implements DataReceiver {
    private final File targetFile;
    private final RandomAccessFile randomAccessFile;

    @SneakyThrows
    public LargeFileReceiver(File file) {
        this.targetFile = file;
        Utils.deleteFile(file);
        this.randomAccessFile = new RandomAccessFile(file, "rw");
    }

    @Override
    public void writeByte(byte value) throws IOException {
        this.randomAccessFile.writeByte(value);
    }

    @Override
    public void writeBytes(byte[] values) throws IOException {
        this.randomAccessFile.write(values);
    }

    @Override
    public void setIndex(int newIndex) throws IOException {
        this.randomAccessFile.seek(newIndex);
    }

    @Override
    public int getIndex() throws IOException {
        return (int) this.randomAccessFile.getFilePointer();
    }

    @Override
    @SneakyThrows
    public void close() {
        this.randomAccessFile.close();
    }
}
