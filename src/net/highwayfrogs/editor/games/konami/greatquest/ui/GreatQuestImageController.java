package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestImageFile;
import net.highwayfrogs.editor.utils.FXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Manages image displays for Frogger The Great Quest.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestImageController extends GreatQuestFileEditorUIController<GreatQuestImageFile> {
    @FXML private ImageView imageView;

    public GreatQuestImageController(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(GreatQuestImageFile imageFile) {
        super.setTargetFile(imageFile);
        updateImage();
    }

    @FXML
    @SneakyThrows
    private void exportImage(ActionEvent event) {
        File selectedFile = FXUtils.promptFileSave(getGameInstance(), "Specify the file to export this image as...", null, "Image Files", "png");
        if (selectedFile != null)
            ImageIO.write(getFile().getImage(), "png", selectedFile);
    }

    @FXML
    @SneakyThrows
    private void importImage(ActionEvent event) {
        /*File selectedFile = Utils.promptFileOpen(getGameInstance(), "Select the image to import...", "Image Files", "png");
        if (selectedFile == null)
            return; // Cancelled.

        getFile().replaceImage(ImageIO.read(selectedFile));
        updateImage();*/ // TODO: Implement.
        FXUtils.makePopUp("Importing images is not supported yet.", AlertType.WARNING);
    }

    /**
     * Update the displayed image.
     */
    public void updateImage() {
        BufferedImage image = getFile().getImage();
        boolean hasImage = (image != null);
        this.imageView.setVisible(hasImage);

        if (hasImage) {
            this.imageView.setFitWidth(image.getWidth());
            this.imageView.setFitHeight(image.getHeight());
            this.imageView.setImage(FXUtils.toFXImage(image, false));
        }
    }
}