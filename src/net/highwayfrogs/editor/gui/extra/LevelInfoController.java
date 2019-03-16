package net.highwayfrogs.editor.gui.extra;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.data.MusicTrack;
import net.highwayfrogs.editor.file.config.data.WorldId;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controls level information.
 * Created by Kneesnap on 3/15/2019.
 */
@Getter
public class LevelInfoController implements Initializable {
    @FXML private ComboBox<MAPLevel> levelSelector;
    @FXML private ComboBox<MAPTheme> themeSelector;
    @FXML private ComboBox<WorldId> worldSelector;
    @FXML private ComboBox<MusicTrack> musicSelector;
    @FXML private TextField stackPosField;
    @FXML private TextField localLevelField;
    @FXML private TextField worldLevelField;
    private Stage stage;
    private FroggerEXEInfo config;
    private LevelInfo selectedLevel;

    private List<Node> disableFields;

    private LevelInfoController(Stage stage, FroggerEXEInfo config) {
        this.stage = stage;
        this.config = config;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.disableFields = Arrays.asList(themeSelector, worldSelector, musicSelector, stackPosField, localLevelField, worldLevelField);
        levelSelector.setItems(FXCollections.observableArrayList(MAPLevel.values()));
        themeSelector.setItems(FXCollections.observableArrayList(MAPTheme.values()));
        worldSelector.setItems(FXCollections.observableArrayList(WorldId.values()));
        musicSelector.setItems(FXCollections.observableArrayList(MusicTrack.values()));

        // Handlers:
        levelSelector.valueProperty().addListener((listener, oldVal, newVal) -> setLevel(newVal));
        worldSelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedLevel().setWorld(newValue));
        themeSelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedLevel().setTheme(newValue));
        musicSelector.valueProperty().addListener((observable, oldValue, newValue) -> getConfig().getMusicTracks().set(getSelectedLevel().getLevel().ordinal(), newValue));

        Utils.setHandleTestKeyPress(stackPosField, Utils::isInteger, newValue -> getSelectedLevel().setStackPosition(Integer.parseInt(newValue)));
        Utils.setHandleTestKeyPress(localLevelField, Utils::isInteger, newValue -> getSelectedLevel().setLocalLevelId(Integer.parseInt(newValue)));
        Utils.setHandleTestKeyPress(worldLevelField, Utils::isInteger, newValue -> getSelectedLevel().setLevelsInWorld(Integer.parseInt(newValue)));

        levelSelector.getSelectionModel().select(0);
    }

    private void setLevel(MAPLevel newLevel) {
        this.selectedLevel = config.getLevel(newLevel);

        boolean hasLevelInfo = (this.selectedLevel != null);

        disableFields.forEach(node -> node.setDisable(!hasLevelInfo));
        if (!hasLevelInfo)
            return;

        stackPosField.setText(String.valueOf(getSelectedLevel().getStackPosition()));
        localLevelField.setText(String.valueOf(getSelectedLevel().getLocalLevelId()));
        worldLevelField.setText(String.valueOf(getSelectedLevel().getLevelsInWorld()));

        themeSelector.getSelectionModel().select(getSelectedLevel().getTheme());
        worldSelector.getSelectionModel().select(getSelectedLevel().getWorld());

        MusicTrack selectedTrack = getConfig().getMusic(newLevel);
        if (selectedTrack != null) {
            musicSelector.getSelectionModel().select(selectedTrack); // Select the current track.
        } else {
            musicSelector.setDisable(true); // There is no entry present currently.
        }
    }

    @FXML
    private void onDone(ActionEvent evt) {
        stage.close(); // Close this window.
    }

    /**
     * Open the level info controller.
     */
    public static void openEditor(FroggerEXEInfo info) {
        Utils.loadFXMLTemplate("level-info", "Level Stack Editor", newStage -> new LevelInfoController(newStage, info));
    }
}
