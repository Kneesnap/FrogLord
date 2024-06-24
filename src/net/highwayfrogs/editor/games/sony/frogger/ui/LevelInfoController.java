package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.data.MusicTrack;
import net.highwayfrogs.editor.file.config.data.WorldId;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controls level information.
 * Created by Kneesnap on 3/15/2019.
 */
@Getter
public class LevelInfoController extends GameUIController<FroggerGameInstance> {
    @FXML private ComboBox<MAPLevel> levelSelector;
    @FXML private ComboBox<MAPLevel> mapFileSelector;
    @FXML private ComboBox<FroggerMapTheme> themeSelector;
    @FXML private ComboBox<WorldId> worldSelector;
    @FXML private ComboBox<MusicTrack> musicSelector;
    @FXML private TextField stackPosField;
    @FXML private TextField localLevelField;
    @FXML private TextField worldLevelField;
    private LevelInfo selectedLevel;

    private List<Node> disableFields;

    private LevelInfoController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.disableFields = Arrays.asList(themeSelector, worldSelector, musicSelector, stackPosField, localLevelField, worldLevelField, mapFileSelector);

        List<MAPLevel> levelInfo = new ArrayList<>();
        for (LevelInfo info : getGameInstance().getAllLevelInfo())
            if (info.getLevel() != null)
                levelInfo.add(info.getLevel());

        levelSelector.setItems(FXCollections.observableArrayList(levelInfo));
        mapFileSelector.setItems(FXCollections.observableArrayList(MAPLevel.values()));
        themeSelector.setItems(FXCollections.observableArrayList(FroggerMapTheme.values()));
        worldSelector.setItems(FXCollections.observableArrayList(WorldId.values()));
        musicSelector.setItems(FXCollections.observableArrayList(MusicTrack.values()));

        // Handlers:
        levelSelector.valueProperty().addListener((listener, oldVal, newVal) -> setLevel(newVal));
        mapFileSelector.valueProperty().addListener((listener, oldVal, newVal) -> getSelectedLevel().setLevel(newVal.ordinal()));
        worldSelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedLevel().setWorld(newValue));
        themeSelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedLevel().setTheme(newValue));
        musicSelector.valueProperty().addListener((observable, oldValue, newValue) -> getGameInstance().getMusicTracks().set(getSelectedLevel().getLevel().ordinal(), newValue));

        Utils.setHandleTestKeyPress(stackPosField, Utils::isInteger, newValue -> getSelectedLevel().setStackPosition(Integer.parseInt(newValue)));
        Utils.setHandleTestKeyPress(localLevelField, Utils::isInteger, newValue -> getSelectedLevel().setLocalLevelId(Integer.parseInt(newValue)));
        Utils.setHandleTestKeyPress(worldLevelField, Utils::isInteger, newValue -> getSelectedLevel().setLevelsInWorld(Integer.parseInt(newValue)));

        levelSelector.getSelectionModel().select(0);
    }

    private void setLevel(MAPLevel newLevel) {
        this.selectedLevel = getGameInstance().getLevel(newLevel);

        boolean hasLevelInfo = (this.selectedLevel != null);

        disableFields.forEach(node -> node.setDisable(!hasLevelInfo));
        if (!hasLevelInfo)
            return;

        stackPosField.setText(String.valueOf(getSelectedLevel().getStackPosition()));
        localLevelField.setText(String.valueOf(getSelectedLevel().getLocalLevelId()));
        worldLevelField.setText(String.valueOf(getSelectedLevel().getLevelsInWorld()));

        mapFileSelector.getSelectionModel().select(getSelectedLevel().getLevel());
        themeSelector.getSelectionModel().select(getSelectedLevel().getTheme());
        worldSelector.getSelectionModel().select(getSelectedLevel().getWorld());

        MusicTrack selectedTrack = getGameInstance().getMusic(newLevel);
        if (selectedTrack != null) {
            musicSelector.getSelectionModel().select(selectedTrack); // Select the current track.
        } else {
            musicSelector.setDisable(true); // There is no entry present currently.
        }
    }

    @FXML
    private void onDone(ActionEvent evt) {
        closeWindow(); // Close this window.
    }

    /**
     * Open the level info controller.
     */
    public static void openEditor(FroggerGameInstance instance) {
        Utils.createWindowFromFXMLTemplate("edit-hardcoded-level-info", new LevelInfoController(instance), "Level Stack Editor", true);
    }
}