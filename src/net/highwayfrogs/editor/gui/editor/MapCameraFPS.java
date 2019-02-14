package net.highwayfrogs.editor.gui.editor;

import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.transform.Affine;

/**
 * Specialized camera class to allow FPS-like movement within the map editor.
 * Created by AndyEder on 2/14/2019.
 */

// TODO: Finish writing this camera component!

public class MapCameraFPS extends Parent
{
    // Constants / variables used for frame delta calculations
    private final double NANOSEC_IN_SECONDS = 1.0 / 1000000000.0;
    private long lastFrameUpdate = 0;
    private double currFrameUpdate;
    private final double updatePeriod = 2.0;

    // For scene graph (node) processing and management
    private final Group rootGrp = new Group();
    private final Affine affineXform = new Affine();


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

        // Add out affine transformation (we manually update this per frame to produce the desired camera behaviour)
        getTransforms().add(affineXform);

        // Ensure the camera is setup with default values and kick-off the camera update thread
        setupAndInitialiseCamera();
        setupAndStartThreadingCameraUpdate();
    }

    /**
     * Setup camera and provide sensible initial / default values.
     */
    private void setupAndInitialiseCamera()
    {
    }

    /**
     * Setup and kick-off the thread that will be responsible for handling the camera updates.
     */
    private void setupAndStartThreadingCameraUpdate()
    {
    }
}
