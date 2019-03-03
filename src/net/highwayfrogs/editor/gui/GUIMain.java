package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.gui.editor.SaveController;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class GUIMain extends Application {
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

        // If this isn't a debug setup, prompt the user to select the files to load.
        File mwdFile = Utils.promptFileOpen("Please select a Frogger MWAD", "Medievil WAD", "MWD");
        if (mwdFile == null) {
            Platform.exit(); // No file given, shutdown.
            return;
        }

        File exeFile = Utils.promptFileOpenExtensions("Please select a Frogger executable", "Frogger Executable", "EXE", "dat", "04", "06", "99");
        if (exeFile == null) {
            Platform.exit(); // No file given, shutdown.
            return;
        }

        resolveEXE(exeFile, () -> openGUI(primaryStage, mwdFile));
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
        }, configDisplayName.entrySet(), Entry::getValue, entry -> null);
    }

    @SneakyThrows
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
        EXE_CONFIG.setup();
        MWDFile mwd = EXE_CONFIG.getMWD();
        mwd.load(new DataReader(new FileSource(mwdFile)));

        // Setup GUI.
        MainController controller = MainController.MAIN_WINDOW;
        controller.loadMWD(mwd);

        scene.setOnKeyPressed(event -> {
            if (!event.isControlDown())
                return;

            if (event.getCode() == KeyCode.S) {
                SaveController.saveFiles(EXE_CONFIG, mwd, mwdFile.getParentFile());
            } else if (event.getCode() == KeyCode.I) {
                controller.importFile();
            } else if (event.getCode() == KeyCode.O) {
                controller.exportFile();
            } else if (event.getCode() == KeyCode.E) {
                controller.getCurrentFile().exportAlternateFormat(controller.getFileEntry());
            } else if (event.getCode() == KeyCode.X) {
                EXE_CONFIG.exportCode(getWorkingDirectory());
            }
        });
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
