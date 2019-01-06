package net.highwayfrogs.editor.gui.editor;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Camera;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.util.converter.NumberStringConverter;
import lombok.Getter;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import net.highwayfrogs.editor.file.map.view.MapMesh;

/**
 * Manages the UI which is displayed when viewing 3D levelsup while saving a MWD.
 * Created by AndyEder on 1/4/2019.
 */
@Getter
public class MapUIController implements Initializable {
    @FXML private AnchorPane anchorPaneUIRoot;
    @FXML private Accordion accordionLeft;

    // Information pane
    @FXML private TitledPane titledPaneInformation;
    @FXML private Label labelLevelName;

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

    public MapUIController() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        accordionLeft.setExpandedPane(titledPaneCamera);
    }

    public double uiRootPaneWidth() { return anchorPaneUIRoot.getPrefWidth(); }
    public double uiRootPaneHeight() { return anchorPaneUIRoot.getPrefHeight(); }

    public void setupBindings(MapMesh mapMesh, MeshView meshView, Rotate rotX, Rotate rotY, Rotate rotZ, Camera camera) {
        // Setup number format and number->string converter
        NumberFormat decFormat = new DecimalFormat("####0.000000");
        NumberStringConverter numStringConverter = new NumberStringConverter(decFormat);

        // Set informational bindings
        labelLevelName.setText(mapMesh.getMap().getTheme().toString());

        // Set camera bindings
        textFieldCamNearClip.textProperty().bindBidirectional(camera.nearClipProperty(), numStringConverter);
        textFieldCamFarClip.textProperty().bindBidirectional(camera.farClipProperty(), numStringConverter);

        textFieldCamPosX.textProperty().bindBidirectional(camera.translateXProperty(), numStringConverter);
        textFieldCamPosY.textProperty().bindBidirectional(camera.translateYProperty(), numStringConverter);
        textFieldCamPosZ.textProperty().bindBidirectional(camera.translateZProperty(), numStringConverter);

        // Set mesh view bindings
        checkBoxShowMesh.selectedProperty().bindBidirectional(meshView.visibleProperty());

        comboBoxMeshDrawMode.getItems().setAll(DrawMode.values());
        comboBoxMeshDrawMode.valueProperty().bindBidirectional(meshView.drawModeProperty());

        comboBoxMeshCullFace.getItems().setAll(CullFace.values());
        comboBoxMeshCullFace.valueProperty().bindBidirectional(meshView.cullFaceProperty());

        textFieldMeshPosX.textProperty().bindBidirectional(meshView.translateXProperty(), numStringConverter);
        textFieldMeshPosY.textProperty().bindBidirectional(meshView.translateYProperty(), numStringConverter);
        textFieldMeshPosZ.textProperty().bindBidirectional(meshView.translateZProperty(), numStringConverter);

        textFieldMeshRotX.textProperty().bindBidirectional(rotX.angleProperty(), numStringConverter);
        textFieldMeshRotY.textProperty().bindBidirectional(rotY.angleProperty(), numStringConverter);
        textFieldMeshRotZ.textProperty().bindBidirectional(rotZ.angleProperty(), numStringConverter);
    }
}
