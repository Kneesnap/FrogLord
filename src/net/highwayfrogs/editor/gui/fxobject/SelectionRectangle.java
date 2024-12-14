package net.highwayfrogs.editor.gui.fxobject;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import lombok.Setter;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseInputState;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Manages a selection rectangle.
 * Created by Kneesnap on 1/12/2024.
 */
public class SelectionRectangle {
    private final MeshViewController<?> controller;
    private final Node node;
    private final Rectangle rectangle;
    private boolean selectionActive;
    @Setter private IMouseDragRectangleListener listener;

    public SelectionRectangle(MeshViewController<?> controller, Node node) {
        this.controller = controller;
        this.node = node;
        this.rectangle = setupRectangle();
        applyListeners();
    }

    private Rectangle setupRectangle() {
        Rectangle rectangle = new Rectangle(5, 5, 25, 25);
        rectangle.setMouseTransparent(true);
        rectangle.setStyle("-fx-fill: rgb(200,200,0); -fx-opacity: 50%; -fx-stroke: black; -fx-stroke-width: 2;");
        return rectangle;
    }

    private void applyListeners() {
        this.node.setOnMousePressed(this::onMousePressed);
        this.node.setOnMouseDragged(this::onMouseDragged);
        this.node.setOnMouseReleased(this::onMouseReleased);
    }

    private void onMousePressed(MouseEvent event) {
        InputManager manager = this.controller.getInputManager();
        if (!manager.isKeyPressed(KeyCode.SHIFT))
            return;

        // Start dragging.
        event.consume();
        this.selectionActive = true;
        manager.getLastDragStartMouseState().apply(event);
        manager.getLastMouseState().apply(event);
        manager.getMouseState().apply(event);
        this.rectangle.setX(0);
        this.rectangle.setY(0);
        this.rectangle.setWidth(0);
        this.rectangle.setHeight(0);
        this.controller.getRoot2D().getChildren().add(this.rectangle);
    }

    private void onMouseDragged(MouseEvent event) {
        if (!this.selectionActive)
            return;

        // Update preview.
        event.consume();
        InputManager manager = this.controller.getInputManager();
        SubScene subScene = this.controller.getSubScene();
        Point2D uiMinOffset = subScene.localToScene(0, 0); // Min corner of 3D view.
        Point2D uiMaxOffset = subScene.localToScene(subScene.getWidth() - 1, subScene.getHeight() - 1); // Max corner of 3D view.

        double minX = Math.max(uiMinOffset.getX(), Math.min(event.getSceneX(), manager.getLastDragStartMouseState().getX()));
        double maxX = Math.min(uiMaxOffset.getX(), Math.max(event.getSceneX(), manager.getLastDragStartMouseState().getX()));
        double minY = Math.max(uiMinOffset.getY(), Math.min(event.getSceneY(), manager.getLastDragStartMouseState().getY()));
        double maxY = Math.min(uiMaxOffset.getY(), Math.max(event.getSceneY(), manager.getLastDragStartMouseState().getY()));
        this.rectangle.setX(minX);
        this.rectangle.setY(minY);
        this.rectangle.setWidth(maxX - minX);
        this.rectangle.setHeight(maxY - minY);

        // Update InputManager.
        manager.getLastMouseState().apply(manager.getMouseState());
        manager.getMouseState().apply(event);
    }

    private void onMouseReleased(MouseEvent event) {
        if (!this.selectionActive)
            return;

        // Stop selecting.
        event.consume();
        this.selectionActive = false;
        this.controller.getRoot2D().getChildren().remove(this.rectangle);

        // Update InputManager.
        InputManager manager = this.controller.getInputManager();
        manager.getLastMouseState().apply(manager.getMouseState());
        manager.getMouseState().apply(event);

        // Alert listener.
        if (this.listener != null)
            this.listener.handle(manager.getLastDragStartMouseState(), manager.getMouseState());
    }

    /**
     * Represents a listener for the rectangle mouse drag.
     */
    public interface IMouseDragRectangleListener {
        void handle(MouseInputState dragStart, MouseInputState dragEnd);
    }
}
