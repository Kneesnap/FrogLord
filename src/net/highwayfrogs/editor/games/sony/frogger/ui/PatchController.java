package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.patch.GamePatch;
import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.games.generic.IGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the script editor.
 * Created by Kneesnap on 8/1/2019.
 */
public class PatchController extends GameUIController<FroggerGameInstance> {
    @FXML private ChoiceBox<GamePatch> patchSelector;
    @FXML private VBox patchConfigEditors;
    @FXML private Button loadExternalButton;
    @FXML private Button applyButton;
    @FXML private Button doneButton;
    @FXML private Label nameLabel;
    @FXML private Label descLabel;
    @FXML private Label warningLabel;
    private final int baseHeight;

    private PatchRuntime selectedPatchRuntime;

    @Getter private static final List<GamePatch> patches = new ArrayList<>();

    public PatchController(FroggerGameInstance instance) {
        super(instance);
        this.baseHeight = 100;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Setup Selector.
        this.patchSelector.setConverter(new AbstractStringConverter<>(GamePatch::getName));

        List<GamePatch> compatiblePatches = new ArrayList<>(getPatches());
        compatiblePatches.removeIf(patch -> !patch.isCompatibleWithVersion(getConfig().getInternalName()));
        this.patchSelector.setItems(FXCollections.observableArrayList(compatiblePatches));
        this.patchSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
            this.selectedPatchRuntime = newValue != null ? new PatchRuntime(getGameInstance(), newValue) : null;
            if (this.selectedPatchRuntime != null && !this.selectedPatchRuntime.runSetup()) {
                Platform.runLater(this::closeWindow);
                return;
            }

            updatePatchDisplay();
        }));

        if (this.selectedPatchRuntime != null) {
            this.patchSelector.setValue(this.selectedPatchRuntime.getPatch());
            this.patchSelector.getSelectionModel().select(this.selectedPatchRuntime.getPatch());
        } else {
            this.patchSelector.getSelectionModel().selectFirst();
        }

        // Setup Buttons.
        doneButton.setOnAction(evt -> closeWindow());

        this.loadExternalButton.setOnAction(evt -> { // Load an external file as a patch.
            File patchFile = FXUtils.promptFileOpen(getGameInstance(), "Select the patch to load...", "Patch Files", "patch");
            if (patchFile == null)
                return;

            this.patchSelector.getSelectionModel().clearSelection();

            GamePatch loadPatch = new GamePatch();
            loadPatch.loadPatchFromConfig(new Config(FileUtils.readLinesFromFile(patchFile)));
            this.selectedPatchRuntime = new PatchRuntime(getGameInstance(), loadPatch);
            if (this.selectedPatchRuntime.runSetup()) { // Setup success.
                updatePatchDisplay();
            } else { // Setup failure.
                closeWindow();
            }
        });

        // Apply the patch when clicked.
        this.applyButton.setOnAction(evt -> {
            if (this.selectedPatchRuntime != null) {
                this.selectedPatchRuntime.run();
                if (!this.selectedPatchRuntime.isHadError())
                    FXUtils.makePopUp("Patch has been applied.", AlertType.INFORMATION);

                // Reset the patch.
                this.selectedPatchRuntime.runSetup();
                updatePatchDisplay();
            }
        });
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        FXUtils.closeOnEscapeKey((Stage) newScene.getWindow(), null);
        if (this.patchSelector.getItems() == null || this.patchSelector.getItems().isEmpty()) {
            FXUtils.makePopUp("There are no patches available for this version.", AlertType.ERROR);
            closeWindow();
        } else {
            updatePatchDisplay();
        }
    }

    /**
     * Updates the patch display.
     */
    public void updatePatchDisplay() {
        this.patchConfigEditors.getChildren().clear();
        this.applyButton.setDisable(this.selectedPatchRuntime == null || !this.selectedPatchRuntime.getPatch().isCompatibleWithVersion(getConfig().getInternalName()));

        // Return if there is no patch to update a display for.
        if (this.selectedPatchRuntime == null) {
            this.descLabel.setVisible(false);
            this.nameLabel.setVisible(false);
            this.warningLabel.setVisible(false);
            return;
        }

        this.nameLabel.setText(this.selectedPatchRuntime.getPatch().getName() + " (By " + this.selectedPatchRuntime.getPatch().getAuthor() + ")");
        this.nameLabel.setVisible(true);
        this.descLabel.setText(this.selectedPatchRuntime.getPatch().getDescription());
        this.descLabel.setVisible(true);

        String warning = getWarning();
        this.warningLabel.setVisible(warning != null);
        this.warningLabel.setText(warning != null ? "WARNING: " + warning : null);

        GamePatch currentPatch = this.selectedPatchRuntime.getPatch();

        // Update patch config display.
        int configSize = 5;
        for (PatchArgument argument : currentPatch.getArguments()) {
            GridPane pane = new GridPane();
            pane.setMinHeight(10);
            pane.setPrefHeight(30);
            VBox.setVgrow(pane, Priority.SOMETIMES);
            pane.setMinWidth(10);
            pane.setPrefWidth(100);
            HBox.setHgrow(pane, Priority.SOMETIMES);

            pane.addRow(0);
            pane.addColumn(0, new Label(argument.getDescription() + ": "));
            pane.setHgap(5);

            PatchValue varValue = this.selectedPatchRuntime.getVariable(argument.getName());
            Node valueNode = varValue.getType().getBehavior().createEditor(this, argument, varValue);
            pane.addColumn(1, valueNode);

            this.patchConfigEditors.getChildren().add(pane);
            configSize += 25;
        }

        // Update window size.
        this.patchConfigEditors.setMinHeight(configSize);
        this.patchConfigEditors.setPrefHeight(configSize);
        this.patchConfigEditors.setMaxHeight(configSize);

        int newHeight = this.baseHeight + configSize;
        Stage stage = isLoadingComplete() ? getStage() : null;
        if (stage != null) {
            stage.setMinHeight(newHeight);
            stage.setMaxHeight(newHeight);
            stage.setHeight(newHeight);
        }
    }

    private String getWarning() {
        if (!this.selectedPatchRuntime.getPatch().isCompatibleWithVersion(getConfig().getInternalName()))
            return "This patch is not compatible with the loaded Frogger version.";

        return null;
    }

    /**
     * Opens the Patch Menu.
     */
    public static void openMenu(FroggerGameInstance instance) {
        if (getPatches().isEmpty())
            loadPatches(instance.getGameType());

        FXUtils.createWindowFromFXMLTemplate("window-patch-menu", new PatchController(instance), "Patch Menu", true);
    }

    /**
     * Loads built-in patches.
     */
    public static void loadPatches(IGameType gameType) {
        getPatches().clear();

        for (URL patchLocation : FileUtils.getInternalResourceFilesInDirectory(gameType.getEmbeddedResourceURL("patches"), true)) {
            try {
                Config config = new Config(patchLocation.openStream());
                GamePatch loadPatch = new GamePatch();
                loadPatch.loadPatchFromConfig(config);
                getPatches().add(loadPatch);
            } catch (Throwable th) {
                System.err.println("Failed to load '" + patchLocation + "'.");
                th.printStackTrace();
            }
        }
    }
}