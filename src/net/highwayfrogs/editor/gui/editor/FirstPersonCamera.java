package net.highwayfrogs.editor.gui.editor;

import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.utils.MathUtils;

/**
 * Specialized camera class to allow FPS-like movement within the map editor.
 * Created by AndyEder on 2/14/2019.
 */
public class FirstPersonCamera extends Parent {
    // Constants / variables used for frame delta calculations
    private static final double NANOSEC_IN_SECONDS = 1.0 / 1000000000.0;
    private final InputManager inputManager;
    private long lastFrameUpdate = 0;
    private double currFrameUpdate;
    private double frameDeltaInSecs = 0.0;
    private final double updatePeriod = 2.0;
    @Setter @Getter private boolean invertY;

    private double defaultMoveSpeed = 100.0;
    @Getter private final DoubleProperty camMoveSpeedProperty = new SimpleDoubleProperty(this.defaultMoveSpeed);

    private static final double CAM_MOUSE_SPEED = 0.2;
    @Getter private final DoubleProperty camMouseSpeedProperty = new SimpleDoubleProperty(CAM_MOUSE_SPEED);

    private static final double CAM_SPEED_DOWN_MULTIPLIER = 0.25;
    @Getter private final DoubleProperty camSpeedDownMultiplierProperty = new SimpleDoubleProperty(CAM_SPEED_DOWN_MULTIPLIER);

    private static final double CAM_SPEED_UP_MULTIPLIER = 4.0;
    @Getter private final DoubleProperty camSpeedUpMultiplierProperty = new SimpleDoubleProperty(CAM_SPEED_UP_MULTIPLIER);

    private static final double CAM_MIN_YAW_ANGLE_DEGREES = -360.0;
    private static final double CAM_MAX_YAW_ANGLE_DEGREES = 360.0;
    private static final double CAM_MIN_PITCH_ANGLE_DEGREES = -90.0;
    private static final double CAM_MAX_PITCH_ANGLE_DEGREES = 90.0;
    private static final double CAM_FOV_CHANGE_DEGREES = 2.5;
    private static final double CAM_MIN_FOV_ANGLE_DEGREES = CAM_FOV_CHANGE_DEGREES;
    private static final double CAM_MAX_FOV_ANGLE_DEGREES = 120.0;


    // Camera processing thread
    AnimationTimer camUpdateThread;

    // For scene graph (node) transformation, processing and management
    private final Group rootGrp = new Group();
    private final Affine affineXform = new Affine();
    private final Rotate rotateYaw = new Rotate(0.0, Rotate.Y_AXIS);
    private final Rotate rotatePitch = new Rotate(0.0, Rotate.X_AXIS);
    private final Rotate rotateRoll = new Rotate(0.0, Rotate.Z_AXIS);
    private final Translate translate = new Translate(0.0, 0.0, 0.0);

    // The actual perspective camera (as read only object and as public property)
    private final ReadOnlyObjectWrapper<PerspectiveCamera> camera = new ReadOnlyObjectWrapper<>(this, "camera", new PerspectiveCamera(true));

    public final PerspectiveCamera getCamera() {
        return camera.get();
    }

    @Getter private final DoubleProperty camPosXProperty = new SimpleDoubleProperty();
    @Getter private final DoubleProperty camPosYProperty = new SimpleDoubleProperty();
    @Getter private final DoubleProperty camPosZProperty = new SimpleDoubleProperty();

    @Getter private final DoubleProperty camYawProperty = new SimpleDoubleProperty();
    @Getter private final DoubleProperty camPitchProperty = new SimpleDoubleProperty();
    @Getter private final DoubleProperty camRollProperty = new SimpleDoubleProperty();

    @Getter private final BooleanProperty camYInvertProperty = new SimpleBooleanProperty();

    //>>

    /**
     * Constructor for the FirstPersonCamera.
     */
    public FirstPersonCamera(InputManager inputManager) {
        this.inputManager = inputManager;
        setupAndInitialise();
    }

