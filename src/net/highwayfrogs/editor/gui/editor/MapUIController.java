package net.highwayfrogs.editor.gui.editor;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Camera;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.view.MapMesh;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

/**
 * Manages the UI which is displayed when viewing Frogger maps.
 * Created by AndyEder on 1/4/2019.
 */
@Getter
public class MapUIController implements Initializable {
    // Useful constants and settings
    private static final int MAP_VIEW_SCALE = 10000;
    @Getter public static IntegerProperty propertyMapViewScale = new SimpleIntegerProperty(MAP_VIEW_SCALE);

    private static final double MAP_VIEW_FAR_CLIP = 15000.0;

    private static final double ROTATION_SPEED = 0.35D;
    @Getter public static DoubleProperty propertyRotationSpeed = new SimpleDoubleProperty(ROTATION_SPEED);

    private static final double SCROLL_SPEED = 4;
    @Getter public static DoubleProperty propertyScrollSpeed = new SimpleDoubleProperty(SCROLL_SPEED);

    private static final double TRANSLATE_SPEED = 10;
    @Getter public static DoubleProperty propertyTranslateSpeed = new SimpleDoubleProperty(TRANSLATE_SPEED);

    private static final int VERTEX_SPEED = 3;
    @Getter public static IntegerProperty propertyVertexSpeed = new SimpleIntegerProperty(VERTEX_SPEED);

    private static final double SPEED_DOWN_MULTIPLIER = 0.25;
    @Getter public static DoubleProperty propertySpeedDownMultiplier = new SimpleDoubleProperty(SPEED_DOWN_MULTIPLIER);

    private static final double SPEED_UP_MULTIPLIER = 4.0;
    @Getter public static DoubleProperty propertySpeedUpMultiplier = new SimpleDoubleProperty(SPEED_UP_MULTIPLIER);

    // Baseline UI components
    @FXML private AnchorPane anchorPaneUIRoot;
    @FXML private Accordion accordionLeft;

    // Level Editor /  Information pane
    @FXML private TitledPane titledPaneInformation;
    @FXML private Label labelLevelThemeName;
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

    // Camera pane
    @FXML private TitledPane titledPaneCamera;
    @FXML private GridPane gridPaneCameraPos;
    @FXML private TextField textFieldCamNearClip;
    @FXML private TextField textFieldCamFarClip;
    @FXML private TextField textFieldCamPosX;
    @FXML private TextField textFieldCamPosY;
    @FXML private TextField textFieldCamPosZ;

    // Mesh pane
    @FXML private CheckBox checkBoxShowMesh;
    @FXML private ComboBox<DrawMode> comboBoxMeshDrawMode;
    @FXML private ComboBox<CullFace> comboBoxMeshCullFace;

    @FXML private TextField textFieldMeshPosX;
    @FXML private TextField textFieldMeshPosY;
    @FXML private TextField textFieldMeshPosZ;
    @FXML private TextField textFieldMeshRotX;
    @FXML private TextField textFieldMeshRotY;
    @FXML private TextField textFieldMeshRotZ;

    private static final NumberStringConverter NUM_TO_STRING_CONVERTER = new NumberStringConverter(new DecimalFormat("####0.000000"));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accordionLeft.setExpandedPane(titledPaneCamera);
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
    public static double getSpeedModifier(Boolean isCtrlDown, Boolean isAltDown, double defaultValue)
    {
        if (isCtrlDown) {
            return (defaultValue * propertySpeedDownMultiplier.get());
        }
        else if (isAltDown) {
            return (defaultValue * propertySpeedUpMultiplier.get());
        }

        return defaultValue;
    }

    /**
     * Primary function (entry point) for setting up data / control bindings, etc. (May be broken out and tidied up later in development).
     */
    public void setupBindings(SubScene subScene3D, MapMesh mapMesh, MeshView meshView, Rotate rotX, Rotate rotY, Rotate rotZ, Camera camera) {

        camera.setFarClip(MAP_VIEW_FAR_CLIP);
        subScene3D.setFill(Color.GRAY);
        subScene3D.setCamera(camera);

        // Set informational bindings and editor bindings
        labelLevelThemeName.setText(mapMesh.getMap().getTheme().toString());
        colorPickerLevelBackground.setValue((Color)subScene3D.getFill());
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

        textFieldMeshRotX.textProperty().bindBidirectional(rotX.angleProperty(), NUM_TO_STRING_CONVERTER);
        textFieldMeshRotY.textProperty().bindBidirectional(rotY.angleProperty(), NUM_TO_STRING_CONVERTER);
        textFieldMeshRotZ.textProperty().bindBidirectional(rotZ.angleProperty(), NUM_TO_STRING_CONVERTER);
    }
}
