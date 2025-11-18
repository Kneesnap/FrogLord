package net.highwayfrogs.editor.gui.editor;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.editor.DisplayList.RenderListManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.gui.mesh.MeshTracker;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Scene3DUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.fx.wrapper.FXFixedMouseSplitPaneSkin;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the UI which is displayed when a mesh is viewed.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public abstract class MeshViewController<TMesh extends DynamicMesh> implements Initializable {
    // Useful constants and settings
    public static final double MAP_VIEW_NEAR_CLIP = 1; // The closer this value is to 0.0, the worse z-fighting gets. Source: https://www.khronos.org/opengl/wiki/Depth_Buffer_Precision
    public static final double MAP_VIEW_FAR_CLIP = 2000.0;
    public static final double MAP_VIEW_FOV = 60.0;
    private static final int SPLIT_DIVIDER_SIZE = 10;

    private final GameInstance gameInstance;
    private SubScene subScene;
    private final Map<MeshView, ChangeListener<DrawMode>> meshViewDrawModeListeners = new HashMap<>();

    // Baseline UI components
    @FXML private AnchorPane anchorPaneUIRoot;
    @FXML private Accordion accordionLeft;

    // Control Settings Pane.
    @FXML private TitledPane cameraSettings;
    @FXML private GridPane cameraSettingsPane;
    @FXML private Label meshNameLabel;
    @FXML private CheckBox checkBoxShowAxis;
    @FXML private CheckBox checkBoxShowMesh;
    @FXML private ComboBox<DrawMode> comboBoxMeshDrawMode;
    @FXML private ComboBox<CullFace> comboBoxMeshCullFace;
    @FXML private CheckBox checkBoxEnablePsxShading;
    @FXML private Label textureSheetDebugLabel;
    @FXML private ImageView textureSheetDebugView;
    @FXML private ColorPicker colorPickerLevelBackground;
    @FXML private TextField textFieldCamMoveSpeed;
    @FXML private Button btnResetCamMoveSpeed;
    @FXML private TextField textFieldCamMouseSpeed;
    @FXML private Button btnResetCamMouseSpeed;
    @FXML private CheckBox checkBoxYInvert;
    @FXML private TextField textFieldCamSpeedDownMultiplier;
    @FXML private Button btnResetCamSpeedDownMultiplier;
    @FXML private TextField textFieldCamSpeedUpMultiplier;
    @FXML private Button btnResetCamSpeedUpMultiplier;
    @FXML private TextField textFieldCamNearClip;
    @FXML private TextField textFieldCamFarClip;
    @FXML private TextField textFieldCamFoV;
    @FXML private TextField textFieldCamPosX;
    @FXML private TextField textFieldCamPosY;
    @FXML private TextField textFieldCamPosZ;
    @FXML private TextField textFieldCamYaw;
    @FXML private TextField textFieldCamPitch;
    @FXML private TextField textFieldCamRoll;

    // View Settings
    @FXML private TitledPane viewSettings;
    @FXML private ScrollPane viewScrollPane;
    @FXML private VBox viewSettingsBox;
    @FXML private GridPane viewSettingsPane;

    // Managers:
    private MeshUIMarkerManager<TMesh> markerManager;
    private final List<MeshUIManager<TMesh>> managers = new ArrayList<>();
    private final Map<Class<? extends MeshUIManager<TMesh>>, MeshUIManager<TMesh>> managersByType = new HashMap<>();
    private final List<MeshUISelector<TMesh, ?>> selectors = new ArrayList<>();
    private final RenderListManager renderManager = new RenderListManager(new Group());
    private final RenderListManager transparentRenderManager = new RenderListManager(new Group());
    private final MeshViewFrameTimer frameTimer = new MeshViewFrameTimer(this);
    private final InputManager inputManager;
    private final FirstPersonCamera firstPersonCamera;

    // Instance Data:
    private final MeshTracker meshTracker = new MeshTracker();
    private TMesh mesh;
    private DisplayList axisDisplayList;

    // Mesh Rendering:
    private MeshView meshView;
    private AnchorPane root2D;
    private Group root3D;
    private Scene meshScene;
    private Scene originalScene;
    private Stage overwrittenStage;
    private final Group lightingGroup = new Group();
    private AmbientLight mainLight;

    private static final NumberStringConverter NUM_TO_STRING_CONVERTER = new NumberStringConverter(new DecimalFormat("####0.000000"));
    private static final FXMLLoader BASIC_MESH_VIEW_FXML_LOADER = new FXMLLoader(MeshViewController.class.getResource("/fxml/scene-basic-3d.fxml"));

    protected MeshViewController(GameInstance instance) {
        this.gameInstance = instance;
        this.inputManager = new InputManager(instance);
        this.firstPersonCamera = new FirstPersonCamera(this.inputManager);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Do nothing for now.
    }

    /**
     * Get the logger for this controller.
     */
    public ILogger getLogger() {
        return ClassNameLogger.getLogger(getGameInstance(), getClass());
    }

    /**
     * Creates a side panel.
     * @param title The title of the side panel.
     * @return newSidePanel
     */
    public UISidePanel createSidePanel(String title) {
        return createSidePanel(title, false);
    }

    /**
     * Creates a side panel.
     * @param title The title of the side panel.
     * @return newSidePanel
     */
    public UISidePanel createSidePanel(String title, boolean insertFirst) {
        UISidePanel sidePanel = new UISidePanel(this.accordionLeft, insertFirst, title);
        sidePanel.setVisible(true);
        return sidePanel;
    }

    /**
     * Sets up the managers.
     */
    protected abstract void setupManagers();

    /**
     * Register a mesh ui manager.
     * @param manager        The manager to register.
     * @param <TMeshManager> The type of manager.
     */
    @SuppressWarnings("unchecked")
    public <TMeshManager extends MeshUIManager<TMesh>> void addManager(TMeshManager manager) {
        if (manager == null)
            throw new IllegalArgumentException("manager cannot be null");

        Class<TMeshManager> managerClass = (Class<TMeshManager>) manager.getClass();
        if (this.managersByType.containsKey(managerClass))
            throw new IllegalStateException("The manager class '" + Utils.getSimpleName(manager) + "' is already registered.");

        this.managersByType.put(managerClass, manager);
        this.managers.add(manager);
    }

    /**
     * Gets the manager registered for a particular manager class.
     * An exception is thrown if no such manager exists.
     * @param managerClass   The class of the manager to get.
     * @param <TMeshManager> The expected manager type to receive.
     * @return mesh ui manager instance
     */
    public <TMeshManager extends MeshUIManager<TMesh>> TMeshManager getRequiredManager(Class<TMeshManager> managerClass) {
        TMeshManager manager = getManager(managerClass);
        if (manager == null)
            throw new RuntimeException("There is no mesh manager instance registered of type " + Utils.getSimpleName(managerClass) + ".");

        return manager;
    }

    /**
     * Gets the manager registered for a particular manager class.
     * @param managerClass   The class to get the instance of.
     * @param <TMeshManager> The return type expected.
     * @return The mesh manager instance, or null if no such manager is registered.
     */
    @SuppressWarnings("unchecked")
    public <TMeshManager extends MeshUIManager<TMesh>> TMeshManager getManager(Class<TMeshManager> managerClass) {
        return (TMeshManager) this.managersByType.get(managerClass);
    }

    /**
     * Returns the camera used for this scene.
     */
    public PerspectiveCamera getCamera() {
        return this.firstPersonCamera.getCamera();
    }

    /**
     * Returns true if the scene is using the default camera scheme.
     */
    public boolean isDefaultCamera() {
        return this.firstPersonCamera.getCamera() == getCamera();
    }

    /**
     * Sets up the mesh controller.
     * @param dynamicMesh     The dynamic mesh which the controller displays.
     * @param stageToOverride The stage to replace with a 3D mesh view.
     * @param loadRoot        The root of the data loaded from the template. (Which we're going to put into the left accordion)
     */
    public void setupController(TMesh dynamicMesh, Stage stageToOverride, Parent loadRoot) {
        this.overwrittenStage = stageToOverride;
        this.mesh = dynamicMesh;

        // Setup MeshView
        this.meshView = new MeshView();
        this.meshView.setCullFace(CullFace.BACK);
        dynamicMesh.addView(this.meshView, this.meshTracker);

        // Create the 3D elements and use them within a subscene.
        this.root3D = new Group();
        if (!isDefaultCamera())
            this.root3D.getChildren().add(getCamera());

        // Setup subScene.
        SubScene subScene3D = new SubScene(this.root3D, stageToOverride.getScene().getWidth() - uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.DISABLED);
        subScene3D.setFill(Color.BLACK);
        subScene3D.setCamera(getCamera());
        subScene3D.setManaged(false); // Prevents the SubScene from impacting its parent node size.

        // Ensure that the render manager has access to the root node
        this.root3D.getChildren().add(this.renderManager.getRoot());
        this.root3D.getChildren().add(this.transparentRenderManager.getRoot());

        // Using a BorderPane attempts to size it properly.
        BorderPane borderPane3D = new BorderPane();
        borderPane3D.setCenter(subScene3D);

        // Initialise the UI layout.
        SplitPane splitPane = new SplitPane();
        splitPane.setSkin(new FXFixedMouseSplitPaneSkin(splitPane)); // Prevents mouse events from getting eaten.
        SplitPane.setResizableWithParent(loadRoot, false);
        SplitPane.setResizableWithParent(borderPane3D, true);
        splitPane.setDividerPositions(.2);
        splitPane.getItems().addAll(loadRoot, borderPane3D);

        // Setup the root node. (So we can also add 2D elements above the 3D space.)
        this.root2D = new AnchorPane();
        GameUIController.setAnchorPaneStretch(splitPane);
        this.root2D.getChildren().add(splitPane);

        // Create and set the scene with antialiasing.
        this.meshScene = new Scene(this.root2D, subScene3D.getWidth(), subScene3D.getHeight(), true, SceneAntialiasing.DISABLED);
        this.originalScene = FXUtils.setSceneKeepPosition(stageToOverride, this.meshScene);

        // Handle scaling of SubScene on stage resizing.
        subScene3D.widthProperty().bind(borderPane3D.widthProperty());
        subScene3D.heightProperty().bind(borderPane3D.heightProperty());

        // Associate camera controls with the scene.
        this.inputManager.assignSceneControls(this.meshScene);
        this.inputManager.setStage(stageToOverride);
        if (isDefaultCamera())
            this.firstPersonCamera.startThreadProcessing();

        this.inputManager.setFinalKeyHandler((manager, event) -> {
            if (onKeyPress(event) || event.getEventType() != KeyEvent.KEY_PRESSED)
                return; // Handled by the other controller.

            if (event.getCode() == KeyCode.ESCAPE) { // Exit the viewer.
                shutdownMeshViewer();
            } else if (event.getCode() == KeyCode.F8) { // Print mesh information.
                getMesh().printDebugMeshInfo();
            } else if (event.getCode() == KeyCode.F9) { // 3D screenshot.
                Scene3DUtils.take3DScreenshot(getGameInstance(), getLogger(), this.mesh, this.mesh.getMeshName());
            } else if (event.getCode() == KeyCode.F10) { // Take screenshot.
                Scene3DUtils.takeScreenshot(getGameInstance(), this.subScene, FileUtils.stripExtension(getMeshDisplayName()), false);
            } else if (event.getCode() == KeyCode.F12 && getMesh().getTextureAtlas() != null) {

                if (getMesh().getTextureAtlas().getTextureSource().isEnableAwtImage()) {
                    getLogger().info("Saving main mesh texture sheet to 'texture-sheet-awt.png'...");
                    try {
                        ImageIO.write(getMesh().getTextureAtlas().getImage(), "png", new File(getGameInstance().getMainGameFolder(), "texture-sheet-awt.png"));
                    } catch (IOException ex) {
                        FXUtils.makeErrorPopUp("Failed to save 'texture-sheet-awt.png'.", ex, true);
                    }
                }

                if (getMesh().getTextureAtlas().getTextureSource().isEnableFxImage()) {
                    getLogger().info("Saving main mesh texture sheet to 'texture-sheet-fx.png'...");
                    try {
                        ImageIO.write(SwingFXUtils.fromFXImage(getMesh().getTextureAtlas().getFxImage(), null), "png", new File(getGameInstance().getMainGameFolder(), "texture-sheet-fx.png"));
                    } catch (IOException ex) {
                        FXUtils.makeErrorPopUp("Failed to save 'texture-sheet-fx.png'.", ex, true);
                    }
                }
            } else if ((event.isControlDown() && event.getCode() == KeyCode.ENTER)) { // Toggle full-screen.
                this.overwrittenStage.setFullScreen(!this.overwrittenStage.isFullScreen());
            } else if (event.getCode() == KeyCode.X) { // Toggle wireframe.
                this.meshView.setDrawMode(this.meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);
            }
        });

        // Setup managers.
        this.managers.clear();
        List<TitledPane> startingPanes = new ArrayList<>(this.accordionLeft.getPanes());
        if (!isDefaultCamera())
            startingPanes.remove(this.cameraSettings);

        this.accordionLeft.getPanes().clear();
        addManager(this.markerManager = new MeshUIMarkerManager<>(this));
        setupManagers();

        setDefaultCameraPosition();
        setupBasicLighting();

        // Ensure that any transparent parts of the map show 3D models behind it.
        if (mapRendersFirst())
            this.renderManager.getRoot().getChildren().add(this.meshView);

        // Setup managers & UI.
        // Should run last since UI managers may use information from this class.
        setupBindings(subScene3D, this.meshView);

        // Ensure that any transparent parts of the map show 3D models behind it.
        if (!mapRendersFirst())
            this.transparentRenderManager.getRoot().getChildren().add(this.meshView);

        runForEachManager(MeshUIManager::setupNodesWhichRenderLast, "setupNodesWhichRenderLast"); // Setup all the nodes which render last.

        // Move default panes to the bottom. (Should happen after setup occurs.)
        this.accordionLeft.getPanes().addAll(startingPanes);

        // Select Manager
        if (this.accordionLeft.getPanes().size() > 0)
            this.accordionLeft.getPanes().get(0).setExpanded(true);

        // Start the task timer.
        this.frameTimer.start();
    }

    /**
     * Adds two nodes to the view settings grid.
     * @param leftNode the node to place in the left column
     * @param rightNode the node to place in the right column
     */
    protected void addToViewSettingsGrid(Node leftNode, Node rightNode) {
        int oldSize = getViewSettingsPane().getRowConstraints().size();
        RowConstraints example = getViewSettingsPane().getRowConstraints().get(0);
        getViewSettingsPane().getRowConstraints().add(new RowConstraints(example.getMinHeight(), example.getPrefHeight(), example.getMaxHeight(), example.getVgrow(), example.getValignment(), example.isFillHeight()));
        GridPane.setColumnIndex(leftNode, 0);
        GridPane.setColumnIndex(rightNode, 1);
        GridPane.setRowIndex(leftNode, oldSize);
        GridPane.setRowIndex(rightNode, oldSize);
        getViewSettingsPane().getChildren().add(leftNode);
        getViewSettingsPane().getChildren().add(rightNode);
    }

    /**
     * Adds a node to the view settings grid.
     * @param node the node to place
     */
    protected void addToViewSettingsGrid(Node node, HPos alignment) {
        RowConstraints example = getViewSettingsPane().getRowConstraints().get(0);
        getViewSettingsPane().getRowConstraints().add(new RowConstraints(example.getMinHeight(), example.getPrefHeight(), example.getMaxHeight(), example.getVgrow(), example.getValignment(), example.isFillHeight()));
        GridPane.setHalignment(node, alignment);
        GridPane.setColumnSpan(node, 2);
        GridPane.setColumnIndex(node, 0);
        GridPane.setRowIndex(node, getViewSettingsPane().getRowConstraints().size() - 1);
        getViewSettingsPane().getChildren().add(node);
    }

    /**
     * If this is true, it gives a preference to 3D models having transparency over the world.
     */
    protected boolean mapRendersFirst() {
        return false;
    }

    /**
     * Shuts down the mesh viewer.
     */
    public void shutdownMeshViewer() {
        // Stop camera processing and clear up the render manager
        this.textureSheetDebugView.imageProperty().unbind();
        if (isDefaultCamera())
            this.firstPersonCamera.stopThreadProcessing();
        this.inputManager.removeSceneControls(this.meshScene);
        this.inputManager.setStage(null);
        this.renderManager.removeAllDisplayLists();
        this.transparentRenderManager.removeAllDisplayLists();
        this.inputManager.shutdown();
        this.frameTimer.stop();

        // Clear selectors
        while (!this.selectors.isEmpty())
            this.selectors.get(this.selectors.size() - 1).cancel();

        // Call shutdown hook.
        try {
            onShutdown();
        } catch (Throwable th) {
            String errorMessage = "Encountered error in MeshViewController shutdown hook.";
            getLogger().throwing("MeshViewController", null, new RuntimeException(errorMessage, th));
        }

        this.meshTracker.disposeMeshes(); // Prevent memory leaks by ensuring texture sheets remove any listeners from static textures sources (which would then keep the texture sheet in memory).
        this.root3D.getChildren().clear(); // Clear data just in-case there's a memory leak.
        FXUtils.setSceneKeepPosition(this.overwrittenStage, this.originalScene);
    }

    /**
     * Called when the mesh view shuts down / exits.
     */
    protected void onShutdown() {
        // Run shutdown hooks.
        for (int i = 0; i < this.managers.size(); i++) {
            MeshUIManager<TMesh> manager = this.managers.get(i);
            try {
                manager.onRemove();
            } catch (Throwable th) {
                String errorMessage = "Encountered an error while running onRemove().";
                getLogger().throwing("MeshViewController", "onShutdown", new RuntimeException(errorMessage, th));
            }
        }
    }

    /**
     * Gets a display string (usually file name) which represents the mesh.
     */
    public abstract String getMeshDisplayName();

    /**
     * Applies the default camera position.
     */
    protected abstract void setDefaultCameraPosition();

    /**
     * Get the root pane width.
     */
    public double uiRootPaneWidth() {
        return anchorPaneUIRoot.getWidth();
    }

    /**
     * Get the root pane height.
     */
    public double uiRootPaneHeight() {
        return anchorPaneUIRoot.getHeight();
    }

    /**
     * Primary function (entry point) for setting up data / control bindings, etc.
     */
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        this.subScene = subScene3D;

        // Setup axis.
        setupAxis();

        // Setup camera.
        PerspectiveCamera camera = getCamera();
        camera.setNearClip(MAP_VIEW_NEAR_CLIP);
        camera.setFarClip(MAP_VIEW_FAR_CLIP);
        camera.setFieldOfView(MAP_VIEW_FOV);

        // Set informational bindings and editor bindings
        this.colorPickerLevelBackground.setValue((Color) this.subScene.getFill());
        this.subScene.fillProperty().bind(this.colorPickerLevelBackground.valueProperty());

        this.meshNameLabel.setText(getMeshDisplayName());
        this.textFieldCamMoveSpeed.textProperty().bindBidirectional(this.firstPersonCamera.getCamMoveSpeedProperty(), NUM_TO_STRING_CONVERTER);
        this.textFieldCamMouseSpeed.textProperty().bindBidirectional(this.firstPersonCamera.getCamMouseSpeedProperty(), NUM_TO_STRING_CONVERTER);
        this.textFieldCamSpeedDownMultiplier.textProperty().bindBidirectional(this.firstPersonCamera.getCamSpeedDownMultiplierProperty(), NUM_TO_STRING_CONVERTER);
        this.textFieldCamSpeedUpMultiplier.textProperty().bindBidirectional(this.firstPersonCamera.getCamSpeedUpMultiplierProperty(), NUM_TO_STRING_CONVERTER);
        this.checkBoxYInvert.selectedProperty().bindBidirectional(this.firstPersonCamera.getCamYInvertProperty());

        this.btnResetCamMoveSpeed.setOnAction(e -> this.firstPersonCamera.resetDefaultCamMoveSpeed());
        this.btnResetCamMouseSpeed.setOnAction(e -> this.firstPersonCamera.resetDefaultCamMouseSpeed());
        this.btnResetCamSpeedDownMultiplier.setOnAction(e -> this.firstPersonCamera.resetDefaultCamSpeedDownMultiplier());
        this.btnResetCamSpeedUpMultiplier.setOnAction(e -> this.firstPersonCamera.resetDefaultCamSpeedUpMultiplier());

        // Set camera bindings
        this.textFieldCamNearClip.textProperty().bindBidirectional(camera.nearClipProperty(), NUM_TO_STRING_CONVERTER);
        this.textFieldCamFarClip.textProperty().bindBidirectional(camera.farClipProperty(), NUM_TO_STRING_CONVERTER);
        this.textFieldCamFoV.textProperty().bindBidirectional(camera.fieldOfViewProperty(), NUM_TO_STRING_CONVERTER);

        this.textFieldCamPosX.textProperty().bindBidirectional(this.firstPersonCamera.getCamPosXProperty(), NUM_TO_STRING_CONVERTER);
        this.textFieldCamPosY.textProperty().bindBidirectional(this.firstPersonCamera.getCamPosYProperty(), NUM_TO_STRING_CONVERTER);
        this.textFieldCamPosZ.textProperty().bindBidirectional(this.firstPersonCamera.getCamPosZProperty(), NUM_TO_STRING_CONVERTER);

        this.textFieldCamYaw.textProperty().bind(this.firstPersonCamera.getCamYawProperty().asString("%.6f"));
        this.textFieldCamPitch.textProperty().bind(this.firstPersonCamera.getCamPitchProperty().asString("%.6f"));
        this.textFieldCamRoll.textProperty().bind(this.firstPersonCamera.getCamRollProperty().asString("%.6f"));

        // Set mesh view bindings
        this.comboBoxMeshDrawMode.getItems().setAll(DrawMode.values());
        this.comboBoxMeshDrawMode.setValue(DrawMode.FILL);
        this.comboBoxMeshCullFace.getItems().setAll(CullFace.values());
        this.comboBoxMeshCullFace.setValue(CullFace.BACK);
        bindMeshSceneControls(this, meshView);

        if (getMesh() instanceof IPSXShadedMesh) {
            IPSXShadedMesh shadedMesh = (IPSXShadedMesh) getMesh();
            this.checkBoxEnablePsxShading.setSelected(shadedMesh.isShadingEnabled());
            this.checkBoxEnablePsxShading.selectedProperty().addListener((observable, oldState, newState) -> ((IPSXShadedMesh) getMesh()).setShadingEnabled(newState));
        } else {
            // PSX Shading is not supported, so keep it disabled.
            this.checkBoxEnablePsxShading.setDisable(true);
        }

        if (getMesh().getMaterial() != null) {
            this.textureSheetDebugView.imageProperty().bind(getMesh().getMaterial().diffuseMapProperty());
        } else {
            this.viewSettingsBox.getChildren().removeAll(this.textureSheetDebugLabel, this.textureSheetDebugView);
        }

        // Must be called after FroggerMapInfoUIController is passed.
        runForEachManager(MeshUIManager::onSetup, "onSetup"); // Setup all the managers.
        runForEachManager(MeshUIManager::updateEditor, "updateEditor"); // Setup all the managers editors.
    }

    private void setupBasicLighting() {
        // There is no lighting on terrain, equivalent to a fully white ambient light.
        this.mainLight = new AmbientLight(Color.WHITE);
        getRoot3D().getChildren().add(this.mainLight);

        // Add lighting group.
        getRoot3D().getChildren().add(this.lightingGroup);

        // Add scope.
        this.mainLight.getScope().add(this.lightingGroup);
    }

    /**
     * Get the length of the axis display.
     */
    protected double getAxisDisplayLength() {
        return 10;
    }

    /**
     * Get the thickness of the axis display.
     */
    protected double getAxisDisplaySize() {
        return 3;
    }

    private void setupAxis() {
        final double axisLength = getAxisDisplayLength();
        final double lineSize = getAxisDisplaySize();

        this.axisDisplayList = getRenderManager().createDisplayListWithNewGroup();
        this.axisDisplayList.addLine(0, 0, 0, axisLength, 0, 0, lineSize, Scene3DUtils.makeUnlitSharpMaterial(Color.RED)); // X Axis.
        this.axisDisplayList.addLine(0, 0, 0, 0, axisLength, 0, lineSize, Scene3DUtils.makeUnlitSharpMaterial(Color.GREEN)); // Y Axis.
        this.axisDisplayList.addLine(0, 0, 0, 0, 0, axisLength, lineSize, Scene3DUtils.makeUnlitSharpMaterial(Color.BLUE)); // Z Axis.

        this.axisDisplayList.setVisible(this.checkBoxShowAxis.isSelected());
        this.checkBoxShowAxis.selectedProperty().addListener((listener, oldValue, newValue) -> this.axisDisplayList.setVisible(newValue));
        this.lightingGroup.getChildren().add(this.axisDisplayList.getRoot());
    }

    private void runForEachManager(Consumer<MeshUIManager<TMesh>> execution, String name) {
        for (MeshUIManager<TMesh> manager : getManagers()) {
            try {
                execution.accept(manager);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Failed to run '%s.%s'.", Utils.getSimpleName(manager), name);
            }
        }
    }

    /**
     * Set UI control focus to the subscene component (clearing the focus from the previously selected component).
     * (This is typically set as the target function for the 'onAction' event handler).
     */
    @FXML
    public void onActionClearFocus() {
        this.subScene.requestFocus();
    }

    /**
     * Handle when a key is pressed.
     * @param event The key event fired.
     * @return wasHandled
     */
    public boolean onKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE && !this.selectors.isEmpty()) {
            // First, attempt to cancel the active selector.
            for (int i = this.selectors.size() - 1; i >= 0; i--) {
                MeshUISelector<TMesh, ?> selector = this.selectors.get(i);
                if (selector.isActive()) {
                    selector.cancel();
                    event.consume();
                    return true;
                }
            }
        }

        for (MeshUIManager<TMesh> manager : getManagers()) { // Handle key presses.
            if (manager.onKeyPress(event)) {
                event.consume();
                return true;
            }
        }

        return false;
    }

    /**
     * Sets up the default inverse camera.
     */
    protected void setupDefaultInverseCamera() {
        setupDefaultInverseCamera(0, 0, 0, 25);
    }

    /**
     * Sets up the default inverse camera.
     */
    protected void setupDefaultInverseCamera(double xPos, double yPos, double zPos, double size) {
        getFirstPersonCamera().setInvertY(true);
        getFirstPersonCamera().setPos(xPos, yPos + size, zPos);
        getFirstPersonCamera().setCameraLookAt(xPos, yPos, zPos + size);
    }

    /**
     * Bind mesh scene controls to the provided MeshView.
     * @param controller the controller to bind the controls from
     * @param meshView the meshView to bind the controls to
     */
    public static void bindMeshSceneControls(MeshViewController<?> controller, MeshView meshView) {
        meshView.setVisible(meshView.visibleProperty().get());
        meshView.setDrawMode(controller.getComboBoxMeshDrawMode().getValue());
        meshView.setCullFace(controller.getComboBoxMeshCullFace().getValue());
        controller.getCheckBoxShowMesh().selectedProperty().bindBidirectional(meshView.visibleProperty());
        controller.getComboBoxMeshDrawMode().valueProperty().bindBidirectional(meshView.drawModeProperty());
        controller.getComboBoxMeshCullFace().valueProperty().bindBidirectional(meshView.cullFaceProperty());

        if (!meshView.isMouseTransparent()) {
            ChangeListener<DrawMode> drawModeChangeListener =
                    (observable, oldMode, newMode) -> meshView.setMouseTransparent(newMode != DrawMode.FILL);

            if (controller.meshViewDrawModeListeners.put(meshView, drawModeChangeListener) != null)
                controller.getLogger().warning("drawModeChangeListener is already set!");

            meshView.drawModeProperty().addListener(drawModeChangeListener);
        }
    }

    /**
     * Unbind mesh scene controls to the provided MeshView.
     * @param controller the controller to unbind the controls from
     * @param meshView the meshView to unbind the controls from
     */
    public static void unbindMeshSceneControls(MeshViewController<?> controller, MeshView meshView) {
        controller.getCheckBoxShowMesh().selectedProperty().unbindBidirectional(meshView.visibleProperty());
        controller.getComboBoxMeshDrawMode().valueProperty().unbindBidirectional(meshView.drawModeProperty());
        controller.getComboBoxMeshCullFace().valueProperty().unbindBidirectional(meshView.cullFaceProperty());

        ChangeListener<DrawMode> drawModeChangeListener = controller.meshViewDrawModeListeners.remove(meshView);
        if (drawModeChangeListener != null)
            meshView.drawModeProperty().removeListener(drawModeChangeListener);
    }

    /**
     * Sets up the mesh viewer UI for a particular scene.
     * @param controller      The controller for the mesh view.
     * @param mesh            The mesh to display.
     * @param <TController>   The type of controller which manages the mesh.
     * @param <TDynMesh>      The main mesh displayed by the controller.
     * @return controller
     */
    public static <TController extends MeshViewController<TDynMesh>, TDynMesh extends DynamicMesh> TController setupMeshViewer(GameInstance instance, TController controller, TDynMesh mesh) {
        Stage stage = instance != null ? instance.getMainStage() : null;
        if (stage == null)
            return null;

        return setupMeshViewer(stage, controller, mesh);
    }

    /**
     * Sets up the mesh viewer UI for a particular scene.
     * @param stageToOverride The stage to create the mesh view on.
     * @param controller      The controller for the mesh view.
     * @param mesh            The mesh to display.
     * @param <TController>   The type of controller which manages the mesh.
     * @param <TDynMesh>      The main mesh displayed by the controller.
     * @return controller
     */
    public static <TController extends MeshViewController<TDynMesh>, TDynMesh extends DynamicMesh> TController setupMeshViewer(Stage stageToOverride, TController controller, TDynMesh mesh) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            FXUtils.showPopup(AlertType.WARNING, "3D not supported.", "Your version of JavaFX does not support 3D, so meshes cannot be previewed.");
            return null;
        }

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = BASIC_MESH_VIEW_FXML_LOADER;
        fxmlLoader.setController(controller);

        // Load template.
        Parent loadRoot;
        try {
            loadRoot = fxmlLoader.load();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load base mesh view fxml template.", ex);
        } finally {
            fxmlLoader.setController(null);
            fxmlLoader.setRoot(null);
            fxmlLoader.getNamespace().clear(); // In FX8, this seems to cause memory leaks if I don't clear it...
        }

        // Setup UI.
        controller.setupController(mesh, stageToOverride, loadRoot);
        return controller;
    }
}