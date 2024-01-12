package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Translate;
import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShadingMode;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.GridController;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Manages general geometry.
 * Created by Kneesnap on 8/20/2019.
 */
@Getter
public class GeometryManager extends MapManager {
    private static final double VERTEX_BOX_SIZE = 1.5;
    private static final PhongMaterial VERTEX_BOX_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final String UNUSED_VERTEX_DISPLAY_LIST = "unusedVertexBoxes";

    private final List<Box> unusedVertexBoxes = new ArrayList<>();
    private GUIEditorGrid geometryEditor;
    private MeshData looseMeshData;

    // Polygon cursor data.
    private MAPPolygon selectedPolygon;
    private MAPPolygon polygonImmuneToTarget;
    private boolean polygonSelected;
    private MeshData cursorData;
    private Consumer<MAPPolygon> promptHandler;
    @Setter private MeshView hoverView; // Anything we're hovering over which should allow selection instead of the cursor.

    public GeometryManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();

        getController().getShadingModeChoiceBox().setItems(FXCollections.observableArrayList(ShadingMode.values()));
        getController().getShadingModeChoiceBox().setValue(getMesh().getTextureMap().getMode());
        getController().getShadingModeChoiceBox().setConverter(new AbstractStringConverter<>(ShadingMode::getName));
        getController().getShadingModeChoiceBox().valueProperty().addListener((observable, oldValue, newValue) -> {
            getMesh().getTextureMap().setMode(newValue);
            getController().getEntityManager().setShadingMode(newValue);
            refreshView();
        });

        Scene mapScene = getController().getMapScene();
        mapScene.setOnMousePressed(e -> {
            getController().getAnchorPaneUIRoot().requestFocus();
            if (!isPolygonSelected())
                hideCursorPolygon();
        });

        mapScene.setOnMouseReleased(evt -> {
            hideCursorPolygon();
            renderCursor(getSelectedPolygon());
        });

        mapScene.setOnMouseMoved(evt -> {
            if (!isPolygonSelected() && this.hoverView == null)
                setCursorPolygon(getMesh().getFacePolyMap().get(evt.getPickResult().getIntersectedFace()));
        });

        // Setup unused vertex box rendering
        getRenderManager().addMissingDisplayList(UNUSED_VERTEX_DISPLAY_LIST);
        getController().getCheckBoxShowUnusedVertices().setOnAction(evt ->
                updateUnusedVertexVisibility());

