package net.highwayfrogs.editor.games.sony.shared.ui.file;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.*;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.image.PsxAbrTransparency;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygonSortMode;
import net.highwayfrogs.editor.games.sony.medievil.map.misc.MediEvilMapFrictionLevel;
import net.highwayfrogs.editor.games.sony.medievil.map.misc.MediEvilMapInteractionType;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Controls the VLO edit screen.
 * Created by Kneesnap on 9/18/2018.
 */
public class VLOController extends SCFileEditorUIController<SCGameInstance, VloFile> {
    private final List<ImageBasedFXNode<?>> imageBasedNodes = new ArrayList<>();
    @FXML private CheckBox paddingCheckBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private ChoiceBox<ImageControllerViewSetting> sizeChoiceBox;
    @FXML private ImageView imageView;
    @FXML private ListView<VloImage> imageList;
    @FXML private Label dimensionLabel;
    @FXML private Label ingameDimensionLabel;
    @FXML private Label idLabel;
    @FXML private VBox flagBox;
    @FXML private Button backButton;
    @FXML private AnchorPane rightSidePane;

    @Getter private VloImage selectedImage;
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

        this.imageList.setItems(FXCollections.observableArrayList(vlo != null ? vlo.getImages() : Collections.emptyList()));
        this.imageList.setCellFactory(param -> new LazyFXListCell<VloImage>((image, index) -> {
            if (image == null)
                return null;

            String imageName = image.getName();
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

                Set<String> locations = new HashSet<>();
                StringBuilder builder = new StringBuilder("Texture Usages (").append(textureUsages.size()).append("):\n");
                for (int i = 0; i < textureUsages.size(); i++) {
                    SCTextureUsage usage = textureUsages.get(i);
                    if (locations.add(usage.getLocationDescription()))
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

        // Ensure the flag stuff on the left has enough room to display.
        this.flagBox.setPrefWidth(this.rightSidePane.getWidth() - this.imageView.getFitWidth() - 4);
        this.rightSidePane.widthProperty().addListener((observable, oldWidth, newWidth)
                -> this.flagBox.setPrefWidth(newWidth.doubleValue() - this.imageView.getFitWidth() - 4));
        this.imageView.fitWidthProperty().addListener((observable, oldWidth, newWidth)
                -> this.flagBox.setPrefWidth(this.rightSidePane.getWidth() - newWidth.doubleValue() - 4));

        this.sizeChoiceBox.setItems(FXCollections.observableArrayList(ImageControllerViewSetting.values()));
        this.sizeChoiceBox.setConverter(new AbstractStringConverter<>(ImageControllerViewSetting::getDescription));
        this.sizeChoiceBox.setValue(ImageControllerViewSetting.SCALED_NEAREST_NEIGHBOR);

        addSelectionBox("ABR:", PsxAbrTransparency.values(),
                VloImage::getAbr, VloImage::setAbr, null,
                () -> new LazyFXListCell<>(PsxAbrTransparency::getDisplayName, "None (Error)"));

        addLabel("Flags:", true, true, null);
        addFlag("Translucent", VloImage.FLAG_TRANSLUCENT, "Marks the entire texture as partially transparent.\nOnly applicable when drawn as a sprite or part of a MOF.\nDoes not impact per-game rendering such as map rendering, SKY_LAND, etc.");
        addCalculatedFlag("Hit X", "Treats the last column of pixels on the image as padding.", null, VloImage::calculateHitX);
        addCalculatedFlag("Hit Y", "Treats the last row of pixels on the image as padding.", null, VloImage::calculateHitY);
        addFlag("Name Reference", VloImage.FLAG_REFERENCED_BY_NAME, "If this checkbox is selected, there is assumed to be hardcoded space available in the executable for this texture.\nOtherwise, the texture data will be allocated at runtime.");
        addFlag("Black is Transparent", VloImage.FLAG_BLACK_IS_TRANSPARENT, "Indicates the black (color=000000) pixels in the image should be treated as fully transparent.\nOn PC game builds, this also controls which VRAM pages the texture may be placed in.");
        addFlag("2D Sprite", VloImage.FLAG_2D_SPRITE, "Indicates this texture can be drawn as a sprite.\nA sprite is either as a 3D billboard image like the Frogger score insects, or 2D UI).\nSprites can also be used as 3D textures, so there's no downside to selecting this flag.\nFailing to enable this flag when the game uses it as a sprite will cause crashes.");
        addFlag("Partly Transparent", VloImage.PT_FLAG_PARTLY_TRANSPARENT, "The purpose of this flag is currently unknown.", VloImage::isPtToolkitFlags);
        addCheckBox("Transparent Padding", "Generated padding is transparent.\nThis flag has been calculated by FrogLord, and may not match the original Vorg config file.",
                VloImage::isPaddingTransparent, VloImage::setPaddingTransparent, null);

        // MediEvil Settings
        addLabel("MediEvil Settings:", true, true, VloImage::isMediEvilFlags);
        addSelectionBox("Sort Mode:", MediEvilMapPolygonSortMode.values(),
                VloImage::getMediEvilPolygonSortMode, VloImage::setMediEvilPolygonSortMode, VloImage::isMediEvilFlags,
                () -> new LazyFXListCell<>(MediEvilMapPolygonSortMode::getDisplayName, "None (Error)"));
        addSelectionBox("Friction Level:", MediEvilMapFrictionLevel.values(),
                VloImage::getMediEvilFrictionLevel, VloImage::setMediEvilFrictionLevel, VloImage::isMediEvilFlags,
                () -> new LazyFXListCell<>(MediEvilMapFrictionLevel::getDisplayName, "None (Error)"));
        addSelectionBox("Interaction:", MediEvilMapInteractionType.values(),
                VloImage::getMediEvilInteractionType, VloImage::setMediEvilInteractionType, VloImage::isMediEvilFlags,
                () -> new LazyFXListCell<>(MediEvilMapInteractionType::getDisplayName, "None (Error)"));

        Button cloneButton = new Button("Clone Image");
        cloneButton.setOnAction(evt -> getFile().getArchive().promptVLOSelection(this::promptCloneVlo, false));
        addOptionalUINode(cloneButton, null);
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

    private Label addLabel(String labelText, boolean underline, boolean bold, Predicate<VloImage> visibilityTest) {
        Label label = new Label(labelText);
        label.setUnderline(underline);
        if (bold)
            label.setFont(Constants.SYSTEM_BOLD_FONT);
        addOptionalUINode(label, visibilityTest, null);
        return label;
    }

    private CheckBox addFlag(String display, int flag, String toolTipText) {
        return addFlag(display, flag, toolTipText, null);
    }

    private CheckBox addFlag(String display, int flag, String toolTipText, Predicate<VloImage> visibilityTest) {
        CheckBox checkbox = new CheckBox(display);

        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (checkbox.isDisabled())
                return;

            this.selectedImage.setFlag(flag, newValue); // Clears cache when necessary.
            this.updateDisplay();
        });

        checkbox.setTooltip(FXUtils.createTooltip(toolTipText));
        addOptionalUINode(checkbox, visibilityTest, (image, checkBox) -> {
            if (image == null)
                return;

            checkBox.setDisable(true);
            try {
                checkBox.setSelected(image.testFlag(flag));
            } finally {
                checkBox.setDisable(false);
            }
        });
        return checkbox;
    }

    private CheckBox addCalculatedFlag(String display, String toolTipText, Predicate<VloImage> visibilityTest, Predicate<VloImage> flagCalculator) {
        CheckBox checkbox = new CheckBox(display);
        checkbox.setDisable(true);
        checkbox.setTooltip(FXUtils.createTooltip(toolTipText));

        addOptionalUINode(checkbox, visibilityTest, (newImage, node) ->
            node.setSelected(newImage != null && (visibilityTest == null || visibilityTest.test(newImage)) && flagCalculator.test(newImage)));
        return checkbox;
    }

    private <T> ComboBox<T> addSelectionBox(String label, T[] values, Function<VloImage, T> getter, BiConsumer<VloImage, T> setter, Predicate<VloImage> visibilityTest, Supplier<ListCell<T>> cellSupplier) {
        ComboBox<T> box = new ComboBox<>(FXCollections.observableArrayList(values));
        box.setPrefHeight(25);
        if (cellSupplier != null)
            FXUtils.applyComboBoxDisplaySettings(box, cellSupplier);

        if ((this.selectedImage != null) && (visibilityTest == null || visibilityTest.test(this.selectedImage))) {
            T currentValue = this.selectedImage != null ? getter.apply(this.selectedImage) : null;
            box.valueProperty().setValue(currentValue); // Set the selected value.
            box.getSelectionModel().select(currentValue); // Automatically scroll to selected value.
        }

        AtomicBoolean firstOpen = new AtomicBoolean(true);
        box.addEventFilter(ComboBox.ON_SHOWN, event -> { // Show the selected value when the dropdown is opened.
            if (firstOpen.getAndSet(false))
                FXUtils.comboBoxScrollToValue(box);
        });

        if (setter != null) {
            box.valueProperty().addListener((listener, oldVal, newVal) -> {
                if (!box.isDisable() && !box.isDisabled() && this.selectedImage != null && (visibilityTest == null || visibilityTest.test(this.selectedImage)))
                    setter.accept(this.selectedImage, newVal);
            });
        }

        addOptionalUINode(label, box, visibilityTest, (image, node) -> {
            box.setDisable(true);

            try {
                node.setValue(image != null ? getter.apply(image) : null);
            } finally {
                box.setDisable(false);
            }
        });
        return box;
    }

    private CheckBox addCheckBox(String label, String toolTipText, Predicate<VloImage> getter, BiConsumer<VloImage, Boolean> setter, Predicate<VloImage> visibilityTest) {
        CheckBox checkbox = new CheckBox(label);

        checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (this.selectedImage == null || checkbox.isDisable())
                return;

            setter.accept(this.selectedImage, newValue); // Clears cache when necessary.
            this.updateDisplay();
        });

        checkbox.setTooltip(FXUtils.createTooltip(toolTipText));
        addOptionalUINode(checkbox, visibilityTest, (image, checkBox) -> {
            if (image == null)
                return;

            checkBox.setDisable(true);
            try {
                checkBox.setSelected(getter.test(image));
            } finally {
                checkBox.setDisable(false);
            }
        });
        return checkbox;
    }

