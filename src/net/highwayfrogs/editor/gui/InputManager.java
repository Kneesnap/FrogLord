package net.highwayfrogs.editor.gui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.system.math.Vector3f;

import java.util.*;
import java.util.logging.Logger;

/**
 * This tracks keyboard and mouse input for usage primarily in 3D environments.
 * Created by Kneesnap on 1/2/2024.
 */
public class InputManager {
    private final Map<KeyCode, List<KeyHandler>> keySpecificHandlers = new HashMap<>();
    private final ChangeListener<? super Boolean> stageInputListener = this::onStageFocusChange;
    private final List<KeyHandler> keyHandlers = new ArrayList<>();
    private Stage stage;
    @Setter private KeyHandler finalKeyHandler;
    private final List<MouseHandler> mouseHandlers = new ArrayList<>();
    @Setter private MouseHandler finalMouseHandler;
    private final Map<EventType<? super MouseEvent>, List<MouseHandler>> mouseHandlersByType = new HashMap<>();
    private final boolean[] pressedKeys = new boolean[KeyCode.values().length];
    @Getter private final MouseInputState lastDragStartMouseState = new MouseInputState();
    @Getter private final MouseInputState lastMouseState = new MouseInputState();
    @Getter private final MouseInputState mouseState = new MouseInputState();
    private Logger cachedLogger;

    public interface KeyHandler {
        void accept(InputManager manager, KeyEvent event);
    }

    public interface MouseHandler {
        void accept(InputManager manager, MouseEvent event, double deltaX, double deltaY);
    }

    /**
     * Assign (setup) the control event handlers on the supplied scene object.
     * @param scene The subscene to receive and process the keyboard and mouse events, etc.
     */
    public void assignSceneControls(Stage stage, Scene scene) {
        scene.addEventHandler(KeyEvent.ANY, this::processKeyEvents);
        scene.addEventHandler(MouseEvent.ANY, this::processMouseEvents);

        // Reset keys when focus is lost.
        stage.focusedProperty().addListener(this.stageInputListener);
        this.stage = stage;
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
     * Test if the particular key is pressed.
     * @param keyCode key code to test current state
     * @return true, if the key is currently pressed
     */
    public boolean isKeyPressed(KeyCode keyCode) {
        return keyCode != null && this.pressedKeys[keyCode.ordinal()];
    }

    /**
     * Tests if the mouse has moved meaningfully since the drag started.
     */
    public boolean hasMouseMovedSinceDragStart() {
        return Math.abs(this.mouseState.getX() - this.lastDragStartMouseState.getX()) >= 5
                || Math.abs(this.mouseState.getY() - this.lastDragStartMouseState.getY()) >= 5;
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
                    String errorMessage = "Failed to run KeyEventHandler " + handler + ".";
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
                String errorMessage = "Failed to run KeyEventHandler " + handler + ".";
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
            String errorMessage = "Failed to run final KeyEventHandler " + this.finalKeyHandler + ".";
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
                        String errorMessage = "Failed to run KeyEventHandler " + handler + ".";
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
                    String errorMessage = "Failed to run KeyEventHandler " + handler + ".";
                    getLogger().throwing("InputManager", "resetKeys", new RuntimeException(errorMessage, th));
                }
            }

            // Fire the last key handler.
            try {
                if (this.finalKeyHandler != null)
                    this.finalKeyHandler.accept(this, newEvent);
            } catch (Throwable th) {
                String errorMessage = "Failed to run the final KeyEventHandler " + this.finalKeyHandler + ".";
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
        double mouseDeltaX = 0;
        double mouseDeltaY = 0;
        if (evt.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
            this.lastDragStartMouseState.apply(evt);
            this.lastMouseState.apply(evt);
            this.mouseState.apply(evt);
        } else if (evt.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
            this.lastMouseState.apply(this.mouseState);
            this.mouseState.apply(evt);
            mouseDeltaX = (this.mouseState.getX() - this.lastMouseState.getX());
            mouseDeltaY = (this.mouseState.getY() - this.lastMouseState.getY());
        }

        // Send out mouse event handlers for the specific mouse event.
        List<MouseHandler> mouseHandlers = this.mouseHandlersByType.get(evt.getEventType());
        if (mouseHandlers != null && mouseHandlers.size() > 0) {
            for (int i = 0; i < mouseHandlers.size(); i++) {
                MouseHandler handler = mouseHandlers.get(i);

                try {
                    handler.accept(this, evt, mouseDeltaX, mouseDeltaY);
                } catch (Throwable th) {
                    String errorMessage = "Failed to run MouseInputHandler " + handler + ".";
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
                String errorMessage = "Failed to run MouseInputHandler " + handler + ".";
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
                String errorMessage = "Failed to run final MouseInputHandler " + this.finalMouseHandler + ".";
                getLogger().throwing("InputManager", "processMouseEvents", new RuntimeException(errorMessage, th));
            }
        }
    }

    /**
     * Gets the logger for this class.
     */
    private Logger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger("InputManager");
    }

    /**
     * Represents cached information about the mouse information at a given time.
     * Data source from MouseEvent.
     */
    @Getter
    public static class MouseInputState {
        private double x; // The x position of the mouse.
        private double y; // The y position of the mouse.
        private final Vector3f intersectedPoint = new Vector3f();
        private int intersectedFaceIndex = -1;
        private Node intersectedNode;

        /**
         * Applies information from a MouseEvent to this mouse state.
         * @param event The event to apply information from.
         */
        public void apply(MouseEvent event) {
            this.x = event.getSceneX();
            this.y = event.getSceneY();

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
            this.intersectedPoint.setXYZ(other.getIntersectedPoint());
            this.intersectedFaceIndex = other.getIntersectedFaceIndex();
            this.intersectedNode = other.getIntersectedNode();
        }
    }
}