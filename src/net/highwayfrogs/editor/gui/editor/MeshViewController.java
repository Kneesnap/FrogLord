package net.highwayfrogs.editor.gui.editor;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.psx.shading.IPSXShadedMesh;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.editor.DisplayList.RenderListManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manages the UI which is displayed when a mesh is viewed.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public abstract class MeshViewController<TMesh extends DynamicMesh> implements Initializable {
    // Useful constants and settings
    public static final double MAP_VIEW_NEAR_CLIP = 0.1;
    public static final double MAP_VIEW_FAR_CLIP = 2000.0;
    public static final double MAP_VIEW_FOV = 60.0;

    private SubScene subScene;
    private Group subScene2DElements;
    private Logger cachedLogger;

    // Baseline UI components
    @FXML private AnchorPane anchorPaneUIRoot;
    @FXML private Accordion accordionLeft;

    // Control Settings Pane.
    @FXML private CheckBox checkBoxShowAxis;
    @FXML private CheckBox checkBoxShowMesh;
    @FXML private ComboBox<DrawMode> comboBoxMeshDrawMode;
    @FXML private ComboBox<CullFace> comboBoxMeshCullFace;
    @FXML private CheckBox checkBoxEnablePsxShading;
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
    @FXML private GridPane viewSettingsPane;

    // Camera Settings.
    @FXML private TitledPane cameraSettings;
    @FXML private GridPane cameraSettingsPane;

    // Managers:
    private MeshUIMarkerManager<TMesh> markerManager;
    private final List<MeshUIManager<TMesh>> managers = new ArrayList<>();
    private final Map<Class<? extends MeshUIManager<TMesh>>, MeshUIManager<TMesh>> managersByType = new HashMap<>();
    private final List<MeshUISelector<TMesh, ?>> selectors = new ArrayList<>();
    private final RenderListManager renderManager = new RenderListManager();
    private final RenderListManager transparentRenderManager = new RenderListManager();
    private final InputManager inputManager = new InputManager();
    private final FirstPersonCamera firstPersonCamera = new FirstPersonCamera(this.inputManager);

    // Instance Data:
    private TMesh mesh;
    private DisplayList axisDisplayList;

    // Mesh Rendering:
    private MeshView meshView;
    private Group root3D;
    private Scene meshScene;
    private Scene originalScene;
    private Stage overwrittenStage;
    private final Group lightingGroup = new Group();
    private AmbientLight mainLight;

    private static final NumberStringConverter NUM_TO_STRING_CONVERTER = new NumberStringConverter(new DecimalFormat("####0.000000"));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Do nothing for now.
    }

    /**
     * Get the logger for this controller.
     */
    public Logger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this));
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
        dynamicMesh.addView(this.meshView);

        // Create the 3D elements and use them within a subscene.
        this.root3D = new Group();
        SubScene subScene3D = new SubScene(this.root3D, stageToOverride.getScene().getWidth() - uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.BALANCED);
        subScene3D.setFill(Color.BLACK);
        subScene3D.setCamera(this.firstPersonCamera.getCamera());

        // Ensure that the render manager has access to the root node
        Group normalGroup = new Group();
        Group transparentGroup = new Group();
        this.root3D.getChildren().add(normalGroup);
        this.root3D.getChildren().add(transparentGroup);
        this.renderManager.setRoot(normalGroup);
        this.transparentRenderManager.setRoot(transparentGroup);

        // Initialise the UI layout.
        BorderPane uiPane = new BorderPane();
        uiPane.setLeft(loadRoot);
        this.subScene2DElements = new Group();
        this.subScene2DElements.getChildren().add(subScene3D);
        uiPane.setCenter(this.subScene2DElements);

        // Create and set the scene with antialiasing.
        this.meshScene = new Scene(uiPane, -1, -1, true, SceneAntialiasing.BALANCED);
        this.originalScene = Utils.setSceneKeepPosition(stageToOverride, this.meshScene);

        // Handle scaling of SubScene on stage resizing.
        this.meshScene.widthProperty().addListener((observable, old, newVal) -> subScene3D.setWidth(newVal.doubleValue() - uiRootPaneWidth()));
        subScene3D.heightProperty().bind(this.meshScene.heightProperty());

        // Associate camera controls with the scene.
        this.firstPersonCamera.assignSceneControls(stageToOverride, this.meshScene);
        this.firstPersonCamera.startThreadProcessing();

        this.inputManager.setFinalKeyHandler((manager, event) -> {
            if (onKeyPress(event) || event.getEventType() != KeyEvent.KEY_PRESSED)
                return; // Handled by the other controller.

            if (event.getCode() == KeyCode.ESCAPE) { // Exit the viewer.
                // Stop camera processing and clear up the render manager
                this.firstPersonCamera.stopThreadProcessing();
                this.renderManager.removeAllDisplayLists();
                this.transparentRenderManager.removeAllDisplayLists();
                this.inputManager.shutdown();

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

                getMesh().removeView(getMeshView()); // Remove view from mesh.
                Utils.setSceneKeepPosition(this.overwrittenStage, this.originalScene);
                this.root3D.getChildren().clear(); // Clear data to avoid memory leak.
            } else if (event.getCode() == KeyCode.F10) { // Take screenshot.
                Utils.takeScreenshot(null, this.subScene, getMeshScene(), Utils.stripExtension(getMeshDisplayName()), false);
            } else if (event.getCode() == KeyCode.F12 && getMesh().getTextureAtlas() != null) {
                getLogger().info("Saving main mesh texture sheet to 'texture-sheet.png'...");

                try {
                    ImageIO.write(getMesh().getTextureAtlas().getImage(), "png", new File(GUIMain.getWorkingDirectory(), "texture-sheet.png"));
                } catch (IOException ex) {
                    Utils.makeErrorPopUp("Failed to save 'texture-sheet.png'.", ex, true);
                }
            } else if ((event.isControlDown() && event.getCode() == KeyCode.ENTER)) { // Toggle full-screen.
                this.overwrittenStage.setFullScreen(!this.overwrittenStage.isFullScreen());
            } else if (event.getCode() == KeyCode.X) { // Toggle wireframe.
                this.meshView.setDrawMode(this.meshView.getDrawMode() == DrawMode.FILL ? DrawMode.LINE : DrawMode.FILL);
            }
        });

        // Setup managers.
        this.managers.clear();
        addManager(this.markerManager = new MeshUIMarkerManager<>(this));
        setupManagers();

        // Select Manager
        if (this.accordionLeft.getPanes().size() > 0)
            this.accordionLeft.getPanes().get(0).setExpanded(true);

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
    }

    /**
     * If this is true, it gives a preference to 3D models having transparency over the world.
     */
    protected boolean mapRendersFirst() {
        return false;
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
        return anchorPaneUIRoot.getPrefWidth();
    }

    /**
     * Get the root pane height.
     */
    public double uiRootPaneHeight() {
        return anchorPaneUIRoot.getPrefHeight();
    }

    /**
     * Primary function (entry point) for setting up data / control bindings, etc.
     */
    public void setupBindings(SubScene subScene3D, MeshView meshView) {
        this.subScene = subScene3D;

        // Setup axis.
        setupAxis();

        // Setup camera.
        PerspectiveCamera camera = this.firstPersonCamera.getCamera();
        camera.setNearClip(MAP_VIEW_NEAR_CLIP);
        camera.setFarClip(MAP_VIEW_FAR_CLIP);
        camera.setFieldOfView(MAP_VIEW_FOV);

        // Set informational bindings and editor bindings
        this.colorPickerLevelBackground.setValue((Color) this.subScene.getFill());
        this.subScene.fillProperty().bind(this.colorPickerLevelBackground.valueProperty());

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
        bindMeshSceneControls(this, meshView);

        if (getMesh() instanceof IPSXShadedMesh) {
            IPSXShadedMesh shadedMesh = (IPSXShadedMesh) getMesh();
            this.checkBoxEnablePsxShading.setSelected(shadedMesh.isShadingEnabled());
            this.checkBoxEnablePsxShading.selectedProperty().addListener((observable, oldState, newState) -> ((IPSXShadedMesh) getMesh()).setShadingEnabled(newState));
        } else {
            // PSX Shading is not supported, so keep it disabled.
            this.checkBoxEnablePsxShading.setDisable(true);
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
        this.axisDisplayList.addLine(0, 0, 0, axisLength, 0, 0, lineSize, Utils.makeSpecialMaterial(Color.RED)); // X Axis.
        this.axisDisplayList.addLine(0, 0, 0, 0, axisLength, 0, lineSize, Utils.makeSpecialMaterial(Color.GREEN)); // Y Axis.
        this.axisDisplayList.addLine(0, 0, 0, 0, 0, axisLength, lineSize, Utils.makeSpecialMaterial(Color.BLUE)); // Z Axis.

        this.axisDisplayList.setVisible(this.checkBoxShowAxis.isSelected());
        this.checkBoxShowAxis.selectedProperty().addListener((listener, oldValue, newValue) -> this.axisDisplayList.setVisible(newValue));
        this.lightingGroup.getChildren().add(this.axisDisplayList.getRoot());
    }

    private void runForEachManager(Consumer<MeshUIManager<TMesh>> execution, String name) {
        for (MeshUIManager<TMesh> manager : getManagers()) {
            try {
                execution.accept(manager);
            } catch (Throwable th) {
                Utils.handleError(getLogger(), th, true, "Failed to run '%s'.", name);
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
            Utils.makePopUp("Your version of JavaFX does not support 3D, so meshes cannot be previewed.", AlertType.WARNING);
            return null;
        }

        // Load FXML for UI layout.
        FXMLLoader fxmlLoader = new FXMLLoader(MeshViewController.class.getResource("/fxml/scene-basic-3d.fxml"));
        fxmlLoader.setController(controller);

        // Load template.
        Parent loadRoot;
        try {
            loadRoot = fxmlLoader.load();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load base mesh view fxml template.", ex);
        }

        // Setup UI.
        controller.setupController(mesh, stageToOverride, loadRoot);
        return controller;
    }
}