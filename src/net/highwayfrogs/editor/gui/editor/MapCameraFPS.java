package net.highwayfrogs.editor.gui.editor;

import javafx.animation.AnimationTimer;
import javafx.beans.property.*;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.input.GestureEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.util.Callback;
import lombok.Getter;
import net.highwayfrogs.editor.MathUtils;

/**
 * Specialized camera class to allow FPS-like movement within the map editor.
 * Created by AndyEder on 2/14/2019.
 */

// TODO:
//  - Finish writing this camera component!
//  - Implement roll (is it really needed...?)

public class MapCameraFPS extends Parent
{
    // Constants / variables used for frame delta calculations
    private final double NANOSEC_IN_SECONDS = 1.0 / 1000000000.0;
    private long lastFrameUpdate = 0;
    private long totalFrameCount = 0;
    private double currFrameUpdate;
    private double frameDeltaInSecs = 0.0;
    private final double updatePeriod = 2.0;

    // For handling user input controls, camera movement and dynamics
    private boolean camMoveForward, camMoveBackward, camMoveUp, camMoveDown, camStrafeLeft, camStrafeRight;

    private final double CAM_MOVE_SPEED = 100.0;
    @Getter private DoubleProperty camMoveSpeedProperty = new SimpleDoubleProperty(CAM_MOVE_SPEED);

    private double mouseX, mouseY;
    private double mouseOldX, mouseOldY;
    private double mouseDeltaX, mouseDeltaY;

    private boolean isControlDown, isAltDown;

    private final double CAM_MOUSE_SPEED = 0.2;
    @Getter private DoubleProperty camMouseSpeedProperty = new SimpleDoubleProperty(CAM_MOUSE_SPEED);

    private final double CAM_SPEED_DOWN_MULTIPLIER = 0.1;
    @Getter private DoubleProperty camSpeedDownMultiplierProperty = new SimpleDoubleProperty(CAM_SPEED_DOWN_MULTIPLIER);

    private final double CAM_SPEED_UP_MULTIPLIER = 2.0;
    @Getter private DoubleProperty camSpeedUpMultiplierProperty = new SimpleDoubleProperty(CAM_SPEED_UP_MULTIPLIER);

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
    public ReadOnlyObjectProperty cameraProperty() {
        return camera.getReadOnlyProperty();
    }

    @Getter private DoubleProperty camPosXProperty = new SimpleDoubleProperty();
    @Getter private DoubleProperty camPosYProperty = new SimpleDoubleProperty();
    @Getter private DoubleProperty camPosZProperty = new SimpleDoubleProperty();

    //>>

    /**
     * Constructor for the MapCameraFPS.
     */
    public MapCameraFPS()
    {
        setupAndInitialise();
    }

    /**
     * Setup and initialise the self-contained camera.
     */
    private void setupAndInitialise()
    {
        // Add this camera instance as a child of the root
        getChildren().add(rootGrp);

        // Bind properties
        affineXform.txProperty().bindBidirectional(camPosXProperty);
        affineXform.tyProperty().bindBidirectional(camPosYProperty);
        affineXform.tzProperty().bindBidirectional(camPosZProperty);

        // Add out affine transformation (we manually update this per frame to produce the desired camera behaviour)
        getTransforms().add(affineXform);

        // Ensure the camera is setup with default values and kick-off the camera update thread
        setupAndInitialiseCamera();
        setupCameraUpdateThread();
    }

    /**
     * Setup camera and provide sensible initial / default values.
     */
    private void setupAndInitialiseCamera()
    {
        rootGrp.getChildren().add(getCamera());
    }

    /**
     * Setup the camera update thread that will be responsible for handling the camera updates.
     */
    private void setupCameraUpdateThread()
    {
        camUpdateThread = new AnimationTimer()
        {
            @Override
            public void handle(long timestamp)
            {
                updateCamera(timestamp);
            }
        };
    }

    public void startThreadProcessing()
    {
        if (camUpdateThread != null)
        {
            lastFrameUpdate = 0;
            camUpdateThread.start();
        }
    }

    public void stopThreadProcessing()
    {
        if (camUpdateThread != null)
        {
            camUpdateThread.stop();
        }
    }

