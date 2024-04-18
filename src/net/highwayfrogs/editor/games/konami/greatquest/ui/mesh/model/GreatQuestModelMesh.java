package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.toc.kcCResourceModel;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager.GreatQuestEntityManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection;

/**
 * Represents a model mesh for Frogger The Great Quest.
 * The main mesh (this) does not actually contain any mesh data itself despite having the capacity for it, because in order to accurately render texture coordinates, we needed texture repeating, which meant no texture atlas.
 * Created by Kneesnap on 4/14/2024.
 */
@Getter
public class GreatQuestModelMesh extends DynamicMesh {
    private final kcModelWrapper modelWrapper;
    private final DynamicMeshCollection<GreatQuestModelMaterialMesh> actualMesh;
    private final boolean swapAxis;
    private final boolean environmentalMesh;

    public GreatQuestModelMesh(kcCResourceModel resourceModel, boolean swapAxis) {
        this(resourceModel != null ? resourceModel.getModelWrapper() : null, resourceModel != null ? resourceModel.getName() : "dummy", swapAxis);
    }

    public GreatQuestModelMesh(kcModelWrapper modelWrapper, boolean swapAxis) {
        this(modelWrapper, modelWrapper != null ? modelWrapper.getExportName() : "dummy", swapAxis);
    }

    public GreatQuestModelMesh(kcModelWrapper modelWrapper, String meshName, boolean swapAxis) {
        super(null, meshName);
        this.modelWrapper = modelWrapper;
        this.actualMesh = new DynamicMeshCollection<>(getMeshName());
        this.swapAxis = swapAxis;
        this.environmentalMesh = modelWrapper != null && GreatQuestEntityManager.isFileNameEnvironmentalMesh(modelWrapper.getFileName());

        // Setup actual mesh.
        kcModel model = modelWrapper != null ? modelWrapper.getModel() : null;
        if (model != null) {
            this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(this, model, null, meshName, swapAxis));
            for (kcMaterial material : model.getMaterials())
                this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(this, model, material, meshName, swapAxis));
        } else {
            // Setup placeholder.
            this.actualMesh.addMesh(new GreatQuestModelMaterialMesh(this, null, null, swapAxis));
        }
    }

    /**
     * Gets the model represented by this mesh.
     */
    public kcModel getModel() {
        return this.modelWrapper != null ? this.modelWrapper.getModel() : null;
    }
}