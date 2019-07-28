package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class GUIMain extends Application {
    public static GUIMain INSTANCE;
    public static Stage MAIN_STAGE;
    @Getter private static File workingDirectory = new File("./");
    public static FroggerEXEInfo EXE_CONFIG;
    public static final Image NORMAL_ICON = GameFile.loadIcon("icon");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //MisfitModel3DObject.performTest();

        INSTANCE = this;
        MAIN_STAGE = primaryStage;
        SystemOutputReplacement.activateReplacement();

        // Debug = automatically load files for convenience.
        File folder = new File("debug");
        if (folder.exists() && folder.isDirectory()) {
            workingDirectory = folder;
            File exeFile = new File(folder, "VANILLA.EXE");
            File mwdFile = new File(folder, "VANILLA.MWD");

            if (exeFile.exists() && mwdFile.exists()) {
                resolveEXE(exeFile, () -> openGUI(primaryStage, mwdFile));
                return;
            }
        }

        openFroggerFiles();
    }


    private void resolveEXE(File exeFile, Runnable onConfigLoad) throws IOException {
        Config execRegistry = new Config(Utils.getResourceStream("executables.cfg"));
        byte[] fileBytes = Files.readAllBytes(exeFile.toPath());

        long crcHash = Utils.getCRC32(exeFile);
        Map<String, String> configDisplayName = new HashMap<>();
        for (String configName : execRegistry.keySet()) {
            String[] hashes = execRegistry.getString(configName).split(",");

            // Executables modified by FrogLord will have a small marker at the end saying which config to use. This works on both playstation and windows executable formats.
            byte[] configNameBytes = configName.getBytes();
            if (Utils.testSignature(fileBytes, fileBytes.length - configNameBytes.length, configNameBytes)) {
                makeExeConfig(exeFile, configName, true);
                onConfigLoad.run();
                return;
            }

            // Use hashes to detect unmodified executables.
            for (String testHash : hashes) {
                if (Long.parseLong(testHash) == crcHash) {
                    makeExeConfig(exeFile, configName, false);
                    onConfigLoad.run();
                    return;
                }
            }

            Config loadedConfig = new Config(Utils.getResourceStream(getExeConfigPath(configName)));
            configDisplayName.put(configName, loadedConfig.getString(FroggerEXEInfo.FIELD_NAME));
        }

        System.out.println("Executable CRC32: " + crcHash); // There was no configuration found, so display the CRC32, in-case we want to make a configuration.
        SelectionMenu.promptSelection("Select a configuration.", resourcePath -> {
            makeExeConfig(exeFile, resourcePath.getKey(), false);
            onConfigLoad.run();
        }, configDisplayName.entrySet(), Entry::getValue, null);
    }

    private void makeExeConfig(File inputExe, String configName, boolean hasConfigIdentifier) {
        EXE_CONFIG = new FroggerEXEInfo(inputExe, Utils.getResourceStream(getExeConfigPath(configName)), configName, hasConfigIdentifier);
    }

    private static String getExeConfigPath(String configName) {
        return "exes/" + configName + ".cfg";
    }

    @SneakyThrows
    private void openGUI(Stage primaryStage, File mwdFile) {
        // Setup GUI (We display the uninitialized GUI before the MWD loads because it intangibly feels better this way.)
        Parent root = FXMLLoader.load(Utils.getResource("javafx/main.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("FrogLord");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(400);
        primaryStage.getIcons().add(NORMAL_ICON);
        primaryStage.show();

        // Load MWD.
        FroggerEXEInfo loadConfig = EXE_CONFIG;
        loadConfig.setup();
        MWDFile mwd = loadConfig.getMWD();
        mwd.load(new DataReader(new FileSource(mwdFile)));
        MainController.MAIN_WINDOW.loadMWD(mwd); // Setup GUI.
    }

    /**
     * Set the current directory to open FileChoosers in.
     * @param directory The directory to set.
     */
    public static void setWorkingDirectory(File directory) {
        if (directory != null && directory.isDirectory())
            workingDirectory = directory;
    }

    /**
     * Opens the frogger files and sets up the UI.
     */
    public void openFroggerFiles() throws IOException {
        boolean isLoadingAgain = (EXE_CONFIG != null); // Is this loading a second time? Ie is there already a loaded game?

        // If this isn't a debug setup, prompt the user to select the files to load.
        File mwdFile = Utils.promptFileOpen("Please select a Frogger MWAD", "Medievil WAD", "MWD");
        if (mwdFile == null) {
            if (!isLoadingAgain)
                Platform.exit(); // No file given. Shutdown if there is nothing loaded already. Otherwise, keep the last data active.
            return;
        }

        File exeFile = Utils.promptFileOpenExtensions("Please select a Frogger executable", "Frogger Executable", "EXE", "dat", "04", "06", "99");
        if (exeFile == null) {
            if (!isLoadingAgain)
                Platform.exit(); // No file given. Shutdown if there is nothing loaded already. Otherwise, keep the last data active.
            return;
        }

        resolveEXE(exeFile, () -> openGUI(MAIN_STAGE, mwdFile));
    }
}
