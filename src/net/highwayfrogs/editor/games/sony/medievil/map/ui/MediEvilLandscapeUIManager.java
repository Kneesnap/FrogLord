package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.input.PickResult;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.psx.math.vector.CVector;
import net.highwayfrogs.editor.games.psx.math.vector.SVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.medievil.MediEvilLevelTableEntry;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygon;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygonSortMode;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.InputManager.MouseTracker;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.basic.RawColorTextureSource;
import net.highwayfrogs.editor.utils.FXUtils;

import java.util.List;

/**
 * Manages UI relating to the landscape/terrain of a MediEvil map.
 * Created by Kneesnap on 3/16/2024.
 */
public class MediEvilLandscapeUIManager extends BakedLandscapeUIManager<MediEvilMapMesh, MediEvilMapPolygon> {
    @Getter private CheckBox highlightSpecialPolygonsCheckBox;

    public static final javafx.scene.paint.Color POLYGON_HIGHLIGHT_BLUE = javafx.scene.paint.Color.rgb(0, 0, 255, .333F);
    public static final RawColorTextureSource MATERIAL_POLYGON_BLUE = new RawColorTextureSource(POLYGON_HIGHLIGHT_BLUE);


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

    private void updateSpecialPolygonPreview() {
        List<MediEvilMapPolygon> polygons = getMap().getGraphicsPacket().getPolygons();
        ITextureSource texturePreview = this.highlightSpecialPolygonsCheckBox.isSelected() ? MATERIAL_POLYGON_BLUE : null;
        for (int i = 0; i < polygons.size(); i++) {
            MediEvilMapPolygon polygon = polygons.get(i);
            getPolygonHighlightNode().setOverlayTexture(getMeshEntryForPolygon(polygon), polygon.isFlagMaskSet(MediEvilMapPolygon.FLAG_SPECIAL) ? texturePreview : null);
        }
    }

    public static class MediEvilPolygonShadingEditor extends BakedLandscapePolygonShadingEditor<MediEvilMapPolygon> {
        private ComboBox<MediEvilMapPolygonSortMode> polygonSortModeSelector;
        private CheckBox polygonIsTriangleADownCheckBox;
        private CheckBox polygonIsTriangleBDownCheckBox;
        private CheckBox polygonIsSpecialCheckBox;

        public MediEvilPolygonShadingEditor(MediEvilLandscapeUIManager manager) {
            super(manager);
        }

        @Override
        public MediEvilLandscapeUIManager getManager() {
            return (MediEvilLandscapeUIManager) super.getManager();
        }

        @Override
        public void setupStaticUI(GUIEditorGrid grid) {
            getManager().highlightSpecialPolygonsCheckBox = grid.addCheckBox("Highlight Special Polygons", false, newValue -> getManager().updateSpecialPolygonPreview());
            grid.addSeparator();

            super.setupStaticUI(grid);
            // Polygon type information.
            this.polygonSortModeSelector = grid.addEnumSelector("Sort Mode", MediEvilMapPolygonSortMode.SORT_BY_AVERAGE_Z_ALLOW_OVERRIDE, MediEvilMapPolygonSortMode.values(), false, null);
            this.polygonIsTriangleADownCheckBox = grid.addCheckBox("Triangle A Faces Down", false, null);
            this.polygonIsTriangleBDownCheckBox = grid.addCheckBox("Triangle B Faces Down", false, null);
            this.polygonIsSpecialCheckBox = grid.addCheckBox("Special (Usable by game code)", false, null);
            this.polygonSortModeSelector.setDisable(true);
            this.polygonIsTriangleADownCheckBox.setDisable(true);
            this.polygonIsTriangleBDownCheckBox.setDisable(true);
            this.polygonIsSpecialCheckBox.setDisable(true);

            this.polygonSortModeSelector.setTooltip(FXUtils.createTooltip("The polygon may have this overridden by its texture's sort mode."));
            this.polygonIsSpecialCheckBox.setTooltip(FXUtils.createTooltip("The purpose of this flag is currently unknown."));

            this.polygonSortModeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (!this.polygonSortModeSelector.isDisabled() && getEditTarget() != null && newValue != null)
                    getEditTarget().setSortMode(newValue);
            });

            this.polygonIsSpecialCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (!this.polygonIsSpecialCheckBox.isDisabled() && getEditTarget() != null) {
                    getEditTarget().setFlagMask(MediEvilMapPolygon.FLAG_SPECIAL, newValue);
                    getManager().updateSpecialPolygonPreview();
                }
            });
        }

        @Override
        public void updateUI() {
            if (isStaticUISetup()) {
                MediEvilMapPolygon polygon = getEditTarget();
                this.polygonSortModeSelector.setDisable(true);
                this.polygonIsSpecialCheckBox.setDisable(true);

                this.polygonSortModeSelector.getSelectionModel().select(polygon != null ? polygon.getSortMode() : MediEvilMapPolygonSortMode.SORT_BY_AVERAGE_Z_ALLOW_OVERRIDE);
                this.polygonIsTriangleADownCheckBox.setSelected(polygon != null && polygon.isFlagMaskSet(MediEvilMapPolygon.FLAG_TRIANGLE_A_DOWN));
                this.polygonIsTriangleBDownCheckBox.setSelected(polygon != null && polygon.isFlagMaskSet(MediEvilMapPolygon.FLAG_TRIANGLE_B_DOWN));
                this.polygonIsSpecialCheckBox.setSelected(polygon != null && polygon.isFlagMaskSet(MediEvilMapPolygon.FLAG_SPECIAL));

                // These behavior patterns are untested/might not be consistent with the game.
                this.polygonSortModeSelector.setDisable(polygon == null);
                this.polygonIsSpecialCheckBox.setDisable(polygon == null);
            }

            super.updateUI();
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
                FXUtils.showPopup(AlertType.ERROR, "No level table entry found.", "There is no level table entry to lookup the remap from.");
                return;
            }

            TextureRemapArray remapArray = levelTableEntry.getRemap();
            if (remapArray == null) {
                FXUtils.showPopup(AlertType.ERROR, "Could not find texture remap.", "There is no texture remap available to assign textures from.");
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