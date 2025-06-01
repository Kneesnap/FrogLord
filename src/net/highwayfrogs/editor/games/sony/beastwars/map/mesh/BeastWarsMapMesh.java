package net.highwayfrogs.editor.games.sony.beastwars.map.mesh;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsTexFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh.BeastWarsShadedTextureManager;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a mesh for a beast wars map.
 * Created by Kneesnap on 9/22/2023.
 */
@Getter
public class BeastWarsMapMesh extends PSXShadedDynamicMesh<BeastWarsMapVertex, BeastWarsShadedTextureManager> {
    private final BeastWarsMapFile map;
    private final BeastWarsMapMeshNode mainNode;

    public static final CursorVertexColor CURSOR_COLOR = new CursorVertexColor(Color.ORANGE, Color.BLACK);
    public static final CursorVertexColor REMOVE_FACE_COLOR = new CursorVertexColor(Color.RED, Color.BLACK);
    public static final CursorVertexColor GRAY_COLOR = new CursorVertexColor(Color.GRAY, Color.BLACK);
    public static final CursorVertexColor YELLOW_COLOR = new CursorVertexColor(Color.YELLOW, Color.BLACK);
    public static final CursorVertexColor GREEN_COLOR = new CursorVertexColor(Color.GREEN, Color.BLACK);
    public static final CursorVertexColor PINK_COLOR = new CursorVertexColor(Color.PINK, Color.BLACK);

    public BeastWarsMapMesh(BeastWarsMapFile mapFile) {
        super(new SequentialTextureAtlas(64, 64, true), DynamicMeshTextureQuality.UNLIT_SHARP, mapFile.shouldMapViewerEnableShadingByDefault(), mapFile.getFileDisplayName());
        this.map = mapFile;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupBasicTextures();
        setupMapTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new BeastWarsMapMeshNode(this);
        addNode(this.mainNode);
    }

    private void setupBasicTextures() {
        getTextureAtlas().addTexture(CURSOR_COLOR);
        getTextureAtlas().addTexture(REMOVE_FACE_COLOR);
        getTextureAtlas().addTexture(GRAY_COLOR);
        getTextureAtlas().addTexture(YELLOW_COLOR);
        getTextureAtlas().addTexture(GREEN_COLOR);
        getTextureAtlas().addTexture(PINK_COLOR);
        getTextureAtlas().addTexture(UnknownTextureSource.CYAN_INSTANCE);
    }

    private void setupMapTextures() {
        // Register map textures into the atlas.
        BeastWarsMapFile map = getMap();
        BeastWarsTexFile texFile = map.getTextureFile();
        if (texFile != null)
            for (int i = 0; i < texFile.getImages().size(); i++)
                getTextureAtlas().addTexture(texFile.getImages().get(i));

        setupShadedPolygons();
    }

    @Override
    public Collection<BeastWarsMapVertex> getAllShadedPolygons() {
        List<BeastWarsMapVertex> vertices = new ArrayList<>();
        for (int z = 0; z < this.map.getHeightMapZLength(); z++) {
            for (int x = 0; x < this.map.getHeightMapXLength(); x++) {
                BeastWarsMapVertex vertex = this.map.getVertex(x, z);
                if (vertex != null && vertex.hasTile())
                    vertices.add(vertex);
            }
        }

        return vertices;
    }

    @Override
    protected BeastWarsShadedTextureManager createShadedTextureManager() {
        return new BeastWarsShadedTextureManager(this);
    }

    /**
     * Manages the shaded texture recreations for the polygons in the map mesh.
     */
    public static class BeastWarsShadedTextureManager extends PSXMeshShadedTextureManager<BeastWarsMapVertex> {
        public BeastWarsShadedTextureManager(BeastWarsMapMesh mesh) {
            super(mesh);
        }

        @Override
        public BeastWarsMapMesh getMesh() {
            return (BeastWarsMapMesh) super.getMesh();
        }

        @Override
        protected PSXShadeTextureDefinition createShadedTexture(BeastWarsMapVertex vertex) {
            return vertex.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled());
        }

        @Override
        protected void updateLooseShadingTexCoords() {
            // Don't have any loose shade definitions.
        }
    }
}