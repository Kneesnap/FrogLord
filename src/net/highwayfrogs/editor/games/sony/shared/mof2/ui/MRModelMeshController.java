package net.highwayfrogs.editor.games.sony.shared.mof2.ui;


import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelCollprimUIManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelHiliteUIManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelMainUIManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers.MRModelTextureAnimationUIManager;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.gui.editor.MeshMouseRotationUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * The core UI controller for the MRModelMesh viewer.
 * Created by Kneesnap on 2/21/2025.
 */
@Getter
public class MRModelMeshController extends MeshViewController<MRModelMesh> {
    private final MRModelMainUIManager mainManager;
    private final MeshMouseRotationUIManager<MRModelMesh> rotationManager;
    private final MRModelCollprimUIManager collprimManager;
    private final MRModelHiliteUIManager hiliteManager;
    private final MRModelTextureAnimationUIManager textureAnimationManager;

    public static final double MAP_VIEW_FAR_CLIP = 2000.0;
    public static final double CAMERA_DEFAULT_TRANSLATE_Y = 0;
    public static final double CAMERA_DEFAULT_TRANSLATE_Z = -45;
    public static final double CAMERA_DEFAULT_ROTATION_X = 40;
    public static final double CAMERA_DEFAULT_ROTATION_Y = 180;

    public MRModelMeshController(SCGameInstance instance) {
        super(instance);
        this.mainManager = new MRModelMainUIManager(this);
        this.rotationManager = new MeshMouseRotationUIManager<>(this);
        this.collprimManager = new MRModelCollprimUIManager(this);
        this.hiliteManager = new MRModelHiliteUIManager(this);
        this.textureAnimationManager = new MRModelTextureAnimationUIManager(this);
    }

    @Override
    public SCGameInstance getGameInstance() {
        return (SCGameInstance) super.getGameInstance();
    }

    @Override
    public PerspectiveCamera getCamera() {
        return this.rotationManager.getCamera();
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getMainLight().getScope().add(getMeshView());
        getMainLight().getScope().addAll(getAxisDisplayList().getNodes());
        getComboBoxMeshCullFace().setValue(CullFace.NONE); // Easier to look at models like this.
        getColorPickerLevelBackground().setValue(Color.GRAY);
    }

    @Override
    protected void setupManagers() {
        addManager(this.mainManager);
        addManager(this.rotationManager);
        addManager(this.collprimManager);
        addManager(this.hiliteManager);
        addManager(this.textureAnimationManager);
    }

    @Override
    protected double getAxisDisplayLength() {
        return 8; // Half of a grid square.
    }

    @Override
    protected double getAxisDisplaySize() {
        return 1;
    }

    @Override
    public String getMeshDisplayName() {
        return getModel().getFileDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        Camera camera = getCamera();
        camera.setFarClip(MAP_VIEW_FAR_CLIP);
        camera.setTranslateZ(CAMERA_DEFAULT_TRANSLATE_Z);
        camera.setTranslateY(CAMERA_DEFAULT_TRANSLATE_Y);
        this.rotationManager.getRotationX().setAngle(CAMERA_DEFAULT_ROTATION_X);
        this.rotationManager.getRotationY().setAngle(CAMERA_DEFAULT_ROTATION_Y);
    }

    /**
     * Gets the model file.
     */
    public MRModel getModel() {
        return getMesh().getModel();
    }

    /**
     * Gets the currently active static mof.
     */
    public MRStaticMof getActiveStaticMof() {
        return getMesh().getActiveStaticMof();
    }

    /**
     * Gets the currently active static mof model.
     */
    public MRAnimatedMofModel getActiveAnimatedMofModel() {
        return getMesh().getActiveMofModel();
    }
}
