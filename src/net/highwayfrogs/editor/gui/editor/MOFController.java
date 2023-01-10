package net.highwayfrogs.editor.gui.editor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShaderMode;
import net.highwayfrogs.editor.file.map.view.TextureMap.TextureSource;
import net.highwayfrogs.editor.file.mof.*;
import net.highwayfrogs.editor.file.mof.hilite.MOFHilite;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnim;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntry;
import net.highwayfrogs.editor.file.mof.poly_anim.MOFPartPolyAnimEntryList;
import net.highwayfrogs.editor.file.mof.prims.MOFPolyTexture;
import net.highwayfrogs.editor.file.mof.prims.MOFPolygon;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.Vector;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.AbstractIndexStringConverter;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controls the MOF editor GUI.
 * Unfortunately this is a supreme mess atm. I'm not sure if it's worth cleaning up though. There won't be a whole lot of major additions to MOF, except maybe FMD, which isn't UI-related.
 * Created by Kneesnap on 2/13/2019.
 */
@Getter
public class MOFController extends EditorController<MOFHolder> {
    private double oldMouseX;
    private double oldMouseY;
    private double mouseX;
    private double mouseY;

    private MOFUIController uiController;
    private PerspectiveCamera camera;
    private Scene mofScene;
    private MOFMesh mofMesh;
    private Group root3D;
    private Rotate rotX;
    private Rotate rotY;
    private Rotate rotZ;
    private final RenderManager renderManager = new RenderManager();
    private boolean selectingVertex;
    private boolean selectingPart;
    private MeshData textureOverlay;

    @Setter private Vector showPosition;

    private static final String LIGHTING_LIST = "extraLighting";
    private static final String HILITE_LIST = "hiliteBoxes";
    private static final String BBOX_LIST = "boundingBoxes";
    private static final String COLLPRIM_BOX_LIST = "collprimBoxes";
    public static final String HILITE_VERTICE_LIST = "mofHiliteVerticeChoices";

    private static final PhongMaterial HILITE_MATERIAL = Utils.makeSpecialMaterial(Color.PURPLE);
    private static final PhongMaterial COLLPRIM_MATERIAL = Utils.makeSpecialMaterial(Color.LIGHTGREEN);
    private static final PhongMaterial BBOX_MATERIAL = Utils.makeSpecialMaterial(Color.RED);

    private static final String GENERIC_POS_LIST = "genericPositionList";
    private static final double GENERIC_POS_SIZE = 3;
    private static final PhongMaterial GENERIC_POS_MATERIAL = Utils.makeSpecialMaterial(Color.YELLOW);
    public static final CursorVertexColor ANIMATION_COLOR = new CursorVertexColor(java.awt.Color.MAGENTA, java.awt.Color.BLACK);
    public static final CursorVertexColor CANT_APPLY_COLOR = new CursorVertexColor(java.awt.Color.RED, java.awt.Color.BLACK);

    @Override
    public void onInit(AnchorPane editorRoot) {
        setupMofViewer(GUIMain.MAIN_STAGE);
    }