    /**
     * Setup and initialise the self-contained camera.
     */
    private void setupAndInitialise() {
        // Add this camera instance as a child of the root
        getChildren().add(this.rootGrp);

        // Bind properties
        this.affineXform.txProperty().bindBidirectional(this.camPosXProperty);
        this.affineXform.tyProperty().bindBidirectional(this.camPosYProperty);
        this.affineXform.tzProperty().bindBidirectional(this.camPosZProperty);

        this.rotateYaw.angleProperty().bindBidirectional(this.camYawProperty);
        this.rotatePitch.angleProperty().bindBidirectional(this.camPitchProperty);
        this.rotateRoll.angleProperty().bindBidirectional(this.camRollProperty);

        // Add out affine transformation (we manually update this per frame to produce the desired camera behaviour)
        getTransforms().add(this.affineXform);

        // Ensure the camera is set up with default values and kick-off the camera update thread
        setupAndInitialiseCamera();
        setupCameraUpdateThread();
    }

    /**
     * Set up the camera and provide sensible initial / default values.
     */
    private void setupAndInitialiseCamera() {
        this.rootGrp.getChildren().add(getCamera());
        this.inputManager.setFinalMouseHandler(this::updateCameraViewFromMouseMovement);
        this.inputManager.setFinalScrollHandler(this::updateCameraFovFromScroll);
    }

    /**
     * Set up the camera update thread that will be responsible for handling the camera updates.
     */
    private void setupCameraUpdateThread() {
        this.camUpdateThread = new AnimationTimer() {
            @Override
            public void handle(long timestamp) {
                updateCamera(timestamp);
            }
        };
    }

    /**
     * Start processing user controls for the camera.
     */
    public void startThreadProcessing() {
        if (this.camUpdateThread != null) {
            this.lastFrameUpdate = 0;
            this.camUpdateThread.start();
        }
    }

    /**
     * Stop processing user controls for the camera.
     */
    public void stopThreadProcessing() {
        if (this.camUpdateThread != null)
            this.camUpdateThread.stop();
    }

    /**
     * Entry point into the [per frame] main camera processing functionality.
     * @param timestamp The system time stamp in nanoseconds.
     */
    private void updateCamera(long timestamp) {
        // If this is the first update then skip a single frame, so we can initialise our last update variable
        if (this.lastFrameUpdate == 0) {
            // Grab the current system timestamp (in nanoseconds), then early out
            this.lastFrameUpdate = System.nanoTime();
            return;
        }

        // Calculate frame time (delta) in seconds
        this.frameDeltaInSecs = (timestamp - this.lastFrameUpdate) * NANOSEC_IN_SECONDS;
        this.lastFrameUpdate = timestamp;

        // This next block is only really needed for test / debugging right now but is useful to have around!
        this.currFrameUpdate += this.frameDeltaInSecs;
        if (this.currFrameUpdate > this.updatePeriod)
            this.currFrameUpdate -= this.updatePeriod;

        // Handle the camera controls (user input)
        updateCameraControls();
    }

    /**
     * Update camera movement based on user inputs.
     */
    private void updateCameraControls() {
        // Check for forwards or backwards camera movement (along 'view' vector)
        boolean camMoveForward = this.inputManager.isKeyPressed(KeyCode.W);
        boolean camMoveBackward = this.inputManager.isKeyPressed(KeyCode.S);
        if (camMoveForward && !camMoveBackward)
            moveCameraForward();
        if (camMoveBackward && !camMoveForward)
            moveCameraBackward();

        // Check for up or down camera movement (along 'up' vector)
        boolean camMoveUp = this.inputManager.isKeyPressed(KeyCode.Q) || this.inputManager.isKeyPressed(KeyCode.PAGE_UP);
        boolean camMoveDown = this.inputManager.isKeyPressed(KeyCode.E) || this.inputManager.isKeyPressed(KeyCode.PAGE_DOWN);
        if (camMoveUp && !camMoveDown)
            moveCameraUp();
        if (camMoveDown && !camMoveUp)
            moveCameraDown();

        // Check for strafe left or right camera movement (along 'right' vector)
        boolean camStrafeLeft = this.inputManager.isKeyPressed(KeyCode.A);
        boolean camStrafeRight = this.inputManager.isKeyPressed(KeyCode.D);
        if (camStrafeLeft && !camStrafeRight)
            strafeCameraLeft();
        if (camStrafeRight && !camStrafeLeft)
            strafeCameraRight();
    }

