package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ConsoleErrorHandler;
import net.highwayfrogs.editor.utils.logging.ConsoleOutputHandler;
import net.highwayfrogs.editor.utils.logging.LogFormatter;
import net.highwayfrogs.editor.utils.logging.UIConsoleHandler;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;

/**
 * The entry point to FrogLord.
 * TODO: Search bar, add bar, etc.
 * TODO: Use new config system in existing code parts?
 *
 * TODO: Solve TODOs in:
 *  - GUIMain.java
 *  - GameInstance.java
 *  - GroupedCollectionViewComponent.java
 *  - CollectionViewComponent.java
 *  - CollectionEditorComponent.java
 *  - SCGameFileListEditor.java
 */
public class GUIMain extends Application {
    @Getter private static Config mainConfig;
    @Getter private static File mainConfigFile;
    @Getter private static File workingDirectory = new File("./");
    @Getter private static GUIMain application;
    @Getter private static final List<GameInstance> activeGameInstances = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        application = this;
        setupLogger();
        SystemOutputReplacement.activateReplacement();

        long availableMemory = Runtime.getRuntime().maxMemory();
        long minMemory = DataSizeUnit.GIGABYTE.getIncrement();
        if (availableMemory < minMemory)
            Utils.makePopUp("FrogLord needs at least 1GB of RAM to function properly.\n"
                    + "FrogLord has only been given " + DataSizeUnit.formatSize(availableMemory) + " Memory.\n"
                    + "Proceed at your own risk. Things may not work properly.", AlertType.WARNING);

        // Start.
        mainConfigFile = new File("main.cfg");
        mainConfig = Config.loadConfigFromTextFile(mainConfigFile, true);
        openLoadGameSettingsMenu();
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
     * Opens the menu for loading a game.
     */
    public static void openLoadGameSettingsMenu() {
        GameConfigController.openGameConfigMenu(getGameLoadSettingsConfig());
    }

    /**
     * Get the config containing data for loading games.
     */
    public static Config getGameLoadSettingsConfig() {
        return mainConfig.getOrCreateChildConfigByName("GameLoadSettings");
    }

    /**
     * Save the main config file.
     */
    public static void saveMainConfig() {
        mainConfig.saveTextFile(mainConfigFile);
    }

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