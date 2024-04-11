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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.file.config.exe.LevelInfo;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Prompt the user to select something.
 * Created by Kneesnap on 11/8/2018.
 */
public class SelectionMenu {

    /**
     * Require the user to perform a selection.
     * @param prompt  The prompt to display the user.
     * @param handler The behavior to execute when the user accepts.
     */
    public static <T> void promptSelection(String prompt, Consumer<T> handler, Collection<T> values, Function<T, String> nameFunction, Function<T, Image> imageFunction) {
        Utils.loadFXMLTemplate(null, "window-wait-for-user-select", "Waiting for selection...", newStage -> new SelectionController<>(newStage, prompt, handler, values, nameFunction, imageFunction));
    }

    /**
     * Allow selecting a theme.
     * @param instance  The game instance to load.
     * @param handler   The handler to fire to handle selecting the theme.
     * @param allowNull Allow selecting null theme.
     */
    public static void promptThemeSelection(FroggerGameInstance instance, Consumer<MAPTheme> handler, boolean allowNull) {
        List<MAPTheme> themes = new ArrayList<>(MAPTheme.values().length + (allowNull ? 1 : 0));
        if (allowNull)
            themes.add(null);
        themes.addAll(Arrays.asList(MAPTheme.values()));

        promptSelection("Select the theme.", handler, themes, theme -> theme != null ? theme.name() : "No Theme", theme -> {
            if (theme == null)
                return null;

            for (LevelInfo levelInfo : instance.getArcadeLevelInfo()) {
                if (levelInfo.getTheme() != theme)
                    continue;

                GameImage image = instance.getImageFromPointer(levelInfo.getWorldImageSelectablePointer());
                return image != null ? image.toFXImage() : null; // There are many reasons why we might not find the image, including there might not be one, or this is the PS1 version which has these images in wads.
            }

            return null;
        });
    }

    public static class SelectionController<T> implements Initializable {
        @FXML private Label promptText;
        @FXML private ListView<T> optionList;
        @FXML private Button accept;

        private final String prompt;
        private final Consumer<T> handler;
        private final Stage stage;
        private final Collection<T> values;
        private final Function<T, String> nameFunction;
        private final Function<T, Image> imageFunction;

        public SelectionController(Stage stage, String prompt, Consumer<T> handler, Collection<T> values, Function<T, String> nameFunction, Function<T, Image> imageFunction) {
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

            // Since the l
            optionList.setOnKeyPressed(evt -> {
                if (evt.getCode() == KeyCode.ESCAPE)
                    stage.close();
            });
        }

        @FXML
        private void onAccept(ActionEvent event) {
            handler.accept(optionList.getSelectionModel().getSelectedItem());
            this.stage.close();
        }
    }

    @AllArgsConstructor
    public static class AttachmentListCell<T> extends ListCell<T> {
        private final Function<T, String> nameFunction;
        private final Function<T, Image> imageFunction;

        @Override
        public void updateItem(T selection, boolean empty) {
            super.updateItem(selection, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            setText(nameFunction.apply(selection));
            if (imageFunction != null) {
                ImageView newView = null;

                if (selection != null) {
                    newView = new ImageView(imageFunction.apply(selection));
                    newView.setFitWidth(25);
                    newView.setFitHeight(25);
                }

                setGraphic(newView);
            }
        }
    }
}