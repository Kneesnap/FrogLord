package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.utils.FXUtils;
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
        FXUtils.createWindowFromFXMLTemplate("window-wait-for-user-input", new InputController(instance, prompt, handler, defaultText), "Waiting for user input...", true);
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

    /**
     * Require the user to perform a selection.
     * @param prompt The prompt to display the user.
     */
    public static String promptInputBlocking(GameInstance instance, String prompt, String defaultText, Consumer<String> handler) {
        AtomicReference<String> resultHolder = new AtomicReference<>(null);
        promptInput(instance, prompt, defaultText, newValue -> {
            resultHolder.set(newValue);
            if (handler != null)
                handler.accept(newValue);
        });
        return resultHolder.get();
    }

    /**
     * Prompts the user to respond with an integer value.
     * @param instance the game instance to prompt under
     * @param prompt the prompt to show to the user
     * @param startValue the initial value to put in the text box
     * @param handler the handler for handling an integer value. If an exception is thrown, the prompt response will be considered invalid.
     * @return integer if successful, or null to indicate there is no new value
     */
    public static Integer promptInputInt(GameInstance instance, String prompt, int startValue, Consumer<Integer> handler) {
        AtomicReference<Integer> resultHolder = new AtomicReference<>(null);
        InputMenu.promptInput(instance, prompt, String.valueOf(startValue), response -> {
            int parsedValue;
            try {
                parsedValue = Integer.parseInt(response);
            } catch (NumberFormatException nfe) {
                FXUtils.makePopUp("The value '" + response + "' cannot be interpreted as an integer!", AlertType.WARNING);
                return;
            }

            try {
                if (handler != null)
                    handler.accept(parsedValue);
            } catch (Throwable th) {
                Utils.handleError(instance.getLogger(), th, true);
                return;
            }

            resultHolder.set(parsedValue);
        });

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