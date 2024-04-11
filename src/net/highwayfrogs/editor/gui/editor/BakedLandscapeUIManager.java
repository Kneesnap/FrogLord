package net.highwayfrogs.editor.gui.editor;

import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Translate;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.RawColorTextureSource;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.fxobject.PSXShadingEditor;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo;
import net.highwayfrogs.editor.gui.mesh.fxobject.TranslationGizmo.IPositionChangeListener;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows viewing polygon data for baked polygons in a static mesh.
 * This is not generally expected to be used, but is a very helpful utility to include for development & research purposes.
 * A separate map editor is used to actually edit terrain data usually.
 * Created by Kneesnap on 1/23/2024.
 */
public abstract class BakedLandscapeUIManager<TMesh extends DynamicMesh, TPolygon> extends MeshUIManager<TMesh> {
    private DisplayList vertexDisplayList;
    @Getter private UISidePanel sidePanel;
    @Getter private BakedLandscapePolygonShadingEditor shadingEditor;
    @Getter private ImageView imagePreview;

    // 3D selection of current polygon.
    private OverlayTarget selectedPolygonTarget;
    @Getter private TPolygon selectedPolygon;
    @Getter private AtomicReference<MeshView>[] vertexGizmos;
    @Getter private Sphere[] vertexSpheres;

    private static final PhongMaterial MATERIAL_GREEN = Utils.makeSpecialMaterial(Color.LIME);
    private static final PhongMaterial MATERIAL_YELLOW = Utils.makeSpecialMaterial(Color.YELLOW);
    private static final PhongMaterial MATERIAL_RED = Utils.makeSpecialMaterial(Color.RED);
    private static final PhongMaterial MATERIAL_BLUE = Utils.makeSpecialMaterial(Color.BLUE);
    public static final PhongMaterial[] VERTEX_MATERIALS = {MATERIAL_YELLOW, MATERIAL_GREEN, MATERIAL_RED, MATERIAL_BLUE};

    public static final UUID VERTEX_POSITION_EDITOR_ID = UUID.randomUUID();
    public static final RawColorTextureSource MATERIAL_POLYGON_HIGHLIGHT = new RawColorTextureSource(javafx.scene.paint.Color.rgb(255, 255, 0, .333F));

    public BakedLandscapeUIManager(MeshViewController<TMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();
        this.vertexDisplayList = getRenderManager().createDisplayList();

        // Unchanging UI Fields
        this.sidePanel = getController().createSidePanel("Landscape Polygon Information", true);
        GUIEditorGrid mainGrid = this.sidePanel.makeEditorGrid();
        this.sidePanel.setVisible(false);

        // Setup Image Preview
        this.imagePreview = new ImageView();
        this.imagePreview.setFitWidth(256);
        this.imagePreview.setFitHeight(256);
        mainGrid.addCenteredImageView(this.imagePreview);

        // Setup Shading Editor
        this.shadingEditor = createShadingEditor();
        this.shadingEditor.setupStaticUI(mainGrid);

        // Create dynamic editor portion.
        GUIEditorGrid dynamicShadingGrid = this.sidePanel.makeEditorGrid();
        this.shadingEditor.setupDynamicUI(dynamicShadingGrid, getController());
    }

    @Override
    public void onRemove() {
        super.onRemove();
        this.shadingEditor.dispose();
    }

    /**
     * Returns the overlay node used to highlight polygons.
     * If null is returned, polygon highlighting will not occur.
     */
    protected abstract DynamicMeshOverlayNode getPolygonHighlightNode();

    /**
     * Creates the shading editor.
     */
    protected abstract BakedLandscapePolygonShadingEditor createShadingEditor();

    /**
     * Creates a polygon shade definition for the polygon.
     * @param polygon the polygon to create the shade definition for
     * @return newPolygonShadeDefinition
     */
    public abstract PSXShadeTextureDefinition createPolygonShadeDefinition(TPolygon polygon);

    /**
     * Gets the mesh entry containing the polygon.
     * @param polygon polygon to search
     * @return meshDataEntry
     */
    public abstract DynamicMeshDataEntry getMeshEntryForPolygon(TPolygon polygon);

    /**
     * Gets the vertex data for a particular ID.
     * @param vertexId The map vertex ID.
     * @return vertex
     */
    protected abstract SVector getVertex(int vertexId);

    /**
     * Gets vertex ids of the selected polygon.
     * @return vertex ids, or null.
     */
    protected abstract int[] getSelectedPolygonVertexIds();

