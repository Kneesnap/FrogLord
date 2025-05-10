package net.highwayfrogs.editor.utils.data.reader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Allows reading data from a RandomAccessFile.
 * Created by Kneesnap on 5/4/2025.
 */
public class RandomAccessFileSource implements DataSource, AutoCloseable {
    private final RandomAccessFile file;

    public RandomAccessFileSource(File file) throws IOException {
        this.file = new RandomAccessFile(file, "r");
    }

    @Override
    public byte readByte() throws IOException {
        return this.file.readByte();
    }

    @Override
    public byte[] readBytes(int amount) throws IOException {
        byte[] bytes = new byte[amount];

        int temp;
        int totalBytesRead = 0;
        while (amount > totalBytesRead && (temp = this.file.read(bytes, totalBytesRead, amount - totalBytesRead)) != -1)
            totalBytesRead += temp;

        if (totalBytesRead != amount)
            throw new IOException("Read an incorrect number of bytes! (Read: " + totalBytesRead + ", Desired: " + amount + ")");

        return bytes;
    }

    @Override
    public int readBytes(byte[] output, int offset, int amount) throws IOException {
        if (amount <= 0)
            return 0;

        int bytesLeft = getSize() - getIndex();
        if (amount > bytesLeft)
            throw new IllegalArgumentException("Cannot read " + amount + " bytes, as there are only " + bytesLeft + " bytes left.");

        int temp;
        int totalBytesRead = 0;
        while (amount > totalBytesRead && (temp = this.file.read(output, offset + totalBytesRead, amount - totalBytesRead)) != -1)
            totalBytesRead += temp;

        if (totalBytesRead != amount)
            throw new IOException("Read an incorrect number of bytes! (Read: " + totalBytesRead + ", Desired: " + amount + ")");

        return amount;
    }

    @Override
    public void skip(int byteCount) throws IOException {
        this.file.skipBytes(byteCount);
    }

    @Override
    public void setIndex(int newIndex) throws IOException {
        this.file.seek(newIndex);
    }

    @Override
    public int getIndex() throws IOException {
        return (int) this.file.getFilePointer();
    }

    @Override
    public int getSize() throws IOException {
        return (int) this.file.length();
    }

    @Override
    public void close() throws Exception {
        this.file.close();
    }
}
