package net.highwayfrogs.editor.games.sony.shared;

import lombok.NonNull;

import java.io.File;

/**
 * Indicates the object can generate an MWD C header.
 * Created by Kneesnap on 8/24/2025.
 */
public interface ISCMWDHeaderGenerator {
    /**
     * Generates an MWD C header for compiling code with.
     * This may not indicate code is compileable for the given version, or that this file is right.
     * @param file targetFile
     */
    void generateMwdCHeader(@NonNull File file);
}