    private <TNode extends Node> void addOptionalUINode(TNode node, BiConsumer<VloImage, TNode> handler) {
        ImageBasedFXNode<TNode> wrapper = new ImageBasedFXNode<>(node, (image, safeWrapper) -> {
            if (updateVisibility(image, safeWrapper, null) && handler != null)
                handler.accept(image, safeWrapper.node);
        });

        this.imageBasedNodes.add(wrapper);
        if (handler != null)
            handler.accept(this.selectedImage, node);
    }

    private <TNode extends Node> void addOptionalUINode(TNode node, Predicate<VloImage> visibilityTest, BiConsumer<VloImage, TNode> handler) {
        if (visibilityTest == null) {
            addOptionalUINode(node, handler);
            return;
        }

        ImageBasedFXNode<TNode> wrapper = new ImageBasedFXNode<>(node, (image, safeWrapper) ->
                updateHook(image, safeWrapper, visibilityTest, handler));

        this.imageBasedNodes.add(wrapper);
        updateHook(this.selectedImage, wrapper, visibilityTest, handler);
    }

    private static <TNode extends Node> BiConsumer<VloImage, ImageBasedFXNode<TNode>> wrapHandler(BiConsumer<VloImage, TNode> handler) {
        return handler != null ? (image, wrapper) -> handler.accept(image, wrapper.node) : null;
    }

