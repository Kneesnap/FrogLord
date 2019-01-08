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

    public void setupBindings(MapMesh mapMesh, MeshView meshView, Rotate rotX, Rotate rotY, Rotate rotZ, Camera camera) {

        // Set informational bindings
        labelLevelName.setText(mapMesh.getMap().getTheme().toString());

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