    /**
     * Entry point into the [per frame] main camera processing functionality.
     * @param timestamp the system time stamp in nanoseconds.
     */
    private void updateCamera(long timestamp)
    {
        // If this is the first update then skip a single frame so we can initialise our last update variable
        if (lastFrameUpdate == 0)
        {
            // Grab the current system timestamp (in nanoseconds), then early out
            lastFrameUpdate = System.nanoTime();
            totalFrameCount = 0;
            return;
        }

        // Calculate frame time (delta) in seconds
        frameDeltaInSecs = (timestamp - lastFrameUpdate) * NANOSEC_IN_SECONDS;
        lastFrameUpdate = timestamp;

        // This next block is only really needed for test / debugging right now but is useful to have around!
        currFrameUpdate += frameDeltaInSecs;
        if (currFrameUpdate > updatePeriod)
        {
            currFrameUpdate -= updatePeriod;
            //System.out.println("totalFrameCount: " + totalFrameCount);
            //System.out.println("rotateYaw: " + rotateYaw.getAngle());
            //System.out.println("rotatePitch: " + rotatePitch.getAngle());
        }

        // Handle the camera controls (user input)
        updateCameraControls();

        // Update the frame counter (potentially a useful metric to have around)
        ++totalFrameCount;
    }

    /**
     * Update camera movement based on user inputs.
     */
    private void updateCameraControls()
    {
        // Check for forwards or backwards camera movement (along 'view' vector)
        if (camMoveForward && !camMoveBackward)
        {
            moveCameraForward();
        }
        if (camMoveBackward && !camMoveForward)
        {
            moveCameraBackward();
        }

        // Check for up or down camera movement (along 'up' vector)
        if (camMoveUp && !camMoveDown)
        {
            moveCameraUp();
        }
        if (camMoveDown && !camMoveUp)
        {
            moveCameraDown();
        }

        // Check for strafe left or right camera movement (along 'right' vector)
        if (camStrafeLeft && !camStrafeRight)
        {
            strafeCameraLeft();
        }
        if (camStrafeRight && !camStrafeLeft)
        {
            strafeCameraRight();
        }
    }

    /**
     * Assign (setup) the control event handlers on the supplied subscene object.
     * @param subScene the subscene to receive and process the keyboard and mouse events, etc.
     */
    public void assignSubSceneControls(SubScene subScene)
    {
        sceneProperty().addListener(listener ->
        {
            if (getScene() != null)
            {
                getScene().addEventHandler(KeyEvent.ANY, evt ->
                {
                    if (evt.getEventType() == KeyEvent.KEY_PRESSED)
                    {
                        switch (evt.getCode())
                        {
                            // Modifiers
                            case CONTROL:
                                isControlDown = true;
                                break;
                            case ALT:
                                isAltDown = true;
                                break;

                            // Forwards + backwards
                            case W:
                            case UP:
                                camMoveForward = true;
                                break;
                            case S:
                            case DOWN:
                                camMoveBackward = true;
                                break;

                            // Strafe left + right
                            case A:
                            case LEFT:
                                camStrafeLeft = true;
                                break;
                            case D:
                            case RIGHT:
                                camStrafeRight = true;
                                break;

                            // Up + down
                            case Q:
                            case PAGE_UP:
                                camMoveUp = true;
                                break;
                            case E:
                            case PAGE_DOWN:
                                camMoveDown = true;
                                break;
                        }
                    }
                    else if (evt.getEventType() == KeyEvent.KEY_RELEASED)
                    {
                        switch (evt.getCode())
                        {
                            // Modifiers
                            case CONTROL:
                                isControlDown = false;
                                break;
                            case ALT:
                                isAltDown = false;
                                break;

                            // Forwards + backwards
                            case W:
                            case UP:
                                camMoveForward = false;
                                break;
                            case S:
                            case DOWN:
                                camMoveBackward = false;
                                break;

                            // Strafe left + right
                            case A:
                            case LEFT:
                                camStrafeLeft = false;
                                break;
                            case D:
                            case RIGHT:
                                camStrafeRight = false;
                                break;

                            // Up + down
                            case Q:
                            case PAGE_UP:
                                camMoveUp = false;
                                break;
                            case E:
                            case PAGE_DOWN:
                                camMoveDown = false;
                                break;
                        }
                    }

                    evt.consume();
                });
            }
        });

        subScene.addEventHandler(MouseEvent.ANY, evt ->
        {
            if (evt.getEventType().equals(MouseEvent.MOUSE_PRESSED))
            {
                mouseX = mouseOldX = evt.getSceneX();
                mouseY = mouseOldY = evt.getSceneY();
            }
            else if (evt.getEventType().equals(MouseEvent.MOUSE_DRAGGED))
            {
                mouseOldX = mouseX;
                mouseOldY = mouseY;
                mouseX = evt.getSceneX();
                mouseY = evt.getSceneY();
                mouseDeltaX = (mouseX - mouseOldX);
                mouseDeltaY = (mouseY - mouseOldY);

                if (evt.isPrimaryButtonDown())
                {
                    // Set translation component (direct from camera's transformation matrix)
                    translate.setX(getPos().getX());
                    translate.setY(getPos().getY());
                    translate.setZ(getPos().getZ());

                    // IMPORTANT: reset affine transform to identity each time!
                    affineXform.setToIdentity();

                    // Clamp
                    rotateYaw.setAngle(MathUtils.clamp(((rotateYaw.getAngle() + (mouseDeltaX * getSpeedModifier(evt, camMouseSpeedProperty))) % 360 + 540) % 360 - 180, -360, 360));
                    rotatePitch.setAngle(MathUtils.clamp(rotatePitch.getAngle() - (mouseDeltaY * getSpeedModifier(evt, camMouseSpeedProperty)), -75, 75));

                    // Dynamically generate the affine transform from the concatenated translation and rotation components
                    affineXform.prepend(translate.createConcatenation(rotateYaw.createConcatenation(rotatePitch)));
                }
                else if (evt.isSecondaryButtonDown())
                {
                    // Further processing needed here?
                }
                else if (evt.isMiddleButtonDown())
                {
                    // Further processing needed here?
                }
            }
        });
    }

