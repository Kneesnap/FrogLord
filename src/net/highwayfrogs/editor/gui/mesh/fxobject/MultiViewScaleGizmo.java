package net.highwayfrogs.editor.gui.mesh.fxobject;

import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.view.RawColorTextureSource;
import net.highwayfrogs.editor.gui.editor.FirstPersonCamera;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A scale gizmo represents a 3D UI element which can be expanded in any of the XYZ directions, indicating scale.
 * This is different from ScaleGizmo, because this gizmo is usable for multiple MeshViews, but looks visually worse.
 * Created by Kneesnap on 1/7/2024.
 */
@Getter
public class MultiViewScaleGizmo extends DynamicMesh {
    private static final RawColorTextureSource RED_TEXTURE_SOURCE = new RawColorTextureSource(Color.RED);
    private static final RawColorTextureSource GREEN_TEXTURE_SOURCE = new RawColorTextureSource(Color.GREEN);
    private static final RawColorTextureSource BLUE_TEXTURE_SOURCE = new RawColorTextureSource(Color.BLUE);
    private static final RawColorTextureSource LIGHT_RED_TEXTURE_SOURCE = new RawColorTextureSource(0xFF7F1919);
    private static final RawColorTextureSource LIGHT_GREEN_TEXTURE_SOURCE = new RawColorTextureSource(0xFF197F19);
    private static final RawColorTextureSource LIGHT_BLUE_TEXTURE_SOURCE = new RawColorTextureSource(0xFF19197F);
    private static final RawColorTextureSource ORANGE_TEXTURE_SOURCE = new RawColorTextureSource(Color.ORANGE);
    private static final RawColorTextureSource WHITE_TEXTURE_SOURCE = new RawColorTextureSource(Color.WHITE);
    private static final double BAR_THICKNESS = 2D;
    private static final double BAR_LENGTH = 25D;
    private static final double BOX_SIZE = 6D;
    private static final double AXIS_BASE_BOX_DISTANCE = BAR_LENGTH + BOX_SIZE;

    private final Map<MeshView, GizmoMeshViewState> meshViewStates = new HashMap<>();
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

    // Used for user interaction.
    private Box axisPlane;
    private Point3D movementAxis;

    public MultiViewScaleGizmo() {
        this(true, true, true);
    }

