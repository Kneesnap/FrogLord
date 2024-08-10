package net.highwayfrogs.editor.games.sony.shared.ui;

import lombok.Getter;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.components.GroupedCollectionViewComponent;

import java.util.Collection;
import java.util.function.BiPredicate;

/**
 * A view component for the SCGameFiles in a game instance.
 * Created by Kneesnap on 4/13/2024.
 */
public class SCGameFileGroupedListViewComponent<TGameInstance extends SCGameInstance> extends GroupedCollectionViewComponent<TGameInstance, SCGameFile<?>> {
    public SCGameFileGroupedListViewComponent(TGameInstance instance) {
        super(instance);
    }

    @Override
    protected void setupViewEntryGroups() {
        getGameInstance().setupFileGroups(this);
        addGroup(new SCGameFileListTypeIdGroup("WAD", WADFile.TYPE_ID));
        addGroup(new SCGameFileListTypeIdGroup("Uncategorized", 0));
    }

    @Override
    protected void onSelect(SCGameFile<?> file) {
        GameUIController<?> controller = file.makeEditorUI();
        getGameInstance().getMainMenuController().showEditor(controller);
    }

    @Override
    public Collection<SCGameFile<?>> getViewEntries() {
        return getGameInstance().getMainArchive().getFiles();
    }

    // Categorize other files by file ID.
    @Override
    protected CollectionViewGroup<SCGameFile<?>> createMissingEntryGroup(SCGameFile<?> gameFile) {
        int id = gameFile.getIndexEntry().getTypeId();
        return new SCGameFileListTypeIdGroup("Unknown File Type ID " + id, id);
    }

    /**
     * A group based on the File Type ID of the SCGameFile.
     */
    @Getter
    public static class SCGameFileListTypeIdGroup extends CollectionViewGroup<SCGameFile<?>> {
        private final int typeId;

        public SCGameFileListTypeIdGroup(String name, int typeId) {
            super(name);
            this.typeId = typeId;
        }

        @Override
        public boolean isPartOfGroup(SCGameFile<?> gameFile) {
            return gameFile.getIndexEntry().getTypeId() == this.typeId;
        }
    }

    /**
     * A file group based on whatever criteria is provided to the constructor.
     */
    public static class LazySCGameFileListGroup extends CollectionViewGroup<SCGameFile<?>>  {
        private final BiPredicate<SCGameFile<?>, FileEntry> predicate;

        public LazySCGameFileListGroup(String name, BiPredicate<SCGameFile<?>, FileEntry> predicate) {
            super(name);
            this.predicate = predicate;
        }

        @Override
        public boolean isPartOfGroup(SCGameFile<?> gameFile) {
            return this.predicate != null && this.predicate.test(gameFile, gameFile.getIndexEntry());
        }
    }
}