    /**
     * Assign (setup) the control event handlers on the supplied scene object.
     * @param scene the subscene to receive and process the keyboard and mouse events, etc.
     */
    public void assignSceneControls(Scene scene)
    {
        scene.addEventHandler(KeyEvent.ANY, evt ->
        {
            if (evt.getEventType() == KeyEvent.KEY_PRESSED)
            {
                switch (evt.getCode())
                {
                    // Modifiers
                    case CONTROL:
                        isControlDown = true;
                        break;
                    case ALT:
                        isAltDown = true;
                        break;

                    // Forwards + backwards
                    case W:
                    case UP:
                        camMoveForward = true;
                        break;
                    case S:
                    case DOWN:
                        camMoveBackward = true;
                        break;

                    // Strafe left + right
                    case A:
                    case LEFT:
                        camStrafeLeft = true;
                        break;
                    case D:
                    case RIGHT:
                        camStrafeRight = true;
                        break;

                    // Up + down
                    case Q:
                    case PAGE_UP:
                        camMoveUp = true;
                        break;
                    case E:
                    case PAGE_DOWN:
                        camMoveDown = true;
                        break;
                }
            }
            else if (evt.getEventType() == KeyEvent.KEY_RELEASED)
            {
                switch (evt.getCode())
                {
                    // Modifiers
                    case CONTROL:
                        isControlDown = false;
                        break;
                    case ALT:
                        isAltDown = false;
                        break;

                    // Forwards + backwards
                    case W:
                    case UP:
                        camMoveForward = false;
                        break;
                    case S:
                    case DOWN:
                        camMoveBackward = false;
                        break;

                    // Strafe left + right
                    case A:
                    case LEFT:
                        camStrafeLeft = false;
                        break;
                    case D:
                    case RIGHT:
                        camStrafeRight = false;
                        break;

                    // Up + down
                    case Q:
                    case PAGE_UP:
                        camMoveUp = false;
                        break;
                    case E:
                    case PAGE_DOWN:
                        camMoveDown = false;
                        break;
                }
            }

            evt.consume();
        });

        scene.addEventHandler(MouseEvent.ANY, evt ->
        {
            if (evt.getEventType().equals(MouseEvent.MOUSE_PRESSED))
            {
                mouseX = mouseOldX = evt.getSceneX();
                mouseY = mouseOldY = evt.getSceneY();
            }
            else if (evt.getEventType().equals(MouseEvent.MOUSE_DRAGGED))
            {
                mouseOldX = mouseX;
                mouseOldY = mouseY;
                mouseX = evt.getSceneX();
                mouseY = evt.getSceneY();
                mouseDeltaX = (mouseX - mouseOldX);
                mouseDeltaY = (mouseY - mouseOldY);

                if (evt.isPrimaryButtonDown())
                {
                    // Set translation component (direct from camera's transformation matrix)
                    translate.setX(getPos().getX());
                    translate.setY(getPos().getY());
                    translate.setZ(getPos().getZ());

                    // IMPORTANT: reset affine transform to identity each time!
                    affineXform.setToIdentity();

                    // Calculate yaw, pitch angles
                    rotateYaw.setAngle(MathUtils.clamp(((rotateYaw.getAngle() + (mouseDeltaX * getSpeedModifier(evt, camMouseSpeedProperty))) % 360 + 540) % 360 - 180, -360, 360));
                    rotatePitch.setAngle(MathUtils.clamp(rotatePitch.getAngle() - (mouseDeltaY * getSpeedModifier(evt, camMouseSpeedProperty)), -75, 75));

                    // Dynamically generate the affine transform from the concatenated translation and rotation components
                    affineXform.prepend(translate.createConcatenation(rotateYaw.createConcatenation(rotatePitch)));
                }
                else if (evt.isSecondaryButtonDown())
                {
                    // Further processing needed here?
                }
                else if (evt.isMiddleButtonDown())
                {
                    // Further processing needed here?
                }
            }
        });
    }

