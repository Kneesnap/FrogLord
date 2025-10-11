package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;
import net.highwayfrogs.editor.gui.ImageResource;
import net.highwayfrogs.editor.gui.components.mesh.Embedded3DViewComponent;

/**
 * Shows information about the model.
 * Created by Kneesnap on 4/15/2024.
 */
public class GreatQuestModelInfoController extends GreatQuestFileEditorUIController<kcModelWrapper> {
    private final Button viewMeshButton;
    private Embedded3DViewComponent<?> previewModelComponent;

    public GreatQuestModelInfoController(GreatQuestInstance instance) {
        super(instance, "Model File", ImageResource.GEOMETRIC_SHAPES_16);
        this.viewMeshButton = new Button("View");
        this.viewMeshButton.setOnAction(this::viewMesh);
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        Region emptyRegion = new Region();
        HBox.setHgrow(emptyRegion, Priority.ALWAYS); // Ensures the button is aligned to the right.
        getLeftSidePanelTopBox().getChildren().addAll(emptyRegion, this.viewMeshButton);
    }

    @Override
    protected void onSelectedFileChange(kcModelWrapper oldWrapper, kcModelWrapper newWrapper) {
        super.onSelectedFileChange(oldWrapper, newWrapper);
        if (this.previewModelComponent != null) {
            removeController(this.previewModelComponent);
            getLeftSideAnchorPane().getChildren().remove(this.previewModelComponent.getRootNode());
            this.previewModelComponent = null;
        }

        if (newWrapper != null) {
            this.previewModelComponent = newWrapper.createEmbeddedModelViewer();
            if (this.previewModelComponent != null) {
                addController(this.previewModelComponent);
                SubScene subScene = this.previewModelComponent.getRootNode();
                AnchorPane.setLeftAnchor(subScene, 2.0);
                AnchorPane.setRightAnchor(subScene, 2.0);
                AnchorPane.setTopAnchor(subScene, 35.0);
                subScene.setWidth(getLeftSideAnchorPane().getWidth() - 4);
                //subScene.setHeight(getLeftSideAnchorPane().getHeight() - 35); // Height is broken.
                getLeftSideAnchorPane().widthProperty().addListener((observable, oldValue, newValue) -> subScene.setWidth(newValue.doubleValue() - 4));
                //getLeftSideAnchorPane().heightProperty().addListener((observable, oldValue, newValue) -> subScene.setHeight(newValue.doubleValue() - 35));
                getLeftSideAnchorPane().getChildren().add(subScene);
            }
        }
    }

    private void viewMesh(ActionEvent evt) {
        getFile().openMeshViewer();
    }
}