package net.highwayfrogs.editor.gui.editor;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import net.highwayfrogs.editor.gui.InputManager.KeyHandler;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Allows implementing a way to select a value in a MeshViewController.
 * Created by Kneesnap on 6/2/2024.
 */
public class SelectionPromptTracker<TPromptTarget> {
    @Getter private final MeshViewController<?> controller;
    @Getter private final MeshUIManager<?> uiManager;
    @Getter private final boolean cancelOnEscape;
    @Getter private boolean promptActive;
    private final KeyHandler keyHandler = (inputManager, keyEvent) -> this.handleKeyPress(keyEvent);
    private Consumer<TPromptTarget> selectionHook;
    private Runnable cancelHook;

    public SelectionPromptTracker(MeshUIManager<?> uiManager, boolean cancelOnEscape) {
        this.controller = uiManager.getController();
        this.uiManager = uiManager;
        this.cancelOnEscape = cancelOnEscape;
    }

    /**
     * Gets the logger used for logging selection prompt related information.
     */
    public Logger getLogger() {
        return this.uiManager != null ? this.uiManager.getLogger() : this.controller.getLogger();
    }

    /**
     * Activate a user-input prompt.
     * @param onSelect the callback hook to accept a selection
     * @param onCancel the cancellation behavior
     */
    public void activate(Consumer<TPromptTarget> onSelect, Runnable onCancel) {
        if (this.promptActive)
            throw new IllegalStateException("The selection prompt is already active.");

        this.promptActive = true;
        this.selectionHook = onSelect;
        this.cancelHook = onCancel;
        if (this.controller != null)
            this.controller.getInputManager().addKeyListener(this.keyHandler);
    }

    /**
     * Cancel the user prompt.
     */
    public void cancel() {
        if (!this.promptActive)
            throw new IllegalStateException("The prompt was not active, so it cannot be cancelled.");

        // Run the cancellation hook.
        if (this.cancelHook != null) {
            try {
                this.cancelHook.run();
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Encountered an Exception while running the cancellation hook.");
            }
        }

        try {
            onPromptDisable(null);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "An Exception occurred while running onPromptDisable(null).");
        }
    }

    /**
     * Handles a map click. The user is responsible for ensuring this is called.
     * @param event The MouseEvent.
     * @param clickedTarget The target value clicked on.
     * @return consumeClick (Whether the click was consumed and should not be handled by anything else)
     */
    public boolean handleClick(MouseEvent event, TPromptTarget clickedTarget) {
        if (!this.promptActive)
            return false;

        boolean acceptValue;

        try {
            acceptValue = onClick(event, clickedTarget);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, true, "Failed to handle the click of %s: %s.", Utils.getSimpleName(clickedTarget), clickedTarget);
            if (event != null)
                event.consume();

            cancel();
            return true;
        }

        // If the value was accepted...
        if (acceptValue && selectValue(clickedTarget) && event != null)
            event.consume();

        return acceptValue;
    }

    /**
     * Any call to this indicates a value has been selected.
     * @param target the value which has been selected.
     * @return if the selection has been accepted and the prompt has alerted the selection hook, shut down, etc
     */
    public boolean selectValue(TPromptTarget target) {
        if (!this.promptActive)
            return false;

        // Send to hook.
        if (this.selectionHook != null) {
            try {
                this.selectionHook.accept(target);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Error in selection prompt callback for %s: %s.", Utils.getSimpleName(target), target);
            }
        }

        // Call disable hook.
        try {
            onPromptDisable(target);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "An Exception occurred while running onPromptDisable(%s). (%s)", Utils.getSimpleName(target), target);
        }

        return true;
    }

    /**
     * Handle when a key is pressed. This event is automatically registered and deregistered to the input manager.
     * @param event The key event fired.
     * @return consumeEvent (Whether the event was consumed and should not be handled by anything else)
     */
    public boolean handleKeyPress(KeyEvent event) {
        if (this.cancelOnEscape && this.promptActive && event.getCode() == KeyCode.ESCAPE) { // Cancel active prompt.
            event.consume();
            cancel();
            return true;
        }

        return false;
    }

    /**
     * Handles a map click. The user is responsible for ensuring this is called.
     * @param event The MouseEvent.
     * @param clickedTarget The target value clicked on.
     * @return consumeClick (Whether the click should be consumed and not handled by anything else)
     */
    protected boolean onClick(MouseEvent event, TPromptTarget clickedTarget) {
        return clickedTarget != null;
    }

    /**
     * Called when the prompt disables.
     */
    protected void onPromptDisable(TPromptTarget value) {
        this.promptActive = false;
        this.selectionHook = null;
        this.cancelHook = null;
        if (this.controller != null)
            this.controller.getInputManager().removeKeyListener(this.keyHandler);

        if (value != null) {
            try {
                onPromptComplete(value);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "onPromptComplete() threw an Exception while accepting %s: %s", Utils.getSimpleName(value), value);
            }
        } else {
            try {
                onPromptCancel();
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, false, "onPromptCancel() threw an Exception.");
            }
        }
    }

    /**
     * Called when the prompt is shutting down after accepting a value.
     * @param value the selected value
     */
    protected void onPromptComplete(TPromptTarget value) {
        // Do nothing.
    }

    /**
     * Called when the prompt is shutting down due to cancellation.
     */
    protected void onPromptCancel() {
        // Do nothing.
    }
}