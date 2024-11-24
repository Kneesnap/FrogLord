package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection;

import java.util.List;

/**
 * Represents a map mesh for Frogger The Great Quest.
 * The main mesh (this) does not actually contain any mesh data itself despite having the capacity for it, because in order to accurately render texture coordinates, we needed texture repeating, which meant no texture atlas.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestMapMesh extends DynamicMesh {
    private final GreatQuestChunkedFile map;
    private final DynamicMeshCollection<GreatQuestMapMaterialMesh> actualMesh;

    public GreatQuestMapMesh(GreatQuestChunkedFile mapFile) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY, mapFile.getExportName());
        this.map = mapFile;
        this.actualMesh = new DynamicMeshCollection<>(getMeshName());

        // Build the actual mesh.
        kcCResOctTreeSceneMgr sceneManager = this.map.getSceneManager();
        addMeshForMaterial(sceneManager, null); // Vertex buffers using an unknown material.
        for (kcMaterial material : sceneManager.getMaterials())
            addMeshForMaterial(sceneManager, material);
    }

    private void addMeshForMaterial(kcCResOctTreeSceneMgr sceneManager, kcMaterial material) {
        List<?> vertexBuffers = sceneManager.getVertexBuffersForMaterial(material);
        if (vertexBuffers != null && vertexBuffers.size() > 0)
            this.actualMesh.addMesh(new GreatQuestMapMaterialMesh(this.map, material));
    }
}