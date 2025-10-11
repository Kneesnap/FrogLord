package net.highwayfrogs.editor.gui.components.mesh;

import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.gui.InputManager;

/**
 * Handles camera rotation.
 * Created by Kneesnap on 10/10/2025.
 */
@Getter
public class RotationCamera {
    private final InputManager inputManager;
    private final Rotate rotationX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotationY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotationZ = new Rotate(0, Rotate.Z_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    @Setter private double movementFactor = .25;

    public RotationCamera(InputManager inputManager) {
        this.inputManager = inputManager;
        if (inputManager != null) {
            inputManager.setFinalMouseHandler(this::updateCameraViewFromMouseMovement);
            inputManager.setFinalScrollHandler(this::updateCameraDistanceFromMouseScroll);
        }
    }

    /**
     * Function to process mouse input events.
     */
    private void updateCameraViewFromMouseMovement(InputManager manager, MouseEvent evt, double mouseDeltaX, double mouseDeltaY) {
        if (!evt.getEventType().equals(MouseEvent.MOUSE_DRAGGED) || manager.isKeyPressed(KeyCode.SHIFT))
            return;

        if (evt.isPrimaryButtonDown()) {
            this.rotationX.setAngle(this.rotationX.getAngle() + (mouseDeltaY * .25));
            this.rotationY.setAngle(this.rotationY.getAngle() + (mouseDeltaX * .25));
        } else if (evt.isMiddleButtonDown()) {
            this.camera.setTranslateX(this.camera.getTranslateX() - (mouseDeltaX * this.movementFactor)); // Move the camera.
            this.camera.setTranslateY(this.camera.getTranslateY() - (mouseDeltaY * this.movementFactor));
        }
    }

    private void updateCameraDistanceFromMouseScroll(InputManager manager, ScrollEvent event, boolean isTrackpadScroll) {
        if (!isTrackpadScroll)
            this.camera.setTranslateZ(this.camera.getTranslateZ() + (event.getDeltaY() * this.movementFactor));
    }

    /**
     * Applies the rotations to the given root node.
     * @param node the node to apply the rotations to.
     */
    public void addRotationsToNode(Node node) {
        if (node == null)
            throw new NullPointerException("node");

        node.getTransforms().addAll(this.rotationX, this.rotationY, this.rotationZ);
    }

    /**
     * Applies the rotations to the given node.
     * @param node the node to apply the rotations to.
     */
    public void applyRotation(Node node) {
        Rotate lightRotateX = new Rotate(0, Rotate.X_AXIS); // Up, Down,
        Rotate lightRotateY = new Rotate(0, Rotate.Y_AXIS); // Left, Right
        Rotate lightRotateZ = new Rotate(0, Rotate.Z_AXIS); // In, Out
        lightRotateX.angleProperty().bind(this.rotationX.angleProperty());
        lightRotateY.angleProperty().bind(this.rotationY.angleProperty());
        lightRotateZ.angleProperty().bind(this.rotationZ.angleProperty());

        double translateX = node.getTranslateX();
        double translateY = node.getTranslateY();
        double translateZ = node.getTranslateZ();

        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Translate))
                continue;

            Translate translate = (Translate) transform;
            translateX += translate.getX();
            translateY += translate.getY();
            translateZ += translate.getZ();
        }

        lightRotateX.setPivotX(-translateX); // Not sure if this should be here.
        lightRotateX.setPivotY(-translateY);
        lightRotateX.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateY.setPivotX(-translateX); // <Left, Right>
        lightRotateY.setPivotY(-translateY); // Not sure if this should be here.
        lightRotateY.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateZ.setPivotX(-translateX); // <Left, Right>
        lightRotateZ.setPivotY(-translateY); // <Up, Down>
        lightRotateZ.setPivotZ(-translateZ); // Not sure if this should be here.
        node.getTransforms().addAll(lightRotateX, lightRotateY, lightRotateZ);
    }

    /**
     * Update rotation for a particular node.
     * @param node The node to update rotation for.
     */
    public void updateRotation(Node node) {
        Rotate lightRotateX = null;
        Rotate lightRotateY = null;
        Rotate lightRotateZ = null;
        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Rotate))
                continue;

            Rotate temp = (Rotate) transform;
            if (temp.getAxis() == Rotate.X_AXIS) {
                lightRotateX = temp;
            } else if (temp.getAxis() == Rotate.Y_AXIS) {
                lightRotateY = temp;
            } else if (temp.getAxis() == Rotate.Z_AXIS) {
                lightRotateZ = temp;
            }
        }

        if (lightRotateX == null || lightRotateY == null || lightRotateZ == null)
            throw new IllegalStateException("Failed to update rotation.");

        // Get translation.
        double translateX = node.getTranslateX();
        double translateY = node.getTranslateY();
        double translateZ = node.getTranslateZ();

        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Translate))
                continue;

            Translate translate = (Translate) transform;
            translateX += translate.getX();
            translateY += translate.getY();
            translateZ += translate.getZ();
        }

        // Update pivots.
        lightRotateX.setPivotY(-translateY);
        lightRotateX.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateY.setPivotX(-translateX); // <Left, Right>
        lightRotateY.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateZ.setPivotX(-translateX); // <Left, Right>
        lightRotateZ.setPivotY(-translateY); // <Up, Down>
    }
}
