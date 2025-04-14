package net.highwayfrogs.editor.gui.fxobject;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.gui.InputManager.MouseTracker;

import java.util.function.Predicate;

/**
 * Allows listening to a node for mouse dragging.
 * Created by Kneesnap on 4/10/2025.
 */
public class FXDragListener {
    @Getter @NonNull private final MouseTracker mouseTracker;
    @Getter private final Node node;
    @Getter private boolean selectionActive;
    @Getter private boolean listenersActive;
    @Setter private Predicate<MouseEvent> dragStartTest;
    @Setter private IMouseDragRectangleListener onDragListener; // Listens for all drag events.
    @Setter private IMouseDragRectangleListener onDragStartListener;
    @Setter private IMouseDragRectangleListener onDragUpdateListener;
    @Setter private IMouseDragRectangleListener onDragCompleteListener;
    private EventHandler<? super MouseEvent> oldMousePressedListener;
    private EventHandler<? super MouseEvent> oldMouseDraggedListener;
    private EventHandler<? super MouseEvent> oldMouseReleasedListener;

    public FXDragListener(Node node) {
        this(new MouseTracker(), node);
    }

    public FXDragListener(MouseTracker tracker, Node node) {
        this.mouseTracker = tracker;
        this.node = node;
    }

    /**
     * Applies the drag listeners to the node.
     */
    public void applyListenersToNode() {
        if (this.listenersActive)
            return; // Already applied.

        this.listenersActive = true;
        this.oldMousePressedListener = this.node.getOnMousePressed();
        this.oldMouseDraggedListener = this.node.getOnMouseDragged();
        this.oldMouseReleasedListener = this.node.getOnMouseReleased();
        this.node.setOnMousePressed(this::onMousePressed);
        this.node.setOnMouseDragged(this::onMouseDragged);
        this.node.setOnMouseReleased(this::onMouseReleased);
    }

    /**
     * Removes the drag listeners from the node.
     */
    public void removeListenersFromNode() {
        if (!this.listenersActive)
            return; // Not applied.

        this.listenersActive = false;
        this.node.setOnMousePressed(this.oldMousePressedListener);
        this.node.setOnMouseDragged(this.oldMouseDraggedListener);
        this.node.setOnMouseReleased(this.oldMouseReleasedListener);
        this.oldMousePressedListener = null;
        this.oldMouseDraggedListener = null;
        this.oldMouseReleasedListener = null;
    }

    /**
     * Returns whether an event should cause drag handling to become active.
     * @param event the event to test
     * @return should the event be treated as the start of a mouse drag
     */
    protected boolean shouldStartDrag(MouseEvent event) {
        return this.dragStartTest == null || (event != null && this.dragStartTest.test(event));
    }

    /**
     * Handles the mouse press event.
     * @param event the event to handle
     * @return true iff the mouse press was handled as a drag
     */
    protected boolean onMousePressed(MouseEvent event) {
        if (!shouldStartDrag(event)) {
            if (this.oldMousePressedListener != null)
                this.oldMousePressedListener.handle(event);

            return false;
        }

        if (this.selectionActive)
            throw new RuntimeException("Started a new mouse drag while one was already active?");

        // Start dragging.
        event.consume();
        this.selectionActive = true;
        this.mouseTracker.handle(event);

        // Alert listeners.
        if (this.onDragStartListener != null)
            this.onDragStartListener.handle(this.mouseTracker);
        if (this.onDragListener != null)
            this.onDragListener.handle(this.mouseTracker);

        return true;
    }

    /**
     * Handles the mouse drag event.
     * @param event the event to handle
     * @return true iff the mouse drag was handled as a drag
     */
    protected boolean onMouseDragged(MouseEvent event) {
        if (!this.selectionActive) {
            if (this.oldMouseDraggedListener != null)
                this.oldMouseDraggedListener.handle(event);

            return false;
        }

        event.consume();
        this.mouseTracker.handle(event);

        // Alert listeners.
        if (this.onDragUpdateListener != null)
            this.onDragUpdateListener.handle(this.mouseTracker);
        if (this.onDragListener != null)
            this.onDragListener.handle(this.mouseTracker);

        return true;
    }

    /**
     * Handles the mouse drag release event.
     * @param event the event to handle
     * @return true iff the mouse release was handled as a mouse drag release
     */
    protected boolean onMouseReleased(MouseEvent event) {
        if (!this.selectionActive) {
            if (this.oldMouseReleasedListener != null)
                this.oldMouseReleasedListener.handle(event);

            return false;
        }

        // Stop selecting.
        event.consume();
        this.selectionActive = false;
        this.mouseTracker.handle(event);

        // Alert listeners.
        try {
            if (this.onDragCompleteListener != null)
                this.onDragCompleteListener.handle(this.mouseTracker);
            if (this.onDragListener != null)
                this.onDragListener.handle(this.mouseTracker);
        } finally {
            // Mark drag as inactive after the listener is called.
            this.mouseTracker.markDragInactiveIfComplete();
        }

        return true;
    }

    /**
     * Represents a listener for the rectangle mouse drag.
     */
    public interface IMouseDragRectangleListener {
        void handle(MouseTracker mouseTracker);
    }
}
