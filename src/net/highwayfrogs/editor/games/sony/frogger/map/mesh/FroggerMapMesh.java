package net.highwayfrogs.editor.games.sony.frogger.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimationTargetPolygon;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh.FroggerShadedTextureManager;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked.FroggerUIMapAnimationManager;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;

import java.awt.*;
import java.util.Collection;

/**
 * The triangle mesh representation of a FroggerMapFile.
 * Created by Kneesnap on 5/28/2024.
 */
@Getter
public class FroggerMapMesh extends PSXShadedDynamicMesh<FroggerMapPolygon, FroggerShadedTextureManager> {
    private final FroggerMapFile map;
    private final FroggerMapMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedMousePolygonNode;
    private final DynamicMeshOverlayNode highlightedAnimatedPolygonsNode;
    private final DynamicMeshOverlayNode highlightedInvisiblePolygonNode;
    private final DynamicMeshOverlayNode highlightedGridPolygonNode;

    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);
    public static final CursorVertexColor BLUE_COLOR = new CursorVertexColor(Color.BLUE, Color.BLACK);

    public FroggerMapMesh(FroggerMapFile mapFile) {
        super(new TreeTextureAtlas(64, 64, true), true);
        this.map = mapFile;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupShadedPolygons();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
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
    }

    @Override
    protected FroggerShadedTextureManager createShadedTextureManager() {
        return new FroggerShadedTextureManager(this);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(BakedLandscapeUIManager.MATERIAL_POLYGON_HIGHLIGHT);
        getTextureAtlas().addTexture(FroggerUIMapAnimationManager.MATERIAL_POLYGON_HIGHLIGHT);
        getTextureAtlas().addTexture(FroggerMapAnimation.UNKNOWN_TEXTURE_SOURCE);
        getTextureAtlas().addTexture(BLUE_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
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