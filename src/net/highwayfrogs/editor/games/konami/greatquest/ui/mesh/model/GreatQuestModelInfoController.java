package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Shows information about the model.
 * Created by Kneesnap on 4/15/2024.
 */
public class GreatQuestModelInfoController extends GreatQuestFileEditorUIController<kcModelWrapper> {
    private final Button viewMeshButton;

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

    private void viewMesh(ActionEvent evt) {
        getFile().openMeshViewer();
    }
}