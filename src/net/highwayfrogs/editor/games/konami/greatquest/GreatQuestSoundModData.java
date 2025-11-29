package net.highwayfrogs.editor.games.konami.greatquest;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.games.generic.data.GameData;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contains sound modding information.
 * Created by Kneesnap on 10/10/2025.
 */
public class GreatQuestSoundModData extends GameData<GreatQuestInstance> {
    private final Map<Integer, String> userSoundFilePathsById = new HashMap<>();
    private final Map<String, Integer> userSoundFileIdsByPaths = new HashMap<>();

    public static final String SIGNATURE = GreatQuestModData.SIGNATURE;
    private static final short CURRENT_VERSION = 0;

    public GreatQuestSoundModData(GreatQuestInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        reader.verifyString(SIGNATURE);
        short version = reader.readUnsignedByteAsShort();
        if (version > CURRENT_VERSION)
            throw new RuntimeException("The version of FrogLord data included in the sound file (v" + version
                    + ") is not supported in this version of FrogLord! (v" + CURRENT_VERSION
                    + ") Try updating FrogLord to a newer version.");

        GreatQuestModData.readStringByIdMap(reader, this.userSoundFilePathsById);

        // Automatic recreation of the opposite mapping.
        for (Entry<Integer, String> entry : this.userSoundFilePathsById.entrySet())
            this.userSoundFileIdsByPaths.put(entry.getValue(), entry.getKey());

        reader.alignRequireEmpty(Constants.INTEGER_SIZE);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeStringBytes(SIGNATURE);
        writer.writeUnsignedByte(CURRENT_VERSION);
        GreatQuestModData.writeStringByIdMap(writer, this.userSoundFilePathsById);
        writer.align(Constants.INTEGER_SIZE);
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
}

