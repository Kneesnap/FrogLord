package net.highwayfrogs.editor.gui.editor;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.script.FroggerScript;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptCommandType;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Manages the script editor.
 * Created by Kneesnap on 8/1/2019.
 */
public class ScriptEditorController implements Initializable {
    @FXML private ChoiceBox<FroggerScript> scriptSelector;
    @FXML private VBox commandEditors;
    @FXML private TextFlow codeArea;
    @FXML private Button doneButton;
    @FXML private Label warningLabel; //TODO: Warn about the possibility of it being too large.
    @FXML private Button printUsagesButton;

    private Stage stage;

    private static final String STYLE_TEST1 = "-fx-fill: #4F8A10;-fx-font-weight:bold;-fx-font-family: Consolas;";
    private static final String STYLE_TEST2 = "-fx-fill: RED;-fx-font-weight:normal;-fx-font-family: Consolas;";

    public ScriptEditorController(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup Selector.
        scriptSelector.setConverter(new AbstractStringConverter<>(FroggerScript::getName));
        scriptSelector.setItems(FXCollections.observableArrayList(GUIMain.EXE_CONFIG.getScripts()));
        scriptSelector.valueProperty().addListener(((observable, oldValue, newValue) -> updateCodeDisplay()));
        scriptSelector.getSelectionModel().selectFirst();

        // Setup Buttons.
        doneButton.setOnAction(evt -> stage.close());
        printUsagesButton.setOnAction(evt -> {
            FroggerScript script = this.scriptSelector.getValue();
            int id = GUIMain.EXE_CONFIG.getScripts().indexOf(script);
            System.out.println("Usages of " + script.getName() + " (" + id + "):");
            for (FormEntry entry : GUIMain.EXE_CONFIG.getFullFormBook())
                if (entry.getScriptId() == id)
                    System.out.println(" - " + entry.getFormName());
        });

        Utils.closeOnEscapeKey(stage, null);
        updateCodeDisplay();
    }

    /**
     * Updates the code display.
     */
    public void updateCodeDisplay() {
        commandEditors.getChildren().clear();
        codeArea.getChildren().clear();
        FroggerScript currentScript = this.scriptSelector.getValue();
        if (currentScript == null)
            return;

        // Update editors.
        for (ScriptCommand command : currentScript.getCommands()) {
            GridPane pane = new GridPane();
            pane.setMinHeight(10);
            pane.setPrefHeight(30);
            VBox.setVgrow(pane, Priority.SOMETIMES);
            pane.setMinWidth(10);
            pane.setPrefWidth(100);
            HBox.setHgrow(pane, Priority.SOMETIMES);

            pane.addRow(0);
            for (int i = 0; i < command.getCommandType().getSize(); i++) {
                Node node;

                if (i == 0) { //TODO: On update.
                    ComboBox<ScriptCommandType> typeChoiceBox = new ComboBox<>();
                    typeChoiceBox.setItems(FXCollections.observableArrayList(ScriptCommandType.values()));
                    typeChoiceBox.getSelectionModel().select(command.getCommandType());
                    typeChoiceBox.setValue(command.getCommandType());
                    node = typeChoiceBox;
                } else {
                    node = new TextField(String.valueOf(command.getArguments()[i - 1]));
                }

                pane.addColumn(i, node);
            }
            commandEditors.getChildren().add(pane);
        }


        // Update text view.
        for (ScriptCommand command : currentScript.getCommands()) {
            Text commandTypeText = new Text(command.getCommandType().name());
            commandTypeText.setStyle(STYLE_TEST1);
            codeArea.getChildren().add(commandTypeText);

            for (int argument : command.getArguments()) { //TODO: Use constants and apply color.
                codeArea.getChildren().add(new Text(" "));
                Text toAdd = new Text(String.valueOf(argument));
                toAdd.setStyle(STYLE_TEST2);
                codeArea.getChildren().add(toAdd);
            }

            codeArea.getChildren().add(new Text(Constants.NEWLINE));
        }
    }

    /**
     * Opens the Script Editor.
     */
    public static void openEditor() {
        Utils.loadFXMLTemplate("script", "Script Editor", ScriptEditorController::new);
    }
}
