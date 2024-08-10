package net.highwayfrogs.editor.games.konami.hudson;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.generic.GameObject;
import net.highwayfrogs.editor.gui.components.CollectionTreeViewComponent.CollectionViewTreeNode;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Hudson file definition which came from the user's filesystem.
 * Created by Kneesnap on 8/4/2024.
 */
public class HudsonFileUserFSDefinition extends GameObject<HudsonGameInstance> implements IHudsonFileDefinition {
    @Getter @NonNull private final File file;
    private String cachedFullFileName;
    private CollectionViewTreeNode<HudsonGameFile> cachedTreePath;

    public HudsonFileUserFSDefinition(HudsonGameInstance instance, @NonNull File file) {
        super(instance);
        this.file = file;
    }

    @Override
    public String getFileName() {
        return this.file.getName();
    }

    @Override
    public String getFullFileName() {
        if (this.cachedFullFileName == null)
            this.cachedFullFileName = Utils.toLocalPath(getGameInstance().getMainGameFolder(), this.file, false);

        return this.cachedFullFileName;
    }

    @Override
    public CollectionViewTreeNode<HudsonGameFile> getOrCreateTreePath(CollectionViewTreeNode<HudsonGameFile> rootNode, HudsonGameFile gameFile) {
        if (this.cachedTreePath != null)
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
        CollectionViewTreeNode<HudsonGameFile> treeNode = rootNode;
        for (int i = folderPaths.size() - 1; i >= 0; i--)
            treeNode = treeNode.getOrCreateChildNode(folderPaths.get(i));

        return this.cachedTreePath = treeNode.addChildNode(gameFile);
    }
}