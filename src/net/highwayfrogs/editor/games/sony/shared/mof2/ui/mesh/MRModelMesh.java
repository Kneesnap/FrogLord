package net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPolygon;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh.MRMofShadedTextureManager;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A JavaFX representation of the MRModel mesh seen within pre-1999 Sony Cambridge games.
 * Created by Kneesnap on 2/18/2025.
 */
@Getter
public class MRModelMesh extends PSXShadedDynamicMesh<MRMofPolygon, MRMofShadedTextureManager> {
    private final MRModel model;
    private final MRModelMeshNode mainNode; // TODO: Node per part?
    private final DynamicMeshOverlayNode highlightedMousePolygonNode;
    private final DynamicMeshOverlayNode highlightedAnimatedPolygonsNode;
    private final DynamicMeshOverlayNode highlightedInvisiblePolygonNode;
    private final DynamicMeshOverlayNode highlightedGridPolygonNode;

    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);
    public static final CursorVertexColor BLUE_COLOR = new CursorVertexColor(Color.BLUE, Color.BLACK);

    public MRModelMesh(MRModel model) {
        super(new TreeTextureAtlas(16, 16, true), DynamicMeshTextureQuality.UNLIT_SHARP, true);
        this.model = model;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupShadedPolygons();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new MRModelMeshNode();
        //TODO: ??? addNode(this.mainNode);

        this.highlightedAnimatedPolygonsNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedAnimatedPolygonsNode);

        this.highlightedInvisiblePolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedInvisiblePolygonNode);

        this.highlightedGridPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedGridPolygonNode);

        this.highlightedMousePolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedMousePolygonNode);
    }

    @Override
    protected MRMofShadedTextureManager createShadedTextureManager() {
        return new MRMofShadedTextureManager(this);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(BakedLandscapeUIManager.MATERIAL_POLYGON_HIGHLIGHT);
        //getTextureAtlas().addTexture(FroggerUIMapAnimationManager.MATERIAL_POLYGON_HIGHLIGHT);
        //getTextureAtlas().addTexture(FroggerMapAnimation.UNKNOWN_TEXTURE_SOURCE);
        getTextureAtlas().addTexture(BLUE_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
    }

    @Override
    public Collection<MRMofPolygon> getAllShadedPolygons() {
        List<MRMofPolygon> allPolygons = new ArrayList<>();
        // TODO: return this.model.getPolygonPacket().getPolygons();
        return allPolygons;
    }

    // TODO: getAnimation methods(?) maybe they belong in MRModel instead actually.

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
            int frame = 0; // TODO: getMesh().getMainNode() != null ? getMesh().getMainNode().getAnimationTickCounter() : -1; // TODO: !
            return polygon.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled(), null, frame);
        }

        @Override
        protected void updateLooseShadingTexCoords() {
            // We don't have any loose shading entries.
        }
    }
}