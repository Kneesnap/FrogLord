package net.highwayfrogs.editor.games.sony.beastwars.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsInstance;
import net.highwayfrogs.editor.games.sony.beastwars.BeastWarsTexFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.gui.texture.BufferedImageWrapper;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Allows editing Beast Wars .TEX files.
 * Created by Kneesnap on 9/13/2023.
 */
public class TexController extends SCFileEditorUIController<BeastWarsInstance, BeastWarsTexFile> {
    @FXML private ImageView imageView;
    @FXML private ListView<BufferedImageWrapper> imageList;
    @FXML private Button backButton;

    @Getter private BufferedImage selectedImage;

    public TexController(BeastWarsInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(BeastWarsTexFile tex) {
        super.setTargetFile(tex);

        this.imageList.setItems(FXCollections.observableArrayList(tex.getImages()));
        this.imageList.setCellFactory(param -> new AbstractAttachmentCell<>((image, index) -> image != null ? "Image #" + index : null));
        this.imageList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> selectImage(newValue, false));
        this.imageList.getSelectionModel().select(0);
    }

    @Override
    public void setParentWadFile(WADFile wadFile) {
        super.setParentWadFile(wadFile);
        this.backButton.setVisible(wadFile != null);
    }

    @FXML
    @SneakyThrows
    private void exportImage(ActionEvent event) {
        File selectedFile = Utils.promptFileSave(getGameInstance(), "Specify the file to export this image as...", null, "Image Files", "png");
        if (selectedFile != null)
            ImageIO.write(this.selectedImage, "png", selectedFile);
    }

    @FXML
    @SneakyThrows
    private void importImage(ActionEvent event) {
        File selectedFile = Utils.promptFileOpen(getGameInstance(), "Select the image to import...", "Image Files", "png");
        if (selectedFile == null)
            return; // Cancelled.

        BufferedImage importImage = ImageIO.read(selectedFile);
        if (importImage.getWidth() != BeastWarsTexFile.TEXTURE_DIMENSION || importImage.getHeight() != BeastWarsTexFile.TEXTURE_DIMENSION) {
            Utils.makePopUp("Only images of size " + BeastWarsTexFile.TEXTURE_DIMENSION + "x" + BeastWarsTexFile.TEXTURE_DIMENSION + " pixels can be imported. This image is " + importImage.getWidth() + "x" + importImage.getHeight(), AlertType.ERROR);
            return;
        }

        try {
            getFile().replaceImage(this.selectedImage, importImage);
        } catch (Throwable th) {
            handleError(th, true, "Failed to replace the image.");
        }

        updateDisplay();
    }

    @FXML
    private void exportAllImages(ActionEvent event) {
        File directory = Utils.promptChooseDirectory(getGameInstance(), "Select the directory to export images to.", true);
        if (directory == null)
            return; // Cancelled.

        try {
            for (int i = 0; i < getFile().getImages().size(); i++) {
                File output = new File(directory, i + ".png");
                ImageIO.write(getFile().getImages().get(i).getImage(), "png", output);
                getLogger().info("Exported image #" + i + ".");
            }
        } catch (IOException ex) {
            handleError(ex, true, "Failed to export all images.");
        }
    }

    @FXML
    @SneakyThrows
    private void importAllImages(ActionEvent event) {
        File selectedFolder = Utils.promptChooseDirectory(getGameInstance(), "Select the directory to import images from.", true);
        if (selectedFolder == null)
            return; // Cancelled.

        int importedFiles = 0;
        for (File file : Utils.listFiles(selectedFolder)) {
            String name = Utils.stripExtension(file.getName());
            if (!Utils.isInteger(name))
                continue;

            int id = Integer.parseInt(name);
            if (id >= 0 && id < getFile().getImages().size()) {
                try {
                    getFile().setImage(id, ImageIO.read(file));
                } catch (Throwable th) {
                    handleError(th, true, "Failed to replace the image with %s.", file.getName());
                }
                importedFiles++;
            }

        }

        getLogger().info("Imported " + importedFiles + " images.");
        updateDisplay();
    }

    @FXML
    private void onImageToggle(ActionEvent event) {
        updateImage();
    }

    @FXML
    private void returnToWad(ActionEvent event) {
        tryReturnToParentWadFile();
    }

    /**
     * Select a particular image in the vlo.
     * @param image The image to select.
     */
    public void selectImage(BufferedImageWrapper image, boolean forceSelect) {
        if (image == null)
            return;

        this.selectedImage = image.getImage();
        this.updateDisplay();

        if (forceSelect) {
            this.imageList.getSelectionModel().select(image);
            this.imageList.scrollTo(image);
        }
    }

    /**
     * Update the displayed image.
     */
    public void updateImage() {
        boolean hasImage = (this.selectedImage != null);
        this.imageView.setVisible(hasImage);

        if (hasImage) {
            BufferedImage image = this.selectedImage;

            this.imageView.setFitWidth(image.getWidth());
            this.imageView.setFitHeight(image.getHeight());
            this.imageView.setImage(Utils.toFXImage(image, false));
        }
    }

    /**
     * Update this GUI.
     */
    public void updateDisplay() {
        updateImage();
    }
}