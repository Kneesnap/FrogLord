package net.highwayfrogs.editor.games.konami.greatquest.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