    /**
     * Assign (setup) the control event handlers on the supplied scene object.
     * @param scene The subscene to receive and process the keyboard and mouse events, etc.
     */
    public void assignSceneControls(Stage stage, Scene scene) {
        this.inputManager.assignSceneControls(stage, scene);
    }

    /**
     * Assign (setup) the control event handlers on the supplied scene object.
     * @param scene The subscene to receive and process the keyboard and mouse events, etc.
     */
    public void removeSceneControls(Stage stage, Scene scene) {
        this.inputManager.removeSceneControls(stage, scene);
    }

    /**
     * Function to process mouse input events.
     */
    private void updateCameraViewFromMouseMovement(InputManager manager, MouseEvent evt, double mouseDeltaX, double mouseDeltaY) {
        if (!evt.getEventType().equals(MouseEvent.MOUSE_DRAGGED) || !evt.isPrimaryButtonDown() || manager.isKeyPressed(KeyCode.SHIFT))
            return;

        // Check mouse y-inversion state
        double yInvert = (this.camYInvertProperty.get()) ? -1 : 1;

        // Set translation component (direct from camera's transformation matrix)
        this.translate.setX(getPos().getX());
        this.translate.setY(getPos().getY());
        this.translate.setZ(getPos().getZ());

        // IMPORTANT: reset affine transform to identity each time!
        this.affineXform.setToIdentity();

        // Calculate yaw, pitch angles
        if (this.invertY && (this.rotatePitch.getAngle() <= CAM_MIN_PITCH_ANGLE_DEGREES || this.rotatePitch.getAngle() >= CAM_MAX_PITCH_ANGLE_DEGREES))
            mouseDeltaX *= -1; // Invert mouse X when upside down.

        setYaw(this.rotateYaw.getAngle() + (mouseDeltaX * this.camMouseSpeedProperty.get()));
        setPitch(this.rotatePitch.getAngle() - (yInvert * mouseDeltaY * this.camMouseSpeedProperty.get()));
        // this.rotatePitch.setAngle(this.rotatePitch.getAngle() - (yInvert * mouseDeltaY * this.camMouseSpeedProperty.get()));

        // Dynamically generate the affine transform from the concatenated translation and rotation components
        this.affineXform.prepend(this.translate.createConcatenation(this.rotateYaw.createConcatenation(this.rotatePitch)));
    }

    /**
     * Function to process scroll events.
     */
    private void updateCameraFovFromScroll(InputManager manager, ScrollEvent event, boolean isTrackpadScroll) {
        if (isTrackpadScroll)
            return; // Changing the FoV with a laptop trackpad looks very bad.

        double newFov = getCamera().fieldOfViewProperty().doubleValue();
        if (event.getDeltaY() > 0) { // Zoom In (Mouse Wheel Forward -> DeltaY is positive)
            newFov -= CAM_FOV_CHANGE_DEGREES;
        } else if (event.getDeltaY() < 0) { // Zoom Out (Mouse Wheel Backwards -> DeltaY is negative)
            newFov += CAM_FOV_CHANGE_DEGREES;
        } else {
            return;
        }

        getCamera().fieldOfViewProperty().set(MathUtils.clamp(newFov, CAM_MIN_FOV_ANGLE_DEGREES, CAM_MAX_FOV_ANGLE_DEGREES));
    }

    /**
     * Moves the camera forwards along the current view vector (+ve z).
     */
    private void moveCameraForward() {
        final double moveDelta = this.frameDeltaInSecs * getSpeedModifier(this.camMoveSpeedProperty.get());
        this.affineXform.setTx(getPos().getX() + (moveDelta * getCamNRow().getX()));
        this.affineXform.setTy(getPos().getY() + (moveDelta * getCamNRow().getY()));
        this.affineXform.setTz(getPos().getZ() + (moveDelta * getCamNRow().getZ()));
    }

