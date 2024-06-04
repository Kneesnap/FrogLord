package net.highwayfrogs.editor.games.shared.coupled;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData.SharedGameData;
import net.highwayfrogs.editor.games.generic.GameInstance;

/**
 * Represents a coupled data entry.
 * Created by Kneesnap on 5/13/2024.
 */
public abstract class CoupledDataEntry<TPeer extends CoupledDataEntry<?>> extends SharedGameData {
    @Getter private CoupledDataParent parent;
    @Getter private boolean firstEntry;
    private DataReader cachedReader;

    public CoupledDataEntry(GameInstance instance) {
        super(instance);
    }

    @Override
    public final void load(DataReader reader) {
        if (this.cachedReader == null && reader != null)
            this.cachedReader = reader;

        loadIfPossible(reader);
    }

    /**
     * Loads the raw data from the reader.
     * @param reader The reader to load information from.
     * @param other the other entry
     * @return true iff no more reading need occur.
     */
    public abstract boolean load(DataReader reader, TPeer other);

    @Override
    public final void save(DataWriter writer) {
        save(writer, getPeer());
    }

    /**
     * Saves the raw data to the writer.
     * @param writer The writer to write information to.
     * @param other the other entry
     */
    public abstract void save(DataWriter writer, TPeer other);

    /**
     * Sets the attached parent.
     * @param parent The parent to apply.
     */
    void setParent(CoupledDataParent parent, boolean firstEntry) {
        if (this.parent == parent)
            return;

        this.parent = parent;
        this.firstEntry = firstEntry;
        onParentChange(parent);
    }

    /**
     * Called when the active coupled data parent changes.
     * @param newParent the new coupled data parent.
     */
    protected void onParentChange(CoupledDataParent newParent) {
        if (newParent != null)
            loadIfPossible(null);
    }

    private void loadIfPossible(DataReader reader) {
        if (reader == null)
            reader = this.cachedReader;

        if (reader == null || !reader.hasMore()) {
            this.cachedReader = null;
            return; // No more data to read!
        }

        // Load and toss the reader if complete.
        TPeer peer = getPeer();
        if (load(reader, peer))
            this.cachedReader = null;

        // Try to load the other one now in-case we have better options.
        if (peer != null && ((CoupledDataEntry<?>) peer).cachedReader != null)
            ((CoupledDataEntry<?>) peer).loadIfPossible(null);
    }

    /**
     * Gets the peer object linked to this object, if there is one.
     */
    @SuppressWarnings("unchecked")
    public TPeer getPeer() {
        if (this.parent == null) {
            return null;
        } else if (this.firstEntry) {
            return (TPeer) this.parent.getSecondData();
        } else {
            return (TPeer) this.parent.getFirstData();
        }
    }
}