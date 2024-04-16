package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Shows information about the map.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestMapInfoController extends GreatQuestFileEditorUIController<GreatQuestChunkedFile> {
    public GreatQuestMapInfoController(GreatQuestInstance instance) {
        super(instance);
    }

    @FXML
    private void onView(ActionEvent evt) {
        MeshViewController.setupMeshViewer(getGameInstance(), new GreatQuestMapMeshController(), new GreatQuestMapMesh(getFile()));
    }
}