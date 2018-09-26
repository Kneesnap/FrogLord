package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.gui.editor.SaveController;

import java.io.File;

public class GUIMain extends Application {
    public static Stage MAIN_STAGE;
    @Getter private static File workingDirectory = new File("./");

    @Override
    public void start(Stage primaryStage) throws Exception {
        MAIN_STAGE = primaryStage;
        SystemOutputReplacement.activateReplacement();

        // Debug = automatically load files for convenience.
        File folder = new File("debug");
        if (folder.exists() && folder.isDirectory()) {
            File mwiFile = new File(folder, "VANILLA.MWI");
            File mwdFile = new File(folder, "VANILLA.MWD");

            if (mwiFile.exists() && mwdFile.exists()) {
                openGUI(primaryStage, mwiFile, mwdFile);
                return;
            }
        }

        // If this isn't a debug setup, prompt the user to select the files to load.
        File mwdFile = promptFile("MWD", "Medievil WAD");
        File mwiFile = promptFile("MWI", "Medievil WAD Index");
        openGUI(primaryStage, mwiFile, mwdFile);
    }

    private File promptFile(String extension, String description) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select " + extension + " to open.");
        fileChooser.getExtensionFilters().add(new ExtensionFilter(description, "*." + extension));
        fileChooser.setInitialDirectory(getWorkingDirectory());

        File result = fileChooser.showOpenDialog(MAIN_STAGE);
        if (result == null)
            return promptFile(extension, description);

        setWorkingDirectory(result.getParentFile());
        return result;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void openGUI(Stage primaryStage, File mwiFile, File mwdFile) throws Exception {
        Parent root = FXMLLoader.load(Utils.getResource("javafx/main.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("FrogLord");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(400);
        primaryStage.show();

        MWIFile mwi = new MWIFile();
        mwi.load(new DataReader(new FileSource(mwiFile)));

        MWDFile mwd = new MWDFile(mwi);
        mwd.load(new DataReader(new FileSource(mwdFile)));

        MainController.MAIN_WINDOW.loadMWD(mwd);

        // Ctrl + S -> Save MWD.
        KeyCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.CONTROL_DOWN);
        scene.setOnKeyPressed(event -> {
            if (ctrlS.match(event))
                saveFiles(mwi, mwd, mwdFile.getParentFile());
        });

    }

    @SneakyThrows
    private static void saveFiles(MWIFile loadedMWI, MWDFile loadedMWD, File folder) {
        FXMLLoader loader = new FXMLLoader(Utils.getResource("javafx/save.fxml"));

        SaveController controller = new SaveController();
        loader.setController(controller);
        AnchorPane anchorPane = loader.load();

        Stage newStage = new Stage();
        newStage.setTitle("Saving MWD");
        newStage.setScene(new Scene(anchorPane));
        newStage.setMinWidth(200);
        newStage.setMinHeight(100);

        controller.onInit(newStage);
        controller.startSaving(loadedMWD, loadedMWI, folder);

        newStage.initModality(Modality.APPLICATION_MODAL);
        newStage.initOwner(MAIN_STAGE);
        newStage.showAndWait();
    }

    /**
     * Set the current directory to open FileChoosers in.
     * @param directory The directory to set.
     */
    public static void setWorkingDirectory(File directory) {
        if (directory != null && directory.isDirectory())
            workingDirectory = directory;
    }
}
