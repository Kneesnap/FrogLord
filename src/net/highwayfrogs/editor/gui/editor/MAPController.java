package net.highwayfrogs.editor.gui.editor;

import javafx.fxml.FXML;
import javafx.scene.PerspectiveCamera;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import net.highwayfrogs.editor.file.map.MAPFile;

/**
 * Sets up the map editor.
 * Created by Kneesnap on 11/22/2018.
 */
public class MAPController extends EditorController<MAPFile> {
    @FXML private AnchorPane rootPane;

    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);

        Box box = new Box(100, 100, 100);
        box.setTranslateX(250);
        box.setTranslateY(50);
        box.setTranslateZ(100);
        box.setRotate(30D);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.TAN);
        box.setMaterial(material);

        editorRoot.getChildren().add(box);

        PerspectiveCamera camera = new PerspectiveCamera(false);
        editorRoot.getScene().setCamera(camera);
    }
}
