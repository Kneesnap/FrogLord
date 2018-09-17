package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.highwayfrogs.editor.Utils;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.MWIFile;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;

import java.io.File;

public class GUIMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SystemOutputReplacement.activateReplacement();

        File folder = new File("debug");
        if (folder.exists() && folder.isDirectory()) {
            openGUI(primaryStage, new File(folder, "VANILLA.MWI"), new File(folder, "VANILLA.MWD"));
            return;
        }

        //TODO: This will need to be a file choose dialogue later.
        System.out.println("TODO: Need to bring up a file chooser to select MWD and MWI.");
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
