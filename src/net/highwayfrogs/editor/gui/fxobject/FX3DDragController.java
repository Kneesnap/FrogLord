package net.highwayfrogs.editor.gui.fxobject;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.KeyHandler;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Manages a 3D shape, so it can be dragged around 3D space.
 * TODO: Support fake-clicks. Eg: When you want to drag an entity with Frogger, you don't click on the entity, you click on 2D UI.
 *  -> These clicks should treat it as accept when a click occurs.
 *  -> Escape to cancel like usual. But also, we need some master tracker to cancel the previous fake-click if a current one or drag is active.
 *  -> Because the entity wasn't clicked, treat the originalPosition as the model origin (model original world position)
 * TODO: Allow a mode where the plane is present, but has depth-test enabled, so that if terrain exceeds the plane height, it will snap to terrain. (Need to ensure we do not raise the plane height when we do this though)
 * TODO: I forgot to make the gizmo base orange during a drag.
 * TODO: Limit the axis planes to the size of the world.
 * TODO: The gizmo base isn't updated to orange while dragged.
 * Created by Kneesnap on 2/20/2026.
 */
public abstract class FX3DDragController {
    @Getter private final MeshViewController<?> controller;
    private final KeyHandler keyListener = this::onKeyPressed;
    private final EventHandler<MouseEvent> mouseEnterListener = this::handleMouseEnter;
    private final EventHandler<MouseEvent> mouseExitListener = this::handleMouseExit;
    private final EventHandler<MouseEvent> mousePressListener = this::handleMousePress;
    private final EventHandler<MouseEvent> mouseDragListener = this::handleMouseDrag;
    private final EventHandler<MouseEvent> mouseReleaseListener = this::handleMouseRelease;
    @Getter private Shape3D shape;
    @Getter private boolean mouseDragActive;
    @Setter @Getter private boolean snapToTerrain;

    @Setter private Material regularMaterial = MATERIAL_DEFAULT;
    @Setter private Material highlightedMaterial = MATERIAL_HIGHLIGHTED;
    @Setter private Material draggedMaterial = MATERIAL_DRAGGED;
    @Setter private double snappingThreshold = 3.5;

    private EventHandler<? super MouseEvent> oldMouseEnterListener;
    private EventHandler<? super MouseEvent> oldMouseExitListener;
    private EventHandler<? super MouseEvent> oldMousePressListener;
    private EventHandler<? super MouseEvent> oldMouseDragListener;
    private EventHandler<? super MouseEvent> oldMouseReleaseListener;

    private Box dragPlane; // If a movement plane is used...
    private Point3D dragStartPosition;
    private Point3D originalPosition;

