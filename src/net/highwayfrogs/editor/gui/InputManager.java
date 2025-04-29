package net.highwayfrogs.editor.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.*;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This tracks keyboard and mouse input for usage primarily in 3D environments.
 * Created by Kneesnap on 1/2/2024.
 */
@RequiredArgsConstructor
public class InputManager {
    private final GameInstance gameInstance;
    private final Map<KeyCode, List<KeyHandler>> keySpecificHandlers = new HashMap<>();
    private final ChangeListener<? super Boolean> stageInputListener = this::onStageFocusChange;
    private final EventHandler<? super KeyEvent> mainKeyEventListener = this::processKeyEvents;
    private final EventHandler<? super MouseEvent> mainMouseEventListener = this::processMouseEvents;
    private final EventHandler<? super ScrollEvent> mainScrollEventListener = this::processScrollEvents;
    private final List<KeyHandler> keyHandlers = new ArrayList<>();
    private Stage stage;
    @Setter private KeyHandler finalKeyHandler;
    private final List<MouseHandler> mouseHandlers = new ArrayList<>();
    @Setter private MouseHandler finalMouseHandler;
    private final Map<EventType<? super MouseEvent>, List<MouseHandler>> mouseHandlersByType = new HashMap<>();
    private final List<ScrollHandler> scrollHandlers = new ArrayList<>();
    @Setter private ScrollHandler finalScrollHandler;
    private final boolean[] pressedKeys = new boolean[KeyCode.values().length];
    @Getter private final MouseTracker mouseTracker = new MouseTracker();

    public interface KeyHandler {
        void accept(InputManager manager, KeyEvent event);
    }

    public interface MouseHandler {
        void accept(InputManager manager, MouseEvent event, double deltaX, double deltaY);
    }

    public interface ScrollHandler {
        void accept(InputManager manager, ScrollEvent event);
    }

    /**
     * Assign (setup) the control event handlers on the supplied scene object.
     * @param scene The subscene to receive and process the keyboard and mouse events, etc.
     */
    public void assignSceneControls(Stage stage, Scene scene) {
        scene.addEventHandler(KeyEvent.ANY, this.mainKeyEventListener);
        scene.addEventHandler(MouseEvent.ANY, this.mainMouseEventListener);
        scene.addEventHandler(ScrollEvent.ANY, this.mainScrollEventListener);

        // Reset keys when focus is lost.
        stage.focusedProperty().addListener(this.stageInputListener);
        this.stage = stage;
    }

    /**
     * Assign (setup) the control event handlers on the supplied scene object.
     * @param scene The subscene to receive and process the keyboard and mouse events, etc.
     */
    public void removeSceneControls(Stage stage, Scene scene) {
        scene.removeEventHandler(KeyEvent.ANY, this.mainKeyEventListener);
        scene.removeEventHandler(MouseEvent.ANY, this.mainMouseEventListener);
        scene.removeEventHandler(ScrollEvent.ANY, this.mainScrollEventListener);

        // Reset keys when focus is lost.
        stage.focusedProperty().removeListener(this.stageInputListener);
        resetKeys(); // No longer active, isn't tracking keys.
        this.stage = null;
    }

    /**
     * Shutdown input management
     */
    public void shutdown() {
        if (this.stage != null) {
            this.stage.focusedProperty().removeListener(this.stageInputListener);
            this.stage = null;
        }
    }

    private void onStageFocusChange(ObservableValue<? extends Boolean> observable, boolean wasFocused, boolean nowFocused) {
        if (wasFocused || !nowFocused)
            resetKeys();
    }

    /**
     * Adds a key listener for a specific key.
     * @param keyCode  The key to add the listener for.
     * @param listener The listener to add.
     */
    public void addKeyListener(KeyCode keyCode, KeyHandler listener) {
        this.keySpecificHandlers.computeIfAbsent(keyCode, key -> new ArrayList<>()).add(listener);
    }

    /**
     * Adds a key listener for all keyboard events.
     * @param listener The listener to add.
     */
    public void addKeyListener(KeyHandler listener) {
        this.keyHandlers.add(listener);
    }

    /**
     * Removes a key listener listening for a specific key.
     * @param keyCode The key to remove the listener for.
     * @param listener The listener to add.
     */
    public boolean removeKeyListener(KeyCode keyCode, KeyHandler listener) {
        List<KeyHandler> handlerList = this.keySpecificHandlers.get(keyCode);
        return handlerList != null && handlerList.remove(listener);
    }

    /**
     * Removes a key listener listening for all keyboard events.
     * @param listener The listener to remove.
     */
    public boolean removeKeyListener(KeyHandler listener) {
        return this.keyHandlers.remove(listener);
    }

