package net.highwayfrogs.editor.gui.editor;

import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
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
import net.highwayfrogs.editor.file.MWDFile;
import net.highwayfrogs.editor.file.config.exe.general.FormEntry;
import net.highwayfrogs.editor.file.map.MAPEditorGUI;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.animation.MAPUVInfo;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.entity.Entity.EntityFlag;
import net.highwayfrogs.editor.file.map.form.Form;
import net.highwayfrogs.editor.file.map.grid.GridSquare;
import net.highwayfrogs.editor.file.map.grid.GridStack;
import net.highwayfrogs.editor.file.map.light.APILightType;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.SelectionMenu.AttachmentListCell;
import net.highwayfrogs.editor.gui.editor.map.manager.MapManager;
import net.highwayfrogs.editor.gui.editor.map.manager.PathManager;
import net.highwayfrogs.editor.gui.editor.map.manager.PathManager.PathDisplaySetting;
import net.highwayfrogs.editor.gui.mesh.MeshData;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Manages the UI which is displayed when viewing Frogger maps.
 * TODO:
 *  - Cleanup the whole map UI code. Use managers, and have an actual system designed.
 *  - Cache things, for instance, if we have an entity manager, and we just want to update the position of entities, just update the positions of entities instead of clearing the list fully and starting over.
 *  - Try creating a system which is better in terms of memory usage, for the 2D editor too.
 *  - Make sure everything is rebuilt when you exit the viewer then come back, in case of changes.
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
    private GUIEditorGrid animationEditor;
    private MAPAnimation editAnimation;
    private MeshData animationMarker;
    private MAPAnimation selectedAnimation;

    // Entity pane
    @FXML private TitledPane entityPane;
    @FXML private GridPane entityGridPane;
    private MAPEditorGUI entityEditor;

    // Light Pane.
    @FXML private TitledPane lightPane;
    @FXML private GridPane lightGridPane;
    private GUIEditorGrid lightEditor;

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

    private List<MapManager> managers = new ArrayList<>();
    private PathManager pathManager;

    private static final NumberStringConverter NUM_TO_STRING_CONVERTER = new NumberStringConverter(new DecimalFormat("####0.000000"));

    //>>

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accordionLeft.setExpandedPane(generalPane);
        entityEditor = new MAPEditorGUI(entityGridPane, this);
        pathDisplayOption.setItems(FXCollections.observableArrayList(PathDisplaySetting.values()));
        pathDisplayOption.getSelectionModel().selectFirst();

        // Setup managers.
        this.managers.clear();
        this.managers.add(this.pathManager = new PathManager(this));
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
     * Sets up the lighting editor.
     */
    public void setupLights() {
        getController().updateLighting();
        if (lightEditor == null)
            lightEditor = new GUIEditorGrid(lightGridPane);

        lightEditor.clearEditor();
        for (int i = 0; i < getController().getFile().getLights().size(); i++) {
            final int tempIndex = i;
            lightEditor.addBoldLabelButton("Light #" + (i + 1) + ":", "Remove", 25, () -> {
                getController().getFile().getLights().remove(tempIndex);
                setupLights(); // Reload this.
            });

            lightEditor.addLabel("ApiType:", getController().getFile().getLights().get(i).getApiType().name(), 25);
            getController().getFile().getLights().get(i).makeEditor(lightEditor, this);

            lightEditor.addSeparator(25);
        }

        lightEditor.addButtonWithEnumSelection("Add Light", apiType -> {
            getController().getFile().getLights().add(new Light(apiType));
            setupLights();
        }, APILightType.values(), APILightType.AMBIENT);
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
     * Setup the animation editor.
     */
    public void setupAnimationEditor() {
        if (animationEditor == null)
            animationEditor = new GUIEditorGrid(animationGridPane);

        animationEditor.clearEditor();

        ComboBox<MAPAnimation> box = this.animationEditor.addSelectionBox("Animation:", getSelectedAnimation(), getMap().getMapAnimations(), newAnim -> {
            this.selectedAnimation = newAnim;
            setupAnimationEditor();
        });
        box.setConverter(new AbstractStringConverter<>(anim -> "Animation #" + getMap().getMapAnimations().indexOf(anim)));
        box.setCellFactory(param -> new AttachmentListCell<>(anim -> "Animation #" + getMap().getMapAnimations().indexOf(anim), anim ->
                anim.getTextures().size() > 0 ? getMap().getVlo().getImageByTextureId(getMap().getConfig().getRemapTable(getMap().getFileEntry()).get(anim.getTextures().get(0))).toFXImage(MWDFile.VLO_ICON_SETTING) : null));

        if (this.selectedAnimation != null) {
            this.animationEditor.addBoldLabelButton("Animation #" + getMap().getMapAnimations().indexOf(this.selectedAnimation) + ":", "Remove", 25, () -> {
                getMap().getMapAnimations().remove(this.selectedAnimation);
                this.selectedAnimation = null;
                setupAnimationEditor(); // Reload this.
            });

            this.selectedAnimation.setupEditor(this, this.animationEditor);

        }

        this.animationEditor.addSeparator(25);
        this.animationEditor.addButton("Add Animation", () -> {
            getMap().getMapAnimations().add(this.selectedAnimation = new MAPAnimation(getMap()));
            setupAnimationEditor();
        });
    }

    /**
     * Setup the path editor.
     */
    public void setupPathEditor() {
        getPathManager().setupEditor();
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
     * Show entity information.
     * @param entity The entity to show information for.
     */
    public void showEntityInfo(Entity entity) {
        FormEntry[] entries = getMap().getConfig().getAllowedForms(getMap().getTheme());
        if (entity != null && !Utils.contains(entries, entity.getFormEntry())) // This wasn't found in this
            entries = getMap().getConfig().getFullFormBook().toArray(new FormEntry[0]);

        // Setup Editor:
        entityEditor.clearEditor();
        if (entity == null) {
            entityEditor.addBoldLabel("There is no entity selected.");
            entityEditor.addButtonWithEnumSelection("Add Entity", this::addNewEntity, entries, entries[0])
                    .setConverter(new AbstractStringConverter<>(FormEntry::getFormName));
            return;
        }

        this.entityEditor.addButtonWithEnumSelection("Add Entity", this::addNewEntity, entries, entity.getFormEntry())
                .setConverter(new AbstractStringConverter<>(FormEntry::getFormName));

        entityEditor.addBoldLabel("General Information:");
        entityEditor.addLabel("Entity Type", entity.getFormEntry().getEntityName());

        entityEditor.addEnumSelector("Form Type", entity.getFormEntry(), entries, false, newEntry -> {
            entity.setFormEntry(newEntry);
            showEntityInfo(entity);
            getController().resetEntities();
        }).setConverter(new AbstractStringConverter<>(FormEntry::getFormName));

        entityEditor.addIntegerField("Entity ID", entity.getUniqueId(), entity::setUniqueId, null);
        entityEditor.addIntegerField("Form ID", entity.getFormGridId(), entity::setFormGridId, null);
        entityEditor.addBoldLabel("Flags:");
        for (EntityFlag flag : EntityFlag.values())
            entityEditor.addCheckBox(Utils.capitalize(flag.name()), entity.testFlag(flag), newState -> entity.setFlag(flag, newState));

        // Populate Entity Data.
        if (entity.getEntityData() != null) {
            this.entityEditor.addSeparator(25);
            entityEditor.addBoldLabel("Entity Data:");
            entity.getEntityData().addData(this, this.entityEditor);
        }

        // Populate Script Data.
        if (entity.getScriptData() != null) {
            this.entityEditor.addSeparator(25);
            this.entityEditor.addBoldLabel("Script Data:");
            entity.getScriptData().addData(this.entityEditor);
        }

        this.entityEditor.addSeparator(25);
        this.entityEditor.addButton("Remove Entity", () -> {
            getMap().getEntities().remove(entity);
            getController().resetEntities();
            showEntityInfo(null); // Don't show the entity we just deleted.
        });

        entityPane.setExpanded(true);
    }

    private void addNewEntity(FormEntry entry) {
        Entity newEntity = new Entity(getMap(), entry);

        if (newEntity.getMatrixInfo() != null) { // Lets you select a polygon to place the new entity on.
            for (GridStack stack : getMap().getGridStacks())
                for (GridSquare square : stack.getGridSquares())
                    getController().renderOverPolygon(square.getPolygon(), MapMesh.GENERAL_SELECTION);
            MeshData data = getMesh().getManager().addMesh();

            selectPolygon(poly -> {
                getMesh().getManager().removeMesh(data);

                // Set entity position to the clicked polygon.
                PSXMatrix matrix = newEntity.getMatrixInfo();
                SVector pos = getController().getCenterOfPolygon(poly);
                matrix.getTransform()[0] = Utils.floatToFixedPointInt20Bit(pos.getFloatX());
                matrix.getTransform()[1] = Utils.floatToFixedPointInt20Bit(pos.getFloatY());
                matrix.getTransform()[2] = Utils.floatToFixedPointInt20Bit(pos.getFloatZ());

                // Add entity.
                addEntityToMap(newEntity);
            }, () -> getMesh().getManager().removeMesh(data));
            return;
        }

        if (newEntity.getPathInfo() != null) {
            if (getMap().getPaths().isEmpty()) {
                Utils.makePopUp("Path entities cannot be added if there are no paths present! Add a path.", AlertType.WARNING);
                return;
            }

            // User selects the path.
            getPathManager().promptPath((path, segment, segDistance) -> {
                newEntity.getPathInfo().setPath(getMap(), path, segment);
                newEntity.getPathInfo().setSegmentDistance(segDistance);
                addEntityToMap(newEntity);
            }, null);
            return;
        }

        addEntityToMap(newEntity);
    }

    private void addEntityToMap(Entity entity) {
        if (entity.getUniqueId() == -1) { // Default entity id, update it to something new.
            HashSet<Integer> entityIds = new HashSet<>();
            for (Entity tempEntity : getMap().getEntities())
                entityIds.add(tempEntity.getUniqueId());

            int tryId = 0;
            while (entityIds.contains(tryId))
                tryId++;

            entity.setUniqueId(tryId); // Sets the entity id.
        }

        getMap().getEntities().add(entity);
        showEntityInfo(entity);
        getController().resetEntities();
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
        showEntityInfo(null);
        setupLights();
        setupGeneralEditor();
        setupAnimationEditor();
        setupGeometryEditor();
        setupPathEditor();
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

        if (isAnimationMode()) {
            boolean removed = this.editAnimation.getMapUVs().removeIf(uvInfo -> uvInfo.getPolygon().equals(clickedPolygon));
            if (!removed)
                this.editAnimation.getMapUVs().add(new MAPUVInfo(getMap(), clickedPolygon));

            updateAnimation();
            return true;
        }

        Platform.runLater(this::setupGeometryEditor);
        return false;
    }

    /**
     * Start editing an animation.
     * @param animation The animation to edit.
     */
    public void editAnimation(MAPAnimation animation) {
        boolean match = animation.equals(this.editAnimation);
        cancelAnimationEdit();
        if (match)
            return;

        this.editAnimation = animation;
        animation.getMapUVs().forEach(uvInfo -> uvInfo.writeOver(getController(), MapMesh.ANIMATION_COLOR));
        this.animationMarker = getMesh().getManager().addMesh();
    }

    /**
     * Test if animation edit mode is active.
     * @return animationMode
     */
    public boolean isAnimationMode() {
        return this.editAnimation != null && this.animationMarker != null;
    }

    /**
     * Update animation data.
     */
    public void updateAnimation() {
        if (!isAnimationMode())
            return;

        getMesh().getManager().removeMesh(this.animationMarker);
        this.editAnimation.getMapUVs().forEach(uvInfo -> uvInfo.writeOver(getController(), MapMesh.ANIMATION_COLOR));
        this.animationMarker = getMesh().getManager().addMesh();
    }

    /**
     * Stop the current animation edit.
     */
    public void cancelAnimationEdit() {
        if (!isAnimationMode())
            return;

        getMesh().getManager().removeMesh(getAnimationMarker());
        this.animationMarker = null;
        this.editAnimation = null;
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