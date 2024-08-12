package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings;
import net.highwayfrogs.editor.file.vlo.ImageFilterSettings.ImageState;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Controls the VLO edit screen.
 * Created by Kneesnap on 9/18/2018.
 */
public class VLOController extends SCFileEditorUIController<SCGameInstance, VLOArchive> {
    @FXML private CheckBox paddingCheckBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private ChoiceBox<ImageControllerViewSetting> sizeChoiceBox;
    @FXML private ImageView imageView;
    @FXML private ListView<GameImage> imageList;
    @FXML private Label dimensionLabel;
    @FXML private Label ingameDimensionLabel;
    @FXML private Label idLabel;
    @FXML private VBox flagBox;
    @FXML private Button backButton;

    @Getter private GameImage selectedImage;
    private final Map<Integer, CheckBox> flagCheckBoxMap = new HashMap<>();
    private final ImageFilterSettings imageFilterSettings = new ImageFilterSettings(ImageState.EXPORT);

    private static final int SCALE_DIMENSION = 256;

    public VLOController(SCGameInstance instance) {
        super(instance);
    }

    @Override
    public void setParentWadFile(WADFile wadFile) {
        super.setParentWadFile(wadFile);
        this.backButton.setVisible(wadFile != null);
    }

    @Override
    public void setTargetFile(VLOArchive vlo) {
        super.setTargetFile(vlo);

        imageList.setItems(FXCollections.observableArrayList(vlo.getImages()));
        imageList.setCellFactory(param -> new AbstractAttachmentCell<>((image, index) -> {
            if (image == null)
                return null;

            String imageName = getConfig().getImageNames().getOrDefault(image.getTextureId(), "");
            return index + ": " + imageName + " [" + image.getFullWidth() + ", " + image.getFullHeight() + "] (ID: " + image.getTextureId() + ")";
        }));

        imageList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> selectImage(newValue, false));

        imageList.getSelectionModel().select(0);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        this.sizeChoiceBox.setItems(FXCollections.observableArrayList(ImageControllerViewSetting.values()));
        this.sizeChoiceBox.setConverter(new AbstractStringConverter<>(ImageControllerViewSetting::getDescription));
        this.sizeChoiceBox.setValue(ImageControllerViewSetting.SCALED_NEAREST_NEIGHBOR);

        addFlag("Translucent", GameImage.FLAG_TRANSLUCENT);
        addFlag("Rotated", GameImage.FLAG_ROTATED);
        addFlag("Hit X", GameImage.FLAG_HIT_X);
        addFlag("Hit Y", GameImage.FLAG_HIT_Y);
        addFlag("Name Reference", GameImage.FLAG_REFERENCED_BY_NAME);
        addFlag("Black is Transparent", GameImage.FLAG_BLACK_IS_TRANSPARENT);
        addFlag("2D Sprite", GameImage.FLAG_2D_SPRITE);
        this.updateFlags();

