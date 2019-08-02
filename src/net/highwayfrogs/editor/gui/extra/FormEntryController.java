package net.highwayfrogs.editor.gui.extra;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.general.FormDeathType;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry.FormLibFlag;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.editor.ScriptEditorController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controls form entry information.
 * Created by Kneesnap on 3/15/2019.
 */
@Getter
public class FormEntryController implements Initializable {
    @FXML private ComboBox<FormEntry> formSelector;
    @FXML private ComboBox<Integer> entitySelector;
    @FXML private ComboBox<FormDeathType> deathSelector;
    @FXML private TextField wadIndexField;
    @FXML private TextField scriptIdField;
    @FXML private GridPane flagGrid;
    @FXML private Label themeLabel;
    @FXML private Label localLabel;
    @FXML private Label globalLabel;
    @FXML private Button editButton;
    @FXML private ComboBox<Integer> scriptSelector;
    @FXML private Label scriptIdLabel;
    private Stage stage;
    private FroggerEXEInfo config;
    private FormEntry selectedEntry;

    private List<Node> disableNodes;
    private CheckBox[] flagToggleMap = new CheckBox[FormLibFlag.values().length];

    private FormEntryController(Stage stage, FroggerEXEInfo config) {
        this.stage = stage;
        this.config = config;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.disableNodes = Arrays.asList(entitySelector, deathSelector, wadIndexField, scriptIdField, flagGrid, themeLabel, localLabel, globalLabel);

        // Setup UI based on whether or not script names are set.
        boolean useNames = GUIMain.EXE_CONFIG.getScripts().size() > 0;
        editButton.setVisible(useNames);
        scriptSelector.setVisible(useNames);
        scriptIdField.setVisible(!useNames);
        scriptIdLabel.setText(useNames ? "Script: " : "Script ID: ");

        // Setup:
        formSelector.setItems(FXCollections.observableArrayList(getConfig().getFullFormBook()));
        formSelector.setConverter(new AbstractStringConverter<>(FormEntry::getFormName));
        entitySelector.setItems(FXCollections.observableArrayList(Utils.getIntegerList(getConfig().getEntityBank().size())));
        entitySelector.setConverter(new AbstractStringConverter<>(getConfig().getEntityBank()::getName));
        deathSelector.setItems(FXCollections.observableArrayList(FormDeathType.values()));
        scriptSelector.setItems(FXCollections.observableArrayList(Utils.getIntegerList(GUIMain.EXE_CONFIG.getScripts().size())));
        scriptSelector.setConverter(new AbstractStringConverter<>(GUIMain.EXE_CONFIG.getScriptBank()::getName));

        // Handlers:
        formSelector.valueProperty().addListener((listener, oldVal, newVal) -> setEntry(newVal));
        entitySelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedEntry().setEntityType(newValue));
        deathSelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedEntry().setDeathType(newValue));
        scriptSelector.valueProperty().addListener(((observable, oldValue, newValue) -> getSelectedEntry().setScriptId(newValue)));
        Utils.setHandleTestKeyPress(wadIndexField, Utils::isInteger, newValue -> getSelectedEntry().setId(Integer.parseInt(newValue)));
        Utils.setHandleTestKeyPress(scriptIdField, Utils::isInteger, newValue -> getSelectedEntry().setScriptId(Integer.parseInt(newValue)));
        editButton.setOnAction(evt -> ScriptEditorController.openEditor(GUIMain.EXE_CONFIG.getScripts().get(this.scriptSelector.getValue())));

        for (int i = 0; i < FormLibFlag.values().length; i++) {
            FormLibFlag lib = FormLibFlag.values()[i];
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

    private void setEntry(FormEntry newEntry) {
        this.selectedEntry = newEntry;

        boolean hasLevelInfo = (this.selectedEntry != null);
        disableNodes.forEach(node -> node.setDisable(!hasLevelInfo));
        if (!hasLevelInfo)
            return;

        entitySelector.getSelectionModel().select(newEntry.getEntityType());
        Utils.comboBoxScrollToValue(entitySelector);
        deathSelector.getSelectionModel().select(newEntry.getDeathType());
        wadIndexField.setText(String.valueOf(newEntry.getId()));
        scriptIdField.setText(String.valueOf(newEntry.getScriptId()));
        scriptSelector.setValue(newEntry.getScriptId());
        scriptSelector.getSelectionModel().select(newEntry.getScriptId());


        themeLabel.setText("Theme: " + newEntry.getTheme());
        localLabel.setText("Local ID: " + newEntry.getLocalFormId());
        globalLabel.setText("Global ID: " + newEntry.getGlobalFormId());

        for (FormLibFlag flag : FormLibFlag.values())
            flagToggleMap[flag.ordinal()].setSelected(newEntry.testFlag(flag));
    }

    @FXML
    private void onDone(ActionEvent evt) {
        stage.close(); // Close this window.
    }

    /**
     * Open the level info controller.
     */
    public static void openEditor(FroggerEXEInfo info) {
        Utils.loadFXMLTemplate("form-entry", "Form Library Editor", newStage -> new FormEntryController(newStage, info));
    }
}
