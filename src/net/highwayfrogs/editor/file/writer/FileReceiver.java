package net.highwayfrogs.editor.file.writer;

import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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

        if (!this.targetFile.getParentFile().canWrite()) {
            Platform.runLater(() -> Utils.makePopUp("Can't write to the file '" + this.targetFile.getName() + "'." + Constants.NEWLINE + "Do you have permission to save in this folder?", AlertType.ERROR));
            return;
        }

        Utils.deleteFile(this.targetFile);
        try {
            Files.write(this.targetFile.toPath(), this.arrayReceiver.toArray());
        } catch (IOException e) {
            Utils.makeErrorPopUp("FileReceiver failed to write data to the file: '" + this.targetFile.getName() + "'.", e, true);
        }
    }
}