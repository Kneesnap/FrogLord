package net.highwayfrogs.editor.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Controls the VLO edit screen.
 * TODO: Transparency switch.
 * TODO: Control In-game width + height.
 * TODO: Show Flags?
 * Created by Kneesnap on 9/18/2018.
 */
public class VLOController {
    @FXML private CheckBox paddingCheckBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private ImageView imageView;
    @FXML private ListView<GameImage> imageList;
    @FXML private Label dimensionLabel;
    @FXML private Label ingameDimensionLabel;
    @FXML private Label idLabel;

    private VLOArchive vloFile;
    private GameImage selectedImage;

    /**
     * Display a VLO file.
     * @param vloArchive the VLO Archive to display.
     */
    public void loadVLO(VLOArchive vloArchive) {
        this.vloFile = vloArchive;

        ObservableList<GameImage> gameImages = FXCollections.observableArrayList(vloFile.getImages());
        imageList.setItems(gameImages);
        imageList.setCellFactory(param -> new AttachmentListCell());

        imageList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedImage = newValue;
            this.updateImage();
            this.updateImageInfo();
        });

        imageList.getSelectionModel().select(0);
    }

    private static class AttachmentListCell extends ListCell<GameImage> {
        @Override
        public void updateItem(GameImage image, boolean empty) {
            super.updateItem(image, empty);
            setText(empty ? null
                    : getIndex() + ": [" + image.getFullWidth() + ", " + image.getFullHeight() + "] (Tex ID: " + image.getTextureId() + ")");
        }
    }


    @FXML
    private void exportImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Specify the file to export this image as...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(new File("./"));

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);

        if (selectedFile == null)
            return; // Cancelled.

        try {
            ImageIO.write(toBufferedImage(), "png", selectedFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void importImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the image to import...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(new File("./"));

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancelled.

        try {
            this.selectedImage.replaceImage(ImageIO.read(selectedFile));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        updateImage();
        updateImageInfo();
    }

    @FXML
    private void exportAllImages(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the directory to export images to.");
        chooser.setInitialDirectory(new File("./"));

        File selectedFolder = chooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder == null)
            return; // Cancelled.

        GameImage originalImage = this.selectedImage;

        try {
            for (int i = 0; i < vloFile.getImages().size(); i++) {
                this.selectedImage = vloFile.getImages().get(i);
                File output = new File(selectedFolder, i + ".png");
                ImageIO.write(toBufferedImage(), "png", output);
                System.out.println("Exported image #" + i + ".");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            this.selectedImage = originalImage;
        }
    }

    @FXML
    private void onImageToggle(ActionEvent event) {
        System.out.println("Updating image.");
        updateImage();
    }

    /**
     * Update the info displayed for the image.
     */
    public void updateImageInfo() {
        dimensionLabel.setText("Archive Dimensions: [Width: " + this.selectedImage.getFullWidth() + ", Height: " + this.selectedImage.getFullHeight() + "]");
        ingameDimensionLabel.setText("In-Game Dimensions: [Width: " + this.selectedImage.getIngameWidth() + ", Height: " + this.selectedImage.getIngameHeight() + "]");
        idLabel.setText("Texture ID: " + this.selectedImage.getTextureId() + ", Flags: " + this.selectedImage.getFlags());
    }

    /**
     * Update the displayed image.
     */
    public void updateImage() {
        boolean hasImage = (this.selectedImage != null);
        imageView.setVisible(hasImage);
        if (hasImage)
            imageView.setImage(SwingFXUtils.toFXImage(toBufferedImage(), null));
    }

    private BufferedImage toBufferedImage() {
        return this.selectedImage.toBufferedImage(!this.paddingCheckBox.isSelected());
    }
}
