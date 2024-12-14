package net.highwayfrogs.editor.games.renderware.mesh.world;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.renderware.chunks.RwWorldChunk;
import net.highwayfrogs.editor.games.renderware.chunks.sector.RwAtomicSectorChunk;
import net.highwayfrogs.editor.games.renderware.mesh.world.RwWorldMesh.RwShadedTextureManager;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents the mesh of a RenderWare world.
 * Created by Kneesnap on 8/18/2024.
 */
@Getter
public class RwWorldMesh extends PSXShadedDynamicMesh<RpTriangle, RwShadedTextureManager> {
    private final RwWorldChunk world;
    private final RwWorldMeshNode mainNode;

    public RwWorldMesh(RwWorldChunk worldChunk) {
        super(new TreeTextureAtlas(64, 64, true), DynamicMeshTextureQuality.LIT_BLURRY, false);
        this.world = worldChunk;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupShadedPolygons();

        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new RwWorldMeshNode(this);
        addNode(this.mainNode);
    }

    @Override
    protected RwShadedTextureManager createShadedTextureManager() {
        return new RwShadedTextureManager(this);
    }

    @Override
    public Collection<RpTriangle> getAllShadedPolygons() {
        List<RpTriangle> allTriangles = new ArrayList<>();
        for (RwAtomicSectorChunk worldSector : getWorld().getWorldSectors())
            allTriangles.addAll(worldSector.getTriangles());
        return allTriangles;
    }

    public static class RwShadedTextureManager extends PSXMeshShadedTextureManager<RpTriangle> {
        public RwShadedTextureManager(RwWorldMesh mesh) {
            super(mesh);
        }

        @Override
        public RwWorldMesh getMesh() {
            return (RwWorldMesh) super.getMesh();
        }

        @Override
        protected PSXShadeTextureDefinition createShadedTexture(RpTriangle triangle) {
            return triangle.createPolygonShadeDefinition(getMesh(), getMesh().isShadingEnabled());
        }

        @Override
        protected void updateLooseShadingTexCoords() {
            // We don't have any loose shading entries.
        }
    }
}