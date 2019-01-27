package net.highwayfrogs.editor.gui.editor;

import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.input.GestureEvent;
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
import net.highwayfrogs.editor.file.map.MAPEditorGUI;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.animation.MAPAnimation;
import net.highwayfrogs.editor.file.map.animation.MAPUVInfo;
import net.highwayfrogs.editor.file.map.entity.Entity;
import net.highwayfrogs.editor.file.map.form.FormBook;
import net.highwayfrogs.editor.file.map.light.Light;
import net.highwayfrogs.editor.file.map.path.Path;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.mesh.MeshData;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Manages the UI which is displayed when viewing Frogger maps.
 * Created by AndyEder on 1/4/2019.
 */
@Getter
public class MapUIController implements Initializable {
    // Useful constants and settings
    private static final int MAP_VIEW_SCALE = 10000;
    @Getter private static IntegerProperty propertyMapViewScale = new SimpleIntegerProperty(MAP_VIEW_SCALE);

    private static final double MAP_VIEW_FAR_CLIP = 15000.0;

    private static final double ROTATION_SPEED = 0.35D;
    @Getter private static DoubleProperty propertyRotationSpeed = new SimpleDoubleProperty(ROTATION_SPEED);

    private static final double SCROLL_SPEED = 4;
    @Getter private static DoubleProperty propertyScrollSpeed = new SimpleDoubleProperty(SCROLL_SPEED);

    private static final double TRANSLATE_SPEED = 10;
    @Getter private static DoubleProperty propertyTranslateSpeed = new SimpleDoubleProperty(TRANSLATE_SPEED);

    private static final int VERTEX_SPEED = 3;
    @Getter private static IntegerProperty propertyVertexSpeed = new SimpleIntegerProperty(3);

    private static final double SPEED_DOWN_MULTIPLIER = 0.25;
    @Getter private static DoubleProperty propertySpeedDownMultiplier = new SimpleDoubleProperty(SPEED_DOWN_MULTIPLIER);

    private static final double SPEED_UP_MULTIPLIER = 4.0;
    @Getter private static DoubleProperty propertySpeedUpMultiplier = new SimpleDoubleProperty(SPEED_UP_MULTIPLIER);

    private MAPController controller;

    // Baseline UI components
    @FXML private AnchorPane anchorPaneUIRoot;
    @FXML private Accordion accordionLeft;

    // Control Settings Pane.
    @FXML private TitledPane titledPaneInformation;
    @FXML private CheckBox checkBoxShowMesh;
    @FXML private ComboBox<DrawMode> comboBoxMeshDrawMode;
    @FXML private ComboBox<CullFace> comboBoxMeshCullFace;
    @FXML private ColorPicker colorPickerLevelBackground;
    @FXML private TextField textFieldSpeedRotation;
    @FXML private Button btnResetSpeedRotation;
    @FXML private TextField textFieldSpeedScroll;
    @FXML private Button btnResetSpeedScroll;
    @FXML private TextField textFieldSpeedTranslate;
    @FXML private Button btnResetSpeedTranslate;
    @FXML private TextField textFieldSpeedDownMultiplier;
    @FXML private Button btnResetSpeedDownMultiplier;
    @FXML private TextField textFieldSpeedUpMultiplier;
    @FXML private Button btnResetSpeedUpMultiplier;
    @FXML private TextField textFieldCamNearClip;
    @FXML private TextField textFieldCamFarClip;
    @FXML private TextField textFieldCamPosX;
    @FXML private TextField textFieldCamPosY;
    @FXML private TextField textFieldCamPosZ;
    @FXML private TextField textFieldMeshPosX;
    @FXML private TextField textFieldMeshPosY;
    @FXML private TextField textFieldMeshPosZ;

    // General pane.
    @FXML private TitledPane generalPane;
    @FXML private GridPane generalGridPane;
    private GUIEditorGrid generalEditor;

    // Entity pane
    @FXML private TitledPane entityPane;
    @FXML private GridPane entityGridPane;
    private MAPEditorGUI entityEditor;

    // Light Pane.
    @FXML private TitledPane lightPane;
    @FXML private GridPane lightGridPane;
    private GUIEditorGrid lightEditor;

    // Animation pane.
    @FXML private TitledPane animationPane;
    @FXML private GridPane animationGridPane;
    private GUIEditorGrid animationEditor;

    // Path pane.
    @FXML private TitledPane pathPane;
    @FXML private GridPane pathGridPane;
    private GUIEditorGrid pathEditor;

    // Geometry pane.
    @FXML private TitledPane geometryPane;
    @FXML private GridPane geometryGridPane;
    private GUIEditorGrid geometryEditor;
    private MeshData looseMeshData;

    private MAPAnimation editAnimation;
    private MeshData animationMarker;

    private Consumer<MAPPolygon> onSelect;
    private Runnable cancelSelection;

