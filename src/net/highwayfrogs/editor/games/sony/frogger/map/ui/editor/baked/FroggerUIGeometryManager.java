package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
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
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.InputManager.MouseInputState;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.SelectionPromptTracker;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Allows viewing / editing baked geometry data in a Frogger map.
 * Created by Kneesnap on 6/6/2024.
 */
public class FroggerUIGeometryManager extends BakedLandscapeUIManager<FroggerMapMesh, FroggerMapPolygon> {
    private static final double VERTEX_BOX_SIZE = 1.5;
    private static final PhongMaterial VERTEX_BOX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    @Getter private final SelectionPromptTracker<FroggerMapPolygon> polygonSelector;
    @Getter private FroggerUIGridManager gridEditorWindow;

    // UI
    private DisplayList unusedVertexBoxes;
    private CheckBox checkBoxShowUnusedVertices;
    private CheckBox checkBoxHighlightInvisibleFaces;

    // Polygon cursor data.
    private FroggerMapPolygon highlightedPolygon;
    private OverlayTarget highlightedPolygonTarget;
    private MeshView hoverView; // Anything we're hovering over which should allow selection instead of the cursor.

    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(java.awt.Color.GREEN, java.awt.Color.BLACK);

    public FroggerUIGeometryManager(FroggerMapMeshController controller) {
        super(controller);
        this.polygonSelector = new FroggerBakedMapPolygonSelector(this);
    }

    @Override
    public FroggerMapMeshController getController() {
        return (FroggerMapMeshController) super.getController();
    }

    @Override
    public boolean isSidePaneHiddenWhenNoPolygon() {
        return false;
    }

    private void tryOpenGridEditor() {
        if (this.gridEditorWindow != null && this.gridEditorWindow.isActive())
            return; // Window is currently active.

        this.gridEditorWindow = FroggerUIGridManager.openGridEditor(getController());
    }

    @Override
    public void onSetup() {
        // Setup UI Pane & Grid.
        this.unusedVertexBoxes = getRenderManager().createDisplayList();
        this.sidePanel = getController().createSidePanel("Baked Map Geometry (Polygons)");

        // Setup UI.
        GUIEditorGrid mainGrid = this.sidePanel.makeEditorGrid();
        mainGrid.addButton("Edit Collision Grid", this::tryOpenGridEditor);
        this.checkBoxShowUnusedVertices = mainGrid.addCheckBox("Show Unused Vertices", true, this::updateUnusedVertexVisibility);
        this.checkBoxHighlightInvisibleFaces = mainGrid.addCheckBox("Highlight Invisible Polygons", false, this::updateInvisiblePolygonHighlighting);
        this.sidePanel.add(new Separator(Orientation.HORIZONTAL));

        Scene mapScene = getController().getMeshScene();
        mapScene.setOnMousePressed(e -> {
            getController().getAnchorPaneUIRoot().requestFocus();
            if (!isPolygonSelected()) // Remove the highlighting in preparation for movement.
                removePolygonHighlighting();
        });

        mapScene.setOnMouseMoved(evt -> {
            if (getController().getPathManager().getPathSelector().isPromptActive())
                return; // Don't highlight polygons while the path selector is active, it's distracting.

            Node node = evt.getPickResult().getIntersectedNode();
            if (node == getController().getMeshView() && !isPolygonSelected() && this.hoverView == null) {
                FroggerMapPolygon polygon = getMesh().getMainNode().getDataSourceByFaceIndex(evt.getPickResult().getIntersectedFace());
                if (polygon != null)
                    highlightPolygon(polygon);
            }
        });

        // Setup unused vertex box rendering.
        this.checkBoxShowUnusedVertices.setOnAction(evt -> updateUnusedVertexVisibility());

        getController().getInputManager().addMouseListener(MouseEvent.MOUSE_CLICKED, (manager, event, deltaX, deltaY) -> {
            MouseInputState mouseState = manager.getMouseTracker().getMouseState();
            Node clickedNode = mouseState.getIntersectedNode();
            if (clickedNode != getController().getMeshView() || (this.hoverView != null) || (this.highlightedPolygon == null))
                return;

            FroggerMapPolygon clickedPolygon = getMesh().getMainNode().getDataSourceByFaceIndex(mouseState.getIntersectedFaceIndex());
            if (clickedPolygon != this.highlightedPolygon)
                return;

            handleClick(event, clickedPolygon);
            event.consume();
        });

        updateUnusedVertexVisibility();
        super.onSetup();
    }

