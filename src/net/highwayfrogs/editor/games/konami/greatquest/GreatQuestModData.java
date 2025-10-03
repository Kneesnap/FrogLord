package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

/**
 * Represents custom data saved to a custom instance of Frogger: The Great Quest.
 * Created by Kneesnap on 10/2/2025.
 */
public class GreatQuestModData extends GameData<GreatQuestInstance> {
    private final Map<Integer, String> userSoundFilePathsById = new HashMap<>();
    private final Map<String, Integer> userSoundFileIdsByPaths = new HashMap<>();
    @Getter private final Map<Integer, String> userGlobalFilePaths = new HashMap<>();

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

        readStringByIdMap(reader, this.userSoundFilePathsById);
        readStringByIdMap(reader, this.userGlobalFilePaths);

        // Automatic recreation of the opposite mapping.
        for (Entry<Integer, String> entry : this.userSoundFilePathsById.entrySet())
            this.userSoundFileIdsByPaths.put(entry.getValue(), entry.getKey());

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
        writeStringByIdMap(writer, this.userSoundFilePathsById);
        writeStringByIdMap(writer, this.userGlobalFilePaths);
        writer.writeStringBytes(SIGNATURE);
        writer.writeInt(headerAddress);
    }

    /**
     * Gets the full sound file path for the given sound ID, if there is one.
     * @param soundId the sound ID to resolve.
     * @return fullSoundPath, or null if there is none.
     */
    public String getUserFullSoundPath(int soundId) {
        return this.userSoundFilePathsById.get(soundId);
    }

    /**
     * Gets the full sound file path for the given sound ID.
     * @param fullPath the path to resolve.
     * @return sfxId, or -1 if the sound effect ID could not be found.
     */
    public int getSfxIdFromFullSoundPath(String fullPath) {
        Integer sfxId = this.userSoundFileIdsByPaths.get(fullPath);
        return sfxId != null ? sfxId : -1;
    }

    /**
     * Sets the SFX path to use for the user created sound.
     * @param sfxId the SFX ID to apply to the user created sound
     * @param fullPath the full path to use for the user created sound ID.
     */
    public void setUserSfxFullPath(int sfxId, String fullPath) {
        if (sfxId < 0 || sfxId > getGameInstance().getNextFreeSoundId())
            throw new IllegalArgumentException("Invalid sfxId: " + sfxId);
        if (getGameInstance().getSoundPathsById().containsKey(sfxId))
            throw new IllegalArgumentException("There is already an original game sound using the ID " + sfxId + ", so the path '" + fullPath + "' cannot be assigned to that ID.");
        if (getGameInstance().getSoundIdsByPath().containsKey(fullPath))
            throw new IllegalArgumentException("There is already another SFX using the path '" + fullPath + "', so SFX ID " + sfxId + " cannot use that path.");

        Integer oldSfxId = this.userSoundFileIdsByPaths.get(fullPath);
        if (oldSfxId != null) {
            if (oldSfxId == sfxId)
                return; // No change.

            throw new IllegalArgumentException("There is already another SFX (ID: " + oldSfxId + ") using the path '" + fullPath + "', so SFX ID " + sfxId + " cannot use that path.");
        }

        boolean hasNewPath = !StringUtils.isNullOrWhiteSpace(fullPath);
        String oldPath = hasNewPath ? this.userSoundFilePathsById.put(sfxId, fullPath) : this.userSoundFilePathsById.remove(sfxId);
        if (oldPath != null && !this.userSoundFilePathsById.remove(sfxId, oldPath))
            throw new IllegalStateException("Failed to remove path '" + oldPath + "' as SFX ID: " + sfxId + " from the userSoundFilePathsById tracking.");

        if (hasNewPath)
            this.userSoundFileIdsByPaths.put(fullPath, sfxId);
    }

    private static Map<Integer, String> readStringByIdMap(DataReader reader, Map<Integer, String> results) {
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


    private static void writeStringByIdMap(DataWriter writer, Map<Integer, String> stringIdMap) {
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