    private static final NumberStringConverter NUM_TO_STRING_CONVERTER = new NumberStringConverter(new DecimalFormat("####0.000000"));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accordionLeft.setExpandedPane(generalPane);
        entityEditor = new MAPEditorGUI(entityGridPane, this);
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
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public static double getSpeedModifier(GestureEvent event, Property<Number> property) {
        return getSpeedModifier(event.isControlDown(), event.isAltDown(), property.getValue().doubleValue());
    }

    /**
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public static double getSpeedModifier(MouseEvent event, Property<Number> property) {
        return getSpeedModifier(event.isControlDown(), event.isAltDown(), property.getValue().doubleValue());
    }

    /**
     * Utility function affording the user different levels of speed control through multipliers.
     */
    public static double getSpeedModifier(Boolean isCtrlDown, Boolean isAltDown, double defaultValue) {
        double multiplier = 1;

        if (isCtrlDown) {
            multiplier = propertySpeedDownMultiplier.get();
        } else if (isAltDown) {
            multiplier = propertySpeedUpMultiplier.get();
        }

        return defaultValue * multiplier;
    }

    /**
     * Sets up the lighting editor.
     */
    public void setupLights() {
        if (lightEditor == null)
            lightEditor = new GUIEditorGrid(lightGridPane);

        lightEditor.clearEditor();
        for (int i = 0; i < getController().getFile().getLights().size(); i++) {
            lightEditor.addBoldLabel("Light #" + (i + 1) + ":");
            getController().getFile().getLights().get(i).makeEditor(lightEditor);

            final int tempIndex = i;
            lightEditor.addButton("Remove Light #" + (i + 1), () -> {
                getController().getFile().getLights().remove(tempIndex);
                setupLights(); // Reload this.
            });
        }

        lightEditor.addButton("Add Light", () -> {
            getController().getFile().getLights().add(new Light());
            setupLights(); // Reload lights.
        });
    }

    /**
     * Sets up the general editor.
     */
    public void setupGeneralEditor() {
        if (generalEditor == null)
            generalEditor = new GUIEditorGrid(generalGridPane);

        generalEditor.clearEditor();
        getController().getFile().setupEditor(generalEditor);
    }

    /**
     * Setup the animation editor.
     */
    public void setupAnimationEditor() {
        if (animationEditor == null)
            animationEditor = new GUIEditorGrid(animationGridPane);

        animationEditor.clearEditor();

        for (int i = 0; i < getMap().getMapAnimations().size(); i++) {
            animationEditor.addBoldLabel("Animation #" + (i + 1));
            getMap().getMapAnimations().get(i).setupEditor(this, animationEditor);

            final int tempIndex = i;
            animationEditor.addButton("Delete Animation #" + (i + 1), () -> {
                getMap().getMapAnimations().remove(tempIndex);
                setupAnimationEditor();
            });
        }

        animationEditor.addButton("Add Animation", () -> {
            getMap().getMapAnimations().add(new MAPAnimation(getMap()));
            setupAnimationEditor();
        });
    }

    /**
     * Setup the path editor.
     */
    public void setupPathEditor() {
        if (this.pathEditor == null)
            this.pathEditor = new GUIEditorGrid(this.pathGridPane);

        this.pathEditor.clearEditor();

        for (int i = 0; i < getMap().getPaths().size(); i++) {
            final int tempIndex = i;

            this.pathEditor.addBoldLabel("Path #" + (i + 1) + ":"); //TODO: It'd be nice to separate this into collapsible title panes.
            getMap().getPaths().get(i).setupEditor(this, this.pathEditor);
            this.pathEditor.addButton("Remove Path #" + (i + 1), () -> {
                getMap().getPaths().remove(tempIndex);
                setupPathEditor();
            });
        }

        this.pathEditor.addButton("Add Path", () -> {
            getMap().getPaths().add(new Path());
            setupPathEditor();
        });
    }

    /**
     * Setup the map geometry editor.
     */
    public void setupGeometryEditor() {
        if (this.geometryEditor == null)
            this.geometryEditor = new GUIEditorGrid(geometryGridPane);

        this.geometryEditor.clearEditor();
        this.geometryEditor.addBoldLabel("Collision Grid:");
        this.geometryEditor.addButton("Edit Grid", () -> GridController.openGridEditor(this));
        this.geometryEditor.addCheckBox("Toggle Polygon Visibility", this.looseMeshData != null, this::updateVisibility);
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
        entityEditor.clearEditor();
        if (entity == null) {
            entityEditor.addBoldLabel("There is no entity selected.");
            entityEditor.addButton("New Entity", () -> addNewEntity(FormBook.values()[0]));
            return;
        }

        entityEditor.addBoldLabel("General Information:");
        entityEditor.addLabel("Entity Type", entity.getFormBook().getEntity().name());

        entityEditor.addEnumSelector("Form Type", entity.getFormBook(), FormBook.values(), false, newBook -> {
            entity.setFormBook(newBook);
            showEntityInfo(entity); // Update entity type.
        });

        entityEditor.addIntegerField("Entity ID", entity.getUniqueId(), entity::setUniqueId, null);
        entityEditor.addIntegerField("Form ID", entity.getFormGridId(), entity::setFormGridId, null);
        entityEditor.addIntegerField("Flags", entity.getFlags(), entity::setFlags, null);

        // Populate Entity Data.
        entityEditor.addBoldLabel("Entity Data:");
        if (entity.getEntityData() != null)
            entity.getEntityData().addData(this.entityEditor);

        // Populate Script Data.
        entityEditor.addBoldLabel("Script Data:");
        if (entity.getScriptData() != null)
            entity.getScriptData().addData(this.entityEditor);

        entityEditor.addButton("Remove Entity", () -> {
            getMap().getEntities().remove(entity);
            getController().resetEntities();
            showEntityInfo(null); // Don't show the entity we just deleted.
        });

        entityEditor.addButton("New Entity", () -> addNewEntity(entity.getFormBook()));

        entityPane.setExpanded(true);
    }

