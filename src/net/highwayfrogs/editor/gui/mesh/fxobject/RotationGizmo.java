package net.highwayfrogs.editor.gui.mesh.fxobject;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.*;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
import net.highwayfrogs.editor.gui.texture.basic.RawColorTextureSource;
import net.highwayfrogs.editor.system.math.Quaternion;
import net.highwayfrogs.editor.system.math.Vector2f;
import net.highwayfrogs.editor.system.math.Vector3f;
import net.highwayfrogs.editor.system.math.Vector4f;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A rotation gizmo is a 3D UI element showing three rings (X=Red, Y=Green, Z=Blue)
 * that the user can click-drag to rotate an object around the corresponding world axis.
 * Created by Kneesnap on 6/12/2026.
 */
@Getter
public class RotationGizmo extends DynamicMesh {
    // Texture sources
    private static final RawColorTextureSource RED_TEXTURE_SOURCE = new RawColorTextureSource(Color.RED);
    private static final RawColorTextureSource GREEN_TEXTURE_SOURCE = new RawColorTextureSource(Color.GREEN);
    private static final RawColorTextureSource BLUE_TEXTURE_SOURCE = new RawColorTextureSource(Color.BLUE);
    private static final RawColorTextureSource LIGHT_RED_TEXTURE_SOURCE = new RawColorTextureSource(0xFF7F1919);
    private static final RawColorTextureSource LIGHT_GREEN_TEXTURE_SOURCE = new RawColorTextureSource(0xFF197F19);
    private static final RawColorTextureSource LIGHT_BLUE_TEXTURE_SOURCE = new RawColorTextureSource(0xFF19197F);
    private static final RawColorTextureSource ORANGE_TEXTURE_SOURCE = new RawColorTextureSource(Color.ORANGE);

    // Ring geometry constants
    private static final int RING_SEGMENTS = 32;
    private static final double RING_RADIUS = 20.0;
    private static final double RING_HALF_THICKNESS = 2.5;
    private static final double RING_INNER = RING_RADIUS - RING_HALF_THICKNESS;
    private static final double RING_OUTER = RING_RADIUS + RING_HALF_THICKNESS;
    private static final double ROTATION_PLANE_SIZE = 10000.0;

    // Transparent material for the rotation plane (invisible, used only for hit-testing during drag)
    private static final javafx.scene.paint.PhongMaterial TRANSPARENT_MATERIAL =
            Scene3DUtils.makeUnlitSharpMaterial(javafx.scene.paint.Color.TRANSPARENT);

    // Change flags
    public static final int X_CHANGED_FLAG = Constants.BIT_FLAG_0;
    public static final int Y_CHANGED_FLAG = Constants.BIT_FLAG_1;
    public static final int Z_CHANGED_FLAG = Constants.BIT_FLAG_2;
    public static final int W_CHANGED_FLAG = Constants.BIT_FLAG_3;
    public static final int FLAG_ACCEPTED_ROTATION = Constants.BIT_FLAG_4;

    // Atlas textures
    private AtlasTexture redTexture;
    private AtlasTexture greenTexture;
    private AtlasTexture blueTexture;
    private AtlasTexture lightRedTexture;
    private AtlasTexture lightGreenTexture;
    private AtlasTexture lightBlueTexture;
    private AtlasTexture orangeTexture;

    // Mesh-level UV indices
    private int redUvIdx = -1;
    private int lightRedUvIdx = -1;
    private int greenUvIdx = -1;
    private int lightGreenUvIdx = -1;
    private int blueUvIdx = -1;
    private int lightBlueUvIdx = -1;
    private int orangeUvIdx = -1;

    // Mesh nodes
    private DynamicMeshUnmanagedNode xAxisNode; // Red ring in YZ plane (rotation around X)
    private DynamicMeshUnmanagedNode yAxisNode; // Green ring in XZ plane (rotation around Y)
    private DynamicMeshUnmanagedNode zAxisNode; // Blue ring in XY plane (rotation around Z)

    // Per-view state
    private final Map<MeshView, GizmoMeshViewState> meshViewStates = new HashMap<>();

