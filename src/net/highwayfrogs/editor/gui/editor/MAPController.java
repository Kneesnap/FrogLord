package net.highwayfrogs.editor.gui.editor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.MAPTheme;
import net.highwayfrogs.editor.gui.GUIMain;

import java.util.List;

/**
 * Sets up the map editor.
 * Created by Kneesnap on 11/22/2018.
 */
public class MAPController extends EditorController<MAPFile> {
    @FXML private Label themeIdLabel;
    @FXML private Label startPosLabel;
    @FXML private Label cameraSourceLabel;
    @FXML private Label cameraTargetLabel;
    @FXML private Label basePointLabel; // This is the bottom left of the map group grid.

    @FXML private Label pathCountLabel;
    @FXML private Label formCountLabel;
    @FXML private Label entityCountLabel;

    @FXML private Label gridSquareLabel;
    @FXML private Label gridStackLabel;
    @FXML private Label gridCountLabel;
    @FXML private Label gridLengthLabel;
    @FXML private Label groupLabel;
    @FXML private Label groupCountLabel;
    @FXML private Label groupLengthLabel;

    @FXML private Label zoneCountLabel;
    @FXML private Label lightCountLabel;
    @FXML private Label vertexCountLabel;
    @FXML private Label polygonCountLabel;
    @FXML private Label mapAnimCountLabel;

    private double oldMouseX;
    private double oldMouseY;
    private double mouseX;
    private double mouseY;
    private double mouseXDelta;
    private double mouseYDelta;

    private static final double ROTATION_SPEED = 0.35D;

    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);
        updateLabels();
    }

    private void updateLabels() {
        MAPFile map = getFile();

        themeIdLabel.setText("Theme: " + MAPTheme.values()[map.getThemeId()].getInternalName() + " [" + map.getThemeId() + "]");
        startPosLabel.setText("Start Pos: (" + map.getStartXTile() + ", " + map.getStartYTile() + ") Rotation: " + map.getStartRotation());
        cameraSourceLabel.setText("Camera Source: (" + map.getCameraSourceOffset().toCoordinateString() + ")");
        cameraTargetLabel.setText("Camera Target: (" + map.getCameraTargetOffset().toCoordinateString() + ")");
        basePointLabel.setText("Base Point: (" + map.getBasePoint().toCoordinateString() + ")");

        // Labels in entity section.
        pathCountLabel.setText("Paths: " + map.getPaths().size());
        formCountLabel.setText("Forms: " + map.getForms().size());
        entityCountLabel.setText("Entities: " + map.getEntities().size());

        // Labels in environment section.
        zoneCountLabel.setText("Zones: " + map.getZones().size());
        lightCountLabel.setText("Lights: " + map.getLights().size());
        vertexCountLabel.setText("Vertices: " + map.getVertexes().size());
        polygonCountLabel.setText("Polygons: " + map.getCachedPolygons().values().stream().mapToInt(List::size).sum());
        mapAnimCountLabel.setText("Animations: " + map.getMapAnimations().size());

        // Grid
        gridStackLabel.setText("Grid Stacks: " + map.getGridStacks().size());
        gridSquareLabel.setText("Grid Squares: " + map.getGridSquares().size());
        gridCountLabel.setText("Count: [" + map.getGridXCount() + ", " + map.getGridZCount() + "]");
        gridLengthLabel.setText("Length: [" + map.getGridXLength() + ", " + map.getGridZLength() + "]");

        groupLabel.setText("Groups: " + map.getGroups().size());
        groupCountLabel.setText("Count: [" + map.getGroupXCount() + ", " + map.getGroupZCount() + "]");
        groupLengthLabel.setText("Length: [" + map.getGroupXLength() + ", " + map.getGroupZLength() + "]");
    }

    @FXML
    private void onMapButtonClicked(ActionEvent event) {
        setupMapViewer();
    }

    private void setupMapViewer() {
        Stage overrideStage = GUIMain.MAIN_STAGE;

        Box box = new Box(100, 100, 100);
        box.setRotate(30D);
        box.setMaterial(new PhongMaterial(Color.TAN));

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFarClip(Double.MAX_VALUE);
        camera.setTranslateZ(-500);

        Group cameraGroup = new Group();
        cameraGroup.getChildren().add(box);
        cameraGroup.getChildren().add(camera);

        Rotate rotX = new Rotate();
        Rotate rotY = new Rotate();
        rotX.setAxis(Rotate.X_AXIS);
        rotY.setAxis(Rotate.Y_AXIS);
        box.getTransforms().add(rotX);
        box.getTransforms().add(rotY);

        Scene newScene = new Scene(cameraGroup, 400, 400, true);
        newScene.setFill(Color.GRAY);

        Scene oldScene = GUIMain.MAIN_STAGE.getScene();
        overrideStage.setScene(newScene);
        newScene.setCamera(camera);

        newScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                overrideStage.setScene(oldScene);
            } else if (event.getCode() == KeyCode.LEFT) {
                camera.setTranslateX(camera.getTranslateX() - 5);
            } else if (event.getCode() == KeyCode.RIGHT) {
                camera.setTranslateX(camera.getTranslateX() + 5);
            } else if (event.getCode() == KeyCode.UP) {
                camera.setTranslateY(camera.getTranslateY() - 5);
            } else if (event.getCode() == KeyCode.DOWN) {
                camera.setTranslateY(camera.getTranslateY() + 5);
            } else if (event.getCode() == KeyCode.E) {
                camera.setTranslateZ(camera.getTranslateZ() - 10);
            } else if (event.getCode() == KeyCode.Q) {
                camera.setTranslateZ(camera.getTranslateZ() + 10);
            } else {
                System.out.println(event.getCode());
            }
        });

        newScene.setOnMousePressed(e -> {
            mouseX = e.getSceneX();
            oldMouseX = e.getSceneX();
            mouseY = e.getSceneY();
            oldMouseY = e.getSceneY();
        });

        newScene.setOnMouseDragged(e -> {
            oldMouseX = mouseX;
            oldMouseY = mouseY;
            mouseX = e.getSceneX();
            mouseY = e.getSceneY();
            mouseXDelta = (mouseX - oldMouseX);
            mouseYDelta = (mouseY - oldMouseY);

            if (e.isPrimaryButtonDown()) {
                rotX.setAngle(rotX.getAngle() + (mouseYDelta * ROTATION_SPEED));
                rotY.setAngle(rotY.getAngle() - (mouseXDelta * ROTATION_SPEED));
            }
        });
    }
}
