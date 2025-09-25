package net.highwayfrogs.editor.gui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;
import net.highwayfrogs.editor.utils.Utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Previews a list of components...?
 * Icons: ADD [Show list of options as a right-click context menu], REMOVE, Shift Up, Shift Down (Allow disabling) [Split Menu Button?]
 * Created by Kneesnap on 4/12/2024.
 */
public class CollectionEditorComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends GameUIController<TGameInstance> {
    @Getter private final CollectionViewComponent<?, TViewEntry> collectionViewComponent;
    @Getter private final boolean padCollectionView;
    @Getter private final VBox verticalBox = new VBox();
    @Getter private final AnchorPane collectionViewPane = new AnchorPane();
    @Getter private final AnchorPane extraUiPane = new AnchorPane();
    @Getter private final VBox extraUiVerticalBox = new VBox(1);
    private final TextField searchTextField = new TextField();
    private final MenuButton addButton = new MenuButton();
    private final Button removeButton = new Button();
    private final Button moveUpButton = new Button();
    private final Button moveDownButton = new Button();
    @Getter private Consumer<TViewEntry> removeButtonLogic;
    @Getter private BiConsumer<TViewEntry, EditorMoveButtonDirection> moveButtonLogic;

    public CollectionEditorComponent(TGameInstance instance, CollectionViewComponent<TGameInstance, TViewEntry> collectionViewComponent, boolean padCollectionView) {
        super(instance);
        this.collectionViewComponent = collectionViewComponent;
        this.padCollectionView = padCollectionView;
        addController(this.collectionViewComponent);
        loadController(new VBox(1));
    }

    @Override
    public VBox getRootNode() {
        return (VBox) super.getRootNode();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        setAnchorPaneStretch(this.verticalBox); // Does nothing if the parent is not an AnchorPane.
        VBox.setVgrow(this.verticalBox, Priority.ALWAYS); // Does nothing if the parent is not a VBox
        getRootNode().getChildren().add(this.verticalBox);

        // Setup collection view pane.
        Node collectionViewRootNode = this.collectionViewComponent.getRootNode();
        VBox.setVgrow(collectionViewRootNode, Priority.ALWAYS);
        if (this.padCollectionView) {
            VBox.setMargin(collectionViewRootNode, new Insets(0, 8, 0, 8)); // Make the box smaller than the surrounding controls (Aesthetics)
        } else {
            VBox.setMargin(collectionViewRootNode, new Insets(0, 1, 0, 1)); // Make the box smaller than the surrounding controls (Aesthetics)
        }

        this.verticalBox.getChildren().add(collectionViewRootNode);

        // Setup extra UI pane.
        setAnchorPaneStretch(this.extraUiPane);
        VBox.setVgrow(this.extraUiPane, Priority.NEVER);
        this.verticalBox.getChildren().add(this.extraUiPane);

        // Setup Extra UI vertical box.
        setAnchorPaneStretch(this.extraUiVerticalBox);
        this.extraUiPane.getChildren().add(this.extraUiVerticalBox);

        // Setup control buttons.
        this.addButton.setGraphic(new ImageView(ImageResource.GHIDRA_ICON_ADDITION_16.getFxImage()));
        this.removeButton.setGraphic(new ImageView(ImageResource.GHIDRA_ICON_SUBTRACTION_SIGN_16.getFxImage()));
        this.moveUpButton.setGraphic(new ImageView(ImageResource.GHIDRA_ICON_ARROW_UP_16.getFxImage()));
        this.moveDownButton.setGraphic(new ImageView(ImageResource.GHIDRA_ICON_ARROW_DOWN_16.getFxImage()));
        updateEditorControls();

        this.removeButton.setOnAction(evt -> {
            if (this.removeButtonLogic != null) {
                evt.consume();
                try {
                    this.removeButtonLogic.accept(getSelectedViewEntry());
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, true, "Failed to remove entry '%s'.", getSelectedViewEntry());
                }
            }
        });

