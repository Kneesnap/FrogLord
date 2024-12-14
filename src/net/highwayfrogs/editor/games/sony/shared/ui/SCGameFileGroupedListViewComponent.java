package net.highwayfrogs.editor.games.sony.shared.ui;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
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
        if (file != null)
            getGameInstance().getMainMenuController().showEditor(file);
    }

    @Override
    protected void onDoubleClick(SCGameFile<?> file) {
        file.handleWadEdit(null);
    }

    @Override
    public Collection<SCGameFile<?>> getViewEntries() {
        return getGameInstance().getMainArchive().getFiles();
    }

    // Categorize other files by file ID.
    @Override
    protected CollectionViewGroup<SCGameFile<?>> createMissingEntryGroup(SCGameFile<?> gameFile) {
        MWIResourceEntry mwiEntry = gameFile.getIndexEntry();
        if (mwiEntry != null) {
            return new SCGameFileListTypeIdGroup("Unknown File Type ID " + mwiEntry.getTypeId(), mwiEntry.getTypeId());
        } else {
            return new LazySCGameFileListGroup("User Files", (file, index) -> index == null, true);
        }
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
            MWIResourceEntry mwiEntry = gameFile.getIndexEntry();
            return mwiEntry != null && mwiEntry.getTypeId() == this.typeId;
        }
    }

    /**
     * A file group based on whatever criteria is provided to the constructor.
     */
    public static class LazySCGameFileListGroup extends CollectionViewGroup<SCGameFile<?>>  {
        private final BiPredicate<SCGameFile<?>, MWIResourceEntry> predicate;
        private final boolean allowNullMwiEntry;

        public LazySCGameFileListGroup(String name, BiPredicate<SCGameFile<?>, MWIResourceEntry> predicate) {
            this(name, predicate, false);
        }

        public LazySCGameFileListGroup(String name, BiPredicate<SCGameFile<?>, MWIResourceEntry> predicate, boolean allowNullMwiEntry) {
            super(name);
            this.predicate = predicate;
            this.allowNullMwiEntry = allowNullMwiEntry;
        }

        @Override
        public boolean isPartOfGroup(SCGameFile<?> gameFile) {
            MWIResourceEntry mwiEntry = gameFile.getIndexEntry();
            return this.predicate != null && (this.allowNullMwiEntry || mwiEntry != null) && this.predicate.test(gameFile, mwiEntry);
        }
    }
}