package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.path.PathInfo;
import net.highwayfrogs.editor.file.map.path.PathSegment;
import net.highwayfrogs.editor.file.map.path.PathType;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Manages paths in the 3D view.
 * Created by Kneesnap on 8/16/2019.
 */
@Getter
public class PathManager extends MapManager {
    private PathDisplaySetting displaySetting = PathDisplaySetting.NONE;
    private Path selectedPath;
    private GUIEditorGrid pathEditor;
    private BiConsumer<Path, PathSegment> promptHandler;

    private static final String DISPLAY_LIST_PATHS = "displayListPaths";
    private static final PhongMaterial MATERIAL_WHITE = Utils.makeSpecialMaterial(Color.WHITE);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_LIGHT_GREEN = Utils.makeSpecialMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial MATERIAL_BLUE = Utils.makeSpecialMaterial(Color.BLUE);
    private static final float MARKER_SIZE = 2;

    public PathManager(MapUIController controller) {
        super(controller);
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
    public void promptPath(BiConsumer<Path, PathSegment> handler, Runnable onCancel) {
        this.promptHandler = handler;
        activatePrompt(onCancel);
        updatePathDisplay();
    }

    /**
     * Accept the data for the prompt.
     * @param path    The path to accept.
     * @param segment The segment to accept.
     */
    public void acceptPrompt(Path path, PathSegment segment) {
        if (this.promptHandler != null)
            this.promptHandler.accept(path, segment);
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

        if (isPromptActive()) { //TODO: Better selection?
            addPaths(DISPLAY_LIST_PATHS, getController().getMap().getPaths(), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);

            // Add path markers.
            for (Path path : getMap().getPaths())
                for (PathSegment segment : path.getSegments())
                    addPathMarker(path, segment, segment.getStartPosition());
        } else if (this.displaySetting == PathDisplaySetting.ALL) {
            addPaths(DISPLAY_LIST_PATHS, getController().getMap().getPaths(), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);
        } else if (this.displaySetting == PathDisplaySetting.SELECTED) {
            if (getSelectedPath() != null)
                addPaths(DISPLAY_LIST_PATHS, Collections.singletonList(getSelectedPath()), MATERIAL_WHITE, MATERIAL_YELLOW, MATERIAL_LIGHT_GREEN);
        }
    }

    private void addPathMarker(Path path, PathSegment segment, SVector vector) {
        float baseX = vector.getFloatX();
        float baseY = vector.getFloatY();
        float baseZ = vector.getFloatZ();
        getRenderManager().addBoundingBoxFromMinMax(DISPLAY_LIST_PATHS, baseX - MARKER_SIZE, baseY - MARKER_SIZE, baseZ - MARKER_SIZE,
                baseX + MARKER_SIZE, baseY + MARKER_SIZE, baseZ + MARKER_SIZE, MATERIAL_BLUE, false)
                .setOnMouseClicked(evt -> acceptPrompt(path, segment));
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

        // We will use pathInfo to 'step' along the paths and to build the geometry
        PathInfo pathInfo = new PathInfo();

        // Track indices (ID's) of paths and segments so that we can feed them in via the PathInfo object
        int pathIndex = 0;
        int segmentIndex;

        double x0, y0, z0;
        double x1, y1, z1;

        for (Path path : pathList) {
            segmentIndex = 0;

            for (PathSegment segment : path.getSegments()) {
                pathInfo.setPathId(pathIndex);
                pathInfo.setSegmentId(segmentIndex);

                if (segment.getType() == PathType.LINE) {
                    pathInfo.setSegmentDistance(0);
                    Vector vec0 = path.evaluatePosition(pathInfo).getPosition();

                    pathInfo.setSegmentDistance(segment.getLength());
                    Vector vec1 = path.evaluatePosition(pathInfo).getPosition();

                    x0 = vec0.getFloatX();
                    y0 = vec0.getFloatY();
                    z0 = vec0.getFloatZ();

                    x1 = vec1.getFloatX();
                    y1 = vec1.getFloatY();
                    z1 = vec1.getFloatZ();

                    getRenderManager().addLineSegment(listID, x0, y0, z0, x1, y1, z1, 0.20, materialLine, true, true);
                } else if (segment.getType() == PathType.ARC) {
                    final int stepSize = Math.min(32, segment.getLength());
                    final int numSteps = 1 + (segment.getLength() / stepSize);

                    for (int step = 0; step < numSteps; ++step) {
                        pathInfo.setSegmentDistance(step * stepSize);
                        Vector vec0 = path.evaluatePosition(pathInfo).getPosition();

                        pathInfo.setSegmentDistance(Math.min((step + 1) * stepSize, segment.getLength()));
                        Vector vec1 = path.evaluatePosition(pathInfo).getPosition();

                        x0 = vec0.getFloatX();
                        y0 = vec0.getFloatY();
                        z0 = vec0.getFloatZ();

                        x1 = vec1.getFloatX();
                        y1 = vec1.getFloatY();
                        z1 = vec1.getFloatZ();

                        if (!((x0 == x1) && (y0 == y1) && (z0 == z1)))
                            getRenderManager().addLineSegment(listID, x0, y0, z0, x1, y1, z1, 0.20, materialArc, false, false);
                    }
                } else if (segment.getType() == PathType.SPLINE) {
                    final int stepSize = Math.min(32, segment.getLength());
                    final int numSteps = 1 + (segment.getLength() / stepSize);

                    for (int step = 0; step < numSteps; ++step) {
                        pathInfo.setSegmentDistance(step * stepSize);
                        Vector vec0 = path.evaluatePosition(pathInfo).getPosition();

                        pathInfo.setSegmentDistance(Math.min((step + 1) * stepSize, segment.getLength()));
                        Vector vec1 = path.evaluatePosition(pathInfo).getPosition();

                        x0 = vec0.getFloatX();
                        y0 = vec0.getFloatY();
                        z0 = vec0.getFloatZ();

                        x1 = vec1.getFloatX();
                        y1 = vec1.getFloatY();
                        z1 = vec1.getFloatZ();

                        if (!((x0 == x1) && (y0 == y1) && (z0 == z1)))
                            getRenderManager().addLineSegment(listID, x0, y0, z0, x1, y1, z1, 0.20, materialSpline, false, false);
                    }
                }

                ++segmentIndex;
            }

            ++pathIndex;
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
            this.selectedPath.setupEditor(getController(), this.pathEditor);
        }
    }

    public enum PathDisplaySetting {
        NONE, SELECTED, ALL
    }
}