        this.moveUpButton.setOnAction(evt -> {
            if (this.moveButtonLogic != null) {
                evt.consume();
                try {
                    this.moveButtonLogic.accept(getSelectedViewEntry(), EditorMoveButtonDirection.UP);
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, true, "Failed to move entry '%s' up.", getSelectedViewEntry());
                }
            }
        });

        this.moveDownButton.setOnAction(evt -> {
            if (this.moveButtonLogic != null) {
                evt.consume();
                try {
                    this.moveButtonLogic.accept(getSelectedViewEntry(), EditorMoveButtonDirection.DOWN);
                } catch (Throwable th) {
                    Utils.handleError(getLogger(), th, true, "Failed to move entry '%s' down.", getSelectedViewEntry());
                }
            }
        });

        // Add UI.
        this.extraUiVerticalBox.getChildren().add(setupSearchTextField());
        this.extraUiVerticalBox.getChildren().add(createControlsBox());
    }

    /**
     * Update the editor controls.
     */
    public void updateEditorControls() {
        this.addButton.setDisable(this.addButton.getItems().isEmpty());
        this.removeButton.setDisable(this.removeButtonLogic == null || getSelectedViewEntry() == null);
        this.moveUpButton.setDisable(this.moveButtonLogic == null || getSelectedViewEntry() == null);
        this.moveDownButton.setDisable(this.moveButtonLogic == null || getSelectedViewEntry() == null);
    }

    /**
     * Sets behavior regarding the menu to create when clicking the add menu icon.
     * @param newMenuItem the menu item to add
     */
    public void addMenuItemToAddButtonLogic(MenuItem newMenuItem) {
        if (newMenuItem == null)
            throw new NullPointerException("newMenuItem");

        this.addButton.getItems().add(newMenuItem);
        this.addButton.setDisable(false);
    }

    /**
     * Sets the behavior to run when the "remove" button is clicked.
     * @param newRemoveButtonLogic the remove button behavior
     */
    public void setRemoveButtonLogic(Consumer<TViewEntry> newRemoveButtonLogic) {
        this.removeButtonLogic = newRemoveButtonLogic;
        this.removeButton.setDisable(this.removeButtonLogic == null || getSelectedViewEntry() == null);
    }

    /**
     * Sets the behavior to run when either "move" button is clicked.
     * @param newMoveButtonLogic the move button behavior
     */
    public void setMoveButtonLogic(BiConsumer<TViewEntry, EditorMoveButtonDirection> newMoveButtonLogic) {
        this.moveButtonLogic = newMoveButtonLogic;
        this.moveUpButton.setDisable(this.moveButtonLogic == null || getSelectedViewEntry() == null);
        this.moveDownButton.setDisable(this.moveButtonLogic == null || getSelectedViewEntry() == null);
    }

    private HBox setupSearchTextField() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);

        this.searchTextField.setPromptText("Search");
        this.searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (this.collectionViewComponent != null) {
                this.collectionViewComponent.setSearchQuery(newValue);
                this.searchTextField.requestFocus();
            }
        });
        HBox.setHgrow(this.searchTextField, Priority.ALWAYS);
        HBox.setMargin(this.searchTextField, new Insets(1, 2, 0, 1));

        box.getChildren().add(this.searchTextField);
        return box;
    }

    private HBox createControlsBox() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setSpacing(10);
        box.getChildren().add(this.addButton);
        box.getChildren().add(this.removeButton);
        box.getChildren().add(this.moveUpButton);
        box.getChildren().add(this.moveDownButton);
        return box;
    }

    /**
     * Gets the view entry currently selected.
     * @return selectedViewEntry
     */
    public TViewEntry getSelectedViewEntry() {
        return this.collectionViewComponent != null ? this.collectionViewComponent.getSelectedViewEntry() : null;
    }

    @Getter
    @AllArgsConstructor
    public enum EditorMoveButtonDirection {
        UP(-1),
        DOWN(1);

        private final int offset;
    }
}