    private static final PhongMaterial MATERIAL_DEFAULT = Scene3DUtils.makeUnlitSharpMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_HIGHLIGHTED = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTYELLOW);
    private static final PhongMaterial MATERIAL_DRAGGED = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);

    public FX3DDragController(MeshViewController<?> controller) {
        this.controller = controller;
    }

    /**
     * Sets the shape which is updated as the display at the position being dragged.
     * @param newShape the shape to apply
     */
    public void setShape(Shape3D newShape) {
        if (this.shape == newShape)
            return;
        if (this.mouseDragActive)
            throw new IllegalStateException("Cannot change the active shape while it is actively being dragged.");

        if (this.shape != null) {
            // Restore previously tracked mouse behaviors.
            this.shape.setOnMouseEntered(this.oldMouseEnterListener);
            this.shape.setOnMouseExited(this.oldMouseExitListener);
            this.shape.setOnMousePressed(this.oldMousePressListener);
            this.shape.setOnMouseDragged(this.oldMouseDragListener);
            this.shape.setOnMouseReleased(this.oldMouseReleaseListener);
        }

        this.shape = newShape;
        this.oldMouseEnterListener = newShape.getOnMouseEntered();
        this.oldMouseExitListener = newShape.getOnMouseExited();
        this.oldMousePressListener = newShape.getOnMousePressed();
        this.oldMouseDragListener = newShape.getOnMouseDragged();
        this.oldMouseReleaseListener = newShape.getOnMouseReleased();
        newShape.setOnMouseEntered(this.mouseEnterListener);
        newShape.setOnMouseExited(this.mouseExitListener);
        newShape.setOnMousePressed(this.mousePressListener);
        newShape.setOnMouseDragged(this.mouseDragListener);
        newShape.setOnMouseReleased(this.mouseReleaseListener);
        this.regularMaterial = newShape.getMaterial();
        if (this.regularMaterial == null)
            this.regularMaterial = MATERIAL_DEFAULT;
    }

    /**
     * Handles the position change, potentially cancelling/declining the change.
     * @param oldPos the old position which was dragged from
     * @param newPos the new position which was dragged to
     * @return the true new position to apply, or null to indicate no move will occur.
     */
    protected abstract Point3D handlePositionChange(Point3D oldPos, Point3D newPos);

    /**
     * Accepts the position change, potentially cancelling/declining the change.
     * @param oldPos the old position which was originally dragged from
     * @param newPos the new position which was dragged to, and placed at
     */
    protected abstract void acceptPosition(Point3D oldPos, Point3D newPos);

    protected void handleMouseEnter(MouseEvent event) {
        // Run previous listener.
        if (this.oldMouseEnterListener != null) {
            this.oldMouseEnterListener.handle(event);
            if (event.isConsumed())
                return;
        }

        if (!this.mouseDragActive && this.highlightedMaterial != null)
            this.shape.setMaterial(this.highlightedMaterial);
    }

    protected void handleMouseExit(MouseEvent event) {
        // Run previous listener.
        if (this.oldMouseExitListener != null) {
            this.oldMouseExitListener.handle(event);
            if (event.isConsumed())
                return;
        }

        if (!this.mouseDragActive && this.highlightedMaterial == this.shape.getMaterial() && this.regularMaterial != null)
            this.shape.setMaterial(this.regularMaterial);
    }

    private void handleMousePress(MouseEvent event) {
        // Run previous listener.
        if (this.oldMousePressListener != null) {
            this.oldMousePressListener.handle(event);
            if (event.isConsumed())
                return;
        }

        this.mouseDragActive = true;
        if (this.draggedMaterial != null)
            this.shape.setMaterial(this.draggedMaterial);

        Group scene = Scene3DUtils.getSubSceneGroup(this.shape.getScene().getRoot());

        // Clear existing axis plane, if it somehow exists.
        if (this.dragPlane != null) {
            scene.getChildren().remove(this.dragPlane);
            this.dragPlane = null;
        }

        // Find the node clicked.
        PickResult result = event.getPickResult();
        Translate currentPos = Scene3DUtils.getOptional3DTranslation(this.shape);
        this.originalPosition = currentPos != null ? new Point3D(currentPos.getX(), currentPos.getY(), currentPos.getZ()) : Point3D.ZERO;

        // Setup axis plane for mouse-picking.
        if (!this.snapToTerrain)
            this.dragPlane = Scene3DUtils.createAxisPlaneXZ(this.shape, scene);

        // Update state.
        Scale scale = Scene3DUtils.getOptional3DScale(this.shape);
        if (scale != null) {
            Point3D startPoint = result.getIntersectedPoint();
            this.dragStartPosition = new Point3D(startPoint.getX() * scale.getX(), startPoint.getY() * scale.getY(), startPoint.getZ() * scale.getZ());
        } else {
            this.dragStartPosition = result.getIntersectedPoint();
        }

        // Add key listener, so we can cancel if necessary.
        if (this.controller != null && this.controller.getInputManager() != null)
            this.controller.getInputManager().addKeyListener(KeyCode.ESCAPE, this.keyListener);

        // Disable dragging on the gizmo, so only the plane will get events.
        this.shape.setMouseTransparent(true);
        this.shape.setCursor(Cursor.CROSSHAIR);

        // Prevent the drag updates from moving the camera view.
        event.consume();
    }

    private void handleMouseDrag(MouseEvent event) {
        // Run previous listener.
        if (this.oldMouseDragListener != null) {
            this.oldMouseDragListener.handle(event);
            if (event.isConsumed())
                return;
        }

        event.consume(); // Prevent this drag from moving the camera view.

        // Abort if no known spot is found.
        PickResult result = event.getPickResult();
        if (result == null)
            return;

        // If there's no drag active, abort?
        if (!this.mouseDragActive)
            return;

        if (this.snapToTerrain) {
            // Ensure that the picked node is a DynamicMesh.
            Node node = result.getIntersectedNode();
            if (!(node instanceof MeshView) || !(((MeshView) node).getMesh() instanceof DynamicMesh))
                return;
        } else if (this.dragPlane != null) {
            // Ensure that the picked node is either the gizmo MeshView, or the AxisPlane. (Prevents other nodes such as 2D UI from breaking the position)
            if ((result.getIntersectedNode() != this.shape) && (result.getIntersectedNode() != this.dragPlane))
                return;
        }

        // Test if there's an intersection point.
        Point3D mousePoint = result.getIntersectedPoint();
        if (mousePoint == null)
            return;

        // Ensure the coordinates we get are now absolute.
        Point3D mouseOffset = mousePoint.subtract(this.dragStartPosition);
        Point3D newWorldPos = result.getIntersectedNode().localToScene(mouseOffset);

        // Get old position.
        Translate shapeTranslate = Scene3DUtils.get3DTranslation(this.shape);
        double oldX = shapeTranslate.getX();
        double oldY = shapeTranslate.getY();
        double oldZ = shapeTranslate.getZ();
        Point3D oldPosition = new Point3D(oldX, oldY, oldZ);

        // Get new position.
        double newX = newWorldPos.getX();
        double newY = this.snapToTerrain ? newWorldPos.getY() : oldY; // Use IF terrain snapping is enabled.
        double newZ = newWorldPos.getZ();
        Point3D newPosition = new Point3D(newX, newY, newZ);

        // Try to snap to the original position.
        if (this.originalPosition.distance(newPosition) < this.snappingThreshold)
            newPosition = this.originalPosition;

        // Call listeners, and fire 3D update.
        newPosition = handlePositionChange(oldPosition, newPosition);
        if (newPosition != null) {
            shapeTranslate.setX(newPosition.getX());
            shapeTranslate.setY(newPosition.getY());
            shapeTranslate.setZ(newPosition.getZ());

            if (this.dragPlane != null) {
                Translate planeTranslate = Scene3DUtils.get3DTranslation(this.dragPlane);
                planeTranslate.setX(newPosition.getX());
                planeTranslate.setY(newPosition.getY());
                planeTranslate.setZ(newPosition.getZ());
            }
        }
    }

    private void handleMouseRelease(MouseEvent event) {
        // Run previous listener.
        if (this.oldMouseReleaseListener != null) {
            this.oldMouseReleaseListener.handle(event);
            if (event.isConsumed())
                return;
        }

        if (this.mouseDragActive) {
            event.consume(); // Prevent the drag updates from moving the camera view.
            stopDragging(false); // Stop dragging.
        }
    }

    private void onKeyPressed(InputManager manager, KeyEvent event) {
        // Cancel the drag, and restore the original position.
        event.consume();
        stopDragging(true);
    }

    private void stopDragging(boolean restoreOriginalPosition) {
        if (this.controller != null && this.controller.getInputManager() != null)
            this.controller.getInputManager().removeKeyListener(KeyCode.ESCAPE, this.keyListener);

        // Remove the axis plane from the scene.
        if (this.dragPlane != null) {
            Group scene = Scene3DUtils.getSubSceneGroup(this.dragPlane.getScene());
            scene.getChildren().remove(this.dragPlane);
            this.dragPlane = null;
        }

        // Make the arrow be highlighted no longer.
        Translate position = Scene3DUtils.get3DTranslation(this.shape);
        Point3D currentPosition = new Point3D(position.getX(), position.getY(), position.getZ());
        if (restoreOriginalPosition && !this.originalPosition.equals(currentPosition)) {
            if (!this.originalPosition.equals(handlePositionChange(currentPosition, this.originalPosition)) && this.controller != null)
                this.controller.getLogger().warning("A(n) %s was unable to return the drag to the original position!", Utils.getSimpleName(this));

            position.setX(this.originalPosition.getX());
            position.setY(this.originalPosition.getY());
            position.setZ(this.originalPosition.getZ());
        }

        // Accept the position, unless it's at the original position.
        Point3D finalPosition = new Point3D(position.getX(), position.getY(), position.getZ());
        if (!restoreOriginalPosition && !this.originalPosition.equals(finalPosition))
            acceptPosition(this.originalPosition, finalPosition);

        // Clear remaining drag state.
        this.dragStartPosition = null;
        this.originalPosition = null;

        // Re-enable dragging on the gizmo.
        this.shape.setMouseTransparent(false);
        this.shape.setCursor(Cursor.DEFAULT);

        this.mouseDragActive = false;
        if (this.shape.getMaterial() == this.draggedMaterial && this.regularMaterial != null)
            this.shape.setMaterial(this.regularMaterial);
    }

    /**
     * Implements {@code FX3DDragController} using lambda functions for lazy instantiation.
     */
    public static class LazyFX3DDragController extends FX3DDragController {
        private final BiConsumer<Point3D, Point3D> positionChangeHandlerWithoutReturn;
        private final BiFunction<Point3D, Point3D, Point3D> positionChangeHandler;
        private final BiConsumer<Point3D, Point3D> positionAcceptor;

        public LazyFX3DDragController(MeshViewController<?> controller, BiConsumer<Point3D, Point3D> positionChangeHandlerWithoutReturn) {
            super(controller);
            this.positionChangeHandlerWithoutReturn = positionChangeHandlerWithoutReturn;
            this.positionChangeHandler = null;
            this.positionAcceptor = null;
        }

        public LazyFX3DDragController(MeshViewController<?> controller, BiFunction<Point3D, Point3D, Point3D> positionChangeHandler) {
            super(controller);
            this.positionChangeHandlerWithoutReturn = null;
            this.positionChangeHandler = positionChangeHandler;
            this.positionAcceptor = null;
        }

        public LazyFX3DDragController(MeshViewController<?> controller, BiConsumer<Point3D, Point3D> positionChangeHandlerWithoutReturn, BiConsumer<Point3D, Point3D> positionAcceptor) {
            super(controller);
            this.positionChangeHandlerWithoutReturn = positionChangeHandlerWithoutReturn;
            this.positionChangeHandler = null;
            this.positionAcceptor = positionAcceptor;
        }

        public LazyFX3DDragController(MeshViewController<?> controller, BiFunction<Point3D, Point3D, Point3D> positionChangeHandler, BiConsumer<Point3D, Point3D> positionAcceptor) {
            super(controller);
            this.positionChangeHandlerWithoutReturn = null;
            this.positionChangeHandler = positionChangeHandler;
            this.positionAcceptor = positionAcceptor;
        }

        @Override
        protected Point3D handlePositionChange(Point3D oldPos, Point3D newPos) {
            if (this.positionChangeHandler != null) {
                return this.positionChangeHandler.apply(oldPos, newPos);
            } else if (this.positionChangeHandlerWithoutReturn != null) {
                this.positionChangeHandlerWithoutReturn.accept(oldPos, newPos);
                return newPos;
            } else {
                return newPos;
            }
        }

        @Override
        protected void acceptPosition(Point3D oldPos, Point3D newPos) {
            if (this.positionAcceptor != null)
                this.positionAcceptor.accept(oldPos, newPos);
        }
    }

    /**
     * Implements an FX3DDragController for use with a Translation Gizmo, imbuing the ability to drag around the base.
     * This works by running the original translation gizmo mouse event handlers, and making ours only trigger when the drag starts on the gizmo base cube, which is explicitly unused in the original mesh.
     */
    public static abstract class FX3DDragTranslationGizmo extends FX3DDragController {
        @NonNull private final TranslationGizmo translationGizmo;

        public FX3DDragTranslationGizmo(MeshViewController<?> controller, @NonNull TranslationGizmo translationGizmo) {
            super(controller);
            this.translationGizmo = translationGizmo;
            translationGizmo.setHighlightBaseNode(true);
            setRegularMaterial(null); // No material changes should happen.
            setHighlightedMaterial(null); // No material changes should happen.
            setDraggedMaterial(null); // No material changes should happen.
        }

        @Override
        public void setShape(Shape3D shape) {
            MeshView oldMeshView = getShape();
            if (shape == oldMeshView)
                return;
            if (shape != null && !(shape instanceof MeshView))
                throw new IllegalArgumentException("The provided shape was not a MeshView! (Was: " + Utils.getSimpleName(shape) + ")");

            if (oldMeshView != null)
                this.translationGizmo.removeView(oldMeshView);

            MeshView newMeshView = (MeshView) shape;
            if (newMeshView != null)
                this.translationGizmo.addView(newMeshView, getController(), this::handlePositionChange);

            // MUST RUN AFTER addView, otherwise the listeners won't have been set!!!!
            super.setShape(shape);
            setRegularMaterial(null); // No material changes should happen.
            setHighlightedMaterial(null); // No material changes should happen.
            setDraggedMaterial(null); // No material changes should happen.
        }

        private void handlePositionChange(MeshView meshView, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, int flags) {
            Point3D oldPosition = new Point3D(oldX, oldY, oldZ);
            Point3D newPosition = new Point3D(newX, newY, newZ);
            if ((flags & TranslationGizmo.FLAG_ACCEPTED_POSITION) == TranslationGizmo.FLAG_ACCEPTED_POSITION) {
                acceptPosition(oldPosition, newPosition);
                return;
            }

            // Handle position change.
            Point3D outputPosition = handlePositionChange(oldPosition, newPosition);
            if (outputPosition != null && !outputPosition.equals(newPosition)) // handlePositionChange() itself will handle any positions it returns itself, so no update listeners need to be triggered, we can just update the node position and leave it like that.
                Scene3DUtils.setNodePosition(getShape(), outputPosition.getX(), outputPosition.getY(), outputPosition.getZ());
        }

        @Override
        public MeshView getShape() {
            return (MeshView) super.getShape();
        }

        @Override
        protected void handleMouseEnter(MouseEvent event) {
            setHighlightedMaterial(null); // Ensure the material does not change.
            super.handleMouseEnter(event);
        }

        @Override
        protected void handleMouseExit(MouseEvent event) {
            setRegularMaterial(null); // Ensure the material does not change.
            super.handleMouseExit(event);
        }
    }

    /**
     * Implements {@code FX3DDragTranslationGizmo} using lambda functions for lazy instantiation.
     */
    public static class LazyDragTranslationGizmo extends FX3DDragTranslationGizmo {
        private final BiConsumer<Point3D, Point3D> positionChangeHandlerWithoutReturn;
        private final BiFunction<Point3D, Point3D, Point3D> positionChangeHandler;
        private final BiConsumer<Point3D, Point3D> positionAcceptor;

        public LazyDragTranslationGizmo(MeshViewController<?> controller, @NonNull TranslationGizmo gizmo, BiConsumer<Point3D, Point3D> positionChangeHandlerWithoutReturn) {
            super(controller, gizmo);
            this.positionChangeHandlerWithoutReturn = positionChangeHandlerWithoutReturn;
            this.positionChangeHandler = null;
            this.positionAcceptor = null;
        }

        public LazyDragTranslationGizmo(MeshViewController<?> controller, @NonNull TranslationGizmo gizmo, BiFunction<Point3D, Point3D, Point3D> positionChangeHandler) {
            super(controller, gizmo);
            this.positionChangeHandlerWithoutReturn = null;
            this.positionChangeHandler = positionChangeHandler;
            this.positionAcceptor = null;
        }

        public LazyDragTranslationGizmo(MeshViewController<?> controller, @NonNull TranslationGizmo gizmo, BiConsumer<Point3D, Point3D> positionChangeHandlerWithoutReturn, BiConsumer<Point3D, Point3D> positionAcceptor) {
            super(controller, gizmo);
            this.positionChangeHandlerWithoutReturn = positionChangeHandlerWithoutReturn;
            this.positionChangeHandler = null;
            this.positionAcceptor = positionAcceptor;
        }

        public LazyDragTranslationGizmo(MeshViewController<?> controller, @NonNull TranslationGizmo gizmo, BiFunction<Point3D, Point3D, Point3D> positionChangeHandler, BiConsumer<Point3D, Point3D> positionAcceptor) {
            super(controller, gizmo);
            this.positionChangeHandlerWithoutReturn = null;
            this.positionChangeHandler = positionChangeHandler;
            this.positionAcceptor = positionAcceptor;
        }

        @Override
        protected Point3D handlePositionChange(Point3D oldPos, Point3D newPos) {
            if (this.positionChangeHandler != null) {
                return this.positionChangeHandler.apply(oldPos, newPos);
            } else if (this.positionChangeHandlerWithoutReturn != null) {
                this.positionChangeHandlerWithoutReturn.accept(oldPos, newPos);
                return newPos;
            } else {
                return newPos;
            }
        }

        @Override
        protected void acceptPosition(Point3D oldPos, Point3D newPos) {
            if (this.positionAcceptor != null)
                this.positionAcceptor.accept(oldPos, newPos);
        }
    }
}
