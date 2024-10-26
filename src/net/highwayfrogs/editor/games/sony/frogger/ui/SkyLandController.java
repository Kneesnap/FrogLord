package net.highwayfrogs.editor.games.sony.frogger.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.file.FroggerSkyLand;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.utils.FXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Controls the UI for viewing sky land.
 * Created by Kneesnap on 12/26/2019.
 */
public class SkyLandController extends SCFileEditorUIController<FroggerGameInstance, FroggerSkyLand> {
    @FXML private ImageView previewImage;
    @FXML private Button exportButton;
    private BufferedImage image;

    public SkyLandController(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(FroggerSkyLand newFile) {
        super.setTargetFile(newFile);
        updateUI(false);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        updateUI(true);
    }

    private void updateUI(boolean ignoreLoadingComplete) {
        if (!ignoreLoadingComplete && !isLoadingComplete())
            return;

        this.image = getFile() != null ? getFile().makeImage() : null;
        boolean hasImage = (this.image != null);
        this.previewImage.setDisable(!hasImage);
        this.exportButton.setDisable(!hasImage);
        if (hasImage)
            this.previewImage.setImage(FXUtils.toFXImage(this.image, false));
    }

    @FXML
    private void actionExport(ActionEvent evt) throws IOException {
        File file = FXUtils.promptFileSave(getGameInstance(), "Save the sky backdrop.", "sky_land", "Image File", "png");
        if (file != null)
            ImageIO.write(this.image, "png", file);
    }
}