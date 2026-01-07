package net.highwayfrogs.editor.games.sony.shared.landscaper.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.shared.landscaper.Landscape;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapePolygon;
import net.highwayfrogs.editor.games.sony.shared.landscaper.LandscapeTexture;
import net.highwayfrogs.editor.games.sony.shared.landscaper.mesh.LandscapeMapMesh.LandscapeShadedTextureManager;
import net.highwayfrogs.editor.gui.editor.BakedLandscapeUIManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.ITextureSource;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;

import java.util.Collection;

/**
 * The triangle mesh representation of a Landscape.
 * Created by Kneesnap on 7/16/2024.
 */
@Getter
public class LandscapeMapMesh extends PSXShadedDynamicMesh<LandscapePolygon, LandscapeShadedTextureManager> {
    private final Landscape landscape;
    private final LandscapeMapMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedMousePolygonNode;

    public LandscapeMapMesh(Landscape landscape) {
        super(new TreeTextureAtlas(64, 64, true), DynamicMeshTextureQuality.UNLIT_SHARP, true, null);
        this.landscape = landscape;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupShadedPolygons();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new LandscapeMapMeshNode(this);
        addNode(this.mainNode);

        this.highlightedMousePolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedMousePolygonNode);
    }

    @Override
    protected LandscapeShadedTextureManager createShadedTextureManager() {
        return new LandscapeShadedTextureManager(this);
    }

    /**
     * Setup basic textures in the texture sheet.
     */
    protected void setupBasicTextures() {
        getTextureAtlas().addTexture(BakedLandscapeUIManager.MATERIAL_POLYGON_HIGHLIGHT);
    }

    /**
     * Setup textures found on the landscape.
     * Not used by default.
     */
    protected void setupLandscapeTextures() {
        for (int i = 0; i < this.landscape.getTextures().size(); i++) {
            LandscapeTexture texture = this.landscape.getTextures().get(i);
            ITextureSource textureSource = texture.getTextureSource();
            if (textureSource != null)
                getTextureAtlas().addTexture(textureSource);
        }
    }

    @Override
    public Collection<LandscapePolygon> getAllShadedPolygons() {
        return this.landscape.getPolygons();
    }

    public static class LandscapeShadedTextureManager extends PSXMeshShadedTextureManager<LandscapePolygon> {
        public LandscapeShadedTextureManager(LandscapeMapMesh mesh) {
            super(mesh);
        }

        @Override
        public LandscapeMapMesh getMesh() {
            return (LandscapeMapMesh) super.getMesh();
        }

        @Override
        protected PSXShadeTextureDefinition createShadedTexture(LandscapePolygon polygon) {
            return polygon.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled());
        }

        @Override
        protected void updateLooseShadingTexCoords() {
            // We don't have any loose shading entries.
        }
    }
}