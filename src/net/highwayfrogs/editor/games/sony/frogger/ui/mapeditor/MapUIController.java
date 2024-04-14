package net.highwayfrogs.editor.games.sony.frogger.ui.mapeditor;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
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
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.CursorVertexColor;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.map.view.TextureMap.ShadingMode;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.ui.FroggerMapInfoUIController;
import net.highwayfrogs.editor.games.sony.frogger.ui.mapeditor.PathManager.PathDisplaySetting;
import net.highwayfrogs.editor.gui.InputManager;
import net.highwayfrogs.editor.gui.editor.FirstPersonCamera;
import net.highwayfrogs.editor.gui.editor.RenderManager;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Manages the UI which is displayed when viewing Frogger maps.
 * TODO: Let's render SKY LAND underneath sky levels. We can apply shading, etc. I think this could work good.
 * Created by AndyEder on 1/4/2019.
 */
@Getter
public class MapUIController implements Initializable {
    // Useful constants and settings
    public static final double MAP_VIEW_NEAR_CLIP = 0.1;
    public static final double MAP_VIEW_FAR_CLIP = 2000.0;
    public static final double MAP_VIEW_FOV = 60.0;

    private static final int VERTEX_SPEED = 3;
    @Getter private static final IntegerProperty propertyVertexSpeed = new SimpleIntegerProperty(3);

    public static final float ENTITY_ICON_SIZE = 16.0f;

    private FroggerMapInfoUIController controller;
    private SubScene subScene;

    // Baseline UI components
    @FXML private AnchorPane anchorPaneUIRoot;
    @FXML private Accordion accordionLeft;

    // Control Settings Pane.
    @FXML private TitledPane titledPaneInformation;
    @FXML private CheckBox checkBoxShowMesh;
    @FXML private ChoiceBox<PathDisplaySetting> pathDisplayOption;
    @FXML private CheckBox checkBoxFaceRemoveMode;
    @FXML private CheckBox checkBoxShowUnusedVertices;
    @FXML private ComboBox<DrawMode> comboBoxMeshDrawMode;
    @FXML private ComboBox<CullFace> comboBoxMeshCullFace;
    @FXML private ChoiceBox<ShadingMode> shadingModeChoiceBox;
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
    @FXML private CheckBox applyLightsCheckBox;

    // General pane.
    @FXML private TitledPane generalPane;
    @FXML private GridPane generalGridPane;

    // Geometry pane.
    @FXML private TitledPane geometryPane;
    @FXML private GridPane geometryGridPane;

    // Animation pane.
    @FXML private TitledPane animationPane;
    @FXML private GridPane animationGridPane;

    // Entity pane
    @FXML private TitledPane entityPane;
    @FXML private GridPane entityGridPane;

    // Light Pane.
    @FXML private TitledPane lightPane;
    @FXML private GridPane lightGridPane;

    // Form pane.
    @FXML private TitledPane formPane;
    @FXML private GridPane formGridPane;

    // Path pane.
    @FXML private TitledPane pathPane;
    @FXML private GridPane pathGridPane;

    // Managers:
    private final List<MapManager> managers = new ArrayList<>();
    private PathManager pathManager;
    private LightManager lightManager;
    private EntityManager entityManager;
    private AnimationManager animationManager;
    private FormManager formManager;
    private GeometryManager geometryManager; // Should be second to last, so when a successful click goes through it will update itself. Also, now this handles cancelling polygon selection.
    private GeneralManager generalManager; // Should be last, so things like cancelling polygon selection happen last.

    // Map Data:
    private final InputManager inputManager = new InputManager();
    private final FirstPersonCamera firstPersonCamera;
    private MapMesh mapMesh;
    private final RenderManager renderManager = new RenderManager();
    private Group root3D;
    private Scene mapScene;
    private MeshView meshView;
    private Scene defaultScene;
    private Stage overwrittenStage;

    private static final NumberStringConverter NUM_TO_STRING_CONVERTER = new NumberStringConverter(new DecimalFormat("####0.000000"));

    public MapUIController() {
        this.firstPersonCamera = new FirstPersonCamera(this.inputManager);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accordionLeft.setExpandedPane(generalPane);

        // Setup managers.
        this.managers.clear();
        this.managers.add(this.pathManager = new PathManager(this));
        this.managers.add(this.lightManager = new LightManager(this));
        this.managers.add(this.entityManager = new EntityManager(this));
        this.managers.add(this.animationManager = new AnimationManager(this));
        this.managers.add(this.formManager = new FormManager(this));
        this.managers.add(this.geometryManager = new GeometryManager(this));
        this.managers.add(this.generalManager = new GeneralManager(this));
    }

