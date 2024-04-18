package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.UnknownTextureSource;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestImageFile;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcMaterial;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModel;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.fxobject.BoxMeshNode;
import net.highwayfrogs.editor.utils.Utils;

import java.awt.*;
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
        super(null, modelName != null ? modelName + "-" + (material != null ? material.getMaterialName() : "unknown") : null);
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
        PhongMaterial parentMaterial = super.updateMaterial(newImage);

        // Setup graphics.
        BufferedImage highlightedImage = new BufferedImage(newImage.getWidth(), newImage.getHeight(), newImage.getType());
        Graphics2D g = highlightedImage.createGraphics();
        try {
            // Clean image.
            g.setBackground(new Color(255, 255, 255, 0));
            g.clearRect(0, 0, highlightedImage.getWidth(), highlightedImage.getHeight());

            // Draw new image.
            g.drawImage(newImage, 0, 0, newImage.getWidth(), newImage.getHeight(), null);
            g.setColor(new Color(200, 200, 0, 127));
            g.fillRect(0, 0, highlightedImage.getWidth(), highlightedImage.getHeight());
        } finally {
            g.dispose();
        }

        if (this.highlightedMaterial == null) {
            this.highlightedMaterial = Utils.makeDiffuseMaterial(Utils.toFXImage(highlightedImage, false));
            return this.highlightedMaterial;
        }

        // Update material image.
        this.highlightedMaterial.setDiffuseMap(Utils.toFXImage(highlightedImage, false));
        return parentMaterial;
    }
}