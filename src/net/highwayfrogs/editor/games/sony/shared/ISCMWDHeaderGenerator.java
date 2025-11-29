package net.highwayfrogs.editor.games.sony.shared;

import lombok.NonNull;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.SCSourceFileGenerator;
import net.highwayfrogs.editor.utils.FileUtils;

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

    /**
     * Generates the vlo source files for the given header file.
     * Automatically creates the source file based on the header file too.
     * @param instance the game instance to create with
     * @param headerFile the header file to create
     */
    default void generateVloSourceFiles(@NonNull SCGameInstance instance, @NonNull File headerFile) {
        String headerFileName = headerFile.getName();
        File sourceFile = new File(headerFile.getParentFile(),
                FileUtils.stripExtension(headerFileName) + (headerFileName.equals(headerFileName.toUpperCase()) ? ".C" : ".c"));

        SCSourceFileGenerator.generateVloResourceCFile(instance, sourceFile, headerFileName);
        SCSourceFileGenerator.generateVloHeaderFile(instance, headerFile, instance.getGameType().isAtLeast(SCGameType.MEDIEVIL));
    }
}
