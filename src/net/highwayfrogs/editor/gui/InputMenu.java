package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.Utils;

import java.util.concurrent.atomic.AtomicReference;
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
    public static void promptInput(GameInstance instance, String prompt, Consumer<String> handler) {
        promptInput(instance, prompt, null, handler);
    }

    /**
     * Require the user to perform a selection.
     * @param prompt  The prompt to display the user.
     * @param handler The behavior to execute when the user accepts.
     */
    public static void promptInput(GameInstance instance, String prompt, String defaultText, Consumer<String> handler) {
        Utils.createWindowFromFXMLTemplate("window-wait-for-user-input", new InputController(instance, prompt, handler, defaultText), "Waiting for user input...", true);
    }

    /**
     * Require the user to perform a selection.
     * @param prompt The prompt to display the user.
     */
    public static String promptInput(GameInstance instance, String prompt, String defaultText) {
        AtomicReference<String> resultHolder = new AtomicReference<>(null);
        promptInput(instance, prompt, defaultText, resultHolder::set);
        return resultHolder.get();
    }

    public static class InputController extends GameUIController<GameInstance> {
        @FXML private Label promptText;
        @FXML private TextField textField;

        private final String text;
        private final Consumer<String> handler;
        private final String defaultText;

        public InputController(GameInstance instance, String promptText, Consumer<String> handler, String defaultText) {
            super(instance);
            this.text = promptText;
            this.handler = handler;
            this.defaultText = defaultText;
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            this.promptText.setText(this.text);

            if (this.defaultText != null)
                this.textField.setText(this.defaultText);

            this.textField.setOnKeyPressed(evt -> {
                if (evt.getCode() == KeyCode.ENTER)
                    attemptSubmit();
            });

            Platform.runLater(this.textField::requestFocus);
        }

        @FXML
        private void onAccept(ActionEvent event) {
            attemptSubmit();
        }

        private void attemptSubmit() {
            String response = this.textField.getText();
            if (response == null || response.isEmpty())
                return;

            closeWindow();
            this.handler.accept(response);
        }

        @FXML
        private void onCancel(ActionEvent event) {
            closeWindow();
        }
    }
}