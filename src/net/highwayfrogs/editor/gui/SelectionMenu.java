package net.highwayfrogs.editor.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.FXUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
    public static <T> void promptSelection(GameInstance instance, String prompt, Consumer<T> handler, Collection<T> values, Function<T, String> nameFunction, Function<T, Image> imageFunction) {
        FXUtils.createWindowFromFXMLTemplate("window-wait-for-user-select", new SelectionController<>(instance, prompt, handler, values, nameFunction, imageFunction, false), "Waiting for selection...", true);
    }

    /**
     * Require the user to perform a selection.
     * @param prompt  The prompt to display the user.
     * @param handler The behavior to execute when the user accepts.
     */
    public static <T> void promptSelectionAllowNull(GameInstance instance, String prompt, Consumer<T> handler, Collection<T> values, Function<T, String> nameFunction, Function<T, Image> imageFunction) {
        FXUtils.createWindowFromFXMLTemplate("window-wait-for-user-select", new SelectionController<>(instance, prompt, handler, values, nameFunction, imageFunction, true), "Waiting for selection...", false);
    }

    /**
     * Allow selecting a theme.
     * @param instance  The game instance to load.
     * @param handler   The handler to fire to handle selecting the theme.
     * @param allowNull Allow selecting null theme.
     */
    public static void promptThemeSelection(FroggerGameInstance instance, Consumer<FroggerMapTheme> handler, boolean allowNull) {
        List<FroggerMapTheme> themes = new ArrayList<>(FroggerMapTheme.values().length + (allowNull ? 1 : 0));
        if (allowNull)
            themes.add(null);
        themes.addAll(Arrays.asList(FroggerMapTheme.values()));

        promptSelection(instance, "Select the theme.", handler, themes, theme -> theme != null ? theme.name() : "No Theme", theme -> {
            if (theme == null)
                return null;

            for (FroggerLevelSelectEntry levelSelectEntry : instance.getArcadeLevelSelectEntries()) {
                if (levelSelectEntry.getTheme() != theme)
                    continue;

                VloImage image = levelSelectEntry.getWorldLevelStackColoredImage();
                return image != null ? image.toFXImage() : null; // There are many reasons why we might not find the image, including there might not be one, or this is the PS1 version which has these images in wads.
            }

            return null;
        });
    }

    public static class SelectionController<T> extends GameUIController<GameInstance> {
        @FXML private Label promptText;
        @FXML private ListView<T> optionList;
        @FXML private Button accept;

        private final String prompt;
        private final Consumer<T> handler;
        private final Collection<T> values;
        private final Function<T, String> nameFunction;
        private final Function<T, Image> imageFunction;
        private final boolean handleNull;
        private boolean handlerAccepted;

        public SelectionController(GameInstance instance, String prompt, Consumer<T> handler, Collection<T> values, Function<T, String> nameFunction, Function<T, Image> imageFunction, boolean handleNull) {
            super(instance);
            this.values = values;
            this.prompt = prompt;
            this.handler = handler;
            this.nameFunction = nameFunction;
            this.imageFunction = imageFunction;
            this.handleNull = handleNull;
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            ObservableList<T> fxOptions = FXCollections.observableArrayList(values);
            optionList.setItems(fxOptions);
            optionList.setCellFactory(param -> new AttachmentListCell<>(nameFunction, imageFunction));
            optionList.getSelectionModel().selectFirst();
            promptText.setText(prompt);

            Platform.runLater(() -> {
                        Stage stage = getStage();
                        if (stage != null) {
                            stage.setOnCloseRequest(event -> {
                                if (!this.handlerAccepted && this.handleNull) {
                                    this.handlerAccepted = true;
                                    this.handler.accept(null);
                                }
                            });
                        }
                    });

            // Since the l
            optionList.setOnKeyPressed(evt -> {
                if (evt.getCode() == KeyCode.ESCAPE)
                    closeWindow();
            });
        }

        @FXML
        private void onAccept(ActionEvent event) {
            this.handlerAccepted = true;
            handler.accept(optionList.getSelectionModel().getSelectedItem());
            closeWindow();
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