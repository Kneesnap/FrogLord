package net.highwayfrogs.editor.games.sony.shared.vlo2.vram;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.utils.Utils;

/**
 * A container for per-VloFile data on the VloTree.
 * Created by Kneesnap on 1/19/2026.
 */
@Getter
public class VloFileTreeData {
    @NonNull private final VloTreeNode node;
    @NonNull private final MWIResourceEntry vloFileEntry; // Must use MWIResourceEntry so if a new VloFile is imported, the Vlo files can still be resolved.
    @NonNull private final VloVramSnapshot snapshot;
    @NonNull private final VloTextureIdTracker textureIdTracker;

    public VloFileTreeData(VloTreeNode node, MWIResourceEntry vloFileEntry) {
        this.node = node;
        this.vloFileEntry = vloFileEntry;
        this.snapshot = new VloVramSnapshot(vloFileEntry.getGameInstance(), node);
        this.textureIdTracker = new VloTextureIdTracker(node, node.getTextureIdTracker(), this);
    }

    /**
     * Returns the VloFile which this data is tracked for.
     */
    public VloFile getVloFile() {
        SCGameFile<?> gameFile = this.vloFileEntry.getGameFile();
        if (!(gameFile instanceof VloFile)) // Shouldn't happen, unless the MWI is configured wrong.
            throw new IllegalStateException("File '" + this.vloFileEntry.getDisplayName() + "' is not a VloFile! (Was: " + Utils.getSimpleName(gameFile) + ")");

        return (VloFile) gameFile;
    }
}