    // Drag state (shared; only one drag active at a time)
    private Point3D rotationAxis;         // Which axis is being rotated (X/Y/Z)
    private Box rotationPlane;            // Transparent plane for hit-testing during drag
    private double dragStartAngle;        // Angle (radians) at the previous drag frame (updated each frame for incremental deltas)
    private float origQX, origQY, origQZ, origQW; // Quaternion at drag start (for ESC restore)
    private MeshView draggedMeshView;
    private DynamicMeshUnmanagedNode hoveredNode; // Which ring is currently hovered
    // ESC cancel: we use a Stage-level event FILTER so it fires in the capture phase,
    // before MeshViewController's key handler (which would otherwise exit the 3D view).
    private Stage draggingStage;
    private EventHandler<KeyEvent> escKeyFilter;

    // Rotation axes in the quaternion's coordinate system.
    // Defaults to standard JavaFX world axes; override via setRotationAxes() when the
    // quaternion lives in a different coordinate system (e.g. Great Quest game space).
    private Vector3f rotationXAxis = new Vector3f(1, 0, 0);
    private Vector3f rotationYAxis = new Vector3f(0, 1, 0);
    private Vector3f rotationZAxis = new Vector3f(0, 0, 1);

    public RotationGizmo() {
        super(new SequentialTextureAtlas(32, 32, false), DynamicMeshTextureQuality.LIT_BLURRY);
        setupTextureAtlas();
        setupMesh();
    }

    // =========================================================================
    // Setup
    // =========================================================================

    private void setupTextureAtlas() {
        getTextureAtlas().startBulkOperations();
        this.redTexture = getTextureAtlas().addTexture(RED_TEXTURE_SOURCE);
        this.greenTexture = getTextureAtlas().addTexture(GREEN_TEXTURE_SOURCE);
        this.blueTexture = getTextureAtlas().addTexture(BLUE_TEXTURE_SOURCE);
        this.lightRedTexture = getTextureAtlas().addTexture(LIGHT_RED_TEXTURE_SOURCE);
        this.lightGreenTexture = getTextureAtlas().addTexture(LIGHT_GREEN_TEXTURE_SOURCE);
        this.lightBlueTexture = getTextureAtlas().addTexture(LIGHT_BLUE_TEXTURE_SOURCE);
        this.orangeTexture = getTextureAtlas().addTexture(ORANGE_TEXTURE_SOURCE);
        getTextureAtlas().endBulkOperations();
    }

    private void setupMesh() {
        Vector2f redUv = getTextureAtlas().getUV(this.redTexture, RED_TEXTURE_SOURCE.getUv());
        Vector2f greenUv = getTextureAtlas().getUV(this.greenTexture, GREEN_TEXTURE_SOURCE.getUv());
        Vector2f blueUv = getTextureAtlas().getUV(this.blueTexture, BLUE_TEXTURE_SOURCE.getUv());
        Vector2f lightRedUv = getTextureAtlas().getUV(this.lightRedTexture, LIGHT_RED_TEXTURE_SOURCE.getUv());
        Vector2f lightGreenUv = getTextureAtlas().getUV(this.lightGreenTexture, LIGHT_GREEN_TEXTURE_SOURCE.getUv());
        Vector2f lightBlueUv = getTextureAtlas().getUV(this.lightBlueTexture, LIGHT_BLUE_TEXTURE_SOURCE.getUv());
        Vector2f orangeUv = getTextureAtlas().getUV(this.orangeTexture, ORANGE_TEXTURE_SOURCE.getUv());

        pushBatchUpdates();

        // X-axis ring (Red): lies in the YZ plane, rotating around X
        this.xAxisNode = new DynamicMeshUnmanagedNode(this);
        addNode(this.xAxisNode);
        DynamicMeshDataEntry xEntry = new DynamicMeshDataEntry(this);
        this.lightRedUvIdx = xEntry.addTexCoordValue(lightRedUv);
        this.redUvIdx = xEntry.addTexCoordValue(redUv);
        this.orangeUvIdx = xEntry.addTexCoordValue(orangeUv); // Registered here; valid globally
        addRingYZPlane(xEntry, RING_INNER, RING_OUTER, RING_SEGMENTS, this.redUvIdx);
        this.xAxisNode.addEntry(xEntry);

        // Y-axis ring (Green): lies in the XZ plane, rotating around Y
        this.yAxisNode = new DynamicMeshUnmanagedNode(this);
        addNode(this.yAxisNode);
        DynamicMeshDataEntry yEntry = new DynamicMeshDataEntry(this);
        this.lightGreenUvIdx = yEntry.addTexCoordValue(lightGreenUv);
        this.greenUvIdx = yEntry.addTexCoordValue(greenUv);
        addRingXZPlane(yEntry, RING_INNER, RING_OUTER, RING_SEGMENTS, this.greenUvIdx);
        this.yAxisNode.addEntry(yEntry);

        // Z-axis ring (Blue): lies in the XY plane, rotating around Z
        this.zAxisNode = new DynamicMeshUnmanagedNode(this);
        addNode(this.zAxisNode);
        DynamicMeshDataEntry zEntry = new DynamicMeshDataEntry(this);
        this.lightBlueUvIdx = zEntry.addTexCoordValue(lightBlueUv);
        this.blueUvIdx = zEntry.addTexCoordValue(blueUv);
        addRingXYPlane(zEntry, RING_INNER, RING_OUTER, RING_SEGMENTS, this.blueUvIdx);
        this.zAxisNode.addEntry(zEntry);

        popBatchUpdates();
    }

