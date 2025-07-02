package net.highwayfrogs.editor.games.renderware.mesh.clump;

import javafx.scene.SubScene;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.renderware.chunks.RwClumpChunk;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Implements a world mesh view controller for RenderWare world meshes.
 * Created by Kneesnap on 8/18/2024.
 */
public class RwClumpMeshController extends MeshViewController<RwClumpMesh> {
    public RwClumpMeshController(GameInstance instance) {
        super(instance);
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getComboBoxMeshCullFace().setValue(CullFace.BACK);
        getMainLight().getScope().add(meshView);
    }

    @Override
    protected void setupManagers() {
        // TODO: Setup managers.
    }

    @Override
    public String getMeshDisplayName() {
        return getClump().getCollectionViewDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        setupDefaultInverseCamera();
    }

    @Override
    protected double getAxisDisplayLength() {
        return 3;
    }

    @Override
    protected double getAxisDisplaySize() {
        return 1;
    }

    @Override
    protected boolean mapRendersFirst() {
        // Gives preference to transparent entities, since the map is rarely (if ever?) transparent.
        return true;
    }

    /**
     * Gets the clump which the mesh represents.
     */
    public RwClumpChunk getClump() {
        return getMesh().getClump();
    }
}