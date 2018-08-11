package net.highwayfrogs.editor.file.writer;

import lombok.Getter;

import java.io.*;

/**
 * A DataReceiver where data is saved to a file.
 * Created by Kneesnap on 8/10/2018.
 */
public class FileReceiver implements DataReceiver {
    private FileOutputStream output;
    @Getter private int index;

    public FileReceiver(File file) throws FileNotFoundException {
        this.output = new FileOutputStream(file);
    }

    @Override
    public void writeByte(byte value) throws IOException {
        this.index++;
        output.write(value);
    }

    @Override
    public void writeBytes(byte[] values) throws IOException {
        this.index += values.length;
        output.write(values);
    }
}
