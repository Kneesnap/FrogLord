package net.highwayfrogs.editor.gui.mesh.fxobject;

import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.map.view.RawColorTextureSource;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.KeyHandler;
import net.highwayfrogs.editor.gui.editor.FirstPersonCamera;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshUnmanagedNode;
import net.highwayfrogs.editor.gui.mesh.wrapper.MeshEntryBox;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A translation gizmo represents a 3D UI element which can be moved in any of the XYZ direction.
 * Created by Kneesnap on 1/3/2024.
 */
@Getter
public class TranslationGizmo extends DynamicMesh {
    private static final RawColorTextureSource RED_TEXTURE_SOURCE = new RawColorTextureSource(Color.RED);
    private static final RawColorTextureSource GREEN_TEXTURE_SOURCE = new RawColorTextureSource(Color.GREEN);
    private static final RawColorTextureSource BLUE_TEXTURE_SOURCE = new RawColorTextureSource(Color.BLUE);
    private static final RawColorTextureSource LIGHT_RED_TEXTURE_SOURCE = new RawColorTextureSource(0xFF7F1919);
    private static final RawColorTextureSource LIGHT_GREEN_TEXTURE_SOURCE = new RawColorTextureSource(0xFF197F19);
    private static final RawColorTextureSource LIGHT_BLUE_TEXTURE_SOURCE = new RawColorTextureSource(0xFF19197F);
    private static final RawColorTextureSource ORANGE_TEXTURE_SOURCE = new RawColorTextureSource(Color.ORANGE);
    private static final RawColorTextureSource WHITE_TEXTURE_SOURCE = new RawColorTextureSource(Color.WHITE);
    private static final Point3D ALL_AXIS = new Point3D(1, 1, 1);
    private static final double BAR_THICKNESS = 2D;
    private static final double BAR_LENGTH = 25D;
    private static final double BOX_SIZE = 6D;
    private static final double SNAPPING_THRESHOLD = 3.5D;

    private final Map<MeshView, GizmoMeshViewState> meshViewStates = new HashMap<>();
    private final KeyHandler keyListener = this::onKeyPressed;
    private final boolean xAxisEnabled;
    private final boolean yAxisEnabled;
    private final boolean zAxisEnabled;
    private AtlasTexture redTexture;
    private AtlasTexture greenTexture;
    private AtlasTexture blueTexture;
    private AtlasTexture lightRedTexture;
    private AtlasTexture lightGreenTexture;
    private AtlasTexture lightBlueTexture;
    private int redTextureUvIndex = -1;
    private int greenTextureUvIndex = -1;
    private int blueTextureUvIndex = -1;
    private int lightRedTextureUvIndex = -1;
    private int lightGreenTextureUvIndex = -1;
    private int lightBlueTextureUvIndex = -1;
    private AtlasTexture orangeTexture;
    private AtlasTexture whiteTexture;
    private int orangeTextureUvIndex = -1;
    private int whiteTextureUvIndex = -1;
    private DynamicMeshUnmanagedNode baseNode;
    private DynamicMeshUnmanagedNode xAxisNode; // Red
    private DynamicMeshUnmanagedNode yAxisNode; // Green
    private DynamicMeshUnmanagedNode zAxisNode; // Blue

    // Drag/user interaction state.
    private Box axisPlane;
    private Point3D movementAxis;
    private Point3D dragStartPosition;
    private double originalPosition = Double.NaN;
    private MeshView draggedMeshView;

    public static final int X_CHANGED_FLAG = Constants.BIT_FLAG_0;
    public static final int Y_CHANGED_FLAG = Constants.BIT_FLAG_1;
    public static final int Z_CHANGED_FLAG = Constants.BIT_FLAG_2;

    public TranslationGizmo() {
        this(true, true, true);
    }

    public TranslationGizmo(boolean xAxisEnabled, boolean yAxisEnabled, boolean zAxisEnabled) {
        super(new SequentialTextureAtlas(32, 32, false));
        this.xAxisEnabled = xAxisEnabled;
        this.yAxisEnabled = yAxisEnabled;
        this.zAxisEnabled = zAxisEnabled;
        setupTextureAtlas();
        setupMesh();
    }

