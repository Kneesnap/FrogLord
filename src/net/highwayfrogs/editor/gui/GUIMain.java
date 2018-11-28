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
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.gui.editor.SaveController;

import java.io.File;
import java.io.IOException;

public class GUIMain extends Application {
    public static Stage MAIN_STAGE;
    @Getter private static File workingDirectory = new File("./");
    public static FroggerEXEInfo EXE_CONFIG; //TODO: Allow selecting which version to use.

    @Override
    public void start(Stage primaryStage) throws Exception {
        MAIN_STAGE = primaryStage;
        SystemOutputReplacement.activateReplacement();

        // Debug = automatically load files for convenience.
        File folder = new File("debug");
        if (folder.exists() && folder.isDirectory()) {
            workingDirectory = folder;
            File mwiFile = new File(folder, "VANILLA.EXE");
            File mwdFile = new File(folder, "VANILLA.MWD");

            if (mwiFile.exists() && mwdFile.exists()) {
                openGUI(primaryStage, mwiFile, mwdFile);
                return;
            }
        }

        // If this isn't a debug setup, prompt the user to select the files to load.
        File mwdFile = promptFile("MWD", "Medievil WAD");
        File mwiFile = promptFile("EXE", "Frogger Executable");
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

    private void openGUI(Stage primaryStage, File exeFile, File mwdFile) throws Exception {
        Parent root = FXMLLoader.load(Utils.getResource("javafx/main.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("FrogLord");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(400);
        primaryStage.getIcons().add(GameFile.loadIcon("icon"));
        primaryStage.show();

        try {
            EXE_CONFIG = new FroggerEXEInfo(exeFile, new File(exeFile.getParentFile(), "frogger.exe"), Utils.getResourceStream("exes/30e.cfg"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        MWDFile mwd = new MWDFile(EXE_CONFIG.readMWI());
        mwd.load(new DataReader(new FileSource(mwdFile)));

        MainController controller = MainController.MAIN_WINDOW;
        controller.loadMWD(mwd);

        // Ctrl + S -> Save MWD.
        KeyCombination ctrlS = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.CONTROL_DOWN); // Save MWD.
        KeyCombination ctrlI = new KeyCodeCombination(KeyCode.I, KeyCodeCombination.CONTROL_DOWN); // Import file.
        KeyCombination ctrlO = new KeyCodeCombination(KeyCode.O, KeyCodeCombination.CONTROL_DOWN); // Output file.
        KeyCombination ctrlE = new KeyCodeCombination(KeyCode.E, KeyCodeCombination.CONTROL_DOWN); // Export Alternative.

        scene.setOnKeyPressed(event -> {
            if (ctrlS.match(event)) {
                saveFiles(EXE_CONFIG, mwd, mwdFile.getParentFile());
            } else if (ctrlI.match(event)) {
                controller.importFile();
            } else if (ctrlO.match(event)) {
                controller.exportFile();
            } else if (ctrlE.match(event)) {
                controller.getCurrentFile().exportAlternateFormat(controller.getFileEntry());
            }
        });

    }

    @SneakyThrows
    private static void saveFiles(FroggerEXEInfo froggerEXE, MWDFile loadedMWD, File folder) {
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
        controller.startSaving(loadedMWD, froggerEXE, folder);

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