    @Override
    public void onRemove() {
        super.onRemove();
        if (this.gridEditorWindow != null) {
            this.gridEditorWindow.closeWindow();
            this.gridEditorWindow = null;
        }
    }

    /**
     * Handles when a polygon is clicked.
     * @param clickedPolygon the polygon which has been clicked
     */
    protected void handleClick(MouseEvent event, FroggerMapPolygon clickedPolygon) {
        if (this.polygonSelector.handleClick(event, clickedPolygon))
            return;

        // If we're looking at invisible faces, toggle visibility.
        if (this.checkBoxHighlightInvisibleFaces.isSelected()) {
            clickedPolygon.setVisible(!clickedPolygon.isVisible());
            updateInvisiblePolygonHighlight(clickedPolygon);
            return;
        }

        // If the animation should be applied.
        FroggerUIMapAnimationManager animationManager = getController().getAnimationManager();

        // Animations can only be applied to textured polygons, because the game doesn't have an appropriate memory allocation otherwise.
        // It appears triangles are valid to include animations on, as their structs all line up.
        if (animationManager != null && animationManager.getEditAnimationPolygonTargetsCheckBox().isSelected()) {
            if (!clickedPolygon.getPolygonType().isTextured()) {
                FXUtils.makePopUp("The selected polygon is not textured, so it cannot be animated.", AlertType.WARNING);
                return;
            }

            FroggerMapAnimation animation = animationManager.getSelectedValue();
            if (animation != null) {
                FroggerMapAnimationTargetPolygon existingTargetPolygon = getMap().getAnimationPacket().getAnimationTarget(clickedPolygon);
                if (existingTargetPolygon != null) {
                    if (existingTargetPolygon.getAnimation() != animation) {
                        FXUtils.makePopUp("The polygon uses another animation.", AlertType.WARNING);
                        return;
                    }

                    existingTargetPolygon.setPolygon(null); // Removes from tracking.
                    animation.getTargetPolygons().remove(existingTargetPolygon);
                    animationManager.updateAnimatedPolygonHighlighting();
                } else {
                    animation.getTargetPolygons().add(new FroggerMapAnimationTargetPolygon(animation, clickedPolygon));
                    animationManager.updateAnimatedPolygonHighlighting();
                }

                return;
            }
        }

        // Default polygon click behavior.
        if (isPolygonSelected()) {
            selectPolygon(null);
            applyHighlighting(this.highlightedPolygon);
        } else {
            highlightPolygon(clickedPolygon);
            selectPolygon(clickedPolygon);
            updateEditor(); // Update editor.
            this.sidePanel.requestFocus();
        }
    }

    @Override
    protected DynamicMeshOverlayNode getPolygonHighlightNode() {
        return getMesh().getHighlightedMousePolygonNode();
    }

    @Override
    protected FroggerBakedPolygonShadingEditor createShadingEditor() {
        return new FroggerBakedPolygonShadingEditor(this);
    }