    // =========================================================================
    // Ring geometry
    // =========================================================================

    /**
     * Adds a flat ring lying in the YZ plane to the entry (used for X-axis rotation).
     * Points on the ring: (0, r*sin(theta), r*cos(theta))
     */
    private static void addRingYZPlane(DynamicMeshDataEntry entry, double inner, double outer, int segments, int uvIdx) {
        for (int i = 0; i < segments; i++) {
            double t1 = (2.0 * Math.PI * i) / segments;
            double t2 = (2.0 * Math.PI * (i + 1)) / segments;
            float inY1 = (float) (inner * Math.sin(t1)), inZ1 = (float) (inner * Math.cos(t1));
            float inY2 = (float) (inner * Math.sin(t2)), inZ2 = (float) (inner * Math.cos(t2));
            float ouY1 = (float) (outer * Math.sin(t1)), ouZ1 = (float) (outer * Math.cos(t1));
            float ouY2 = (float) (outer * Math.sin(t2)), ouZ2 = (float) (outer * Math.cos(t2));
            int v0 = entry.addVertexValue(0, inY1, inZ1);
            int v1 = entry.addVertexValue(0, inY2, inZ2);
            int v2 = entry.addVertexValue(0, ouY2, ouZ2);
            int v3 = entry.addVertexValue(0, ouY1, ouZ1);
            entry.addFace(v0, uvIdx, v2, uvIdx, v1, uvIdx);
            entry.addFace(v0, uvIdx, v3, uvIdx, v2, uvIdx);
        }
    }

    /**
     * Adds a flat ring lying in the XZ plane to the entry (used for Y-axis rotation).
     * Points on the ring: (r*cos(theta), 0, r*sin(theta))
     */
    private static void addRingXZPlane(DynamicMeshDataEntry entry, double inner, double outer, int segments, int uvIdx) {
        for (int i = 0; i < segments; i++) {
            double t1 = (2.0 * Math.PI * i) / segments;
            double t2 = (2.0 * Math.PI * (i + 1)) / segments;
            float inX1 = (float) (inner * Math.cos(t1)), inZ1 = (float) (inner * Math.sin(t1));
            float inX2 = (float) (inner * Math.cos(t2)), inZ2 = (float) (inner * Math.sin(t2));
            float ouX1 = (float) (outer * Math.cos(t1)), ouZ1 = (float) (outer * Math.sin(t1));
            float ouX2 = (float) (outer * Math.cos(t2)), ouZ2 = (float) (outer * Math.sin(t2));
            int v0 = entry.addVertexValue(inX1, 0, inZ1);
            int v1 = entry.addVertexValue(inX2, 0, inZ2);
            int v2 = entry.addVertexValue(ouX2, 0, ouZ2);
            int v3 = entry.addVertexValue(ouX1, 0, ouZ1);
            entry.addFace(v0, uvIdx, v2, uvIdx, v1, uvIdx);
            entry.addFace(v0, uvIdx, v3, uvIdx, v2, uvIdx);
        }
    }

