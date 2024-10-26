package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

import java.awt.image.BufferedImage;

/**
 * Allows viewing a kcCResourceSkeleton as part of a mesh.
 * Created by Kneesnap on 10/9/2024.
 */
@Getter
public class GreatQuestModelSkeletonMesh extends DynamicMesh {
    private final GreatQuestModelMesh fullMesh;
    private final GreatQuestModelSkeletonMeshNode mainNode;
    private PhongMaterial highlightedMaterial;

    public GreatQuestModelSkeletonMesh(GreatQuestModelMesh fullMesh, String modelName) {
        super(null, DynamicMeshTextureQuality.LIT_BLURRY, modelName);
        this.fullMesh = fullMesh;
        updateMaterial(ColorUtils.makeColorImage(Color.TEAL));

        this.mainNode = new GreatQuestModelSkeletonMeshNode(this);
        addNode(this.mainNode);
    }

    @Override
    protected PhongMaterial updateMaterial(BufferedImage newImage) {
        newImage = GreatQuestUtils.fillEmptyAlpha(newImage);
        PhongMaterial parentMaterial = super.updateMaterial(newImage);
        this.highlightedMaterial = Scene3DUtils.updateHighlightMaterial(this.highlightedMaterial, newImage);
        return parentMaterial;
    }
}
