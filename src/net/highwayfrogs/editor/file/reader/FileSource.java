package net.highwayfrogs.editor.file.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Turns a file into a data source.
 * Created by Kneesnap on 8/10/2018.
 */
public class FileSource implements DataSource {
    private RandomAccessFile file;

    public FileSource(File file) throws FileNotFoundException {
        this.file = new RandomAccessFile(file, "r");
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) file.read();
    }

    @Override
    public byte[] readBytes(int amount) throws IOException {
        byte[] bytes = new byte[amount];
        file.read(bytes);
        return bytes;
    }

    @Override
    public void skip(int byteCount) throws IOException {
        file.skipBytes(byteCount);
    }

    @Override
    public void setIndex(int newIndex) throws IOException {
        file.seek(newIndex);
    }

    @Override
    public int getIndex() throws IOException {
        return (int) file.getFilePointer();
    }

    @Override
    public int getSize() throws IOException {
        return (int) file.length();
    }
}
