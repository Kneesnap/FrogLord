package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;

/**
 * Represents a map mesh for Frogger The Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestMapMesh extends DynamicMesh {
    private final GreatQuestChunkedFile map;
    private final GreatQuestMapMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedPolygonNode;

    public GreatQuestMapMesh(GreatQuestChunkedFile mapFile) {
        super(new SequentialTextureAtlas(64, 64, true));
        this.map = mapFile;

        // Add textures.
        getTextureAtlas().startBulkOperations();
        getTextureAtlas().setFallbackTexture(UnknownTextureSource.MAGENTA_INSTANCE);
        setupMapTextures();
        getTextureAtlas().endBulkOperations();

        // Setup main node.
        this.mainNode = new GreatQuestMapMeshNode(this);
        addNode(this.mainNode);

        this.highlightedPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedPolygonNode);
    }

    private void setupMapTextures() {
        // Find the level table entry for the map this mesh represents.
        for (kcMaterial material : getMap().getSceneManager().getMaterials()) {
            GreatQuestImageFile imageFile = material.getTexture();
            if (imageFile != null)
                getTextureAtlas().addTexture(imageFile);
        }
    }
}