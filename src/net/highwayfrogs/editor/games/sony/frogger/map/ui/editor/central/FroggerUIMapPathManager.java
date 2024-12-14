package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;

import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
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
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.FroggerPathSegmentType;
import net.highwayfrogs.editor.games.sony.frogger.map.data.path.segments.FroggerPathSegment;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerCentralUIManager.FroggerCentralMapListManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapPathManager.FroggerPathPreview;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.SelectionPromptTracker;
import net.highwayfrogs.editor.gui.editor.UISidePanel;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.lambda.TriConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows editing Frogger map paths.
 * TODO: Allow calculating position and rotation independently, more closely to how the old Frogger system does it? Not sure.
 * TODO: Allow a toggle to making the entities move along the paths. Eg: You can see when things will / will not line up. TODO: this should also be valid for EntityData & ScriptData, so they can do bobbing & stuff.
 * TODO: We should allow dragging an entity along the path in 3D space. Eg: Click and hold will let you drag, by finding nearby path parts
 * TODO: Go over path speed. What kind of unit is this? Create separate editor grid functions for path speed which accurately mimic the behavior. Update entity data, units, etc.
 * TODO: Allow selecting individual segments instead of showing them all at once, and highlight them.
 * TODO: Can I make a 3D path editor soon? I bet it's doable.
 * Created by Kneesnap on 6/2/2024.
 */
public class FroggerUIMapPathManager extends FroggerCentralMapListManager<FroggerPath, FroggerPathPreview> {
    @Getter private final PathSelectionTracker pathSelector;
    private DisplayList pathDisplayList;
    @Getter private TextField fullPathLengthField;

    private static final PhongMaterial MATERIAL_WHITE = Scene3DUtils.makeUnlitSharpMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_LIGHT_GREEN = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial MATERIAL_BLUE = Scene3DUtils.makeUnlitSharpMaterial(Color.BLUE);
    private static final PhongMaterial MATERIAL_GREEN = Scene3DUtils.makeUnlitSharpMaterial(Color.GREEN);
    private static final PhongMaterial MATERIAL_PURPLE = Scene3DUtils.makeUnlitSharpMaterial(Color.PURPLE);

    public FroggerUIMapPathManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
        this.pathSelector = new PathSelectionTracker(this);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        this.pathDisplayList = getRenderManager().createDisplayListWithNewGroup();
    }

    @Override
    protected void setupMainGridEditor(UISidePanel sidePanel) {
        super.setupMainGridEditor(sidePanel);
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

    /**
     * Represents a 3D path preview.
     */
    @Getter
    @RequiredArgsConstructor
    public static class FroggerPathPreview {
        private final FroggerUIMapPathManager pathManager;
        private final FroggerPath path;
        private final List<Node> nodes = new ArrayList<>();
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
                final int stepSize = Math.min(SEGMENT_STEP_SIZE, segment.getLength());
                if (stepSize == 0) { // If the path is 100% empty, just show the start.
                    Vector pos = segment.getStartPosition();
                    addPathLineSegment(pos.getFloatX(), pos.getFloatY(), pos.getFloatZ(), pos.getFloatX(), pos.getFloatY(), pos.getFloatZ(), .2, material, true, this.path, segment, 0);
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
                                material, step == 0, this.path, segment, (step * stepSize) + (stepSize / 2));
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
        private Cylinder addPathLineSegment(double x0, double y0, double z0, double x1, double y1, double z1, double radius, PhongMaterial material, boolean showStart, FroggerPath path, FroggerPathSegment segment, int segDistance) {
            EventHandler<MouseEvent> mouseEventEventHandler = evt -> handleClick(evt, path, segment, segDistance);
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
            line.setOnMouseClicked(mouseEventEventHandler);
            line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);

            if (showStart) {
                Sphere sphStart = this.pathManager.pathDisplayList.addSphere(x0, y0, z0, radius * 5.0, material, false);
                sphStart.setOnMouseClicked(mouseEventEventHandler);
                this.nodes.add(sphStart);
            }

            this.nodes.add(line);
            this.pathManager.pathDisplayList.add(line);
            return line;
        }

        private void handleClick(MouseEvent event, FroggerPath path, FroggerPathSegment segment, int segDistance) {
            if (this.pathManager.getPathSelector().isPromptActive() && this.pathManager.getPathSelector().handleClick(event, new FroggerPathSelection(path, segment, segDistance)))
                return;

            // Select clicked path.
            this.pathManager.getValueSelectionBox().getSelectionModel().select(path);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class FroggerPathSelection {
        private final FroggerPath path;
        private final FroggerPathSegment pathSegment;
        private final int segmentDistance;
    }

    public static class PathSelectionTracker extends SelectionPromptTracker<FroggerPathSelection> {
        public PathSelectionTracker(FroggerUIMapPathManager pathManager) {
            super(pathManager, true);
        }

        @Override
        public FroggerUIMapPathManager getUiManager() {
            return (FroggerUIMapPathManager) super.getUiManager();
        }

        /**
         * Prompts the user for a path.
         * @param handler  The handler to accept a prompt with.
         * @param onCancel A callback to run upon cancelling.
         */
        public void promptPath(TriConsumer<FroggerPath, FroggerPathSegment, Integer> handler, Runnable onCancel) {
            activate(result -> handler.accept(result.getPath(), result.getPathSegment(), result.getSegmentDistance()), onCancel);
            getUiManager().updateValueVisibility(); // Update the values to show with their weird colors.
        }

        @Override
        protected void onPromptDisable(FroggerPathSelection pathSelection) {
            super.onPromptDisable(pathSelection);
            getUiManager().updateValueVisibility(); // Update the values to not appear with their weird colors anymore.
        }
    }
}