package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;

/**
 * This game heavily relies upon the textures repeating in texture coordinate space.
 * In other words, if it were using OpenGL, it would use GL_REPEAT: <a href="https://learnopengl.com/img/getting-started/texture_wrapping.png"/>
 * In order to recreate this functionality, it means we CANNOT use a texture atlas. Instead, we need a separate mesh for each material.
 * So, every single texture needs its own mesh.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestMapMaterialMesh extends DynamicMesh {
    private final GreatQuestChunkedFile map;
    private final kcMaterial mapMaterial;
    private final GreatQuestMapMaterialMeshNode mainNode;
    private final DynamicMeshOverlayNode highlightedPolygonNode;

    public GreatQuestMapMaterialMesh(GreatQuestChunkedFile mapFile, kcMaterial material) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY);
        this.map = mapFile;
        this.mapMaterial = material;

        GreatQuestImageFile imageFile = material != null ? material.getTexture() : null;
        updateMaterial(imageFile != null ? imageFile.getImage() : UnknownTextureSource.MAGENTA_INSTANCE.makeImage());
        // TODO: Apply specular, diffuse, etc stuff from material!

        // Setup main node.
        this.mainNode = new GreatQuestMapMaterialMeshNode(this);
        addNode(this.mainNode);

        this.highlightedPolygonNode = new DynamicMeshOverlayNode(this);
        addNode(this.highlightedPolygonNode);
    }
}