    public void setupController(FroggerMapInfoUIController controller, Stage stageToOverride, MapMesh mesh, MeshView meshView, Parent loadRoot) {
        this.overwrittenStage = stageToOverride;
        this.mapMesh = mesh;
        this.controller = controller;
        this.meshView = meshView;

        // Create the 3D elements and use them within a subscene.
        this.root3D = new Group(meshView);
        SubScene subScene3D = new SubScene(this.root3D, stageToOverride.getScene().getWidth() - uiRootPaneWidth(), stageToOverride.getScene().getHeight(), true, SceneAntialiasing.BALANCED);
        subScene3D.setFill(Color.BLACK);
        subScene3D.setCamera(this.firstPersonCamera.getCamera());

        // Ensure that the render manager has access to the root node
        this.renderManager.setRenderRoot(this.root3D);

        // Setup the UI layout.
        BorderPane uiPane = new BorderPane();
        uiPane.setLeft(loadRoot);
        uiPane.setCenter(subScene3D);

        // Create and set the scene.
        this.mapScene = new Scene(uiPane);
        this.defaultScene = Utils.setSceneKeepPosition(stageToOverride, this.mapScene);

        // Handle scaling of SubScene on stage resizing.
        this.mapScene.widthProperty().addListener((observable, old, newVal) -> subScene3D.setWidth(newVal.doubleValue() - uiRootPaneWidth()));
        subScene3D.heightProperty().bind(this.mapScene.heightProperty());

        // Associate camera controls with the scene.
        this.firstPersonCamera.assignSceneControls(stageToOverride, this.mapScene);
        this.firstPersonCamera.startThreadProcessing();

        this.mapScene.setOnKeyPressed(event -> {
            if (onKeyPress(event))
                return; // Handled by the other controller.

            // Exit the viewer.
            if (event.getCode() == KeyCode.ESCAPE) {
                // Stop camera processing and clear up the render manager
                firstPersonCamera.stopThreadProcessing();
                renderManager.removeAllDisplayLists();
                Utils.setSceneKeepPosition(this.overwrittenStage, this.defaultScene);
            } else if (event.getCode() == KeyCode.F10) {
                Utils.takeScreenshot(controller.getGameInstance(), this.subScene, getMapScene(), Utils.stripExtension(getMap().getFileDisplayName()), false);
            }
        });

        // Set the initial camera position based on start position and in-game camera offset.
        MAPFile map = getMap();
        SVector startPos = map.getCameraSourceOffset();

        GridStack startStack = map.getGridStack(map.getStartXTile(), map.getStartZTile());
        float gridX = Utils.fixedPointIntToFloat4Bit(map.getWorldX(map.getStartXTile(), true));
        float baseY = startStack != null ? startStack.calculateWorldHeight(map) : 0;
        float gridZ = Utils.fixedPointIntToFloat4Bit(map.getWorldZ(map.getStartZTile(), true));

        // Make sure the start position is off the ground.
        float yOffset = startPos.getFloatY();
        if (Math.abs(yOffset) <= .0001)
            yOffset = -100f;

        this.firstPersonCamera.setPos(gridX + startPos.getFloatX(), baseY + yOffset, gridZ + startPos.getFloatZ());
        this.firstPersonCamera.setCameraLookAt(gridX, baseY, gridZ + 1); // Set the camera to look at the start position, too. The -1 is necessary to fix some near-zero math. It fixes it for QB.MAP for example.

        setupBindings(controller, subScene3D, meshView); // Setup UI.
    }

    /**
     * Get the root pane width.
     */
    public double uiRootPaneWidth() {
        return this.anchorPaneUIRoot.getPrefWidth();
    }

    /**
     * Get the root pane height.
     */
    public double uiRootPaneHeight() {
        return this.anchorPaneUIRoot.getPrefHeight();
    }

    /**
     * Primary function (entry point) for setting up data / control bindings, etc. (May be broken out and tidied up later in development).
     */
    public void setupBindings(FroggerMapInfoUIController controller, SubScene subScene3D, MeshView meshView) {
        this.controller = controller;
        this.subScene = subScene3D;

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
        this.checkBoxShowMesh.selectedProperty().bindBidirectional(meshView.visibleProperty());

        this.comboBoxMeshDrawMode.getItems().setAll(DrawMode.values());
        this.comboBoxMeshDrawMode.valueProperty().bindBidirectional(meshView.drawModeProperty());

        this.comboBoxMeshCullFace.getItems().setAll(CullFace.values());
        this.comboBoxMeshCullFace.valueProperty().bindBidirectional(meshView.cullFaceProperty());

        // Must be called after FroggerMapInfoUIController is passed.
        runForEachManager(MapManager::onSetup, "onSetup"); // Setup all of the managers.
        runForEachManager(MapManager::setupEditor, "setupEditor"); // Setup all of the managers editors.
    }

    private void runForEachManager(Consumer<MapManager> execution, String name) {
        for (MapManager manager : getManagers()) {
            try {
                execution.accept(manager);
            } catch (Throwable th) {
                getController().handleError(th, false, "Failed to run '%s' for the map manager '%s'.", name, manager.getClass().getSimpleName());
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
        for (MapManager manager : getManagers()) // Handle key presses.
            if (manager.onKeyPress(event))
                return true;

        return false;
    }

    /**
     * Handle when a polygon is selected.
     * @param event          The mouse event.
     * @param clickedPolygon The polygon clicked.
     * @return false = Not handled. True = handled.
     */
    public boolean handleClick(MouseEvent event, MAPPolygon clickedPolygon) {
        if (getGeometryManager().isPromptActive()) { // Highest priority.
            getGeometryManager().acceptPrompt(clickedPolygon);
            return true;
        }

        // Send to managers.
        for (MapManager manager : getManagers())
            if (manager.handleClick(event, clickedPolygon))
                return true;

        return false;
    }

    /**
     * Render over an existing polygon.
     * @param targetPoly The polygon to render over.
     * @param color      The color to render.
     */
    public void renderOverPolygon(MAPPolygon targetPoly, CursorVertexColor color) {
        getMapMesh().renderOverPolygon(targetPoly, color);
    }

    /**
     * Gets the map file this controls.
     */
    public MAPFile getMap() {
        return getController().getFile();
    }
}