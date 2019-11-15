package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.sound.AbstractVBFile;
import net.highwayfrogs.editor.file.sound.GameSound;
import net.highwayfrogs.editor.utils.Utils;

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
public class VABController extends EditorController<AbstractVBFile> {
    @FXML private ListView<GameSound> soundList;
    @FXML private Button playButton;
    @FXML private Label label1;
    @FXML private TextField sampleRateField;
    @FXML private CheckBox repeatCheckBox;
    @FXML private Slider sliderSampleRate;

    private GameSound selectedSound;
    private Clip currentClip;

    @Override
    public void loadFile(AbstractVBFile vbFile) {
        super.loadFile(vbFile);

        sliderSampleRate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(oldValue))
                return;

            this.selectedSound.setSampleRate(newValue.intValue()); // Apply the new sample rate.
            updateInterface();
        });

        ObservableList<GameSound> gameSounds = FXCollections.observableArrayList(vbFile.getAudioEntries());
        soundList.setItems(gameSounds);
        soundList.setCellFactory(param -> new AttachmentListCell());

        soundList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedSound = newValue;
            this.updateInterface();
        });

        soundList.getSelectionModel().select(0);
    }

    @Override
    public void onClose(AnchorPane editorRoot) {
        super.onClose(editorRoot);
        closeClip();
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
        File selectedFile = Utils.promptFileSave("Specify the file to export this sound as...", this.selectedSound.getSoundName(), "Sound Files", "wav");
        if (selectedFile == null)
            return;

        try {
            this.selectedSound.exportToFile(selectedFile);
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException ex) {
            throw new RuntimeException("Failed to export sound as " + selectedFile.getName(), ex);
        }
    }

    @FXML
    private void importSound(ActionEvent event) {
        File selectedFile = Utils.promptFileOpen("Select the sound to import...", "Sound Files", "wav");
        if (selectedFile == null)
            return; // Cancelled.

        try {
            this.selectedSound.replaceWithFile(selectedFile);
        } catch (UnsupportedAudioFileException | IOException ex) {
            throw new RuntimeException("Failed to import sound file " + selectedFile.getName(), ex);
        }

        updateInterface();
    }

    @FXML
    @SneakyThrows
    private void exportAllSounds(ActionEvent event) {
        File selectedFolder = Utils.promptChooseDirectory("Select the directory to export sounds to.", false);
        if (selectedFolder == null)
            return; // Cancelled.

        for (GameSound sound : getFile().getAudioEntries()) {
            sound.exportToFile(new File(selectedFolder, sound.getSoundName() + ".wav"));
            System.out.println("Exported sound: " + sound.getSoundName());
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
            toggleComponents(true);

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
            Utils.makePopUp("Improperly formatted number: '" + text + "'.", AlertType.ERROR);
            return;
        }

        this.selectedSound.setSampleRate(newRate);
        updateInterface();
    }

    /**
     * Update sound and sound info.
     */
    public void updateInterface() {
        updateSound();
        updateSoundInfo();
    }

    /**
     * Update the info displayed for the image.
     */
    public void updateSoundInfo() {
        label1.setText("Vanilla Track ID: " + selectedSound.getVanillaTrackId());
        this.sampleRateField.setText(String.valueOf(selectedSound.getSampleRate()));
        this.sliderSampleRate.setValue(selectedSound.getSampleRate());
    }

    private void closeClip() {
        if (this.currentClip == null)
            return;

        this.currentClip.stop(); //Stop the old playing clip. Also sets the play button back to "Play", enables the repeat button, etc.
        this.currentClip.close(); // Tell the old clip that we aren't going to use it again.
    }

    /**
     * Update the displayed image.
     */
    public void updateSound() {
        closeClip();

        this.currentClip = this.selectedSound.getClip();
        this.currentClip.addLineListener(e -> {
            if (e.getType() != Type.STOP)
                return;

            Platform.runLater(() -> {
                this.playButton.setText("Play");
                toggleComponents(false);
            });
        });
    }

    private void toggleComponents(boolean newState) {
        this.repeatCheckBox.setDisable(newState);
        this.sampleRateField.setDisable(newState);
        this.sliderSampleRate.setDisable(newState);
    }
}
