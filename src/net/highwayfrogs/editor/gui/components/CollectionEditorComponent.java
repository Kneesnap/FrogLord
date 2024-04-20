package net.highwayfrogs.editor.gui.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionViewComponent.ICollectionViewEntry;

/**
 * Previews a list of components...?
 * Icons: ADD [Show list of options as a right-click context menu], REMOVE, Shift Up, Shift Down (Allow disabling) [Split Menu Button?]
 * TODO: Finish
 * Created by Kneesnap on 4/12/2024.
 */
public class CollectionEditorComponent<TGameInstance extends GameInstance, TViewEntry extends ICollectionViewEntry> extends GameUIController<TGameInstance> {
    @Getter private final CollectionViewComponent<?, TViewEntry> collectionViewComponent;
    private final VBox verticalBox = new VBox();
    private final AnchorPane collectionViewPane = new AnchorPane();
    private final AnchorPane extraUiPane = new AnchorPane();
    private final VBox extraUiVerticalBox = new VBox();
    private final TextField searchTextField = new TextField();

    public CollectionEditorComponent(TGameInstance instance, CollectionViewComponent<TGameInstance, TViewEntry> collectionViewComponent) {
        super(instance);
        this.collectionViewComponent = collectionViewComponent;
        loadController(new AnchorPane());
    }

    @Override
    public AnchorPane getRootNode() {
        return (AnchorPane) super.getRootNode();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        setAnchorPaneStretch(this.verticalBox);
        getRootNode().getChildren().add(this.verticalBox);

        // Setup collection view pane.
        setAnchorPaneStretch(this.collectionViewPane);
        VBox.setVgrow(this.collectionViewPane, Priority.ALWAYS);
        this.verticalBox.getChildren().add(this.collectionViewPane);

        // Setup extra UI pane.
        setAnchorPaneStretch(this.extraUiPane);
        VBox.setVgrow(this.extraUiPane, Priority.NEVER);
        this.verticalBox.getChildren().add(this.extraUiPane);

        // Setup Extra UI vertical box.
        setAnchorPaneStretch(this.extraUiVerticalBox);
        this.extraUiPane.getChildren().add(this.extraUiVerticalBox);

        // Add UI.
        this.extraUiVerticalBox.getChildren().add(setupSearchTextField());
        this.extraUiVerticalBox.getChildren().add(createTestBox()); // TODO: Add UI buttons.

        // Setup list component.
        this.collectionViewPane.getChildren().add(this.collectionViewComponent.getRootNode());
    }

    private HBox setupSearchTextField() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);

        this.searchTextField.setAccessibleText("TEST1!!!"); // TODO: TOSS
        this.searchTextField.setPromptText("Search");
        this.searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (this.collectionViewComponent != null)
                this.collectionViewComponent.setSearchQuery(newValue);
        });
        HBox.setHgrow(this.searchTextField, Priority.ALWAYS);

        box.getChildren().add(this.searchTextField);
        return box;
    }

    private HBox createTestBox() {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setSpacing(10);
        box.getChildren().add(new Label("Test1"));
        box.getChildren().add(new Label("Test2"));
        box.getChildren().add(new Label("Test3"));
        box.getChildren().add(new Label("Test4"));
        return box;
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        if (this.collectionViewComponent != null)
            this.collectionViewComponent.onSceneAdd(newScene);
    }

    @Override
    public void onSceneRemove(Scene oldScene) {
        super.onSceneRemove(oldScene);
        if (this.collectionViewComponent != null)
            this.collectionViewComponent.onSceneRemove(oldScene);
    }

    /**
     * Gets the view entry currently selected.
     * @return selectedViewEntry
     */
    public TViewEntry getSelectedViewEntry() {
        return this.collectionViewComponent != null ? this.collectionViewComponent.getSelectedViewEntry() : null;
    }
}