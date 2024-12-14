package net.highwayfrogs.editor.games.shared.basic.file.definition;

import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.gui.components.CollectionTreeViewComponent.CollectionViewTreeNode;

import java.io.File;

/**
 * Defines where a file came from.
 * Created by Kneesnap on 8/12/2024.
 */
public interface IGameFileDefinition {
    /**
     * Gets the game instance for the file.
     */
    BasicGameInstance getGameInstance();

    /**
     * Gets the individual file name.
     */
    String getFileName();

    /**
     * Gets the full file path, if known.
     */
    String getFullFilePath();

    /**
     * If there is a file object associated with this file definition, return it! Otherwise, return null.
     */
    File getFile();

    /**
     * Gets or creates the tree node for the file.
     * @param rootNode the root node to create the path from
     * @param gameFile the game file corresponding to this file definition
     */
    CollectionViewTreeNode<BasicGameFile<?>> getOrCreateTreePath(CollectionViewTreeNode<BasicGameFile<?>> rootNode, BasicGameFile<?> gameFile);
}