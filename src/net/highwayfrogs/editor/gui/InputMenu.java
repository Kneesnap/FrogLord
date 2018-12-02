package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;

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
    @SneakyThrows
    public static void promptInput(String prompt, Consumer<String> handler) {
        FXMLLoader loader = new FXMLLoader(Utils.getResource("javafx/input.fxml"));

        Stage newStage = new Stage();
        newStage.setTitle("Please Reply.");

        InputController controller = new InputController(newStage, prompt, handler);
        loader.setController(controller);
        AnchorPane anchorPane = loader.load();

        newStage.setScene(new Scene(anchorPane));
        newStage.setMinWidth(200);
        newStage.setMinHeight(100);

        newStage.initModality(Modality.WINDOW_MODAL);
        newStage.setAlwaysOnTop(true);
        newStage.initOwner(GUIMain.MAIN_STAGE);
        newStage.showAndWait();
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
            Platform.runLater(textField::requestFocus);
        }

        @FXML
        private void onAccept(ActionEvent event) {
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