    private void addNewEntity(FormBook book) {
        Entity newEntity = new Entity(getMap(), book);
        getMap().getEntities().add(newEntity);
        showEntityInfo(newEntity);
        getController().resetEntities();
    }

    /**
     * Primary function (entry point) for setting up data / control bindings, etc. (May be broken out and tidied up later in development).
     */
    public void setupBindings(MAPController controller, SubScene subScene3D, MeshView meshView) {
        this.controller = controller;
        PerspectiveCamera camera = controller.getCamera();

        camera.setFarClip(MAP_VIEW_FAR_CLIP);
        subScene3D.setFill(Color.GRAY);
        subScene3D.setCamera(camera);

        // Set informational bindings and editor bindings
        colorPickerLevelBackground.setValue((Color) subScene3D.getFill());
        subScene3D.fillProperty().bind(colorPickerLevelBackground.valueProperty());

        textFieldSpeedRotation.textProperty().bindBidirectional(propertyRotationSpeed, NUM_TO_STRING_CONVERTER);
        textFieldSpeedScroll.textProperty().bindBidirectional(propertyScrollSpeed, NUM_TO_STRING_CONVERTER);
        textFieldSpeedTranslate.textProperty().bindBidirectional(propertyTranslateSpeed, NUM_TO_STRING_CONVERTER);
        textFieldSpeedDownMultiplier.textProperty().bindBidirectional(propertySpeedDownMultiplier, NUM_TO_STRING_CONVERTER);
        textFieldSpeedUpMultiplier.textProperty().bindBidirectional(propertySpeedUpMultiplier, NUM_TO_STRING_CONVERTER);

        btnResetSpeedRotation.setOnAction(e -> propertyRotationSpeed.set(ROTATION_SPEED));
        btnResetSpeedScroll.setOnAction(e -> propertyScrollSpeed.set(SCROLL_SPEED));
        btnResetSpeedTranslate.setOnAction(e -> propertyTranslateSpeed.set(TRANSLATE_SPEED));
        btnResetSpeedDownMultiplier.setOnAction(e -> propertySpeedDownMultiplier.set(SPEED_DOWN_MULTIPLIER));
        btnResetSpeedUpMultiplier.setOnAction(e -> propertySpeedUpMultiplier.set(SPEED_UP_MULTIPLIER));

        // Set camera bindings
        textFieldCamNearClip.textProperty().bindBidirectional(camera.nearClipProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamFarClip.textProperty().bindBidirectional(camera.farClipProperty(), NUM_TO_STRING_CONVERTER);

        textFieldCamPosX.textProperty().bindBidirectional(camera.translateXProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamPosY.textProperty().bindBidirectional(camera.translateYProperty(), NUM_TO_STRING_CONVERTER);
        textFieldCamPosZ.textProperty().bindBidirectional(camera.translateZProperty(), NUM_TO_STRING_CONVERTER);

        // Set mesh view bindings
        checkBoxShowMesh.selectedProperty().bindBidirectional(meshView.visibleProperty());

        comboBoxMeshDrawMode.getItems().setAll(DrawMode.values());
        comboBoxMeshDrawMode.valueProperty().bindBidirectional(meshView.drawModeProperty());

        comboBoxMeshCullFace.getItems().setAll(CullFace.values());
        comboBoxMeshCullFace.valueProperty().bindBidirectional(meshView.cullFaceProperty());

        textFieldMeshPosX.textProperty().bindBidirectional(meshView.translateXProperty(), NUM_TO_STRING_CONVERTER);
        textFieldMeshPosY.textProperty().bindBidirectional(meshView.translateYProperty(), NUM_TO_STRING_CONVERTER);
        textFieldMeshPosZ.textProperty().bindBidirectional(meshView.translateZProperty(), NUM_TO_STRING_CONVERTER);

        // Must be called after MAPController is passed.
        showEntityInfo(null);
        setupLights();
        setupGeneralEditor();
        setupAnimationEditor();
        setupGeometryEditor();
        setupPathEditor();
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
        if (event.getCode() == KeyCode.ESCAPE && !getController().isPolygonSelected() && onSelect != null) {
            onSelect = null;
            if (cancelSelection != null) {
                cancelSelection.run();
                cancelSelection = null;
            }

            return true;
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