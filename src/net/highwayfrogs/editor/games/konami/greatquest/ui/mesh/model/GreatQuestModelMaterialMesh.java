package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
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
    private final boolean swapAxis;
    private final kcMaterial gameMaterial;
    private final GreatQuestModelMaterialMeshNode mainNode;
    private PhongMaterial highlightedMaterial;

    public GreatQuestModelMaterialMesh(GreatQuestModelMesh fullMesh, kcModelWrapper modelWrapper, kcMaterial material, boolean swapAxis) {
        this(fullMesh, modelWrapper != null ? modelWrapper.getModel() : null, material, modelWrapper != null ? modelWrapper.getExportName() : null, swapAxis);
    }

    public GreatQuestModelMaterialMesh(GreatQuestModelMesh fullMesh, kcModel model, kcMaterial material, String modelName, boolean swapAxis) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY, modelName != null ? modelName + "-" + (material != null ? material.getMaterialName() : "unknown") : null);
        this.fullMesh = fullMesh;
        this.model = model;
        this.swapAxis = swapAxis;
        this.gameMaterial = material;

        // Add textures.
        GreatQuestImageFile imageFile = material != null ? material.getTexture() : null;
        updateMaterial(imageFile != null ? imageFile.getImage() : UnknownTextureSource.MAGENTA_INSTANCE.makeImage());
        // TODO: Apply specular, diffuse, etc stuff from material!

        // Setup main node.
        // Setup actual mesh.
        if (model != null) {
            this.mainNode = new GreatQuestModelMaterialMeshNode(this);
            addNode(this.mainNode);
        } else {
            // Setup placeholder.
            this.mainNode = null;
            updateMaterial(UnknownTextureSource.MAGENTA_INSTANCE.makeImage());
            addNode(new BoxMeshNode(this, .5F, .5F, .5F));
        }
    }

    @Override
    protected PhongMaterial updateMaterial(BufferedImage newImage) {
        newImage = GreatQuestUtils.fillEmptyAlpha(newImage);
        PhongMaterial parentMaterial = super.updateMaterial(newImage);
        this.highlightedMaterial = Scene3DUtils.updateHighlightMaterial(this.highlightedMaterial, newImage);
        return parentMaterial;
    }
}