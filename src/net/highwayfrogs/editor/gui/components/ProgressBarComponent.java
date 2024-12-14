package net.highwayfrogs.editor.gui.components;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.system.AbstractService;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.function.Consumer;

/**
 * Represents a progress bar.
 * Built to be thread-safe. This can be run if the load operation runs on the FX thread.
 * This runs on the FX application thread, and the task does not.
 * Created by Kneesnap on 4/27/2024.
 */
public class ProgressBarComponent extends GameUIController<GameInstance> {
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    // Progress bar stuff.
    private final Runnable updateHandler = this::handleUpdate;
    private final Object lock = new Object();
    @Getter private int completedProgress;
    @Getter private int totalProgress;
    @Getter private String status;
    private boolean nextUpdateQueued;

    private static final URL PROGRESS_BAR_FXML_TEMPLATE_URL = FileUtils.getResourceURL("fxml/window-progress-bar.fxml");;
    private static final FXMLLoader PROGRESS_BAR_FXML_TEMPLATE_LOADER = new FXMLLoader(PROGRESS_BAR_FXML_TEMPLATE_URL);

    public ProgressBarComponent(GameInstance instance) {
        super(instance);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        this.progressLabel.setText("Please wait...");
        this.progressBar.setProgress(0);
        this.statusLabel.setText("");
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        newScene.getWindow().setOnCloseRequest(Event::consume); // Prevent shutting the window, cancellation is not supported.
    }

    /**
     * Adds to the total progress completed.
     * @param addedProgress the amount of progress to add
     */
    public void addCompletedProgress(int addedProgress) {
        if (addedProgress == 0)
            return;

        synchronized (this.lock) {
            update(this.completedProgress + addedProgress, -1, null);
        }
    }

    /**
     * Sets the total progress required, and resets the completed progress to zero.
     * @param totalProgress the new total progress
     */
    public void setTotalProgress(int totalProgress) {
        update(0, totalProgress, null);
    }

    /**
     * Sets the status message
     * @param newStatus the new status message
     */
    public void setStatusMessage(String newStatus) {
        update(-1, -1, newStatus != null ? newStatus : "");
    }

    /**
     * Perform a full update of all fields which may have changed.
     * @param completedProgress the new completed progress, or -1 to signify no change
     * @param totalProgress the new total progress, or -1 to signify no change.
     * @param statusMessage the new status message, or null to signify no change.
     */
    public void update(int completedProgress, int totalProgress, String statusMessage) {
        synchronized (this.lock) {
            boolean shouldUpdate = false;
            if (completedProgress >= 0 && completedProgress != this.completedProgress) {
                this.completedProgress = completedProgress;
                shouldUpdate = true;
            }

            if (totalProgress >= 0 && this.totalProgress != totalProgress) {
                this.totalProgress = totalProgress;
                shouldUpdate = true;
            }

            if (statusMessage != null && !statusMessage.equals(this.status)) {
                this.status = statusMessage;
                shouldUpdate = true;
            }

            if (shouldUpdate && !this.nextUpdateQueued) {
                this.nextUpdateQueued = true;
                Platform.runLater(this.updateHandler);
            }
        }
    }

    private void handleUpdate() {
        synchronized (this.lock) {
            this.nextUpdateQueued = false;
            double newProgress = this.totalProgress > 0 && this.completedProgress >= 0 ? (double) this.completedProgress / this.totalProgress : 0;
            int progressPercent = (int) (newProgress * 100);
            this.progressLabel.setText("Progress: " + progressPercent + "% (" + this.completedProgress + "/" + this.totalProgress + ")");
            if (this.status != null)
                this.statusLabel.setText(this.status);

            this.progressBar.setProgress(newProgress);
        }
    }

    /**
     * Start saving the MWD File.
     * @param gameInstance the game instance the progress bar operation is executing as
     * @param windowTitle the name of the displayed window
     * @param task the task to run which controls the progress bar
     */
    public static void openProgressBarWindow(GameInstance gameInstance, String windowTitle, Consumer<ProgressBarComponent> task) {
        ProgressBarComponent controller = GameUIController.loadController(gameInstance, PROGRESS_BAR_FXML_TEMPLATE_LOADER, new ProgressBarComponent(gameInstance));
        if (controller == null)
            return;

        AbstractService.runAbstractTask(() -> new ProgressBarTask(controller, task));
        GameUIController.openWindow(controller, windowTitle, true);
    }

    @RequiredArgsConstructor
    private static final class ProgressBarTask extends Task<Void> {
        private final ProgressBarComponent progressBarComponent;
        private final Consumer<ProgressBarComponent> providedTask;

        @Override
        protected Void call() {
            try {
                this.providedTask.accept(this.progressBarComponent);
            } catch (Throwable th) {
                Platform.runLater(() -> {
                    Utils.handleError(this.progressBarComponent.getLogger(), th, true, "Failed to run progress bar task.");
                    this.progressBarComponent.closeWindow();
                });
                return null;
            }

            // Success!
            Platform.runLater(this.progressBarComponent::closeWindow);
            return null;
        }
    }
}
