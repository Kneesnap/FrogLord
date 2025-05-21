package net.highwayfrogs.editor.games.sony.shared.map.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.gui.DefaultFileUIController;
import net.highwayfrogs.editor.gui.ImageResource;

/**
 * Manages the ability to view an SCMapFile.
 * Created by Kneesnap on 5/12/2024.
 */
@Getter
public class SCMapFileUIController<TGameFile extends SCMapFile<TGameInstance>, TGameInstance extends SCGameInstance> extends DefaultFileUIController<TGameInstance, TGameFile> {
    private Button viewButton;

    public SCMapFileUIController(TGameInstance instance) {
        super(instance, "Map File", ImageResource.TREASURE_MAP_16.getFxImage());
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        this.viewButton = new Button("View");
        getLeftSidePanelTopBox().getChildren().add(this.viewButton);
        this.viewButton.setOnMouseClicked(evt -> {
            if (getFile() != null)
                getFile().performDefaultUIAction();
        });
        this.viewButton.setDisable(getFile() == null || getFile().getPolygonPacket() == null);
    }

    @Override
    public void setTargetFile(TGameFile newMap) {
        super.setTargetFile(newMap);
        if (this.viewButton != null)
            this.viewButton.setDisable(newMap == null || newMap.getPolygonPacket() == null);
    }
}