    /**
     * Moves the camera forwards along the current view vector (+ve z).
     */
    private void moveCameraForward()
    {
        final double moveDelta = frameDeltaInSecs * getSpeedModifier(isControlDown, isAltDown, camMoveSpeedProperty.get());
        affineXform.setTx(getPos().getX() + (moveDelta * getCamNRow().getX()));
        affineXform.setTy(getPos().getY() + (moveDelta * getCamNRow().getY()));
        affineXform.setTz(getPos().getZ() + (moveDelta * getCamNRow().getZ()));
    }

    /**
     * Moves the camera backwards along the current 'view' vector (-ve z).
     */
    private void moveCameraBackward()
    {
        final double moveDelta = frameDeltaInSecs * getSpeedModifier(isControlDown, isAltDown, camMoveSpeedProperty.get());
        affineXform.setTx(getPos().getX() + (moveDelta * -getCamNRow().getX()));
        affineXform.setTy(getPos().getY() + (moveDelta * -getCamNRow().getY()));
        affineXform.setTz(getPos().getZ() + (moveDelta * -getCamNRow().getZ()));
    }

    /**
     * Moves the camera up along the current 'up' vector (-ve y).
     */
    private void moveCameraUp()
    {
        final double moveDelta = frameDeltaInSecs * getSpeedModifier(isControlDown, isAltDown, camMoveSpeedProperty.get());
        affineXform.setTx(getPos().getX() + (moveDelta * -getCamVRow().getX()));
        affineXform.setTy(getPos().getY() + (moveDelta * -getCamVRow().getY()));
        affineXform.setTz(getPos().getZ() + (moveDelta * -getCamVRow().getZ()));
    }

    /**
     * Moves the camera down along the current 'up' vector (+ve y).
     */
    private void moveCameraDown()
    {
        final double moveDelta = frameDeltaInSecs * getSpeedModifier(isControlDown, isAltDown, camMoveSpeedProperty.get());
        affineXform.setTx(getPos().getX() + (moveDelta * getCamVRow().getX()));
        affineXform.setTy(getPos().getY() + (moveDelta * getCamVRow().getY()));
        affineXform.setTz(getPos().getZ() + (moveDelta * getCamVRow().getZ()));
    }

    /**
     * Strafes the camera left along the current 'right' vector (-ve x).
     */
    private void strafeCameraLeft()
    {
        final double moveDelta = frameDeltaInSecs * getSpeedModifier(isControlDown, isAltDown, camMoveSpeedProperty.get());
        affineXform.setTx(getPos().getX() + (moveDelta * -getCamURow().getX()));
        affineXform.setTy(getPos().getY() + (moveDelta * -getCamURow().getY()));
        affineXform.setTz(getPos().getZ() + (moveDelta * -getCamURow().getZ()));
    }

    /**
     * Strafes the camera right along the current 'right' vector (+ve x).
     */
    private void strafeCameraRight()
    {
        final double moveDelta = frameDeltaInSecs * getSpeedModifier(isControlDown, isAltDown, camMoveSpeedProperty.get());
        affineXform.setTx(getPos().getX() + (moveDelta * getCamURow().getX()));
        affineXform.setTy(getPos().getY() + (moveDelta * getCamURow().getY()));
        affineXform.setTz(getPos().getZ() + (moveDelta * getCamURow().getZ()));
    }

