package net.highwayfrogs.editor.games.konami.hudson.ui;

import net.highwayfrogs.editor.games.konami.hudson.HudsonGameFile;
import net.highwayfrogs.editor.games.konami.hudson.HudsonGameInstance;
import net.highwayfrogs.editor.games.konami.hudson.IHudsonFileDefinition;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionTreeViewComponent;

import java.util.Collection;

/**
 * A view component for the HudsonGameFiles in a game instance.
 * Created by Kneesnap on 8/8/2024.
 */
public class HudsonFileBasicListViewComponent<TGameInstance extends HudsonGameInstance> extends CollectionTreeViewComponent<TGameInstance, HudsonGameFile> {
    public HudsonFileBasicListViewComponent(TGameInstance instance) {
        super(instance);
    }

    @Override
    protected CollectionViewTreeNode<HudsonGameFile> getOrCreateTreePath(CollectionViewTreeNode<HudsonGameFile> rootNode, HudsonGameFile gameFile) {
        return gameFile.getFileDefinition() != null ? gameFile.getFileDefinition().getOrCreateTreePath(rootNode, gameFile) : rootNode;
    }

    @Override
    protected void onSelect(HudsonGameFile file) {
        GameUIController<?> controller = file.makeEditorUI();
        getGameInstance().getMainMenuController().showEditor(controller);
    }

    @Override
    public Collection<HudsonGameFile> getViewEntries() {
        return getGameInstance().getAllFiles();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compare(HudsonGameFile o1, HudsonGameFile o2) {
        IHudsonFileDefinition fileDefinition1 = o1.getFileDefinition();
        if (!(fileDefinition1 instanceof Comparable))
            return 0;

        IHudsonFileDefinition fileDefinition2 = o2.getFileDefinition();
        if (!fileDefinition1.getClass().isInstance(fileDefinition2))
            return 0;

        return ((Comparable<IHudsonFileDefinition>) fileDefinition1).compareTo(fileDefinition2);
    }
}