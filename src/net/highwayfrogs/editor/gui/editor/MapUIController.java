package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.map.manager.*;
import net.highwayfrogs.editor.gui.editor.map.manager.PathManager.PathDisplaySetting;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.AbstractStringConverter;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Manages the UI which is displayed when viewing Frogger maps.
 * TODO:
 *  Cleanup the whole map UI code. Use managers, and have an actual system designed.
 *  Cache things, for instance, if we have an entity manager, and we just want to update the position of entities, just update the positions of entities instead of clearing the list fully and starting over.
 *  Try creating a system which is better in terms of memory usage, for the 2D editor too.
 *  Make sure everything is rebuilt when you exit the viewer then come back, in case of changes.
 * Created by AndyEder on 1/4/2019.
 */
@Getter
public class MapUIController implements Initializable {
    // Useful constants and settings
    public static final double MAP_VIEW_NEAR_CLIP = 0.1;
    public static final double MAP_VIEW_FAR_CLIP = 2000.0;
    public static final double MAP_VIEW_FOV = 60.0;

    private static final int VERTEX_SPEED = 3;
    @Getter private static IntegerProperty propertyVertexSpeed = new SimpleIntegerProperty(3);

    private static float ENTITY_ICON_SIZE = 16.0f;
    @Getter private static FloatProperty propertyEntityIconSize = new SimpleFloatProperty(ENTITY_ICON_SIZE);

    private MAPController controller;
    private SubScene subScene;

    // Baseline UI components
    @FXML private AnchorPane anchorPaneUIRoot;
    @FXML private Accordion accordionLeft;

    // Control Settings Pane.
    @FXML private TitledPane titledPaneInformation;
    @FXML private CheckBox checkBoxShowMesh;
    @FXML private ChoiceBox<PathDisplaySetting> pathDisplayOption;
    @FXML private CheckBox checkBoxFaceRemoveMode;
    @FXML private ComboBox<DrawMode> comboBoxMeshDrawMode;
    @FXML private ComboBox<CullFace> comboBoxMeshCullFace;
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
    @FXML private TextField textFieldEntityIconSize;
    @FXML private CheckBox applyLightsCheckBox;

    // General pane.
    @FXML private TitledPane generalPane;
    @FXML private GridPane generalGridPane;
    private GUIEditorGrid generalEditor;

    // Geometry pane.
    @FXML private TitledPane geometryPane;
    @FXML private GridPane geometryGridPane;
    private GUIEditorGrid geometryEditor;
    private MeshData looseMeshData;

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
    private GUIEditorGrid formEditor;
    private Form selectedForm;

    // Path pane.
    @FXML private TitledPane pathPane;
    @FXML private GridPane pathGridPane;

    // Non-GUI.
    private Consumer<MAPPolygon> onSelect;
    private Runnable cancelSelection;

    // Managers:
    private List<MapManager> managers = new ArrayList<>();
    private PathManager pathManager;
    private LightManager lightManager;
    private EntityManager entityManager;
    private AnimationManager animationManager;

    private static final NumberStringConverter NUM_TO_STRING_CONVERTER = new NumberStringConverter(new DecimalFormat("####0.000000"));

    //>>

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accordionLeft.setExpandedPane(generalPane);