    private void setupTextureAtlas() {
        getTextureAtlas().startBulkOperations();
        this.redTexture = getTextureAtlas().addTexture(RED_TEXTURE_SOURCE);
        this.greenTexture = getTextureAtlas().addTexture(GREEN_TEXTURE_SOURCE);
        this.blueTexture = getTextureAtlas().addTexture(BLUE_TEXTURE_SOURCE);
        this.lightRedTexture = getTextureAtlas().addTexture(LIGHT_RED_TEXTURE_SOURCE);
        this.lightGreenTexture = getTextureAtlas().addTexture(LIGHT_GREEN_TEXTURE_SOURCE);
        this.lightBlueTexture = getTextureAtlas().addTexture(LIGHT_BLUE_TEXTURE_SOURCE);
        this.orangeTexture = getTextureAtlas().addTexture(ORANGE_TEXTURE_SOURCE);
        this.whiteTexture = getTextureAtlas().addTexture(WHITE_TEXTURE_SOURCE);
        getTextureAtlas().endBulkOperations();
    }

    private void setupMesh() {
        Vector2f whiteTextureUv = getTextureAtlas().getUV(this.whiteTexture, WHITE_TEXTURE_SOURCE.getUv());
        Vector2f redTextureUv = getTextureAtlas().getUV(this.redTexture, RED_TEXTURE_SOURCE.getUv());
        Vector2f greenTextureUv = getTextureAtlas().getUV(this.greenTexture, GREEN_TEXTURE_SOURCE.getUv());
        Vector2f blueTextureUv = getTextureAtlas().getUV(this.blueTexture, BLUE_TEXTURE_SOURCE.getUv());
        Vector2f lightRedTextureUv = getTextureAtlas().getUV(this.lightRedTexture, LIGHT_RED_TEXTURE_SOURCE.getUv());
        Vector2f lightGreenTextureUv = getTextureAtlas().getUV(this.lightGreenTexture, LIGHT_GREEN_TEXTURE_SOURCE.getUv());
        Vector2f lightBlueTextureUv = getTextureAtlas().getUV(this.lightBlueTexture, LIGHT_BLUE_TEXTURE_SOURCE.getUv());
        Vector2f orangeTextureUv = getTextureAtlas().getUV(this.orangeTexture, ORANGE_TEXTURE_SOURCE.getUv());
        final double halfThickness = BAR_THICKNESS / 2;
        final double halfBoxSize = (BOX_SIZE / 2);
        final double barEnd = BAR_LENGTH + halfBoxSize;

        // Setup X-Axis Node (Red)
        if (this.xAxisEnabled) {
            this.xAxisNode = new DynamicMeshUnmanagedNode(this);
            addNode(this.xAxisNode);
            DynamicMeshDataEntry xNodeEntry = new DynamicMeshDataEntry(this);
            this.lightRedTextureUvIndex = xNodeEntry.addTexCoordValue(lightRedTextureUv);
            this.redTextureUvIndex = xNodeEntry.addTexCoordValue(redTextureUv);
            MeshEntryBox.createBox(xNodeEntry, halfBoxSize, -halfThickness, -halfThickness, barEnd, halfThickness, halfThickness, this.redTextureUvIndex);
            MeshUtils.createXAxisPyramid(xNodeEntry, BAR_LENGTH, 0, 0, BOX_SIZE, BOX_SIZE, BOX_SIZE, this.redTextureUvIndex);
            this.xAxisNode.addEntry(xNodeEntry);
        }

        // Setup Z-Axis Node (Blue)
        if (this.zAxisEnabled) {
            this.zAxisNode = new DynamicMeshUnmanagedNode(this);
            addNode(this.zAxisNode);
            DynamicMeshDataEntry zNodeEntry = new DynamicMeshDataEntry(this);
            this.lightBlueTextureUvIndex = zNodeEntry.addTexCoordValue(lightBlueTextureUv);
            this.blueTextureUvIndex = zNodeEntry.addTexCoordValue(blueTextureUv);
            MeshEntryBox.createBox(zNodeEntry, -halfThickness, -halfThickness, halfBoxSize, halfThickness, halfThickness, barEnd, this.blueTextureUvIndex);
            MeshUtils.createZAxisPyramid(zNodeEntry, 0, 0, BAR_LENGTH, BOX_SIZE, BOX_SIZE, BOX_SIZE, this.blueTextureUvIndex);
            this.zAxisNode.addEntry(zNodeEntry);
        }

        // Setup Y-Axis Node (Green)
        // Added after X & Z due to expected usage making it less likely to have ordering problems if we do it this way.
        if (this.yAxisEnabled) {
            this.yAxisNode = new DynamicMeshUnmanagedNode(this);
            addNode(this.yAxisNode);
            DynamicMeshDataEntry yNodeEntry = new DynamicMeshDataEntry(this);
            this.lightGreenTextureUvIndex = yNodeEntry.addTexCoordValue(lightGreenTextureUv);
            this.greenTextureUvIndex = yNodeEntry.addTexCoordValue(greenTextureUv);
            MeshEntryBox.createBox(yNodeEntry, -halfThickness, -halfBoxSize, -halfThickness, halfThickness, -barEnd, halfThickness, this.greenTextureUvIndex);
            MeshUtils.createYAxisPyramid(yNodeEntry, 0, -BAR_LENGTH, 0, BOX_SIZE, BOX_SIZE, BOX_SIZE, this.greenTextureUvIndex);
            this.yAxisNode.addEntry(yNodeEntry);
        }

        // Setup base node.
        // The base node should be added last, so it always renders on top of the arrows.
        this.baseNode = new DynamicMeshUnmanagedNode(this);
        addNode(this.baseNode);
        DynamicMeshDataEntry baseNodeEntry = new DynamicMeshDataEntry(this);
        this.whiteTextureUvIndex = baseNodeEntry.addTexCoordValue(whiteTextureUv);
        this.orangeTextureUvIndex = baseNodeEntry.addTexCoordValue(orangeTextureUv);
        MeshEntryBox.createCenteredBox(baseNodeEntry, 0, 0, 0, BOX_SIZE, BOX_SIZE, BOX_SIZE, whiteTextureUvIndex);
        this.baseNode.addEntry(baseNodeEntry);
    }

