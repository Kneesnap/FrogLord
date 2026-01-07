package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.games.psx.image.PsxAbrTransparency;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.games.sony.shared.utils.SCAnalysisUtils;
import net.highwayfrogs.editor.games.sony.shared.utils.SCAnalysisUtils.SCTextureUsage;
import net.highwayfrogs.editor.games.sony.shared.utils.SCImageUtils;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.FileUtils.SavedFilePath;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * Controls the VLO edit screen.
 * Created by Kneesnap on 9/18/2018.
 */
public class VLOController extends SCFileEditorUIController<SCGameInstance, VloFile> {
    @FXML private CheckBox paddingCheckBox;
    @FXML private ChoiceBox<PsxAbrTransparency> abrChoiceBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private ChoiceBox<ImageControllerViewSetting> sizeChoiceBox;
    @FXML private ImageView imageView;
    @FXML private ListView<VloImage> imageList;
    @FXML private Label dimensionLabel;
    @FXML private Label ingameDimensionLabel;
    @FXML private Label idLabel;
    @FXML private VBox flagBox;
    @FXML private Button backButton;

    @Getter private VloImage selectedImage;
    private final Map<Integer, CheckBox> flagCheckBoxMap = new HashMap<>(); // TODO: Something broke here, the flag names aren't long enough
    private int imageFilterSettings = VloImage.DEFAULT_IMAGE_STRIPPED_VIEW_SETTINGS;

    private static final int IMAGE_EXPORT_SETTINGS = VloImage.DEFAULT_IMAGE_NO_PADDING_EXPORT_SETTINGS;

