package net.highwayfrogs.editor.games.renderware.mesh.world;

import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.chunks.RwMaterialChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwWorldChunk;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection;

/**
 * Represents a map mesh for RenderWare with the REPEAT texture map.
 * The main mesh (this) does not actually contain any mesh data itself despite having the capacity for it, because in order to accurately render texture coordinates, we needed texture repeating, which meant no texture atlas.
 * Created by Kneesnap on 8/23/2024.
 */
@Getter
public class RwWorldCombinedMesh extends DynamicMesh {
    private final RwWorldChunk world;
    private final DynamicMeshCollection<RwWorldMaterialMesh> actualMesh;

    public RwWorldCombinedMesh(RwWorldChunk world) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY, world.getStreamFile() != null ? world.getStreamFile().getLocationName() : null);
        this.world = world;
        this.actualMesh = new DynamicMeshCollection<>(getMeshName());

        // Build the actual mesh.
        this.actualMesh.addMesh(new RwWorldMaterialMesh(world, null, -1)); // Unknown texture.
        for (int i = 0; i < world.getMaterialList().getMaterials().size(); i++) {
            RwMaterialChunk material = world.getMaterialList().getMaterials().get(i);
            this.actualMesh.addMesh(new RwWorldMaterialMesh(world, material, i));
        }
    }
}