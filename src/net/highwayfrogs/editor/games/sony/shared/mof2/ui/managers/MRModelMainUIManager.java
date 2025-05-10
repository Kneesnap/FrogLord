package net.highwayfrogs.editor.games.sony.shared.mof2.ui.managers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.AmbientLight;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import lombok.Getter;
import net.highwayfrogs.editor.file.config.NameBank;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofModelSet;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMofXarAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.flipbook.MRMofFlipbookAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.texture.MRMofTextureAnimation;
import net.highwayfrogs.editor.games.sony.shared.mof2.collision.MRMofBoundingBox;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRMofPart;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.MRModelMeshController;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelAnimationPlayer;
import net.highwayfrogs.editor.games.sony.shared.mof2.ui.mesh.MRModelMesh;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.editor.DisplayList;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewFrameTimer.MeshViewFixedFrameRateTimerTask;
import net.highwayfrogs.editor.gui.editor.SelectionPromptTracker;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages the main UI controls for the MRModelMesh.
 * Created by Kneesnap on 5/8/2025.
 */
@Getter
public class MRModelMainUIManager extends MeshUIManager<MRModelMesh> {
    private final MRModelMainUIComponent uiComponent;
    private final SelectionPromptTracker<SVector> vertexSelector; // TODO: Implement these.
    private final SelectionPromptTracker<MRMofPart> partSelector;
    private final DisplayList boundingBoxes;
    private final DisplayList lights;
    private int framesPerSecond = -1;
    private MeshViewFixedFrameRateTimerTask<MRModelMainUIComponent> animationTask;

    private static final PhongMaterial BBOX_MATERIAL = Scene3DUtils.makeUnlitSharpMaterial(Color.RED);

    public MRModelMainUIManager(MRModelMeshController controller) {
        super(controller);
        this.uiComponent = MRModelMainUIComponent.loadComponent(this);
        this.vertexSelector = new SelectionPromptTracker<>(this, true);
        this.partSelector = new SelectionPromptTracker<>(this, true);
        this.boundingBoxes = controller.getRenderManager().createDisplayList();
        this.lights = controller.getRenderManager().createDisplayList();
    }

    @Override
    public SCGameInstance getGameInstance() {
        return (SCGameInstance) super.getGameInstance();
    }

    @Override
    public MRModelMeshController getController() {
        return (MRModelMeshController) super.getController();
    }

    @Override
    public void onSetup() {
        super.onSetup();
        this.uiComponent.setupModel(getMesh().getModel());
        getController().getAccordionLeft().getPanes().add(this.uiComponent.getRootNode());
        getController().getInputManager().addKeyListener(KeyCode.LEFT, this::handleArrowKeyPress);
        getController().getInputManager().addKeyListener(KeyCode.RIGHT, this::handleArrowKeyPress);
        setFrameRate(getGameInstance().getFPS());
    }

    public void setBrightMode(boolean brightMode) {
        // Setup bright mode lighting.
        if (this.lights.isEmpty()) {
            this.lights.getNodes().add(new AmbientLight(Color.color(.2F, .2F, .2F)));

            PointLight pointLight1 = new PointLight();
            pointLight1.setColor(Color.color(0.9, 0.9, 0.9));
            pointLight1.setTranslateX(-100.0);
            pointLight1.setTranslateY(-100.0);
            pointLight1.setTranslateZ(-100.0);
            this.lights.add(pointLight1);

            PointLight pointLight2 = new PointLight();
            pointLight2.setColor(Color.color(0.8, 0.8, 1.0));
            pointLight2.setTranslateX(100.0);
            pointLight2.setTranslateY(-100.0);
            pointLight2.setTranslateZ(-100.0);
            this.lights.add(pointLight2);
        }

        getController().getMeshView().setMaterial(brightMode ? getMesh().getLitMaterial() : getMesh().getMaterial()); // Control if lighting is applied.
        for (int i = 0; i < this.lights.size(); i++)
            ((LightBase) this.lights.getNodes().get(i)).setLightOn(brightMode);
    }

    private void handleArrowKeyPress(InputManager manager, KeyEvent event) {
        if (getAnimationPlayer().isAnimationPlaying() || this.vertexSelector.isPromptActive() || this.partSelector.isPromptActive())
            return;

        if (event.getCode() == KeyCode.LEFT) {
            getAnimationPlayer().applyPreviousAnimationTick();
            this.uiComponent.updateFrameText();
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            getAnimationPlayer().applyNextAnimationTick();
            this.uiComponent.updateFrameText();
            event.consume();
        }
    }

