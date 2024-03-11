package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.input.PickResult;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;

/**
 * Manages UI relating to the landscape/terrain of an old Frogger map.
 * Created by Kneesnap on 1/22/2024.
 */
public class OldFroggerLandscapeUIManager extends BakedLandscapeUIManager<OldFroggerMapMesh, OldFroggerMapPolygon> {
    public OldFroggerLandscapeUIManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the landscape is displayed for.
     */
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }

    @Override
    public void onSetup() {
        super.onSetup();

        // Add mesh click listener.
        getController().getMeshScene().setOnMouseClicked(evt -> {
            PickResult result = evt.getPickResult();
            if (result == null || result.getIntersectedNode() != getController().getMeshView())
                return; // No pick result, or the thing that was clicked was not the main mesh.

            int intersectedFace = result.getIntersectedFace();
            OldFroggerMapPolygon polygon = null;
            if (intersectedFace >= 0)
                polygon = getMesh().getMainNode().getDataSourceByFaceIndex(intersectedFace);

            // If the polygon is the currently selected polygon, de-select it.
            if (polygon == getSelectedPolygon())
                polygon = null;

            selectPolygon(polygon);
        });
    }

    @Override
    protected DynamicMeshOverlayNode getPolygonHighlightNode() {
        return getMesh().getHighlightedPolygonNode();
    }

    @Override
    protected OldFroggerPolygonShadingEditor createShadingEditor() {
        return new OldFroggerPolygonShadingEditor(this);
    }

    @Override
    public PSXShadeTextureDefinition createPolygonShadeDefinition(OldFroggerMapPolygon polygon) {
        return polygon.createPolygonShadeDefinition(getMap());
    }

    @Override
    public DynamicMeshDataEntry getMeshEntryForPolygon(OldFroggerMapPolygon polygon) {
        return getMesh().getMainNode().getDataEntry(polygon);
    }

    @Override
    protected SVector getVertex(int vertexId) {
        return getMap().getVertexPacket().getVertices().get(vertexId);
    }

    @Override
    protected int[] getSelectedPolygonVertexIds() {
        return getSelectedPolygon() != null ? getSelectedPolygon().getVertices() : null;
    }

    public static class OldFroggerPolygonShadingEditor extends BakedLandscapePolygonShadingEditor {
        public OldFroggerPolygonShadingEditor(OldFroggerLandscapeUIManager manager) {
            super(manager);
        }

        @Override
        public OldFroggerLandscapeUIManager getManager() {
            return (OldFroggerLandscapeUIManager) super.getManager();
        }

        @Override
        protected void onVertexPositionChange(MeshView meshView, int localVertexIndex, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, int flags) {
            OldFroggerMapMesh mesh = getManager().getController().getMesh();
            mesh.getMainNode().updateMapVertex(getManager().getSelectedPolygonVertexIds()[localVertexIndex]);
        }

        @Override
        protected void onColorUpdate(int colorIndex, CVector color) {
            OldFroggerMapPolygon polygon = getManager().getSelectedPolygon();
            OldFroggerMapMesh mesh = getManager().getController().getMesh();
            PSXShadeTextureDefinition oldShadedTexture = mesh.getShadedTextureManager().getShadedTexture(polygon);

            // Apply color changes.
            PSXShadeTextureDefinition newShadedTexture = oldShadedTexture.clone();
            newShadedTexture.getColors()[colorIndex].copyFrom(color);
            getManager().getController().getMesh().getShadedTextureManager().updatePolygon(polygon, newShadedTexture);
        }

        @Override
        protected void onTextureUvUpdate(int uvIndex, SCByteTextureUV uv) {
            OldFroggerMapMesh mesh = getManager().getController().getMesh();
            OldFroggerMapPolygon polygon = getManager().getSelectedPolygon();
            PSXShadeTextureDefinition oldShadedTexture = mesh.getShadedTextureManager().getShadedTexture(polygon);

            // Apply updated uv data and update the 3D view.
            PSXShadeTextureDefinition newShadedTexture = oldShadedTexture.clone();
            newShadedTexture.getTextureUVs()[uvIndex].copyFrom(uv);
            mesh.getShadedTextureManager().updatePolygon(polygon, newShadedTexture);
        }
    }
}
