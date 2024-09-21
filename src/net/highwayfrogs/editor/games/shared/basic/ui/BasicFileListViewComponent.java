package net.highwayfrogs.editor.games.shared.basic.ui;

import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.games.shared.basic.file.definition.IGameFileDefinition;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.CollectionTreeViewComponent;

import java.util.Collection;

/**
 * A view component for the BasicGameFile in a game instance.
 * Created by Kneesnap on 8/12/2024.
 */
public class BasicFileListViewComponent<TGameInstance extends BasicGameInstance> extends CollectionTreeViewComponent<TGameInstance, BasicGameFile<?>> {
    public BasicFileListViewComponent(TGameInstance instance) {
        super(instance);
    }

    @Override
    protected CollectionViewTreeNode<BasicGameFile<?>> getOrCreateTreePath(CollectionViewTreeNode<BasicGameFile<?>> rootNode, BasicGameFile<?> gameFile) {
        return gameFile.getFileDefinition() != null ? gameFile.getFileDefinition().getOrCreateTreePath(rootNode, gameFile) : rootNode;
    }

    @Override
    protected void onSelect(BasicGameFile<?> file) {
        if (file == null)
            return;

        GameUIController<?> controller = file.makeEditorUI();
        getGameInstance().getMainMenuController().showEditor(controller);
    }

    @Override
    protected void onDoubleClick(BasicGameFile<?> file) {
        file.handleDoubleClick();
    }

    @Override
    public Collection<BasicGameFile<?>> getViewEntries() {
        return getGameInstance().getAllFiles();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compare(BasicGameFile<?> o1, BasicGameFile<?> o2) {
        IGameFileDefinition fileDefinition1 = o1.getFileDefinition();
        if (!(fileDefinition1 instanceof Comparable))
            return 0;

        IGameFileDefinition fileDefinition2 = o2.getFileDefinition();
        if (!fileDefinition1.getClass().isInstance(fileDefinition2))
            return 0;

        return ((Comparable<IGameFileDefinition>) fileDefinition1).compareTo(fileDefinition2);
    }
}