    /**
     * Adds a flat ring lying in the XY plane to the entry (used for Z-axis rotation).
     * Points on the ring: (r*cos(theta), r*sin(theta), 0)
     */
    private static void addRingXYPlane(DynamicMeshDataEntry entry, double inner, double outer, int segments, int uvIdx) {
        for (int i = 0; i < segments; i++) {
            double t1 = (2.0 * Math.PI * i) / segments;
            double t2 = (2.0 * Math.PI * (i + 1)) / segments;
            float inX1 = (float) (inner * Math.cos(t1)), inY1 = (float) (inner * Math.sin(t1));
            float inX2 = (float) (inner * Math.cos(t2)), inY2 = (float) (inner * Math.sin(t2));
            float ouX1 = (float) (outer * Math.cos(t1)), ouY1 = (float) (outer * Math.sin(t1));
            float ouX2 = (float) (outer * Math.cos(t2)), ouY2 = (float) (outer * Math.sin(t2));
            int v0 = entry.addVertexValue(inX1, inY1, 0);
            int v1 = entry.addVertexValue(inX2, inY2, 0);
            int v2 = entry.addVertexValue(ouX2, ouY2, 0);
            int v3 = entry.addVertexValue(ouX1, ouY1, 0);
            entry.addFace(v0, uvIdx, v2, uvIdx, v1, uvIdx);
            entry.addFace(v0, uvIdx, v3, uvIdx, v2, uvIdx);
        }
    }

    // =========================================================================
    // View management
    // =========================================================================

    /**
     * Adds a MeshView with full controller context and a rotation change listener.
     * @param view       the MeshView to register
     * @param controller the mesh view controller
     * @param listener   called whenever the rotation changes
     */
    public void addView(MeshView view, MeshViewController<?> controller, IRotationChangeListener listener) {
        addView(view, controller.getMeshTracker());
        GizmoMeshViewState state = this.meshViewStates.get(view);
        state.setController(controller);
        state.setChangeListener(listener);
    }

    @Override
    public boolean addView(MeshView view, MeshTracker meshTracker) {
        if (!super.addView(view, meshTracker))
            return false;

        view.setCullFace(CullFace.NONE); // Show both sides of each ring
        this.meshViewStates.put(view, new GizmoMeshViewState(this, view));

        view.setOnMouseEntered(this::onMouseEnter);
        view.setOnMouseMoved(this::onMouseMove);
        view.setOnMouseExited(this::onMouseExit);
        view.setOnMousePressed(this::onDragStart);
        view.setOnMouseDragged(this::onDragUpdate);
        view.setOnMouseReleased(this::onDragStop);
        return true;
    }

    @Override
    public boolean removeView(MeshView view) {
        stopDragging(view, false);

        if (!super.removeView(view))
            return false;

        view.setCullFace(CullFace.BACK); // Restore default
        view.setOnMouseEntered(null);
        view.setOnMouseMoved(null);
        view.setOnMouseExited(null);
        view.setOnMousePressed(null);
        view.setOnMouseDragged(null);
        view.setOnMouseReleased(null);

        this.meshViewStates.remove(view);
        return true;
    }

    /**
     * Updates the rotation change listener for the given view.
     * Only valid after {@link #addView} has been called.
     */
    public void setChangeListener(MeshView view, IRotationChangeListener listener) {
        GizmoMeshViewState state = this.meshViewStates.get(view);
        if (state == null)
            throw new RuntimeException("MeshView '" + view + "' is not registered to this RotationGizmo.");
        state.setChangeListener(listener);
    }

    // =========================================================================
    // Rotation state
    // =========================================================================

    /**
     * Gets the quaternion X component stored for this view.
     */
    public float getRotationX(MeshView meshView) {
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        return state != null ? state.quatX : 0f;
    }

    /**
     * Gets the quaternion Y component stored for this view.
     */
    public float getRotationY(MeshView meshView) {
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        return state != null ? state.quatY : 0f;
    }

    /**
     * Gets the quaternion Z component stored for this view.
     */
    public float getRotationZ(MeshView meshView) {
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        return state != null ? state.quatZ : 0f;
    }

    /**
     * Gets the quaternion W component stored for this view.
     */
    public float getRotationW(MeshView meshView) {
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        return state != null ? state.quatW : 1f;
    }