    /**
     * Moves the camera backwards along the current 'view' vector (-ve z).
     */
    private void moveCameraBackward() {
        final double moveDelta = this.frameDeltaInSecs * getSpeedModifier(this.camMoveSpeedProperty.get());
        this.affineXform.setTx(getPos().getX() + (moveDelta * -getCamNRow().getX()));
        this.affineXform.setTy(getPos().getY() + (moveDelta * -getCamNRow().getY()));
        this.affineXform.setTz(getPos().getZ() + (moveDelta * -getCamNRow().getZ()));
    }

    /**
     * Moves the camera up along the current 'up' vector (-ve y).
     */
    private void moveCameraUp() {
        final double moveDelta = this.frameDeltaInSecs * getSpeedModifier(this.camMoveSpeedProperty.get());
        this.affineXform.setTx(getPos().getX() + (moveDelta * -getCamVRow().getX()));
        this.affineXform.setTy(getPos().getY() + (moveDelta * -getCamVRow().getY()));
        this.affineXform.setTz(getPos().getZ() + (moveDelta * -getCamVRow().getZ()));
    }

    /**
     * Moves the camera down along the current 'up' vector (+ve y).
     */
    private void moveCameraDown() {
        final double moveDelta = this.frameDeltaInSecs * getSpeedModifier(this.camMoveSpeedProperty.get());
        this.affineXform.setTx(getPos().getX() + (moveDelta * getCamVRow().getX()));
        this.affineXform.setTy(getPos().getY() + (moveDelta * getCamVRow().getY()));
        this.affineXform.setTz(getPos().getZ() + (moveDelta * getCamVRow().getZ()));
    }

    /**
     * Strafes the camera left along the current 'right' vector (-ve x).
     */
    private void strafeCameraLeft() {
        final double moveDelta = this.frameDeltaInSecs * getSpeedModifier(this.camMoveSpeedProperty.get());
        this.affineXform.setTx(getPos().getX() + (moveDelta * -getCamURow().getX()));
        this.affineXform.setTy(getPos().getY() + (moveDelta * -getCamURow().getY()));
        this.affineXform.setTz(getPos().getZ() + (moveDelta * -getCamURow().getZ()));
    }

    /**
     * Strafes the camera right along the current 'right' vector (+ve x).
     */
    private void strafeCameraRight() {
        final double moveDelta = this.frameDeltaInSecs * getSpeedModifier(this.camMoveSpeedProperty.get());
        this.affineXform.setTx(getPos().getX() + (moveDelta * getCamURow().getX()));
        this.affineXform.setTy(getPos().getY() + (moveDelta * getCamURow().getY()));
        this.affineXform.setTz(getPos().getZ() + (moveDelta * getCamURow().getZ()));
    }

    /**
     * Moves the camera a specified distance along its look vector.
     * @param moveDirection The direction to move along the camera's look vector.
     */
    public void moveCameraAlongLookVector(double moveDirection) {
        if (moveDirection > 0.0) {
            moveCameraForward();
        } else {
            moveCameraBackward();
        }
    }

    /**
     * Rotates the camera to look at a specified position in 3D world space.
     * @param xTarget The x-position to look at.
     * @param yTarget The y-position to look at.
     * @param zTarget The z-position to look at.
     */
    public void setCameraLookAt(double xTarget, double yTarget, double zTarget) {
        setCameraLookAt(new Point3D(xTarget, yTarget, zTarget));
    }

    /**
     * Rotates the camera to look at a specified position in 3D world space.
     * @param target The position to look at.
     */
    public void setCameraLookAt(Point3D target) {
        // Calculate the camera's reference frame in terms of look, right and up vectors
        Point3D deltaPos = target.subtract(getPos()); // (eye - target) = right-handed matrix... Don't we need to convert this to a left-handed matrix?
        Point3D zVec = deltaPos.normalize();
        Point3D xVec = new Point3D(0, 1, 0).normalize().crossProduct(zVec).normalize();
        Point3D yVec = zVec.crossProduct(xVec).normalize();

        // Update the affine transformation to point the camera towards the target point
        this.affineXform.setToTransform(xVec.getX(), yVec.getX(), zVec.getX(), getPos().getX(),
                xVec.getY(), yVec.getY(), zVec.getY(), getPos().getY(),
                xVec.getZ(), yVec.getZ(), zVec.getZ(), getPos().getZ());

        // Extract camera orientation angles from the affine transformation into the camera's internal yaw, pitch, roll (not currently used).
        double yaw, pitch;
        if (this.invertY) {
            yaw = Math.toDegrees(Math.atan2(this.affineXform.getMzx(), -this.affineXform.getMzz()));
            pitch = Math.toDegrees(Math.atan2(Math.sqrt(deltaPos.getX() * deltaPos.getX() + deltaPos.getZ() * deltaPos.getZ()), -deltaPos.getY()) + (Math.PI / 2));
        } else {
            yaw = Math.toDegrees(Math.atan2(-this.affineXform.getMzx(), this.affineXform.getMzz()));
            pitch = Math.toDegrees(-Math.atan2(zVec.getY(), Math.sqrt(zVec.getX() * zVec.getX() + zVec.getZ() * zVec.getZ())));
        }

        setPitchAndYaw(pitch, yaw);
    }

