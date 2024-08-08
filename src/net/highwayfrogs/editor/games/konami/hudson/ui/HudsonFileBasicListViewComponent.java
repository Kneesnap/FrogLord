package net.highwayfrogs.editor.games.konami.hudson.ui;

import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.GroupedCollectionViewComponent;

/**
 * A view component for the HudsonGameFiles in a game instance.
 * Created by Kneesnap on 8/8/2024.
 */
public class HudsonFileBasicListViewComponent<TGameInstance extends HudsonGameInstance> extends GroupedCollectionViewComponent<TGameInstance, HudsonGameFile> {
    public HudsonFileBasicListViewComponent(TGameInstance instance) {
        super(instance);
    }

    @Override
    protected void setupViewEntryGroups() {
        addGroup(new LazyCollectionViewGroup<>("Files", value -> true));
    }

    @Override
    protected void onSelect(HudsonGameFile file) {
        GameUIController<?> controller = file.makeEditorUI();
        getGameInstance().getMainMenuController().showEditor(controller);
    }

    @Override
    public Iterable<HudsonGameFile> getViewEntries() {
        return getGameInstance().getAllGameFiles();
    }
}