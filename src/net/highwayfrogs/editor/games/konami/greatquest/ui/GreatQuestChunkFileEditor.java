package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceTableOfContents;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ListViewComponent;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.fx.wrapper.LazyFXListCell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents editor UI for the list of chunks in a chunked file.
 * Created by Kneesnap on 10/20/2024.
 */
@Getter
public class GreatQuestChunkFileEditor extends GreatQuestFileEditorUIController<GreatQuestChunkedFile> {
    private final CollectionEditorComponent<GreatQuestInstance, kcCResource> collectionEditorComponent;
    private final GreatQuestChunkListViewComponent chunkListComponent;
    private GameUIController<?> tempExtraController;

    public GreatQuestChunkFileEditor(GreatQuestInstance instance) {
        this(instance, "Chunks");
    }

    public GreatQuestChunkFileEditor(GreatQuestInstance instance, String fileNameText) {
        this(instance, fileNameText, ImageResource.ZIPPED_FOLDER_16);
    }

    public GreatQuestChunkFileEditor(GreatQuestInstance instance, String fileNameText, ImageResource icon) {
        super(instance, fileNameText, icon);
        this.chunkListComponent = new GreatQuestChunkListViewComponent(this);
        this.collectionEditorComponent = new CollectionEditorComponent<>(instance, this.chunkListComponent, true);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        this.chunkListComponent.extendParentUI();

        // Register the editor.
        if (getLeftSidePanelFreeArea() != null) {
            this.chunkListComponent.applyDefaultEditor(this.collectionEditorComponent);
            this.collectionEditorComponent.setMoveButtonLogic(null); // Prevent moving up/down.
            Node propertyListViewRootNode = this.collectionEditorComponent.getRootNode();
            VBox.setVgrow(propertyListViewRootNode, Priority.ALWAYS);
            getLeftSidePanelFreeArea().getChildren().add(propertyListViewRootNode);
            addController(this.collectionEditorComponent);
        }
    }

    @Override
    protected void onSelectedFileChange(GreatQuestChunkedFile oldChunkedFile, GreatQuestChunkedFile newChunkedFile) {
        super.onSelectedFileChange(oldChunkedFile, newChunkedFile);
        if (this.tempExtraController != null) {
            getRightSidePanelFreeArea().getChildren().remove(this.tempExtraController.getRootNode());
            removeController(this.tempExtraController);
            this.tempExtraController = null;
        }

        this.chunkListComponent.refreshDisplay();
        this.chunkListComponent.updateResourceGroupComboBox(newChunkedFile);
        this.collectionEditorComponent.updateEditorControls();
    }

    @Override
    public boolean trySetTargetFile(GameObject<?> file) {
        if (file != null && !(file instanceof GreatQuestChunkedFile))
            return false;

        setTargetFile((GreatQuestChunkedFile) file);
        return true;
    }

    @Getter
    public static class GreatQuestChunkListViewComponent extends ListViewComponent<GreatQuestInstance, kcCResource> {
        private final GreatQuestChunkFileEditor listComponent;
        private final ComboBox<GreatQuestChunkResourceCategory> resourceTypeComboBox;
        private final ComboBox<kcCResourceTableOfContents> resourceGroupComboBox;
        private final Region emptyRegion = new Region();
        private final CustomMenuItem addNewChunkItem = new CustomMenuItem(new Label("Add New"));
        private final CustomMenuItem importGqsFileItem = new CustomMenuItem(new Label("Import GQS File"));

        public GreatQuestChunkListViewComponent(GreatQuestChunkFileEditor listComponent) {
            super(listComponent.getGameInstance());
            this.listComponent = listComponent;
            this.resourceTypeComboBox = createResourceTypeComboBox();
            this.resourceGroupComboBox = createResourceGroupComboBox();
        }

        @Override
        public boolean matchesSearchQuery(kcCResource resource, String searchQuery) {
            if (!super.matchesSearchQuery(resource, searchQuery))
                return false;

            GreatQuestChunkResourceCategory category = this.resourceTypeComboBox != null ? this.resourceTypeComboBox.getValue() : null;
            return category == null || category == GreatQuestChunkResourceCategory.ALL || Utils.contains(category.getResourceIds(), resource.getChunkType());
        }

