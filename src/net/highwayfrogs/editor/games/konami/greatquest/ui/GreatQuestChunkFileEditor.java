package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.KCResourceID;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResource;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;
import net.highwayfrogs.editor.gui.components.ListViewComponent;
import net.highwayfrogs.editor.system.AbstractAttachmentCell;
import net.highwayfrogs.editor.system.AbstractStringConverter;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.Utils;

import java.util.Collections;
import java.util.List;

/**
 * Represents editor UI for the list of chunks in a chunked file.
 * Created by Kneesnap on 10/20/2024.
 */
@Getter
public class GreatQuestChunkFileEditor extends DefaultFileUIController<GreatQuestInstance, GreatQuestChunkedFile> {
    private final CollectionEditorComponent<GreatQuestInstance, kcCResource> collectionEditorComponent;
    private final GreatQuestChunkListViewComponent chunkListComponent;

    public GreatQuestChunkFileEditor(GreatQuestInstance instance) {
        this(instance, "Chunks");
    }

    public GreatQuestChunkFileEditor(GreatQuestInstance instance, String fileNameText) {
        this(instance, fileNameText, ImageResource.ZIPPED_FOLDER_16.getFxImage());
    }

    public GreatQuestChunkFileEditor(GreatQuestInstance instance, String fileNameText, Image icon) {
        super(instance, fileNameText, icon);
        this.chunkListComponent = new GreatQuestChunkListViewComponent(this);
        this.collectionEditorComponent = new CollectionEditorComponent<>(instance, this.chunkListComponent, true);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        this.chunkListComponent.extendParentUI();
        getContentBox().setPrefWidth(300);

        // Register the editor.
        if (getLeftSidePanelFreeArea() != null) {
            this.chunkListComponent.applyDefaultEditor(this.collectionEditorComponent);
            Node propertyListViewRootNode = this.collectionEditorComponent.getRootNode();
            VBox.setVgrow(propertyListViewRootNode, Priority.ALWAYS);
            getLeftSidePanelFreeArea().getChildren().add(propertyListViewRootNode);
            addController(this.collectionEditorComponent);
        }
    }

    @Override
    public void setTargetFile(GreatQuestChunkedFile newChunkedFile) {
        GreatQuestChunkedFile oldChunkedFile = getFile();
        super.setTargetFile(newChunkedFile);
        if (oldChunkedFile != newChunkedFile) {
            this.chunkListComponent.refreshDisplay();
            this.collectionEditorComponent.updateEditorControls();
        }
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
        private final CustomMenuItem addNewChunkItem = new CustomMenuItem(new Label("Add New"));

        public GreatQuestChunkListViewComponent(GreatQuestChunkFileEditor listComponent) {
            super(listComponent.getGameInstance());
            this.listComponent = listComponent;
            this.resourceTypeComboBox = createComboBox();
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
            if (this.listComponent != null && this.listComponent.getFile() != null) {
                return this.listComponent.getFile().getChunks();
            } else {
                return Collections.emptyList();
            }
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
            Region emptyRegion = new Region();
            HBox.setHgrow(emptyRegion, Priority.ALWAYS); // Ensures the combo box is aligned to the right.
            getListComponent().getLeftSidePanelTopBox().getChildren().addAll(emptyRegion, this.resourceTypeComboBox);
            updateAddMenuEntries(this.resourceTypeComboBox.getValue());

            this.resourceTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue != newValue && newValue != null) {
                    getListComponent().getChunkListComponent().refreshDisplay();
                    getListComponent().getCollectionEditorComponent().updateEditorControls();
                    updateAddMenuEntries(newValue);
                }
            });

            this.addNewChunkItem.setOnAction(event -> {
                // TODO: PS2 PAL FIX -> Multiple TOC Support.
                // TODO: SORTING? [Ensure loaded order matches what is expected, Disable our ability to manually order sections, and automatically sort sections to ensure ordering is OK.]
                FXUtils.makePopUp("Not yet implemented.", AlertType.ERROR); // TODO: IMPLEMENT!
            });

            getListComponent().getCollectionEditorComponent().addMenuItemToAddButtonLogic(this.addNewChunkItem);
        }

        private static ComboBox<GreatQuestChunkResourceCategory> createComboBox() {
            ComboBox<GreatQuestChunkResourceCategory> comboBox = new ComboBox<>(FXCollections.observableArrayList(GreatQuestChunkResourceCategory.values()));
            comboBox.getSelectionModel().select(GreatQuestChunkResourceCategory.ALL);
            comboBox.setConverter(new AbstractStringConverter<>(GreatQuestChunkResourceCategory::getDisplayName));
            comboBox.setCellFactory(listView -> new AbstractAttachmentCell<>((resourceType, index) -> resourceType != null ? resourceType.getDisplayName() : null,
                    (resourceType, index) -> resourceType != null ? new ImageView(resourceType.getIcon().getFxImage()) : null));
            return comboBox;
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
