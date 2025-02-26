package net.highwayfrogs.editor.games.sony.shared.mof2.ui;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.games.sony.shared.ui.file.MOFController.MOFUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.utils.ColorUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;

/**
 * TODO: Documentation.
 * TODO: Copy features from MOFController.java
 * Created by Kneesnap on 2/21/2025.
 */
public class MRModelMeshController extends MeshViewController<MRModelMesh> {
    // TODO: UI Managers
    private MOFUIController uiController; // TODO: Port into managers.
    private PerspectiveCamera camera; // TODO: KEEP?
    private Scene mofScene; // TODO: meshScene
    private Group root3D;
    private Rotate rotX; // TODO: KEEP?
    private Rotate rotY;
    private Rotate rotZ;
    private final RenderManager renderManager = new RenderManager();
    private boolean selectingVertex; // TODO: ???
    private boolean selectingPart; // TODO: ???
    private MeshData textureOverlay; // TODO: ???
    private MeshData hiliteOverlay; // TODO: ???

    @Setter private Vector showPosition; // TODO: ???

    private static final PhongMaterial HILITE_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.PURPLE);
    private static final PhongMaterial COLLPRIM_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial BBOX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.RED);
    private static final PhongMaterial GENERIC_POS_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.YELLOW);
    public static final CursorVertexColor ANIMATION_COLOR = new CursorVertexColor(java.awt.Color.MAGENTA, java.awt.Color.BLACK);
    public static final CursorVertexColor CANT_APPLY_COLOR = new CursorVertexColor(java.awt.Color.RED, java.awt.Color.BLACK);
    public static final CursorVertexColor HILITE_COLOR = new CursorVertexColor(ColorUtils.toAWTColor(Color.PURPLE), java.awt.Color.BLACK);
    public static final double MAP_VIEW_FAR_CLIP = 2000.0;
    public static final ImageFilterSettings PREVIEW_SETTINGS = new ImageFilterSettings(ImageState.EXPORT).setTrimEdges(true);


    public MRModelMeshController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public SCGameInstance getGameInstance() {
        return (SCGameInstance) super.getGameInstance();
    }

    @Override
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        super.setupBindings(subScene3D, meshView);
        getMainLight().getScope().add(getMeshView());
        getMainLight().getScope().addAll(getAxisDisplayList().getNodes());
        // TODO: this.generalManager.getSidePanel().requestFocus();
    }

    @Override
    protected void setupManagers() {

    }

    @Override
    public String getMeshDisplayName() {
        return getModel().getFileDisplayName();
    }

    @Override
    protected void setDefaultCameraPosition() {
        MRModel model = getModel();
    }

    /**
     * Gets the model file.
     */
    public MRModel getModel() {
        return getMesh().getModel();
    }
}
