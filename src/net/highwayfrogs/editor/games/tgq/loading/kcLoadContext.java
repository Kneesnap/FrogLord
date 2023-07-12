package net.highwayfrogs.editor.games.tgq.loading;

import lombok.Getter;
import net.highwayfrogs.editor.games.tgq.TGQBinFile;

/**
 * Contains all data shared between files for loading.
 * Created by Kneesnap on 7/11/2023.
 */
@Getter
public class kcLoadContext {
    private final TGQBinFile mainArchive;
    private final kcMaterialLoadContext materialLoadContext;

    public kcLoadContext(TGQBinFile mainArchive) {
        this.mainArchive = mainArchive;
        this.materialLoadContext = new kcMaterialLoadContext(mainArchive);
    }

    /**
     * Called when loading is completed.
     */
    public void onComplete() {
        this.materialLoadContext.onComplete();
    }
}
