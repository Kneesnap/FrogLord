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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.view.MOFMesh;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controls the MOF editor GUI.
 * TODO: Add Hilite highlighter.
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
    private RenderManager renderManager = new RenderManager();

    private static final String LIGHTING_LIST = "extraLighting";

    @Override
    public void onInit(AnchorPane editorRoot) {
        setupMofViewer(GUIMain.MAIN_STAGE);
    }

    @SneakyThrows
    private void setupMofViewer(Stage stageToOverride) {
        this.mofMesh = getFile().getMofMesh();
        this.uiController = new MOFUIController(this);

        // Create mesh view and initialise with xyz rotation transforms, materials and initial face culling policy.
        MeshView meshView = new MeshView(getMofMesh());

        this.rotX = new Rotate(0, Rotate.X_AXIS);
        this.rotY = new Rotate(0, Rotate.Y_AXIS);
        this.rotZ = new Rotate(0, Rotate.Z_AXIS);
        meshView.getTransforms().addAll(rotX, rotY, rotZ);

        meshView.setMaterial(getFile().getTextureMap().getDiffuseMaterial());
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
                getUiController().stopPlaying();
                getRenderManager().removeAllDisplayLists();
                Utils.setSceneKeepPosition(stageToOverride, defaultScene);
            }

            // Toggle wireframe mode.
            if (event.getCode() == KeyCode.X)
                meshView.setDrawMode(meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);
        });

        mofScene.setOnScroll(evt -> camera.setTranslateZ(camera.getTranslateZ() + (evt.getDeltaY() * .25)));

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
    }

    /**
     * Updates lighting settings for the model.
     * @param useBrightMode Should we apply the fancy lighting?
     */
    public void updateLighting(boolean useBrightMode) {
        getRenderManager().addMissingDisplayList(LIGHTING_LIST);
        getRenderManager().clearDisplayList(LIGHTING_LIST);

        AmbientLight ambLight = new AmbientLight();
        float colorValue = useBrightMode ? .2F : 1;
        ambLight.setColor(Color.color(colorValue, colorValue, colorValue));
        getRenderManager().addNode(LIGHTING_LIST, ambLight);

        if (useBrightMode) {
            PointLight pointLight1 = new PointLight();
            pointLight1.setColor(Color.color(0.9, 0.9, 0.9));
            pointLight1.setTranslateX(-100.0);
            pointLight1.setTranslateY(-100.0);
            pointLight1.setTranslateZ(-100.0);
            getRenderManager().addNode(LIGHTING_LIST, pointLight1);

            PointLight pointLight2 = new PointLight();
            pointLight2.setColor(Color.color(0.8, 0.8, 1.0));
            pointLight2.setTranslateX(100.0);
            pointLight2.setTranslateY(-100.0);
            pointLight2.setTranslateZ(-100.0);
            getRenderManager().addNode(LIGHTING_LIST, pointLight2);
        }
    }

    @Getter
    public static final class MOFUIController implements Initializable {
        private MOFHolder holder;
        private MOFController controller;

        // Baseline UI components
        @FXML private AnchorPane anchorPaneUIRoot;
        @FXML private Accordion accordionLeft;

        @FXML private Label modelName;
        @FXML private Button playButton;
        @FXML private CheckBox repeatCheckbox;
        @FXML private TextField fpsField;

        @FXML private Label frameLabel;
        @FXML private Button btnLast;
        @FXML private Button btnNext;

        @FXML private TitledPane paneAnim;
        @FXML private ComboBox<Integer> animationSelector;
        @FXML private CheckBox brightModeCheckbox;
        @FXML private CheckBox viewHilitesCheckbox;

        private List<Node> toggleNodes = new ArrayList<>();
        private List<Node> playNodes = new ArrayList<>();

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

            toggleNodes.addAll(Arrays.asList(repeatCheckbox, animationSelector, fpsField, frameLabel, btnNext, btnLast));
            playNodes.addAll(Arrays.asList(playButton, btnLast, frameLabel, btnNext));

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
        }

        @FXML
        private void nextFrame(ActionEvent evt) {
            getController().getMofMesh().nextFrame();
            updateFrameText();
        }

        @FXML
        private void lastFrame(ActionEvent evt) {
            getController().getMofMesh().previousFrame();
            updateFrameText();
        }

        /**
         * Sets the new animation id to use.
         * @param newAnimation The new animation to use.
         */
        public void setAnimation(int newAnimation) {
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
            frameLabel.setText("Frame " + getController().getMofMesh().getFrameCount());
        }

        /**
         * Start playing the MOF animation.
         */
        public void startPlaying(EventHandler<ActionEvent> onFinish) {
            stopPlaying();

            boolean repeat = repeatCheckbox.isSelected();
            if (!repeat) // Reset at frame zero when playing a non-paused mof.
                getMofMesh().resetFrame();

            this.animationPlaying = true;
            this.animationTimeline = new Timeline(new KeyFrame(Duration.millis(1000D / getFramesPerSecond()), evt -> {
                getMofMesh().nextFrame();
                updateFrameText();
            }));
            this.animationTimeline.setCycleCount(repeat ? Timeline.INDEFINITE : getMofMesh().getMofHolder().getMaxFrame(getMofMesh().getAction()) - 1);
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
}