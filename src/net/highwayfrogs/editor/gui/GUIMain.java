package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.reader.FileSource;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
        INSTANCE = this;
        MAIN_STAGE = primaryStage;
        SystemOutputReplacement.activateReplacement();
        // checkForNewVersion();

        long availableMemory = Runtime.getRuntime().maxMemory();
        long minMemory = DataSizeUnit.GIGABYTE.getIncrement();
        if (availableMemory < minMemory)
            Utils.makePopUp("FrogLord needs at least 1GB of RAM to function properly.\n"
                    + "FrogLord has only been given " + DataSizeUnit.formatSize(availableMemory) + " Memory.\n"
                    + "Proceed at your own risk. Things may not work properly.", AlertType.WARNING);

        openFroggerFiles();
    }

    /**
     * Gets a map of versions to acceptable exe hashes.
     */
    public static Map<String, String[]> getVersions() {
        Config execRegistry = new Config(Utils.getResourceStream("executables.cfg"));

        Map<String, String[]> versionMap = new HashMap<>();
        for (String configName : execRegistry.keySet())
            versionMap.put(configName, execRegistry.getString(configName).split(","));
        return versionMap;
    }

    private void resolveEXE(File exeFile, Runnable onConfigLoad) throws IOException {
        Map<String, String[]> versions = getVersions();
        byte[] fileBytes = Files.readAllBytes(exeFile.toPath());

        long crcHash = Utils.getCRC32(exeFile);
        Map<String, String> configDisplayName = new HashMap<>();
        for (String configName : versions.keySet()) {
            String[] hashes = versions.get(configName);

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

        primaryStage.setTitle("FrogLord " + Constants.VERSION);
        primaryStage.setScene(scene);
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
        File mwdFile = Utils.promptFileOpen("Please select a Frogger MWAD", "Millenium WAD", "MWD");
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

    /**
     * Checks if there is an
     */
    private static void checkForNewVersion() {
        new Thread(() -> { // Too lazy to setup any kind of thread pooling atm. It'll work
            FrogLordVersion versionInfo = null;
            try {
                URL url = new URL("http://api.highwayfrogs.net/FrogLord/version");
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                httpConn.setRequestMethod("GET");
                httpConn.setDoInput(true);
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setConnectTimeout(60000);
                httpConn.setReadTimeout(60000);
                int responseCode = httpConn.getResponseCode();
                if (responseCode == 200) {
                    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(httpConn.getInputStream()));
                    versionInfo = (FrogLordVersion) ois.readObject();
                    ois.close();
                }
                httpConn.disconnect();
            } catch (Throwable th) {
                // There is no case where we want to handle this (besides debugging).
            }

            if (versionInfo != null && versionInfo.isAfterThisVersion())
                Platform.runLater(versionInfo::displayVersionInfo); // If there's a new version, display it.
        }).start();
    }

    @Getter
    @AllArgsConstructor
    public static class FrogLordVersion implements Serializable {
        private final String versionNumber; // The version displayed to the user.
        private final String updateURL;
        private final String releaseNotes; // Wondering if there's a more sensible way to store this. Works for now.
        private final int versionId; // This is a really simple way to check whether a version is newer than the current one.

        /**
         * Checks if this update is newer than the version of FrogLord currently being urn.
         */
        public boolean isAfterThisVersion() {
            return this.versionId > Constants.UPDATE_VERSION;
        }

        /**
         * Displays a popup with information about this version.
         */
        public void displayVersionInfo() {
            NewVersionController.openMenu(this);
        }

        /**
         * Saves the version data.
         */
        @SuppressWarnings("unused")
        public void saveToFile(File file) throws IOException {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(this);
            oos.close();
        }
    }
}
