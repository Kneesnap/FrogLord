package net.highwayfrogs.editor.utils;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contains static utilities used to interact with JavaFX.
 * Created by Kneesnap on 10/25/2024.
 */
public class FXUtils {
    private static final Map<BufferedImage, TextureCache> imageCacheMap = new HashMap<>();
    private static final long IMAGE_CACHE_EXPIRE = TimeUnit.MINUTES.toMillis(5);
    private static final Map<String, FXMLLoader> CACHED_RESOURCE_PATH_FXML_LOADERS = new HashMap<>();

    /**
     * Make a combo box scroll to the value it has selected.
     * @param comboBox The box to scroll.
     */
    @SuppressWarnings("unchecked")
    public static <T> void comboBoxScrollToValue(ComboBox<T> comboBox) {
        if (comboBox.getSkin() != null)
            ((ComboBoxListViewSkin<T>) comboBox.getSkin()).getListView().scrollTo(comboBox.getValue());
    }

    private static class TextureCache {
        private long lastUpdate;
        private final Image fxImage;

        public TextureCache(Image fxImage) {
            this.fxImage = fxImage;
        }

        /**
         * Gets the image.
         */
        public Image getImage() {
            this.lastUpdate = System.currentTimeMillis();
            return fxImage;
        }

        /**
         * Has this image expired?
         * @return hasExpired
         */
        public boolean hasExpired() {
            return (System.currentTimeMillis() - lastUpdate) > IMAGE_CACHE_EXPIRE;
        }
    }

