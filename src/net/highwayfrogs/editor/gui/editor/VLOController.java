package net.highwayfrogs.editor.gui.editor;

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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.Getter;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.gui.GUIMain;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Controls the VLO edit screen.
 * Created by Kneesnap on 9/18/2018.
 */
public class VLOController extends EditorController<VLOArchive> {
    @FXML private CheckBox paddingCheckBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private CheckBox sizeCheckBox;
    @FXML private ImageView imageView;
    @FXML private ListView<GameImage> imageList;
    @FXML private Label dimensionLabel;
    @FXML private Label ingameDimensionLabel;
    @FXML private Label idLabel;
    @FXML private VBox flagBox;

    @Getter private GameImage selectedImage;
    private double defaultEditorMaxHeight;
    private Map<Integer, CheckBox> flagCheckBoxMap = new HashMap<>();
    private ImageFilterSettings imageFilterSettings = new ImageFilterSettings(ImageState.EXPORT);

    private static final int SCALE_DIMENSION = 256;

    @Override
    public void loadFile(VLOArchive vlo) {
        super.loadFile(vlo);

        ObservableList<GameImage> gameImages = FXCollections.observableArrayList(vlo.getImages());
        imageList.setItems(gameImages);
        imageList.setCellFactory(param -> new AttachmentListCell());

        imageList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedImage = newValue;
            this.updateFlags();
            this.updateDisplay();
        });

        imageList.getSelectionModel().select(0);
    }

    @Override
    public void onInit(AnchorPane editorRoot) {
        super.onInit(editorRoot);
        this.defaultEditorMaxHeight = editorRoot.getMaxHeight();

        addFlag("Translucent", GameImage.FLAG_TRANSLUCENT);
        addFlag("Rotated", GameImage.FLAG_ROTATED);
        addFlag("Hit X", GameImage.FLAG_HIT_X);
        addFlag("Hit Y", GameImage.FLAG_HIT_Y);
        addFlag("Name Reference", GameImage.FLAG_REFERENCED_BY_NAME);
        addFlag("Black is Transparent", GameImage.FLAG_BLACK_IS_TRANSPARENT);
        addFlag("2D Sprite", GameImage.FLAG_2D_SPRITE);
        this.updateFlags();
    }

    @Override
    public void onClose(AnchorPane editorRoot) {
        super.onClose(editorRoot);
        editorRoot.setMaxHeight(this.defaultEditorMaxHeight);
    }

    private void addFlag(String display, int flag) {
        CheckBox checkbox = new CheckBox(display);

        checkbox.setOnAction(event -> {
            short flagValue = this.selectedImage.getFlags();
            if (this.selectedImage.testFlag(flag)) {
                flagValue ^= flag;
            } else {
                flagValue |= flag;
            }
            this.selectedImage.setFlags(flagValue);
            this.updateDisplay();
        });

        flagBox.getChildren().add(checkbox);
        flagCheckBoxMap.put(flag, checkbox);
    }

    private void updateFlags() {
        if (this.selectedImage != null)
            for (Entry<Integer, CheckBox> entry : flagCheckBoxMap.entrySet())
                entry.getValue().setSelected(this.selectedImage.testFlag(entry.getKey()));
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
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);

        if (selectedFile == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());
        try {
            ImageIO.write(toBufferedImage(this.selectedImage), "png", selectedFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void importImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the image to import...");
        fileChooser.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png"));
        fileChooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFile.getParentFile());
        try {
            this.selectedImage.replaceImage(ImageIO.read(selectedFile));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        updateDisplay();
    }

    @FXML
    private void exportAllImages(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select the directory to export images to.");
        chooser.setInitialDirectory(GUIMain.getWorkingDirectory());

        File selectedFolder = chooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder == null)
            return; // Cancelled.

        GUIMain.setWorkingDirectory(selectedFolder);
        updateFilter();
        getFile().exportAllImages(selectedFolder, imageFilterSettings);
    }

    @FXML
    private void onImageToggle(ActionEvent event) {
        updateImage();
    }

    @FXML
    private void openPaddingEditor(ActionEvent evt) {
        ImagePaddingController.openPaddingMenu(this);
    }

    @FXML
    private void openVramEditor(ActionEvent evt) {
        VRAMPageController.openEditor(this);
    }

    /**
     * Update the info displayed for the image.
     */
    public void updateImageInfo() {
        dimensionLabel.setText("Archive Dimensions: [Width: " + this.selectedImage.getFullWidth() + ", Height: " + this.selectedImage.getFullHeight() + "]");
        ingameDimensionLabel.setText("In-Game Dimensions: [Width: " + this.selectedImage.getIngameWidth() + ", Height: " + this.selectedImage.getIngameHeight() + "]");
        idLabel.setText("Texture ID: " + this.selectedImage.getTextureId() + ", Flags: " + this.selectedImage.getFlags() + " Page: " + this.selectedImage.getTexturePage());
    }

    /**
     * Update the displayed image.
     */
    public void updateImage() {
        boolean hasImage = (this.selectedImage != null);
        imageView.setVisible(hasImage);

        if (hasImage) {
            BufferedImage image = toBufferedImage(this.selectedImage);

            boolean scaleSize = sizeCheckBox.isSelected();
            imageView.setFitWidth(scaleSize ? SCALE_DIMENSION : image.getWidth());
            imageView.setFitHeight(scaleSize ? SCALE_DIMENSION : image.getHeight());

            imageView.setImage(SwingFXUtils.toFXImage(image, null));
        }
    }

    /**
     * Update this GUI.
     */
    public void updateDisplay() {
        updateImage();
        updateImageInfo();
    }

    private BufferedImage toBufferedImage(GameImage image) {
        updateFilter();
        return image.toBufferedImage(imageFilterSettings);
    }

    private void updateFilter() {
        imageFilterSettings.resetState();
        imageFilterSettings.setTrimEdges(!this.paddingCheckBox.isSelected());
        imageFilterSettings.setAllowTransparency(this.transparencyCheckBox.isSelected());
    }
}