    @Override
    public PSXShadeTextureDefinition createPolygonShadeDefinition(FroggerMapPolygon polygon) {
        return polygon.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled(), null, -1);
    }

    @Override
    public DynamicMeshDataEntry getMeshEntryForPolygon(FroggerMapPolygon polygon) {
        return getMesh().getMainNode().getDataEntry(polygon);
    }

    @Override
    protected SVector getVertex(int vertexId) {
        List<SVector> vertices = getMap().getVertexPacket().getVertices();
        return vertices.size() > vertexId && vertexId >= 0 ? vertices.get(vertexId) : null;
    }

    @Override
    protected int[] getSelectedPolygonVertexIds() {
        return getSelectedPolygon() != null ? getSelectedPolygon().getVertices() : null;
    }

    /**
     * Gets the map file.
     */
    public FroggerMapFile getMap() {
        return getMesh().getMap();
    }

    /**
     * Setup a node so the cursor will not be shown when the mouse hovers over the node.
     * @param view The node to setup.
     */
    public void setupMeshViewHighlightSkipHover(MeshView view) {
        view.setOnMouseEntered(evt -> this.hoverView = (MeshView) evt.getSource());
        view.setOnMouseExited(evt -> onStopHover((MeshView) evt.getSource()));
    }

    /**
     * Called when the mouse stops hovering over a given mesh.
     * @param view The view to stop.
     */
    private void onStopHover(MeshView view) {
        if (Objects.equals(view, this.hoverView))
            this.hoverView = null;
    }

    @Override
    public boolean onKeyPress(KeyEvent event) {
        if (isPolygonSelected() && event.getCode() == KeyCode.ESCAPE) {
            deselectHighlightedPolygon();
            return true;
        }

        return super.onKeyPress(event);
    }

    private void updateInvisiblePolygonHighlighting(boolean visible) {
        getMesh().pushBatchOperations();
        getMesh().getHighlightedInvisiblePolygonNode().clear();

        if (visible) {
            List<FroggerMapPolygon> polygons = getMap().getPolygonPacket().getPolygons();
            for (int i = 0; i < polygons.size(); i++) {
                FroggerMapPolygon polygon = polygons.get(i);
                if (!polygon.isVisible())
                    updateInvisiblePolygonHighlight(polygon);
            }
        }

        getMesh().popBatchOperations();
    }

    private void updateInvisiblePolygonHighlight(FroggerMapPolygon polygon) {
        ITextureSource overlayTexture = polygon.isVisible() ? null : GREEN_COLOR;
        DynamicMeshDataEntry polygonDataEntry = getMesh().getMainNode().getDataEntry(polygon);
        getMesh().getHighlightedInvisiblePolygonNode().setOverlayTexture(polygonDataEntry, overlayTexture);
    }

    /**
     * Removes the highlighting from the highlighting polygon, and deselects the polygon if it is currently selected.
     * The polygon cannot be re-highlighted.
     */
    public void deselectHighlightedPolygon() {
        if (this.highlightedPolygon == null)
            return;

        selectPolygon(null);
        this.highlightedPolygon = null;
        removePolygonHighlighting();
    }

    /**
     * Removes the highlighting from the highlighting polygon.
     * However, the polygon in question is still tracked, and can be re-highlighted.
     */
    public void removePolygonHighlighting() {
        if (this.highlightedPolygonTarget == null)
            return;

        getMesh().getHighlightedMousePolygonNode().remove(this.highlightedPolygonTarget);
        this.highlightedPolygonTarget = null;
    }

    /**
     * Set the polygon that the cursor is hovering over.
     * @param newPolygon The poly to highlight.
     */
    public void highlightPolygon(FroggerMapPolygon newPolygon) {
        if (newPolygon == this.highlightedPolygon)
            return;

        deselectHighlightedPolygon();
        if (newPolygon != null)
            applyHighlighting(this.highlightedPolygon = newPolygon);
    }

    private void applyHighlighting(FroggerMapPolygon targetPolygon) {
        if (this.hoverView != null)
            return;

        removePolygonHighlighting();

        if (targetPolygon != null && (!isPolygonSelected() || getSelectedPolygon() != targetPolygon)) {
            this.highlightedPolygonTarget = createOverlayTarget(targetPolygon, MATERIAL_POLYGON_HIGHLIGHT);
            getMesh().getHighlightedMousePolygonNode().add(this.highlightedPolygonTarget);
        }
    }

    private OverlayTarget createOverlayTarget(FroggerMapPolygon polygon, ITextureSource textureSource) {
        return new OverlayTarget(getMesh().getMainNode().getDataEntry(polygon), textureSource);
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
            this.unusedVertexBoxes.add(box);
        }

        // Update existing boxes.
        for (int i = 0; i < this.unusedVertexBoxes.size(); i++) {
            Box box = (Box) this.unusedVertexBoxes.getNodes().get(i);
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
        this.updateUnusedVertexVisibility(this.checkBoxShowUnusedVertices.isSelected());
    }

    /**
     * Updates the vertex display.
     */
    private void updateUnusedVertexVisibility(boolean verticesVisible) {
        if (verticesVisible) {
            updateUnusedVertexBoxes(getMap().findUnusedVertices());
        } else {
            updateUnusedVertexBoxes(Collections.emptyList());
        }
    }

    /**
     * Supports selecting baked map polygons.
     */
    public static class FroggerBakedMapPolygonSelector extends SelectionPromptTracker<FroggerMapPolygon> {
        public FroggerBakedMapPolygonSelector(FroggerUIGeometryManager manager) {
            super(manager, true);
        }

        @Override
        public FroggerUIGeometryManager getUiManager() {
            return (FroggerUIGeometryManager) super.getUiManager();
        }

        @Override
        public void activate(Consumer<FroggerMapPolygon> onSelect, Runnable onCancel) {
            super.activate(onSelect, onCancel);
            getUiManager().deselectHighlightedPolygon(); // Ensure polygon clicking is available.
        }
    }

    public static class FroggerBakedPolygonShadingEditor extends BakedLandscapePolygonShadingEditor<FroggerMapPolygon> {
        private CheckBox polygonIsSemiTransparentCheckBox;
        private CheckBox polygonIsEnvironmentMappedCheckBox;
        private CheckBox polygonIsMaxOrderTableCheckBox;

        public FroggerBakedPolygonShadingEditor(FroggerUIGeometryManager manager) {
            super(manager);
        }

        @Override
        public FroggerUIGeometryManager getManager() {
            return (FroggerUIGeometryManager) super.getManager();
        }

        @Override
        public void setupStaticUI(GUIEditorGrid grid) {
            super.setupStaticUI(grid);
            // Polygon type information.
            this.polygonIsSemiTransparentCheckBox = grid.addCheckBox("Semi Transparent", false, null);
            this.polygonIsEnvironmentMappedCheckBox = grid.addCheckBox("Environment Mapped", false, null);
            this.polygonIsMaxOrderTableCheckBox = grid.addCheckBox("Water Wibble (Max OT)", false, null);
            this.polygonIsSemiTransparentCheckBox.setDisable(true);
            this.polygonIsEnvironmentMappedCheckBox.setDisable(true);
            this.polygonIsMaxOrderTableCheckBox.setDisable(true);

            this.polygonIsSemiTransparentCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (!this.polygonIsSemiTransparentCheckBox.isDisabled() && getEditTarget() != null)
                    getEditTarget().setFlagMask(FroggerMapPolygon.FLAG_SEMI_TRANSPARENT, newValue);
            });

            this.polygonIsEnvironmentMappedCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (!this.polygonIsEnvironmentMappedCheckBox.isDisabled() && getEditTarget() != null)
                    getEditTarget().setFlagMask(FroggerMapPolygon.FLAG_ENVIRONMENT_MAPPED, newValue);
            });

            this.polygonIsMaxOrderTableCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (!this.polygonIsMaxOrderTableCheckBox.isDisabled() && getEditTarget() != null)
                    getEditTarget().setFlagMask(FroggerMapPolygon.FLAG_MAX_ORDER_TABLE, newValue);
            });
        }

        @Override
        public void updateUI() {
            if (isStaticUISetup()) {
                FroggerMapPolygon polygon = getEditTarget();
                this.polygonIsSemiTransparentCheckBox.setDisable(true);
                this.polygonIsEnvironmentMappedCheckBox.setDisable(true);
                this.polygonIsMaxOrderTableCheckBox.setDisable(true);
                this.polygonIsSemiTransparentCheckBox.setSelected(polygon != null && polygon.testFlag(FroggerMapPolygon.FLAG_SEMI_TRANSPARENT));
                this.polygonIsEnvironmentMappedCheckBox.setSelected(polygon != null && polygon.testFlag(FroggerMapPolygon.FLAG_ENVIRONMENT_MAPPED));
                this.polygonIsMaxOrderTableCheckBox.setSelected(polygon != null && polygon.testFlag(FroggerMapPolygon.FLAG_MAX_ORDER_TABLE));

                boolean isTextured = polygon != null && polygon.getPolygonType().isTextured(); // FT4/GT4 are the only ones where water wibble will work. However, FT3 (unknown about GT3) seems to still set max OT, even if water wibble won't apply.
                this.polygonIsMaxOrderTableCheckBox.setText(isTextured && polygon.getPolygonType().isQuad() ? "Water Wibble (& Max OT)" : "Max OT (Draws First)");
                this.polygonIsSemiTransparentCheckBox.setDisable(!isTextured);
                this.polygonIsEnvironmentMappedCheckBox.setDisable(!isTextured);
                this.polygonIsMaxOrderTableCheckBox.setDisable(!isTextured);
            }

            super.updateUI();
        }

        @Override
        protected void onVertexPositionChange(MeshView meshView, int localVertexIndex, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, int flags) {
            FroggerMapMesh mesh = getManager().getController().getMesh();
            mesh.getMainNode().updateVertex(getManager().getSelectedPolygonVertexIds()[localVertexIndex]);
        }

        @Override
        protected void onColorUpdate(int colorIndex, CVector color) {
            FroggerMapPolygon polygon = getEditTarget();
            FroggerMapMesh mesh = getManager().getController().getMesh();

            // Apply color changes.
            polygon.getColors()[colorIndex].copyFrom(color);
            mesh.getShadedTextureManager().updatePolygon(polygon);
        }

        @Override
        protected void onTextureUvUpdate(int uvIndex, SCByteTextureUV uv) {
            FroggerMapPolygon polygon = getEditTarget();
            FroggerMapMesh mesh = getManager().getController().getMesh();

            // Apply updated uv data and update the 3D view.
            polygon.getTextureUvs()[uvIndex].copyFrom(uv);
            mesh.getShadedTextureManager().updatePolygon(polygon);
        }

        @Override
        protected void selectNewTexture(ITextureSource oldTextureSource) {
            TextureRemapArray remapArray = getManager().getMap().getTextureRemap();
            if (remapArray == null) {
                FXUtils.makePopUp("There is no texture remap available to assign textures from.", AlertType.ERROR);
                return;
            }

            // Resolve VLO.
            VLOArchive vloArchive = oldTextureSource instanceof GameImage ? ((GameImage) oldTextureSource).getParent() : null;
            if (vloArchive == null)
                vloArchive = getManager().getMap().getVloFile();
            if (getEditTarget() == null)
                return;

            remapArray.askUserToSelectImage(vloArchive, false, selectedImage -> {
                FroggerMapPolygon polygon = getEditTarget();
                if (!shouldHandleUIChanges() || !getShadeDefinition().isTextured() || polygon == null)
                    return;

                if (selectedImage != null) {
                    int remapIndex = remapArray.getRemapIndex(selectedImage.getTextureId());
                    if (remapIndex < 0)
                        throw new RuntimeException("Could not find the selected image (" + selectedImage.getTextureId() + ") in the textureRemap!");

                    polygon.setTextureId((short) remapIndex);
                } else {
                    polygon.setTextureId((short) -1);
                }

                setShadeDefinition(polygon, getManager().createPolygonShadeDefinition(polygon));
                getManager().getMesh().getShadedTextureManager().updatePolygon(polygon);
                getManager().getMesh().getMainNode().updateTexCoords(polygon);
            });
        }
    }
}