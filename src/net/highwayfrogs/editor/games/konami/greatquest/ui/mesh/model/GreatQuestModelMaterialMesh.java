package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;

/**
 * A mesh for a 3D model file in the Great Quest.
 * Created by Kneesnap on 4/15/2024.
 */
@Getter
public class GreatQuestModelMaterialMesh extends DynamicMesh {
    private final kcModel model;
    private final kcMaterial gameMaterial;
    private final GreatQuestModelMaterialMeshNode mainNode;

    public GreatQuestModelMaterialMesh(kcModelWrapper modelWrapper, kcMaterial material) {
        this(modelWrapper.getModel(), material, modelWrapper.getExportName());
    }

    public GreatQuestModelMaterialMesh(kcModel model, kcMaterial material, String modelName) {
        super(null, modelName != null ? modelName + "-" + (material != null ? material.getMaterialName() : "unknown") : null);
        this.model = model;
        this.gameMaterial = material;

        // Add textures.
        GreatQuestImageFile imageFile = material != null ? material.getTexture() : null;
        updateMaterial(imageFile != null ? imageFile.getImage() : UnknownTextureSource.MAGENTA_INSTANCE.makeImage());
        // TODO: Apply specular, diffuse, etc stuff from material!

        // Setup main node.
        this.mainNode = new GreatQuestModelMaterialMeshNode(this);
        addNode(this.mainNode);
    }
}