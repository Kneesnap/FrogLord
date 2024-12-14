package net.highwayfrogs.editor.games.shared.basic.file.definition;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.data.GameObject;
import net.highwayfrogs.editor.games.shared.basic.BasicGameInstance;
import net.highwayfrogs.editor.games.shared.basic.file.BasicGameFile;
import net.highwayfrogs.editor.gui.components.CollectionTreeViewComponent.CollectionViewTreeNode;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a file which exists accessible via the java.io.File object, which is pretty much always local to the user's file-system.
 * Created by Kneesnap on 8/12/2024.
 */
public class PhysicalFileDefinition extends GameObject<BasicGameInstance> implements IGameFileDefinition {
    @Getter @NonNull private final File file;
    private String cachedFullFilePath;
    private CollectionViewTreeNode<BasicGameFile<?>> cachedTreePath;

    public PhysicalFileDefinition(BasicGameInstance instance, @NonNull File file) {
        super(instance);
        this.file = file;
    }

    @Override
    public String getFileName() {
        return this.file.getName();
    }

    @Override
    public String getFullFilePath() {
        if (this.cachedFullFilePath == null)
            this.cachedFullFilePath = FileUtils.toLocalPath(getGameInstance().getMainGameFolder(), this.file, false);

        return this.cachedFullFilePath;
    }

    @Override
    public CollectionViewTreeNode<BasicGameFile<?>> getOrCreateTreePath(CollectionViewTreeNode<BasicGameFile<?>> rootNode, BasicGameFile<?> gameFile) {
        if (this.cachedTreePath != null && this.cachedTreePath.isActive())
            return this.cachedTreePath;

        File mainGameFolder = getGameInstance().getMainGameFolder();

        // Get a list of the folder names.
        File tempFolder = this.file.getParentFile();
        List<String> folderPaths = new ArrayList<>();
        while (tempFolder != null && !Objects.equals(tempFolder, mainGameFolder)) {
            folderPaths.add(tempFolder.getName());
            tempFolder = tempFolder.getParentFile();
        }

        // Get tree nodes.
        CollectionViewTreeNode<BasicGameFile<?>> treeNode = rootNode;
        for (int i = folderPaths.size() - 1; i >= 0; i--)
            treeNode = treeNode.getOrCreateChildNode(folderPaths.get(i));

        return this.cachedTreePath = treeNode.addChildNode(gameFile);
    }
}