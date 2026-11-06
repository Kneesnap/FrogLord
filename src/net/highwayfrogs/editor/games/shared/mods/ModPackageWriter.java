package net.highwayfrogs.editor.games.shared.mods;

import lombok.NonNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a mod package as a zip: mod.cfg first, then the content files sorted by path, all deflated.
 * The writer computes the [Files] section and the content hash itself.
 * Created by jimbolina on 11/6/2026.
 */
public class ModPackageWriter {
    private static final long FIXED_ENTRY_TIME = 315619200000L;

    /**
     * Writes a package to a file. The package's [Files] section and content hash are updated first.
     * @param modPackage the package to write
     * @param outputFile the file to write
     * @throws IOException if writing fails
     */
    public void write(@NonNull ModPackage modPackage, @NonNull File outputFile) throws IOException {
        File parent = outputFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Could not create the folder '" + parent + "'.");

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            write(modPackage, outputStream);
        }
    }

    /**
     * Writes a package to a stream. The package's [Files] section and content hash are updated first.
     * @param modPackage the package to write
     * @param outputStream the stream to write; it is finished but not closed
     * @throws IOException if writing fails
     */
    public void write(@NonNull ModPackage modPackage, @NonNull OutputStream outputStream) throws IOException {
        for (ModPackageEntry entry : modPackage.getEntryList()) {
            String pathProblem = ModFormatConstants.getPathProblem(entry.getPath());
            if (pathProblem != null)
                throw new IOException("Cannot write the entry: " + pathProblem + ".");

            byte[] data = entry.getData();
            if (data.length != entry.getSize() || !ModFormatConstants.calculateFileHash(data).equalsIgnoreCase(entry.getSha1()))
                throw new IOException("The contents of '" + entry.getPath() + "' do not match its recorded size and SHA-1.");
        }

        modPackage.updateFiles();
        byte[] manifestBytes = modPackage.getManifest().toText().getBytes(StandardCharsets.UTF_8);

        ZipOutputStream zipStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
        zipStream.setMethod(ZipOutputStream.DEFLATED);
        zipStream.setLevel(Deflater.BEST_COMPRESSION);
        writeEntry(zipStream, ModFormatConstants.MANIFEST_NAME, manifestBytes);

        List<String> paths = new ArrayList<>(modPackage.getEntries().keySet());
        Collections.sort(paths);
        for (String path : paths)
            writeEntry(zipStream, path, modPackage.getEntry(path).getData());

        zipStream.finish();
        zipStream.flush();
    }

    private static void writeEntry(ZipOutputStream zipStream, String path, byte[] data) throws IOException {
        ZipEntry zipEntry = new ZipEntry(path);
        zipEntry.setTime(FIXED_ENTRY_TIME);
        zipStream.putNextEntry(zipEntry);
        zipStream.write(data);
        zipStream.closeEntry();
    }
}