    public MultiViewScaleGizmo(boolean xAxisEnabled, boolean yAxisEnabled, boolean zAxisEnabled) {
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
            MeshEntryBox.createCenteredBox(xNodeEntry, AXIS_BASE_BOX_DISTANCE, 0, 0, BOX_SIZE, BOX_SIZE, BOX_SIZE, this.redTextureUvIndex);
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
            MeshEntryBox.createCenteredBox(zNodeEntry, 0, 0, AXIS_BASE_BOX_DISTANCE, BOX_SIZE, BOX_SIZE, BOX_SIZE, this.blueTextureUvIndex);
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
            MeshEntryBox.createCenteredBox(yNodeEntry, 0, -AXIS_BASE_BOX_DISTANCE, 0, BOX_SIZE, BOX_SIZE, BOX_SIZE, this.greenTextureUvIndex);
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
     * @param view     the MeshView to add.
     * @param camera   the camera the scene is viewed from.
     * @param listener the listener to call when the scale changes.
     */
    public void addView(MeshView view, FirstPersonCamera camera, IScaleChangeListener listener) {
        addView(view);
        GizmoMeshViewState state = this.meshViewStates.get(view);
        state.setCamera(camera);
        state.setChangeListener(listener);
    }

    @Override
    public boolean addView(MeshView view) {
        if (!super.addView(view))
            return false;

        this.meshViewStates.put(view, new GizmoMeshViewState(this, view));

        // Setup listeners.
        // Uses the simple press-drag-release gesture as described by https://docs.oracle.com/javase/8/javafx/api/javafx/scene/input/MouseEvent.html
        view.setOnMouseEntered(this::onMouseEnter);
        view.setOnMouseExited(this::onMouseExit);
        view.setOnMousePressed(this::onDragStart);
        view.setOnMouseDragged(this::onDragUpdate);
        view.setOnMouseReleased(this::onDragStop);
        return true;
    }

    @Override
    public boolean removeView(MeshView view) {
        if (!super.removeView(view))
            return false;

        this.meshViewStates.remove(view);

        // Remove listeners.
        view.setOnMouseEntered(null);
        view.setOnMouseExited(null);
        view.setOnMousePressed(null);
        view.setOnMouseDragged(null);
        view.setOnMouseReleased(null);

        // Remove plane.
        stopDragging(view);
        return true;
    }

    /**
     * Set the position change listener for a given mesh view.
     * This function is only valid after the MeshView has been registered with addView().
     * @param view     view to set the change listener for
     * @param listener the new listener to apply
     */
    public void setChangeListener(MeshView view, IScaleChangeListener listener) {
        GizmoMeshViewState state = this.meshViewStates.get(view);
        if (state == null)
            throw new RuntimeException("The provided MeshView '" + view + "' has not been registered to " + Utils.getSimpleName(this) + ", and thus it is not yet possible to set the listener.");

        state.setChangeListener(listener);
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

        // Setup moving data.
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        if (clickedMeshNode == this.xAxisNode) {
            this.movementAxis = Rotate.X_AXIS;
            this.xAxisNode.updateTextureIndex(this.redTextureUvIndex, this.orangeTextureUvIndex);
            this.xAxisNode.updateTextureIndex(this.lightRedTextureUvIndex, this.orangeTextureUvIndex);

            // Set starting distance.
            Scale scale = Scene3DUtils.getOptional3DScale(meshView);
            double xScale = scale != null ? scale.getX() : 1;
            state.setStartDistance(BAR_LENGTH * xScale);
        } else if (clickedMeshNode == this.yAxisNode) {
            this.movementAxis = Rotate.Y_AXIS;
            this.yAxisNode.updateTextureIndex(this.greenTextureUvIndex, this.orangeTextureUvIndex);
            this.yAxisNode.updateTextureIndex(this.lightGreenTextureUvIndex, this.orangeTextureUvIndex);

            // Set starting distance.
            Scale scale = Scene3DUtils.getOptional3DScale(meshView);
            double yScale = scale != null ? scale.getY() : 1;
            state.setStartDistance(BAR_LENGTH * yScale);
        } else if (clickedMeshNode == this.zAxisNode) {
            this.movementAxis = Rotate.Z_AXIS;
            this.zAxisNode.updateTextureIndex(this.blueTextureUvIndex, this.orangeTextureUvIndex);
            this.zAxisNode.updateTextureIndex(this.lightBlueTextureUvIndex, this.orangeTextureUvIndex);

            // Set starting distance.
            Scale scale = Scene3DUtils.getOptional3DScale(meshView);
            double zScale = scale != null ? scale.getZ() : 1;
            state.setStartDistance(BAR_LENGTH * zScale);
        } else {
            // Didn't click one of the axis.
            this.movementAxis = null;
            return;
        }

        // Setup axis plane for mouse-picking.
        this.axisPlane = Scene3DUtils.createAxisPlane(meshView, scene, state.getCamera(), this.movementAxis);

        // Update state.
        state.setDragStartPosition(result.getIntersectedPoint());

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
        if (this.movementAxis == null || state.getDragStartPosition() == null)
            return;

        // Test if there's an intersection point.
        Point3D mousePoint = result.getIntersectedPoint();
        if (mousePoint == null)
            return;

        // Ensure the coordinates we get are now absolute.
        Point3D mouseOffset = mousePoint.subtract(state.getDragStartPosition());

        // Get old position.
        Scale gizmoScale = Scene3DUtils.get3DScale(meshView);
        double oldX = gizmoScale.getX();
        double oldY = gizmoScale.getY();
        double oldZ = gizmoScale.getZ();

        // Update positions.
        if (this.movementAxis == Rotate.X_AXIS) {
            double mouseX = mouseOffset.getX() + state.getStartDistance();
            if (mouseX < BOX_SIZE)
                return;

            double newX = (mouseX - (oldX * BOX_SIZE)) / BAR_LENGTH;
            gizmoScale.setX(newX);
            if (state.getChangeListener() != null)
                state.getChangeListener().handle(meshView, oldX, oldY, oldZ, newX, oldY, oldZ);
        } else if (this.movementAxis == Rotate.Y_AXIS) {
            double mouseY = mouseOffset.getY() - state.getStartDistance();
            if (mouseY > -BOX_SIZE)
                return;

            double newY = -(mouseY + (oldY * BOX_SIZE)) / BAR_LENGTH;
            gizmoScale.setY(newY);
            if (state.getChangeListener() != null)
                state.getChangeListener().handle(meshView, oldX, oldY, oldZ, oldX, newY, oldZ);
        } else if (this.movementAxis == Rotate.Z_AXIS) {
            double mouseZ = mouseOffset.getZ() + state.getStartDistance();
            if (mouseZ < BOX_SIZE)
                return;

            double newZ = (mouseZ - (oldZ * BOX_SIZE)) / BAR_LENGTH;
            gizmoScale.setZ(newZ);
            if (state.getChangeListener() != null)
                state.getChangeListener().handle(meshView, oldX, oldY, oldZ, oldX, oldY, newZ);
        }
    }

    private void onDragStop(MouseEvent event) {
        MeshView meshView = (MeshView) event.getSource();
        stopDragging(meshView);

        // Prevent the drag updates from moving the camera view.
        event.consume();
    }

    private void stopDragging(MeshView meshView) {
        // Remove the axis plane from the scene.
        Group scene = Scene3DUtils.getSubSceneGroup(meshView.getScene().getRoot());
        if (this.axisPlane != null) {
            scene.getChildren().remove(this.axisPlane);
            this.axisPlane = null;
        }

        // Make the arrow be highlighted no longer.
        if (this.movementAxis != null) {
            if (this.movementAxis == Rotate.X_AXIS) {
                this.xAxisNode.updateTextureIndex(this.orangeTextureUvIndex, this.redTextureUvIndex);
            } else if (this.movementAxis == Rotate.Y_AXIS) {
                this.yAxisNode.updateTextureIndex(this.orangeTextureUvIndex, this.greenTextureUvIndex);
            } else if (this.movementAxis == Rotate.Z_AXIS) {
                this.zAxisNode.updateTextureIndex(this.orangeTextureUvIndex, this.blueTextureUvIndex);
            }

            this.movementAxis = null;
        }

        // Update state.
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        state.setDragStartPosition(null);

        // Re-enable dragging on the gizmo.
        meshView.setMouseTransparent(false);
        meshView.setCursor(Cursor.DEFAULT);
    }

    @Getter
    @RequiredArgsConstructor
    private static class GizmoMeshViewState {
        private final MultiViewScaleGizmo gizmo;
        private final MeshView meshView;
        private final List<Box> planes = new ArrayList<>();
        @Setter private Point3D dragStartPosition;
        @Setter private double startDistance;
        @Setter private IScaleChangeListener changeListener;
        @Setter private FirstPersonCamera camera;
    }

    /**
     * Listens for change in scale XYZ.
     */
    public interface IScaleChangeListener {
        /**
         * Handle a scale change.
         * @param meshView the mesh view which updated its scale
         * @param oldX     old x coordinate
         * @param oldY     old y coordinate
         * @param oldZ     old z coordinate
         * @param newX     new x coordinate
         * @param newY     new y coordinate
         * @param newZ     new z coordinate
         */
        void handle(MeshView meshView, double oldX, double oldY, double oldZ, double newX, double newY, double newZ);
    }
}