package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.AllArgsConstructor;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    @FXML private SplitPane mainSplitPane;
    @FXML private ListView<GameFile> fileList;
    @FXML private AnchorPane editorPane;
    @FXML private TextArea consoleText;
    private MWDFile mwdFile;

    public static MainController MAIN_WINDOW;

    /**
     * Print a message to the console window.
     * @param message    The message to print.
     * @param formatting Any formatting to accompany the message.
     */
    public void printMessage(String message, Object... formatting) {
        if (consoleText != null)
            consoleText.appendText(String.format(message, formatting) + System.lineSeparator());
    }

    /**
     * Load a MWDFile as the active MWD being used.
     * @param file The MWD file to load.
     */
    public void loadMWD(MWDFile file) {
        this.mwdFile = file;

        ObservableList<GameFile> gameFiles = FXCollections.observableArrayList(mwdFile.getFiles());
        fileList.setItems(gameFiles);
        fileList.setCellFactory(param -> new AttachmentListCell(mwdFile));

        fileList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
                System.out.println("You have selected " + mwdFile.getEntryMap().get(newValue).getDisplayName() + ". Later, this will open the editor menu."));
    }


    @AllArgsConstructor
    private static class AttachmentListCell extends ListCell<GameFile> {
        private MWDFile mwdFile;

        @Override
        public void updateItem(GameFile file, boolean empty) {
            super.updateItem(file, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            FileEntry entry = mwdFile.getEntryMap().get(file);
            setGraphic(new ImageView(file.getIcon()));
            setText(entry.getDisplayName());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MAIN_WINDOW = this;
        System.out.println("Hello from FrogLord.");
    }
}
