package net.highwayfrogs.editor.games.konami.ancientshadow.ui;

import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowGameFile;
import net.highwayfrogs.editor.games.konami.ancientshadow.AncientShadowInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.GroupedCollectionViewComponent;

/**
 * A view component for the AncientShadowGameFiles in a game instance.
 * Created by Kneesnap on 8/4/2024.
 */
public class AncientShadowFileBasicListViewComponent extends GroupedCollectionViewComponent<AncientShadowInstance, AncientShadowGameFile> {
    public AncientShadowFileBasicListViewComponent(AncientShadowInstance instance) {
        super(instance);
    }

    @Override
    protected void setupViewEntryGroups() {
        addGroup(new LazyCollectionViewGroup<>("Files", value -> true));
    }

    @Override
    protected void onSelect(AncientShadowGameFile file) {
        GameUIController<?> controller = file.makeEditorUI();
        getGameInstance().getMainMenuController().showEditor(controller);
    }

    @Override
    public Iterable<AncientShadowGameFile> getViewEntries() {
        return getGameInstance().getAllGameFiles();
    }
}