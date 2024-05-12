package net.highwayfrogs.editor.games.sony.moonwarrior;

import javafx.scene.Node;
import javafx.scene.control.Button;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Manages the ability to view a map.
 * Created by Kneesnap on 5/8/2024.
 */
public class MoonWarriorMapUIController extends DefaultFileUIController<MoonWarriorInstance, MoonWarriorMap> {
    private Button viewButton;

    public MoonWarriorMapUIController(MoonWarriorInstance instance) {
        super(instance, "Map File", ImageResource.TREASURE_MAP_15.getFxImage());
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
        this.viewButton.setDisable(getFile() == null || getFile().getPolygonPacket() == null);
    }

    @Override
    public void setTargetFile(MoonWarriorMap newMap) {
        super.setTargetFile(newMap);
        if (this.viewButton != null)
            this.viewButton.setDisable(newMap == null || newMap.getPolygonPacket() == null);
    }
}