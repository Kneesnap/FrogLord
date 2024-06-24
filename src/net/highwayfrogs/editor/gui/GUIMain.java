package net.highwayfrogs.editor.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.Utils;
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
        Thread.setDefaultUncaughtExceptionHandler(GUIMain::handleFxThreadError);

        application = this;
        setupLogger();
        Runtime.getRuntime().addShutdownHook(new Thread(GUIMain::onShutdown));

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

    private static void handleFxThreadError(Thread thread, Throwable throwable) {
        if (Platform.isFxApplicationThread()) {
            Utils.handleError(null, throwable, true, "An unhandled error occurred in the user interface.");
        } else {
            // This method shouldn't be possible to call from outside the FX thread.
            Utils.handleError(null, throwable, true, "An unhandled error occurred on %s. (SHOULDN'T OCCUR????)", thread);
        }
    }
    
    @SuppressWarnings("CallToPrintStackTrace")
    private static void onShutdown() {
        Logger.getLogger(GUIMain.class.getSimpleName()).info("FrogLord is shutting down...");
        
        // Logger shutdown.
        Logger globalLogger = Logger.getGlobal();
        for (int i = 0; i < globalLogger.getHandlers().length; i++) {
            try {
                Handler handler = globalLogger.getHandlers()[i];
                handler.flush();
                handler.close();
            } catch (Throwable th) {
                // Can't do a lot except to log it to the console.
                th.printStackTrace();
            }
        }
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

        // Setup System.out/System.err console handler.
        ConsoleOutputHandler consoleHandler = new ConsoleOutputHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);

        // Setup UI Handler.
        UIConsoleHandler uiConsoleHandler = new UIConsoleHandler();
        uiConsoleHandler.setFormatter(new LogFormatter(LogFormatter.PARTIAL_LOG_FORMAT));
        uiConsoleHandler.setLevel(Level.CONFIG);
        logger.addHandler(uiConsoleHandler);

        // Setup file handler.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = dateFormat.format(Calendar.getInstance().getTime());

        // Generate log file name.
        int id = 0;
        String logFileName;
        File logFile;
        do {
            logFileName = dateStr + "-" + (id++) + ".log";
            logFile = new File("logs" + File.separator + logFileName);
        } while (logFile.exists() && logFile.isFile());

        // Setup logging.
        try {
            FileHandler fileHandler = new FileHandler("logs/" + logFileName.replace("%", "%%"), true);
            fileHandler.setFormatter(formatter);
            fileHandler.setFilter(record -> !LogFormatter.isJavaFXMessage(record));
            logger.addHandler(fileHandler);
        } catch (Throwable th) {
            logger.throwing("GUIMain", "setupLogger", new RuntimeException("Failed to setup FileHandler for logger.", th));
        }
    }
}