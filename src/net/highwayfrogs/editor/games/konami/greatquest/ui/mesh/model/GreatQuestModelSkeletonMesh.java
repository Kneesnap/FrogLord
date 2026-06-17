package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestUtils;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshAdapterNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshDataEntry;
import net.highwayfrogs.editor.gui.mesh.MeshTracker;
import net.highwayfrogs.editor.gui.texture.BufferedImageWrapper;
import net.highwayfrogs.editor.gui.texture.atlas.AtlasTexture;
import net.highwayfrogs.editor.gui.texture.atlas.SequentialTextureAtlas;
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
    private final GreatQuestModelBoneConnectorNode boneConnectorNode;
    private final GreatQuestModelSkeletonMeshNode boneNode;
    private final AtlasTexture defaultBoneTexture;
    private final AtlasTexture parentBoneTexture;
    private final AtlasTexture childBoneTexture;
    private final AtlasTexture defaultConnectorTexture;
    private final AtlasTexture selectedConnectorTexture;
    private final AtlasTexture selectedBoneTexture;
    private PhongMaterial highlightedMaterial;
    private kcNode selectedBone;
    private final EventHandler<? super MouseEvent> mouseEventHandler = this::handleClick;

    public GreatQuestModelSkeletonMesh(GreatQuestModelMesh fullMesh, String modelName) {
        super(new SequentialTextureAtlas(32, 32, false), DynamicMeshTextureQuality.LIT_BLURRY, modelName);
        this.fullMesh = fullMesh;
        getTextureAtlas().startBulkOperations();
        this.defaultBoneTexture = getTextureAtlas().addTexture(new BufferedImageWrapper(ColorUtils.makeColorImage(Color.TEAL)));
        this.selectedBoneTexture = getTextureAtlas().addTexture(new BufferedImageWrapper(ColorUtils.makeColorImage(Color.YELLOW)));
        this.childBoneTexture = getTextureAtlas().addTexture(new BufferedImageWrapper(ColorUtils.makeColorImage(Color.LIMEGREEN)));
        this.parentBoneTexture = getTextureAtlas().addTexture(new BufferedImageWrapper(ColorUtils.makeColorImage(Color.RED)));
        this.defaultConnectorTexture = getTextureAtlas().addTexture(new BufferedImageWrapper(ColorUtils.makeColorImage(Color.WHITE)));
        this.selectedConnectorTexture = getTextureAtlas().addTexture(new BufferedImageWrapper(ColorUtils.makeColorImage(Color.SALMON)));
        getTextureAtlas().endBulkOperations();

        this.boneConnectorNode = new GreatQuestModelBoneConnectorNode(this);
        addNode(this.boneConnectorNode);

        this.boneNode = new GreatQuestModelSkeletonMeshNode(this);
        addNode(this.boneNode);
    }

    @Override
    protected PhongMaterial updateMaterial(BufferedImage newImage) {
        newImage = GreatQuestUtils.fillEmptyAlpha(newImage);
        PhongMaterial parentMaterial = super.updateMaterial(newImage);
        this.highlightedMaterial = Scene3DUtils.updateHighlightMaterial(this.highlightedMaterial, newImage);
        return parentMaterial;
    }

    @Override
    public boolean addView(MeshView view, MeshTracker meshTracker) {
        boolean success = super.addView(view, meshTracker);
        if (success && view.getOnMouseClicked() == null)
            view.setOnMouseClicked(this.mouseEventHandler);

        return success;
    }

    @Override
    public boolean removeView(MeshView view) {
        boolean success = super.removeView(view);
        if (success && view.getOnMouseClicked() == this.mouseEventHandler)
            view.setOnMouseClicked(null);

        return success;
    }

    /**
     * Programmatically selects a bone, updating the skeleton mesh highlighting.
     * @param bone the bone to select, or null to deselect
     */
    public void setSelectedBone(kcNode bone) {
        this.selectedBone = bone;
        updateTexCoords();
    }

    /**
     * Handles the mesh getting clicked.
     * @param event the click event.
     */
    public void handleClick(MouseEvent event) {
        PickResult result = event.getPickResult();
        int faceIndex = result.getIntersectedFace();
        if (faceIndex < 0)
            return;

        DynamicMeshDataEntry entry = getDataEntryByFaceIndex(faceIndex);
        if (!(entry instanceof DynamicMeshAdapterNode.DynamicMeshTypedDataEntry))
            return;

        @SuppressWarnings("unchecked")
        kcNode bone = ((DynamicMeshAdapterNode<kcNode>.DynamicMeshTypedDataEntry) entry).getDataSource();
        if (bone == null)
            return;

        getLogger().info("Clicked on bone '%s', tag=%d", bone.getName(), bone.getTag());
        setSelectedBone(bone);
    }
}
