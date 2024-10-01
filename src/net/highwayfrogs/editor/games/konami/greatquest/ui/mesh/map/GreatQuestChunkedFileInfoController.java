package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;

/**
 * Shows information about the map.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestChunkedFileInfoController extends GreatQuestFileEditorUIController<GreatQuestChunkedFile> {
    @FXML private Button viewMeshBtn;

    public GreatQuestChunkedFileInfoController(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void setTargetFile(GreatQuestChunkedFile chunkedFile) {
        super.setTargetFile(chunkedFile);
        updateViewButton();
    }

    @Override
    protected void onControllerLoad(Node rootNode) {
        super.onControllerLoad(rootNode);
        updateViewButton();
    }

    private void updateViewButton() {
        if (this.viewMeshBtn != null) // Disable viewing maps if there is no map for the chunked file.
            this.viewMeshBtn.setDisable(getFile() == null || getFile().getSceneManager() == null);
    }

    @FXML
    private void onView(ActionEvent evt) {
        if (!getFile().openMeshViewer())
            throw new IllegalStateException("Cannot open the map mesh viewer, this is unexpected!");
    }
}