    private static final SavedFilePath IMAGE_EXPORT_DIRECTORY = new SavedFilePath("vlo-bulk-export", "Select the folder to export images to...");
    private static final SavedFilePath IMAGE_IMPORT_DIRECTORY = new SavedFilePath("vlo-bulk-import", "Select the folder to import images from...");
    private static final WeakHashMap<SCGameInstance, List<SCTextureUsage>[]> TEXTURE_USAGES_PER_INSTANCE = new WeakHashMap<>();

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
    public void setTargetFile(VloFile vlo) {
        super.setTargetFile(vlo);

        this.abrChoiceBox.setDisable(vlo == null || !vlo.isPsxMode());
        this.abrChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (this.selectedImage != null && oldValue != newValue && newValue != null && this.selectedImage.getAbr() != newValue && !this.abrChoiceBox.isDisable())
                this.selectedImage.setAbr(newValue);
        });

        this.imageList.setItems(FXCollections.observableArrayList(vlo.getImages()));
        this.imageList.setCellFactory(param -> new LazyFXListCell<VloImage>((image, index) -> {
            if (image == null)
                return null;

            String imageName = image.getOriginalName();
            return index + ": " + (imageName != null ? imageName : "") + " [" + image.getPaddedWidth() + ", " + image.getPaddedHeight() + "] (ID: " + image.getTextureId() + ")";
        }).setWithoutIndexContextMenuHandler((contextMenu, image) -> {
            MenuItem findTextureUsages = new MenuItem("Find Usages");
            contextMenu.getItems().add(findTextureUsages);
            contextMenu.setOnAction(event -> {
                List<SCTextureUsage>[] allTextureUsages = TEXTURE_USAGES_PER_INSTANCE.computeIfAbsent(getGameInstance(), SCAnalysisUtils::generateTextureUsageMapping);
                List<SCTextureUsage> textureUsages = allTextureUsages != null && allTextureUsages.length > image.getTextureId() ? allTextureUsages[image.getTextureId()] : null;
                if (textureUsages == null || textureUsages.isEmpty()) {
                    FXUtils.showPopup(AlertType.INFORMATION, "No usages found.", "No usages of this image were found.\nThis does *NOT* guarantee that the image is never unused.");
                    return;
                }

                StringBuilder builder = new StringBuilder("Texture Usages (").append(textureUsages.size()).append("):\n");
                for (int i = 0; i < textureUsages.size(); i++) {
                    SCTextureUsage usage = textureUsages.get(i);
                    builder.append(" - ").append(usage.getLocationDescription()).append('\n');
                }

                FXUtils.showPopup(AlertType.INFORMATION, "Texture Usage Finder:", builder.toString());
            });
        }));

        this.imageList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> selectImage(newValue, false));
        this.imageList.getSelectionModel().selectFirst();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        this.abrChoiceBox.setItems(FXCollections.observableArrayList(PsxAbrTransparency.values()));
        this.abrChoiceBox.setConverter(new AbstractStringConverter<>(PsxAbrTransparency::getDisplayName));
        this.abrChoiceBox.setValue(PsxAbrTransparency.DEFAULT);

        this.sizeChoiceBox.setItems(FXCollections.observableArrayList(ImageControllerViewSetting.values()));
        this.sizeChoiceBox.setConverter(new AbstractStringConverter<>(ImageControllerViewSetting::getDescription));
        this.sizeChoiceBox.setValue(ImageControllerViewSetting.SCALED_NEAREST_NEIGHBOR);

        addFlag("Translucent", VloImage.FLAG_TRANSLUCENT, "Marks the entire texture as partially transparent.\nOnly applicable when drawn as a sprite or part of a MOF.\nDoes not impact per-game rendering such as map rendering, SKY_LAND, etc.");
        //addFlag("Rotated", VloImage.FLAG_ROTATED, "This flag is unused, and does not appear to be set.");
        // TODO: !
        //addFlag("Hit X", VloImage.FLAG_HIT_X, "Treats the last column of pixels on the image as padding.");
        //addFlag("Hit Y", VloImage.FLAG_HIT_Y, "Treats the last row of pixels on the image as padding.");
        addFlag("Name Reference", VloImage.FLAG_REFERENCED_BY_NAME, "If this checkbox is selected, there is assumed to be hardcoded space available in the executable for this texture.\nOtherwise, the texture data will be allocated at runtime.");
        addFlag("Black is Transparent", VloImage.FLAG_BLACK_IS_TRANSPARENT, "Indicates the black (color=000000) pixels in the image should be treated as fully transparent.");
        addFlag("2D Sprite", VloImage.FLAG_2D_SPRITE, "Indicates this texture can be drawn as a sprite.\nA sprite is either as a 3D billboard image like the Frogger score insects, or 2D UI).\nSprites can also be used as 3D textures, so there's no downside to selecting this flag.\nFailing to enable this flag when the game uses it as a sprite will cause crashes.");
        this.updateFlags();

        Button cloneButton = new Button("Clone Image");
        cloneButton.setOnAction(evt -> getFile().getArchive().promptVLOSelection(this::promptCloneVlo, false));
        flagBox.getChildren().add(cloneButton);
    }

    private void promptCloneVlo(VloFile cloneFrom) {
        cloneFrom.promptImageSelection(gameImage -> {
            if (gameImage == null)
                return;

            int newView = getFile().getImages().size();
            getFile().getImages().add(gameImage.clone()); // TODO: This is now immmutable, rewrite this feature.
            imageList.setItems(FXCollections.observableArrayList(getFile().getImages()));
            imageList.getSelectionModel().select(newView);
            imageList.scrollTo(newView);
            Platform.runLater(() -> promptCloneVlo(cloneFrom)); // If we don't delay this, the existing window won't shut.
        }, true);
    }

    private CheckBox addFlag(String display, int flag, String toolTipText) {
        CheckBox checkbox = new CheckBox(display);

        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            this.selectedImage.setFlag(flag, newValue); // Clears cache when necessary.
            this.updateDisplay();
        });

        checkbox.setTooltip(FXUtils.createTooltip(toolTipText));

        flagBox.getChildren().add(checkbox);
        flagCheckBoxMap.put(flag, checkbox);
        return checkbox;
    }

    private void updateFlags() {
        if (this.selectedImage != null)
            for (Entry<Integer, CheckBox> entry : flagCheckBoxMap.entrySet())
                entry.getValue().setSelected(this.selectedImage.testFlag(entry.getKey()));
    }

    @FXML
    @SneakyThrows
    private void exportImage(ActionEvent event) {
        String originalName = this.selectedImage.getOriginalName();
        BufferedImage image = this.selectedImage.toBufferedImage(IMAGE_EXPORT_SETTINGS);
        FileUtils.askUserToSaveImageFile(getLogger(), getGameInstance(), image, originalName != null ? originalName : String.valueOf(this.selectedImage.getTextureId()));
    }

    @FXML
    @SneakyThrows
    private void importImage(ActionEvent event) {
        BufferedImage loadedImage = FileUtils.askUserToOpenImageFile(getLogger(), getGameInstance());
        if (loadedImage != null) {
            this.selectedImage.replaceImage(loadedImage, ProblemResponse.CREATE_POPUP);
            updateDisplay();
        }
    }

    @FXML
    private void exportAllImages(ActionEvent event) {
        File selectedFolder = FileUtils.askUserToSelectFolder(getGameInstance(), IMAGE_EXPORT_DIRECTORY);
        if (selectedFolder == null)
            return; // Cancelled.

        getFile().exportAllImages(selectedFolder, IMAGE_EXPORT_SETTINGS);
    }

    @FXML
    @SneakyThrows
    private void importAllImages(ActionEvent event) {
        File selectedFolder = FileUtils.askUserToSelectFolder(getGameInstance(), IMAGE_IMPORT_DIRECTORY);
        if (selectedFolder == null)
            return; // Cancelled.

        updateFilter();
        int importedFiles = 0;
        for (File file : FileUtils.listFiles(selectedFolder)) {
            String name = FileUtils.stripExtension(file.getName());
            if (!NumberUtils.isInteger(name))
                continue;

            int id = Integer.parseInt(name);
            if (id >= 0 && id < getFile().getImages().size()) {
                getFile().getImages().get(id).replaceImage(ImageIO.read(file), ProblemResponse.CREATE_POPUP);
                importedFiles++;
            }
        }

        getLogger().info("Imported %d images.", importedFiles);
        updateDisplay();
    }

    @FXML
    private void onImageToggle(ActionEvent event) {
        updateImage();
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
        int paddedWidth = this.selectedImage.getPaddedWidth();
        int paddedHeight = this.selectedImage.getPaddedHeight();
        int unpaddedWidth = this.selectedImage.getUnpaddedWidth();
        int unpaddedHeight = this.selectedImage.getUnpaddedHeight();
        this.abrChoiceBox.setValue(this.selectedImage.getAbr());
        this.ingameDimensionLabel.setText("Size: " + unpaddedWidth + "x" + unpaddedHeight);
        this.dimensionLabel.setText("Padding: " + (paddedWidth - unpaddedWidth) + "x" + (paddedHeight - unpaddedHeight));

        // TODO: Show clut XY here?
        // TODO: Split into more lines.
        this.idLabel.setText("Mode: " + this.selectedImage.getBitDepth().getDisplayName() + ", VRAM X: " + this.selectedImage.getVramX() + ", Y: " + this.selectedImage.getVramY() + ", Page: " + this.selectedImage.getPage());
    }

    /**
     * Select a particular image in the vlo.
     * @param image The image to select.
     */
    public void selectImage(VloImage image, boolean forceSelect) {
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
            this.imageView.setImage(FXUtils.toFXImage(image, false));
        }
    }

    /**
     * Update this GUI.
     */
    public void updateDisplay() {
        updateImage();
        updateImageInfo();
    }

    private BufferedImage toBufferedImage(VloImage image) {
        updateFilter();
        BufferedImage awtImage = image.toBufferedImage(this.imageFilterSettings);
        if (this.sizeChoiceBox.getValue() == ImageControllerViewSetting.SCALED_NEAREST_NEIGHBOR)
            awtImage = SCImageUtils.scaleForDisplay(awtImage, PsxVram.PSX_VRAM_PAGE_EXPANDED_WIDTH, true);

        return awtImage;
    }

    private void setFlag(int flag, boolean active) {
        if (active) {
            this.imageFilterSettings |= flag;
        } else {
            this.imageFilterSettings &= ~flag;
        }
    }

    private void updateFilter() {
        setFlag(VloImage.IMAGE_EXPORT_FLAG_ENABLE_TRANSPARENCY, this.transparencyCheckBox.isSelected());
        setFlag(VloImage.IMAGE_EXPORT_FLAG_INCLUDE_PADDING, this.paddingCheckBox.isSelected());
        setFlag(VloImage.IMAGE_EXPORT_FLAG_HIGHLIGHT_PADDING, this.paddingCheckBox.isSelected());
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