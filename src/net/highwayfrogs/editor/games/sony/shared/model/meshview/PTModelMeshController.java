package net.highwayfrogs.editor.games.sony.shared.model.meshview;

import javafx.scene.SubScene;
import javafx.scene.shape.MeshView;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Controls a PTModelMesh.
 * Created by Kneesnap on 5/22/2024.
 */
public class PTModelMeshController extends MeshViewController<PTModelMesh> {
    public PTModelMeshController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getMainLight().getScope().add(getMeshView());
        getCheckBoxEnablePsxShading().setSelected(true);
    }

    @Override
    protected void setupManagers() {
        // TODO: Add an editor later.
    }

    @Override
    public String getMeshDisplayName() {
        return getMesh().getModel().getStaticMeshFile().getFileDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        getFirstPersonCamera().setPos(0, -2, -20);
    }

    @Override
    protected double getAxisDisplayLength() {
        return 3.333333333333D;
    }

    @Override
    protected double getAxisDisplaySize() {
        return .25;
    }
}