    /**
     * Adds a mouse listener for a specific mouse event type.
     * @param eventType The event type to track.
     * @param listener The listener to add.
     */
    public void addMouseListener(EventType<? super MouseEvent> eventType, MouseHandler listener) {
        this.mouseHandlersByType.computeIfAbsent(eventType, key -> new ArrayList<>()).add(listener);
    }

    /**
     * Adds a mouse listener for all mouse events.
     * @param listener The listener to add.
     */
    public void addMouseListener(MouseHandler listener) {
        this.mouseHandlers.add(listener);
    }

    /**
     * Removes a mouse listener listening for a specific mouse event type.
     * @param eventType The event type to remove.
     * @param listener The listener to add.
     */
    public boolean removeMouseListener(EventType<? super MouseEvent> eventType, MouseHandler listener) {
        List<MouseHandler> handlerList = this.mouseHandlersByType.get(eventType);
        return handlerList != null && handlerList.remove(listener);
    }

    /**
     * Adds a scroll listener for all scroll events.
     * @param listener The listener to add.
     */
    public void addScrollListener(ScrollHandler listener) {
        this.scrollHandlers.add(listener);
    }

    /**
     * Test if the particular key is pressed.
     * @param keyCode key code to test current state
     * @return true, if the key is currently pressed
     */
    public boolean isKeyPressed(KeyCode keyCode) {
        return keyCode != null && this.pressedKeys[keyCode.ordinal()];
    }

    /**
     * Function to process key input events.
     */
    private void processKeyEvents(KeyEvent evt) {
        KeyCode keyCode = evt.getCode();
        if (keyCode == null)
            return;

        // Send out key handlers for specific keys.
        List<KeyHandler> keyHandlers = this.keySpecificHandlers.get(keyCode);
        if (keyHandlers != null && keyHandlers.size() > 0) {
            for (int i = 0; i < keyHandlers.size(); i++) {
                KeyHandler handler = keyHandlers.get(i);
                try {
                    handler.accept(this, evt);
                } catch (Throwable th) {
                    String errorMessage = "Failed to run KeyHandler " + handler + ".";
                    getLogger().throwing("InputManager", "processKeyEvents", new RuntimeException(errorMessage, th));
                }

                if (evt.isConsumed()) {
                    updateKeyboardStates(evt);
                    return;
                }
            }
        }

        // Send out generic key handlers.
        for (int i = 0; i < this.keyHandlers.size(); i++) {
            KeyHandler handler = this.keyHandlers.get(i);

            try {
                handler.accept(this, evt);
            } catch (Throwable th) {
                String errorMessage = "Failed to run KeyHandler " + handler + ".";
                getLogger().throwing("InputManager", "processKeyEvents", new RuntimeException(errorMessage, th));
            }

            // If the event was consumed, abort.
            if (evt.isConsumed()) {
                updateKeyboardStates(evt);
                return;
            }
        }

        try {
            if (this.finalKeyHandler != null)
                this.finalKeyHandler.accept(this, evt);
        } catch (Throwable th) {
            String errorMessage = "Failed to run final KeyHandler " + this.finalKeyHandler + ".";
            getLogger().throwing("InputManager", "processKeyEvents", new RuntimeException(errorMessage, th));
        }

        // Update keyboard states (final)
        updateKeyboardStates(evt);
    }

    private void updateKeyboardStates(KeyEvent evt) {
        KeyCode keyCode = evt.getCode();
        if (evt.getEventType() == KeyEvent.KEY_PRESSED) {
            this.pressedKeys[keyCode.ordinal()] = true;
            evt.consume();
        } else if (evt.getEventType() == KeyEvent.KEY_RELEASED) {
            this.pressedKeys[keyCode.ordinal()] = false;
            evt.consume();
        }
    }

    /**
     * Reset the state of all keys, to be considered unpressed.
     */
    public void resetKeys() {
        for (int i = 0; i < KeyCode.values().length; i++) {
            if (!this.pressedKeys[i])
                continue; // Skip key which isn't pressed.

            // Create a key event.
            KeyCode keyCode = KeyCode.values()[i];
            KeyEvent newEvent = new KeyEvent(null, null, KeyEvent.KEY_RELEASED, KeyEvent.CHAR_UNDEFINED, null, keyCode, false, false, false, false);

            // Fire the event, regardless of if the event is consumed.
            List<KeyHandler> keyHandlers = this.keySpecificHandlers.get(keyCode);
            if (keyHandlers != null && keyHandlers.size() > 0) {
                for (int j = 0; j < keyHandlers.size(); j++) {
                    KeyHandler handler = keyHandlers.get(j);
                    try {
                        handler.accept(this, newEvent);
                    } catch (Throwable th) {
                        String errorMessage = "Failed to run KeyHandler " + handler + ".";
                        getLogger().throwing("InputManager", "resetKeys", new RuntimeException(errorMessage, th));
                    }
                }
            }

            // Send out generic key handlers.
            for (int j = 0; j < this.keyHandlers.size(); j++) {
                KeyHandler handler = this.keyHandlers.get(j);

                try {
                    handler.accept(this, newEvent);
                } catch (Throwable th) {
                    String errorMessage = "Failed to run KeyHandler " + handler + ".";
                    getLogger().throwing("InputManager", "resetKeys", new RuntimeException(errorMessage, th));
                }
            }

            // Fire the last key handler.
            try {
                if (this.finalKeyHandler != null)
                    this.finalKeyHandler.accept(this, newEvent);
            } catch (Throwable th) {
                String errorMessage = "Failed to run the final KeyHandler " + this.finalKeyHandler + ".";
                getLogger().throwing("InputManager", "resetKeys", new RuntimeException(errorMessage, th));
            }

            // Make sure the key is seen as released.
            this.pressedKeys[i] = false;
        }
    }

