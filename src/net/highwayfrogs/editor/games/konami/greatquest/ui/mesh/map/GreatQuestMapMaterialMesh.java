package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.scene.paint.Color;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResOctTreeSceneMgr.kcVtxBufFileStruct;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;
import net.highwayfrogs.editor.utils.ColorUtils;

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

    public GreatQuestMapMaterialMesh(GreatQuestChunkedFile mapFile, kcMaterial material) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY);
        this.map = mapFile;
        this.mapMaterial = material;

        GreatQuestImageFile imageFile = material != null ? material.getTexture() : null;
        updateMaterial(imageFile != null ? imageFile.getImage() : UnknownTextureSource.MAGENTA_INSTANCE.makeImage());

        // Setup main node.
        this.mainNode = new GreatQuestMapMaterialMeshNode(this);
        addNode(this.mainNode);
    }

    public GreatQuestMapMaterialMesh(GreatQuestChunkedFile mapFile, kcVtxBufFileStruct highlightedVertexBuffer) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY);
        this.map = mapFile;
        this.mapMaterial = null;

        updateMaterial(ColorUtils.makeColorImage(Color.rgb(200, 200, 0, .5F))); // Highlight.

        // Setup main node.
        this.mainNode = new GreatQuestMapMaterialMeshNode(this, highlightedVertexBuffer);
        addNode(this.mainNode);
    }
}