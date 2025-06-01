package net.highwayfrogs.editor.gui.fxobject;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import lombok.NonNull;
import net.highwayfrogs.editor.gui.InputManager.MouseInputState;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Manages a selection rectangle.
 * Created by Kneesnap on 1/12/2024.
 */
public class SelectionRectangle extends FXDragListener {
    @NonNull private final MeshViewController<?> controller;
    private final Rectangle rectangle;

    public SelectionRectangle(@NonNull MeshViewController<?> controller, Node node) {
        super(controller.getInputManager().getMouseTracker(), node);
        this.controller = controller;
        this.rectangle = setupRectangle();
    }

    private Rectangle setupRectangle() {
        Rectangle rectangle = new Rectangle(5, 5, 25, 25);
        rectangle.setMouseTransparent(true);
        rectangle.setStyle("-fx-fill: rgb(200,200,0); -fx-opacity: 50%; -fx-stroke: black; -fx-stroke-width: 2;");
        return rectangle;
    }

    @Override
    protected boolean shouldStartDrag(MouseEvent event) {
        // In 3D space, dragging the mouse normally will move the camera view or rotate the model.
        // So, we need a key to enable it instead.
        return super.shouldStartDrag(event) && event.isShiftDown();
    }

    @Override
    protected boolean onMousePressed(MouseEvent event) {
        if (!super.onMousePressed(event))
            return false;

        this.rectangle.setX(0);
        this.rectangle.setY(0);
        this.rectangle.setWidth(0);
        this.rectangle.setHeight(0);
        this.controller.getRoot2D().getChildren().add(this.rectangle);
        return true;
    }

    @Override
    protected boolean onMouseDragged(MouseEvent event) {
        if (!super.onMouseDragged(event))
            return false;

        SubScene subScene = this.controller.getSubScene();
        Point2D uiMinOffset = subScene.localToScene(0, 0); // Min corner of 3D view.
        Point2D uiMaxOffset = subScene.localToScene(subScene.getWidth() - 1, subScene.getHeight() - 1); // Max corner of 3D view.

        MouseInputState lastDragStartMouseState = this.controller.getInputManager().getMouseTracker().getLastDragStartMouseState();
        double minX = Math.max(uiMinOffset.getX(), Math.min(event.getSceneX(), lastDragStartMouseState.getSceneX()));
        double maxX = Math.min(uiMaxOffset.getX(), Math.max(event.getSceneX(), lastDragStartMouseState.getSceneX()));
        double minY = Math.max(uiMinOffset.getY(), Math.min(event.getSceneY(), lastDragStartMouseState.getSceneY()));
        double maxY = Math.min(uiMaxOffset.getY(), Math.max(event.getSceneY(), lastDragStartMouseState.getSceneY()));
        this.rectangle.setX(minX);
        this.rectangle.setY(minY);
        this.rectangle.setWidth(maxX - minX);
        this.rectangle.setHeight(maxY - minY);
        return true;
    }

    @Override
    protected boolean onMouseReleased(MouseEvent event) {
        if (!super.onMouseReleased(event))
            return false;

        this.controller.getRoot2D().getChildren().remove(this.rectangle);
        return true;
    }
}
