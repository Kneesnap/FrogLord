package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.shared.sound.ISoundSample;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBodyEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVBFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.utils.FXUtils;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * Controls the VAB sound screen.
 * TODO: This menu has speed issues due to usage of Clip.close(). We should switch to a cached Clip model to avoid this issue.
 *  -> Alternatively, it might be feasible to use AudioClip.getProvider().create()? Perhaps this would give us more control and be less likely to fail?
 * Created by Kneesnap on 5/13/2024.
 */
public class SCVABUIController extends SCFileEditorUIController<SCGameInstance, SCSplitVBFile> {
    @FXML private ListView<ISoundSample> soundList;
    @FXML private Button playButton;
    @FXML private Label label1;
    @FXML private TextField sampleRateField;
    @FXML private CheckBox repeatCheckBox;
    @FXML private Slider sliderSampleRate;

    private SCSplitSoundBankBodyEntry selectedSoundBodyEntry;
    private ISoundSample selectedSound;
    private Clip currentClip;

    public SCVABUIController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        this.sliderSampleRate.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals(oldValue))
                return;

            String errorMessage = this.selectedSound.getAudioFormat().setSampleRate(newValue.intValue()); // Apply the new sample rate.
            if (errorMessage != null) {
                FXUtils.makePopUp(errorMessage, AlertType.ERROR);
                return;
            }

            updateInterface();
        });
    }

    @Override
    public void setTargetFile(SCSplitVBFile vbFile) {
        super.setTargetFile(vbFile);

        this.soundList.setItems(FXCollections.observableArrayList(vbFile.getSoundBank().getSounds()));
        this.soundList.setCellFactory(param -> new AbstractAttachmentCell<>((sound, id) -> sound != null ? sound.getSoundName() : null));
        this.soundList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedSound = newValue;
            this.selectedSoundBodyEntry = (SCSplitSoundBankBodyEntry) newValue;
            this.updateInterface();
        });

        this.soundList.getSelectionModel().select(0);
    }

    @Override
    public void onSceneRemove(Scene oldScene) {
        super.onSceneRemove(oldScene);
        closeClip();
    }

    @FXML
    private void exportSound(ActionEvent event) {
        File selectedFile = FXUtils.promptFileSave(getGameInstance(), "Specify the file to export this sound as...", this.selectedSound.getSoundName(), "Sound Files", "wav");
        if (selectedFile == null)
            return;

        try {
            this.selectedSound.saveToImportableFile(selectedFile);
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException ex) {
            throw new RuntimeException("Failed to export sound as " + selectedFile.getName(), ex);
        }
    }

    @FXML
    private void importSound(ActionEvent event) {
        File selectedFile = FXUtils.promptFileOpen(getGameInstance(), "Select the sound to import...", "Sound Files", "wav");
        if (selectedFile == null)
            return; // Cancelled.

        try {
            this.selectedSound.importSoundFromFile(selectedFile);
        } catch (UnsupportedAudioFileException | IOException ex) {
            FXUtils.makeErrorPopUp("Failed to import sound file " + selectedFile.getName(), ex, true);
        }

        updateInterface();
    }

    @FXML
    @SneakyThrows
    private void exportAllSounds(ActionEvent event) {
        File selectedFolder = FXUtils.promptChooseDirectory(getGameInstance(), "Select the directory to export sounds to.", false);
        if (selectedFolder == null)
            return; // Cancelled.

        for (ISoundSample sound : getFile().getBody().getEntries()) {
            sound.saveToImportableFile(new File(selectedFolder, sound.getSoundName() + ".wav"));
            getLogger().info("Exported sound: " + sound.getSoundName());
        }
    }

    @FXML
    private void togglePlay(ActionEvent event) {
        if (this.currentClip == null)
            return;

        if (this.currentClip.isActive()) {
            this.currentClip.stop();
            this.currentClip.setMicrosecondPosition(0); // Reset play position.
        } else {
            toggleComponents(true);
            this.playButton.setText("Stop");

            this.currentClip.setMicrosecondPosition(0); // Reset play position.
            if (this.repeatCheckBox.isSelected()) {
                this.currentClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                this.currentClip.start();
            }
        }
    }

    @FXML
    private void onSampleRateUpdate(ActionEvent evt) {
        String text = this.sampleRateField.getText();
        int newRate;

        try {
            newRate = Integer.parseInt(text);
        } catch (NumberFormatException nfx) {
            FXUtils.makePopUp("Improperly formatted number: '" + text + "'.", AlertType.ERROR);
            return;
        }

        String errorMessage = this.selectedSound.getAudioFormat().setSampleRate(newRate); // Apply the new sample rate.
        if (errorMessage != null) {
            FXUtils.makePopUp(errorMessage, AlertType.ERROR);
            return;
        }

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
        this.label1.setText("Internal Track ID: " + (this.selectedSoundBodyEntry != null ? this.selectedSoundBodyEntry.getInternalTrackId() : "-1"));
        this.sampleRateField.setText(String.valueOf(this.selectedSound.getAudioFormat().getSampleRate()));
        this.sliderSampleRate.setValue(this.selectedSound.getAudioFormat().getSampleRate());
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

        if (this.currentClip != null) {
            byte[] pcmData = this.selectedSound.getRawAudioPlaybackData();

            try {
                this.currentClip.open(this.selectedSound.getAudioFormat(), pcmData, 0, pcmData.length);
            } catch (LineUnavailableException exception) {
                handleError(exception, true, "Could not load audio data from file.");
            }
        } else {
            try {
                this.currentClip = this.selectedSound.getAudioClip();
            } catch (LineUnavailableException e) {
                handleError(e, true, "Could not get AudioClip from file.");
            }

            this.currentClip.addLineListener(e -> {
                if (e.getType() != Type.STOP)
                    return;

                Platform.runLater(() -> {
                    this.playButton.setText("Play");
                    toggleComponents(false);
                });
            });
        }
    }

    private void toggleComponents(boolean newState) {
        this.repeatCheckBox.setDisable(newState);
        this.sampleRateField.setDisable(newState);
        this.sliderSampleRate.setDisable(newState);
    }
}