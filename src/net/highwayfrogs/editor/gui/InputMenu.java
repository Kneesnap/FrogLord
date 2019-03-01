package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Prompt the user to reply.
 * Created by Kneesnap on 12/2/2018.
 */
public class InputMenu {

    /**
     * Require the user to perform a selection.
     * @param prompt  The prompt to display the user.
     * @param handler The behavior to execute when the user accepts.
     */
    public static void promptInput(String prompt, Consumer<String> handler) {
        Utils.loadFXMLTemplate("input", "Waiting for user input...", newStage -> new InputController(newStage, prompt, handler));
    }

    public static class InputController implements Initializable {
        @FXML private Label promptText;
        @FXML private TextField textField;

        private Stage stage;
        private String text;
        private Consumer<String> handler;

        public InputController(Stage stage, String promptText, Consumer<String> handler) {
            this.text = promptText;
            this.handler = handler;
            this.stage = stage;
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            this.promptText.setText(this.text);

            this.textField.setOnKeyPressed(evt -> {
                if (evt.getCode() == KeyCode.ENTER)
                    attemptSubmit();
            });

            Platform.runLater(textField::requestFocus);
        }

        @FXML
        private void onAccept(ActionEvent event) {
            attemptSubmit();
        }

        private void attemptSubmit() {
            String response = this.textField.getText();
            if (response == null || response.isEmpty())
                return;

            this.stage.close();
            handler.accept(response);
        }

        @FXML
        private void onCancel(ActionEvent event) {
            this.stage.close();
        }
    }
}
