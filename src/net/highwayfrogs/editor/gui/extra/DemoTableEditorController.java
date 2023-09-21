package net.highwayfrogs.editor.gui.extra;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.DemoFile;
import net.highwayfrogs.editor.file.config.data.MAPLevel;
import net.highwayfrogs.editor.file.config.exe.general.DemoTableEntry;
import net.highwayfrogs.editor.games.sony.SCGameObject;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Manages the demo editor.
 * Created by Kneesnap on 11/21/2019.
 */
public class DemoTableEditorController extends SCGameObject<FroggerGameInstance> implements Initializable {
    private final Stage stage;
    private DemoTableEntry selectedEntry;
    @FXML private ChoiceBox<DemoTableEntry> demoSelector;
    @FXML private ChoiceBox<MAPLevel> levelSelector;
    @FXML private ChoiceBox<DemoFile> fileSelector;
    @FXML private ChoiceBox<MAPLevel> unlockSelector;

    public DemoTableEditorController(FroggerGameInstance instance, Stage stage) {
        super(instance);
        this.stage = stage;
    }

    @FXML
    private void onDone(ActionEvent evt) {
        this.stage.close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Utils.closeOnEscapeKey(this.stage, null);
        this.demoSelector.setItems(FXCollections.observableArrayList(getGameInstance().getDemoTableEntries()));
        this.demoSelector.setConverter(new AbstractIndexStringConverter<>(getGameInstance().getDemoTableEntries(), (index, entry) -> "#" + (index + 1) + ", " + (entry.isValidData() ? Utils.stripExtension(getGameInstance().getResourceEntryByID(entry.getDemoResourceFile()).getDisplayName()) : "SKIPPED")));
        this.demoSelector.valueProperty().addListener(((observable, oldValue, newValue) -> selectEntry(newValue)));

        List<DemoFile> demoFiles = getArchive().getAllFiles(DemoFile.class);
        levelSelector.setItems(FXCollections.observableArrayList(MAPLevel.values()));
        fileSelector.setItems(FXCollections.observableArrayList(demoFiles));
        unlockSelector.setItems(FXCollections.observableArrayList(MAPLevel.values()));

        fileSelector.setConverter(new AbstractIndexStringConverter<>(demoFiles, (index, entry) -> Utils.stripExtension(entry.getFileDisplayName())));

        levelSelector.valueProperty().addListener(((observable, oldValue, newValue) -> this.selectedEntry.setLevel(newValue)));
        fileSelector.valueProperty().addListener(((observable, oldValue, newValue) -> this.selectedEntry.setDemoResourceFile(newValue.getIndexEntry().getResourceId())));
        unlockSelector.valueProperty().addListener(((observable, oldValue, newValue) -> this.selectedEntry.setUnlockLevel(newValue)));

        this.demoSelector.getSelectionModel().selectFirst();
        selectEntry(getGameInstance().getDemoTableEntries().get(0));
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
        Utils.loadFXMLTemplate(instance, "demo-table", "Demo Table Editor", DemoTableEditorController::new);
    }
}