    /**
     * Sets the rotation quaternion for the given view.
     * Does NOT visually rotate the gizmo mesh itself — the rings always align to world axes.
     * @param meshView  the view to update
     * @param qx        quaternion X
     * @param qy        quaternion Y
     * @param qz        quaternion Z
     * @param qw        quaternion W
     * @param fireEvent whether to notify the change listener
     */
    public void setRotation(MeshView meshView, float qx, float qy, float qz, float qw, boolean fireEvent) {
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        if (state == null)
            return;

        float oldX = state.quatX, oldY = state.quatY, oldZ = state.quatZ, oldW = state.quatW;
        state.quatX = qx;
        state.quatY = qy;
        state.quatZ = qz;
        state.quatW = qw;

        if (fireEvent && state.getChangeListener() != null)
            state.getChangeListener().handle(meshView, oldX, oldY, oldZ, oldW, qx, qy, qz, qw,
                    X_CHANGED_FLAG | Y_CHANGED_FLAG | Z_CHANGED_FLAG | W_CHANGED_FLAG);
    }

    /**
     * Sets the 3D world-space position where this gizmo appears.
     * @param meshView the view to position
     * @param x        world X
     * @param y        world Y
     * @param z        world Z
     */
    public void setPosition(MeshView meshView, double x, double y, double z) {
        Translate t = Scene3DUtils.get3DTranslation(meshView);
        t.setX(x);
        t.setY(y);
        t.setZ(z);
    }

    /**
     * Overrides the rotation axes used in {@link #applyDeltaRotation}.
     * Use this when the quaternion being edited lives in a coordinate system that differs
     * from the gizmo's visual (JavaFX world) space.  For example, if the quaternion is in
     * game space where JavaFX-Y maps to game-Z, set:
     * <pre>  setRotationAxes(new Vector3f(1,0,0), new Vector3f(0,0,1), new Vector3f(0,-1,0));</pre>
     * @param xAxis game-space axis corresponding to the red (X) ring
     * @param yAxis game-space axis corresponding to the green (Y) ring
     * @param zAxis game-space axis corresponding to the blue (Z) ring
     */
    public void setRotationAxes(Vector3f xAxis, Vector3f yAxis, Vector3f zAxis) {
        this.rotationXAxis = xAxis;
        this.rotationYAxis = yAxis;
        this.rotationZAxis = zAxis;
    }

    /**
     * Whether a drag is currently in progress.
     */
    public boolean isDraggingActive() {
        return this.rotationAxis != null && this.rotationPlane != null;
    }

    // =========================================================================
    // Hover highlight
    // =========================================================================

    private DynamicMeshUnmanagedNode getNodeForFaceIndex(int faceIndex) {
        if (faceIndex < 0)
            return null;
        DynamicMeshDataEntry entry = getDataEntryByFaceIndex(faceIndex);
        if (entry == null)
            return null;
        DynamicMeshNode node = entry.getMeshNode();
        if (node == this.xAxisNode) return this.xAxisNode;
        if (node == this.yAxisNode) return this.yAxisNode;
        if (node == this.zAxisNode) return this.zAxisNode;
        return null;
    }

    private void applyHoverHighlight(DynamicMeshUnmanagedNode node, boolean highlight) {
        if (node == null) return;
        if (node == this.xAxisNode) {
            if (highlight)
                node.updateTextureIndex(this.redUvIdx, this.lightRedUvIdx);
            else
                node.updateTextureIndex(this.lightRedUvIdx, this.redUvIdx);
        } else if (node == this.yAxisNode) {
            if (highlight)
                node.updateTextureIndex(this.greenUvIdx, this.lightGreenUvIdx);
            else
                node.updateTextureIndex(this.lightGreenUvIdx, this.greenUvIdx);
        } else if (node == this.zAxisNode) {
            if (highlight)
                node.updateTextureIndex(this.blueUvIdx, this.lightBlueUvIdx);
            else
                node.updateTextureIndex(this.lightBlueUvIdx, this.blueUvIdx);
        }
    }

    private void updateHoverHighlight(MouseEvent event) {
        DynamicMeshUnmanagedNode newNode = getNodeForFaceIndex(event.getPickResult().getIntersectedFace());
        if (newNode == this.hoveredNode)
            return;

        applyHoverHighlight(this.hoveredNode, false);
        this.hoveredNode = newNode;
        applyHoverHighlight(this.hoveredNode, true);
    }

    private void clearHoverHighlight() {
        applyHoverHighlight(this.hoveredNode, false);
        this.hoveredNode = null;
    }

    // =========================================================================
    // Mouse event handlers
    // =========================================================================

    private void onMouseEnter(MouseEvent event) {
        if (!event.isPrimaryButtonDown())
            updateHoverHighlight(event);
    }