        mapScene.setOnMouseClicked(evt -> {
            MAPPolygon clickedPoly = getMesh().getFacePolyMap().get(evt.getPickResult().getIntersectedFace());

            if (this.hoverView == null && getSelectedPolygon() != null && (getSelectedPolygon() == clickedPoly)) {
                if (isPolygonSelected()) {
                    this.polygonImmuneToTarget = getSelectedPolygon();
                    removeCursorPolygon();
                } else if (getController() == null || !getController().handleClick(evt, clickedPoly)) {
                    if (getController() != null && getController().getCheckBoxFaceRemoveMode().isSelected()) {
                        getMap().removeFace(getSelectedPolygon());
                        removeCursorPolygon();
                        refreshView();
                        getController().getAnimationManager().setupEditor();
                    } else {
                        setCursorPolygon(clickedPoly);
                        this.polygonSelected = true;
                        setupEditor(); // Update editor.
                    }
                }
            }
        });
    }

    /**
     * Called when the mouse stops hovering over a given mesh.
     * @param view The view to stop.
     */
    public void onStopHover(MeshView view) {
        if (Objects.equals(view, this.hoverView))
            this.hoverView = null;
    }

    /**
     * Setup a node so the cursor will not be shown when the mouse hovers over the node.
     * @param view The node to setup.
     */
    public void setupView(MeshView view) {
        view.setOnMouseEntered(evt -> setHoverView((MeshView) evt.getSource()));
        view.setOnMouseExited(evt -> onStopHover((MeshView) evt.getSource()));
    }

    @Override
    public boolean onKeyPress(KeyEvent event) {
        if (isPolygonSelected() && event.getCode() == KeyCode.ESCAPE) {
            removeCursorPolygon();
            return true;
        }

        return super.onKeyPress(event);
    }

    @Override
    public void setupEditor() {
        if (this.geometryEditor == null)
            this.geometryEditor = new GUIEditorGrid(getController().getGeometryGridPane());

        this.geometryEditor.clearEditor();
        this.geometryEditor.addButton("Edit Collision Grid", () -> GridController.openGridEditor(this));
        this.geometryEditor.addCheckBox("Highlight Invisible Polygons", this.looseMeshData != null, this::updateVisibility);
        this.geometryEditor.addButton("Refresh View", getController().getGeometryManager()::refreshView);
        this.geometryEditor.addSeparator(25);

        if (getSelectedPolygon() != null) {
            getSelectedPolygon().setupEditor(this.geometryEditor, getController());
        }
    }

    @Override
    public boolean handleClick(MouseEvent event, MAPPolygon clickPoly) {
        if (getLooseMeshData() != null) { // Toggle visibility.
            clickPoly.setAllowDisplay(!clickPoly.isAllowDisplay());
            updateVisibility(true);
            return true;
        }

        Platform.runLater(this::setupEditor); // This is why this is registered second to last.
        return super.handleClick(event, clickPoly);
    }

    private void updateVisibility(boolean drawState) {
        if (this.looseMeshData != null) {
            getMesh().getManager().removeMesh(this.looseMeshData);
            this.looseMeshData = null;
        }

        if (drawState) {
            for (MAPPolygon poly : getMap().getAllPolygons())
                if (!poly.isAllowDisplay())
                    getController().renderOverPolygon(poly, MapMesh.INVISIBLE_COLOR);
            this.looseMeshData = getMesh().getManager().addMesh();
        }
    }

    /**
     * Prompts the user for a polygon.
     * @param handler  The handler to accept a prompt with.
     * @param onCancel A callback to run upon cancelling.
     */
    public void selectPolygon(Consumer<MAPPolygon> handler, Runnable onCancel) {
        this.promptHandler = handler;
        activatePrompt(onCancel);
    }

    /**
     * Accept the data for the prompt.
     * @param poly The polygon to accept.
     */
    public void acceptPrompt(MAPPolygon poly) {
        if (this.promptHandler != null)
            this.promptHandler.accept(poly);
        onPromptFinish();
    }

    @Override
    protected void cleanChildPrompt() {
        super.cleanChildPrompt();
        this.promptHandler = null;
    }

    /**
     * Supposedly removes the cursor polygon.
     */
    public void removeCursorPolygon() {
        if (this.selectedPolygon == null)
            return;

        this.polygonSelected = false;
        this.selectedPolygon = null;
        hideCursorPolygon();
    }

    /**
     * Hides the cursor polygon.
     */
    public void hideCursorPolygon() {
        if (cursorData == null)
            return;

        getMesh().getManager().removeMesh(cursorData);
        this.cursorData = null;
    }

    /**
     * Set the polygon that the cursor is hovering over.
     * @param newPoly The poly to highlight.
     */
    public void setCursorPolygon(MAPPolygon newPoly) {
        if (newPoly == this.selectedPolygon || newPoly == this.polygonImmuneToTarget)
            return;

        removeCursorPolygon();
        this.polygonImmuneToTarget = null;
        if (newPoly != null)
            renderCursor(this.selectedPolygon = newPoly);
    }

    private void renderCursor(MAPPolygon cursorPoly) {
        if (cursorPoly == null || this.hoverView != null)
            return;

        boolean showRemoveColor = !isPolygonSelected() && getController().getCheckBoxFaceRemoveMode().isSelected();
        getController().renderOverPolygon(cursorPoly, showRemoveColor ? MapMesh.REMOVE_FACE_COLOR : MapMesh.CURSOR_COLOR);
        cursorData = getMesh().getManager().addMesh();
    }

    /**
     * Refresh map data.
     */
    public void refreshView() {
        hideCursorPolygon();
        getController().getMapMesh().getTextureMap().updateMap(getMap(), null);
        getMesh().updateData();
        renderCursor(getSelectedPolygon());
        updateUnusedVertexVisibility();
    }

    private void updateUnusedVertexBoxes(List<SVector> newBoxes) {
        // Add new boxes.
        while (newBoxes.size() > this.unusedVertexBoxes.size()) {
            Box box = new Box(VERTEX_BOX_SIZE, VERTEX_BOX_SIZE, VERTEX_BOX_SIZE);
            box.setMaterial(VERTEX_BOX_MATERIAL);
            box.setDrawMode(DrawMode.LINE);
            box.setCullFace(CullFace.BACK);
            box.getTransforms().addAll(new Translate(0, 0, 0));
            box.setMouseTransparent(false);
            final int index = this.unusedVertexBoxes.size();
            box.setOnMouseClicked(evt -> System.out.println("Clicked Vertex " + getMap().getVertexes().indexOf(newBoxes.get(index))));
            this.unusedVertexBoxes.add(box);
            getRenderManager().addNode(UNUSED_VERTEX_DISPLAY_LIST, box);
        }

        // Update existing boxes.
        for (int i = 0; i < this.unusedVertexBoxes.size(); i++) {
            Box box = this.unusedVertexBoxes.get(i);
            boolean isUsed = i < newBoxes.size();
            box.setVisible(isUsed);

            if (isUsed) {
                SVector vec = newBoxes.get(i);
                Translate translate = (Translate) box.getTransforms().get(0);
                translate.setX(vec.getFloatX()); // Update transform.
                translate.setY(vec.getFloatY());
                translate.setZ(vec.getFloatZ());
                box.setMaterial(VERTEX_BOX_MATERIAL); // Update material.
            }
        }
    }

    /**
     * Updates the vertex display.
     */
    public void updateUnusedVertexVisibility() {
        if (getController().getCheckBoxShowUnusedVertices().isSelected()) {
            updateUnusedVertexBoxes(getMap().findUnusedVertices());
        } else {
            updateUnusedVertexBoxes(Collections.emptyList());
        }
    }
}
