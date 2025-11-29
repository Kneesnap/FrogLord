package net.highwayfrogs.editor.games.konami.greatquest.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.konami.greatquest.GreatQuestInstance;

/**
 * Represents the different available archive file types.
 * Created by Kneesnap on 11/6/2025.
 */
@Getter
@RequiredArgsConstructor
public enum GreatQuestArchiveFileType {
    CHUNKED_FILE(".dat"),
    ICON(".ico"),
    IMAGE(".img"),
    MODEL(".vtx");

    private final String extension;

    /**
     * Determine the default compression state for a resource of this type.
     * @param instance the game instance to determine compression state with
     * @return if the default state of a file of this type is being compressed.
     */
    public boolean isCompressedByDefault(GreatQuestInstance instance) {
        if (instance == null)
            throw new NullPointerException("instance");

        if (instance.isPS2()) {
            return true; // All assets appear compressed on PS2.
        } else if (instance.isPC()) {
            switch (this) {
                case CHUNKED_FILE:
                    return false;
                case ICON:
                case IMAGE:
                case MODEL:
                    return true;
                default:
                    throw new UnsupportedOperationException("Unsupported enum value: " + this);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported game platform: " + instance.getPlatform());
        }
    }

    /**
     * Gets the file type from the given file path.
     * @param filePath the file path to get the type from
     * @param fallbackValue the fallback value to return if
     * @return fileType
     */
    public static GreatQuestArchiveFileType getFileTypeFromFilePath(String filePath, GreatQuestArchiveFileType fallbackValue) {
        if (filePath != null) {
            filePath = filePath.toLowerCase();
            for (int i = 0; i < values().length; i++) {
                GreatQuestArchiveFileType fileType = values()[i];
                if (filePath.endsWith(fileType.getExtension()))
                    return fileType;
            }
        }

        return fallbackValue;
    }
}
