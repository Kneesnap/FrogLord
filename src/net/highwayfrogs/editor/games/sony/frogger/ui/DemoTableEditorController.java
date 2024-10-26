package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.exe.general.DemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
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
    private DemoTableEntry selectedEntry;
    @FXML private ChoiceBox<DemoTableEntry> demoSelector;
    @FXML private ChoiceBox<MAPLevel> levelSelector;
    @FXML private ChoiceBox<DemoFile> fileSelector;
    @FXML private ChoiceBox<MAPLevel> unlockSelector;

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
        this.demoSelector.setConverter(new AbstractIndexStringConverter<>(getGameInstance().getDemoTableEntries(), (index, entry) -> "#" + (index + 1) + ", " + (entry.isValidData() ? FileUtils.stripExtension(getGameInstance().getResourceEntryByID(entry.getDemoResourceFile()).getDisplayName()) : "SKIPPED")));
        this.demoSelector.valueProperty().addListener(((observable, oldValue, newValue) -> selectEntry(newValue)));

        List<DemoFile> demoFiles = getGameInstance().getMainArchive().getAllFiles(DemoFile.class);
        levelSelector.setItems(FXCollections.observableArrayList(MAPLevel.values()));
        fileSelector.setItems(FXCollections.observableArrayList(demoFiles));
        unlockSelector.setItems(FXCollections.observableArrayList(MAPLevel.values()));

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

    private void selectEntry(DemoTableEntry newEntry) {
        this.selectedEntry = newEntry;
        this.levelSelector.setDisable(!newEntry.isValidData());
        this.fileSelector.setDisable(!newEntry.isValidData());
        this.unlockSelector.setDisable(!newEntry.isValidData());

        if (newEntry.isValidData()) {
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