package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;

import java.io.File;

public class GUIMain extends Application {
    public static Stage MAIN_STAGE;
    public static final File WORKING_DIRECTORY = new File("./");

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
        fileChooser.setInitialDirectory(WORKING_DIRECTORY);

        File result = fileChooser.showOpenDialog(MAIN_STAGE);
        return result != null ? result : promptFile(extension, description);
    }


    public static void main(String[] args) {
        launch(args);
    }

    private void openGUI(Stage primaryStage, File mwiFile, File mwdFile) throws Exception {
        Parent root = FXMLLoader.load(Utils.getResource("javafx/main.fxml"));
        primaryStage.setTitle("FrogLord");
        primaryStage.setScene(new Scene(root));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(400);
        primaryStage.show();

        MWIFile mwi = new MWIFile();
        mwi.load(new DataReader(new FileSource(mwiFile)));

        MWDFile mwd = new MWDFile(mwi);
        mwd.load(new DataReader(new FileSource(mwdFile)));

        MainController.MAIN_WINDOW.loadMWD(mwd);
    }
}