    private void onMouseMove(MouseEvent event) {
        if (!event.isPrimaryButtonDown())
            updateHoverHighlight(event);
    }

    private void onMouseExit(MouseEvent event) {
        if (!event.isPrimaryButtonDown())
            clearHoverHighlight();
    }

    private void onDragStart(MouseEvent event) {
        MeshView meshView = (MeshView) event.getSource();
        Group scene = Scene3DUtils.getSubSceneGroup(meshView.getScene().getRoot());

        // Remove any stale rotation plane
        if (this.rotationPlane != null) {
            scene.getChildren().remove(this.rotationPlane);
            this.rotationPlane = null;
        }

        // Identify which ring was clicked
        PickResult result = event.getPickResult();
        DynamicMeshUnmanagedNode clickedNode = getNodeForFaceIndex(result.getIntersectedFace());
        if (clickedNode == null)
            return;

        // Set rotation axis and turn the ring orange
        if (clickedNode == this.xAxisNode) {
            this.rotationAxis = Rotate.X_AXIS;
            clickedNode.updateTextureIndex(this.redUvIdx, this.orangeUvIdx);
            clickedNode.updateTextureIndex(this.lightRedUvIdx, this.orangeUvIdx);
        } else if (clickedNode == this.yAxisNode) {
            this.rotationAxis = Rotate.Y_AXIS;
            clickedNode.updateTextureIndex(this.greenUvIdx, this.orangeUvIdx);
            clickedNode.updateTextureIndex(this.lightGreenUvIdx, this.orangeUvIdx);
        } else { // zAxisNode
            this.rotationAxis = Rotate.Z_AXIS;
            clickedNode.updateTextureIndex(this.blueUvIdx, this.orangeUvIdx);
            clickedNode.updateTextureIndex(this.lightBlueUvIdx, this.orangeUvIdx);
        }

        // Store original quaternion so we can restore it on ESC
        GizmoMeshViewState state = this.meshViewStates.get(meshView);
        this.origQX = state.quatX;
        this.origQY = state.quatY;
        this.origQZ = state.quatZ;
        this.origQW = state.quatW;

        // Create the invisible plane for hit-testing during drag (also blocks camera events)
        this.rotationPlane = createRotationPlane(meshView, scene);

        // Compute start angle from the initial click position in screen space.
        // Screen-space projection works at any camera angle, including when the camera ray is
        // nearly parallel to the rotation plane (which would cause 3D plane intersection to fail).
        this.dragStartAngle = computeAngleFromScreenPosition(meshView, event.getScreenX(), event.getScreenY());

        // Register a Stage-level ESC filter to cancel the drag.
        // A Stage filter fires during capture phase, before MeshViewController's key handler,
        // so it prevents ESC from exiting the 3D view while a rotation drag is active.
        this.draggedMeshView = meshView;
        this.hoveredNode = null;
        this.draggingStage = state.getController() != null ? state.getController().getOverwrittenStage() : null;
        if (this.draggingStage != null) {
            this.escKeyFilter = evt -> {
                if (evt.getCode() == KeyCode.ESCAPE) {
                    evt.consume();
                    stopDragging(this.draggedMeshView, true);
                }
            };
            this.draggingStage.addEventFilter(KeyEvent.KEY_PRESSED, this.escKeyFilter);
        }

        // Make the gizmo transparent so only the plane receives drag events
        meshView.setMouseTransparent(true);
        meshView.setCursor(Cursor.CROSSHAIR);
        event.consume();
    }

    private void onDragUpdate(MouseEvent event) {
        if (!isDraggingActive())
            return;

        event.consume();

        MeshView meshView = (MeshView) event.getSource();
        GizmoMeshViewState state = this.meshViewStates.get(meshView);

        double currentAngle = computeAngleFromScreenPosition(meshView, event.getScreenX(), event.getScreenY());

        // Compute incremental delta from the previous frame's angle.
        // Using an incremental delta (rather than total from drag-start) allows:
        //   1. Rotations beyond 180° (no clamp at the atan2 [-π, π] boundary)
        //   2. Correct handling of ±π wraparound without large jumps
        double deltaAngle = currentAngle - this.dragStartAngle;
        // Normalize to [-π, π] so crossing the ±π atan2 boundary doesn't produce a ~2π jump
        if (deltaAngle > Math.PI) deltaAngle -= 2 * Math.PI;
        if (deltaAngle < -Math.PI) deltaAngle += 2 * Math.PI;

        this.dragStartAngle = currentAngle; // advance reference for the next frame

        applyDeltaRotation(meshView, state, deltaAngle);
    }