    private <TNode extends Node> boolean updateVisibility(VloImage image, ImageBasedFXNode<TNode> nodeWrapper, Predicate<VloImage> visibilityTest) {
        boolean visible = image != null && (visibilityTest == null || visibilityTest.test(image));
        TNode node = nodeWrapper.node;
        if (visible && !this.flagBox.getChildren().contains(node)) {
            insertNode(nodeWrapper);
        } else if (!visible) {
            this.flagBox.getChildren().remove(node);
        }

        return visible;
    }

    private <TNode extends Node> void insertNode(ImageBasedFXNode<TNode> nodeWrapper) {
        int nodeIndex = this.imageBasedNodes.indexOf(nodeWrapper);
        if (nodeIndex < 0)
            throw new IllegalArgumentException("nodeWrapper is not registered as part of imageBasedNodes!");

        // Maintains the imageBasedNodes order.
        int lastWrapperNodeIndex = -1;
        int lastInsertionIndex = -1;
        for (int i = 0; i < this.flagBox.getChildren().size(); i++) {
            Node fxNode = this.flagBox.getChildren().get(i);

            // Find current node in imageBasedNodes list.
            int currWrapperNodeIndex = lastWrapperNodeIndex + 1;
            for (; currWrapperNodeIndex < this.imageBasedNodes.size(); currWrapperNodeIndex++)
                if (this.imageBasedNodes.get(currWrapperNodeIndex).node == fxNode)
                    break;

            if (currWrapperNodeIndex >= this.imageBasedNodes.size()) {
                lastInsertionIndex++;
                continue; // The fxNode has no imagedBasedNode registration, so skip it.
            }

            lastWrapperNodeIndex = currWrapperNodeIndex;
            lastInsertionIndex = i;

            // The last one we found was
            if (lastWrapperNodeIndex >= nodeIndex) {
                lastInsertionIndex--;
                break;
            }
        }

        int insertionIndex = lastInsertionIndex + 1;
        if (insertionIndex < 0)
            throw new IllegalStateException("Could not find insertionIndex for node: " + nodeWrapper.node + "/" + nodeWrapper.nodeUpdater);

        this.flagBox.getChildren().add(insertionIndex, nodeWrapper.node);
    }