        // Setup managers.
        this.managers.clear();
        this.managers.add(this.pathManager = new PathManager(this));
        this.managers.add(this.lightManager = new LightManager(this));
        this.managers.add(this.entityManager = new EntityManager(this));
        this.managers.add(this.animationManager = new AnimationManager(this));
    }

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
     * Sets up the general editor.
     */
    public void setupGeneralEditor() {
        if (generalEditor == null)
            generalEditor = new GUIEditorGrid(generalGridPane);

        generalEditor.clearEditor();
        getController().getFile().setupEditor(getController(), generalEditor);
    }

    /**
     * Setup the map geometry editor.
     */
    public void setupGeometryEditor() {
        if (this.geometryEditor == null)
            this.geometryEditor = new GUIEditorGrid(geometryGridPane);

        this.geometryEditor.clearEditor();
        this.geometryEditor.addButton("Edit Collision Grid", () -> GridController.openGridEditor(this));
        this.geometryEditor.addCheckBox("Highlight Invisible Polygons", this.looseMeshData != null, this::updateVisibility);
        this.geometryEditor.addSeparator(25);

        MAPPolygon showPolygon = getController().getSelectedPolygon();
        if (showPolygon != null) {
            this.geometryEditor.addBoldLabel("Selected Polygon:");
            showPolygon.setupEditor(this, this.geometryEditor);
        }
    }

    /**
     * Setup the map form editor.
     */
    public void setupFormEditor() {
        if (this.formEditor == null)
            this.formEditor = new GUIEditorGrid(formGridPane);

        if (this.selectedForm == null && !getMap().getForms().isEmpty())
            this.selectedForm = getMap().getForms().get(0);

        this.formEditor.clearEditor();

        if (this.selectedForm != null) {
            ComboBox<Form> box = this.formEditor.addSelectionBox("Form", getSelectedForm(), getMap().getForms(), newForm -> {
                this.selectedForm = newForm;
                setupFormEditor();
            });

            box.setConverter(new AbstractStringConverter<>(form -> "Form #" + getMap().getForms().indexOf(form)));
        }

        this.formEditor.addBoldLabel("Management:");
        this.formEditor.addButton("Add Form", () -> {
            this.selectedForm = new Form();
            getMap().getForms().add(this.selectedForm);
            setupFormEditor();
        });

        if (this.selectedForm != null) {
            this.formEditor.addButton("Remove Form", () -> {
                getMap().getForms().remove(getSelectedForm());
                this.selectedForm = null;
                setupFormEditor();
            });

            getSelectedForm().setupEditor(this, this.formEditor);
        }
    }

    private void updateVisibility(boolean drawState) {
        if (this.looseMeshData != null) {
            getMesh().getManager().removeMesh(this.looseMeshData);
            this.looseMeshData = null;
        }

        if (drawState) {
            getMap().forEachPrimitive(prim -> {
                if (!prim.isAllowDisplay() && prim instanceof MAPPolygon)
                    getController().renderOverPolygon((MAPPolygon) prim, MapMesh.INVISIBLE_COLOR);
            });
            this.looseMeshData = getMesh().getManager().addMesh();
        }
    }

    /**
     * Primary function (entry point) for setting up data / control bindings, etc. (May be broken out and tidied up later in development).
     */
    public void setupBindings(MAPController controller, SubScene subScene3D, MeshView meshView) {
        this.controller = controller;
        this.subScene = subScene3D;

        CameraFPS cameraFPS = controller.getCameraFPS();
        PerspectiveCamera camera = cameraFPS.getCamera();

        camera.setNearClip(MAP_VIEW_NEAR_CLIP);
        camera.setFarClip(MAP_VIEW_FAR_CLIP);
        camera.setFieldOfView(MAP_VIEW_FOV);

        // Set informational bindings and editor bindings
        colorPickerLevelBackground.setValue((Color) this.subScene.getFill());
        this.subScene.fillProperty().bind(colorPickerLevelBackground.valueProperty());

        textFieldCamMoveSpeed.textProperty().bindBidirectional(cameraFPS.getCamMoveSpeedProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamMouseSpeed.textProperty().bindBidirectional(cameraFPS.getCamMouseSpeedProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamSpeedDownMultiplier.textProperty().bindBidirectional(cameraFPS.getCamSpeedDownMultiplierProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamSpeedUpMultiplier.textProperty().bindBidirectional(cameraFPS.getCamSpeedUpMultiplierProperty(), NUM_TO_STRING_CONVERTER);
        checkBoxYInvert.selectedProperty().bindBidirectional(cameraFPS.getCamYInvertProperty());

        btnResetCamMoveSpeed.setOnAction(e -> cameraFPS.resetDefaultCamMoveSpeed());
        btnResetCamMouseSpeed.setOnAction(e -> cameraFPS.resetDefaultCamMouseSpeed());
        btnResetCamSpeedDownMultiplier.setOnAction(e -> cameraFPS.resetDefaultCamSpeedDownMultiplier());
        btnResetCamSpeedUpMultiplier.setOnAction(e -> cameraFPS.resetDefaultCamSpeedUpMultiplier());

        // Set camera bindings
        textFieldCamNearClip.textProperty().bindBidirectional(camera.nearClipProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamFarClip.textProperty().bindBidirectional(camera.farClipProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamFoV.textProperty().bindBidirectional(camera.fieldOfViewProperty(), NUM_TO_STRING_CONVERTER);

        textFieldCamPosX.textProperty().bindBidirectional(cameraFPS.getCamPosXProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamPosY.textProperty().bindBidirectional(cameraFPS.getCamPosYProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamPosZ.textProperty().bindBidirectional(cameraFPS.getCamPosZProperty(), NUM_TO_STRING_CONVERTER);

        textFieldCamYaw.textProperty().bind(cameraFPS.getCamYawProperty().asString("%.6f"));
        textFieldCamPitch.textProperty().bind(cameraFPS.getCamPitchProperty().asString("%.6f"));
        textFieldCamRoll.textProperty().bind(cameraFPS.getCamRollProperty().asString("%.6f"));

        // Set mesh view bindings
        checkBoxShowMesh.selectedProperty().bindBidirectional(meshView.visibleProperty());

        comboBoxMeshDrawMode.getItems().setAll(DrawMode.values());
        comboBoxMeshDrawMode.valueProperty().bindBidirectional(meshView.drawModeProperty());

        comboBoxMeshCullFace.getItems().setAll(CullFace.values());
        comboBoxMeshCullFace.valueProperty().bindBidirectional(meshView.cullFaceProperty());

        textFieldEntityIconSize.textProperty().bindBidirectional(propertyEntityIconSize, NUM_TO_STRING_CONVERTER);

        // Must be called after MAPController is passed.
        getManagers().forEach(MapManager::onSetup); // Setup all of the managers.
        getManagers().forEach(MapManager::setupEditor); // Setup all of the managers editors.
        setupGeneralEditor();
        setupGeometryEditor();
        setupFormEditor();
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
     * Gets the map file being edited.
     * @return mapFile
     */
    public MAPFile getMap() {
        return getController().getFile();
    }

    /**
     * Gets the mesh being controlled.
     * @return mapMesh
     */
    public MapMesh getMesh() {
        return getController().getMapMesh();
    }

    /**
     * Handle when a key is pressed.
     * @param event The key event fired.
     * @return wasHandled
     */
    public boolean onKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            for (MapManager manager : getManagers()) { // Cancel active prompts.
                if (manager.isPromptActive()) {
                    manager.cancelPrompt();
                    return true;
                }
            }

            if (!getController().isPolygonSelected() && onSelect != null) { // Handle cancelling polygon selection.
                onSelect = null;
                if (cancelSelection != null) {
                    cancelSelection.run();
                    cancelSelection = null;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Handle when a polygon is selected.
     * @param event          The mouse event.
     * @param clickedPolygon The polygon clicked.
     * @return false = Not handled. True = handled.
     */
    public boolean handleClick(MouseEvent event, MAPPolygon clickedPolygon) {
        // Highest priority.
        if (getOnSelect() != null) {
            getOnSelect().accept(clickedPolygon);
            this.onSelect = null;
            this.cancelSelection = null;
            return true;
        }

        if (getLooseMeshData() != null) { // Toggle visibility.
            clickedPolygon.setAllowDisplay(!clickedPolygon.isAllowDisplay());
            updateVisibility(true);
            return true;
        }

        // Send to managers.
        for (MapManager manager : getManagers())
            if (manager.handleClick(event, clickedPolygon))
                return true;

        // Nothing has handled it yet.
        Platform.runLater(this::setupGeometryEditor);
        return false;
    }

    /**
     * Waits for the the user to select a polygon.
     * @param onSelect The behavior when selected.
     * @param onCancel The behavior if cancelled.
     */
    public void selectPolygon(Consumer<MAPPolygon> onSelect, Runnable onCancel) {
        this.onSelect = onSelect;
        this.cancelSelection = onCancel;
    }
}