    /**
     * Sets the animation frame-rate.
     * @param framesPerSecond the number of animation ticks/frames to advance per second.
     */
    public void setFrameRate(int framesPerSecond) {
        if (framesPerSecond <= 0)
            throw new IllegalArgumentException("framesPerSecond must be a positive non-zero number! (Was: " + framesPerSecond + ")");
        if (this.framesPerSecond == framesPerSecond)
            return;

        if (this.animationTask != null) {
            this.animationTask.cancel();
            this.animationTask = null;
        }

        this.framesPerSecond = framesPerSecond;
        this.animationTask = getController().getFrameTimer().getOrCreateTimer(framesPerSecond).addTask(1, this::tickAnimationTask);
        if (this.uiComponent.getFpsField() != null)
            this.uiComponent.getFpsField().setText(String.valueOf(framesPerSecond));
    }

    private void tickAnimationTask(MeshViewFixedFrameRateTimerTask<?> task) {
        MRModelAnimationPlayer player = getAnimationPlayer();
        if (!player.isAnimationPlaying())
            return;

        if (!this.uiComponent.getRepeatCheckbox().isSelected() && player.getAnimationTick() + 1 >= this.uiComponent.getMaxFrame()) {
            this.uiComponent.toggleAnimationPlayback();
            return; // Reached end.
        }

        player.setAnimationTick(player.getAnimationTick() + task.getDeltaFrames());
    }

    /**
     * Gets the animation player.
     */
    public MRModelAnimationPlayer getAnimationPlayer() {
        return getMesh().getAnimationPlayer();
    }

    /**
     * Adds a MRMofBoundingBox to the view.
     * @param box The box to add.
     */
    public Box addMOFBoundingBox(MRMofBoundingBox box, PhongMaterial material) {
        SVector min = box.getVertices()[0];
        SVector max = box.getVertices()[7];
        Box boxNode = this.boundingBoxes.addBoundingBoxFromMinMax(min.getFloatX(), min.getFloatY(), min.getFloatZ(), max.getFloatX(), max.getFloatY(), max.getFloatZ(), material, true);
        getController().getRotationManager().applyRotation(boxNode);
        return boxNode;
    }

    /**
     * Update bounding boxes.
     * @param showBoxes Should show hilite boxes.
     */
    public void updateBoundingBoxes(boolean showBoxes) {
        this.boundingBoxes.clear();
        if (!showBoxes)
            return;

        MRStaticMof staticMof = getController().getActiveStaticMof();
        if (staticMof != null)
            for (MRMofPart part : staticMof.getParts())
                addMOFBoundingBox(part.makeBoundingBox(), BBOX_MATERIAL);

        MRAnimatedMof animatedMof = getMesh().getModel().getAnimatedMof();
        if (animatedMof != null)
            addMOFBoundingBox(animatedMof.makeBoundingBox(), BBOX_MATERIAL);
    }

    @Getter
    public static class MRModelMainUIComponent extends GameUIController<SCGameInstance> {
        private final MRModelMainUIManager uiManager;

        @FXML private Label modelName;
        @FXML private ComboBox<Integer> animationSelector;
        @FXML private ComboBox<Integer> partHideSelector;
        @FXML private ColorPicker colorPicker;
        @FXML private Button playButton;
        @FXML private CheckBox repeatCheckbox;
        @FXML private TextField fpsField;
        @FXML private CheckBox textureAnimationCheckbox;

        @FXML private Slider frameSlider;
        @FXML private Label frameLabel;

        @FXML private TitledPane paneAnim;
        @FXML private ComboBox<MRAnimatedMofModel> animatedModelComboBox;
        @FXML private CheckBox brightModeCheckbox;
        @FXML private CheckBox viewBoundingBoxesCheckbox;

        private final List<Node> toggleNodes = new ArrayList<>();
        private final List<Node> playNodes = new ArrayList<>();

        private static final int SELECTION_SHOW_DEFAULT = -2;
        private static final int SELECTION_SHOW_ALL = -1;

        public MRModelMainUIComponent(MRModelMainUIManager uiManager) {
            super(uiManager.getGameInstance());
            this.uiManager = uiManager;
        }

        @Override
        public TitledPane getRootNode() {
            return (TitledPane) super.getRootNode();
        }

