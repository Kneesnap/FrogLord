package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerDemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;

import java.util.List;

/**
 * Manages the demo editor.
 * Created by Kneesnap on 11/21/2019.
 */
public class DemoTableEditorController extends GameUIController<FroggerGameInstance> {
    private FroggerDemoTableEntry selectedEntry;
    @FXML private ChoiceBox<FroggerDemoTableEntry> demoSelector;
    @FXML private ChoiceBox<FroggerMapLevelID> levelSelector;
    @FXML private ChoiceBox<DemoFile> fileSelector;
    @FXML private ChoiceBox<FroggerMapLevelID> unlockSelector;

    public DemoTableEditorController(FroggerGameInstance instance) {
        super(instance);
    }

    @FXML
    private void onDone(ActionEvent evt) {
        closeWindow();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.demoSelector.setItems(FXCollections.observableArrayList(getGameInstance().getDemoTableEntries()));
        this.demoSelector.setConverter(new AbstractIndexStringConverter<>(getGameInstance().getDemoTableEntries(), (index, entry) -> "#" + (index + 1) + ", " + (entry.isSkipped() ? "SKIPPED" : FileUtils.stripExtension(getGameInstance().getResourceEntryByID(entry.getDemoResourceFile()).getDisplayName()))));
        this.demoSelector.valueProperty().addListener(((observable, oldValue, newValue) -> selectEntry(newValue)));

        List<DemoFile> demoFiles = getGameInstance().getMainArchive().getAllFiles(DemoFile.class);
        levelSelector.setItems(FXCollections.observableArrayList(FroggerMapLevelID.values()));
        fileSelector.setItems(FXCollections.observableArrayList(demoFiles));
        unlockSelector.setItems(FXCollections.observableArrayList(FroggerMapLevelID.values()));

        fileSelector.setConverter(new AbstractIndexStringConverter<>(demoFiles, (index, entry) -> FileUtils.stripExtension(entry.getFileDisplayName())));

        levelSelector.valueProperty().addListener(((observable, oldValue, newValue) -> this.selectedEntry.setLevel(newValue)));
        fileSelector.valueProperty().addListener(((observable, oldValue, newValue) -> this.selectedEntry.setDemoResourceFile(newValue.getFileResourceId())));
        unlockSelector.valueProperty().addListener(((observable, oldValue, newValue) -> this.selectedEntry.setUnlockLevel(newValue)));

        this.demoSelector.getSelectionModel().selectFirst();
        selectEntry(getGameInstance().getDemoTableEntries().get(0));
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        Stage stage = (Stage) newScene.getWindow();
        FXUtils.closeOnEscapeKey(stage, null);
    }

    private void selectEntry(FroggerDemoTableEntry newEntry) {
        this.selectedEntry = newEntry;
        this.levelSelector.setDisable(newEntry.isSkipped());
        this.fileSelector.setDisable(newEntry.isSkipped());
        this.unlockSelector.setDisable(newEntry.isSkipped());

        if (!newEntry.isSkipped()) {
            this.levelSelector.getSelectionModel().select(newEntry.getLevel());
            this.fileSelector.getSelectionModel().select(getGameInstance().getGameFile(newEntry.getDemoResourceFile()));
            this.unlockSelector.getSelectionModel().select(newEntry.getUnlockLevel());
        }
    }

    /**
     * Opens the demo table Editor.
     */
    public static void openEditor(FroggerGameInstance instance) {
        FXUtils.createWindowFromFXMLTemplate("edit-hardcoded-demo-table", new DemoTableEditorController(instance), "Demo Table Editor", true);
    }
}