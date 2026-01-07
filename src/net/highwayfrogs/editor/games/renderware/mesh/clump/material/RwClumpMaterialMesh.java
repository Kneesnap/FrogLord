package net.highwayfrogs.editor.games.renderware.mesh.clump.material;

import lombok.Getter;
import net.highwayfrogs.editor.games.renderware.chunks.RwClumpChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwImageChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwMaterialChunk;
import net.highwayfrogs.editor.games.renderware.chunks.RwTextureChunk;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.texture.basic.UnknownTextureSource;

/**
 * RenderWare relies upon the textures repeating in texture coordinate space.
 * In other words, if it were using OpenGL, it would use GL_REPEAT: <a href="https://learnopengl.com/img/getting-started/texture_wrapping.png"/>
 * In order to recreate this functionality, it means we CANNOT use a texture atlas. Instead, we need a separate mesh for each material.
 * So, every single texture needs its own mesh.
 * Created by Kneesnap on 8/27/2024.
 */
@Getter
public class RwClumpMaterialMesh extends DynamicMesh {
    private final RwClumpChunk clump;
    private final RwMaterialChunk rwMaterial;
    private final int rwMaterialIndex;
    private final RwClumpMaterialMeshNode mainNode;

    public RwClumpMaterialMesh(RwClumpChunk clump, RwMaterialChunk material, int materialIndex) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY);
        this.clump = clump;
        this.rwMaterial = material;
        this.rwMaterialIndex = materialIndex;

        RwTextureChunk texture = material != null ? material.getTexture() : null;
        RwImageChunk image = texture != null ? texture.getImage() : null;
        updateMaterial(image != null ? image.getImage() : UnknownTextureSource.MAGENTA_INSTANCE.makeImage());
        // TODO: Apply specular, diffuse, etc stuff from material!

        // Setup main node.
        this.mainNode = new RwClumpMaterialMeshNode(this);
        addNode(this.mainNode);
    }
}