        @Override
        protected void onControllerLoad(Node rootNode) {
            this.brightModeCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> this.uiManager.setBrightMode(newValue)));
            this.viewBoundingBoxesCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> this.uiManager.updateBoundingBoxes(newValue)));

            /*mofScene.setOnMouseClicked(evt -> {
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

                if (this.hiliteOverlay != null && this.getUiController().getViewHilitesCheckbox().isSelected()) {
                    for (MOFPart part : getFile().asStaticFile().getParts()) {
                        for (MOFHilite hilite : part.getHilites()) {
                            if (hilite.getAttachType() == HiliteAttachType.PRIM && hilite.getPolygon() == clickedPoly) {
                                hilite.setupEditor(this);
                                return;
                            }
                        }
                    }
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
            });*/ // TODO: IMPLEMENT SELECTORS.

            // Setup frame slider.
            this.frameSlider.setMin(0);
            this.frameSlider.setBlockIncrement(1);
            this.frameSlider.setMinorTickCount(1);
            this.frameSlider.setSnapToTicks(true);
            this.frameSlider.setShowTickLabels(false);
            this.frameSlider.setShowTickMarks(true);
            this.frameSlider.valueProperty().addListener(((observable, oldValue, newValue) -> {
                MRModelAnimationPlayer animationPlayer = getAnimationPlayer();
                if (!animationPlayer.isAnimationPlaying()) { // If an animation is playing, this is handled by the animation.
                    animationPlayer.setAnimationTick((int) (double) newValue);
                    updateFrameText();
                }
            }));

            this.toggleNodes.addAll(Arrays.asList(this.repeatCheckbox, this.animationSelector, this.fpsField, this.frameLabel, this.frameSlider, this.textureAnimationCheckbox));
            this.playNodes.addAll(Arrays.asList(this.playButton, this.frameSlider, this.frameLabel));

            this.playButton.setOnAction(evt -> toggleAnimationPlayback());

            FXUtils.setHandleKeyPress(this.fpsField, newString -> {
                if (!NumberUtils.isInteger(newString))
                    return false;

                int newFps = Integer.parseInt(newString);
                if (newFps < 0)
                    return false;

                this.uiManager.setFrameRate(newFps);
                return true;
            }, null);
        }

        private void toggleAnimationPlayback() {
            MRModelAnimationPlayer player = getAnimationPlayer();
            boolean newState = !player.isAnimationPlaying();
            this.playButton.setText(newState ? "Stop" : "Play");
            for (Node node : getToggleNodes())
                node.setDisable(newState); // Set the toggle state of nodes.

            player.setAnimationPlaying(newState);
            player.setAnimationTick(0); // Start playing at frame zero, or reset back to frame zero.
            updateFrameText();
        }

        private void setupMeshBasedUI() {
            if ((this.animatedModelComboBox.getItems() != null && this.animatedModelComboBox.getItems().size() > 0) || this.animatedModelComboBox.isDisabled())
                return;

            // Setup default texture animation.
            getAnimationPlayer().setTextureAnimationEnabled(this.textureAnimationCheckbox.isSelected());
            this.textureAnimationCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> getAnimationPlayer().setTextureAnimationEnabled(newValue)));

            // Setup model/modelSet selector.
            MRAnimatedMofModel model = getController().getActiveAnimatedMofModel();
            if (model != null) {
                List<MRAnimatedMofModel> models = new ArrayList<>();
                for (MRAnimatedMofModelSet modelSet : model.getParentModelSet().getParentMof().getModelSets())
                    models.addAll(modelSet.getModels());

                this.animatedModelComboBox.setConverter(new AbstractStringConverter<>(safeModel -> "Model #" + (safeModel.getStaticModelID() + 1) + " (Set #" + (safeModel.getParentModelSet().getModelSetIndex() + 1) + ")"));
                this.animatedModelComboBox.setItems(FXCollections.observableArrayList(models));
                this.animatedModelComboBox.setValue(model);
                this.animatedModelComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    if (oldValue != newValue)
                        getController().getMesh().setActiveMofModel(newValue, getController());
                });
            } else {
                this.animatedModelComboBox.setDisable(true);
            }
        }

        /**
         * Sets the MOF Holder this controls.
         */
        public void setupModel(MRModel model) {
            setupMeshBasedUI();

            // Setup animation control.
            List<Integer> numbers = new ArrayList<>(Utils.getIntegerList(getAnimationListCount()));
            numbers.add(0, -1);
            this.animationSelector.setItems(FXCollections.observableArrayList(numbers));
            this.animationSelector.setConverter(new AbstractStringConverter<>(this::getAnimationListName));
            this.animationSelector.valueProperty().addListener(((observable, oldValue, newValue) -> {
                if (newValue != null)
                    setAnimation(newValue);
            }));

            this.animationSelector.getSelectionModel().select(0); // Automatically selects no animation.

            MRStaticMof staticMof = getController().getActiveStaticMof();
            List<Integer> parts = new ArrayList<>(Utils.getIntegerList(staticMof != null ? staticMof.getParts().size() : 0));
            parts.add(0, SELECTION_SHOW_ALL);
            parts.add(0, SELECTION_SHOW_DEFAULT);

            boolean firstTime = (this.partHideSelector.getItems() == null || this.partHideSelector.getItems().isEmpty());
            this.partHideSelector.setDisable(true);
            this.partHideSelector.setItems(FXCollections.observableArrayList(parts));
            this.partHideSelector.setConverter(new AbstractStringConverter<>(this::getPartUIName));
            this.partHideSelector.setDisable(false);
            if (firstTime) {
                this.partHideSelector.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
                    if (!this.partHideSelector.isDisable())
                        onUISelectPart(newValue);
                }));
                this.partHideSelector.getSelectionModel().select(0); // Select hide some parts.
            }

            boolean disableState = staticMof == null || staticMof.getTextureAnimationCount() == 0;
            for (Node node : this.playNodes)
                node.setDisable(disableState); // Disable playing non-existing animation.

            updateTempUI();
            this.modelName.setText(model != null ? model.getFileDisplayName() : null);
        }

        /**
         * Gets the number of animations in this mof. (Does not include texture animation).
         * Get the maximum animation action id.
         * @return maxAnimation
         */
        public int getAnimationListCount() {
            MRAnimatedMofModel animatedMof = getController().getActiveAnimatedMofModel();
            MRStaticMof staticMof = getController().getActiveStaticMof();
            int textureAnimations = animatedMof != null ? animatedMof.getParentModelSet().getCelSet().getAnimations().size() : 0;
            int flipbookAnimations = staticMof != null ? staticMof.getFlipbookAnimationCount() : 0;
            return textureAnimations + flipbookAnimations;
        }

        /**
         * Gets the name of a particular animation list ID, if there is one.
         * @param animationId The animation ID to get.
         * @return name
         */
        private String getAnimationListName(int animationId) {
            if (animationId == -1) {
                MRStaticMof staticMof = getController().getActiveStaticMof();
                return staticMof != null && staticMof.getTextureAnimationCount() > 0 ? "Texture Animation" : "No Animation";
            }

            NameBank bank = getGameInstance().getVersionConfig().getAnimationBank();
            if (bank == null)
                return (animationId != 0) ? "Animation " + animationId : "Default Animation";

            String bankName = SCUtils.stripWin95(FileUtils.stripExtension(getController().getModel().getFileDisplayName()));
            NameBank childBank = bank.getChildBank(bankName);
            return childBank != null ? childBank.getName(animationId) : bank.getEmptyChildNameFor(animationId, getAnimationListCount());
        }

        private String getPartUIName(int partId) {
            if (partId == SELECTION_SHOW_DEFAULT)
                return "Default";
            if (partId == SELECTION_SHOW_ALL)
                return "Show All Parts";

            return "Part #" + partId;
        }

        /**
         * Updates the MOF display when the UI selects a new option.
         * @param partId The part ID to display.
         */
        private void onUISelectPart(int partId) {
            MRModelMesh mesh = getController().getMesh();
            MRStaticMof staticMof = getController().getActiveStaticMof();
            if (staticMof == null) {
                mesh.getHiddenParts().clear();
                mesh.updateMofMesh(null); // We don't want to update the UI.
                return;
            }

            if (partId == SELECTION_SHOW_DEFAULT) {
                mesh.getHiddenParts().clear();
                for (MRMofPart part : staticMof.getParts())
                    if (part.isHiddenByConfiguration())
                        mesh.getHiddenParts().add(part);
            } else if (partId == SELECTION_SHOW_ALL) {
                mesh.getHiddenParts().clear();
            } else if (partId >= 0 && partId < staticMof.getParts().size()) {
                MRMofPart part = staticMof.getParts().get(partId);
                if (!mesh.getHiddenParts().remove(part))
                    mesh.getHiddenParts().add(part);
            } else {
                throw new RuntimeException("UI Error, Unexpected MOF Part ID: " + partId);
            }

            mesh.updateMofMesh(null); // We don't want to update the UI.
        }

        /**
         * Gets the ModelMeshController for the scene.
         */
        public MRModelMeshController getController() {
            return this.uiManager.getController();
        }

        /**
         * Gets the animation player.
         */
        public MRModelAnimationPlayer getAnimationPlayer() {
            return this.uiManager.getAnimationPlayer();
        }

        /**
         * Sets the new animation id to use.
         * @param newAnimationId The new animation to use, using IDs as shown in the list.
         */
        public void setAnimation(int newAnimationId) {
            if (this.uiManager.vertexSelector.isPromptActive() || this.uiManager.partSelector.isPromptActive())
                return;

            if (newAnimationId < 0) {
                getAnimationPlayer().setNoActiveAnimation();
                MRStaticMof staticMof = getController().getActiveStaticMof();
                for (Node node : this.playNodes)
                    node.setDisable(staticMof == null || staticMof.getTextureAnimationCount() == 0); // Disable playing non-existing animation.

                return;
            }

            MRAnimatedMofModel animatedMof = getController().getActiveAnimatedMofModel();
            int xarAnimations = animatedMof != null ? animatedMof.getParentModelSet().getCelSet().getAnimations().size() : 0;

            if (xarAnimations > newAnimationId) { // XAR
                getAnimationPlayer().setXarAnimationID(newAnimationId, true);
            } else {
                int flipbookAnimationId = newAnimationId - xarAnimations;
                getAnimationPlayer().setFlipbookAnimationID(flipbookAnimationId, true);
            }

            updateFrameText();

            // Toggle UI controls for playing.
            for (Node node : this.playNodes)
                node.setDisable(false); // Disable playing non-existing animation.
        }

        /**
         * A very quick and dirty (and temporary!) UI. Will be replaced...
         */
        public void updateTempUI() {
            getController().getAnchorPaneUIRoot().requestFocus();
            this.paneAnim.setExpanded(true);
            getController().getAccordionLeft().setExpandedPane(this.paneAnim);
            this.paneAnim.requestFocus();
            this.fpsField.setText(String.valueOf(this.uiManager.getFramesPerSecond()));
            updateFrameText();
        }

        private void updateFrameText() {
            int animationTick = getAnimationPlayer().getAnimationTick();
            int frameCount = getMaxFrame();

            int currentFrame = animationTick % frameCount;
            if (currentFrame < 0)
                currentFrame += frameCount;

            this.frameLabel.setText("Frame " + currentFrame);
            this.frameSlider.setMax(frameCount - 1);
            this.frameSlider.setValue(currentFrame);
        }

        /**
         * Get the animation's frame count.
         * @return frameCount
         */
        private int getMaxFrame() {
            MRStaticMof staticMof = getController().getActiveStaticMof();
            int maxFrame = 1;

            // Texture animations.
            if (staticMof != null && this.textureAnimationCheckbox.isSelected()) {
                for (MRMofPart mofPart : staticMof.getParts()) {
                    for (MRMofTextureAnimation anim : mofPart.getTextureAnimations()) {
                        int frameCount = anim.getTotalFrameCount();
                        if (frameCount > maxFrame)
                            maxFrame = frameCount;
                    }
                }
            }

            // XAR animation.
            MRAnimatedMofXarAnimation xarAnimation = getAnimationPlayer().getXarAnimation();
            if (xarAnimation != null) {
                int frameCount = xarAnimation.getFrameCount();
                if (frameCount > maxFrame)
                    maxFrame = frameCount;
            }

            // Flipbook animation.
            MRMofFlipbookAnimation flipbookAnimation = getAnimationPlayer().getFlipbookAnimation();
            if (flipbookAnimation != null) {
                int frameCount = flipbookAnimation.getFrameCount();
                if (frameCount > maxFrame)
                    maxFrame = frameCount;
            }

            return maxFrame;
        }

        /**
         * Loads the component for a given manager.
         * @param manager the manager to load the component for
         * @return newMainUIComponent
         */
        public static MRModelMainUIComponent loadComponent(MRModelMainUIManager manager) {
            if (manager == null)
                throw new NullPointerException("manager");

            GameInstance instance = manager.getGameInstance();
            FXMLLoader fxmlTemplateLoader = FXUtils.getFXMLTemplateLoader(instance, "mof-main-pane");

            // Load fxml data.
            MRModelMainUIComponent controller = new MRModelMainUIComponent(manager);
            if (GameUIController.loadController(instance, fxmlTemplateLoader, controller) == null)
                throw new RuntimeException("Failed to load controller!");

            return controller;
        }
    }
}