    @SneakyThrows
    private void setupMofViewer(Stage stageToOverride) {
        this.mofMesh = getFile().makeMofMesh();
        this.uiController = new MOFUIController(this);

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(getMofMesh());

        this.rotX = new Rotate(0, Rotate.X_AXIS);
        this.rotY = new Rotate(0, Rotate.Y_AXIS);
        this.rotZ = new Rotate(0, Rotate.Z_AXIS);
        meshView.getTransforms().addAll(rotX, rotY, rotZ);

        meshView.setMaterial(getMofMesh().getTextureMap().getDiffuseMaterial());
        meshView.setCullFace(CullFace.NONE);

        // Setup a perspective camera through which the 3D view is realised.
        this.camera = new PerspectiveCamera(true);

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/javafx/mof-view.fxml"));
        fxmlLoader.setController(this.uiController);
        Parent loadRoot = fxmlLoader.load();

        // Create the 3D elements and use them within a subscene.
        this.root3D = new Group(this.camera, meshView);
        SubScene subScene3D = new SubScene(root3D, stageToOverride.getScene().getWidth() - uiController.uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.BALANCED);
        camera.setFarClip(MapUIController.MAP_VIEW_FAR_CLIP);
        subScene3D.setFill(Color.GRAY);
        subScene3D.setCamera(camera);
        getRenderManager().setRenderRoot(this.root3D);
        getRenderManager().addMissingDisplayList(HILITE_VERTICE_LIST);
        updateHiliteBoxes();
        updateCollprimBoxes();

        // Setup the UI layout.
        BorderPane uiPane = new BorderPane();
        uiPane.setLeft(loadRoot);
        uiPane.setCenter(subScene3D);

        // Create and set the scene.
        mofScene = new Scene(uiPane);
        Scene defaultScene = Utils.setSceneKeepPosition(stageToOverride, mofScene);

        // Handle scaling of SubScene on stage resizing.
        mofScene.widthProperty().addListener((observable, old, newVal) -> subScene3D.setWidth(newVal.doubleValue() - uiController.uiRootPaneWidth()));
        subScene3D.heightProperty().bind(mofScene.heightProperty());

        // Input (key) event processing.
        mofScene.setOnKeyPressed(event -> {
            // Exit the viewer.
            if (event.getCode() == KeyCode.ESCAPE) {
                if (isSelectingVertex()) {
                    this.selectingVertex = false;
                    getRenderManager().clearDisplayList(HILITE_VERTICE_LIST);
                    return;
                }

                if (isSelectingPart()) {
                    this.selectingPart = false;
                    updateTexture3D();
                    return;
                }

                getUiController().stopPlaying();
                getRenderManager().removeAllDisplayLists();
                Utils.setSceneKeepPosition(stageToOverride, defaultScene);
                return;
            } else if (event.getCode() == KeyCode.F10) {
                Utils.takeScreenshot(subScene3D, getMofScene(), Utils.stripExtension(getFile().getFileEntry().getDisplayName()));
            }

            if (event.getCode() == KeyCode.S && event.isControlDown()) { // Save the texture map.
                try {
                    ImageIO.write(getMofMesh().getTextureMap().getTextureTree().getImage(), "png", new File(GUIMain.getWorkingDirectory(), "texMap-" + Utils.stripExtension(getMofMesh().getMofHolder().getFileEntry().getDisplayName()) + ".png"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            // Toggle wireframe mode.
            if (event.getCode() == KeyCode.X)
                meshView.setDrawMode(meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);

            if (!getUiController().shouldPreventFrameChange()) {
                if (event.getCode() == KeyCode.LEFT) {
                    getMofMesh().previousFrame();
                    getUiController().updateFrameText();
                } else if (event.getCode() == KeyCode.RIGHT) {
                    getMofMesh().nextFrame();
                    getUiController().updateFrameText();
                }
            }
        });

        mofScene.setOnScroll(evt -> camera.setTranslateZ(camera.getTranslateZ() + (evt.getDeltaY() * .25)));

        mofScene.setOnMouseClicked(evt -> {
            MOFPolygon clickedPoly = getMofMesh().getFacePolyMap().get(evt.getPickResult().getIntersectedFace());
            if (clickedPoly == null)
                return;

            if (isSelectingPart()) {
                MOFPartPolyAnimEntryList newList = new MOFPartPolyAnimEntryList(clickedPoly.getParentPart());
                newList.getParent().getPartPolyAnimLists().add(newList);
                getUiController().updateTextureList();
                getUiController().getAnimationListChoiceBox().getSelectionModel().select(newList);
                this.selectingPart = false;
                updateTexture3D();
                return;
            }

            if (!(clickedPoly instanceof MOFPolyTexture) || getUiController().getSelectedList() == null)
                return;

            MOFPartPolyAnim anim = null;
            MOFPart part = clickedPoly.getParentPart();
            for (MOFPartPolyAnim testAnim : part.getPartPolyAnims()) {
                if (clickedPoly.equals(testAnim.getMofPolygon())) {
                    anim = testAnim;
                    break;
                }
            }

            if (anim != null && anim.getEntryList() != getUiController().getSelectedList())
                return; // If there's already an animation, but it's on a different entry list, don't remove it.

            if (anim == null && (clickedPoly.getParentPart() != getUiController().getSelectedList().getParent()))
                return; // If the poly is not on the part which the texture list is for, then we can't add the texture.

            if (anim != null) { // Remove anim entry.
                anim.getParentPart().getPartPolyAnims().remove(anim);
            } else { // Add anim entry.
                clickedPoly.getParentPart().getPartPolyAnims().add(new MOFPartPolyAnim(part, clickedPoly, getUiController().getSelectedList()));
            }

            updateTexture3D();
        });

        mofScene.setOnMousePressed(e -> {
            mouseX = oldMouseX = e.getSceneX();
            mouseY = oldMouseY = e.getSceneY();

            uiController.anchorPaneUIRoot.requestFocus();
        });

        mofScene.setOnMouseDragged(e -> {
            oldMouseX = mouseX;
            oldMouseY = mouseY;
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
            double mouseXDelta = (mouseX - oldMouseX);
            double mouseYDelta = (mouseY - oldMouseY);

            if (e.isPrimaryButtonDown()) {
                rotX.setAngle(rotX.getAngle() + (mouseYDelta * 0.25)); // Rotate the object.
                rotY.setAngle(rotY.getAngle() - (mouseXDelta * 0.25));
            } else if (e.isMiddleButtonDown()) {
                camera.setTranslateX(camera.getTranslateX() - (mouseXDelta * 0.25)); // Move the camera.
                camera.setTranslateY(camera.getTranslateY() - (mouseYDelta * 0.25));
            }
        });

        camera.setTranslateZ(-100.0);
        camera.setTranslateY(-10.0);
        updateLighting(false);
        updateCollprimBoxes();
    }

    /**
     * Updates lighting settings for the model.
     * @param useBrightMode Should we apply the fancy lighting?
     */
    public void updateLighting(boolean useBrightMode) {
        getRenderManager().addMissingDisplayList(LIGHTING_LIST);
        getRenderManager().clearDisplayList(LIGHTING_LIST);
        Group lightingGroup = new Group();

        AmbientLight ambLight = new AmbientLight();
        float colorValue = useBrightMode ? .2F : 1;
        ambLight.setColor(Color.color(colorValue, colorValue, colorValue));
        lightingGroup.getChildren().add(ambLight);

        if (useBrightMode) {
            PointLight pointLight1 = new PointLight();
            pointLight1.setColor(Color.color(0.9, 0.9, 0.9));
            pointLight1.setTranslateX(-100.0);
            pointLight1.setTranslateY(-100.0);
            pointLight1.setTranslateZ(-100.0);
            lightingGroup.getChildren().add(pointLight1);

            PointLight pointLight2 = new PointLight();
            pointLight2.setColor(Color.color(0.8, 0.8, 1.0));
            pointLight2.setTranslateX(100.0);
            pointLight2.setTranslateY(-100.0);
            pointLight2.setTranslateZ(-100.0);
            lightingGroup.getChildren().add(pointLight2);
        }

        getRenderManager().addNode(LIGHTING_LIST, lightingGroup);
    }

    /**
     * Adds a MOFBBox to the view.
     * @param listId The render list id.
     * @param box    The box to add.
     */
    public Box addMOFBoundingBox(String listId, MOFBBox box, PhongMaterial material) {
        SVector min = box.getVertices()[0];
        SVector max = box.getVertices()[7];
        Box boxNode = getRenderManager().addBoundingBoxFromMinMax(listId, min.getFloatX(), min.getFloatY(), min.getFloatZ(), max.getFloatX(), max.getFloatY(), max.getFloatZ(), material, true);
        applyRotation(boxNode);
        return boxNode;
    }

    /**
     * Update hilite boxes.
     */
    public void updateHiliteBoxes() {
        int totalHilites = getMofMesh().getMofHolder().asStaticFile().getHiliteCount();
        getUiController().getViewHilitesCheckbox().setText("Show Hilites [" + totalHilites + "]");
        getUiController().getViewHilitesCheckbox().setDisable(totalHilites == 0);

        boolean display = getUiController().getViewHilitesCheckbox().isSelected();
        updateHiliteBoxes(display);
    }

    /**
     * Update hilite boxes.
     * @param showBoxes Should show hilite boxes.
     */
    public void updateHiliteBoxes(boolean showBoxes) {
        getRenderManager().addMissingDisplayList(HILITE_LIST);
        getRenderManager().clearDisplayList(HILITE_LIST);

        if (!showBoxes)
            return;

        for (MOFPart part : getFile().asStaticFile().getParts()) {
            for (MOFHilite hilite : part.getHilites()) {
                SVector vertex = hilite.getVertex();
                Box hiliteBox = getRenderManager().addBoundingBoxCenteredWithDimensions(HILITE_LIST, vertex.getFloatX(), vertex.getFloatY(), vertex.getFloatZ(), 1, 1, 1, HILITE_MATERIAL, true);
                applyRotation(hiliteBox);
                hiliteBox.setOnMouseClicked(evt -> hilite.setupEditor(this));
                hiliteBox.setMouseTransparent(false);
            }
        }
    }

    /**
     * Update collprim boxes.
     */
    public void updateCollprimBoxes() {
        boolean display = getUiController().getViewCollprimCheckbox().isSelected();
        int collprimCount = getMofMesh().getMofHolder().asStaticFile().getCollprimCount();
        boolean foundAny = (collprimCount > 0);

        getUiController().getViewCollprimCheckbox().setText("Show Collprim(s) [" + collprimCount + "]");
        getUiController().getViewCollprimCheckbox().setDisable(!foundAny);
        getUiController().getAddCollprimButton().setDisable(foundAny || getFile().asStaticFile().getParts().isEmpty());
        updateCollprimBoxes(foundAny && display);
    }

    /**
     * Update collprim boxes.
     * @param showCollprim Should show collprim boxes.
     */
    public void updateCollprimBoxes(boolean showCollprim) {
        getRenderManager().addMissingDisplayList(COLLPRIM_BOX_LIST);
        getRenderManager().clearDisplayList(COLLPRIM_BOX_LIST);
        if (!showCollprim) {
            getUiController().getCollprimEditorGrid().clearEditor();
            return;
        }

        for (MOFPart part : getFile().asStaticFile().getParts()) {
            MOFCollprim collprim = part.getCollprim();
            if (collprim != null)
                applyRotation(collprim.addDisplay(this, getRenderManager(), COLLPRIM_BOX_LIST, COLLPRIM_MATERIAL));
        }
    }

    /**
     * Update bounding boxes.
     * @param showBoxes Should show hilite boxes.
     */
    public void updateBoundingBoxes(boolean showBoxes) {
        getRenderManager().addMissingDisplayList(BBOX_LIST);
        getRenderManager().clearDisplayList(BBOX_LIST);
        if (!showBoxes)
            return;

        for (MOFPart part : getFile().asStaticFile().getParts())
            addMOFBoundingBox(BBOX_LIST, part.makeBoundingBox(), BBOX_MATERIAL);

        if (getFile().isAnimatedMOF())
            addMOFBoundingBox(BBOX_LIST, getFile().getAnimatedFile().makeBoundingBox(), BBOX_MATERIAL);
    }

    public void applyRotation(Node node) {
        Rotate lightRotateX = new Rotate(0, Rotate.X_AXIS); // Up, Down,
        Rotate lightRotateY = new Rotate(0, Rotate.Y_AXIS); // Left, Right
        Rotate lightRotateZ = new Rotate(0, Rotate.Z_AXIS); // In, Out
        lightRotateX.angleProperty().bind(rotX.angleProperty());
        lightRotateY.angleProperty().bind(rotY.angleProperty());
        lightRotateZ.angleProperty().bind(rotZ.angleProperty());

        double translateX = node.getTranslateX();
        double translateY = node.getTranslateY();
        double translateZ = node.getTranslateZ();

        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Translate))
                continue;

            Translate translate = (Translate) transform;
            translateX += translate.getX();
            translateY += translate.getY();
            translateZ += translate.getZ();
        }

        lightRotateX.setPivotY(-translateY);
        lightRotateX.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateY.setPivotX(-translateX); // <Left, Right>
        lightRotateY.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateZ.setPivotX(-translateX); // <Left, Right>
        lightRotateZ.setPivotY(-translateY); // <Up, Down>
        node.getTransforms().addAll(lightRotateX, lightRotateY, lightRotateZ);
    }

    /**
     * Update rotation for a particular node.
     * @param node The node to update rotation for.
     */
    public void updateRotation(Node node) {
        Rotate lightRotateX = null;
        Rotate lightRotateY = null;
        Rotate lightRotateZ = null;
        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Rotate))
                continue;