    private void onDragStop(MouseEvent event) {
        MeshView meshView = (MeshView) event.getSource();
        if (isDraggingActive()) {
            event.consume();
            stopDragging(meshView, false);
        }
    }

    // =========================================================================
    // Drag helpers
    // =========================================================================

    /**
     * Creates a large transparent plane perpendicular to the active rotation axis.
     * Used for mouse hit-testing during drag.
     */
    private Box createRotationPlane(MeshView meshView, Group scene) {
        double ps = ROTATION_PLANE_SIZE;
        Box plane;
        if (this.rotationAxis == Rotate.X_AXIS)
            plane = new Box(0, ps, ps); // YZ plane
        else if (this.rotationAxis == Rotate.Y_AXIS)
            plane = new Box(ps, 0, ps); // XZ plane
        else
            plane = new Box(ps, ps, 0); // XY plane

        plane.setMaterial(TRANSPARENT_MATERIAL);
        plane.setDepthTest(DepthTest.DISABLE);

        Translate translate = Scene3DUtils.getOptional3DTranslation(meshView);
        if (translate != null)
            plane.getTransforms().add(translate);

        scene.getChildren().add(plane);
        return plane;
    }

    /**
     * Computes the angle (in radians) of the mouse screen position relative to the gizmo center.
     * <p>
     * We simply measure {@code atan2(my, mx)} — the angle of the mouse offset around the gizmo
     * center in plain screen space. This is robust at any camera angle because it never relies on
     * projecting a 3D depth axis (+Z) to the screen, which would collapse to a near-zero vector
     * when the camera looks roughly along −Z and cause degenerate, jumpy angle computations.
     * The actual rotation axis is handled separately by {@link #applyDeltaRotation}; all this
     * method cares about is "how far did the mouse sweep around the center."
     *
     * @param gizmo   the gizmo MeshView (used to find the screen-space center)
     * @param screenX screen X coordinate of the mouse
     * @param screenY screen Y coordinate of the mouse
     * @return the angle in radians, in the range (−π, π]
     */
    private double computeAngleFromScreenPosition(MeshView gizmo, double screenX, double screenY) {
        javafx.geometry.Point2D center = gizmo.localToScreen(0, 0, 0);
        return Math.atan2(screenY - center.getY(), screenX - center.getX());
    }

    /**
     * Applies a delta rotation around the active axis to the stored quaternion.
     * The axis vector is taken from {@link #rotationXAxis}/{@link #rotationYAxis}/{@link #rotationZAxis}
     * so callers can supply game-space axes when the quaternion lives in a different coordinate
     * system than the visual gizmo.
     */
    private void applyDeltaRotation(MeshView meshView, GizmoMeshViewState state, double deltaAngle) {
        // Build axis vector in the quaternion's coordinate system
        Vector3f axis;
        if (this.rotationAxis == Rotate.X_AXIS)
            axis = this.rotationXAxis;
        else if (this.rotationAxis == Rotate.Y_AXIS)
            axis = this.rotationYAxis;
        else
            axis = this.rotationZAxis;

        // Create delta quaternion representing the incremental rotation
        Vector4f deltaQuat = Quaternion.fromAxisAngle(axis, (float) deltaAngle);

        // Compose with the CURRENT quaternion (incremental delta applied each frame).
        // Using the current quaternion (rather than origQuat) enables full 360° rotations:
        // each frame's small delta is accumulated, so there is no absolute-angle ceiling.
        // origQuat is still stored so ESC can restore the pre-drag state.
        Vector4f currentQuat = new Vector4f(state.quatX, state.quatY, state.quatZ, state.quatW);
        Vector4f newQuat = Quaternion.multiply(currentQuat, deltaQuat);
        newQuat.normalise();

        float oldX = state.quatX, oldY = state.quatY, oldZ = state.quatZ, oldW = state.quatW;
        state.quatX = newQuat.getX();
        state.quatY = newQuat.getY();
        state.quatZ = newQuat.getZ();
        state.quatW = newQuat.getW();

        if (state.getChangeListener() != null
                && (oldX != state.quatX || oldY != state.quatY || oldZ != state.quatZ || oldW != state.quatW))
            state.getChangeListener().handle(meshView, oldX, oldY, oldZ, oldW,
                    state.quatX, state.quatY, state.quatZ, state.quatW,
                    X_CHANGED_FLAG | Y_CHANGED_FLAG | Z_CHANGED_FLAG | W_CHANGED_FLAG);
    }

