package net.highwayfrogs.editor.games.sony.shared.vlo2.ui;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.psx.image.PsxAbrTransparency;
import net.highwayfrogs.editor.games.psx.image.PsxImageBitDepth;
import net.highwayfrogs.editor.games.psx.image.PsxVram;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapPolygonSortMode;
import net.highwayfrogs.editor.games.sony.medievil.map.misc.MediEvilMapFrictionLevel;
import net.highwayfrogs.editor.games.sony.medievil.map.misc.MediEvilMapInteractionType;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.ui.SCFileEditorUIController;
import net.highwayfrogs.editor.games.sony.shared.utils.SCImageUtils;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloPadding;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ListViewComponent;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Controls the VLO edit screen.
 * Created by Kneesnap on 9/18/2018.
 */
public class VloFileUIController extends SCFileEditorUIController<SCGameInstance, VloFile> {
    @Getter private VloFileEditorComponent editorComponent;
    private final List<ImageBasedFXNode<?>> imageBasedNodes = new ArrayList<>();
    @FXML private CheckBox paddingCheckBox;
    @FXML private CheckBox transparencyCheckBox;
    @FXML private ChoiceBox<ImageControllerViewSetting> sizeChoiceBox;
    @FXML private ImageView imageView;
    @FXML private Label dimensionLabel;
    @FXML private Label ingameDimensionLabel;
    @FXML private Label idLabel;
    @FXML private VBox flagBox;
    @FXML private Button backButton;
    @FXML private AnchorPane leftSidePane;
    @FXML private AnchorPane rightSidePane;

    @Getter private VloImage selectedImage;
    private int imageFilterSettings = VloImage.DEFAULT_IMAGE_STRIPPED_VIEW_SETTINGS;

    private static final int SCALE_DIMENSION = 256;

    public VloFileUIController(SCGameInstance instance) {
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
        this.editorComponent.getCollectionViewComponent().refreshDisplay();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);

        // Register file list.
        this.editorComponent = new VloFileEditorComponent(getGameInstance(), this, false);
        setAnchorPaneStretch(this.editorComponent.getRootNode());
        this.editorComponent.extendEditorUI();
        this.leftSidePane.getChildren().add(this.editorComponent.getRootNode());
        addController(this.editorComponent);

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
        addFlag("Translucent", VloImage.FLAG_TRANSLUCENT, "Marks the entire texture as partially transparent.\nOnly applicable when drawn as a sprite or part of a MOF.\nThis setting may be ignored if the polygons using this texture are not also marked as semi-transparent.");
        addCalculatedFlag("Hit X", "Treats the last column of pixels on the image as padding.", null, VloImage::calculateHitX);
        addCalculatedFlag("Hit Y", "Treats the last row of pixels on the image as padding.", null, VloImage::calculateHitY);
        addFlag("Name Reference", VloImage.FLAG_REFERENCED_BY_NAME, "If this checkbox is selected, there is assumed to be hardcoded space available in the executable for this texture.\nOtherwise, the texture data will be allocated at runtime.");
        addFlag("Black is Transparent", VloImage.FLAG_BLACK_IS_TRANSPARENT, "Indicates the black (color=000000) pixels in the image should be treated as fully transparent.\nOn PC game builds, this also controls which VRAM pages the texture may be placed in.");
        addFlag("2D Sprite", VloImage.FLAG_2D_SPRITE, "Indicates this texture can be drawn as a sprite.\nA sprite is either as a 3D billboard image like the Frogger score insects, or 2D UI).\nSprites can also be used as 3D textures, so there's no downside to selecting this flag.\nFailing to enable this flag when the game uses it as a sprite will cause crashes.");
        addFlag("Partly Transparent", VloImage.PT_FLAG_PARTLY_TRANSPARENT, "The purpose of this flag is currently unknown.", VloImage::isPtToolkitFlags);
        addCheckBox("Transparent Padding", "Generated padding is transparent.\nThis flag has been calculated by FrogLord, and may not match the original Vorg config file.",
                VloImage::isPaddingTransparent, VloImage::setPaddingTransparent, null);

        // Maybe in the future we will allow the user to control this.
        // But at the time of writing, this isn't understood enough to allow such a thing.
        addCalculatedFlag("Enable Fog", "If set, the image will be capable of fading to gray, mimicking fog."
                + "\nThis is done by generating extra clut rows, progressively closer to the color gray.",
                image -> image.getParent().hasClutFogSupport(), VloImage::isClutFogEnabled);

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
    private void onImageToggle(ActionEvent event) {
        updateImage();
    }

