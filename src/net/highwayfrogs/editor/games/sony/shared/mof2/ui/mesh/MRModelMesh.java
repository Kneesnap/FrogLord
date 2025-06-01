package net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModelType;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModelSet;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimationPolygonTarget;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelHiliteUIManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelTextureAnimationUIManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh.MRMofShadedTextureManager;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.MeshTracker;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A JavaFX representation of the MRModel mesh seen within pre-1999 Sony Cambridge games.
 * Created by Kneesnap on 2/18/2025.
 */
@Getter
public class MRModelMesh extends PSXShadedDynamicMesh<MRMofPolygon, MRMofShadedTextureManager> {
    @NonNull private final MRModel model;
    private final List<MRModelMeshPartNode> modelMeshPartNodes = new ArrayList<>();
    private final DynamicMeshOverlayNode highlightedMousePolygonNode;
    private final DynamicMeshOverlayNode highlightedAnimatedPolygonsNode;
    private final DynamicMeshOverlayNode highlightedInvisiblePolygonNode;
    private final DynamicMeshOverlayNode highlightedSelectionPolygonNode;
    private final MRModelAnimationPlayer animationPlayer;
    private final Set<MRMofPart> hiddenParts = new HashSet<>();
    private MRAnimatedMofModel activeMofModel;
    private PhongMaterial litMaterial;
    private PhongMaterial litHighlightedMaterial;

    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);
    public static final CursorVertexColor BLUE_COLOR = new CursorVertexColor(Color.BLUE, Color.BLACK);

    public MRModelMesh(MRModel model) {
        super(new TreeTextureAtlas(16, 16, true), DynamicMeshTextureQuality.UNLIT_SHARP, true, model.getFileDisplayName());
        this.model = model;
        this.animationPlayer = new MRModelAnimationPlayer(this);

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupShadedPolygons();
        getTextureAtlas().endBulkOperations();

        this.highlightedAnimatedPolygonsNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedAnimatedPolygonsNode);

        this.highlightedInvisiblePolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedInvisiblePolygonNode);

        this.highlightedSelectionPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedSelectionPolygonNode);

        this.highlightedMousePolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedMousePolygonNode);

        // Add part meshes.
        setActiveMofModel(getFirstModel(model), null);
    }

    @Override
    protected MRMofShadedTextureManager createShadedTextureManager() {
        return new MRMofShadedTextureManager(this);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(BakedLandscapeUIManager.MATERIAL_POLYGON_HIGHLIGHT);
        getTextureAtlas().addTexture(MRModelHiliteUIManager.HILITE_COLOR);
        getTextureAtlas().addTexture(MRModelTextureAnimationUIManager.ANIMATION_COLOR);
        getTextureAtlas().addTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        getTextureAtlas().addTexture(BLUE_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
    }

    @Override
    public Collection<MRMofPolygon> getAllShadedPolygons() {
        List<MRMofPolygon> allPolygons = new ArrayList<>();

        List<MRStaticMof> staticMofs = this.model.getStaticMofs();
        for (int i = 0; i < staticMofs.size(); i++)
            allPolygons.addAll(staticMofs.get(i).getAllPolygons());

        return allPolygons;
    }

    /**
     * Sets the active mof model for the given controller.
     * @param activeMofModel the active mof model to apply
     * @param controller the controller
     */
    public void setActiveMofModel(MRAnimatedMofModel activeMofModel, MRModelMeshController controller) {
        this.activeMofModel = activeMofModel;

        MRStaticMof staticMof = getActiveStaticMof();
        if (staticMof != null)
            for (MRMofPart mofPart : staticMof.getParts())
                if (mofPart.isHiddenByConfiguration())
                    this.hiddenParts.add(mofPart);

        updateMofMesh(controller);
    }

    /**
     * Adds the MeshView to this model.
     * @param view the MeshView to add.
     * @param useHighlight If the highlighted material should be used.
     * @param preferUnlitSharp If the sharp unit material should be preferred.
     * @return if the meshView was successfully added
     */
    public boolean addView(MeshView view, MeshTracker meshTracker, boolean useHighlight, boolean preferUnlitSharp) {
        if (!addView(view, meshTracker))
            return false;

        PhongMaterial material;
        if (useHighlight) {
            material = getLitHighlightedMaterial();
        } else if (preferUnlitSharp) {
            material = getMaterial();
        } else {
            material = getLitMaterial();
        }

        view.setMaterial(material);
        return true;
    }

    @Override
    protected PhongMaterial updateMaterial(javafx.scene.image.Image newFxImage) {
        PhongMaterial result = super.updateMaterial(newFxImage);
        if (this.litMaterial != null)
            Scene3DUtils.updateLitBlurryMaterial(this.litMaterial, newFxImage);
        if (this.litHighlightedMaterial != null)
            this.litHighlightedMaterial = Scene3DUtils.updateHighlightMaterial(this.litHighlightedMaterial, newFxImage);

        return result;
    }

    /**
     * Gets the 3D PhongMaterial (diffuse components only, affected by lighting).
     * @return phongMaterial
     */
    public PhongMaterial getLitMaterial() {
        if (this.litMaterial == null && getMaterialFxImage() != null)
            this.litMaterial = Scene3DUtils.makeLitBlurryMaterial(getMaterialFxImage());

        return this.litMaterial;
    }

    /**
     * Gets the 3D PhongMaterial (diffuse components only, affected by lighting).
     * @return phongMaterial
     */
    public PhongMaterial getLitHighlightedMaterial() {
        if (this.litHighlightedMaterial == null && getMaterialFxImage() != null)
            this.litHighlightedMaterial = Scene3DUtils.updateHighlightMaterial(null, getMaterialFxImage());

        return this.litHighlightedMaterial;
    }

    /**
     * Updates the mof .
     */
    public void updateMofMesh(MRModelMeshController controller) {
        pushBatchOperations();
        clearMeshData();
        for (int i = 0; i < this.modelMeshPartNodes.size(); i++)
            removeNode(this.modelMeshPartNodes.get(i));

        this.modelMeshPartNodes.clear();

        // Setup nodes for parts(?)
        MRStaticMof staticMof = getActiveStaticMof();
        if (staticMof != null) {
            for (int i = 0; i < staticMof.getParts().size(); i++) {
                MRMofPart mofPart = staticMof.getParts().get(i);
                if (this.hiddenParts.contains(mofPart))
                    continue;

                MRModelMeshPartNode newNode = new MRModelMeshPartNode(this, mofPart);
                this.modelMeshPartNodes.add(newNode);
                addNode(newNode);
            }
        }

        popBatchOperations();

        // Update UI.
        if (controller != null) {
            controller.getCollprimManager().refreshList();
            controller.getHiliteManager().refreshList();
            controller.getTextureAnimationManager().refreshList();
            controller.getMainManager().getUiComponent().setupModel(getModel());
        }
    }

    /**
     * Gets the polygon data entry for the given polygon, if there is one
     * @param polygon the polygon to get the data entry for
     * @return dataEntry, or null
     */
    public DynamicMeshDataEntry getPolygonDataEntry(MRMofPolygon polygon) {
        if (polygon == null)
            return null;

        for (int i = 0; i < this.modelMeshPartNodes.size(); i++) {
            DynamicMeshDataEntry dataEntry = this.modelMeshPartNodes.get(i).getDataEntry(polygon);
            if (dataEntry != null)
                return dataEntry;
        }

        return null;
    }

    /**
     * Gets the active static mof, if there is one.
     * This method is capable of returning null if there is no active static mof.
     */
    public MRStaticMof getActiveStaticMof() {
        MRModelType modelType = this.model.getModelType();
        switch (modelType) {
            case DUMMY:
                return null;
            case ANIMATED:
                return this.activeMofModel != null ? this.activeMofModel.getStaticMof() : null;
            case STATIC:
                return this.model.getStaticMof();
            default:
                throw new UnsupportedOperationException("Unsupported MRModelType: " + modelType);
        }
    }

    public static class MRMofShadedTextureManager extends PSXMeshShadedTextureManager<MRMofPolygon> {
        public MRMofShadedTextureManager(MRModelMesh mesh) {
            super(mesh);
        }

        @Override
        public MRModelMesh getMesh() {
            return (MRModelMesh) super.getMesh();
        }

        @Override
        protected PSXShadeTextureDefinition createShadedTexture(MRMofPolygon polygon) {
            MRMofTextureAnimation animation = null;
            int animationTick = 0;

            // Resolve animation data.
            MRModelAnimationPlayer animationPlayer = getMesh().getAnimationPlayer();
            if (animationPlayer.isTextureAnimationEnabled()) {
                for (int i = 0; i < polygon.getMofPart().getTextureAnimationPolygonTargets().size(); i++) {
                    MRMofTextureAnimationPolygonTarget target = polygon.getMofPart().getTextureAnimationPolygonTargets().get(i);
                    if (target != null && target.getPolygon() == polygon) {
                        animation = target.getAnimation();
                        break;
                    }
                }

                if (animation != null)
                    animationTick = animationPlayer.getAnimationTick();
            }

            return polygon.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled(), animation, animationTick);
        }

        @Override
        protected void updateLooseShadingTexCoords() {
            // We don't have any loose shading entries.
        }
    }

    private static MRAnimatedMofModel getFirstModel(MRModel model) {
        if (model == null || !model.isAnimatedMof())
            return null;

        MRAnimatedMof animatedMof = model.getAnimatedMof();
        for (int i = 0; i < animatedMof.getModelSets().size(); i++) {
            MRAnimatedMofModelSet modelSet = animatedMof.getModelSets().get(i);
            if (!modelSet.getModels().isEmpty())
                return modelSet.getModels().get(0);
        }

        return null;
    }
}