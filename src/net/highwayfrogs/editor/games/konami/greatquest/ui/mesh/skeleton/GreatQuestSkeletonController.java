package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.skeleton;

import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;

/**
 * Allows previewing an animation skeleton in 3D space.
 * TODO: OK, so each bone has a position relative to the previous bone. Also, each prim has the bone IDs it needs to use.
 * TODO: So, can we come up with a way to preview these bone positions/rotations/translations as positions?
 * TODO: Ideally we can just view the animated skeleton. Yeah, like without the model we just view the skeleton, then we can allow in that view to apply a model to that skeleton. Perfect.
 * Created by Kneesnap on 9/30/2024.
 */
public class GreatQuestSkeletonController extends MeshViewController<DynamicMesh> {
    @Getter private final kcCResourceSkeleton skeleton;
    private DisplayList skeletonDisplayList;

    public GreatQuestSkeletonController(@NonNull kcCResourceSkeleton skeleton) {
        super(skeleton.getGameInstance());
        this.skeleton = skeleton;
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getLightingGroup().getChildren().add(getMeshView());
        getComboBoxMeshCullFace().setValue(CullFace.NONE); // Great Quest has no back-face culling.
        getColorPickerLevelBackground().setValue(Color.GRAY); // Gray is better for viewing models.

        // Setup skeleton.
        /*this.skeletonDisplayList = getRenderManager().createDisplayListWithNewGroup();
        for (kcNode node : this.skeleton.getAllNodes()) {
            if (node.getParent() != null)
                this.skeleton.add

            this.skeletonDisplayList.addSphere()
        }*/

        // Create mesh views necessary to display.
        /*if (getModel() != null && getMesh().getActualMesh() != null) {
            this.meshViewCollection = new GreatQuestModelMeshViewCollection(this);
            this.meshViewCollection.setMesh(getMesh().getActualMesh());
        }*/
    }

    @Override
    protected void setupManagers() {
        // TODO: !
    }

    @Override
    public String getMeshDisplayName() {
        return null; // TODO: !
    }

    @Override
    protected void setDefaultCameraPosition() {
        // TODO: !
    }
}