    /**
     * Adds a MeshView with extra data setup.
     * @param view the MeshView to add.
     * @param controller the mesh view controller the scene is viewed from.
     * @param listener the listener to call when the position changes.
     */
    public void addView(MeshView view, MeshViewController<?> controller, IPositionChangeListener listener) {
        addView(view);
        GizmoMeshViewState state = this.meshViewStates.get(view);
        state.setController(controller);
        state.setCamera(controller.getFirstPersonCamera());
        state.setInputManager(controller.getInputManager());
        state.setChangeListener(listener);
    }

    @Override
    public void addView(MeshView view) {
        super.addView(view);
        this.meshViewStates.put(view, new GizmoMeshViewState(this, view));

        // Setup listeners.
        // Uses the simple press-drag-release gesture as described by https://docs.oracle.com/javase/8/javafx/api/javafx/scene/input/MouseEvent.html
        view.setOnMouseEntered(this::onMouseEnter);
        view.setOnMouseExited(this::onMouseExit);
        view.setOnMousePressed(this::onDragStart);
        view.setOnMouseDragged(this::onDragUpdate);
        view.setOnMouseReleased(this::onDragStop);
    }

    @Override
    public void removeView(MeshView view) {
        super.addView(view);

        // Remove listeners.
        view.setOnMouseEntered(null);
        view.setOnMouseExited(null);
        view.setOnMousePressed(null);
        view.setOnMouseDragged(null);
        view.setOnMouseReleased(null);

        // Remove plane.
        stopDragging(view, false);
        this.meshViewStates.remove(view);
    }

    /**
     * Set the position change listener for a given mesh view.
     * This function is only valid after the MeshView has been registered with addView().
     * @param view view to set the change listener for
     * @param listener the new listener to apply
     */
    public void setChangeListener(MeshView view, IPositionChangeListener listener) {
        GizmoMeshViewState state = this.meshViewStates.get(view);
        if (state == null)
            throw new RuntimeException("The provided MeshView '" + view + "' has not been registered to " + Utils.getSimpleName(this) + ", and thus it is not yet possible to set the listener.");

        state.setChangeListener(listener);
    }

    /**
     * Gets the X position coordinate of the provided MeshView.
     * @param meshView the MeshView to get the position from
     * @return positional value
     */
    public double getPositionX(MeshView meshView) {
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot read position from a MeshView which isn't displaying this gizmo!");

        // Get position value.
        Translate position = Scene3DUtils.getOptional3DTranslation(meshView);
        return position != null ? position.getX() : 0;
    }