    /**
     * Ends the current drag, optionally restoring the original quaternion.
     * @param meshView           the view being dragged (may be null if no drag is active)
     * @param restoreOriginal    true to cancel and restore the pre-drag quaternion
     */
    private void stopDragging(MeshView meshView, boolean restoreOriginal) {
        if (meshView == null)
            return;

        // Remove the Stage ESC filter
        if (this.escKeyFilter != null && this.draggingStage != null) {
            this.draggingStage.removeEventFilter(KeyEvent.KEY_PRESSED, this.escKeyFilter);
            this.escKeyFilter = null;
            this.draggingStage = null;
        }

        GizmoMeshViewState state = this.meshViewStates.get(meshView);

        // Remove rotation plane
        if (this.rotationPlane != null) {
            Group scene = Scene3DUtils.getSubSceneGroup(this.rotationPlane.getScene());
            if (scene != null)
                scene.getChildren().remove(this.rotationPlane);
            this.rotationPlane = null;
        }

        // Restore ring color and optionally restore quaternion
        if (this.rotationAxis != null) {
            if (this.rotationAxis == Rotate.X_AXIS)
                this.xAxisNode.updateTextureIndex(this.orangeUvIdx, this.redUvIdx);
            else if (this.rotationAxis == Rotate.Y_AXIS)
                this.yAxisNode.updateTextureIndex(this.orangeUvIdx, this.greenUvIdx);
            else
                this.zAxisNode.updateTextureIndex(this.orangeUvIdx, this.blueUvIdx);

            if (restoreOriginal && state != null) {
                float oldX = state.quatX, oldY = state.quatY, oldZ = state.quatZ, oldW = state.quatW;
                state.quatX = this.origQX;
                state.quatY = this.origQY;
                state.quatZ = this.origQZ;
                state.quatW = this.origQW;
                if (state.getChangeListener() != null)
                    state.getChangeListener().handle(meshView, oldX, oldY, oldZ, oldW,
                            state.quatX, state.quatY, state.quatZ, state.quatW,
                            X_CHANGED_FLAG | Y_CHANGED_FLAG | Z_CHANGED_FLAG | W_CHANGED_FLAG);
            } else if (!restoreOriginal && state != null && state.getChangeListener() != null) {
                state.getChangeListener().handle(meshView, this.origQX, this.origQY, this.origQZ, this.origQW,
                        state.quatX, state.quatY, state.quatZ, state.quatW, FLAG_ACCEPTED_ROTATION);
            }

            this.rotationAxis = null;
        }

        this.dragStartAngle = 0;
        this.draggedMeshView = null;

        meshView.setMouseTransparent(false);
        meshView.setCursor(Cursor.DEFAULT);
    }

    // =========================================================================
    // Inner classes
    // =========================================================================

    @Getter
    @RequiredArgsConstructor
    private static class GizmoMeshViewState {
        private final RotationGizmo gizmo;
        private final MeshView meshView;
        @Setter private IRotationChangeListener changeListener;
        @Setter private MeshViewController<?> controller;
        float quatX = 0f, quatY = 0f, quatZ = 0f, quatW = 1f; // identity quaternion
    }

    /**
     * Listener interface for rotation changes produced by a {@link RotationGizmo}.
     */
    public interface IRotationChangeListener {
        /**
         * Called whenever the gizmo rotation changes.
         * @param meshView the view being edited
         * @param oldX     previous quaternion X
         * @param oldY     previous quaternion Y
         * @param oldZ     previous quaternion Z
         * @param oldW     previous quaternion W
         * @param newX     new quaternion X
         * @param newY     new quaternion Y
         * @param newZ     new quaternion Z
         * @param newW     new quaternion W
         * @param flags    change flags (see X_CHANGED_FLAG etc.)
         */
        void handle(MeshView meshView, float oldX, float oldY, float oldZ, float oldW,
                    float newX, float newY, float newZ, float newW, int flags);
    }
}