    private <TNode extends Node> void updateHook(VloImage image, ImageBasedFXNode<TNode> nodeWrapper, Predicate<VloImage> visibilityTest, BiConsumer<VloImage, TNode> handler) {
        if (updateVisibility(image, nodeWrapper, visibilityTest) && handler != null)
            handler.accept(image, nodeWrapper.node);
    }

    private <TNode extends Node> void addOptionalUINode(String label, TNode node, Predicate<VloImage> visibilityTest, BiConsumer<VloImage, TNode> handler) {
        HBox box = new HBox();
        box.setSpacing(5);

        Label fxLabel = new Label(label);
        HBox.setHgrow(fxLabel, Priority.SOMETIMES);
        fxLabel.setAlignment(Pos.CENTER_LEFT);
        fxLabel.setTextAlignment(TextAlignment.LEFT);
        fxLabel.setPrefHeight(25);
        box.getChildren().add(fxLabel);

        HBox.setHgrow(node, Priority.SOMETIMES);
        box.getChildren().add(node);

        // Synchronize hbox width to vbox width.
        box.setPrefWidth(this.flagBox.getWidth());
        this.flagBox.widthProperty().addListener((observable, oldWidth, newWidth)
                -> box.setPrefWidth(newWidth.doubleValue()));

        ImageBasedFXNode<HBox> wrapper = new ImageBasedFXNode<>(box, (image, safeWrapper) ->
            registerAndUpdate(node, visibilityTest, image, handler, safeWrapper));

        this.imageBasedNodes.add(wrapper);
        registerAndUpdate(node, visibilityTest, this.selectedImage, handler, wrapper);
    }

    private <TNode extends Node> void registerAndUpdate(TNode node, Predicate<VloImage> visibilityTest, VloImage image, BiConsumer<VloImage, TNode> updateHandler, ImageBasedFXNode<HBox> nodeWrapper) {
        if (updateVisibility(image, nodeWrapper, visibilityTest) && updateHandler != null)
            updateHandler.accept(image, node);
    }

    private void updateOptionalUI(VloImage image) {
        for (int i = 0; i < this.imageBasedNodes.size(); i++)
            this.imageBasedNodes.get(i).updateNode(image);
    }

    @FXML
    @SneakyThrows
    private void exportImage(ActionEvent event) {
        String name = this.selectedImage.getName();
        BufferedImage image = this.selectedImage.toBufferedImage(IMAGE_EXPORT_SETTINGS);
        FileUtils.askUserToSaveImageFile(getLogger(), getGameInstance(), image, name != null ? name : String.valueOf(this.selectedImage.getTextureId()));
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
        int unpaddedWidth = this.selectedImage.getInternalUnpaddedWidth();
        int unpaddedHeight = this.selectedImage.getInternalUnpaddedHeight();
        this.ingameDimensionLabel.setText("Size: " + unpaddedWidth + "x" + unpaddedHeight);
        this.dimensionLabel.setText("Padding: " + (paddedWidth - unpaddedWidth) + "x" + (paddedHeight - unpaddedHeight));
        this.idLabel.setText((getFile().isPsxMode() ? this.selectedImage.getBitDepth().getDisplayName() + ", " : "")
                + "VRAM X: " + this.selectedImage.getVramX() + ", Y: " + this.selectedImage.getVramY() + ", Page: " + this.selectedImage.getPage());
    }

    /**
     * Select a particular image in the vlo.
     * @param image The image to select.
     */
    public void selectImage(VloImage image, boolean forceSelect) {
        if (image == null)
            return;

        this.selectedImage = image;
        updateOptionalUI(image);
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

    @RequiredArgsConstructor
    private static class ImageBasedFXNode<TNode extends Node> {
        @NonNull private final TNode node;
        private final BiConsumer<VloImage, ImageBasedFXNode<TNode>> nodeUpdater;


        /**
         * Updates the node.
         * @param image the image to update with
         */
        public void updateNode(VloImage image) {
            if (this.nodeUpdater != null)
                this.nodeUpdater.accept(image, this);
        }
    }
}