    /**
     * Sets the new X position coordinate.
     * @param meshView the MeshView to set the coordinate for
     * @param newX new x coordinate
     * @param fireEvent whether the listener should be alerted of this change
     */
    public void setPositionX(MeshView meshView, double newX, boolean fireEvent) {
        if (!Double.isFinite(newX))
            throw new IllegalArgumentException("The x world coordinate cannot be set to: " + newX);
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot apply position to a MeshView which isn't displaying this gizmo!");

        // Update position value.
        Translate position = Scene3DUtils.get3DTranslation(meshView);
        double oldX = position.getX();
        position.setX(newX);

        // Fire the event if necessary.
        if (fireEvent) {
            GizmoMeshViewState state = this.meshViewStates.get(meshView);
            if (state != null && state.getChangeListener() != null)
                state.getChangeListener().handle(meshView, oldX, position.getY(), position.getZ(), newX, position.getY(), position.getZ(), X_CHANGED_FLAG);
        }
    }

    /**
     * Gets the Y position coordinate of the provided MeshView.
     * @param meshView the MeshView to get the position from
     * @return positional value
     */
    public double getPositionY(MeshView meshView) {
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot read position from a MeshView which isn't displaying this gizmo!");

        // Get position value.
        Translate position = Scene3DUtils.getOptional3DTranslation(meshView);
        return position != null ? position.getY() : 0;
    }

    /**
     * Sets the new Y position coordinate.
     * @param meshView the MeshView to set the coordinate for
     * @param newY new y coordinate
     * @param fireEvent whether the listener should be alerted of this change
     */
    public void setPositionY(MeshView meshView, double newY, boolean fireEvent) {
        if (!Double.isFinite(newY))
            throw new IllegalArgumentException("The y world coordinate cannot be set to: " + newY);
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot apply position to a MeshView which isn't displaying this gizmo!");

        // Update position value.
        Translate position = Scene3DUtils.get3DTranslation(meshView);
        double oldY = position.getY();
        position.setY(newY);

        // Fire the event if necessary.
        if (fireEvent) {
            GizmoMeshViewState state = this.meshViewStates.get(meshView);
            if (state != null && state.getChangeListener() != null)
                state.getChangeListener().handle(meshView, position.getX(), oldY, position.getZ(), position.getX(), newY, position.getZ(), Y_CHANGED_FLAG);
        }
    }

    /**
     * Gets the Z position coordinate of the provided MeshView.
     * @param meshView the MeshView to get the position from
     * @return positional value
     */
    public double getPositionZ(MeshView meshView) {
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot read position from a MeshView which isn't displaying this gizmo!");

        // Get position value.
        Translate position = Scene3DUtils.getOptional3DTranslation(meshView);
        return position != null ? position.getZ() : 0;
    }

    /**
     * Sets the new Y position coordinate.
     * @param meshView the MeshView to set the coordinate for
     * @param newZ new z coordinate
     * @param fireEvent whether the listener should be alerted of this change
     */
    public void setPositionZ(MeshView meshView, double newZ, boolean fireEvent) {
        if (!Double.isFinite(newZ))
            throw new IllegalArgumentException("The z world coordinate cannot be set to: " + newZ);
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot apply position to a MeshView which isn't displaying this gizmo!");

        // Update position value.
        Translate position = Scene3DUtils.get3DTranslation(meshView);
        double oldZ = position.getZ();
        position.setZ(newZ);

        // Fire the event if necessary.
        if (fireEvent) {
            GizmoMeshViewState state = this.meshViewStates.get(meshView);
            if (state != null && state.getChangeListener() != null)
                state.getChangeListener().handle(meshView, position.getX(), position.getY(), oldZ, position.getX(), position.getY(), newZ, Z_CHANGED_FLAG);
        }
    }

    /**
     * Gets the position coordinates of the provided MeshView.
     * @param meshView the MeshView to get the position from
     * @return positional value, or null if there is no position
     */
    public Translate getPosition(MeshView meshView) {
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot read position from a MeshView which isn't displaying this gizmo!");

        return Scene3DUtils.getOptional3DTranslation(meshView);
    }

