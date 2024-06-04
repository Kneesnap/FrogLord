package net.highwayfrogs.editor.games.konami.greatquest.animation.key;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.IInfoWriter;
import net.highwayfrogs.editor.games.konami.greatquest.animation.kcControlType;
import net.highwayfrogs.editor.utils.Utils;

/**
 * A single track key.
 * Created by Kneesnap on 5/2/2024.
 */
@Getter
public abstract class kcTrackKey extends GameData<GreatQuestInstance> implements IInfoWriter {
    private final kcControlType controlType; // NOTE: Consider making not final later.
    private int timeStamp; // TODO: Turn into more widely recognized units.

    public kcTrackKey(GreatQuestInstance instance, kcControlType controlType) {
        super(instance);
        this.controlType = controlType;
    }

    @Override
    public void load(DataReader reader) {
        int startIndex = reader.getIndex();
        int expectedByteLength = getExpectedDataByteLength();
        int expectedEndIndex = startIndex + expectedByteLength;
        this.timeStamp = reader.readInt();

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
            getLogger().severe("Failed to read track key '" + Utils.getSimpleName(this) + "', which read " + readByteCount + " bytes, but was supposed to read " + expectedByteLength + ".");
            reader.setIndex(expectedEndIndex);
        }
    }

    @Override
    public void save(DataWriter writer) {
        int startIndex = writer.getIndex();
        int expectedKeyDataLength = getExpectedDataByteLength();
        int expectedEndIndex = startIndex + expectedKeyDataLength;

        // Write key data.
        writer.writeInt(this.timeStamp);

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
            getLogger().severe("The '" + this + "' was expected to be " + expectedKeyDataLength + " bytes, but " + writtenByteCount + " were written.");
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        writeInfo(builder);
        return builder.toString();
    }

    /**
     * Gets the size of the data.
     */
    public int getExpectedDataByteLength() {
        return this.controlType != null ? this.controlType.getProbablySize() : 0;
    }
}