    /**
     * Function to process mouse input events.
     */
    private void processMouseEvents(MouseEvent evt) {
        this.mouseTracker.handle(evt);
        double mouseDeltaX = 0;
        double mouseDeltaY = 0;
        if (evt.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
            mouseDeltaX = this.mouseTracker.getDeltaXSinceLastMouseMove();
            mouseDeltaY = this.mouseTracker.getDeltaYSinceLastMouseMove();
        }

        // Send out mouse event handlers for the specific mouse event.
        List<MouseHandler> mouseHandlers = this.mouseHandlersByType.get(evt.getEventType());
        if (mouseHandlers != null && mouseHandlers.size() > 0) {
            for (int i = 0; i < mouseHandlers.size(); i++) {
                MouseHandler handler = mouseHandlers.get(i);

                try {
                    handler.accept(this, evt, mouseDeltaX, mouseDeltaY);
                } catch (Throwable th) {
                    String errorMessage = "Failed to run MouseHandler " + handler + ".";
                    getLogger().throwing("InputManager", "processMouseEvents", new RuntimeException(errorMessage, th));
                }

                // If the event was consumed, abort.
                if (evt.isConsumed())
                    return;
            }
        }

        // Send out generic key handlers.
        for (int i = 0; i < this.mouseHandlers.size(); i++) {
            MouseHandler handler = this.mouseHandlers.get(i);

            try {
                handler.accept(this, evt, mouseDeltaX, mouseDeltaY);
            } catch (Throwable th) {
                String errorMessage = "Failed to run MouseHandler " + handler + ".";
                getLogger().throwing("InputManager", "processMouseEvents", new RuntimeException(errorMessage, th));
            }

            // If the event was consumed, abort.
            if (evt.isConsumed())
                return;
        }

        if (this.finalMouseHandler != null) {
            try {
                this.finalMouseHandler.accept(this, evt, mouseDeltaX, mouseDeltaY);
            } catch (Throwable th) {
                String errorMessage = "Failed to run final MouseHandler " + this.finalMouseHandler + ".";
                getLogger().throwing("InputManager", "processMouseEvents", new RuntimeException(errorMessage, th));
            }
        }

        // Mark drag as inactive after the listener is called.
        this.mouseTracker.markDragInactiveIfComplete();
    }

    /**
     * Function to process mouse input events.
     */
    private void processScrollEvents(ScrollEvent event) {
        if (Math.abs(event.getDeltaY()) < .00001)
            return;

        // Send out generic scroll handlers.
        for (int i = 0; i < this.scrollHandlers.size(); i++) {
            ScrollHandler handler = this.scrollHandlers.get(i);

            try {
                handler.accept(this, event);
            } catch (Throwable th) {
                String errorMessage = "Failed to run ScrollHandler " + handler + ".";
                getLogger().throwing("InputManager", "processScrollEvents", new RuntimeException(errorMessage, th));
            }

            // If the event was consumed, abort.
            if (event.isConsumed())
                return;
        }

        if (this.finalScrollHandler != null) {
            try {
                this.finalScrollHandler.accept(this, event);
            } catch (Throwable th) {
                String errorMessage = "Failed to run final ScrollHandler " + this.finalScrollHandler + ".";
                getLogger().throwing("InputManager", "processScrollEvents", new RuntimeException(errorMessage, th));
            }
        }
    }


    /**
     * Gets the logger for this class.
     */
    private ILogger getLogger() {
        return ClassNameLogger.getLogger(this.gameInstance, getClass());
    }

    /**
     * Represents cached information about the mouse information at a given time.
     * Data source from MouseEvent.
     */
    @Getter
    public static class MouseInputState {
        private double x; // The x position of the mouse relative to the node.
        private double y; // The y position of the mouse relative to the node.
        private double sceneX; // The x position of the mouse.
        private double sceneY; // The y position of the mouse.
        private final Vector3f intersectedPoint = new Vector3f();
        private int intersectedFaceIndex = -1;
        private Node intersectedNode;