    /**
     * Prompt the user to select a file.
     * TODO: Replace with FileUtils.askUserToOpenFile()
     * @param title      The title of the window to display.
     * @param typeInfo   The label to show for the file-type.
     * @param extensions Allowed extensions.
     * @return selectedFile, Can be null.
     */
    @Deprecated
    public static File promptFileOpenExtensions(GameInstance instance, String title, String typeInfo, String... extensions) {
        Utils.verify(extensions.length > 0, "");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        List<String> allExtensions = new ArrayList<>();
        for (String ext : extensions) {
            String type = ext.contains(".") ? ext : "*." + ext; // Unix is case-sensitive, so we add both lower-case and upper-case.
            String lowerCase = type.toLowerCase();
            String upperCase = type.toUpperCase();

            if (lowerCase.equals(upperCase)) {
                allExtensions.add(type);
            } else {
                allExtensions.add(lowerCase);
                allExtensions.add(upperCase);
            }
        }
        fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, allExtensions));

        fileChooser.setInitialDirectory(FileUtils.getValidFolder(FrogLordApplication.getWorkingDirectory()));

        File selectedFile = fileChooser.showOpenDialog(instance != null ? instance.getMainStage() : null);
        if (selectedFile != null)
            FrogLordApplication.setWorkingDirectory(selectedFile.getParentFile());

        return selectedFile;
    }

    /**
     * Prompt the user to select a file.
     * TODO: Replace with FileUtils.askUserToOpenFile()
     * @param title     The title of the window to display.
     * @param typeInfo  The label to show for the file-type.
     * @param extension Allowed extension.
     * @return selectedFile, Can be null.
     */
    @Deprecated
    public static File promptFileOpen(GameInstance instance, String title, String typeInfo, String extension) {
        return promptFileOpenExtensions(instance, title, typeInfo, extension);
    }

    /**
     * Prompt the user to save a file.
     * TODO: Replace with FileUtils.askUserToSaveFile()
     * @param title       The title of the window to display.
     * @param suggestName The initial name to suggest saving the file as.
     * @param typeInfo    The label to show for the file-type.
     * @param extension   Allowed extension.
     * @return selectedFile, Can be null.
     */
    @Deprecated
    public static File promptFileSave(GameInstance instance, String title, String suggestName, String typeInfo, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (extension != null) {
            String type = "*." + extension; // Unix is case-sensitive, so we add both lower-case and upper-case.

            String lowerCase = type.toLowerCase();
            String upperCase = type.toUpperCase();

            if (lowerCase.equals(upperCase)) {
                fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, type));
            } else {
                fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, lowerCase, upperCase));
            }
        }

        fileChooser.setInitialDirectory(FileUtils.getValidFolder(FrogLordApplication.getWorkingDirectory()));
        if (suggestName != null) {
            String initialName = suggestName;
            if (extension != null && !extension.equals("*") && !initialName.endsWith("." + extension))
                initialName += "." + extension;

            fileChooser.setInitialFileName(initialName);
        }

        File selectedFile = fileChooser.showSaveDialog(instance != null ? instance.getMainStage() : null);
        if (selectedFile != null)
            FrogLordApplication.setWorkingDirectory(selectedFile.getParentFile());

        return selectedFile;
    }

    /**
     * Prompt the user to select a directory.
     * TODO: Replace with FileUtils.askUserToSelectFolder()
     * @param title         The title of the window.
     * @param saveDirectory Should this directory be saved as the current directory?
     * @return directoryFile
     */
    @Deprecated
    public static File promptChooseDirectory(GameInstance instance, String title, boolean saveDirectory) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(FileUtils.getValidFolder(FrogLordApplication.getWorkingDirectory()));

        File selectedFolder = chooser.showDialog(instance != null ? instance.getMainStage() : null);
        if (selectedFolder != null && saveDirectory)
            FrogLordApplication.setWorkingDirectory(selectedFolder);

        return selectedFolder;
    }

    /**
     * Load a FXML template as a new window.
     * @param template The name of the template to load. Should not be user-controllable, as there is no path sanitization.
     * @param controller the window controller
     * @param title the title of the window to show
     * @param waitUntilClose if true, the thread will be blocked until the window is closed
     */
    public static boolean createWindowFromFXMLTemplate(String template, GameUIController<?> controller, String title, boolean waitUntilClose) {
        if (controller == null)
            throw new NullPointerException("controller");

        GameInstance instance = controller.getGameInstance();
        FXMLLoader fxmlTemplateLoader = getFXMLTemplateLoader(instance, template);
        if (fxmlTemplateLoader == null) {
            makePopUp("The UI template '" + template + "' was not found.", AlertType.ERROR);
            return false;
        }

        // Load fxml data.
        if (GameUIController.loadController(instance, fxmlTemplateLoader, controller) == null)
            return false;

        // Open a window.
        GameUIController.openWindow(controller, title, waitUntilClose);
        return true;
    }

    /**
     * Gets the fxml template URL by its name.
     * @param template The template name.
     * @return fxmlTemplateUrl
     */
    public static FXMLLoader getFXMLTemplateLoader(GameInstance gameInstance, String template) {
         FXMLLoader fxmlLoader = gameInstance != null ? gameInstance.getFXMLTemplateLoader(template) : null;
         if (fxmlLoader != null)
             return fxmlLoader;

         fxmlLoader = CACHED_RESOURCE_PATH_FXML_LOADERS.get(template);
         if (fxmlLoader == null) {
             String localPath = "fxml/" + template + ".fxml";
             URL url = FileUtils.getResourceURL(localPath);
             if (url == null)
                 throw new RuntimeException("Could not find resource '" + localPath + "' for " + Utils.getSimpleName(gameInstance) + ".");
             CACHED_RESOURCE_PATH_FXML_LOADERS.put(template, fxmlLoader = new FXMLLoader(url));
         }

         return fxmlLoader;
    }

    /**
     * Make a given stage close when the escape key is pressed.
     * @param stage   The stage to apply.
     * @param onClose Behavior to run when the escape key is pressed.
     */
    public static void closeOnEscapeKey(Stage stage, Runnable onClose) {
        closeOnEscapeKey(stage, onClose, true);
    }

    private static void closeOnEscapeKey(Stage stage, Runnable onClose, boolean firstTime) {
        Scene scene = stage.getScene();
        if (scene == null || !Platform.isFxApplicationThread()) {
            if (firstTime)
                Platform.runLater(() -> closeOnEscapeKey(stage, onClose, false));
            return;
        }

        Utils.verify(scene.getOnKeyPressed() == null, "Scene already has a key-press listener!");
        scene.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ESCAPE) {
                if (onClose != null)
                    onClose.run();
                stage.close();
            }
        });
    }

    /**
     * Switch the Stage's scene without changing window size.
     * @param stage    The stage that should own the scene.
     * @param newScene The new scene to use.
     * @return oldScene
     */
    public static Scene setSceneKeepPosition(Stage stage, Scene newScene) {
        Scene oldScene = stage.getScene();

        Window oldWindow = oldScene.getWindow();
        double width = oldWindow.getWidth();
        double height = oldWindow.getHeight();
        double x = oldWindow.getX();
        double y = oldWindow.getY();

        stage.setScene(newScene);

        // This function worked without the following on my machines, but other machines (whether it be system settings, differing FX versions, etc) would not resize the scene properly.
        // The following appears to fix it.
        stage.hide();
        stage.show();

        // Maintain the position the viewer Scene was at when it was closed.
        Window newWindow = newScene.getWindow();
        newWindow.setX(x);
        newWindow.setY(y);
        newWindow.setWidth(width);
        newWindow.setHeight(height);

        return oldScene;
    }

    /**
     * Turn a BufferedImage into an FX Image.
     * @param image The image to convert.
     * @return convertedImage
     */
    public static Image toFXImage(BufferedImage image, boolean useCache) {
        if (!useCache)
            return SwingFXUtils.toFXImage(image, null);

        synchronized (imageCacheMap) {
            return imageCacheMap.computeIfAbsent(image, bufferedImage -> new TextureCache(SwingFXUtils.toFXImage(bufferedImage, null))).getImage();
        }
    }

    /**
     * Set TextField key-press handling.
     * @param field  The TextField to apply to.
     * @param setter Handles text.
     * @param onPass Called if not null and the setter passed.
     */
    public static void setHandleKeyPress(TextField field, Function<String, Boolean> setter, Runnable onPass) {
        AtomicReference<String> resetTextRef = new AtomicReference<>(field.getText());
        field.setStyle(null);
        field.setOnKeyPressed(evt -> {
            KeyCode code = evt.getCode();
            if (field.getStyle().isEmpty() && (code.isLetterKey() || code.isDigitKey() || code == KeyCode.BACK_SPACE)) {
                field.setStyle("-fx-text-inner-color: darkgreen;");
            } else if (code == KeyCode.ESCAPE) {
                if (field.getParent() != null)
                    field.getParent().requestFocus();
                evt.consume(); // Don't pass further, eg: we don't want to exit the UI we're in.
                field.setText(resetTextRef.get());
                field.setStyle(null);
            } else if (code == KeyCode.ENTER) {
                boolean successfullyHandled = false;
                String newText = field.getText();

                try {
                    successfullyHandled = setter == null || setter.apply(newText);
                } catch (Throwable th) {
                    makeErrorPopUp("An error occurred applying the text '" + newText + "'.", th, true);
                }

                // Run completion hook. If it doesn't pass, return false. If it errors. warn and set it red.
                if (successfullyHandled) {
                    try {
                        if (onPass != null)
                            onPass.run();
                        field.setStyle(null); // Disable any red / green styling.
                        resetTextRef.set(newText);
                        if (field.getParent() != null) // Remove field focus after success.
                            field.getParent().requestFocus();
                        return;
                    } catch (Throwable th) {
                        makeErrorPopUp("An error occurred handling the text '" + newText + "'.", th, true);
                    }
                }

                field.setStyle(Constants.FX_STYLE_INVALID_TEXT);
            }
        });
    }

    /**
     * Set TextField key-press handling.
     * @param field  The TextField to apply to.
     * @param tester Handles text.
     * @param onPass Called if not null and the setter passed.
     */
    public static void setHandleTestKeyPress(TextField field, Function<String, Boolean> tester, Consumer<String> onPass) {
        setHandleKeyPress(field, value -> {
            if (!tester.apply(value))
                return false;

            onPass.accept(value);
            return true;
        }, null);
    }

    /**
     * Make a popup show up from an exception.
     * @param message The message to display.
     * @param ex      The exception which caused the error.
     */
    public static void makeErrorPopUp(String message, Throwable ex, boolean printException) {
        // Get the exception as a string.
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        if (message != null && message.length() > 0) {
            printWriter.append(message);
            printWriter.append(System.lineSeparator());
        }

        if (ex != null)
            ex.printStackTrace(printWriter);

        if (printException) {
            Utils.handleError(null, ex, true, stringWriter.toString());
        } else {
            String errorMessage = stringWriter.toString();

            Alert alert = new Alert(AlertType.ERROR, errorMessage, ButtonType.OK);
            if (ex != null) {
                alert.setResizable(true);
                alert.setWidth(1000);
                alert.setHeight(750);
            }

            alert.showAndWait();
        }
    }

    /**
     * Make a popup show up.
     * @param message The message to display.
     */
    public static void makePopUp(String message, AlertType type) {
        new Alert(type, message, ButtonType.OK).showAndWait();
    }

    /**
     * Make a yes or no popup prompt show up.
     * @param message The message to display.
     */
    public static boolean makePopUpYesNo(String message) {
        return new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO).showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Report an error if the action fails.
     * @param action the action to run
     */
    public static void reportErrorIfFails(Runnable action) {
        try {
            action.run();
        } catch (Throwable th) {
            Utils.handleError(null, th, true);
        }
    }

    /**
     * Runs the provided task on the FX Application Thread.
     * @param task the task to run
     */
    public static void runOnFXThread(Runnable task) {
        if (task == null)
            throw new NullPointerException("task");

        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /**
     * Sets the text on the user's clipboard.
     * @param clipboardText The text to apply
     */
    public static void setClipboardText(String clipboardText) {
        ClipboardContent content = new ClipboardContent();
        content.putString(clipboardText);
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Calculate the volume of the given 3D Node.
     * @param node the node to calculate the area from.
     * @return volume
     */
    public static double calculateVolume(Node node) {
        if (node instanceof Box) {
            Box box = (Box) node;
            return box.getWidth() * box.getHeight() * box.getDepth();
        } else if (node instanceof Sphere) {
            Sphere sphere = (Sphere) node;
            return 4 / 3D * Math.PI * (sphere.getRadius() * sphere.getRadius() * sphere.getRadius());
        } else if (node instanceof Cylinder) {
            Cylinder cylinder = (Cylinder) node;
            return Math.PI * (cylinder.getRadius() * cylinder.getRadius()) * cylinder.getHeight();
        } else if (node instanceof Shape3D) {
            throw new RuntimeException("Unsupported JavaFX Shape3D: " + Utils.getSimpleName(node));
        } else {
            throw new RuntimeException("Unsupported JavaFX Node: " + Utils.getSimpleName(node));
        }
    }

    private static void setupCacheTimerTask() {
        Utils.getAsyncTaskTimer().scheduleAtFixedRate(Utils.createTimerTask(() -> {
            synchronized (imageCacheMap) {
                imageCacheMap.entrySet().removeIf(entry -> entry.getValue().hasExpired());
            }
        }), 0, TimeUnit.MINUTES.toMillis(1));
    }

    static {
        setupCacheTimerTask();
    }
}
