package net.highwayfrogs.editor.gui.mesh.fxobject;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import lombok.Getter;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.utils.Scene3DUtils;

/**
 * Implements node-agnostic screen-space 3D dragging.
 * Creates an invisible plane parallel to the camera view plane, centered on the dragged point.
 * The calling code is responsible for disabling camera movement and other input.
 * Created by Kneesnap on 6/19/2026.
 */
public abstract class DragGizmo {
    @Getter private boolean dragActive;
    @Getter private boolean readyToBeginDrag;
    private boolean consumeMouseUntilRelease;
    @Getter private Node draggedNode;
    @Getter private Box dragPlane;
    @Getter protected Point3D planePos;
    @Getter private Point3D startMousePointInPlane;
    private Point3D startMousePointInScene;
    private double startMouseSceneX = Double.NaN;
    private double startMouseSceneY = Double.NaN;
    @Getter private final Vector3f rotationAxis = new Vector3f();
    @Getter private double startAngle;
    @Getter private double startDistance;

    private final EventHandler<? super MouseEvent> mousePressHandler = this::onSkeletonMousePressed;
    private final EventHandler<? super MouseEvent> mouseDragHandler = this::onSkeletonMouseDragged;
    private final EventHandler<? super MouseEvent> mouseReleaseHandler = this::onSkeletonMouseReleased;

