package net.highwayfrogs.editor.gui.extra;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.config.exe.general.FormDeathType;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
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
    @FXML private TextField flagField;
    @FXML private Label themeLabel;
    @FXML private Label localLabel;
    @FXML private Label globalLabel;
    private Stage stage;
    private FroggerEXEInfo config;
    private FormEntry selectedEntry;

    private List<Node> disableNodes;

    private FormEntryController(Stage stage, FroggerEXEInfo config) {
        this.stage = stage;
        this.config = config;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.disableNodes = Arrays.asList(entitySelector, deathSelector, wadIndexField, scriptIdField, flagField, themeLabel, localLabel, globalLabel);

        formSelector.setItems(FXCollections.observableArrayList(getConfig().getFullFormBook()));
        formSelector.setConverter(new AbstractStringConverter<>(FormEntry::getFormName));
        entitySelector.setItems(FXCollections.observableArrayList(Utils.getIntegerList(getConfig().getEntityBank().size())));
        entitySelector.setConverter(new AbstractStringConverter<>(getConfig().getEntityBank()::getName));
        deathSelector.setItems(FXCollections.observableArrayList(FormDeathType.values()));

        // Handlers:
        formSelector.valueProperty().addListener((listener, oldVal, newVal) -> setEntry(newVal));
        entitySelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedEntry().setEntityType(newValue));
        deathSelector.valueProperty().addListener((observable, oldValue, newValue) -> getSelectedEntry().setDeathType(newValue));
        Utils.setHandleTestKeyPress(wadIndexField, Utils::isInteger, newValue -> getSelectedEntry().setId(Integer.parseInt(newValue)));
        Utils.setHandleTestKeyPress(scriptIdField, Utils::isInteger, newValue -> getSelectedEntry().setScriptId(Integer.parseInt(newValue)));
        Utils.setHandleTestKeyPress(flagField, Utils::isInteger, newValue -> getSelectedEntry().setFlags(Integer.parseInt(newValue)));

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
        flagField.setText(String.valueOf(newEntry.getFlags()));
        themeLabel.setText("Theme: " + newEntry.getTheme());
        localLabel.setText("Local ID: " + newEntry.getLocalFormId());
        globalLabel.setText("Global ID: " + newEntry.getGlobalFormId());
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
