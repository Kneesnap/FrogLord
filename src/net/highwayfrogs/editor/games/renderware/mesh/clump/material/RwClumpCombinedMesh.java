package net.highwayfrogs.editor.games.renderware.mesh.clump.material;

import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.chunks.RwClumpChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwGeometryChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwMaterialChunk;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a clump mesh for RenderWare with the REPEAT texture map.
 * The main mesh (this) does not actually contain any mesh data itself despite having the capacity for it, because in order to accurately render texture coordinates, we needed texture repeating, which meant no texture atlas.
 * Created by Kneesnap on 8/27/2024.
 */
@Getter
public class RwClumpCombinedMesh extends DynamicMesh {
    private final RwClumpChunk clump;
    private final DynamicMeshCollection<RwClumpMaterialMesh> actualMesh;

    public RwClumpCombinedMesh(RwClumpChunk clump) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY, clump.getStreamFile() != null ? clump.getStreamFile().getLocationName() : null);
        this.clump = clump;
        this.actualMesh = new DynamicMeshCollection<>(getMeshName());

        // Build the actual mesh.
        Set<RwMaterialChunk> seenMaterials = new HashSet<>();
        this.actualMesh.addMesh(new RwClumpMaterialMesh(clump, null, -1)); // Unknown texture.
        for (RwGeometryChunk geometry : clump.getGeometryList().getGeometries()) {
            for (int i = 0; i < geometry.getMaterialList().getMaterials().size(); i++) {
                RwMaterialChunk material = geometry.getMaterialList().getMaterials().get(i);
                if (seenMaterials.add(material))
                    this.actualMesh.addMesh(new RwClumpMaterialMesh(clump, material, i));
            }
        }
    }
}