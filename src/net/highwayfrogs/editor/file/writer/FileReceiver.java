package net.highwayfrogs.editor.file.writer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A DataReceiver where data is saved to a file.
 * Created by Kneesnap on 8/10/2018.
 */
public class FileReceiver implements DataReceiver {
    private RandomAccessFile output;

    public FileReceiver(File file) throws FileNotFoundException {
        this.output = new RandomAccessFile(file, "rw");
    }

    @Override
    public void writeByte(byte value) throws IOException {
        output.write(value);
    }

    @Override
    public void writeBytes(byte[] values) throws IOException {
        output.write(values);
    }

    @Override
    public void setIndex(int newIndex) throws IOException {
        output.seek(newIndex);
    }

    @Override
    public int getIndex() throws IOException {
        return (int) output.getFilePointer();
    }

    @Override
    public void close() {
        try {
            output.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
