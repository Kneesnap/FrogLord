package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
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
     * @param froggerEXE The executable to save data to.
     * @param folder    The folder to output the mwds.
     */
    public void startSaving(MWDFile loadedMWD, FroggerEXEInfo froggerEXE, File folder) {
        File mwdFile = new File(folder, "FROGPSX.MWD");
        DataWriter mwdWriter;

        try {
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

            froggerEXE.patchEXE(loadedMWD.getWadIndexTable());
            froggerEXE.saveExecutable();
            Platform.runLater(stage::close);
        }).start();
    }
}
