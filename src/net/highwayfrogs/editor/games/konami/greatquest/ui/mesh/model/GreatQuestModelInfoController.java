package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.model;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.model.kcModelWrapper;
import net.highwayfrogs.editor.games.konami.greatquest.ui.GreatQuestFileEditorUIController;

/**
 * Shows information about the model.
 * Created by Kneesnap on 4/15/2024.
 */
public class GreatQuestModelInfoController extends GreatQuestFileEditorUIController<kcModelWrapper> {
    public GreatQuestModelInfoController(GreatQuestInstance instance) {
        super(instance);
    }

    @FXML
    private void onView(ActionEvent evt) {
        getFile().openMeshViewer();
    }
}