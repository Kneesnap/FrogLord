package net.highwayfrogs.editor.games.sony.shared.mof2.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModelType;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Represents the UI shown when selecting a MOF file.
 * Created by Kneesnap on 2/21/2025.
 */
public class MRModelFileUIController extends DefaultFileUIController<SCGameInstance, MRModel> {
    private Button viewButton;

    public MRModelFileUIController(SCGameInstance instance) {
        super(instance, "MOF Model File", ImageResource.GEOMETRIC_SHAPES_16.getFxImage());
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        this.viewButton = new Button("View");
        getLeftSidePanelTopBox().getChildren().add(this.viewButton);
        this.viewButton.setOnMouseClicked(evt -> {
            if (getFile() != null)
                getFile().handleWadEdit(null);
        });
        this.viewButton.setDisable(getFile() == null || getFile().getModelType() == MRModelType.DUMMY);
    }

    @Override
    public void setTargetFile(MRModel newModel) {
        super.setTargetFile(newModel);
        if (this.viewButton != null)
            this.viewButton.setDisable(newModel == null || newModel.getModelType() == MRModelType.DUMMY);
    }
}