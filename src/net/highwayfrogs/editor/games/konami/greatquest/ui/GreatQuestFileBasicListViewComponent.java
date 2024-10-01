package net.highwayfrogs.editor.games.konami.greatquest.ui;

import javafx.scene.Scene;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestGameFile;
import net.highwayfrogs.editor.gui.components.ListViewComponent;

import java.util.List;

/**
 * A view component for the GreatQuestArchiveFiles in a game instance.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestFileBasicListViewComponent extends ListViewComponent<GreatQuestInstance, GreatQuestGameFile> {
    public GreatQuestFileBasicListViewComponent(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void onSceneAdd(Scene newScene) {
        super.onSceneAdd(newScene);
        if (getSelectedViewEntry() == null && getEntries().size() > 0)
            getRootNode().getSelectionModel().selectFirst();
    }

    @Override
    protected void onSelect(GreatQuestGameFile file) {
        if (file != null)
            getGameInstance().getMainMenuController().showEditor(file);
    }

    @Override
    protected void onDoubleClick(GreatQuestGameFile file) {
        file.handleDoubleClick();
    }

    @Override
    public List<GreatQuestGameFile> getViewEntries() {
        return getGameInstance().getAllFiles();
    }
}