package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormDeathType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry.FroggerFormLibFlag;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Controls form entry information.
 * Created by Kneesnap on 3/15/2019.
 */
@Getter
public class FormEntryController extends GameUIController<FroggerGameInstance> {
    @FXML private ComboBox<FroggerFormEntry> formSelector;
    @FXML private ComboBox<Integer> entitySelector;
    @FXML private ComboBox<FroggerFormDeathType> deathSelector;
    @FXML private TextField wadIndexField;
    @FXML private TextField scriptIdField;
    @FXML private GridPane flagGrid;
    @FXML private Label themeLabel;
    @FXML private Label localLabel;
    @FXML private Label globalLabel;
    @FXML private Button editButton;
    @FXML private ComboBox<Integer> scriptSelector;
    @FXML private Label scriptIdLabel;
    private FroggerFormEntry selectedEntry;

    private List<Node> disableNodes;
    private final CheckBox[] flagToggleMap = new CheckBox[FroggerFormLibFlag.values().length];

    private FormEntryController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public FroggerConfig getConfig() {
        return (FroggerConfig) super.getConfig();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.disableNodes = Arrays.asList(entitySelector, deathSelector, wadIndexField, scriptIdField, flagGrid, themeLabel, localLabel, globalLabel);

        // Setup UI based on whether script names are set.
        boolean useNames = getGameInstance().getScripts().size() > 0;
        editButton.setVisible(useNames);
        scriptSelector.setVisible(useNames);
        scriptIdField.setVisible(!useNames);
        scriptIdLabel.setText(useNames ? "Script: " : "Script ID: ");

        // Setup:
        formSelector.setItems(FXCollections.observableArrayList(getGameInstance().getFullFormBook()));
        formSelector.setConverter(new AbstractStringConverter<>(FroggerFormEntry::getFormTypeName));
        entitySelector.setItems(FXCollections.observableArrayList(Utils.getIntegerList(getConfig().getEntityBank().size())));
        entitySelector.setConverter(new AbstractStringConverter<>(getConfig().getEntityBank()::getName));
        deathSelector.setItems(FXCollections.observableArrayList(FroggerFormDeathType.values()));
        scriptSelector.setItems(FXCollections.observableArrayList(Utils.getIntegerList(getGameInstance().getScripts().size())));
        scriptSelector.setConverter(new AbstractStringConverter<>(getConfig().getScriptBank()::getName));

        // Handlers:
        formSelector.valueProperty().addListener((listener, oldVal, newVal) -> setEntry(newVal));
        entitySelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedEntry().setEntityType(newValue));
        deathSelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedEntry().setDeathType(newValue));
        scriptSelector.valueProperty().addListener(((observable, oldValue, newValue) -> getSelectedEntry().setScriptId(newValue)));
        FXUtils.setHandleTestKeyPress(wadIndexField, NumberUtils::isInteger, newValue -> getSelectedEntry().setId(Integer.parseInt(newValue)));
        FXUtils.setHandleTestKeyPress(scriptIdField, NumberUtils::isInteger, newValue -> getSelectedEntry().setScriptId(Integer.parseInt(newValue)));
        editButton.setOnAction(evt -> ScriptEditorController.openEditor(getGameInstance(), getGameInstance().getScripts().get(this.scriptSelector.getValue())));

        for (int i = 0; i < FroggerFormLibFlag.values().length; i++) {
            FroggerFormLibFlag lib = FroggerFormLibFlag.values()[i];
            int row = (i / 2);
            int column = (i % 2);

            CheckBox newToggle = new CheckBox(lib.getDisplayName());
            GridPane.setRowIndex(newToggle, row);
            GridPane.setColumnIndex(newToggle, column);
            flagGrid.getChildren().add(newToggle);
            newToggle.selectedProperty().addListener((observable, oldVal, newVal) -> getSelectedEntry().setFlag(lib, newVal));
            flagToggleMap[i] = newToggle;
        }

        formSelector.getSelectionModel().select(0);
    }

    private void setEntry(FroggerFormEntry newEntry) {
        this.selectedEntry = newEntry;

        boolean hasLevelInfo = (this.selectedEntry != null);
        disableNodes.forEach(node -> node.setDisable(!hasLevelInfo));
        if (!hasLevelInfo)
            return;

        entitySelector.getSelectionModel().select(newEntry.getEntityType());
        FXUtils.comboBoxScrollToValue(entitySelector);
        deathSelector.getSelectionModel().select(newEntry.getDeathType());
        wadIndexField.setText(String.valueOf(newEntry.getId()));
        scriptIdField.setText(String.valueOf(newEntry.getScriptId()));
        scriptSelector.setValue(newEntry.getScriptId());
        scriptSelector.getSelectionModel().select(newEntry.getScriptId());


        themeLabel.setText("Theme: " + newEntry.getTheme());
        localLabel.setText("Local ID: " + newEntry.getLocalFormId());
        globalLabel.setText("Global ID: " + newEntry.getGlobalFormId());

        for (FroggerFormLibFlag flag : FroggerFormLibFlag.values())
            flagToggleMap[flag.ordinal()].setSelected(newEntry.testFlag(flag));
    }

    @FXML
    private void onDone(ActionEvent evt) {
        closeWindow(); // Close this window.
    }

    /**
     * Open the level info controller.
     */
    public static void openEditor(FroggerGameInstance instance) {
        FXUtils.createWindowFromFXMLTemplate("edit-hardcoded-form-entry", new FormEntryController(instance), "Form Library Editor", true);
    }
}