    /**
     * Moves the camera a specified distance along its look vector.
     * @param moveDirection the direction to move along the camera's look vector.
     */
    public void moveCameraAlongLookVector(double moveDirection)
    {
        if (moveDirection > 0.0)
        {
            moveCameraForward();
        }
        else
        {
            moveCameraBackward();
        }
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
    public final Point3D getPos() { return CAM_POS_COL.call(getLocalToSceneTransform()); }

    /**
     * Set the camera's position by directly manipulating the affine transform translation component.
     * @param xPos the desired x-coordinate.
     * @param yPos the desired y-coordinate.
     * @param zPos the desired z-coordinate.
     */
    public void setPos(double xPos, double yPos, double zPos)
    {
        affineXform.setTx(xPos);
        affineXform.setTy(yPos);
        affineXform.setTz(zPos);
    }

    /**
     * Set the camera's position by directly manipulating the affine transform translation component.
     * @param newPos the desired position.
     */
    public void setPos(Point3D newPos)
    {
        setPos(newPos.getX(), newPos.getY(), newPos.getZ());
    }

    /**
     * Set the camera's yaw rotation by directly manipulating the rotation components.
     * @param angle the desired yaw.
     */
    public void setYaw(double angle)
    {
        rotateYaw.setAngle(angle);

        // IMPORTANT! Update the affine transformation
        affineXform.prepend(translate.createConcatenation(rotateYaw.createConcatenation(rotatePitch)));
    }

    /**
     * Set the camera's pitch rotation by directly manipulating the rotation components.
     * @param angle the desired pitch.
     */
    public void setPitch(double angle)
    {
        rotatePitch.setAngle(angle);

        // IMPORTANT! Update the affine transformation
        affineXform.prepend(translate.createConcatenation(rotateYaw.createConcatenation(rotatePitch)));
    }

    /**
     * Set the camera's roll rotation by directly manipulating the rotation components.
     * @param angle the desired roll.
     */
    public void setRoll(double angle)
    {
        rotateRoll.setAngle(angle);

        // IMPORTANT! Update the affine transformation
        affineXform.prepend(translate.createConcatenation(rotateYaw.createConcatenation(rotatePitch)));
    }

    /**
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public double getSpeedModifier(GestureEvent event, Property<Number> property)
    {
        return getSpeedModifier(event.isControlDown(), event.isAltDown(), property.getValue().doubleValue());
    }

    /**
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public double getSpeedModifier(MouseEvent event, Property<Number> property)
    {
        return getSpeedModifier(event.isControlDown(), event.isAltDown(), property.getValue().doubleValue());
    }

    /**
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public double getSpeedModifier(Boolean isCtrlDown, Boolean isAltDown, double defaultValue)
    {
        double multiplier = 1;

        if (isCtrlDown)
        {
            multiplier = camSpeedDownMultiplierProperty.get();
        }
        else if (isAltDown)
        {
            multiplier = camSpeedUpMultiplierProperty.get();
        }

        return defaultValue * multiplier;
    }

    /**
     * Reset the camera's default property values for move speed, mouse speed, etc.
     */
    public void resetDefaultCamMoveSpeed() { camMoveSpeedProperty.set(CAM_MOVE_SPEED); }
    public void resetDefaultCamMouseSpeed() { camMouseSpeedProperty.set(CAM_MOUSE_SPEED); }
    public void resetDefaultCamSpeedDownMultiplier() { camSpeedDownMultiplierProperty.set(CAM_SPEED_DOWN_MULTIPLIER); }
    public void resetDefaultCamSpeedUpMultiplier() { camSpeedUpMultiplierProperty.set(CAM_SPEED_UP_MULTIPLIER); }

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
    private Point3D getCamViewCol() { return CAM_VIEW_COL.call(getLocalToSceneTransform()); }
    private Point3D getCamUpCol() { return CAM_UP_COL.call(getLocalToSceneTransform()); }
    private Point3D getCamRightCol() { return CAM_RIGHT_COL.call(getLocalToSceneTransform()); }

    private Point3D getCamNRow() { return CAM_N_ROW.call(getLocalToSceneTransform()); }
    private Point3D getCamVRow() { return CAM_V_ROW.call(getLocalToSceneTransform()); }
    private Point3D getCamURow() { return CAM_U_ROW.call(getLocalToSceneTransform()); }
}
