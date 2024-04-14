package net.highwayfrogs.editor.games.konami.greatquest.loading;

import lombok.Getter;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestAssetBinFile;

/**
 * Contains all data shared between files for loading.
 * Created by Kneesnap on 7/11/2023.
 */
@Getter
public class kcLoadContext {
    private final GreatQuestAssetBinFile mainArchive;
    private final kcMaterialLoadContext materialLoadContext;

    public kcLoadContext(GreatQuestAssetBinFile mainArchive) {
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