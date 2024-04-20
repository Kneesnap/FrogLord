package net.highwayfrogs.editor.games.konami.greatquest.script.interim;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameData;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;
import net.highwayfrogs.editor.games.konami.greatquest.script.kcScriptDisplaySettings;
import net.highwayfrogs.editor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an interim object which holds the core script data when loading/saving, but isn't very nice to work with.
 * This interim format is converted to/from a list of kcScript objects for saving/loading.
 * Created by Kneesnap on 6/26/2023.
 */
@Getter
public class kcScriptListInterim extends GameData<GreatQuestInstance> {
    private final GreatQuestChunkedFile chunkedFile;
    private final List<kcScriptTOC> entries;
    private final List<kcInterimScriptEffect> effects;
    private int[] causeData;
    private int causeReadIndex = -1;
    private int causeMaxIndex = -1;

    public kcScriptListInterim(GreatQuestChunkedFile chunkedFile, List<kcScriptTOC> entries, List<Integer> causeData, List<kcInterimScriptEffect> effects) {
        super(chunkedFile != null ? chunkedFile.getGameInstance() : null);
        this.chunkedFile = chunkedFile;
        this.entries = entries;
        this.effects = effects;

        this.causeData = new int[causeData.size()];
        for (int i = 0; i < causeData.size(); i++)
            this.causeData[i] = causeData.get(i);
    }

    public kcScriptListInterim(GreatQuestChunkedFile chunkedFile) {
        super(chunkedFile != null ? chunkedFile.getGameInstance() : null);
        this.chunkedFile = chunkedFile;
        this.entries = new ArrayList<>();
        this.effects = new ArrayList<>();
    }

    /**
     * Sets up the cause reader to read cause data.
     * @param startIndex The index to start reading from.
     * @param count      The amount of valid values.
     */
    public void setupCauseReader(int startIndex, int count) {
        this.causeReadIndex = startIndex;
        this.causeMaxIndex = startIndex + count;
    }

    /**
     * Read the next cause value.
     * @return nextCauseValue.
     */
    public int readNextCauseValue() {
        if (this.causeReadIndex < 0 || this.causeReadIndex >= this.causeMaxIndex)
            throw new RuntimeException("Cannot read the next cause value, it would go outside the range of valid data.");
        return this.causeData[this.causeReadIndex++];
    }

    /**
     * Test if there are more values to read.
     * @return hasMoreCauseValues
     */
    public boolean hasMoreCauseValues() {
        return this.causeMaxIndex > this.causeReadIndex;
    }

    /**
     * Finds a script effect written at a given offset from the first byte of script effect data.
     * @param byteOffset The offset (in bytes) to lookup.
     * @return Returns the index of the effect in the list, or null.
     */
    public int getEffectByOffset(long byteOffset) {
        int left = 0, right = this.effects.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            kcInterimScriptEffect midEffect = this.effects.get(mid);
            if (midEffect.getDataOffset() == byteOffset) {
                return mid;
            } else if (byteOffset > midEffect.getDataOffset()) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

    @Override
    public void load(DataReader reader) {
        // Read objects.
        this.entries.clear();
        long numObjects = reader.readUnsignedIntAsLong();
        for (int i = 0; i < numObjects; i++) {
            kcScriptTOC toc = new kcScriptTOC();
            toc.load(reader);
            this.entries.add(toc);
        }

        // 'Cause' Data
        int causeDataSize = reader.readInt();
        this.causeData = new int[causeDataSize];
        for (int i = 0; i < causeDataSize; i++)
            this.causeData[i] = reader.readInt();

        // 'Effect' data.
        this.effects.clear();
        int effectReadStart = reader.getIndex();
        int effectBytes = reader.readInt() * Constants.INTEGER_SIZE;

        long firstEffectStart = reader.getIndex();
        while (effectReadStart + effectBytes > reader.getIndex()) {
            kcInterimScriptEffect effect = new kcInterimScriptEffect(this.chunkedFile);
            effect.setDataOffset(reader.getIndex() - firstEffectStart);
            effect.load(reader);
            this.effects.add(effect);
        }
    }

    @Override
    public void save(DataWriter writer) {
        // Write objects.
        writer.writeInt(this.entries.size());
        for (int i = 0; i < this.entries.size(); i++)
            this.entries.get(i).save(writer);

        // 'Cause' Data
        writer.writeInt(this.causeData.length);
        for (int i = 0; i < this.causeData.length; i++)
            writer.writeInt(this.causeData[i]);

        // 'Effect' data.
        final int valuesPerEffect = (kcInterimScriptEffect.SIZE_IN_BYTES / Constants.INTEGER_SIZE);
        writer.writeInt(this.effects.size() * valuesPerEffect); // Number of 32bit integers comprising the effects.
        for (int i = 0; i < this.effects.size(); i++)
            this.effects.get(i).save(writer);
    }

    /**
     * Writes the script to a string builder.
     * @param builder  The builder to write the script to.
     * @param settings The settings to used to render the text.
     */
    public void toString(StringBuilder builder, kcScriptDisplaySettings settings) {
        if (this.entries.size() > 0) {
            builder.append("// Objects [").append(this.entries.size()).append("]:\n");
            for (int i = 0; i < this.entries.size(); i++) {
                kcScriptTOC toc = this.entries.get(i);
                builder.append("// Cause Types: ").append(Utils.toHexString(toc.getCauseTypes()))
                        .append(", ObjectCauseIndex: ").append(toc.getCauseStartIndex())
                        .append(", NumObjectCause: ").append(toc.getCauseCount())
                        .append(", NumObjectEffect: ").append(toc.getEffectCount())
                        .append('\n');
            }

            builder.append('\n');
        }

        // Write cause data.
        if (this.causeData != null && this.causeData.length > 0) {
            builder.append("// Cause Data [").append(this.causeData.length).append("]:\n//");
            for (int i = 0; i < this.causeData.length; i++) {
                if ((i % 12) == 0 && i > 0)
                    builder.append(this.causeData.length > i + 1 ? "\n//" : "\n");
                builder.append(' ').append(Utils.toHexString(this.causeData[i]));
            }

            builder.append('\n');
        }

        // Write effects.
        for (int i = 0; i < this.effects.size(); i++) {
            this.effects.get(i).toString(builder, settings);
            builder.append('\n');
        }
    }
}