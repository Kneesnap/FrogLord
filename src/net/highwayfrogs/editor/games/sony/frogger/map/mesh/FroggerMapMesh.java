package net.highwayfrogs.editor.games.sony.frogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerSkyLand.SkyLandTile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh.FroggerShadedTextureManager;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIGeometryManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIGridManager;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIMapAnimationManager;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;

import java.util.Collection;
import java.util.Map;

/**
 * The triangle mesh representation of a FroggerMapFile.
 * Created by Kneesnap on 5/28/2024.
 */
@Getter
public class FroggerMapMesh extends PSXShadedDynamicMesh<FroggerMapPolygon, FroggerShadedTextureManager> {
    private final FroggerMapFile map;
    private final FroggerMapMeshNode mainNode;
    private final FroggerSkyLandMeshNode skyLandMeshNode;
    private final DynamicMeshOverlayNode highlightedMousePolygonNode;
    private final DynamicMeshOverlayNode highlightedAnimatedPolygonsNode;
    private final DynamicMeshOverlayNode highlightedInvisiblePolygonNode;
    private final DynamicMeshOverlayNode highlightedGridPolygonNode;
    private final DynamicMeshOverlayNode highlightedGroupPolygonNode;

    public FroggerMapMesh(FroggerMapFile mapFile) {
        super(new TreeTextureAtlas(64, 64, true), DynamicMeshTextureQuality.UNLIT_SHARP, true, mapFile.getFileDisplayName());
        this.map = mapFile;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupShadedPolygons();
        Map<SkyLandTile, FroggerMapPolygon> skyLandPolygons = FroggerSkyLandMeshNode.addShadedPolygons(this);
        getTextureAtlas().endBulkOperations();

        if (skyLandPolygons != null && skyLandPolygons.size() > 0) {
            this.skyLandMeshNode = new FroggerSkyLandMeshNode(this, skyLandPolygons);
            addNode(this.skyLandMeshNode);
        } else {
            this.skyLandMeshNode = null;
        }

        // Setup main node. (After sky land mesh, so sky land shows behind transparent things.
        this.mainNode = new FroggerMapMeshNode(this);
        addNode(this.mainNode);

        this.highlightedAnimatedPolygonsNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedAnimatedPolygonsNode);

        this.highlightedInvisiblePolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedInvisiblePolygonNode);

        this.highlightedGridPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedGridPolygonNode);

        this.highlightedMousePolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedMousePolygonNode);

        this.highlightedGroupPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedGroupPolygonNode);
    }

    @Override
    protected FroggerShadedTextureManager createShadedTextureManager() {
        return new FroggerShadedTextureManager(this);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(BakedLandscapeUIManager.MATERIAL_POLYGON_HIGHLIGHT);
        getTextureAtlas().addTexture(FroggerUIMapAnimationManager.MATERIAL_POLYGON_HIGHLIGHT);
        getTextureAtlas().addTexture(FroggerMapAnimation.UNKNOWN_TEXTURE_SOURCE);
        getTextureAtlas().addTexture(FroggerUIGeometryManager.GREEN_COLOR);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_DARK_RED);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_RED);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_PINK);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_PURPLE);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_HOT_PINK);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_GREEN);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_LIME_GREEN);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_AQUA);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_DARK_BLUE);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_YELLOW);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_GOLD);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GRID_ORANGE);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_GREY);
        getTextureAtlas().addTexture(FroggerUIGridManager.MATERIAL_HIGHLIGHT_LIGHT_GREY);
    }

    @Override
    public Collection<FroggerMapPolygon> getAllShadedPolygons() {
        return getMap().getPolygonPacket().getPolygons();
    }

    /**
     * Gets the animation active for the given polygon.
     * @param polygon the polygon to find the animation for
     * @return polygon animation, or null if no animation exists
     */
    public FroggerMapAnimation getAnimation(FroggerMapPolygon polygon) {
        FroggerMapFilePacketAnimation animationPacket = getMap().getAnimationPacket();
        if (animationPacket == null)
            return null; // Animations aren't available.

        FroggerMapAnimationTargetPolygon targetPolygon = animationPacket.getAnimationTarget(polygon);
        return targetPolygon != null ? targetPolygon.getAnimation() : null;
    }

    public static class FroggerShadedTextureManager extends PSXMeshShadedTextureManager<FroggerMapPolygon> {
        public FroggerShadedTextureManager(FroggerMapMesh mesh) {
            super(mesh);
        }

        @Override
        public FroggerMapMesh getMesh() {
            return (FroggerMapMesh) super.getMesh();
        }

        @Override
        protected PSXShadeTextureDefinition createShadedTexture(FroggerMapPolygon polygon) {
            int frame = getMesh().getMainNode() != null ? getMesh().getMainNode().getAnimationTickCounter() : -1;
            return polygon.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled(), getMesh().getAnimation(polygon), frame);
        }

        @Override
        protected void updateLooseShadingTexCoords() {
            // We don't have any loose shading entries.
        }
    }
}