package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.FroggerVersionComparison;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ConsoleErrorHandler;
import net.highwayfrogs.editor.utils.logging.ConsoleOutputHandler;
import net.highwayfrogs.editor.utils.logging.LogFormatter;
import net.highwayfrogs.editor.utils.logging.UIConsoleHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.logging.Formatter;
import java.util.logging.*;

public class GUIMain extends Application {
    public static GUIMain INSTANCE;
    public static Stage MAIN_STAGE;
    @Getter private static File workingDirectory = new File("./");
    public static final Image NORMAL_ICON = SCGameFile.loadIcon("icon");
    private static boolean loadedSuccessfullyAtLeastOnce;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        INSTANCE = this;
        MAIN_STAGE = primaryStage;
        setupLogger();
        SystemOutputReplacement.activateReplacement();

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

    private void createGameInstance(File exeFile, File mwdFile, Consumer<SCGameInstance> onConfigLoad) throws IOException {
        Map<String, String[]> versions = getVersions();
        byte[] fileBytes = Files.readAllBytes(exeFile.toPath());

        long crcHash = Utils.getCRC32(exeFile);
        Map<String, String> configDisplayName = new HashMap<>();
        for (String configName : versions.keySet()) {
            String[] hashes = versions.get(configName);

            // Executables modified by FrogLord will have a small marker at the end saying which config to use. This works on both playstation and windows executable formats.
            byte[] configNameBytes = configName.getBytes();
            if (Utils.testSignature(fileBytes, fileBytes.length - configNameBytes.length, configNameBytes)) {
                SCGameInstance instance = makeGameInstance(exeFile, mwdFile, configName);
                onConfigLoad.accept(instance);
                return;
            }

            // Use hashes to detect unmodified executables.
            for (String testHash : hashes) {
                if (Long.parseLong(testHash) == crcHash) {
                    SCGameInstance instance = makeGameInstance(exeFile, mwdFile, configName);
                    onConfigLoad.accept(instance);
                    return;
                }
            }

            Config loadedConfig = new Config(Utils.getResourceStream(getExeConfigPath(configName)));
            configDisplayName.put(configName, loadedConfig.getString(SCGameConfig.CFG_DISPLAY_NAME));
        }

        System.out.println("Executable CRC32: " + crcHash); // There was no configuration found, so display the CRC32, in-case we want to make a configuration.
        List<Entry<String, String>> internalToDisplayNames = new ArrayList<>(configDisplayName.entrySet());
        internalToDisplayNames.sort(Entry.comparingByKey());

        SelectionMenu.promptSelection("Select a configuration.", resourcePath -> {
            SCGameInstance instance = makeGameInstance(exeFile, mwdFile, resourcePath.getKey());
            onConfigLoad.accept(instance);
        }, internalToDisplayNames, Entry::getValue, null);
    }

    private SCGameInstance makeGameInstance(File inputExe, File inputMwd, String configName) {
        Config config = new Config(Utils.getResourceStream(getExeConfigPath(configName)));
        SCGameType gameType = config.getEnum(SCGameConfig.CFG_GAME_TYPE, SCGameType.FROGGER);
        SCGameInstance instance = gameType.createInstance();
        instance.loadGame(configName, config, inputMwd, inputExe);
        return instance;
    }

    private static String getExeConfigPath(String configName) {
        return "exes/" + configName + ".cfg";
    }

    @SneakyThrows
    private void openGUI(Stage primaryStage, SCGameInstance instance) {
        // Setup GUI (We display the uninitialized GUI before the MWD loads because it intangibly feels better this way.)
        Parent root = FXMLLoader.load(Utils.getResource("javafx/main.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("FrogLord " + Constants.VERSION);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(NORMAL_ICON);
        primaryStage.show();

        // Setup MWD UI.
        MainController.MAIN_WINDOW.loadMWD(instance); // Setup GUI.
        loadedSuccessfullyAtLeastOnce = true;
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
        // Setup version comparison.
        FroggerVersionComparison.setup(getWorkingDirectory());

        // If this isn't a debug setup, prompt the user to select the files to load.
        File mwdFile = Utils.promptFileOpen("Please select a Millennium WAD", "Millennium WAD", "MWD");
        if (mwdFile == null) {
            if (!loadedSuccessfullyAtLeastOnce)
                Platform.exit(); // No file given. Shutdown if there is nothing loaded already. Otherwise, keep the last data active.
            return;
        }

        File exeFile = Utils.promptFileOpenExtensions("Please select a Millennium executable", "Millennium Executable", "EXE", "dat", "04", "06", "26", "64", "65", "66", "99");
        if (exeFile == null) {
            if (!loadedSuccessfullyAtLeastOnce)
                Platform.exit(); // No file given. Shutdown if there is nothing loaded already. Otherwise, keep the last data active.
            return;
        }

        createGameInstance(exeFile, mwdFile, instance -> {
            openGUI(MAIN_STAGE, instance);
            if (instance.isFrogger())
                FroggerVersionComparison.addNewVersionToConfig((FroggerGameInstance) instance);
        });
    }

    @SuppressWarnings("ConditionalBreakInInfiniteLoop")
    private static void setupLogger() {
        Utils.makeDirectory(new File("logs")); // Ensure the logs directory exists.

        // Setup global logger.
        Logger logger = Logger.getGlobal();

        // Delete all handlers recursively, and reach the root node.
        while (true) {
            Handler[] handlers = logger.getHandlers();
            for (int i = 0; i < handlers.length; i++)
                logger.removeHandler(handlers[i]);

            if (logger.getParent() == null) { // Reached the root.
                break;
            } else {
                logger = logger.getParent();
            }
        }

        // Setup root logger.
        logger.setLevel(Level.ALL);

        // Setup log formatter
        Formatter formatter = new LogFormatter(LogFormatter.FULL_LOG_FORMAT);

        // Setup System.out console handler.
        ConsoleOutputHandler consoleHandler = new ConsoleOutputHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);

        // Setup System.err console handler.
        ConsoleErrorHandler errorHandler = new ConsoleErrorHandler();
        errorHandler.setFormatter(formatter);
        errorHandler.setLevel(Level.ALL);
        logger.addHandler(errorHandler);

        // Setup UI Handler.
        UIConsoleHandler uiConsoleHandler = new UIConsoleHandler();
        uiConsoleHandler.setFormatter(new LogFormatter(LogFormatter.PARTIAL_LOG_FORMAT));
        uiConsoleHandler.setLevel(Level.CONFIG);
        logger.addHandler(uiConsoleHandler);

        // Setup file handler.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormat.format(Calendar.getInstance().getTime());
        try {
            FileHandler fileHandler = new FileHandler("logs/" + dateStr + "-%u.log");
            fileHandler.setFormatter(formatter);
            fileHandler.setFilter(record -> !LogFormatter.isJavaFXMessage(record));
            logger.addHandler(fileHandler);
        } catch (Throwable th) {
            logger.throwing("GUIMain", "setupLogger", new RuntimeException("Failed to setup FileHandler for logger.", th));
        }
    }
}