package net.highwayfrogs.editor.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.utils.Utils;

import java.io.IOException;
import java.net.URL;

/**
 * Represents an entity capable of managing a user interface.
 * This user interface could be as large as a standalone window, or as small as a single UI node.
 * Created by Kneesnap on 4/11/2024.
 */
@Getter
public abstract class GameUIController<TGameInstance extends GameInstance> extends GameObject<TGameInstance> {
    private Node rootNode;
    private boolean loadingComplete;

    public GameUIController(TGameInstance instance) {
        super(instance);
    }

    /**
     * After FXML template loading has occurred,
     * @param rootNode the root node for this controller
     */
    public void loadController(Node rootNode) {
        if (this.loadingComplete)
            throw new IllegalStateException("The controller '" + Utils.getSimpleName(this) + "' has already been loaded.");

        this.rootNode = rootNode;

        try {
            onControllerLoad(rootNode);
            this.loadingComplete = true;
        } catch (Throwable th) {
            handleError(th, true, "Failed to setup the UI.");
        }
    }

    /**
     * Handle an exception which should be reported to the user.
     * @param th the exception to log
     * @param showWindow if true, a popup window will display the error
     * @param message the message to accompany the exception
     * @param arguments format string arguments to the message
     */
    public void handleError(Throwable th, boolean showWindow, String message, Object... arguments) {
        Utils.handleError(getLogger(), th, showWindow, 2, message, arguments);
    }

    /**
     * Gets the scene which the root node is attached to.
     */
    public Scene getScene() {
        if (!this.loadingComplete)
            throw new IllegalStateException("Cannot access scene for '" + Utils.getSimpleName(this) + "' before loading is complete.");

        return this.rootNode != null ? this.rootNode.getScene() : null;
    }

    /**
     * Gets the stage which the root node's scene is displayed for, if it exists.
     */
    public Stage getStage() {
        if (!this.loadingComplete)
            throw new IllegalStateException("Cannot access stage before loading is complete.");

        Scene nodeScene = getScene();
        if (nodeScene == null)
            return null;

        Window sceneWindow = nodeScene.getWindow();
        if (sceneWindow == null)
            return null;

        if (!(sceneWindow instanceof Stage))
            throw new IllegalStateException("Unexpected situation! The scene's Window is a(n) " + Utils.getSimpleName(sceneWindow) + ", and not a Stage.");

        return (Stage) sceneWindow;
    }

    /**
     * Closes the window.
     */
    public void closeWindow() {
        Stage stage = getStage();
        if (stage == null) {
            getLogger().warning("Could not close window, since the Stage was null!");
            return;
        }

        stage.close();
    }

    /**
     * Called when the controller is loaded from the fxml template
     * @param rootNode the node loaded from the fxml template
     */
    protected abstract void onControllerLoad(Node rootNode);

    /**
     * Called when the controller is added to a scene.
     */
    public void onSceneAdd(Scene newScene) {
        // Don't need to do anything.
    }

    /**
     * Called when the node is removed from a scene.
     */
    public void onSceneRemove(Scene oldScene) {
        // Don't need to do anything.
    }

    /**
     * Loads a UI controller from the provided fxml template
     * @param instance the game instance to create the ui template from
     * @param fxmlResourceLocation the fxml template location
     * @param controller the controller to use
     * @return controller
     * @param <TGameInstance> the type of game instance
     * @param <TUIController> the type of ui controller to create
     */
    public static <TGameInstance extends GameInstance, TUIController extends GameUIController<?>> TUIController loadController(TGameInstance instance, URL fxmlResourceLocation, TUIController controller) {
        if (fxmlResourceLocation == null)
            throw new NullPointerException("fxmlResourceLocation");
        if (controller == null)
            throw new NullPointerException("controller");

        FXMLLoader loader = new FXMLLoader(fxmlResourceLocation);

        Node rootNode;
        try {
            loader.setController(controller);
            rootNode = loader.load();
        } catch (IOException ex) {
            Utils.handleError(instance.getLogger(), ex, true, "Failed to load FXML template '%s'.", fxmlResourceLocation);
            return null;
        }

        controller.loadController(rootNode);
        return controller;
    }

    /**
     * Opens a new UI window with the given UI controller.
     * @param controller the controller to apply to the window
     * @param title the title of the window
     * @param waitUntilClose if true, the thread will become blocked until the window is closed.
     * @return the stage the window was displayed to
     */
    public static Stage openWindow(GameUIController<?> controller, String title, boolean waitUntilClose) {
        if (controller == null)
            throw new NullPointerException("controller");

        Node controllerRootNode = controller.getRootNode();
        if (controllerRootNode == null)
            throw new IllegalArgumentException("The provided UI controller has no root node, and thus has no content to apply to the window.");
        if (!(controllerRootNode instanceof Parent))
            throw new IllegalArgumentException("The provided UI controller's root node is not Parent, it is " + Utils.getSimpleName(controllerRootNode) + ", which cannot be applied to a Scene.");

        // Create stage.
        Stage newStage = new Stage();
        if (title != null)
            newStage.setTitle(title);
        newStage.getIcons().add(GUIMain.MAIN_ICON);

        // Create scene.
        Scene newScene = new Scene((Parent) controller.getRootNode());
        newStage.setScene(newScene);
        newStage.setResizable(false);

        try {
            controller.onSceneAdd(newScene);
        } catch (Throwable th) {
            Utils.handleError(controller.getLogger(), th, true, "Failed to setup the UI controller %s for the scene.", Utils.getSimpleName(controller));
        }

        // Make the main menu the parent.
        GameInstance gameInstance = controller.getGameInstance();
        GameUIController<?> mainMenuController = gameInstance != null ? gameInstance.getMainMenuController() : null;
        if (mainMenuController != null && mainMenuController != controller) {
            newStage.initModality(Modality.WINDOW_MODAL);
            newStage.initOwner(mainMenuController.getStage());
        }

        // Show the window and wait.
        if (waitUntilClose) {
            newStage.showAndWait();

            // After the window is shut, run the shutdown hook.
            try {
                controller.onSceneRemove(newScene);
            } catch (Throwable th) {
                Utils.handleError(controller.getLogger(), th, true, "Failed to cleanup the UI controller %s for the scene.", Utils.getSimpleName(controller));
            }
        } else {
            newStage.show();

            // When the window is shut, run the removal hook.
            newStage.setOnCloseRequest(event -> {
                try {
                    controller.onSceneRemove(newScene);
                } catch (Throwable th) {
                    Utils.handleError(controller.getLogger(), th, true, "Failed to cleanup the UI controller %s for the scene.", Utils.getSimpleName(controller));
                }
            });
        }

        return newStage;
    }

    /**
     * Set the node to stretch in the anchor pane.
     * @param node the node to set
     */
    public static void setAnchorPaneStretch(Node node) {
        AnchorPane.setTopAnchor(node, 0D);
        AnchorPane.setBottomAnchor(node, 0D);
        AnchorPane.setLeftAnchor(node, 0D);
        AnchorPane.setRightAnchor(node, 0D);
    }
}