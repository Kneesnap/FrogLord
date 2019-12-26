package net.highwayfrogs.editor.gui.editor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import net.highwayfrogs.editor.file.map.SkyLand;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Controls the UI for viewing sky land.
 * Created by Kneesnap on 12/26/2019.
 */
public class SkyLandController extends EditorController<SkyLand> {
    @FXML private ImageView previewImage;
    @FXML private Button exportButton;
    private BufferedImage image;

    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);

        this.image = getFile().makeImage();
        boolean hasImage = (this.image != null);
        this.previewImage.setDisable(!hasImage);
        this.exportButton.setDisable(!hasImage);
        if (hasImage)
            this.previewImage.setImage(Utils.toFXImage(this.image, false));
    }

    @FXML
    private void actionExport(ActionEvent evt) throws IOException {
        File file = Utils.promptFileSave("Save the sky backdrop.", "sky_land", "Image File", "png");
        if (file != null)
            ImageIO.write(this.image, "png", file);
    }

}

