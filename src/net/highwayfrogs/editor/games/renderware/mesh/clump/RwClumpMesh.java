package net.highwayfrogs.editor.games.renderware.mesh.clump;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.psx.shading.PSXShadeTextureDefinition;
import net.highwayfrogs.editor.games.psx.shading.PSXShadedTextureManager.PSXMeshShadedTextureManager;
import net.highwayfrogs.editor.games.renderware.chunks.RwClumpChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk;
import net.highwayfrogs.editor.games.renderware.mesh.clump.RwClumpMesh.RwShadedTextureManager;
import net.highwayfrogs.editor.games.renderware.struct.types.RpTriangle;
import net.highwayfrogs.editor.gui.mesh.PSXShadedDynamicMesh;
import net.highwayfrogs.editor.gui.texture.atlas.TreeTextureAtlas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents the mesh of an RwClumpChunk, without repeating texture UVs.
 * Created by Kneesnap on 8/26/2024.
 */
@Getter
public class RwClumpMesh extends PSXShadedDynamicMesh<RpTriangle, RwShadedTextureManager> {
    private final RwClumpChunk clump;
    private final RwClumpMeshNode mainNode;

    public RwClumpMesh(RwClumpChunk clumpChunk) {
        super(new TreeTextureAtlas(64, 64, true), DynamicMeshTextureQuality.LIT_BLURRY, false, clumpChunk.getCollectionViewDisplayName());
        this.clump = clumpChunk;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupShadedPolygons();

        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new RwClumpMeshNode(this);
        addNode(this.mainNode);
    }

    @Override
    protected RwShadedTextureManager createShadedTextureManager() {
        return new RwShadedTextureManager(this);
    }

    @Override
    public Collection<RpTriangle> getAllShadedPolygons() {
        List<RpTriangle> allTriangles = new ArrayList<>();
        for (RwGeometryChunk geometry : getClump().getGeometryList().getGeometries())
            allTriangles.addAll(geometry.getTriangles());
        return allTriangles;
    }

    public static class RwShadedTextureManager extends PSXMeshShadedTextureManager<RpTriangle> {
        public RwShadedTextureManager(RwClumpMesh mesh) {
            super(mesh);
        }

        @Override
        public RwClumpMesh getMesh() {
            return (RwClumpMesh) super.getMesh();
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