package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.PickResult;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseTracker;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.FXUtils;

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

            // Ensure the face clicked is the same face hovered initially. Avoids accidental face clicks.
            int intersectedFace = result.getIntersectedFace();
            InputManager inputManager = getController().getInputManager();
            MouseTracker mouseTracker = inputManager != null ? inputManager.getMouseTracker() : null;
            if (mouseTracker != null && mouseTracker.getLastDragStartMouseState().getIntersectedFaceIndex() != intersectedFace)
                return;

            // Find clicked polygon.
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
        return polygon.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled());
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

    public static class MediEvilPolygonShadingEditor extends BakedLandscapePolygonShadingEditor<MediEvilMapPolygon> {
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
            mesh.getMainNode().updateVertex(getManager().getSelectedPolygonVertexIds()[localVertexIndex]);
        }

        @Override
        protected void onColorUpdate(int colorIndex, CVector color) {
            MediEvilMapPolygon polygon = getEditTarget();
            MediEvilMapMesh mesh = getManager().getController().getMesh();

            // Apply color changes.
            if (polygon.getPolygonType().getColorCount() <= colorIndex)
                throw new RuntimeException("Cannot apply color " + colorIndex + " to polygon of type: " + polygon.getPolygonType() + ".");

            // Apply color and update polygon.
            SVector vertex = getManager().getMap().getGraphicsPacket().getVertices().get(polygon.getVertices()[colorIndex]);
            vertex.setPadding(MediEvilMapPolygon.toPackedShort(color));
            mesh.getShadedTextureManager().updatePolygon(polygon);
        }

        @Override
        protected void onTextureUvUpdate(int uvIndex, SCByteTextureUV uv) {
            MediEvilMapPolygon polygon = getEditTarget();
            MediEvilMapMesh mesh = getManager().getController().getMesh();

            // Apply updated uv data and update the 3D view.
            polygon.getTextureUvs()[uvIndex].copyFrom(uv);
            mesh.getShadedTextureManager().updatePolygon(polygon);
        }

        @Override
        protected void selectNewTexture(ITextureSource oldTextureSource) {
            MediEvilLevelTableEntry levelTableEntry = getManager().getMap().getLevelTableEntry();
            if (levelTableEntry == null) {
                FXUtils.makePopUp("There is no level table entry to lookup the remap from.", AlertType.ERROR);
                return;
            }

            TextureRemapArray remapArray = levelTableEntry.getRemap();
            if (remapArray == null) {
                FXUtils.makePopUp("There is no texture remap available to assign textures from.", AlertType.ERROR);
                return;
            }

            // Resolve VLO.
            VloFile vloArchive = oldTextureSource instanceof VloImage ? ((VloImage) oldTextureSource).getParent() : null;
            if (vloArchive == null)
                vloArchive = levelTableEntry.getVloFile();
            if (getEditTarget() == null)
                return;

            remapArray.askUserToSelectImage(vloArchive, false, selectedImage -> {
                MediEvilMapPolygon polygon = getEditTarget();
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

                setShadeDefinition(polygon, getManager().createPolygonShadeDefinition(polygon));
                getManager().getMesh().getShadedTextureManager().updatePolygon(polygon);
                getManager().getMesh().getMainNode().updateTexCoords(polygon);
            });
        }
    }
}