    /**
     * Callback for obtaining the camera position.
     * (Returns associated column vector from the supplied transformation object).
     */
    private final Callback<Transform, Point3D> CAM_POS_COL = (xform) -> new Point3D(xform.getTx(), xform.getTy(), xform.getTz());

    /**
     * Get the camera's position direct from its current transformation matrix.
     * @return Point3D.
     */
    public final Point3D getPos() {
        return CAM_POS_COL.call(getLocalToSceneTransform());
    }

    /**
     * Set the camera's position by directly manipulating the affine transform translation component.
     * @param xPos The desired x-coordinate.
     * @param yPos The desired y-coordinate.
     * @param zPos The desired z-coordinate.
     */
    public void setPos(double xPos, double yPos, double zPos) {
        this.affineXform.setTx(xPos);
        this.affineXform.setTy(yPos);
        this.affineXform.setTz(zPos);
    }

    /**
     * Applies a new pitch & yaw value.
     * @param pitch the pitch value to apply
     * @param yaw the yaw value to apply
     */
    public void setPitchAndYaw(double pitch, double yaw) {
        // Set translation component (direct from camera's transformation matrix)
        this.translate.setX(getPos().getX());
        this.translate.setY(getPos().getY());
        this.translate.setZ(getPos().getZ());

        // Reset affine transform to identity each time!
        this.affineXform.setToIdentity();

        setPitch(pitch);
        setYaw(yaw);

        // Generate the affine transform from the concatenated translation and rotation components
        this.affineXform.prepend(this.translate.createConcatenation(this.rotateYaw.createConcatenation(this.rotatePitch)));
    }

    /**
     * Set the camera's yaw rotation by directly manipulating the rotation components.
     * @param angle The desired yaw.
     */
    public void setYaw(double angle) {
        double newYaw = MathUtils.clampAngleInDegrees(angle);
        this.rotateYaw.setAngle(MathUtils.clamp(newYaw, CAM_MIN_YAW_ANGLE_DEGREES, CAM_MAX_YAW_ANGLE_DEGREES));
        // TODO: figure out how to update affine transform here without performing forced update
    }

    /**
     * Set the camera's pitch rotation by directly manipulating the rotation components.
     * @param angle The desired pitch.
     */
    public void setPitch(double angle) {
        double newPitch;
        if (this.invertY) {
            newPitch = (((angle % 360F) + 540F) % 360F) - 180F;
            if (newPitch > CAM_MIN_PITCH_ANGLE_DEGREES && newPitch < CAM_MAX_PITCH_ANGLE_DEGREES) {
                if (Math.abs(newPitch - CAM_MIN_PITCH_ANGLE_DEGREES) <= Math.abs(newPitch - CAM_MAX_PITCH_ANGLE_DEGREES)) {
                    newPitch = CAM_MIN_PITCH_ANGLE_DEGREES;
                } else {
                    newPitch = CAM_MAX_PITCH_ANGLE_DEGREES;
                }
            }
        } else {
            newPitch = MathUtils.clamp(angle, CAM_MIN_PITCH_ANGLE_DEGREES, CAM_MAX_PITCH_ANGLE_DEGREES);
        }

        this.rotatePitch.setAngle(newPitch);
        // TODO: figure out how to update affine transform here without performing forced update
    }

