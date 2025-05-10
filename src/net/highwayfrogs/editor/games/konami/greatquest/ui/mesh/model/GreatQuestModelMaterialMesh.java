package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.fxobject.BoxMeshNode;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.awt.image.BufferedImage;

/**
 * A mesh for a 3D model file in the Great Quest.
 * Created by Kneesnap on 4/15/2024.
 */
@Getter
public class GreatQuestModelMaterialMesh extends DynamicMesh {
    private final GreatQuestModelMesh fullMesh;
    private final kcModel model;
    private final kcMaterial gameMaterial;
    private final GreatQuestModelMaterialMeshNode mainNode;
    private PhongMaterial highlightedMaterial;

    public GreatQuestModelMaterialMesh(GreatQuestModelMesh fullMesh, kcModelWrapper modelWrapper, kcMaterial material) {
        this(fullMesh, modelWrapper != null ? modelWrapper.getModel() : null, material, modelWrapper != null ? modelWrapper.getExportName() : null);
    }

    public GreatQuestModelMaterialMesh(GreatQuestModelMesh fullMesh, kcModel model, kcMaterial material, String modelName) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY, modelName != null ? modelName + "-" + (material != null ? material.getMaterialName() : "unknown") : null);
        this.fullMesh = fullMesh;
        this.model = model;
        this.gameMaterial = material;

        // Add textures.
        GreatQuestImageFile imageFile = model != null && material != null ? material.getTexture() : null;
        updateMaterial(imageFile != null ? imageFile.getImage() : UnknownTextureSource.MAGENTA_INSTANCE.makeImage());

        // Setup main node.
        // Setup actual mesh.
        if (model != null) {
            this.mainNode = new GreatQuestModelMaterialMeshNode(this);
            addNode(this.mainNode);
        } else {
            // Setup placeholder.
            this.mainNode = null;
            addNode(new BoxMeshNode(this, .5F, .5F, .5F));
        }
    }

    @Override
    protected PhongMaterial updateMaterial(BufferedImage newImage) {
        if (this.gameMaterial != null && this.gameMaterial.hasAlphaBlend())
            newImage = GreatQuestUtils.fillEmptyAlpha(newImage);
        PhongMaterial parentMaterial = super.updateMaterial(newImage);
        if (this.highlightedMaterial != null)
            this.highlightedMaterial = Scene3DUtils.updateHighlightMaterial(this.highlightedMaterial, newImage);
        return parentMaterial;
    }

    /**
     * Gets or creates the highlighted material.
     */
    public PhongMaterial getHighlightedMaterial() {
        if (this.highlightedMaterial == null)
            this.highlightedMaterial = Scene3DUtils.createHighlightMaterial(getMaterial());

        return this.highlightedMaterial;
    }

    /**
     * Returns whether the skeleton axis rotation is applied.
     * This is a guess, more to ensure the model viewer shows a decent rotation.
     */
    public boolean isSkeletonAxisRotationApplied() {
        return this.fullMesh != null && this.fullMesh.isSkeletonAxisRotationApplied();
    }
}