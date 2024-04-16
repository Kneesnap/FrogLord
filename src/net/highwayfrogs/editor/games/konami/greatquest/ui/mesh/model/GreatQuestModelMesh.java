package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceModel;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection;
import net.highwayfrogs.editor.gui.mesh.fxobject.BoxMeshNode;

/**
 * Represents a model mesh for Frogger The Great Quest.
 * The main mesh (this) does not actually contain any mesh data itself despite having the capacity for it, because in order to accurately render texture coordinates, we needed texture repeating, which meant no texture atlas.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestModelMesh extends DynamicMesh {
    private final kcModelWrapper modelWrapper;
    private final DynamicMeshCollection<GreatQuestModelMaterialMesh> actualMesh;

    public GreatQuestModelMesh(kcCResourceModel resourceModel) {
        this(resourceModel != null ? resourceModel.getModelWrapper() : null, resourceModel != null ? resourceModel.getName() : "dummy");
    }

    public GreatQuestModelMesh(kcModelWrapper modelWrapper) {
        this(modelWrapper, modelWrapper != null ? modelWrapper.getExportName() : "dummy");
    }

    public GreatQuestModelMesh(kcModelWrapper modelWrapper, String meshName) {
        super(null, meshName);
        this.modelWrapper = modelWrapper;
        this.actualMesh = new DynamicMeshCollection<>(getMeshName());

        // Setup actual mesh.
        kcModel model = modelWrapper != null ? modelWrapper.getModel() : null;
        if (model != null) {
            this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(modelWrapper, null));
            for (kcMaterial material : model.getMaterials())
                this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(model, material, meshName));
        } else {
            // Setup placeholder.
            updateMaterial(UnknownTextureSource.MAGENTA_INSTANCE.makeImage());
            addNode(new BoxMeshNode(this, .5F, .5F, .5F));
        }
    }

    /**
     * Gets the model represented by this mesh.
     */
    public kcModel getModel() {
        return this.modelWrapper != null ? this.modelWrapper.getModel() : null;
    }
}