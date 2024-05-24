package net.highwayfrogs.editor.games.sony.shared.model;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.DefaultFileUIController;

/**
 * Represents the UI controller for an action set file.
 * Created by Kneesnap on 5/21/2024.
 */
@Getter
public class PTModelFileUIController<TGameFile extends SCGameFile<TGameInstance>, TGameInstance extends SCGameInstance> extends DefaultFileUIController<TGameInstance, TGameFile> {
    private Button viewButton;

    public PTModelFileUIController(TGameInstance instance, String fileNameText, Image icon) {
        super(instance, fileNameText, icon);
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
        this.viewButton.setDisable(getFile() == null);
    }

    @Override
    public void setTargetFile(TGameFile newMap) {
        super.setTargetFile(newMap);
        if (this.viewButton != null)
            this.viewButton.setDisable(newMap == null);
    }
}