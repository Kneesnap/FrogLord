package net.highwayfrogs.editor.gui.components.mesh;

import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshCollection.MeshViewCollection;
import net.highwayfrogs.editor.gui.mesh.MeshTracker;
import net.highwayfrogs.editor.utils.lambda.Consumer3;

/**
 * Represents a UI component for a 3D viewport.
 * Created by Kneesnap on 10/10/2025.
 */
public class Embedded3DViewComponent<TGameInstance extends GameInstance> extends GameUIController<TGameInstance> {
    private final MeshTracker meshTracker = new MeshTracker();
    @Getter private final Group root3D = new Group();
    private final InputManager inputManager;
    @Getter private final RotationCamera rotationCamera;

    private static final int DEFAULT_WIDTH = 500;
    private static final int DEFAULT_HEIGHT = 300;

    public Embedded3DViewComponent(TGameInstance instance) {
        super(instance);
        this.inputManager = new InputManager(instance);
        this.rotationCamera = new RotationCamera(this.inputManager);
        loadController(new SubScene(new Region(), DEFAULT_WIDTH, DEFAULT_HEIGHT, true, SceneAntialiasing.DISABLED));
    }

    public Embedded3DViewComponent(TGameInstance instance, @NonNull DynamicMesh mesh) {
        this(instance);
        addMeshToView(mesh);
    }

    @Override
    public SubScene getRootNode() {
        return (SubScene) super.getRootNode();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        SubScene subScene = (SubScene) rootNode;
        subScene.setFill(Color.GRAY);

        this.rotationCamera.getCamera().setFarClip(1000F);
        this.rotationCamera.getCamera().setNearClip(.1F);
        this.inputManager.assignSceneControls(subScene);

        // Apply 3D stuff to scene.
        subScene.setCamera(this.rotationCamera.getCamera());
        subScene.setRoot(this.root3D);
    }

    /**
     * Adds a mesh to the 3D view.
     * @param mesh the mesh to add to the view.
     */
    public void addMeshToView(DynamicMesh mesh) {
        if (mesh == null)
            throw new NullPointerException("mesh");

        MeshView meshView = new MeshView();
        addMeshView(new MeshView());
        meshView.setCullFace(CullFace.NONE);
        if (mesh.addView(meshView, this.meshTracker))
            addMeshView(meshView);
    }

    private void addMeshView(MeshView meshView) {
        this.root3D.getChildren().add(meshView);
        this.rotationCamera.addRotationsToNode(meshView);
    }

    /**
     * Adds a mesh collection to the view component.
     * @param meshCollection the mesh collection to add
     */
    public <TMesh extends DynamicMesh> void addMeshCollection(DynamicMeshCollection<TMesh> meshCollection, Consumer3<Integer, TMesh, MeshView> setupMeshHandler) {
        if (meshCollection == null)
            throw new NullPointerException("meshCollection");

        ModelMeshViewCollection<TMesh> meshViewCollection = new ModelMeshViewCollection<>(this, setupMeshHandler);
        meshViewCollection.setMesh(meshCollection);
    }

    /**
     * Takes a screenshot of the 3D view.
     */
    public Image takeScreenshot() {
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);

        SubScene subScene = getRootNode();
        WritableImage resultImage = new WritableImage((int) subScene.getWidth(), (int) subScene.getHeight()); // Must be correct size, this doesn't scale the image if we provide something lower, only cuts it off.
        return subScene.snapshot(snapshotParameters, resultImage);
    }

    /**
     * The camera to display with.
     */
    public PerspectiveCamera getCamera() {
        return this.rotationCamera.getCamera();
    }

    private static class ModelMeshViewCollection<TMesh extends DynamicMesh> extends MeshViewCollection<TMesh> {
        private final Embedded3DViewComponent<? extends GameInstance> viewComponent;
        private final Consumer3<Integer, TMesh, MeshView> setupMeshHandler;

        public ModelMeshViewCollection(Embedded3DViewComponent<? extends GameInstance> viewComponent, Consumer3<Integer, TMesh, MeshView> setupMeshHandler) {
            super(null, null);
            this.viewComponent = viewComponent;
            this.setupMeshHandler = setupMeshHandler;
        }

        @Override
        protected void onMeshViewSetup(int meshIndex, TMesh mesh, MeshView meshView) {
            super.onMeshViewSetup(meshIndex, mesh, meshView);
            this.viewComponent.addMeshView(meshView);
            if (this.setupMeshHandler != null)
                this.setupMeshHandler.accept(meshIndex, mesh, meshView);
        }
    }

}