    /**
     * Sets the new X position coordinate.
     * @param meshView the MeshView to set the coordinate for
     * @param newX new x coordinate
     * @param fireEvent whether the listener should be alerted of this change
     */
    public void setPosition(MeshView meshView, double newX, double newY, double newZ, boolean fireEvent) {
        if (!Double.isFinite(newX))
            throw new IllegalArgumentException("The x world coordinate cannot be set to: " + newX);
        if (!Double.isFinite(newY))
            throw new IllegalArgumentException("The y world coordinate cannot be set to: " + newY);
        if (!Double.isFinite(newZ))
            throw new IllegalArgumentException("The z world coordinate cannot be set to: " + newZ);
        if (!getMeshViews().contains(meshView))
            throw new IllegalArgumentException("Cannot apply position to a MeshView which isn't displaying this gizmo!");

        // Update position value.
        Translate position = Scene3DUtils.get3DTranslation(meshView);
        double oldX = position.getX();
        double oldY = position.getY();
        double oldZ = position.getZ();
        position.setX(newX);
        position.setY(newY);
        position.setZ(newZ);

        // Fire the event if necessary.
        if (fireEvent) {
            GizmoMeshViewState state = this.meshViewStates.get(meshView);
            if (state != null && state.getChangeListener() != null)
                state.getChangeListener().handle(meshView, oldX, oldY, oldZ, newX, newY, newZ, X_CHANGED_FLAG | Y_CHANGED_FLAG | Z_CHANGED_FLAG);
        }
    }

    /**
     * Test if the gizmo is currently used for dragging.
     */
    public boolean isDraggingActive() {
        return this.movementAxis != null && this.axisPlane != null && this.dragStartPosition != null;
    }

    private void onMouseEnter(MouseEvent event) {
        if (event.isPrimaryButtonDown())
            return; // If the button is currently held, don't do anything here.

        // Find the node clicked.
        DynamicMeshDataEntry clickedMeshEntry = getDataEntryByFaceIndex(event.getPickResult().getIntersectedFace());
        DynamicMeshNode clickedMeshNode = clickedMeshEntry != null ? clickedMeshEntry.getMeshNode() : null;
        if (clickedMeshNode == null)
            return;

        if (clickedMeshNode == this.xAxisNode) {
            this.xAxisNode.updateTextureIndex(this.redTextureUvIndex, this.lightRedTextureUvIndex);
            this.movementAxis = Rotate.X_AXIS;
        } else if (clickedMeshNode == this.yAxisNode) {
            this.yAxisNode.updateTextureIndex(this.greenTextureUvIndex, this.lightGreenTextureUvIndex);
            this.movementAxis = Rotate.Y_AXIS;
        } else if (clickedMeshNode == this.zAxisNode) {
            this.zAxisNode.updateTextureIndex(this.blueTextureUvIndex, this.lightBlueTextureUvIndex);
            this.movementAxis = Rotate.Z_AXIS;
        }
    }

    private void onMouseExit(MouseEvent event) {
        if (event.isPrimaryButtonDown())
            return; // If the button is currently held, don't do anything here.

        if (this.movementAxis == Rotate.X_AXIS) {
            this.xAxisNode.updateTextureIndex(this.lightRedTextureUvIndex, this.redTextureUvIndex);
        } else if (this.movementAxis == Rotate.Y_AXIS) {
            this.yAxisNode.updateTextureIndex(this.lightGreenTextureUvIndex, this.greenTextureUvIndex);
        } else if (this.movementAxis == Rotate.Z_AXIS) {
            this.zAxisNode.updateTextureIndex(this.lightBlueTextureUvIndex, this.blueTextureUvIndex);
        }
    }

