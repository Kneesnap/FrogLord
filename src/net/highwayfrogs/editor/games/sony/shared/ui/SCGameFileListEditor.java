package net.highwayfrogs.editor.games.sony.shared.ui;

import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.components.CollectionEditorComponent;

/**
 * Implements the list editor
 * TODO: Consider for deletion, unless abstract methods are added to the parent.
 * Created by Kneesnap on 4/13/2024.
 */
public class SCGameFileListEditor<TGameInstance extends SCGameInstance> extends CollectionEditorComponent<TGameInstance, SCGameFile<?>> {
    public SCGameFileListEditor(TGameInstance instance) {
        super(instance,  new SCGameFileGroupedListViewComponent<>(instance), false);
    }
}