        @Override
        protected void onSelect(kcCResource resource) {
            // Update controls based on selection.
            this.listComponent.getCollectionEditorComponent().updateEditorControls();

            if (this.listComponent.tempExtraController != null) {
                this.listComponent.getRightSidePanelFreeArea().getChildren().remove(this.listComponent.tempExtraController.getRootNode());
                this.listComponent.removeController(this.listComponent.tempExtraController);
                this.listComponent.tempExtraController = null;
            }

            this.listComponent.tempExtraController = resource != null ? resource.createExtraUIController() : null;
            if (this.listComponent.tempExtraController != null) {
                this.listComponent.getRightSidePanelFreeArea().getChildren().add(this.listComponent.tempExtraController.getRootNode());
                this.listComponent.addController(this.listComponent.tempExtraController);
            }

            if (resource != null) {
                this.listComponent.getPropertyListViewer().showProperties(resource.createPropertyList());
            } else if (this.listComponent.getFile() != null) {
                this.listComponent.getPropertyListViewer().showProperties(this.listComponent.getFile().createPropertyList());
            } else {
                this.listComponent.getPropertyListViewer().clear();
            }
        }

        @Override
        protected void onDoubleClick(kcCResource resource) {
            if (resource != null)
                resource.handleDoubleClick();
        }

        @Override
        public List<kcCResource> getViewEntries() {
            if (this.listComponent == null || this.listComponent.getFile() == null)
                return Collections.emptyList();

            GreatQuestChunkedFile chunkedFile = this.listComponent.getFile();
            kcCResourceTableOfContents resourceGroup = this.resourceGroupComboBox != null ? this.resourceGroupComboBox.getValue() : null;
            if (resourceGroup != null && chunkedFile.getTableOfContents().contains(resourceGroup)) {
                return resourceGroup.getResourceChunks();
            } else {
                return chunkedFile.getChunks();
            }
        }

        @Override
        public void removeViewEntry(@NonNull kcCResource resource, boolean updateUI) {
            getListComponent().getFile().removeResource(resource);
            if (getRootNode().getSelectionModel().getSelectedItem() == resource)
                getRootNode().getSelectionModel().clearSelection();
            if (updateUI)
                refreshDisplay();
        }

        private void updateAddMenuEntries(GreatQuestChunkResourceCategory category) {
            this.addNewChunkItem.setDisable(category == GreatQuestChunkResourceCategory.ALL || category == GreatQuestChunkResourceCategory.UNUSED);
            if (category == GreatQuestChunkResourceCategory.OCTTREESCENEMGR && getViewEntries().size() >= 1) // Only allow one OctTreeMgr.
                this.addNewChunkItem.setDisable(true);
        }

