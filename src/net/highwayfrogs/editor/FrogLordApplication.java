package net.highwayfrogs.editor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameConfigController;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.DataSizeUnit;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.logging.ConsoleOutputHandler;
import net.highwayfrogs.editor.utils.logging.LogFormatter;
import net.highwayfrogs.editor.utils.logging.UIConsoleHandler;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;

/**
 * The entry point to FrogLord.
 * TODO: At some point we want to:
 *  -> PropertyList with nested portions.
 *  -> Add a scripting console + a way to run scripts for each game, as well as an actual mod system definition.
 *  -> Use new config system in existing code parts?
 *  -> Improve the file path system by thinking about all the places we use it, and if we want to combine any usages.
 *   -> Also, I don't like FrogLordApplication.getWorkingDirectory(), we should allow paths to specify their default directories.
 *   -> I also don't like how having multiple instances of FrogLord can break the configuration. Is there some way we can handle this better? Perhaps reloading configs which are not for the active game instance when we go to save and the file was unexpectedly changed?
 *
 * TODO: Globus's computer seems to have different text settings than mine, so many of the UI buttons are just too small for the text.
 *  -> What do I need to make the FrogLord UI appear consistent across systems?
 * TODO: I think we should make unit tests at some point. Mesh system, texture tree system, Noodle? Utils? Math classes? DataReader/Writer? Config? Gradle, hrmm.
 *
 * TODO: I'd like to buff the MOF viewer to allow previewing all of the things in this file format.
 *  -> Animations probably should be viewable independently of each other? Not sure. But can they compound? Not sure.
 *  -> Are there any xars with flipbook animations for example? Stuff we've taken for granted should be challenged.
 *  -> Add TRUE interpolation support. Eg: Don't just add MediEvil interpolation, add an option to smooth animations.
 * TODO: Is it finally time to delete the GameObject abstract class?
 */
public class FrogLordApplication extends Application {
    @Getter private static Config mainConfig;
    @Getter private static File mainConfigFile;
    @Getter private static File mainApplicationFolder;
    @Getter private static File workingDirectory = new File("./");
    @Getter private static FrogLordApplication application;
    @Getter private static final List<GameInstance> activeGameInstances = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        launch(FrogLordApplication.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(FrogLordApplication::handleFxThreadError);

        application = this;
        resolveMainFolder(); // Run before setting up the logger.
        setupLogger();
        Runtime.getRuntime().addShutdownHook(new Thread(FrogLordApplication::onShutdown));

        long availableMemory = Runtime.getRuntime().maxMemory();
        long minMemory = DataSizeUnit.GIGABYTE.getIncrement();
        if (availableMemory < minMemory)
            FXUtils.makePopUp("FrogLord needs at least 1GB of RAM to function properly.\n"
                    + "FrogLord has only been given " + DataSizeUnit.formatSize(availableMemory) + " Memory.\n"
                    + "Proceed at your own risk. Things may not work properly.", AlertType.WARNING);

        mainConfigFile = new File(mainApplicationFolder, "main.cfg");
        mainConfig = Config.loadConfigFromTextFile(mainConfigFile, true);
        openLoadGameSettingsMenu();
    }

    @SuppressWarnings("ThrowFromFinallyBlock")
    private static void resolveMainFolder() {
        // Can we write file data to the working directory?
        // If not, we probably also can't write to main.cfg, or other files we tend to store in the FrogLord directory.
        // For example, if FrogLord is installed using the installed (eg: "C:\Program Files\FrogLord\"), Windows will not let us write there.
        // So, we'll default to a folder in the home directory instead.
        mainApplicationFolder = new File(System.getProperty("user.home"), "FrogLord");

        File testFile = new File("_working_directory_test_");
        try {
            if (testFile.exists() && !testFile.delete())
                throw new RuntimeException("Failed to delete pre-existing working directory test file '" + testFile + "'.");

            if (testFile.createNewFile())
                mainApplicationFolder = new File(".");
        } catch (IOException exception) {
            // Ignored.
        } finally {
            if (testFile.exists() && !testFile.delete())
                throw new RuntimeException("Failed to delete working directory test file '" + testFile + "'.");
        }

        FileUtils.makeDirectory(mainApplicationFolder);
    }

    private static void handleFxThreadError(Thread thread, Throwable throwable) {
        String errorMessages = (throwable != null ? "\n" + Utils.getErrorMessagesString(throwable) : "");
        if (Platform.isFxApplicationThread()) {
            Utils.handleError(null, throwable, true, "An unhandled error occurred in the user interface.%s", errorMessages);
        } else {
            // This method shouldn't be possible to call from outside the FX thread.
            Utils.handleError(null, throwable, true, "An unhandled error occurred on %s. (SHOULDN'T OCCUR????)%s", thread, errorMessages);
        }
    }

    /**
     * Called when FrogLord shuts down.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void onShutdown() {
        Logger.getLogger(FrogLordApplication.class.getSimpleName()).info("FrogLord is shutting down...");
        saveMainConfig();
        
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
        FileUtils.makeDirectory(new File(getMainApplicationFolder(), "logs")); // Ensure the logs directory exists.

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
            logFile = new File(getMainApplicationFolder(), "logs" + File.separator + logFileName);
        } while (logFile.exists() && logFile.isFile());

        // Setup logging.
        try {
            FileHandler fileHandler = new FileHandler(getMainApplicationFolder().getAbsolutePath().replace('\\', '/') + "/" + "logs/" + logFileName.replace("%", "%%"), true);
            fileHandler.setFormatter(formatter);
            fileHandler.setFilter(record -> !LogFormatter.isJavaFXMessage(record));
            logger.addHandler(fileHandler);
        } catch (Throwable th) {
            logger.throwing("FrogLordApplication", "setupLogger", new RuntimeException("Failed to setup FileHandler for logger.", th));
        }
    }
}