    private void onDragStart(MouseEvent event) {
        MeshView meshView = (MeshView) event.getSource();
        Group scene = Scene3DUtils.getSubSceneGroup(meshView.getScene().getRoot());

        // Clear existing axis plane, if it somehow exists.
        if (this.axisPlane != null) {
            scene.getChildren().remove(this.axisPlane);
            this.axisPlane = null;
        }

        // Find the node clicked.
        PickResult result = event.getPickResult();
        DynamicMeshDataEntry clickedMeshEntry = getDataEntryByFaceIndex(result.getIntersectedFace());
        DynamicMeshNode clickedMeshNode = clickedMeshEntry != null ? clickedMeshEntry.getMeshNode() : null;
        if (clickedMeshNode == null)
            return;

        // Setup Axis Planes
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        if (clickedMeshNode == this.xAxisNode) {
            this.movementAxis = Rotate.X_AXIS;
            this.xAxisNode.updateTextureIndex(this.redTextureUvIndex, this.orangeTextureUvIndex);
            this.xAxisNode.updateTextureIndex(this.lightRedTextureUvIndex, this.orangeTextureUvIndex);
            this.originalPosition = getPositionX(meshView);
        } else if (clickedMeshNode == this.yAxisNode) {
            this.movementAxis = Rotate.Y_AXIS;
            this.yAxisNode.updateTextureIndex(this.greenTextureUvIndex, this.orangeTextureUvIndex);
            this.yAxisNode.updateTextureIndex(this.lightGreenTextureUvIndex, this.orangeTextureUvIndex);
            this.originalPosition = getPositionY(meshView);
        } else if (clickedMeshNode == this.zAxisNode) {
            this.movementAxis = Rotate.Z_AXIS;
            this.zAxisNode.updateTextureIndex(this.blueTextureUvIndex, this.orangeTextureUvIndex);
            this.zAxisNode.updateTextureIndex(this.lightBlueTextureUvIndex, this.orangeTextureUvIndex);
            this.originalPosition = getPositionZ(meshView);
        } else {
            // Didn't click one of the axis.
            this.movementAxis = null;
            return;
        }

        // Setup axis plane for mouse-picking.
        this.axisPlane = Scene3DUtils.createAxisPlane(meshView, scene, state.getCamera(), this.movementAxis);

        // Update state.
        Scale scale = Scene3DUtils.getOptional3DScale(meshView);
        if (scale != null) {
            Point3D startPoint = result.getIntersectedPoint();
            this.dragStartPosition = new Point3D(startPoint.getX() * scale.getX(), startPoint.getY() * scale.getY(), startPoint.getZ() * scale.getZ());
        } else {
            this.dragStartPosition = result.getIntersectedPoint();
        }

        // Add key listener, so we can cancel if necessary.
        this.draggedMeshView = meshView;
        if (state.getInputManager() != null)
            state.getInputManager().addKeyListener(KeyCode.ESCAPE, this.keyListener);

        // Disable dragging on the gizmo, so only the plane will get events.
        meshView.setMouseTransparent(true);
        meshView.setCursor(Cursor.CROSSHAIR);

        // Prevent the drag updates from moving the camera view.
        event.consume();
    }

    private void onDragUpdate(MouseEvent event) {
        MeshView meshView = (MeshView) event.getSource();
        event.consume(); // Prevent this drag from moving the camera view.

        // Abort if no known spot is found.
        PickResult result = event.getPickResult();
        if (result == null)
            return;

        // Ensure that the picked node is either the gizmo MeshView, or the AxisPlane. (Prevents other nodes such as 2D UI from breaking the position)
        if ((result.getIntersectedNode() != meshView) && (result.getIntersectedNode() != this.axisPlane))
            return;

        // If there's no axis currently selected, or no drag start, we're probably selecting the base, which shouldn't do anything here.
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        if (!isDraggingActive())
            return;

        // Test if there's an intersection point.
        Point3D mousePoint = result.getIntersectedPoint();
        if (mousePoint == null)
            return;

        // Ensure the coordinates we get are now absolute.
        Point3D mouseOffset = mousePoint.subtract(this.dragStartPosition);
        Point3D newWorldPos = result.getIntersectedNode().localToScene(mouseOffset);

        // Get old position.
        Translate gizmoTranslate = Scene3DUtils.get3DTranslation(meshView);
        Translate planeTranslate = Scene3DUtils.get3DTranslation(this.axisPlane);
        double oldX = gizmoTranslate.getX();
        double oldY = gizmoTranslate.getY();
        double oldZ = gizmoTranslate.getZ();

        // Update positions.
        if (this.movementAxis == Rotate.X_AXIS) {
            double newX = newWorldPos.getX();
            if (Math.abs(newX - this.originalPosition) <= SNAPPING_THRESHOLD)
                newX = this.originalPosition; // Snap positions near the start position to the start position.

            // Update positions & fire event.
            gizmoTranslate.setX(newX);
            planeTranslate.setX(newX);
            if (state.getChangeListener() != null && oldX != newX)
                state.getChangeListener().handle(meshView, oldX, oldY, oldZ, newX, oldY, oldZ, X_CHANGED_FLAG);
        } else if (this.movementAxis == Rotate.Y_AXIS) {
            double newY = newWorldPos.getY();
            if (Math.abs(newY - this.originalPosition) <= SNAPPING_THRESHOLD)
                newY = this.originalPosition; // Snap positions near the start position to the start position.

            // Update positions & fire event.
            gizmoTranslate.setY(newY);
            planeTranslate.setY(newY);
            if (state.getChangeListener() != null && oldY != newY)
                state.getChangeListener().handle(meshView, oldX, oldY, oldZ, oldX, newY, oldZ, Y_CHANGED_FLAG);
        } else if (this.movementAxis == Rotate.Z_AXIS) {
            double newZ = newWorldPos.getZ();
            if (Math.abs(newZ - this.originalPosition) <= SNAPPING_THRESHOLD)
                newZ = this.originalPosition; // Snap positions near the start position to the start position.

            // Update positions & fire event.
            gizmoTranslate.setZ(newZ);
            planeTranslate.setZ(newZ);
            if (state.getChangeListener() != null && oldZ != newZ)
                state.getChangeListener().handle(meshView, oldX, oldY, oldZ, oldX, oldY, newZ, Z_CHANGED_FLAG);
        }
    }