        /**
         * Extends the parent UI.
         * This must be run after the parent UI has been loaded, since otherwise it can't work.
         */
        private void extendParentUI() {
            HBox.setHgrow(this.resourceGroupComboBox, Priority.NEVER);
            HBox.setHgrow(this.resourceTypeComboBox, Priority.NEVER);
            HBox.setHgrow(this.emptyRegion, Priority.ALWAYS); // Ensures the combo box is aligned to the right.
            getListComponent().getLeftSidePanelTopBox().getChildren().addAll(this.emptyRegion, this.resourceTypeComboBox);
            updateAddMenuEntries(this.resourceTypeComboBox.getValue());

            this.resourceTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue != newValue && newValue != null) {
                    getListComponent().getChunkListComponent().refreshDisplay();
                    getListComponent().getCollectionEditorComponent().updateEditorControls();
                    updateAddMenuEntries(newValue);
                }
            });

            this.resourceGroupComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue == newValue)
                    return;

                getListComponent().getChunkListComponent().refreshDisplay();
                getListComponent().getCollectionEditorComponent().updateEditorControls();
            });

            this.addNewChunkItem.setOnAction(event -> {
                FXUtils.showPopup(AlertType.ERROR, "Not supported.", "Chunks can be created by loading .gqs files.");
            });

            this.importGqsFileItem.setOnAction(event -> this.listComponent.getFile().askUserToImportGqsFile());

            getListComponent().getCollectionEditorComponent().addMenuItemToAddButtonLogic(this.addNewChunkItem);
            getListComponent().getCollectionEditorComponent().addMenuItemToAddButtonLogic(this.importGqsFileItem);
        }

        private void updateResourceGroupComboBox(GreatQuestChunkedFile newChunkedFile) {
            if (this.resourceGroupComboBox == null)
                return;

            // Update.
            ObservableList<kcCResourceTableOfContents> resourceGroups = getResourceGroups(newChunkedFile);
            ObservableList<Node> leftSidePanelChildren = getListComponent().getLeftSidePanelTopBox().getChildren();
            int groupComboBoxIndex = leftSidePanelChildren.indexOf(this.resourceGroupComboBox);
            if (resourceGroups != null && resourceGroups.size() > 0) {
                this.resourceGroupComboBox.setItems(resourceGroups);
                this.resourceGroupComboBox.getSelectionModel().selectFirst();
                if (groupComboBoxIndex < 0) // Add combo box.
                    leftSidePanelChildren.add(leftSidePanelChildren.indexOf(this.emptyRegion), this.resourceGroupComboBox);
            } else {
                this.resourceGroupComboBox.getSelectionModel().clearSelection();
                if (groupComboBoxIndex >= 0) // Hide combo box.
                    leftSidePanelChildren.remove(groupComboBoxIndex);
            }
        }

        private static ComboBox<GreatQuestChunkResourceCategory> createResourceTypeComboBox() {
            ComboBox<GreatQuestChunkResourceCategory> comboBox = new ComboBox<>(FXCollections.observableArrayList(GreatQuestChunkResourceCategory.values()));
            comboBox.getSelectionModel().select(GreatQuestChunkResourceCategory.ALL);
            FXUtils.applyComboBoxDisplaySettings(comboBox, () -> new AbstractAttachmentCell<>(
                    (resourceType, index) -> resourceType != null ? resourceType.getDisplayName() : null,
                    (resourceType, index) -> resourceType != null ? new ImageView(resourceType.getIcon().getFxImage()) : null));
            HBox.setMargin(comboBox, new Insets(0, 2, 0, 0));
            return comboBox;
        }

        private static ComboBox<kcCResourceTableOfContents> createResourceGroupComboBox() {
            ComboBox<kcCResourceTableOfContents> comboBox = new ComboBox<>();
            FXUtils.applyComboBoxDisplaySettings(comboBox, () -> new LazyFXListCell<>(
                    (resource, index) -> " Group " + (index - 1) + " (" + resource.getResourceChunks().size() + ")",
                    "All Groups"));
            return comboBox;
        }

        private static ObservableList<kcCResourceTableOfContents> getResourceGroups(GreatQuestChunkedFile chunkedFile) {
            if (chunkedFile == null)
                return FXCollections.emptyObservableList();

            List<kcCResourceTableOfContents> resourceGroups = chunkedFile.getTableOfContents();
            if (resourceGroups.size() <= 1)
                return FXCollections.emptyObservableList();

            resourceGroups = new ArrayList<>(resourceGroups);
            resourceGroups.add(0, null);
            return FXCollections.observableArrayList(resourceGroups);
        }
    }

    @Getter
    public enum GreatQuestChunkResourceCategory {
        ALL("All", ImageResource.GHIDRA_ICON_CHECKMARK_GREEN_16, KCResourceID.values()),
        RAW(KCResourceID.RAW),
        TEXTURE(KCResourceID.TEXTURE),
        OCTTREESCENEMGR(KCResourceID.OCTTREESCENEMGR),
        MODEL(KCResourceID.MODEL),
        TRACK(KCResourceID.TRACK),
        HIERARCHY(KCResourceID.HIERARCHY),
        ANIMSET(KCResourceID.ANIMSET),
        TRIMESH(KCResourceID.TRIMESH),
        GENERIC(KCResourceID.GENERIC),
        TOC(KCResourceID.TOC),
        ACTIONSEQUENCE(KCResourceID.ACTIONSEQUENCE),
        NAMEDHASH(KCResourceID.NAMEDHASH),
        ENTITYINST(KCResourceID.ENTITYINST),
        UNUSED("Unsupported", ImageResource.GHIDRA_ICON_RED_X_16, KCResourceID.NONE, KCResourceID.DUMMY, KCResourceID.ACTORDESC);

        private final String displayName;
        private final ImageResource icon;
        private final KCResourceID[] resourceIds;

        GreatQuestChunkResourceCategory(KCResourceID resourceId) {
            this(resourceId.getDisplayName(), resourceId.getIcon(), resourceId);
        }

        GreatQuestChunkResourceCategory(String displayName, ImageResource icon, KCResourceID... resourceIds) {
            this.displayName = displayName;
            this.icon = icon;
            this.resourceIds = resourceIds;
        }
    }
}