            Rotate temp = (Rotate) transform;
            if (temp.getAxis() == Rotate.X_AXIS) {
                lightRotateX = temp;
            } else if (temp.getAxis() == Rotate.Y_AXIS) {
                lightRotateY = temp;
            } else if (temp.getAxis() == Rotate.Z_AXIS) {
                lightRotateZ = temp;
            }
        }

        if (lightRotateX == null || lightRotateY == null || lightRotateZ == null) {
            System.out.println("Failed to update rotation.");
            return;
        }

        double translateX = node.getTranslateX();
        double translateY = node.getTranslateY();
        double translateZ = node.getTranslateZ();

        for (Transform transform : node.getTransforms()) {
            if (!(transform instanceof Translate))
                continue;

            Translate translate = (Translate) transform;
            translateX += translate.getX();
            translateY += translate.getY();
            translateZ += translate.getZ();
        }

        lightRotateX.setPivotY(-translateY);
        lightRotateX.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateY.setPivotX(-translateX); // <Left, Right>
        lightRotateY.setPivotZ(-translateZ); // Depth <Closest, Furthest>
        lightRotateZ.setPivotX(-translateX); // <Left, Right>
        lightRotateZ.setPivotY(-translateY); // <Up, Down>
    }

    /**
     * Setup the editor to edit an animation.
     * @param entryList The animation to edit.
     */
    public void setupAnimationEditor(MOFPartPolyAnimEntryList entryList) {
        GUIEditorGrid grid = getUiController().getTextureEditGrid();
        grid.clearEditor();
        if (entryList == null)
            return;

        // Setup preview.
        // Create the animation preview.
        List<Node> toDisable = new ArrayList<>();
        if (entryList.getEntries().size() > 0) {
            List<MOFPartPolyAnimEntry> entries = entryList.getEntries();
            MWDFile mwd = entryList.getMWD();

            grid.addBoldLabel("Preview:");
            AtomicBoolean isAnimating = new AtomicBoolean(false);
            AtomicInteger framesWaited = new AtomicInteger(0);
            int maxValidFrame = entries.size() - 1;
            ImageView imagePreview = grid.addCenteredImage(mwd.getImageByTextureId(entries.get(0).getImageId()).toFXImage(MAPAnimation.PREVIEW_SETTINGS), 150);
            Slider frameSlider = grid.addIntegerSlider("Animation Frame", 0, newFrame ->
                    imagePreview.setImage(mwd.getImageByTextureId(entries.get(newFrame).getImageId()).toFXImage(MAPAnimation.PREVIEW_SETTINGS)), 0, maxValidFrame);

            toDisable.add(frameSlider);
            double millisInterval = (1000D / mwd.getFPS());
            Timeline animationTimeline = new Timeline(new KeyFrame(Duration.millis(millisInterval), evt -> {
                if (framesWaited.getAndIncrement() == entries.get((int) frameSlider.getValue()).getDuration()) {
                    framesWaited.set(0);
                } else {
                    return;
                }

                int i = (int) frameSlider.getValue() + 1;
                if (i == maxValidFrame + 1)
                    i = 0;
                frameSlider.setValue(i);
            }));
            animationTimeline.setCycleCount(Timeline.INDEFINITE);

            imagePreview.setOnMouseClicked(evt -> {
                isAnimating.set(!isAnimating.get());
                boolean playNow = isAnimating.get();
                if (playNow) {
                    animationTimeline.play();
                } else {
                    animationTimeline.pause();
                }

                for (Node node : toDisable)
                    node.setDisable(playNow);
            });
        }

        // Setup editor.
        grid.addBoldLabel("Textures:");
        for (int i = 0; i < entryList.getEntries().size(); i++) {
            final int tempIndex = i;
            MOFPartPolyAnimEntry entry = entryList.getEntries().get(i);
            GameImage image = entryList.getMWD().getImageByTextureId(entry.getImageId());
            Image scaledImage = Utils.toFXImage(image.toBufferedImage(VLOArchive.ICON_EXPORT), true);
            ImageView view = new ImageView(scaledImage);
            view.setFitWidth(20);
            view.setFitHeight(20);

            view.setOnMouseClicked(evt -> getMofMesh().getMofHolder().getVloFile().promptImageSelection(newImage -> {
                entry.setImageId(newImage.getTextureId());
                setupAnimationEditor(entryList);
            }, false));

            HBox hbox = new HBox();
            hbox.setSpacing(5);
            hbox.getChildren().add(view);
            hbox.getChildren().add(new Label("Duration:"));
            grid.setupNode(hbox);

            HBox hbox2 = new HBox();

            TextField durationField = grid.setupSecondNode(new TextField(String.valueOf(entry.getDuration())), false);
            Utils.setHandleTestKeyPress(durationField, Utils::isInteger, newValue -> {
                entry.setDuration(Integer.parseInt(newValue));
                setupAnimationEditor(entryList);
            });
            durationField.setMaxWidth(30);

            Button removeButton = new Button("Remove");
            removeButton.setOnAction(evt -> {
                entryList.getEntries().remove(tempIndex);
                setupAnimationEditor(entryList);
            });

            hbox2.setSpacing(40);
            hbox2.getChildren().add(durationField);
            hbox2.getChildren().add(removeButton);
            grid.setupSecondNode(hbox2, false);
            grid.addRow(25);

            toDisable.add(view);
            toDisable.add(durationField);
            toDisable.add(removeButton);
        }

        toDisable.add(grid.addButton("Add Texture", () -> getMofMesh().getMofHolder().getVloFile().promptImageSelection(newImage -> {
            entryList.getEntries().add(new MOFPartPolyAnimEntry(newImage.getTextureId(), 1));
            setupAnimationEditor(entryList);
        }, false)));

        toDisable.add(grid.addButton("Delete Animation", () -> {
            entryList.getParent().getPartPolyAnims().removeIf(anim -> anim.getEntryList() == entryList);
            entryList.getParent().getPartPolyAnimLists().remove(entryList);

            getUiController().updateTextureList();
            getUiController().getAnimationListChoiceBox().getSelectionModel().selectFirst();
            updateTexture3D();
        }));
    }

    public void updateTexture3D() {
        if (this.textureOverlay != null) {
            getMofMesh().getManager().removeMesh(this.textureOverlay);
            this.textureOverlay = null;
        }

        if (isSelectingPart()) {
            for (MOFPolygon mofPolygon : getFile().asStaticFile().getAllPolygons())
                renderOverPolygon(mofPolygon, ANIMATION_COLOR);

            this.textureOverlay = getMofMesh().getManager().addMesh();
            return;
        }

        MOFPartPolyAnimEntryList entryList = getUiController().getSelectedList();
        if (entryList != null) {
            for (MOFPart part : getFile().asStaticFile().getParts()) {
                if (part == entryList.getParent()) { // It's the part we can edit.
                    for (MOFPartPolyAnim anim : part.getPartPolyAnims())
                        renderOverPolygon(anim.getMofPolygon(), (anim.getEntryList() == entryList) ? ANIMATION_COLOR : CANT_APPLY_COLOR);

                    // Render over non-textured polys.
                    for (MOFPolygon mofPolygon : part.getAllPolygons())
                        if (!(mofPolygon instanceof MOFPolyTexture))
                            renderOverPolygon(mofPolygon, CANT_APPLY_COLOR);
                } else { // We can't control this part.
                    for (MOFPolygon mofPoly : part.getAllPolygons())
                        renderOverPolygon(mofPoly, CANT_APPLY_COLOR);
                }
            }


            this.textureOverlay = getMofMesh().getManager().addMesh();
        }
    }

    /**
     * Render over an existing polygon.
     * @param targetPoly The polygon to render over.
     * @param source     The source to render.
     */
    public void renderOverPolygon(MOFPolygon targetPoly, TextureSource source) {
        getMofMesh().renderOverPolygon(targetPoly, source);
    }

    @Getter
    public static final class MOFUIController implements Initializable {
        private MOFHolder holder;
        private MOFController controller;

        // Baseline UI components
        @FXML private AnchorPane anchorPaneUIRoot;
        @FXML private Accordion accordionLeft;

        @FXML private Label modelName;
        @FXML private ComboBox<Integer> animationSelector;
        @FXML private Button playButton;
        @FXML private CheckBox repeatCheckbox;
        @FXML private CheckBox textureAnimationCheckbox;
        @FXML private TextField fpsField;

        @FXML private Slider frameSlider;
        @FXML private Label frameLabel;

        @FXML private TitledPane paneAnim;
        @FXML private ComboBox<ShaderMode> shaderModeComboBox;
        @FXML private CheckBox brightModeCheckbox;
        @FXML private CheckBox viewBoundingBoxesCheckbox;

        private MOFPartPolyAnimEntryList selectedList;
        @FXML private TitledPane texturePane;
        @FXML private ChoiceBox<MOFPartPolyAnimEntryList> animationListChoiceBox;
        @FXML private Button addTextureAnimationButton;
        @FXML private GridPane textureEditPane;
        private GUIEditorGrid textureEditGrid;

        @FXML private TitledPane collprimPane;
        @FXML private CheckBox viewCollprimCheckbox;
        @FXML private Button addCollprimButton;
        @FXML private GridPane collprimEditPane;
        private GUIEditorGrid collprimEditorGrid;

        @FXML private TitledPane hilitePane;
        @FXML private CheckBox viewHilitesCheckbox;
        @FXML private Button addHiliteButton;
        @FXML private GridPane hiliteEditPane;
        private GUIEditorGrid hiliteEditorGrid;

        private final List<Node> toggleNodes = new ArrayList<>();
        private final List<Node> playNodes = new ArrayList<>();

        // Animation data.
        private int framesPerSecond = 20;
        private boolean animationPlaying;
        private Timeline animationTimeline;

        public MOFUIController(MOFController controller) {
            this.controller = controller;
        }

        @Override
        public void initialize(URL location, ResourceBundle resources) {
            this.brightModeCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> getController().updateLighting(newValue)));
            this.viewHilitesCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> getController().updateHiliteBoxes(newValue)));
            this.viewCollprimCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> getController().updateCollprimBoxes(newValue)));
            this.viewBoundingBoxesCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> getController().updateBoundingBoxes(newValue)));

            // Allow toggling texture animations.
            this.textureAnimationCheckbox.setSelected(getMofMesh().getTextureMap().isUseModelTextureAnimation());
            this.textureAnimationCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                getMofMesh().getTextureMap().setUseModelTextureAnimation(newValue);
                getMofMesh().updateFrame(); // Update the display.
            });

            // Setup frame slider.
            frameSlider.setMin(0);
            frameSlider.setBlockIncrement(1);
            frameSlider.setMinorTickCount(1);
            frameSlider.setSnapToTicks(true);
            frameSlider.setShowTickLabels(false);
            frameSlider.setShowTickMarks(true);
            frameSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
                if (!isAnimationPlaying()) { // If an animation is playing, this is handled by the animation.
                    getMofMesh().setFrame((int) (double) newValue);
                    updateFrameText();
                }
            }));

            // Shader stuff.
            this.shaderModeComboBox.setItems(FXCollections.observableArrayList(ShaderMode.values()));
            this.shaderModeComboBox.getSelectionModel().select(getMofMesh().getTextureMap().getMode());
            this.shaderModeComboBox.setConverter(new AbstractStringConverter<>(ShaderMode::getName));
            this.shaderModeComboBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
                getMofMesh().getTextureMap().updateModel(getHolder(), newValue);
                getMofMesh().updateFrame();
            }));

            this.addCollprimButton.setOnAction(evt -> {
                getHolder().asStaticFile().getParts().get(0).setCollprim(new MOFCollprim(getHolder().asStaticFile().getParts().get(0)));
                controller.updateCollprimBoxes();
            });

            this.animationListChoiceBox.valueProperty().addListener(((observable, oldValue, newValue) -> {
                this.selectedList = newValue;
                getController().updateTexture3D();
                getController().setupAnimationEditor(newValue);
            }));

            this.addTextureAnimationButton.setOnAction(evt -> {
                getController().selectingPart = true;
                getController().updateTexture3D();
            });

            this.addHiliteButton.setOnAction(evt -> {
                PhongMaterial material = Utils.makeSpecialMaterial(Color.MAGENTA);
                RenderManager manager = getController().getRenderManager();
                manager.clearDisplayList(HILITE_VERTICE_LIST);

                getController().selectingVertex = true;
                for (MOFPart part : getHolder().asStaticFile().getParts()) {
                    MOFPartcel partcel = part.getCel(Math.max(0, getMofMesh().getAnimationId()), getMofMesh().getFrame());

                    for (int i = 0; i < partcel.getVertices().size(); i++) {
                        final int index = i;
                        SVector vec = partcel.getVertices().get(i);
                        Box box = manager.addBoundingBoxCenteredWithDimensions(HILITE_VERTICE_LIST, vec.getFloatX(), vec.getFloatY(), vec.getFloatZ(), 1, 1, 1, material, true);
                        box.setMouseTransparent(false);
                        getController().applyRotation(box);
                        box.setOnMouseClicked(mouseEvt -> {
                            manager.clearDisplayList(HILITE_VERTICE_LIST);
                            getController().selectingVertex = false;

                            SVector realVec = part.getStaticPartcel().getVertices().get(index);
                            MOFHilite newHilite = new MOFHilite(part, realVec);
                            part.getHilites().add(newHilite);
                            getController().updateHiliteBoxes();
                            newHilite.setupEditor(getController());
                        });
                    }
                }
            });

            this.textureEditGrid = new GUIEditorGrid(getTextureEditPane());
            this.collprimEditorGrid = new GUIEditorGrid(getCollprimEditPane());
            this.hiliteEditorGrid = new GUIEditorGrid(getHiliteEditPane());

            toggleNodes.addAll(Arrays.asList(repeatCheckbox, animationSelector, fpsField, frameLabel, frameSlider, textureAnimationCheckbox));
            playNodes.addAll(Arrays.asList(playButton, frameSlider, frameLabel));

            playButton.setOnAction(evt -> {
                boolean newState = !isAnimationPlaying();
                playButton.setText(newState ? "Stop" : "Play");
                for (Node node : getToggleNodes())
                    node.setDisable(newState); // Set the toggle state of nodes.

                if (newState) {
                    startPlaying(playButton.getOnAction());
                } else {
                    stopPlaying();
                }
            });

            Utils.setHandleKeyPress(fpsField, newString -> {
                if (!Utils.isInteger(newString))
                    return false;

                int newFps = Integer.parseInt(newString);
                if (newFps < 0)
                    return false;

                this.framesPerSecond = newFps;
                return true;
            }, null);

            setHolder(getController()); // Setup current MOF.
        }

        private void updateTextureList() {
            List<MOFPartPolyAnimEntryList> entryLists = new ArrayList<>();
            entryLists.add(null);
            for (MOFPart part : getHolder().asStaticFile().getParts())
                for (MOFPartPolyAnimEntryList entryList : part.getPartPolyAnimLists())
                    if (!entryLists.contains(entryList))
                        entryLists.add(entryList);
            this.animationListChoiceBox.setItems(FXCollections.observableArrayList(entryLists));
            this.animationListChoiceBox.setConverter(new AbstractIndexStringConverter<>(entryLists, (index, val) -> val != null ? "Animation #" + (index + 1) + ", Part #" + val.getParent().getPartID() : "None"));
        }

        /**
         * Get the root pane width.
         */
        public double uiRootPaneWidth() {
            return anchorPaneUIRoot.getPrefWidth();
        }

        /**
         * Sets the MOF Holder this controls.
         */
        public void setHolder(MOFController controller) {
            this.controller = controller;
            this.holder = controller.getFile();

            // Setup animation control.
            List<Integer> numbers = new ArrayList<>(Utils.getIntegerList(holder.getMaxAnimation()));
            numbers.add(0, -1);
            animationSelector.setItems(FXCollections.observableArrayList(numbers));
            animationSelector.setConverter(new AbstractStringConverter<>(holder::getName));
            animationSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
                if (newValue != null)
                    setAnimation(newValue);
            }));

            animationSelector.getSelectionModel().select(0); // Automatically selects no animation.

            boolean disableState = !getHolder().asStaticFile().hasTextureAnimation();
            for (Node node : playNodes)
                node.setDisable(disableState); // Disable playing non-existing animation.

            updateTempUI();
            modelName.setText(getHolder().getFileEntry().getDisplayName());
            updateTextureList();
            this.animationListChoiceBox.getSelectionModel().selectFirst();
        }

        private boolean shouldPreventFrameChange() {
            return getController().isSelectingVertex() || isAnimationPlaying();
        }

        /**
         * Sets the new animation id to use.
         * @param newAnimation The new animation to use.
         */
        public void setAnimation(int newAnimation) {
            if (getController().isSelectingVertex())
                return;

            controller.getMofMesh().setAction(newAnimation);
            updateFrameText();

            // Toggle UI controls for playing.
            boolean disableState = newAnimation == -1 && !getHolder().asStaticFile().hasTextureAnimation();
            for (Node node : playNodes)
                node.setDisable(disableState); // Disable playing non-existing animation.
        }

        /**
         * A very quick and dirty (and temporary!) UI. Will be replaced...
         */
        public void updateTempUI() {
            anchorPaneUIRoot.requestFocus();
            paneAnim.setExpanded(true);
            accordionLeft.setExpandedPane(paneAnim);
            paneAnim.requestFocus();
            fpsField.setText(String.valueOf(getFramesPerSecond()));
            updateFrameText();
        }

        private void updateFrameText() {
            int currentFrame = getController().getMofMesh().getFrameCount();
            int frameCount = getMofMesh().getMofHolder().getFrameCount(getMofMesh().getAnimationId());

            frameLabel.setText("Frame " + currentFrame);
            frameSlider.setMax(frameCount - 1);
            frameSlider.setValue(currentFrame);
        }

        /**
         * Start playing the MOF animation.
         */
        public void startPlaying(EventHandler<ActionEvent> onFinish) {
            stopPlaying();

            boolean repeat = repeatCheckbox.isSelected();
            if (!repeat) { // Reset at frame zero when playing a non-paused mof.
                getMofMesh().resetFrame();
                updateFrameText();
            }

            this.animationPlaying = true;
            this.animationTimeline = new Timeline(new KeyFrame(Duration.millis(1000D / getFramesPerSecond()), evt -> {
                getMofMesh().nextFrame();
                updateFrameText();
            }));
            this.animationTimeline.setCycleCount(repeat ? Timeline.INDEFINITE : getMofMesh().getMofHolder().getFrameCount(getMofMesh().getAction()) - 1);
            this.animationTimeline.play();
            this.animationTimeline.setOnFinished(onFinish);
        }

        /**
         * Stop playing the MOF animation.
         */
        public void stopPlaying() {
            if (!isAnimationPlaying())
                return;

            this.animationPlaying = false;
            this.animationTimeline.stop();
            this.animationTimeline = null;
        }

        /**
         * Gets the MofMesh being viewed.
         * @return mofMesh
         */
        public MOFMesh getMofMesh() {
            return getController().getMofMesh();
        }
    }

    /**
     * Updates the marker to display at the given position.
     * If null is supplied, it'll get removed.
     */
    public void updateMarker(Vector vec, int bits, Vector origin, Box updateBox) {
        if (updateBox == null) {
            getRenderManager().addMissingDisplayList(GENERIC_POS_LIST);
            getRenderManager().clearDisplayList(GENERIC_POS_LIST);
        }

        this.showPosition = vec;

        if (vec != null) {
            float baseX = vec.getFloatX(bits);
            float baseY = vec.getFloatY(bits);
            float baseZ = vec.getFloatZ(bits);
            if (origin != null) {
                baseX += origin.getFloatX();
                baseY += origin.getFloatY();
                baseZ += origin.getFloatZ();
            }

            if (updateBox != null) {
                if (updateBox.getTransforms() != null)
                    for (Transform transform : updateBox.getTransforms()) {
                        if (!(transform instanceof Translate))
                            continue;

                        Translate translate = (Translate) transform;
                        translate.setX(baseX);
                        translate.setY(baseY);
                        translate.setZ(baseZ);
                    }
            } else {
                Box box = getRenderManager().addBoundingBoxFromMinMax(GENERIC_POS_LIST, baseX - GENERIC_POS_SIZE, baseY - GENERIC_POS_SIZE, baseZ - GENERIC_POS_SIZE, baseX + GENERIC_POS_SIZE, baseY + GENERIC_POS_SIZE, baseZ + GENERIC_POS_SIZE, GENERIC_POS_MATERIAL, true);
                applyRotation(box);
            }
        }
    }
}