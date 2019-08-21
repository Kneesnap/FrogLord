package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.GridController;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.mesh.MeshData;

/**
 * Manages general geometry.
 * Created by Kneesnap on 8/20/2019.
 */
@Getter
public class GeometryManager extends MapManager {
    private GUIEditorGrid geometryEditor;
    private MeshData looseMeshData;

    public GeometryManager(MapUIController controller) {
        super(controller);
    }

    @Override
    public void setupEditor() {
        if (this.geometryEditor == null)
            this.geometryEditor = new GUIEditorGrid(getController().getGeometryGridPane());

        this.geometryEditor.clearEditor();
        this.geometryEditor.addButton("Edit Collision Grid", () -> GridController.openGridEditor(this));
        this.geometryEditor.addCheckBox("Highlight Invisible Polygons", this.looseMeshData != null, this::updateVisibility);
        this.geometryEditor.addSeparator(25);

        MAPPolygon showPolygon = getController().getGeneralManager().getSelectedPolygon();
        if (showPolygon != null) {
            this.geometryEditor.addBoldLabel("Selected Polygon:");
            showPolygon.setupEditor(this, this.geometryEditor);
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
}
