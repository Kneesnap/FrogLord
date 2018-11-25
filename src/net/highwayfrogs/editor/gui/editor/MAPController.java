package net.highwayfrogs.editor.gui.editor;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.gui.GUIMain;

/**
 * Sets up the map editor.
 * Created by Kneesnap on 11/22/2018.
 */
public class MAPController extends EditorController<MAPFile> {
    @FXML private AnchorPane rootPane;

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
        setupNewWindow();
    }

    private void setupNewWindow() {
        Stage newStage = new Stage();

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
        newStage.setScene(newScene);

        newStage.setResizable(false);
        newStage.initOwner(GUIMain.MAIN_STAGE);
        newStage.show();
        newScene.setCamera(camera);

        newScene.setOnKeyPressed(event -> {
            System.out.println(event.getCode());
            if (event.getCode() == KeyCode.LEFT) {
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