    @FXML
    private void openVramEditor(ActionEvent evt) {
        VloVramUIController.openEditor(this);
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

        if (forceSelect)
            this.editorComponent.getCollectionViewComponent().setSelectedViewEntryInUI(image);
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
            awtImage = SCImageUtils.scaleForDisplay(awtImage, PsxVram.PSX_VRAM_PAGE_EXPANDED_WIDTH);

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

    private static class VloFileEditorComponent extends CollectionEditorComponent<SCGameInstance, VloImage> {
        private final VloFileUIController controller;
        private final MenuItem addImageItem = new MenuItem("Add Image");
        private final MenuItem addFroggerTextImageItem = new MenuItem("Add Frogger Text Image");

        public VloFileEditorComponent(SCGameInstance instance, VloFileUIController controller, boolean padCollectionView) {
            super(instance, new VloFileBasicListViewComponent(instance, controller), padCollectionView);
            this.controller = controller;
        }

        @Override
        public VloFileBasicListViewComponent getCollectionViewComponent() {
            return (VloFileBasicListViewComponent) super.getCollectionViewComponent();
        }

        private void extendEditorUI() {
            addMenuItemToAddButtonLogic(this.addImageItem);
            this.addImageItem.setOnAction(event -> promptUserAddImage(null));

            // TODO: ADD text menu item if the game is frogger. (Maybe)

            setRemoveButtonLogic(image -> {
                if (image != null && image.getParent() == this.controller.getFile() && image.getParent().removeImage(image))
                    getCollectionViewComponent().refreshDisplay();
            });
        }

        private void promptUserAddImage(PsxImageBitDepth bitDepth) {
            VloFile vloFile = this.controller.getFile();
            File selectedFile = FileUtils.askUserToOpenFile(vloFile.getGameInstance(), FileUtils.IMPORT_SINGLE_IMAGE_PATH);
            if (selectedFile == null)
                return;

            BufferedImage loadedImage;
            try {
                loadedImage = ImageIO.read(selectedFile);
            } catch (IOException ex) {
                Utils.handleError(vloFile.getLogger(), ex, true, "The file '%s' could not be loaded as an image.", selectedFile.getName());
                return;
            }

            String imageName = FileUtils.stripExtension(selectedFile.getName());
            VloImage existingImage;
            if ((existingImage = vloFile.getImageByName(imageName)) != null) {
                if (!FXUtils.makePopUpYesNo("Image already present.", "An image already exists named '" + imageName + "', would you like to replace it?"))
                    return;

                existingImage.replaceImage(loadedImage, bitDepth, -1, existingImage.testFlag(VloImage.FLAG_TRANSLUCENT), ProblemResponse.CREATE_POPUP);
                return;
            } else if (!VloImage.isValidTextureName(imageName)) {
                FXUtils.showPopup(AlertType.WARNING, "Invalid texture name.", "The texture name '" + imageName + "' is not valid, please rename the file.");
                return;
            }

            vloFile.addImage(imageName, loadedImage, VloPadding.DEFAULT, bitDepth, null, false);
            getCollectionViewComponent().refreshDisplay();
        }
    }

    private static class VloFileBasicListViewComponent extends ListViewComponent<SCGameInstance, VloImage> {
        private final VloFileUIController controller;

        public VloFileBasicListViewComponent(SCGameInstance instance, VloFileUIController controller) {
            super(instance);
            this.controller = controller;
        }

        @Override
        public void onSceneAdd(Scene newScene) {
            super.onSceneAdd(newScene);
            if (getSelectedViewEntry() == null && getEntries().size() > 0)
                getRootNode().getSelectionModel().selectFirst();
        }

        @Override
        protected void onSelect(VloImage image) {
            this.controller.selectImage(image, false);
            this.controller.editorComponent.updateEditorControls();
        }

        @Override
        protected void onDoubleClick(VloImage image) {
            // Do nothing.
        }

        @Override
        public List<VloImage> getViewEntries() {
            VloFile file = this.controller != null ? this.controller.getFile() : null;
            return file != null ? file.getImages() : Collections.emptyList();
        }
    }
}