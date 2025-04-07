package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
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
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.CVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridSquare;
import net.highwayfrogs.editor.games.sony.frogger.map.data.grid.FroggerGridStack;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketGrid;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.SelectionPromptTracker;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Allows viewing / editing baked geometry data in a Frogger map.
 * TODO: Create some generalized selection system which integrates in 3D space allowing a smooth highlighting and selection between different types of things.
 * Created by Kneesnap on 6/6/2024.
 */
public class FroggerUIGeometryManager extends BakedLandscapeUIManager<FroggerMapMesh, FroggerMapPolygon> {
    private static final double VERTEX_BOX_SIZE = 1.5;
    private static final PhongMaterial VERTEX_BOX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    @Getter private final SelectionPromptTracker<FroggerMapPolygon> polygonSelector;

    // UI
    private DisplayList unusedVertexBoxes;
    private CheckBox checkBoxShowUnusedVertices;
    private CheckBox checkBoxHighlightInvisibleFaces;

    // Polygon cursor data.
    private FroggerMapPolygon highlightedPolygon;
    private OverlayTarget highlightedPolygonTarget;
    private MeshView hoverView; // Anything we're hovering over which should allow selection instead of the cursor.

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

    @Override
    public void onSetup() {
        // Setup UI Pane & Grid.
        this.unusedVertexBoxes = getRenderManager().createDisplayList();
        this.sidePanel = getController().createSidePanel("Baked Map Geometry (Polygons)");

        // Setup UI.
        GUIEditorGrid mainGrid = this.sidePanel.makeEditorGrid();
        mainGrid.addButton("Edit Collision Grid", () -> FroggerUIGridManager.openGridEditor(getController()));
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
            Node clickedNode = manager.getMouseState().getIntersectedNode();
            if (clickedNode != getController().getMeshView() || (this.hoverView != null) || (this.highlightedPolygon == null))
                return;

            FroggerMapPolygon clickedPolygon = getMesh().getMainNode().getDataSourceByFaceIndex(manager.getMouseState().getIntersectedFaceIndex());
            if (clickedPolygon != this.highlightedPolygon)
                return;

            event.consume();
            handleClick(clickedPolygon);
        });

        updateUnusedVertexVisibility();
        super.onSetup();
    }

    /**
     * Handles when a polygon is clicked.
     * @param clickedPolygon the polygon which has been clicked
     */
    protected void handleClick(FroggerMapPolygon clickedPolygon) {
        // If we're looking at invisible faces, toggle visibility.
        if (this.checkBoxHighlightInvisibleFaces.isSelected()) {
            clickedPolygon.setVisible(!clickedPolygon.isVisible());
            return;
        }

        // If the animation should be applied.
        FroggerUIMapAnimationManager animationManager = getController().getAnimationManager();

        // Animations can only be applied to textured polygons, because the game doesn't have an appropriate memory allocation otherwise.
        // It appears triangles are valid to include animations on, as their structs all line up.
        if (animationManager != null && animationManager.getEditAnimationPolygonTargetsCheckBox().isSelected() && clickedPolygon.getPolygonType().isTextured()) {
            FroggerMapAnimation animation = animationManager.getSelectedValue();
            if (animation != null) {
                FroggerMapAnimationTargetPolygon existingTargetPolygon = getMap().getAnimationPacket().getAnimationTarget(clickedPolygon);
                if (existingTargetPolygon != null && existingTargetPolygon.getAnimation() == animation) {
                    existingTargetPolygon.setPolygon(null); // Removes from tracking.
                    animation.getTargetPolygons().remove(existingTargetPolygon);
                    animationManager.updateAnimatedPolygonHighlighting();
                } else if (existingTargetPolygon == null) {
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
        return getMap().getVertexPacket().getVertices().get(vertexId);
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
        getMesh().getHighlightedInvisiblePolygonNode().clear();

        if (visible) {
            List<FroggerMapPolygon> polygons = getMap().getPolygonPacket().getPolygons();
            for (int i = 0; i < polygons.size(); i++) {
                FroggerMapPolygon polygon = polygons.get(i);
                if (!polygon.isVisible())
                    getMesh().getHighlightedInvisiblePolygonNode().add(createOverlayTarget(polygon, FroggerMapMesh.GREEN_COLOR));
            }
        }
    }

    /**
     * Update the grid polygon highlighting.
     * @param visible Whether it should be visible
     */
    public void updateGridPolygonHighlighting(boolean visible) {
        getMesh().getHighlightedGridPolygonNode().clear();

        if (visible) {
            FroggerMapFilePacketGrid gridPacket = getMap().getGridPacket();
            for (int z = 0; z < gridPacket.getGridZCount(); z++) {
                for (int x = 0; x < gridPacket.getGridXCount(); x++) {
                    FroggerGridStack gridStack = gridPacket.getGridStack(x, z);
                    for (int i = 0; i < gridStack.getGridSquares().size(); i++) {
                        FroggerGridSquare gridSquare = gridStack.getGridSquares().get(i);
                        if (gridSquare.getPolygon() != null)
                            getMesh().getHighlightedGridPolygonNode().add(createOverlayTarget(gridSquare.getPolygon(), FroggerMapMesh.BLUE_COLOR));
                    }
                }
            }
        }
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
            this.polygonIsMaxOrderTableCheckBox = grid.addCheckBox("Max Order Table", false, null);
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
                this.polygonIsSemiTransparentCheckBox.setDisable(polygon == null);
                this.polygonIsEnvironmentMappedCheckBox.setDisable(polygon == null);
                this.polygonIsMaxOrderTableCheckBox.setDisable(polygon == null);
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
    }
}