package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.highwayfrogs.editor.file.sound.GameSound;
import net.highwayfrogs.editor.file.sound.VBFile;
import net.highwayfrogs.editor.gui.GUIMain;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Controls the VAB sound screen.
 * Created by Kneesnap on 9/18/2018.
 */
public class VABController extends EditorController<VBFile> {
    @FXML private ListView<GameSound> soundList;
    @FXML private Button playButton;
    @FXML private Label label1;
    @FXML private Label label2;
    @FXML private TextField sampleRateField;
    @FXML private CheckBox repeatCheckBox;

    private GameSound selectedSound;
    private Clip currentClip;

    @Override
    public void loadFile(VBFile vbFile) {
        super.loadFile(vbFile);

        ObservableList<GameSound> gameSounds = FXCollections.observableArrayList(vbFile.getAudioEntries());
        soundList.setItems(gameSounds);
        soundList.setCellFactory(param -> new AttachmentListCell());

        soundList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedSound = newValue;
            this.updateSound();
            this.updateSoundInfo();
        });

        soundList.getSelectionModel().select(0);
    }

    private static class AttachmentListCell extends ListCell<GameSound> {
        @Override
        public void updateItem(GameSound sound, boolean empty) {
            super.updateItem(sound, empty);
            setText(empty ? null : sound.getSoundName());
        }
    }

    @FXML
    private void exportSound(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify the file to export this sound as...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Sound Files", "*.wav"));
        fileChooser.setInitialDirectory(GUIMain.WORKING_DIRECTORY);

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);

        try {
            if (selectedFile != null)
                this.selectedSound.exportToFile(selectedFile);
        } catch (LineUnavailableException | IOException ex) {
            throw new RuntimeException("Failed to export sound as " + selectedFile.getName(), ex);
        }
    }

    @FXML
    private void importSound(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the sound to import...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Sound Files", "*.wav"));
        fileChooser.setInitialDirectory(GUIMain.WORKING_DIRECTORY);

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancelled.

        try {
            this.selectedSound.replaceWithFile(selectedFile);
        } catch (UnsupportedAudioFileException | IOException ex) {
            throw new RuntimeException("Failed to import sound file " + selectedFile.getName(), ex);
        }

        updateSound();
        updateSoundInfo();
    }

    @FXML
    private void exportAllSounds(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the directory to export sounds to.");
        chooser.setInitialDirectory(GUIMain.WORKING_DIRECTORY);

        File selectedFolder = chooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder == null)
            return; // Cancelled.

        try {
            for (int i = 0; i < getFile().getAudioEntries().size(); i++) {
                File output = new File(selectedFolder, i + ".wav");
                getFile().getAudioEntries().get(i).exportToFile(output);
                System.out.println("Exported sound #" + i + ".");
            }

        } catch (IOException | LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void togglePlay(ActionEvent event) {
        if (this.currentClip == null)
            return;

        this.currentClip.setMicrosecondPosition(0); // Reset play position.

        if (this.currentClip.isActive()) {
            this.currentClip.stop();
        } else {
            this.repeatCheckBox.setDisable(true);

            if (this.repeatCheckBox.isSelected()) {
                this.currentClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                this.currentClip.start();
            }

            this.playButton.setText("Stop");
        }
    }

    @FXML
    private void onSampleRateUpdate(ActionEvent evt) {
        String text = this.sampleRateField.getText();
        int newRate;

        try {
            newRate = Integer.parseInt(text);
        } catch (NumberFormatException nfx) {
            System.out.println("Improperly formatted number: '" + text + "'.");
            return;
        }

        this.selectedSound.setSampleRate(newRate);
        updateSound();
    }

    /**
     * Update the info displayed for the image.
     */
    public void updateSoundInfo() {
        label1.setText("Vanilla Track ID: " + selectedSound.getVanillaTrackId());
        label2.setText("Channels: " + selectedSound.getChannelCount());
        this.sampleRateField.setText(String.valueOf(selectedSound.getSampleRate()));
    }

    /**
     * Update the displayed image.
     */
    public void updateSound() {
        if (this.currentClip != null)
            this.currentClip.stop(); // Stop the old playing clip.

        this.currentClip = makeClip(this.selectedSound);
        this.currentClip.addLineListener(e -> {
            if (e.getType() != Type.STOP)
                return;

            Platform.runLater(() -> {
                this.playButton.setText("Play");
                this.repeatCheckBox.setDisable(false);
            });
        });
    }

    private Clip makeClip(GameSound sound) {
        try {
            return sound.toStandardAudio();
        } catch (LineUnavailableException ex) {
            throw new RuntimeException("Failed to make AudioClip for sound.", ex);
        }
    }
}
