package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.config.script.FroggerScript;
import net.highwayfrogs.editor.file.config.script.ScriptCommand;
import net.highwayfrogs.editor.file.config.script.ScriptCommandType;
import net.highwayfrogs.editor.file.config.script.format.BankFormatter;
import net.highwayfrogs.editor.file.config.script.format.ScriptFormatter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Manages the script editor.
 * Created by Kneesnap on 8/1/2019.
 */
public class ScriptEditorController extends GameUIController<FroggerGameInstance> {
    @FXML private ChoiceBox<FroggerScript> scriptSelector;
    @FXML private VBox commandEditors;
    @FXML private TextFlow codeArea;
    @FXML private Button doneButton;
    @FXML private Label warningLabel;
    @FXML private Label usagesLabel;
    private final int baseHeight;

    private FroggerScript openScript;

    private static final Font DISPLAY_FONT = Font.font("Consolas");
    private static final String COMMAND_TYPE_STYLE = "-fx-fill: #4F8A10;-fx-font-weight:bold;";

    public ScriptEditorController(FroggerGameInstance instance) {
        super(instance);
        this.baseHeight = 100;
    }

    public ScriptEditorController(FroggerGameInstance instance, FroggerScript openScript) {
        this(instance);
        this.openScript = openScript;
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        // Setup Selector.
        scriptSelector.setConverter(new AbstractStringConverter<>(FroggerScript::getName));
        scriptSelector.setItems(FXCollections.observableArrayList(getGameInstance().getScripts()));
        scriptSelector.valueProperty().addListener(((observable, oldValue, newValue) -> updateCodeDisplay()));
        if (this.openScript != null) {
            scriptSelector.setValue(this.openScript);
            scriptSelector.getSelectionModel().select(this.openScript);
            this.openScript = null;
        } else {
            scriptSelector.getSelectionModel().selectFirst();
        }

        // Setup Buttons.
        this.usagesLabel.setText(getUsagesOfScriptDescription());
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        this.doneButton.setOnAction(evt -> closeWindow());
        Utils.closeOnEscapeKey((Stage) newScene.getWindow(), null);
        updateCodeDisplay();
    }

    private String getUsagesOfScriptDescription() {
        FroggerScript script = this.scriptSelector.getValue();
        int id = getGameInstance().getScripts().indexOf(script);
        if (id <= 0) // The first script is SCRIPT_NONE.
            return "";

        StringBuilder results = new StringBuilder("Forms: ");

        // Find usages in form library.
        boolean foundAny = false;
        for (FormEntry entry : getGameInstance().getFullFormBook()) {
            if (entry.getScriptId() != id)
                continue;

            if (foundAny)
                results.append(", ");
            results.append(entry.getFormName());
            foundAny = true;
        }

        if (!foundAny)
            results.append("None");

        results.append(Constants.NEWLINE).append("Called by: ");

        // Find usages in other scripts.
        foundAny = false;
        for (FroggerScript otherScript : getGameInstance().getScripts()) {
            for (ScriptCommand command : otherScript.getCommands()) {
                for (int i = 0; i < command.getCommandType().getFormatters().length; i++) {
                    ScriptFormatter formatter = command.getCommandType().getFormatters()[i];
                    if (formatter == BankFormatter.SCRIPT_INSTANCE && id == command.getArguments()[i]) {
                        if (foundAny)
                            results.append(", ");
                        results.append(otherScript.getName());
                        foundAny = true;
                    }
                }
            }
        }

        if (!foundAny)
            results.append("None");

        return results.toString();
    }

    /**
     * Updates the code display.
     */
    public void updateCodeDisplay() {
        commandEditors.setSpacing(2);
        commandEditors.getChildren().clear();
        codeArea.getChildren().clear();
        this.warningLabel.setVisible(false);
        this.usagesLabel.setText(getUsagesOfScriptDescription());
        FroggerScript currentScript = this.scriptSelector.getValue();
        if (currentScript == null)
            return;

        this.warningLabel.setVisible(currentScript.isTooLarge());

        // Update editors.
        double cmdSize = 0;
        for (ScriptCommand command : currentScript.getCommands()) {
            GridPane pane = new GridPane();
            pane.setMinHeight(10);
            pane.setPrefHeight(30);
            VBox.setVgrow(pane, Priority.SOMETIMES);
            pane.setMinWidth(10);
            pane.setPrefWidth(100);
            HBox.setHgrow(pane, Priority.SOMETIMES);

            pane.setHgap(2);
            pane.addRow(0);
            for (int i = 0; i < command.getCommandType().getSize(); i++) {
                Node node;

                if (i == 0) {
                    ComboBox<ScriptCommandType> typeChoiceBox = new ComboBox<>();
                    typeChoiceBox.setItems(FXCollections.observableArrayList(ScriptCommandType.values()));
                    typeChoiceBox.getSelectionModel().select(command.getCommandType());
                    typeChoiceBox.setValue(command.getCommandType());
                    node = typeChoiceBox;

                    typeChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
                        command.setCommandType(newValue);
                        updateCodeDisplay();
                    }));
                } else {
                    node = command.getCommandType().getFormatters()[i - 1].makeEditor(getGameInstance(), this, command, i - 1);
                }

                pane.addColumn(i, node);
            }
            commandEditors.getChildren().add(pane);
            cmdSize += 25 + commandEditors.getSpacing();
        }


        // Update text view.
        double textSize = 0;
        for (ScriptCommand command : currentScript.getCommands()) {
            Text commandTypeText = new Text(command.getCommandType().name());
            commandTypeText.setStyle(COMMAND_TYPE_STYLE);
            commandTypeText.setFont(DISPLAY_FONT);
            codeArea.getChildren().add(commandTypeText);

            for (int i = 0; i < command.getArguments().length; i++) {
                codeArea.getChildren().add(new Text(" "));
                ScriptFormatter formatter = command.getCommandType().getFormatters()[i];
                Text toAdd = new Text(formatter.numberToString(getGameInstance(), command.getArguments()[i]));
                toAdd.setStyle(formatter.getTextStyle());
                toAdd.setFont(DISPLAY_FONT);
                codeArea.getChildren().add(toAdd);
            }

            codeArea.getChildren().add(new Text(Constants.NEWLINE));
            textSize += commandTypeText.getFont().getSize() * 1.5D;
        }

        // Update window size.
        this.commandEditors.setMinHeight(cmdSize);
        this.commandEditors.setPrefHeight(cmdSize);
        this.commandEditors.setMaxHeight(cmdSize);
        this.codeArea.setMinHeight(textSize);
        this.codeArea.setPrefHeight(textSize);

        double newHeight = this.baseHeight + cmdSize + textSize;
        Stage stage = isLoadingComplete() ? getStage() : null;
        if (stage != null) {
            stage.setMinHeight(newHeight);
            stage.setMaxHeight(newHeight);
            stage.setHeight(newHeight);
        }
    }

    /**
     * Opens the Script Editor.
     */
    public static void openEditor(FroggerGameInstance instance) {
        Utils.createWindowFromFXMLTemplate("window-script-editor", new ScriptEditorController(instance), "Script Editor", true);
    }

    /**
     * Opens the Script Editor and view a given script.
     */
    public static void openEditor(FroggerGameInstance instance, FroggerScript script) {
        Utils.createWindowFromFXMLTemplate("window-script-editor", new ScriptEditorController(instance, script), "Script Editor", true);
    }
}