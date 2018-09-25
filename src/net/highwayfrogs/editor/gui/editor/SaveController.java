package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.file.writer.FileReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the screen which pops up while saving a MWD.
 * Created by Kneesnap on 9/25/2018.
 */
public class SaveController {
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    private Stage stage;

    /**
     * Initialize this controller.
     * @param stage The stage we're controlling.
     */
    public void onInit(Stage stage) {
        this.stage = stage;
        stage.setResizable(false);
        stage.setAlwaysOnTop(true);
        stage.setOnCloseRequest(Event::consume); // Prevent shutting the window.
    }

    /**
     * Start saving the MWD File.
     * @param loadedMWD The loaded MWD file to save.
     * @param mwdFile   The file to save the MWD to on disk.
     * @param loadedMWI The loaded MWI file to save.
     * @param mwiFile   The file to save the MWD to on disk.
     */
    public void startSaving(MWDFile loadedMWD, File mwdFile, MWIFile loadedMWI, File mwiFile) {
        DataWriter mwiWriter;
        DataWriter mwdWriter;

        try {
            mwiWriter = new DataWriter(new FileReceiver(mwiFile));
            mwdWriter = new DataWriter(new FileReceiver(mwdFile));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Failed to save files.", ex);
        }

        AtomicInteger currentFile = new AtomicInteger(0);
        final int fileCount = loadedMWD.getFiles().size();

        new Thread(() -> {
            loadedMWD.setSaveCallback((entry, file) -> Platform.runLater(() -> {
                int fileId = currentFile.incrementAndGet();

                double progress = (double) fileId / (double) fileCount;
                progressLabel.setText((int) (progress * 100D) + "%");
                progressBar.setProgress(progress);
                statusLabel.setText("Saving " + entry.getDisplayName() + " (" + fileId + "/" + fileCount + ")");
            }));

            loadedMWD.save(mwdWriter);
            mwdWriter.closeReceiver();
            loadedMWD.setSaveCallback(null);

            loadedMWI.save(mwiWriter);
            mwiWriter.closeReceiver();
            Platform.runLater(stage::close);
        }).start();
    }
}