        /**
         * Applies information from a MouseEvent to this mouse state.
         * @param event The event to apply information from.
         */
        public void apply(MouseEvent event) {
            this.x = event.getX();
            this.y = event.getY();
            this.sceneX = event.getSceneX();
            this.sceneY = event.getSceneY();

            // Reset information to a default state.
            this.intersectedPoint.setXYZ(0F, 0F, 0F);
            this.intersectedFaceIndex = -1;
            this.intersectedNode = null;

            // Grab more information from the mouse.
            PickResult result = event.getPickResult();
            if (result != null) {
                this.intersectedFaceIndex = result.getIntersectedFace();
                this.intersectedNode = result.getIntersectedNode();
                if (result.getIntersectedPoint() != null)
                    this.intersectedPoint.setXYZ(result.getIntersectedPoint());
            }
        }

        /**
         * Applies the contents of another mouse input state to this one.
         * @param other The input state to apply.
         */
        public void apply(MouseInputState other) {
            this.x = other.getX();
            this.y = other.getY();
            this.sceneX = other.getSceneX();
            this.sceneY = other.getSceneY();
            this.intersectedPoint.setXYZ(other.getIntersectedPoint());
            this.intersectedFaceIndex = other.getIntersectedFaceIndex();
            this.intersectedNode = other.getIntersectedNode();
        }
    }

    @Getter
    public static class MouseTracker {
        private final MouseInputState lastDragStartMouseState = new MouseInputState();
        private final MouseInputState lastMouseState = new MouseInputState();
        private final MouseInputState mouseState = new MouseInputState();
        private MouseDragState dragState = MouseDragState.INACTIVE;
        @Setter private boolean significantMouseDragRecorded;

        public static final int SIZABLE_DRAG_THRESHOLD = 4;

        /**
         * Handles the mouse event.
         * @param event the mouse event to handle
         */
        public void handle(MouseEvent event) {
            if (MouseEvent.MOUSE_PRESSED.equals(event.getEventType())) {
                this.dragState = MouseDragState.START;
                this.significantMouseDragRecorded = false;
                this.lastDragStartMouseState.apply(event);
                this.lastMouseState.apply(event);
                this.mouseState.apply(event);
            } else if (MouseEvent.MOUSE_DRAGGED.equals(event.getEventType())) {
                this.dragState = MouseDragState.IN_PROGRESS;
                this.lastMouseState.apply(this.mouseState);
                this.mouseState.apply(event);
                if (Math.abs(getDeltaXSinceDragStart()) >= SIZABLE_DRAG_THRESHOLD || Math.abs(getDeltaYSinceDragStart()) >= SIZABLE_DRAG_THRESHOLD)
                    this.significantMouseDragRecorded = true;
            } else if (MouseEvent.MOUSE_RELEASED.equals(event.getEventType())) {
                this.dragState = MouseDragState.COMPLETE;
                this.lastMouseState.apply(this.mouseState);
                this.mouseState.apply(event);
                if (Math.abs(getDeltaXSinceDragStart()) >= SIZABLE_DRAG_THRESHOLD || Math.abs(getDeltaYSinceDragStart()) >= SIZABLE_DRAG_THRESHOLD)
                    this.significantMouseDragRecorded = true;
            }
        }

        /**
         * Gets the delta X change since the last mouse movement.
         */
        public double getDeltaXSinceLastMouseMove() {
            return this.mouseState.getSceneX() - this.lastMouseState.getSceneX();
        }

        /**
         * Gets the delta Y change since the last mouse movement.
         */
        public double getDeltaYSinceLastMouseMove() {
            return this.mouseState.getSceneY() - this.lastMouseState.getSceneY();
        }

        /**
         * Gets the delta X change since the last mouse drag began.
         */
        public double getDeltaXSinceDragStart() {
            return this.mouseState.getSceneX() - this.lastDragStartMouseState.getSceneX();
        }

        /**
         * Gets the delta Y change since the last mouse drag began.
         */
        public double getDeltaYSinceDragStart() {
            return this.mouseState.getSceneY() - this.lastDragStartMouseState.getSceneY();
        }

        /**
         * Marks the drag state as inactive if the drag is complete.
         * This only needs to be called by the mouse dragging systems, and not mouse drag listeners.
         */
        public void markDragInactiveIfComplete() {
            if (this.dragState == MouseDragState.COMPLETE)
                this.dragState = MouseDragState.INACTIVE;
        }
    }

    /**
     * Represents the current state of the drag.
     */
    public enum MouseDragState {
        INACTIVE, // The drag is not currently active.
        START, // The first stage of the drag.
        IN_PROGRESS, // The drag started previously and is currently updating.
        COMPLETE, // The drag is processing its final update before returning to inactive.
    }
}