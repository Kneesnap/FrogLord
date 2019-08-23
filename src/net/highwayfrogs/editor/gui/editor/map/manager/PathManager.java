package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
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
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.TriConsumer;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Collections;
import java.util.List;

/**
 * Manages paths in the 3D view.
 * Created by Kneesnap on 8/16/2019.
 */
@Getter
public class PathManager extends MapManager {
    private PathDisplaySetting displaySetting = PathDisplaySetting.NONE;
    private Path selectedPath;
    private GUIEditorGrid pathEditor;
    private TriConsumer<Path, PathSegment, Integer> promptHandler; // Path, Segment, Segment Distance.

    private static final String DISPLAY_LIST_PATHS = "displayListPaths";
    private static final PhongMaterial MATERIAL_WHITE = Utils.makeSpecialMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_LIGHT_GREEN = Utils.makeSpecialMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial MATERIAL_BLUE = Utils.makeSpecialMaterial(Color.BLUE);
    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.GREEN);
    private static final PhongMaterial MATERIAL_PURPLE = Utils.makeSpecialMaterial(Color.PURPLE);
    private static final float MARKER_SIZE = 2;

    public PathManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        getController().getPathDisplayOption().setItems(FXCollections.observableArrayList(PathDisplaySetting.values()));
        getController().getPathDisplayOption().getSelectionModel().selectFirst();
        getController().getPathDisplayOption().valueProperty().addListener(((observable, oldValue, newValue) -> setDisplaySetting(newValue)));
    }

    /**
     * Sets the display setting.
     * @param newSetting The new setting.
     */
    public void setDisplaySetting(PathDisplaySetting newSetting) {
        this.displaySetting = newSetting;
        updatePathDisplay();
    }

    /**
     * Prompts the user for a path.
     * @param handler  The handler to accept a prompt with.
     * @param onCancel A callback to run upon cancelling.
     */
    public void promptPath(TriConsumer<Path, PathSegment, Integer> handler, Runnable onCancel) {
        this.promptHandler = handler;
        activatePrompt(onCancel);
        updatePathDisplay();
    }

    /**
     * Accept the data for the prompt.
     * @param path    The path to accept.
     * @param segment The segment to add to.
     */
    public void acceptPrompt(Path path, PathSegment segment, int segDistance) {
        if (this.promptHandler != null)
            this.promptHandler.accept(path, segment, segDistance);
        onPromptFinish();
    }

    @Override
    protected void cleanChildPrompt() {
        super.cleanChildPrompt();
        this.promptHandler = null;
        updatePathDisplay(); // Updates the colors so it doesn't appear to be in a prompt anymore.
    }

    /**
     * Update display of paths.
     */
    public void updatePathDisplay() {
        getRenderManager().addMissingDisplayList(DISPLAY_LIST_PATHS);
        getRenderManager().clearDisplayList(DISPLAY_LIST_PATHS);

        if (isPromptActive()) {
            addPaths(DISPLAY_LIST_PATHS, getMap().getPaths(), MATERIAL_BLUE, MATERIAL_GREEN, MATERIAL_PURPLE);
        } else if (this.displaySetting == PathDisplaySetting.ALL) {
            addPaths(DISPLAY_LIST_PATHS, getMap().getPaths(), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);
        } else if (this.displaySetting == PathDisplaySetting.SELECTED) {
            if (getSelectedPath() != null)
                addPaths(DISPLAY_LIST_PATHS, Collections.singletonList(getSelectedPath()), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);
        }
    }

    private void handleClick(Path path, PathSegment segment, int segDistance) {
        if (isPromptActive()) {
            acceptPrompt(path, segment, segDistance);
            return;
        }

        // Select clicked path.
        this.selectedPath = path;
        setupEditor(); // Show this path as the selected path.
    }

    /**
     * Adds a cylindrical representation of a 3D line.
     * @param listID    The display list ID.
     * @param x0        The x-coordinate defining the start of the line segment.
     * @param y0        The y-coordinate defining the start of the line segment.
     * @param z0        The z-coordinate defining the start of the line segment.
     * @param x1        The x-coordinate defining the end of the line segment.
     * @param y1        The y-coordinate defining the end of the line segment.
     * @param z1        The z-coordinate defining the end of the line segment.
     * @param radius    The radius of the cylinder (effectively the 'width' of the line).
     * @param material  The material used to render the line segment.
     * @param showStart Whether or not to display a sphere at the start of the line segment.
     * @return The newly created/added cylinder (cylinder primitive only!)
     */
    public Cylinder addPathLineSegment(String listID, double x0, double y0, double z0, double x1, double y1, double z1, double radius, PhongMaterial material, boolean showStart, Path path, PathSegment segment, int segDistance) {
        EventHandler<MouseEvent> mouseEventEventHandler = evt -> handleClick(path, segment, segDistance);
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
        getRenderManager().addNode(listID, line);

        if (showStart) {
            Sphere sphStart = getRenderManager().addSphere(listID, x0, y0, z0, radius * 5.0, material, false);
            sphStart.setOnMouseClicked(mouseEventEventHandler);
        }

        return line;
    }

    /**
     * Adds a list of paths to the specified display list (collection of line, arc and spline segments).
     * @param listID         The display list ID.
     * @param pathList       The paths to realise.
     * @param materialLine   The material to use for rendering line segments.
     * @param materialArc    The material to use for rendering arc segments.
     * @param materialSpline The material to use for rendering spline segments.
     */
    public void addPaths(String listID, List<Path> pathList, PhongMaterial materialLine, PhongMaterial materialArc, PhongMaterial materialSpline) {
        getRenderManager().addMissingDisplayList(listID);
        PathInfo pathInfo = new PathInfo(); // We will use pathInfo to 'step' along the paths and to build the geometry

        for (int pathIndex = 0; pathIndex < pathList.size(); pathIndex++) {
            Path path = pathList.get(pathIndex);

            for (int segmentIndex = 0; segmentIndex < path.getSegments().size(); segmentIndex++) {
                PathSegment segment = path.getSegments().get(segmentIndex);
                pathInfo.setPathId(pathIndex);
                pathInfo.setSegmentId(segmentIndex);

                PhongMaterial material = materialSpline;
                if (segment.getType() == PathType.LINE)
                    material = materialLine;
                if (segment.getType() == PathType.ARC)
                    material = materialArc;

                final int stepSize = Math.min(32, segment.getLength());
                final int numSteps = 1 + (segment.getLength() / stepSize);

                for (int step = 0; step < numSteps; ++step) {
                    pathInfo.setSegmentDistance(step * stepSize);
                    Vector v0 = path.evaluatePosition(pathInfo).getPosition();

                    pathInfo.setSegmentDistance(Math.min((step + 1) * stepSize, segment.getLength()));
                    Vector v1 = path.evaluatePosition(pathInfo).getPosition();
                    if (!v0.equals(v1))
                        addPathLineSegment(listID, v0.getFloatX(), v0.getFloatY(), v0.getFloatZ(), v1.getFloatX(), v1.getFloatY(), v1.getFloatZ(), 0.20,
                                material, step == 0, path, segment, (step * stepSize) + (stepSize / 2));
                }
            }
        }
    }

    @Override
    public void setupEditor() {
        if (this.pathEditor == null)
            this.pathEditor = new GUIEditorGrid(getController().getPathGridPane());

        if (this.selectedPath == null && !getMap().getPaths().isEmpty())
            this.selectedPath = getMap().getPaths().get(0);

        updatePathDisplay();
        this.pathEditor.clearEditor();

        ComboBox<Path> box = this.pathEditor.addSelectionBox("Path:", getSelectedPath(), getMap().getPaths(), newPath -> {
            this.selectedPath = newPath;
            setupEditor();
        });
        box.setConverter(new AbstractStringConverter<>(path -> "Path #" + getMap().getPaths().indexOf(path)));

        this.pathEditor.addLabelButton("", "Add Path", 25.0, () -> {
            getMap().getPaths().add(this.selectedPath = new Path());
            setupEditor();
        });

        if (this.selectedPath != null) {
            this.pathEditor.addLabelButton("", "Remove Path", 25.0, () -> {
                int pathIndex = getMap().getPaths().indexOf(this.selectedPath);
                for (Entity entity : getMap().getEntities()) {
                    if (entity.getPathInfo() != null && entity.getPathInfo().getPathId() == pathIndex) {
                        Utils.makePopUp("You must remove all entities from this path before you can remove it.", AlertType.WARNING);
                        return;
                    }
                }

                // Remove path.
                getMap().removePath(this.selectedPath);
                this.selectedPath = null;
                updatePathDisplay();
                setupEditor();
            });

            this.pathEditor.addSeparator(25.0);
            this.pathEditor.addBoldLabel("Path Data");
            this.selectedPath.setupEditor(this, this.pathEditor);
        }
    }

    public enum PathDisplaySetting {
        NONE, SELECTED, ALL
    }
}