    private void onDragStop(MouseEvent event) {
        MeshView meshView = (MeshView) event.getSource();

        event.consume(); // Prevent the drag updates from moving the camera view.
        stopDragging(meshView, false); // Stop dragging.
    }

    private void onKeyPressed(InputManager manager, KeyEvent event) {
        // Cancel the drag, and restore the original position.
        event.consume();
        stopDragging(this.draggedMeshView, true);
    }

    private void stopDragging(MeshView meshView, boolean restoreOriginalPosition) {
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        if (state != null && state.getInputManager() != null)
            state.getInputManager().removeKeyListener(KeyCode.ESCAPE, this.keyListener);

        // Remove the axis plane from the scene.
        if (this.axisPlane != null) {
            Group scene = Scene3DUtils.getSubSceneGroup(this.axisPlane.getScene());
            scene.getChildren().remove(this.axisPlane);
            this.axisPlane = null;
        }

        // Make the arrow be highlighted no longer.
        if (this.movementAxis != null) {
            if (this.movementAxis == Rotate.X_AXIS) {
                this.xAxisNode.updateTextureIndex(this.orangeTextureUvIndex, this.redTextureUvIndex);
                if (restoreOriginalPosition)
                    setPositionX(meshView, this.originalPosition, true);
            } else if (this.movementAxis == Rotate.Y_AXIS) {
                this.yAxisNode.updateTextureIndex(this.orangeTextureUvIndex, this.greenTextureUvIndex);
                if (restoreOriginalPosition)
                    setPositionY(meshView, this.originalPosition, true);
            } else if (this.movementAxis == Rotate.Z_AXIS) {
                this.zAxisNode.updateTextureIndex(this.orangeTextureUvIndex, this.blueTextureUvIndex);
                if (restoreOriginalPosition)
                    setPositionZ(meshView, this.originalPosition, true);
            }

            this.movementAxis = null;
            this.originalPosition = Double.NaN;
        }

        // Clear remaining drag state.
        this.dragStartPosition = null;
        this.draggedMeshView = null;

        // Re-enable dragging on the gizmo.
        meshView.setMouseTransparent(false);
        meshView.setCursor(Cursor.DEFAULT);
    }

    @Getter
    @RequiredArgsConstructor
    private static class GizmoMeshViewState {
        private final TranslationGizmo gizmo;
        private final MeshView meshView;
        @Setter private IPositionChangeListener changeListener;
        @Setter private MeshViewController<?> controller;
        @Setter private FirstPersonCamera camera;
        @Setter private InputManager inputManager;
    }

    /**
     * Listens for change in position.
     */
    public interface IPositionChangeListener {
        /**
         * Handle a position change.
         * @param meshView the mesh view which updated its scale
         * @param oldX old x coordinate
         * @param oldY old y coordinate
         * @param oldZ old z coordinate
         * @param newX new x coordinate
         * @param newY new y coordinate
         * @param newZ new z coordinate
         * @param flags flags containing information about the change
         */
        void handle(MeshView meshView, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, int flags);

        /**
         * Creates a listener which only listens for the change, but doesn't care about the changed positional values.
         * @param runnable The runnable to run on change.
         * @return newListener, or null
         */
        public static IPositionChangeListener makeListener(Runnable runnable) {
            return runnable != null ? (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> runnable.run() : null;
        }

        /**
         * Creates a listener which only listens for the change, but doesn't care about the changed positional values.
         * @param listener The consumer to run on change.
         * @return newListener, or null
         */
        public static IPositionChangeListener makeListener(Consumer<MeshView> listener) {
            return listener != null ? (meshView, oldX, oldY, oldZ, newX, newY, newZ, flags) -> listener.accept(meshView) : null;
        }
    }
}