    /**
     * Set the camera's roll rotation by directly manipulating the rotation components.
     * @param angle The desired roll.
     */
    public void setRoll(double angle) {
        this.rotateRoll.setAngle(angle);
        // TODO: figure out how to update affine transform here without performing forced update
    }

    /**
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public double getSpeedModifier(double defaultValue) {
        boolean isControlDown = this.inputManager.isKeyPressed(KeyCode.CONTROL);
        boolean isAltDown = this.inputManager.isKeyPressed(KeyCode.ALT);
        return getSpeedModifier(isAltDown, isControlDown, defaultValue);
    }

    /**
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public double getSpeedModifier(boolean isAltDown, boolean isCtrlDown, double defaultValue) {
        double multiplier = 1;

        if (isCtrlDown) {
            multiplier = this.camSpeedDownMultiplierProperty.get();
        } else if (isAltDown) { // This beeps on Java 8, but the beeping is fixed in JavaFX 11+
            multiplier = this.camSpeedUpMultiplierProperty.get();
        }

        return defaultValue * multiplier;
    }

    /**
     * Reset the camera's default property values for move speed, mouse speed, etc.
     */
    public void resetDefaultCamMoveSpeed() {
        this.camMoveSpeedProperty.set(this.defaultMoveSpeed);
    }

    public void resetDefaultCamMouseSpeed() {
        this.camMouseSpeedProperty.set(CAM_MOUSE_SPEED);
    }

    public void resetDefaultCamSpeedDownMultiplier() {
        this.camSpeedDownMultiplierProperty.set(CAM_SPEED_DOWN_MULTIPLIER);
    }

    public void resetDefaultCamSpeedUpMultiplier() {
        this.camSpeedUpMultiplierProperty.set(CAM_SPEED_UP_MULTIPLIER);
    }

    /**
     * Callbacks for obtaining the camera view (look), up and right vectors.
     * (Returns associated row or column vector from the supplied transformation object).
     */
    private final Callback<Transform, Point3D> CAM_VIEW_COL = (xform) -> new Point3D(xform.getMzx(), xform.getMzy(), xform.getMzz());
    private final Callback<Transform, Point3D> CAM_UP_COL = (xform) -> new Point3D(xform.getMyx(), xform.getMyy(), xform.getMyz());
    private final Callback<Transform, Point3D> CAM_RIGHT_COL = (xform) -> new Point3D(xform.getMxx(), xform.getMxy(), xform.getMxz());

    private final Callback<Transform, Point3D> CAM_N_ROW = (xform) -> new Point3D(xform.getMxz(), xform.getMyz(), xform.getMzz());
    private final Callback<Transform, Point3D> CAM_V_ROW = (xform) -> new Point3D(xform.getMxy(), xform.getMyy(), xform.getMzy());
    private final Callback<Transform, Point3D> CAM_U_ROW = (xform) -> new Point3D(xform.getMxx(), xform.getMyx(), xform.getMzx());

    /**
     * Get the camera's view, up and right vectors direct from its current transformation matrix.
     * @return Point3D.
     */
    private Point3D getCamViewCol() {
        return CAM_VIEW_COL.call(getLocalToSceneTransform());
    }

    private Point3D getCamUpCol() {
        return CAM_UP_COL.call(getLocalToSceneTransform());
    }

    private Point3D getCamRightCol() {
        return CAM_RIGHT_COL.call(getLocalToSceneTransform());
    }

    private Point3D getCamNRow() {
        return CAM_N_ROW.call(getLocalToSceneTransform());
    }

    private Point3D getCamVRow() {
        return CAM_V_ROW.call(getLocalToSceneTransform());
    }

    private Point3D getCamURow() {
        return CAM_U_ROW.call(getLocalToSceneTransform());
    }

    /**
     * Sets the new default camera movement speed.
     * @param newMoveSpeed The new camera movement speed.
     */
    public void setDefaultMoveSpeed(double newMoveSpeed) {
        if (Math.abs(this.camMoveSpeedProperty.get() - this.defaultMoveSpeed) <= .001)
            this.camMoveSpeedProperty.set(newMoveSpeed);
        this.defaultMoveSpeed = newMoveSpeed;
    }
}