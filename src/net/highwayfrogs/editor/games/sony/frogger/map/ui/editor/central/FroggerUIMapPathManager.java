package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPath;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathInfo.FroggerPathMotionType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseHandler;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.MeshViewFrameTimer.MeshViewFixedFrameRateTimerTask;
import net.highwayfrogs.editor.gui.editor.SelectionPromptTracker;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Allows editing Frogger map paths.
 * TODO: Allow calculating position and rotation independently, more closely to how the old Frogger system does it? Not sure.
 * TODO: We should allow dragging an entity along the path in 3D space. Eg: Click and hold will let you drag, by finding nearby path parts
 * TODO: Go over path speed. What kind of unit is this? Create separate editor grid functions for path speed which accurately mimic the behavior. Update entity data, units, etc.
 * TODO: Allow selecting individual segments instead of showing them all at once, and highlight them.
 * TODO: Can I make a 3D path editor soon? I bet it's doable.
 * TODO: Consider path speed to be part of the path itself instead of per-entity. (Add validation that all entities on a path follow the true path speed, still allow speed editing on each individual entity, just have it impact the underlying path. If there is none, disable/hide the field.)
 * Created by Kneesnap on 6/2/2024.
 */
public class FroggerUIMapPathManager extends FroggerCentralMapListManager<FroggerPath, FroggerPathPreview> {
    @Getter private final PathSelectionTracker pathSelector;
    private final Map<Node, FroggerPathSegmentPreview> previewsByLargerClickAreas = new HashMap<>();
    @Getter private final Map<FroggerMapEntity, FroggerPathInfo> pathRunnerPreviews = new HashMap<>();
    private DisplayList pathDisplayList;
    @Getter private TextField fullPathLengthField;
    private CheckBox tickPathRunners;

    private static final PhongMaterial MATERIAL_INVISIBLE = Scene3DUtils.makeUnlitSharpMaterial(Color.rgb(255, 255, 255, 0));
    private static final PhongMaterial MATERIAL_WHITE = Scene3DUtils.makeUnlitSharpMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_LIGHT_GREEN = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial MATERIAL_BLUE = Scene3DUtils.makeUnlitSharpMaterial(Color.BLUE);
    private static final PhongMaterial MATERIAL_GREEN = Scene3DUtils.makeUnlitSharpMaterial(Color.GREEN);
    private static final PhongMaterial MATERIAL_PURPLE = Scene3DUtils.makeUnlitSharpMaterial(Color.PURPLE);
    private static final int MOUSE_PATH_DISTANCE_THRESHOLD = 20;

