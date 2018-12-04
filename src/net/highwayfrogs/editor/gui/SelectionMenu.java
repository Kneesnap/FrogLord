package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.Utils;

import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Prompt the user to select something.
 * TODO: Center ListView + Button.
 * Created by Kneesnap on 11/8/2018.
 */
public class SelectionMenu {

    /**
     * Require the user to perform a selection.
     * @param prompt  The prompt to display the user.
     * @param handler The behavior to execute when the user accepts.
     */
    public static <T> void promptSelection(String prompt, Consumer<T> handler, Collection<T> values, Function<T, String> nameFunction, Function<T, ImageView> imageFunction) {
        Utils.loadFXMLTemplate("select", "Waiting for selection...", newStage -> new SelectionController<>(newStage, prompt, handler, values, nameFunction, imageFunction));
    }

    public static class SelectionController<T> implements Initializable {
        @FXML private Label promptText;
        @FXML private ListView<T> optionList;
        @FXML private Button accept;

        private String prompt;
        private Consumer<T> handler;
        private Stage stage;
        private Collection<T> values;
        private Function<T, String> nameFunction;
        private Function<T, ImageView> imageFunction;

        public SelectionController(Stage stage, String prompt, Consumer<T> handler, Collection<T> values, Function<T, String> nameFunction, Function<T, ImageView> imageFunction) {
            this.values = values;
            this.stage = stage;
            this.prompt = prompt;
            this.handler = handler;
            this.nameFunction = nameFunction;
            this.imageFunction = imageFunction;
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            ObservableList<T> fxOptions = FXCollections.observableArrayList(values);
            optionList.setItems(fxOptions);
            optionList.setCellFactory(param -> new AttachmentListCell<>(nameFunction, imageFunction));
            optionList.getSelectionModel().selectFirst();
            promptText.setText(prompt);
            Utils.closeOnEscapeKey(stage, null);
        }

        @FXML
        private void onAccept(ActionEvent event) {
            handler.accept(optionList.getSelectionModel().getSelectedItem());
            this.stage.close();
        }
    }

    @AllArgsConstructor
    private static class AttachmentListCell<T> extends ListCell<T> {
        private Function<T, String> nameFunction;
        private Function<T, ImageView> imageFunction;

        @Override
        public void updateItem(T selection, boolean empty) {
            super.updateItem(selection, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            setText(nameFunction.apply(selection));
            if (imageFunction != null)
                setGraphic(imageFunction.apply(selection));
        }
    }
}
