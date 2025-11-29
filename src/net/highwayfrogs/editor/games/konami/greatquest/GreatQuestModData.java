package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

/**
 * Represents custom data saved to a custom instance of Frogger: The Great Quest.
 * Created by Kneesnap on 10/2/2025.
 */
@Getter
public class GreatQuestModData extends GameData<GreatQuestInstance> {
    private final Map<Integer, String> userGlobalFilePaths = new HashMap<>();

    public static final String SIGNATURE = "FROGLORD";
    private static final short CURRENT_VERSION = 0;

    public GreatQuestModData(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        int headerAddress = reader.getIndex();
        reader.verifyString(SIGNATURE);
        short version = reader.readUnsignedByteAsShort();
        if (version > CURRENT_VERSION)
            throw new RuntimeException("The version of FrogLord data included in this bin file (v" + version
                    + ") is not supported in this version of FrogLord! (v" + CURRENT_VERSION
                    + ") Try updating FrogLord to a newer version.");

        readStringByIdMap(reader, this.userGlobalFilePaths);

        // Final load.
        reader.verifyString(SIGNATURE);
        int readHeaderAddress = reader.readInt();
        if (readHeaderAddress != headerAddress)
            throw new RuntimeException("The FrogLord data header was expected to start at 0x" + NumberUtils.to0PrefixedHexString(readHeaderAddress) + ", but actually started at 0x" + NumberUtils.to0PrefixedHexString(headerAddress) + ".");
    }

    @Override
    public void save(DataWriter writer) {
        int headerAddress = writer.getIndex();
        writer.writeStringBytes(SIGNATURE);
        writer.writeUnsignedByte(CURRENT_VERSION);
        writeStringByIdMap(writer, this.userGlobalFilePaths);
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(headerAddress);
    }

    /**
     * Read a string by ID mapping.
     * @param reader the reader to read from
     * @param results the map to store the results in
     * @return results
     */
    static Map<Integer, String> readStringByIdMap(DataReader reader, Map<Integer, String> results) {
        if (results != null) {
            results.clear();
        } else {
            results = new HashMap<>();
        }

        int entryCount = reader.readInt();
        for (int i = 0; i < entryCount; i++) {
            int id = reader.readInt();
            int strLength = reader.readUnsignedShortAsInt();
            String value = reader.readTerminatedString(strLength, StandardCharsets.US_ASCII);
            results.put(id, value);
        }

        return results;
    }


    /**
     * Write a string by ID mapping.
     * @param writer the writer to write to
     * @param stringIdMap the map to write
     */
    static void writeStringByIdMap(DataWriter writer, Map<Integer, String> stringIdMap) {
        List<Entry<Integer, String>> entries = new ArrayList<>(stringIdMap.entrySet());
        entries.sort(Comparator.comparingInt(Entry::getKey));

        writer.writeInt(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            Entry<Integer, String> entry = entries.get(i);
            writer.writeInt(entry.getKey());

            // Write string length.
            int strLengthPos = writer.getIndex();
            writer.writeUnsignedShort(0);
            int writtenByteCount = writer.writeStringBytes(entry.getValue());

            // Write string data.
            writer.jumpTemp(strLengthPos);
            writer.writeUnsignedShort(writtenByteCount);
            writer.jumpReturn();
        }
    }
}