    public FroggerUIMapPathManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
        this.pathSelector = new PathSelectionTracker(this);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        this.pathDisplayList = getRenderManager().createDisplayListWithNewGroup();
        getFrameTimer(getGameInstance().getFPS()).addTask(1, this, FroggerUIMapPathManager::tickPathRunnerPreviews);
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
        this.tickPathRunners = getMainGrid().addCheckBox("Tick Path Runners", false, this::onTickPathRunnerCheckBoxUpdate);
        this.fullPathLengthField = getMainGrid().addFloatField("Full Path Length", 0, null, null);
        this.fullPathLengthField.setDisable(true);
    }

    @Override
    public String getTitle() {
        return "Paths";
    }

    @Override
    public String getValueName() {
        return "Path";
    }

    @Override
    public List<FroggerPath> getValues() {
        return getMap().getPathPacket().getPaths();
    }

    @Override
    protected boolean tryRemoveValue(FroggerPath path) {
        // Count entities on path.
        int pathId = path.getPathIndex();
        int pathEntityCount = 0;
        for (FroggerMapEntity entity : getMap().getEntityPacket().getEntities())
            if (entity.getPathInfo() != null && entity.getPathInfo().getPathId() == pathId)
                pathEntityCount++;

        if (pathEntityCount > 0) {
            FXUtils.makePopUp(pathEntityCount + (pathEntityCount == 1 ? " entity was" : " entities were") + " using the path, so it cannot be removed.", AlertType.WARNING);
            return false;
        }

        getMap().getPathPacket().removePath(path);
        return true;
    }

    @Override
    public boolean isValueVisibleByUI(FroggerPath path) {
        return this.pathSelector.isPromptActive() || super.isValueVisibleByUI(path);
    }

    @Override
    protected FroggerPathPreview setupDisplay(FroggerPath path) {
        return new FroggerPathPreview(this, path);
    }

    @Override
    protected void updateEditor(FroggerPath selectedPath) {
        getEditorGrid().addBoldLabel("Path Data");
        selectedPath.setupEditor(getDelegatesByValue().get(selectedPath), getEditorGrid());
    }

    @Override
    protected FroggerPath createNewValue() {
        return new FroggerPath(getMap());
    }

    @Override
    protected void onDelegateRemoved(FroggerPath path, FroggerPathPreview pathPreview) {
        pathPreview.removePathNodes();
    }

    @Override
    protected void onSelectedValueChange(FroggerPath oldPath, FroggerPathPreview oldPathPreview, FroggerPath newPath, FroggerPathPreview newPathPreview) {
        this.fullPathLengthField.setText(newPath != null ? Float.toString(newPath.calculateTotalLengthFloat()) : "No Path");
    }

    @Override
    protected void setVisible(FroggerPath path, FroggerPathPreview pathPreview, boolean visible) {
        pathPreview.setVisible(visible);
    }

    private void onTickPathRunnerCheckBoxUpdate(boolean enablePathRunnerPreview) {
        FroggerUIMapEntityManager entityManager = getController().getEntityManager();

        if (enablePathRunnerPreview) {
            getEditorGrid().getGridPane().setDisable(true);
            entityManager.getEditorGrid().getGridPane().setDisable(true);
            return;
        }

        // Enable UI once again.
        getEditorGrid().getGridPane().setDisable(false);
        entityManager.getEditorGrid().getGridPane().setDisable(false);

        // Reset path runner stuff.
        this.pathRunnerPreviews.clear();
        List<FroggerPath> paths = getValues();
        for (int i = 0; i < paths.size(); i++) {
            FroggerPath path = paths.get(i);
            for (int j = 0; j < path.getPathEntities().size(); j++)
                entityManager.updateEntityPositionRotation(path.getPathEntities().get(j));
        }
    }

    /**
     * Updates the polygon mesh data for polygons with animations.
     */
    public void tickPathRunnerPreviews(MeshViewFixedFrameRateTimerTask<?> timerTask) {
        if (!this.tickPathRunners.isSelected())
            return;

        FroggerUIMapEntityManager entityManager = getController().getEntityManager();
        List<FroggerPath> paths = getValues();
        int deltaFrames = timerTask.getDeltaFrames();
        for (int i = 0; i < paths.size(); i++) {
            FroggerPath path = paths.get(i);
            for (int j = 0; j < path.getPathEntities().size(); j++) {
                FroggerMapEntity pathEntity = path.getPathEntities().get(j);
                FroggerPathInfo pathInfo = this.pathRunnerPreviews.computeIfAbsent(pathEntity, entity -> entity.getPathInfo() != null ? entity.getPathInfo().clone() : null);
                if (pathInfo == null || pathInfo.testFlag(FroggerPathMotionType.FINISHED))
                    continue;

                int distanceMoved = deltaFrames * pathInfo.getSpeed();
                if (pathInfo.testFlag(FroggerPathMotionType.BACKWARDS)) {
                    pathInfo.setTotalPathDistance(pathInfo.getTotalPathDistance() - distanceMoved, true);
                } else {
                    pathInfo.setTotalPathDistance(pathInfo.getTotalPathDistance() + distanceMoved, true);
                }

                entityManager.updateEntityPositionRotation(pathEntity);
            }
        }
    }


    /**
     * Represents a 3D path preview.
     */
    @Getter
    @RequiredArgsConstructor
    public static class FroggerPathPreview {
        private final FroggerUIMapPathManager pathManager;
        private final FroggerPath path;
        private final List<Node> nodes = new ArrayList<>();
        private final List<FroggerPathSegmentPreview> segmentPreviews = new ArrayList<>();
        @Setter private TextField pathSegmentLengthField;

        private static final int SEGMENT_STEP_SIZE = 32;

        /**
         * Gets the mesh controller.
         */
        public FroggerMapMeshController getController() {
            return this.pathManager.getController();
        }

        /**
         * Removes the path nodes from existence.
         */
        public void removePathNodes() {
            if (this.nodes.isEmpty())
                return;

            this.pathManager.pathDisplayList.removeAll(this.nodes);
            this.nodes.clear();
            for (int i = 0; i < this.segmentPreviews.size(); i++)
                this.pathManager.previewsByLargerClickAreas.remove(this.segmentPreviews.get(i).getInvisibleLargerCylinder());
            this.segmentPreviews.clear();
        }

        /**
         * Sets whether the path preview is visible.
         * @param visible whether the path preview is visible
         */
        public void setVisible(boolean visible) {
            if (!visible) {
                removePathNodes();
                return;
            }

            if (this.pathManager.getPathSelector().isPromptActive()) {
                addPath(new FroggerPathInfo(this.pathManager.getMap()), MATERIAL_BLUE, MATERIAL_GREEN, MATERIAL_PURPLE);
            } else {
                addPath(new FroggerPathInfo(this.pathManager.getMap()), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);
            }
        }

        /**
         * Update the path display.
         */
        public void updatePath() {
            setVisible(this.pathManager.isValueVisibleByUI(this.path));
        }

        /**
         * Generates the 3D nodes used to preview the path.
         * @param pathInfo The dummy path state object to use to calculate path information.
         * @param materialLine The material to use for rendering line segments.
         * @param materialArc The material to use for rendering arc segments.
         * @param materialSpline The material to use for rendering spline segments.
         */
        public void addPath(FroggerPathInfo pathInfo, PhongMaterial materialLine, PhongMaterial materialArc, PhongMaterial materialSpline) {
            removePathNodes(); // Ensure any earlier path nodes are gone.

            int maxSegmentLength = SEGMENT_STEP_SIZE;
            if (this.pathManager.getValueDisplaySetting().getValue() == ListDisplayType.ALL || this.pathManager.pathSelector.isPromptActive())
                maxSegmentLength *= 2;

            // Add preview nodes for each segment.
            pathInfo.setPathId(this.path.getPathIndex());
            for (int segmentIndex = 0; segmentIndex < this.path.getSegments().size(); segmentIndex++) {
                FroggerPathSegment segment = this.path.getSegments().get(segmentIndex);
                pathInfo.setSegmentId(segmentIndex);

                // Choose material.
                PhongMaterial material = materialSpline;
                if (segment.getType() == FroggerPathSegmentType.LINE)
                    material = materialLine;
                if (segment.getType() == FroggerPathSegmentType.ARC)
                    material = materialArc;

                // Handle if the segment is empty.
                final int stepSize = Math.min(maxSegmentLength, segment.getLength());
                if (stepSize == 0) { // If the path is 100% empty, just show the start.
                    Vector pos = segment.getStartPosition();
                    addPathLineSegment(pos.getFloatX(), pos.getFloatY(), pos.getFloatZ(), pos.getFloatX(), pos.getFloatY(), pos.getFloatZ(), .2, material, true, segment, 0);
                    continue;
                }

                final int numSteps = 1 + (segment.getLength() / stepSize);
                for (int step = 0; step < numSteps; ++step) {
                    pathInfo.setSegmentDistance(step * stepSize);
                    Vector v0 = this.path.evaluatePosition(pathInfo).getPosition();

                    pathInfo.setSegmentDistance(Math.min((step + 1) * stepSize, segment.getLength()));
                    Vector v1 = this.path.evaluatePosition(pathInfo).getPosition();
                    if (!v0.equals(v1))
                        addPathLineSegment(v0.getFloatX(), v0.getFloatY(), v0.getFloatZ(), v1.getFloatX(), v1.getFloatY(), v1.getFloatZ(), 0.20,
                                material, step == 0, segment, (step * stepSize) + (stepSize / 2));
                }
            }
        }

        /**
         * Adds a cylindrical representation of a 3D line.
         * @param x0        The x-coordinate defining the start of the line segment.
         * @param y0        The y-coordinate defining the start of the line segment.
         * @param z0        The z-coordinate defining the start of the line segment.
         * @param x1        The x-coordinate defining the end of the line segment.
         * @param y1        The y-coordinate defining the end of the line segment.
         * @param z1        The z-coordinate defining the end of the line segment.
         * @param radius    The radius of the cylinder (effectively the 'width' of the line).
         * @param material  The material used to render the line segment.
         * @param showStart Whether to display a sphere at the start of the line segment.
         * @return The newly created/added cylinder (cylinder primitive only!)
         */
        private FroggerPathSegmentPreview addPathLineSegment(double x0, double y0, double z0, double x1, double y1, double z1, double radius, PhongMaterial material, boolean showStart, FroggerPathSegment segment, int segDistance) {
            final Point3D yAxis = new Point3D(0.0, 1.0, 0.0);
            final Point3D p0 = new Point3D(x0, y0, z0);
            final Point3D p1 = new Point3D(x1, y1, z1);
            final Point3D diff = p1.subtract(p0);
            final double length = diff.magnitude();

            final Point3D mid = p1.midpoint(p0);
            final Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

            final Point3D axisOfRotation = diff.crossProduct(yAxis);
            final double angle = Math.acos(diff.normalize().dotProduct(yAxis));
            final Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

            Cylinder line = new Cylinder(radius, length, 3);
            line.setMaterial(material);
            line.setDrawMode(DrawMode.FILL);
            line.setCullFace(CullFace.BACK);
            line.setMouseTransparent(false);
            line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);

            Cylinder invisibleLargerCylinder = null;
            if (this.pathManager.pathSelector.isPromptActive() && this.pathManager.pathSelector.mouseUpdateHook != null) {
                // The purpose of this is to ensure the mouse clicks are registered
                invisibleLargerCylinder = new Cylinder(MOUSE_PATH_DISTANCE_THRESHOLD, length, 3);
                invisibleLargerCylinder.setMaterial(MATERIAL_INVISIBLE);
                invisibleLargerCylinder.setMouseTransparent(false);
                invisibleLargerCylinder.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
                this.nodes.add(invisibleLargerCylinder);
                this.pathManager.pathDisplayList.add(invisibleLargerCylinder);
            }

            Sphere startOfSegmentMarker = null;
            if (showStart) {
                startOfSegmentMarker = this.pathManager.pathDisplayList.addSphere(x0, y0, z0, radius * 5.0, material, false);
                this.nodes.add(startOfSegmentMarker);
            }

            this.nodes.add(line);
            this.pathManager.pathDisplayList.add(line);

            // Create preview.
            FroggerPathSegmentPreview preview = new FroggerPathSegmentPreview(segment, segDistance, line, invisibleLargerCylinder, startOfSegmentMarker);
            EventHandler<MouseEvent> mouseEventEventHandler = evt -> handleClick(evt, preview);
            line.setOnMouseClicked(mouseEventEventHandler);
            if (invisibleLargerCylinder != null) {
                invisibleLargerCylinder.setOnMouseClicked(mouseEventEventHandler);
                this.pathManager.previewsByLargerClickAreas.put(invisibleLargerCylinder, preview);
            }

            if (startOfSegmentMarker != null)
                startOfSegmentMarker.setOnMouseClicked(mouseEventEventHandler);

            return preview;
        }

        private void handleClick(MouseEvent event, FroggerPathSegmentPreview segmentPreview) {
            if (this.pathManager.getPathSelector().isPromptActive() && this.pathManager.getPathSelector().handleClick(event, segmentPreview))
                return;

            // Select clicked path.
            this.pathManager.getValueSelectionBox().getSelectionModel().select(path);
        }
    }

    public static class PathSelectionTracker extends SelectionPromptTracker<FroggerPathSegmentPreview> {
        private final MouseHandler mouseHandler = this::handleMouseMove;
        private Consumer<FroggerPathSegmentPreview> mouseUpdateHook;


        public PathSelectionTracker(FroggerUIMapPathManager pathManager) {
            super(pathManager, true);
        }

        @Override
        public FroggerUIMapPathManager getUiManager() {
            return (FroggerUIMapPathManager) super.getUiManager();
        }

        @Override
        public void activate(Consumer<FroggerPathSegmentPreview> onSelect, Runnable onCancel) {
            super.activate(onSelect, onCancel);
            getUiManager().updateValueVisibility(); // Update the values to show with their weird colors.
            if (getController() != null)
                getController().getInputManager().addMouseListener(MouseEvent.MOUSE_MOVED, this.mouseHandler);
        }

        @Override
        protected void onPromptDisable(FroggerPathSegmentPreview pathSelection) {
            if (getController() != null)
                getController().getInputManager().removeMouseListener(MouseEvent.MOUSE_MOVED, this.mouseHandler);

            super.onPromptDisable(pathSelection);
            getUiManager().updateValueVisibility(); // Update the values to not appear with their weird colors anymore.
        }

        private void handleMouseMove(InputManager manager, MouseEvent event, double deltaX, double deltaY) {
            if (!isPromptActive() || manager.getMouseTracker().isSignificantMouseDragRecorded() || this.mouseUpdateHook == null)
                return;

            PickResult pickResult = event.getPickResult();
            if (pickResult == null)
                return;

            FroggerPathSegmentPreview preview = getUiManager().previewsByLargerClickAreas.get(pickResult.getIntersectedNode());
            if (preview != null)
                this.mouseUpdateHook.accept(preview);
        }

        /**
         * Prompts the user for a path.
         * @param handler  The handler to accept a prompt with.
         * @param onCancel A callback to run upon cancelling.
         */
        public void promptPath(Consumer<FroggerPathSegmentPreview> handler, Consumer<FroggerPathSegmentPreview> updateHandler, Runnable onCancel) {
            this.mouseUpdateHook = updateHandler;
            activate(handler, onCancel);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class FroggerPathSegmentPreview {
        private final FroggerPathSegment pathSegment;
        private final int segmentDistance; // How far along the segment the middle of this cylinder is at.
        private final Cylinder fxCylinder;
        private final Cylinder invisibleLargerCylinder;
        private final Sphere startOfSegmentMarker;

        /**
         * Gets the path which the segment preview represents a portion of.
         */
        public FroggerPath getPath() {
            return this.pathSegment.getPath();
        }
    }
}