        Button cloneButton = new Button("Clone Image");
        cloneButton.setOnAction(evt -> getFile().getArchive().promptVLOSelection(null, this::promptCloneVlo, false));
        flagBox.getChildren().add(cloneButton);
    }

    private void promptCloneVlo(VLOArchive cloneFrom) {
        cloneFrom.promptImageSelection(gameImage -> {
            if (gameImage == null)
                return;

            int newView = getFile().getImages().size();
            getFile().getImages().add(gameImage.clone());
            imageList.setItems(FXCollections.observableArrayList(getFile().getImages()));
            imageList.getSelectionModel().select(newView);
            imageList.scrollTo(newView);
            Platform.runLater(() -> promptCloneVlo(cloneFrom)); // If we don't delay this, the existing window won't shut.
        }, true);
    }

    private void addFlag(String display, int flag) {
        CheckBox checkbox = new CheckBox(display);

        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (this.selectedImage.setFlag(flag, newValue))
                this.imageFilterSettings.invalidateRenderCache();

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

    @FXML
    @SneakyThrows
    private void exportImage(ActionEvent event) {
        File selectedFile = Utils.promptFileSave(getGameInstance(), "Specify the file to export this image as...", null, "Image Files", "png");
        if (selectedFile != null)
            ImageIO.write(toBufferedImage(this.selectedImage), "png", selectedFile);
    }

    @FXML
    @SneakyThrows
    private void importImage(ActionEvent event) {
        File selectedFile = Utils.promptFileOpen(getGameInstance(), "Select the image to import...", "Image Files", "png");
        if (selectedFile == null)
            return; // Cancelled.

        this.selectedImage.replaceImage(ImageIO.read(selectedFile));
        updateDisplay();
    }

    @FXML
    private void exportAllImages(ActionEvent event) {
        File selectedFolder = Utils.promptChooseDirectory(getGameInstance(), "Select the directory to export images to.", true);
        if (selectedFolder == null)
            return; // Cancelled.

        updateFilter();
        getFile().exportAllImages(selectedFolder, imageFilterSettings);
    }

    @FXML
    @SneakyThrows
    private void importAllImages(ActionEvent event) {
        File selectedFolder = Utils.promptChooseDirectory(getGameInstance(), "Select the directory to import images from.", true);
        if (selectedFolder == null)
            return; // Cancelled.

        updateFilter();
        int importedFiles = 0;
        for (File file : Utils.listFiles(selectedFolder)) {
            String name = Utils.stripExtension(file.getName());
            if (!Utils.isInteger(name))
                continue;

            int id = Integer.parseInt(name);
            if (id >= 0 && id < getFile().getImages().size()) {
                getFile().getImages().get(id).replaceImage(ImageIO.read(file));
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
    private void openPaddingEditor(ActionEvent evt) {
        ImagePaddingController.openPaddingMenu(this);
    }

    @FXML
    private void openVramEditor(ActionEvent evt) {
        VRAMPageController.openEditor(this);
    }

    @FXML
    private void returnToWad(ActionEvent event) {
        tryReturnToParentWadFile();
    }

    /**
     * Update the info displayed for the image.
     */
    public void updateImageInfo() {
        dimensionLabel.setText("Archive Dimensions: [Width: " + this.selectedImage.getFullWidth() + ", Height: " + this.selectedImage.getFullHeight() + "]");
        ingameDimensionLabel.setText("In-Game Dimensions: [Width: " + this.selectedImage.getIngameWidth() + ", Height: " + this.selectedImage.getIngameHeight() + "]");
        idLabel.setText("ABR: " + this.selectedImage.getAbr() + " Mode: " + this.selectedImage.getClutMode().getDisplayName() + ", Page: " + this.selectedImage.getPage());
    }

    /**
     * Select a particular image in the vlo.
     * @param image The image to select.
     */
    public void selectImage(GameImage image, boolean forceSelect) {
        if (image == null)
            return;

        this.selectedImage = image;
        this.updateFlags();
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
            BufferedImage image = toBufferedImage(this.selectedImage);

            boolean scaleSize = this.sizeChoiceBox.getValue() != ImageControllerViewSetting.ORIGINAL_SIZE;
            this.imageView.setFitWidth(scaleSize ? SCALE_DIMENSION : image.getWidth());
            this.imageView.setFitHeight(scaleSize ? SCALE_DIMENSION : image.getHeight());
            this.imageView.setImage(Utils.toFXImage(image, false));
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
        return image.toBufferedImage(this.imageFilterSettings);
    }

    private void updateFilter() {
        this.imageFilterSettings.setTrimEdges(!this.paddingCheckBox.isSelected());
        this.imageFilterSettings.setAllowTransparency(this.transparencyCheckBox.isSelected());
        this.imageFilterSettings.setScaleToMaxSize(this.sizeChoiceBox.getValue() == ImageControllerViewSetting.SCALED_NEAREST_NEIGHBOR);
    }

    @Getter
    @AllArgsConstructor
    public enum ImageControllerViewSetting {
        ORIGINAL_SIZE("Original Size"),
        SCALED_BLURRY("Scaled (Blurry)"),
        SCALED_NEAREST_NEIGHBOR("Scaled (Sharp)");

        private final String description;
    }
}