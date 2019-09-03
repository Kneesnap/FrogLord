package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.MAPPolygonData;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.GridController;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.mesh.MeshData;

import java.util.function.Consumer;

/**
 * Manages general geometry.
 * Created by Kneesnap on 8/20/2019.
 */
@Getter
public class GeometryManager extends MapManager {
    private GUIEditorGrid geometryEditor;
    private MeshData looseMeshData;

    // Polygon cursor data.
    private MAPPolygon selectedPolygon;
    private MAPPolygon polygonImmuneToTarget;
    private boolean polygonSelected;
    private MeshData cursorData;
    private Consumer<MAPPolygon> promptHandler;

    public GeometryManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();

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
            if (!isPolygonSelected())
                setCursorPolygon(getMesh().getFacePolyMap().get(evt.getPickResult().getIntersectedFace()));
        });

        mapScene.setOnMouseClicked(evt -> {
            MAPPolygon clickedPoly = getMesh().getFacePolyMap().get(evt.getPickResult().getIntersectedFace());

            if (getSelectedPolygon() != null && (getSelectedPolygon() == clickedPoly)) {
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
                    }
                }
            }
        });
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
        this.geometryEditor.addSeparator(25);

        if (getSelectedPolygon() != null) {
            this.geometryEditor.addBoldLabel("Selected Polygon:");
            getSelectedPolygon().setupEditor(this, this.geometryEditor);

            this.geometryEditor.addSeparator(25);
            MAPPolygonData data = new MAPPolygonData();
            data.loadFrom(getSelectedPolygon());
            data.setupEditor(this.geometryEditor, getController());
            this.geometryEditor.addButton("Apply Changes", () -> data.applyToPolygon(getSelectedPolygon()));
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
            getMap().forEachPrimitive(prim -> {
                if (!prim.isAllowDisplay() && prim instanceof MAPPolygon)
                    getController().renderOverPolygon((MAPPolygon) prim, MapMesh.INVISIBLE_COLOR);
            });
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
        if (cursorPoly == null)
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
        getMesh().updateData();
        renderCursor(getSelectedPolygon());
    }
}