    private static final double DRAG_PLANE_SIZE = 10000;
    private static final PhongMaterial DRAG_PLANE_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.TRANSPARENT);

    /**
     * Starts dragging against a camera-facing plane.
     * @param sceneNode any node in the scene graph containing the 3D SubScene group
     * @param camera the camera whose view plane should be matched
     * @param planePos plane center in scene coordinates
     * @param startMousePoint initial mouse point in scene coordinates
     * @return true iff dragging was started
     */
    public boolean startDrag(Node sceneNode, Camera camera, Point3D planePos, Point3D startMousePoint) {
        if (this.dragActive)
            stopDrag(true);

        this.consumeMouseUntilRelease = false;
        if (sceneNode == null || sceneNode.getScene() == null || camera == null || planePos == null || startMousePoint == null)
            return false;

        Group sceneGroup = Scene3DUtils.getSubSceneGroup(sceneNode.getScene());
        if (sceneGroup == null)
            return false;

        Box plane = createCameraFacingDragPlane(camera, planePos);
        sceneGroup.getChildren().add(plane);
        this.dragPlane = plane;
        this.draggedNode = sceneNode;
        this.planePos = planePos;
        this.startMousePointInPlane = plane.sceneToLocal(startMousePoint);
        getDragRotationAxisInModelSpace(sceneNode, this.rotationAxis);
        this.startAngle = Math.atan2(this.startMousePointInPlane.getY(), this.startMousePointInPlane.getX());
        this.startDistance = Math.sqrt((this.startMousePointInPlane.getX() * this.startMousePointInPlane.getX())
                + (this.startMousePointInPlane.getY() * this.startMousePointInPlane.getY()));

        // Attempt to start the drag.
        if (!onDragStart(sceneNode)) {
            cleanupDrag(sceneNode, true);
            return false;
        }

        this.dragActive = true;
        this.readyToBeginDrag = false;
        sceneNode.setMouseTransparent(true);
        sceneNode.setCursor(javafx.scene.Cursor.CROSSHAIR);
        return true;
    }

    /**
     * Obtains the position to place the plane at in the world
     * @param node the node which was clicked
     * @param event the related event, if there is one
     * @return planeStartPosition
     */
    protected abstract Point3D getPlaneStartPosition(Node node, MouseEvent event);

    /**
     * Called when a drag begins.
     * @param node the node to drag
     * @return true iff the drag should continue
     */
    protected abstract boolean onDragStart(Node node);

    /**
     * Gets the latest drag update from the JavaFX mouse event.
     * @param event the drag event
     * @return drag update, or null if the event did not hit the active drag plane
     */
    public DragUpdate updateDrag(MouseEvent event) {
        if (!this.dragActive || event == null)
            return null;

        PickResult result = event.getPickResult();
        if (result == null || result.getIntersectedNode() != this.dragPlane)
            return null;

        Point3D planePoint = result.getIntersectedPoint();
        if (planePoint == null)
            return null;

        DragUpdate dragUpdate = new DragUpdate(this, planePoint, event);
        onDragUpdate(dragUpdate);
        return dragUpdate;
    }

    /**
     * Stops the active drag and removes the invisible plane.
     */
    public void stopDrag(boolean cancel) {
        if (this.dragActive)
            onDragStop(this.draggedNode, cancel);

        if (this.dragActive || this.readyToBeginDrag)
            cleanupDrag(this.draggedNode, cancel);

        this.planePos = null;
        this.startMousePointInPlane = null;
        this.startMousePointInScene = null;
        this.startMouseSceneX = Double.NaN;
        this.startMouseSceneY = Double.NaN;
        this.consumeMouseUntilRelease |= cancel;
        this.dragActive = false;
        this.readyToBeginDrag = false;
    }

    private void cleanupDrag(Node node, boolean cancel) {
        onDragClean(node, cancel);
        if (!cancel)
            removeSceneMouseFilters();

        if (this.dragPlane != null) {
            Group sceneGroup = Scene3DUtils.getSubSceneGroup(this.dragPlane.getScene());
            if (sceneGroup != null)
                sceneGroup.getChildren().remove(this.dragPlane);

            this.dragPlane = null;
        }

        if (this.draggedNode != null) {
            this.draggedNode.setMouseTransparent(false);
            this.draggedNode.setCursor(javafx.scene.Cursor.DEFAULT);
            this.draggedNode = null;
        }

        this.planePos = null;
        this.readyToBeginDrag = false;
    }

    private void installSceneMouseFilters(Scene activeScene) {
        if (activeScene == null)
            return;

        removeSceneMouseFilters();
        activeScene.addEventFilter(MouseEvent.MOUSE_DRAGGED, this.mouseDragHandler);
        activeScene.addEventFilter(MouseEvent.MOUSE_RELEASED, this.mouseReleaseHandler);
    }

    private void removeSceneMouseFilters() {
        Scene activeScene = this.draggedNode != null ? this.draggedNode.getScene() : null;
        if (activeScene == null)
            return;

        activeScene.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this.mouseDragHandler);
        activeScene.removeEventFilter(MouseEvent.MOUSE_RELEASED, this.mouseReleaseHandler);
    }

    /**
     * Gets the drag plane's view normal converted into local space for the supplied node.
     * @param localNode the node whose local coordinate space should receive the normal
     * @return normal vector in local coordinates, or null if dragging is inactive
     */
    public Point3D getPlaneNormalInLocalSpace(Node localNode) {
        if (this.dragPlane == null || localNode == null || this.planePos == null)
            return null;

        Point3D sceneNormal = this.dragPlane.localToScene(0, 0, 1).subtract(this.dragPlane.localToScene(0, 0, 0)).normalize();
        Point3D localCenter = localNode.sceneToLocal(this.planePos);
        return localNode.sceneToLocal(this.planePos.add(sceneNormal)).subtract(localCenter).normalize();
    }

    private void getDragRotationAxisInModelSpace(Node node, Vector3f result) {
        Point3D modelNormal = getPlaneNormalInLocalSpace(node);
        if (modelNormal != null) {
            result.setXYZ(modelNormal);
        } else {
            result.setXYZ(0, 0, 1);
        }
    }

    /**
     * Adds the drag handlers to the given node.
     * @param node the node to add the handlers to
     */
    public void addDragHandlers(Node node) {
        node.addEventFilter(MouseEvent.MOUSE_PRESSED, this.mousePressHandler);
        node.addEventFilter(MouseEvent.MOUSE_DRAGGED, this.mouseDragHandler);
        node.addEventFilter(MouseEvent.MOUSE_RELEASED, this.mouseReleaseHandler);
    }

    /**
     * Removes the drag handlers from the given node.
     * @param node the node to remove the handlers from
     */
    public void removeDragHandlers(Node node) {
        node.removeEventFilter(MouseEvent.MOUSE_PRESSED, this.mousePressHandler);
        node.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this.mouseDragHandler);
        node.removeEventFilter(MouseEvent.MOUSE_RELEASED, this.mouseReleaseHandler);
        removeSceneMouseFilters();
    }

    /**
     * Called when the Node is clicked, as an attempt to start a drag.
     * The gizmo implementation will determine if this is a valid situation to start a drag.
     * @param event the mouse event trigger
     * @return true iff the click has the potential to start a drag
     */
    protected abstract boolean startMouseDrag(MouseEvent event);

    private void onSkeletonMousePressed(MouseEvent event) {
        this.consumeMouseUntilRelease = false;
        if (this.dragActive || event.getButton() != MouseButton.PRIMARY)
            return;

        PickResult result = event.getPickResult();
        Node clickedNode = result.getIntersectedNode();
        Point3D clickedPoint = result.getIntersectedPoint();
        if (clickedNode == null || clickedPoint == null)
            return;

        if (!startMouseDrag(event))
            return;

        this.draggedNode = clickedNode;
        this.startMousePointInScene = clickedNode.localToScene(clickedPoint);
        this.startMouseSceneX = event.getSceneX();
        this.startMouseSceneY = event.getSceneY();
        this.readyToBeginDrag = true;
        installSceneMouseFilters(clickedNode.getScene());
        event.consume();
    }

    /**
     * Called to process a drag update.
     * @param dragUpdate the drag update to process
     */
    protected abstract void onDragUpdate(DragUpdate dragUpdate);

    private void onSkeletonMouseDragged(MouseEvent event) {
        if (this.consumeMouseUntilRelease) {
            event.consume();
            return;
        }

        if (!this.dragActive && this.readyToBeginDrag) {
            event.consume();

            Node node = this.draggedNode;
            if (node == null)
                return;

            Point3D planeStartPos = getPlaneStartPosition(node, event);
            if (planeStartPos == null)
                return;

            if (!startDrag(node, getDragCamera(node), planeStartPos, this.startMousePointInScene))
                this.consumeMouseUntilRelease = true;

            this.startMousePointInScene = null;
        }

        if (!this.dragActive)
            return; // Drag was canceled, so don't process any further.

        event.consume();
        this.updateDrag(event);
    }

    /**
     * Called when the drag is stopped.
     * @param node the node which has stopped dragging
     * @param cancel if the drag was canceled
     */
    protected abstract void onDragStop(Node node, boolean cancel);

    /**
     * Called when a drag is cleaned up (due to completion, cancellation, or other).
     * Extra care should be taken within implementations for unexpected state such as null-pointer dereferences, as this function may be called after failure.
     * @param node the node which has had its drag canceled
     * @param cancel if the drag was canceled
     */
    protected abstract void onDragClean(Node node, boolean cancel);

    private void onSkeletonMouseReleased(MouseEvent event) {
        if (this.consumeMouseUntilRelease) {
            this.consumeMouseUntilRelease = false;
            removeSceneMouseFilters();
            event.consume();
            return;
        }

        if (!this.dragActive && !this.readyToBeginDrag)
            return;

        event.consume();
        stopDrag(false);
    }

    private static Box createCameraFacingDragPlane(Camera camera, Point3D planePos) {
        Box plane = new Box(DRAG_PLANE_SIZE, DRAG_PLANE_SIZE, 0);
        plane.setMaterial(DRAG_PLANE_MATERIAL);
        plane.setDepthTest(DepthTest.DISABLE);

        Transform cameraTransform = camera.getLocalToSceneTransform();
        plane.getTransforms().add(new Affine(
                cameraTransform.getMxx(), cameraTransform.getMxy(), cameraTransform.getMxz(), planePos.getX(),
                cameraTransform.getMyx(), cameraTransform.getMyy(), cameraTransform.getMyz(), planePos.getY(),
                cameraTransform.getMzx(), cameraTransform.getMzy(), cameraTransform.getMzz(), planePos.getZ()));
        return plane;
    }

    private static Camera getDragCamera(Node node) {
        if (node == null || node.getScene() == null)
            return null;

        Camera camera = node.getScene().getCamera();
        if (camera != null)
            return camera;

        SubScene subScene = Scene3DUtils.getSubScene(node.getScene());
        return subScene != null ? subScene.getCamera() : null;
    }

    /**
     * Describes a drag hit against the active drag plane.
     */
    @Getter
    public static class DragUpdate {
        private final DragGizmo gizmo;
        private final Point3D planePoint;
        private final Point3D scenePoint;
        private final Point3D planeOffset;
        private final double angle;
        private final double distance;
        private final double mouseSceneX;
        private final double mouseSceneY;
        private final double mouseSceneOffsetX;
        private final double mouseSceneOffsetY;

        private DragUpdate(DragGizmo gizmo, Point3D planePoint, MouseEvent event) {
            this.gizmo = gizmo;
            this.planePoint = planePoint;
            this.scenePoint = gizmo.dragPlane.localToScene(planePoint);
            this.planeOffset = planePoint.subtract(gizmo.startMousePointInPlane);
            this.angle = Math.atan2(planePoint.getY(), planePoint.getX());
            this.distance = Math.sqrt((planePoint.getX() * planePoint.getX()) + (planePoint.getY() * planePoint.getY()));
            this.mouseSceneX = event.getSceneX();
            this.mouseSceneY = event.getSceneY();
            this.mouseSceneOffsetX = Double.isFinite(gizmo.startMouseSceneX) ? this.mouseSceneX - gizmo.startMouseSceneX : 0;
            this.mouseSceneOffsetY = Double.isFinite(gizmo.startMouseSceneY) ? this.mouseSceneY - gizmo.startMouseSceneY : 0;
        }
    }
}