    /**
     * Selects the currently highlighted polygon.
     * This is for display purposes only, any mesh editing should occur in the separate mesh height-field editor.
     * @param polygon the polygon to highlight, or null
     */
    @SuppressWarnings("unchecked")
    public void selectPolygon(TPolygon polygon) {
        if (this.selectedPolygon == polygon)
            return;

        // Remove any position editor.
        getController().getMarkerManager().removeGizmo(VERTEX_POSITION_EDITOR_ID);

        // De-select old polygon.
        if (this.selectedPolygon != null) {
            DynamicMeshOverlayNode highlightNode = getPolygonHighlightNode();
            if (highlightNode != null)
                highlightNode.remove(this.selectedPolygonTarget);

            this.selectedPolygonTarget = null;
            this.selectedPolygon = null;
            this.vertexDisplayList.clear();
            if (this.vertexSpheres != null)
                Arrays.fill(this.vertexSpheres, null);
            if (this.vertexGizmos != null)
                Arrays.fill(this.vertexGizmos, null);
        }

        // No new polygon to select.
        if (polygon == null) {
            this.shadingEditor.setShadeDefinition(null);
            return;
        }

        // Find the mesh entry representing the target polygon.
        DynamicMeshDataEntry polygonMeshEntry = getMeshEntryForPolygon(polygon);
        if (polygonMeshEntry == null) {
            this.shadingEditor.setShadeDefinition(null);
            return;
        }

        this.selectedPolygon = polygon;

        // Highlight the polygon.
        DynamicMeshOverlayNode highlightNode = getPolygonHighlightNode();
        if (highlightNode != null) {
            this.selectedPolygonTarget = new OverlayTarget(polygonMeshEntry, MATERIAL_POLYGON_HIGHLIGHT);
            highlightNode.add(this.selectedPolygonTarget);
        }

        // Create vertex displays.
        int[] vertexIds = getSelectedPolygonVertexIds();
        if (vertexIds != null) {
            if (this.vertexSpheres == null || this.vertexSpheres.length != vertexIds.length)
                this.vertexSpheres = new Sphere[vertexIds.length];
            if (this.vertexGizmos == null || this.vertexGizmos.length != vertexIds.length)
                this.vertexGizmos = new AtomicReference[vertexIds.length];

            for (int i = 0; i < vertexIds.length; i++) {
                SVector vertex = getVertex(vertexIds[i]);
                if (vertex != null)
                    this.vertexSpheres[i] = this.vertexDisplayList.addSphere(vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ(), 1, VERTEX_MATERIALS[i], false);
            }
        }

        // Update Editor UI
        this.shadingEditor.setShadeDefinition(createPolygonShadeDefinition(polygon));
        this.sidePanel.requestFocus();
    }

    @Override
    public void updateEditor() {
        super.updateEditor();
        this.shadingEditor.updateUI();
    }

    @Getter
    public static abstract class BakedLandscapePolygonShadingEditor extends PSXShadingEditor {
        private final BakedLandscapeUIManager<?, ?> manager;
        private final IPositionChangeListener vertexPositionChangeListener;

        public BakedLandscapePolygonShadingEditor(BakedLandscapeUIManager<?, ?> manager) {
            super(null, manager.getImagePreview());
            this.manager = manager;
            this.vertexPositionChangeListener = this::onVertexPositionChangeReceived;
        }

        @Override
        public void setupDynamicUI(GUIEditorGrid grid, MeshViewController<?> controller) {
            super.setupDynamicUI(grid, controller);
            if (getShadeDefinition() == null)
                return; // No shader definition, no update.

            int[] vertexIds = getManager().getSelectedPolygonVertexIds();
            if (vertexIds != null && vertexIds.length > 0) {
                grid.addBoldLabel("Vertices:");
                String[] vertexNames = getShadeDefinition().getVertexNames(); // Vertices aren't flipped.
                for (int i = 0; i < vertexIds.length; i++) {
                    int vertexId = vertexIds[i];
                    SVector vertexPos = getManager().getVertex(vertexId);
                    if (vertexPos != null)
                        getManager().getVertexGizmos()[i] = grid.addPositionEditor(controller, VERTEX_POSITION_EDITOR_ID, vertexNames[i] + " (" + vertexId + ")", vertexPos, this.vertexPositionChangeListener);
                }
            }
        }

        private void onVertexPositionChangeReceived(MeshView meshView, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, int flags) {
            // Find the sphere corresponding to the moved gizmo.
            // It's a little jank, but I'd like to wait and see future listeners of the translation gizmo before deciding how it should be changed to make this easier (if it even should be).
            Sphere sphere = null;
            int vertexIndex = -1;
            if (meshView != null) {
                for (int i = 0; i < getManager().getVertexGizmos().length; i++) {
                    AtomicReference<MeshView> vertexGizmo = getManager().getVertexGizmos()[i];
                    if (vertexGizmo != null && vertexGizmo.get() == meshView) {
                        sphere = getManager().getVertexSpheres()[i];
                        vertexIndex = i;
                        break;
                    }
                }
            }

            // Update sphere position.
            if (sphere != null) {
                Translate spherePos = Scene3DUtils.get3DTranslation(sphere);
                if ((flags & TranslationGizmo.X_CHANGED_FLAG) == TranslationGizmo.X_CHANGED_FLAG)
                    spherePos.setX(newX);
                if ((flags & TranslationGizmo.Y_CHANGED_FLAG) == TranslationGizmo.Y_CHANGED_FLAG)
                    spherePos.setY(newY);
                if ((flags & TranslationGizmo.Z_CHANGED_FLAG) == TranslationGizmo.Z_CHANGED_FLAG)
                    spherePos.setZ(newZ);
            }

            // Allow any further handling such as updating the map mesh.
            onVertexPositionChange(meshView, vertexIndex, oldX, oldY, oldZ, newX, newY, newZ, flags);
        }

        /**
         * Called when the vertex position changed.
         */
        protected abstract void onVertexPositionChange(MeshView meshView, int localVertexIndex, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, int flags);

        @Override
        public void setShadeDefinition(PSXShadeTextureDefinition shadeDefinition) {
            boolean oldDefinitionExisted = getShadeDefinition() != null;
            boolean newDefinitionExists = (shadeDefinition != null);
            super.setShadeDefinition(shadeDefinition);

            if (!oldDefinitionExisted && newDefinitionExists) {
                this.manager.getSidePanel().requestFocus();
            } else if (oldDefinitionExisted && !newDefinitionExists) {
                this.manager.getSidePanel().setVisible(false);
            }
        }
    }
}