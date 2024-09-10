package net.highwayfrogs.editor.games.konami.greatquest.ui;

import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestGameFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.gui.components.GroupedCollectionViewComponent;

import java.util.Collection;

/**
 * A view component for the GreatQuestArchiveFiles in a game instance.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestFileBasicListViewComponent extends GroupedCollectionViewComponent<GreatQuestInstance, GreatQuestGameFile> {
    public GreatQuestFileBasicListViewComponent(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    protected void setupViewEntryGroups() {
        addGroup(new LazyCollectionViewGroup<>("Files", value -> true));
    }

    @Override
    protected void onSelect(GreatQuestGameFile file) {
        getGameInstance().getMainMenuController().showEditor(file);
    }

    @Override
    public Collection<GreatQuestGameFile> getViewEntries() {
        return getGameInstance().getAllFiles();
    }
}