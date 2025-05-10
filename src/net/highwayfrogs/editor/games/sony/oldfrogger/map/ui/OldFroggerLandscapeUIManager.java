package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.input.PickResult;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.oldfrogger.config.OldFroggerLevelTableEntry;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseTracker;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;

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

            // Ensure the face clicked is the same face hovered initially. Avoids accidental face clicks.
            int intersectedFace = result.getIntersectedFace();
            InputManager inputManager = getController().getInputManager();
            MouseTracker mouseTracker = inputManager != null ? inputManager.getMouseTracker() : null;
            if (mouseTracker != null && mouseTracker.getLastDragStartMouseState().getIntersectedFaceIndex() != intersectedFace)
                return;

            // Find clicked polygon.
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
        return polygon.createPolygonShadeDefinition(getMesh());
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

    public static class OldFroggerPolygonShadingEditor extends BakedLandscapePolygonShadingEditor<OldFroggerMapPolygon> {
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
            mesh.getMainNode().updateVertex(getManager().getSelectedPolygonVertexIds()[localVertexIndex]);
        }

        @Override
        protected void onColorUpdate(int colorIndex, CVector color) {
            OldFroggerMapPolygon polygon = getEditTarget();
            OldFroggerMapMesh mesh = getManager().getController().getMesh();

            // Apply color changes.
            polygon.getColors()[colorIndex].copyFrom(color);
            mesh.getShadedTextureManager().updatePolygon(polygon);
        }

        @Override
        protected void onTextureUvUpdate(int uvIndex, SCByteTextureUV uv) {
            OldFroggerMapPolygon polygon = getEditTarget();
            OldFroggerMapMesh mesh = getManager().getController().getMesh();

            // Apply updated uv data and update the 3D view.
            polygon.getTextureUvs()[uvIndex].copyFrom(uv);
            mesh.getShadedTextureManager().updatePolygon(polygon);
        }

        @Override
        protected void selectNewTexture(ITextureSource oldTextureSource) {
            OldFroggerLevelTableEntry levelTableEntry = getManager().getMap().getLevelTableEntry();
            if (levelTableEntry == null)
                return;

            TextureRemapArray remapArray = levelTableEntry.getTextureRemap();
            if (remapArray == null)
                return;

            // Resolve VLO.
            VLOArchive vloArchive = oldTextureSource instanceof GameImage ? ((GameImage) oldTextureSource).getParent() : null;
            if (vloArchive == null)
                vloArchive = levelTableEntry.getMainVLOArchive();
            if (vloArchive == null || getEditTarget() == null)
                return;

            remapArray.askUserToSelectImage(vloArchive, false, selectedImage -> {
                OldFroggerMapPolygon polygon = getEditTarget();
                if (!shouldHandleUIChanges() || !getShadeDefinition().isTextured() || polygon == null)
                    return;

                if (selectedImage != null) {
                    int remapIndex = remapArray.getRemapIndex(selectedImage.getTextureId());
                    if (remapIndex < 0)
                        throw new RuntimeException("Could not find the selected image (" + selectedImage.getTextureId() + ") in the textureRemap!");

                    polygon.setTextureId(remapIndex);
                } else {
                    polygon.setTextureId(-1);
                }

                getManager().getController().getMesh().getShadedTextureManager().updatePolygon(polygon);
            });
        }
    }
}