package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcTrack;
import net.highwayfrogs.editor.games.konami.greatquest.chunks.kcCResourceSkeleton.kcNode;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * A single track key.
 * Created by Kneesnap on 5/2/2024.
 */
@Getter
public abstract class kcTrackKey<TSelf extends kcTrackKey<TSelf>> extends GameData<GreatQuestInstance> implements IInfoWriter {
    private final kcControlType controlType; // NOTE: Consider making not final later.
    @Setter private int tick;

    public kcTrackKey(GreatQuestInstance instance, kcControlType controlType) {
        super(instance);
        this.controlType = controlType;
    }

    @Override
    public void load(DataReader reader) {
        int startIndex = reader.getIndex();
        int expectedByteLength = getExpectedDataByteLength();
        int expectedEndIndex = startIndex + expectedByteLength;
        this.tick = reader.readInt();

        try {
            loadKeyData(reader, expectedEndIndex);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Failed to read %s of type '%s'.", Utils.getSimpleName(this), this.controlType);
            reader.setIndex(expectedEndIndex);
            return;
        }

        // Ensure the amount of bytes read was the expected amount.
        int endIndex = reader.getIndex();
        if (expectedEndIndex != reader.getIndex()) {
            int readByteCount = endIndex - startIndex;
            getLogger().severe("Failed to read track key '%s', which read %d byte(s), but was supposed to read %d.", Utils.getSimpleName(this), readByteCount, expectedByteLength);
            reader.setIndex(expectedEndIndex);
        }
    }

    @Override
    public void save(DataWriter writer) {
        int startIndex = writer.getIndex();
        int expectedKeyDataLength = getExpectedDataByteLength();
        int expectedEndIndex = startIndex + expectedKeyDataLength;

        // Write key data.
        writer.writeInt(this.tick);

        try {
            saveKeyData(writer, expectedEndIndex);
        } catch (Throwable th) {
            Utils.handleError(getLogger(), th, false, "Failed to write %s of type '%s'.", Utils.getSimpleName(this), this.controlType);
            writer.setIndex(expectedEndIndex);
            return;
        }

        // Get ending info.
        int realEndIndex = writer.getIndex();
        int writtenByteCount = realEndIndex - startIndex;
        if (expectedKeyDataLength != writtenByteCount) {
            getLogger().severe("The '%s' was expected to be %d byte(s), but %d were written.", this, expectedKeyDataLength, writtenByteCount);
            writer.setIndex(expectedEndIndex);
        }
    }

    /**
     * Loads main key data from the reader.
     * @param reader the reader to read data from
     * @param dataEndIndex the index which data is no longer readable.
     */
    protected abstract void loadKeyData(DataReader reader, int dataEndIndex);

    /**
     * Saves main key data to the writer.
     * @param writer the writer to write data to
     * @param dataEndIndex the index which data writing is complete
     */
    protected abstract void saveKeyData(DataWriter writer, int dataEndIndex);

    /**
     * Interpolates the value between the previous value and the current one, then applies it to the target system.
     * @param node the bone which is being calculated
     * @param previousKey the previous key to apply the interpolated value to
     * @param state the state to apply the interpolated value to
     * @param t a value between 0.0 and 1.0 representing how much to include from each
     */
    protected abstract void applyInterpolateValueImpl(kcNode node, TSelf previousKey, kcAnimState state, float t);

    /**
     * Interpolates the value between the previous value and the current one, then applies it to the target system.
     * @param node the bone which is being calculated
     * @param previousKey the previous key to apply the interpolated value to
     * @param state the state to apply the interpolated value to
     * @param t a value between 0.0 and 1.0 representing how much to include from each
     */
    @SuppressWarnings("unchecked")
    public void applyInterpolateValue(kcNode node, kcTrackKey<?> previousKey, kcAnimState state, float t) {
        if (previousKey != null && !getClass().isInstance(previousKey))
            throw new ClassCastException("Cannot use track key " + Utils.getSimpleName(previousKey) + " as if it is a " + getClass().getSimpleName() + ".");

        this.applyInterpolateValueImpl(node, (TSelf) previousKey, state, t);
    }

    /**
     * Copies the between the previous value and the current one, then applies it to the target system.
     * @param node the bone which is associated with the key
     * @param otherKey the key to copy values from
     */
    protected abstract void copyValueFromImpl(kcNode node, TSelf otherKey);

    /**
     * Interpolates the value between the previous value and the current one, then applies it to the target system.
     * @param node the bone which is associated with the key
     * @param otherKey the previous key to apply the interpolated value to
     */
    @SuppressWarnings("unchecked")
    public void copyValueFrom(kcNode node, kcTrackKey<?> otherKey) {
        if (otherKey != null && !getClass().isInstance(otherKey))
            throw new ClassCastException("Cannot copy from track key " + Utils.getSimpleName(otherKey) + " as if it is a " + getClass().getSimpleName() + ".");

        this.copyValueFromImpl(node, (TSelf) otherKey);
    }

    /**
     * When a new key is created, configure it, and change neighbors as necessary to accommodate.
     * @param track the track which this setup occurs on
     * @param node the bone which is associated with the key
     * @param oldNextKey the key which previously was the next key in the track (May = null)
     * @param newNextKey the key which is now the new next key in the track
     */
    protected abstract void setupNewNextKeyImpl(kcTrack track, kcNode node, TSelf oldNextKey, TSelf newNextKey);

    /**
     * When a new key is created, configure it, and change neighbors as necessary to accommodate.
     * @param track the track which this setup occurs on
     * @param node the bone which is associated with the key
     * @param oldNextKey the key which previously was the next key in the track (May = null)
     * @param newNextKey the key which is now the new next key in the track
     */
    @SuppressWarnings("unchecked")
    public void setupNewNextKey(kcTrack track, kcNode node, kcTrackKey<?> oldNextKey, kcTrackKey<?> newNextKey) {
        if (oldNextKey != null && !getClass().isInstance(oldNextKey))
            throw new ClassCastException("Cannot setup with track key " + Utils.getSimpleName(oldNextKey) + " as if it is a " + getClass().getSimpleName() + ".");
        if (!getClass().isInstance(newNextKey))
            throw new ClassCastException("Cannot setup the key " + Utils.getSimpleName(newNextKey) + " as if it is a " + getClass().getSimpleName() + ".");

        this.setupNewNextKeyImpl(track, node, (TSelf) oldNextKey, (TSelf) newNextKey);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        writeInfo(builder);
        return builder.toString();
    }

    @Override
    public void writeInfo(StringBuilder builder) {
        builder.append(Utils.getSimpleName(this)).append("['").append(getControlType())
                .append("']: Timestamp=").append(this.tick);
    }

    /**
     * Gets the size of the data.
     */
    public int getExpectedDataByteLength() {
        return this.controlType != null ? this.controlType.getProbablySize() : 0;
    }
}