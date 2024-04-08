package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import javafx.scene.input.PickResult;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;

/**
 * Manages UI relating to the landscape/terrain of a MediEvil map.
 * Created by Kneesnap on 3/16/2024.
 */
public class MediEvilLandscapeUIManager extends BakedLandscapeUIManager<MediEvilMapMesh, MediEvilMapPolygon> {
    public MediEvilLandscapeUIManager(MeshViewController<MediEvilMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the landscape is displayed for.
     */
    public MediEvilMapFile getMap() {
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
            MediEvilMapPolygon polygon = null;
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
    protected MediEvilPolygonShadingEditor createShadingEditor() {
        return new MediEvilPolygonShadingEditor(this);
    }

    @Override
    public PSXShadeTextureDefinition createPolygonShadeDefinition(MediEvilMapPolygon polygon) {
        return polygon.createPolygonShadeDefinition(getMap(), getMesh().isShadingEnabled());
    }

    @Override
    public DynamicMeshDataEntry getMeshEntryForPolygon(MediEvilMapPolygon polygon) {
        return getMesh().getMainNode().getDataEntry(polygon);
    }

    @Override
    protected SVector getVertex(int vertexId) {
        return getMap().getGraphicsPacket().getVertices().get(vertexId);
    }

    private int[] cachedSelectedPolygonVertexIds = new int[3];
    @Override
    protected int[] getSelectedPolygonVertexIds() {
        if (getSelectedPolygon() == null || getSelectedPolygon().getVertices() == null)
            return null;

        // If the vertex array is the expected length, return it directly.
        if (getSelectedPolygon().getVertexCount() == getSelectedPolygon().getVertices().length)
            return getSelectedPolygon().getVertices();

        // Allocate cached polygon ids if the size doesn't match.
        if (getSelectedPolygon().getVertexCount() != this.cachedSelectedPolygonVertexIds.length)
            this.cachedSelectedPolygonVertexIds = new int[getSelectedPolygon().getVertexCount()];

        System.arraycopy(getSelectedPolygon().getVertices(), 0, this.cachedSelectedPolygonVertexIds, 0, this.cachedSelectedPolygonVertexIds.length);
        return this.cachedSelectedPolygonVertexIds;
    }

    public static class MediEvilPolygonShadingEditor extends BakedLandscapePolygonShadingEditor {
        public MediEvilPolygonShadingEditor(MediEvilLandscapeUIManager manager) {
            super(manager);
        }

        @Override
        public MediEvilLandscapeUIManager getManager() {
            return (MediEvilLandscapeUIManager) super.getManager();
        }

        @Override
        protected void onVertexPositionChange(MeshView meshView, int localVertexIndex, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, int flags) {
            MediEvilMapMesh mesh = getManager().getController().getMesh();
            mesh.getMainNode().updateMapVertex(getManager().getSelectedPolygonVertexIds()[localVertexIndex]);
        }

        @Override
        protected void onColorUpdate(int colorIndex, CVector color) {
            MediEvilMapPolygon polygon = getManager().getSelectedPolygon();
            MediEvilMapMesh mesh = getManager().getController().getMesh();
            PSXShadeTextureDefinition oldShadedTexture = mesh.getShadedTextureManager().getShadedTexture(polygon);

            // Apply color changes.
            PSXShadeTextureDefinition newShadedTexture = oldShadedTexture.clone();
            newShadedTexture.getColors()[colorIndex].copyFrom(color);
            getManager().getController().getMesh().getShadedTextureManager().updatePolygon(polygon, newShadedTexture);
        }

        @Override
        protected void onTextureUvUpdate(int uvIndex, SCByteTextureUV uv) {
            MediEvilMapMesh mesh = getManager().getController().getMesh();
            MediEvilMapPolygon polygon = getManager().getSelectedPolygon();
            PSXShadeTextureDefinition oldShadedTexture = mesh.getShadedTextureManager().getShadedTexture(polygon);

            // Apply updated uv data and update the 3D view.
            PSXShadeTextureDefinition newShadedTexture = oldShadedTexture.clone();
            newShadedTexture.getTextureUVs()[uvIndex].copyFrom(uv);
            mesh.getShadedTextureManager().updatePolygon(polygon, newShadedTexture);
        }
    }
}