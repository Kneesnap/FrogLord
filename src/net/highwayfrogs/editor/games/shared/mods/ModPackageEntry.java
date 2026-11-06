package net.highwayfrogs.editor.games.shared.mods;

import lombok.Getter;
import lombok.NonNull;

import java.util.function.Supplier;

/**
 * A content file held by a mod package: its zip path, size, SHA-1 and a supplier of its bytes.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class ModPackageEntry {
    @NonNull private final String path;
    private final long size;
    @NonNull private final String sha1;
    @NonNull private final Supplier<byte[]> dataSupplier;

    public ModPackageEntry(@NonNull String path, long size, @NonNull String sha1, @NonNull Supplier<byte[]> dataSupplier) {
        String pathProblem = ModFormatConstants.getPathProblem(path);
        if (pathProblem != null)
            throw new IllegalArgumentException("Invalid mod package path: " + pathProblem + ".");

        this.path = path;
        this.size = size;
        this.sha1 = sha1;
        this.dataSupplier = dataSupplier;
    }

    /**
     * Creates an entry holding the given bytes, calculating its size and SHA-1.
     * @param path the zip path of the entry
     * @param data the file contents
     * @return newEntry
     */
    public static ModPackageEntry fromBytes(@NonNull String path, @NonNull byte[] data) {
        return new ModPackageEntry(path, data.length, ModFormatConstants.calculateFileHash(data), () -> data);
    }

    /**
     * Gets the contents of the entry.
     * @return fileBytes
     */
    public byte[] getData() {
        return this.dataSupplier.get();
    }

    /**
     * Gets the canonical [Files] line describing this entry.
     * @return filesLine
     */
    public String toFilesLine() {
        return ModFormatConstants.formatFilesLine(this.sha1, this.size, this.path);
    }

    @Override
    public String toString() {
        return toFilesLine();
    }
}
