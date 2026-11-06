package net.highwayfrogs.editor.games.shared.mods;

import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads a mod package zip into a ModPackage, checking entry paths, the manifest profile and the [Files] fingerprint.
 * Problems are recorded on the returned package; only an unreadable zip throws.
 * Created by jimbolina on 11/6/2026.
 */
public class ModPackageReader {
    @Getter private final ModManifestValidator validator;
    private static final String MACOS_METADATA_FOLDER = "__MACOSX/";
    private static final String MACOS_FOLDER_FILE = ".DS_Store";

    /**
     * Creates a reader which checks paths and [Files] but does not validate the manifest profile.
     */
    public ModPackageReader() {
        this(null);
    }

    /**
     * Creates a reader.
     * @param validator the validator to check the manifest with, or null to skip profile validation
     */
    public ModPackageReader(ModManifestValidator validator) {
        this.validator = validator;
    }

    /**
     * Reads a package from a file.
     * @param inputFile the .mod file
     * @param problems the list to add problems to; the same problems are also recorded on the returned package
     * @return modPackage, or null if no manifest could be read; problems are added to the list either way
     * @throws IOException if the file is not a readable zip
     */
    public ModPackage read(File inputFile, List<ModProblem> problems) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFile))) {
            return read(inputStream, problems);
        }
    }

    /**
     * Reads a package from a stream. The stream is read to its end but not closed.
     * @param inputStream the zip data
     * @param problems the list to add problems to; the same problems are also recorded on the returned package
     * @return modPackage, or null if no manifest could be read
     * @throws IOException if the data is not a readable zip
     */
    public ModPackage read(InputStream inputStream, List<ModProblem> problems) throws IOException {
        Map<String, byte[]> rawEntries = new LinkedHashMap<>();
        ZipInputStream zipStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8);
        ZipEntry zipEntry;
        while ((zipEntry = zipStream.getNextEntry()) != null) {
            String name = zipEntry.getName();
            byte[] data = readFully(zipStream);
            zipStream.closeEntry();
            if (zipEntry.isDirectory() || name.endsWith("/") || isIgnoredMetadata(name))
                continue;

            String pathProblem = ModFormatConstants.getPathProblem(name);
            if (pathProblem != null) {
                problems.add(ModProblem.error(null, null, "Rejected zip entry: " + pathProblem + "."));
                continue;
            }

            if (rawEntries.put(name, data) != null)
                problems.add(ModProblem.error(null, null, "The zip holds '" + name + "' more than once."));
        }

        String prefix = findRootPrefix(rawEntries.keySet(), problems);
        if (prefix == null)
            return null;

        byte[] manifestBytes = rawEntries.remove(prefix + ModFormatConstants.MANIFEST_NAME);
        String manifestText = new String(manifestBytes, StandardCharsets.UTF_8);
        if (manifestText.startsWith("﻿")) {
            problems.add(ModProblem.warning(null, null, "mod.cfg starts with a byte order mark."));
            manifestText = manifestText.substring(1);
        }

        ModManifest manifest = ModManifest.parse(manifestText, problems);
        if (manifest == null)
            return null;

        ModPackage modPackage = new ModPackage(manifest);
        for (Map.Entry<String, byte[]> entry : rawEntries.entrySet()) {
            String path = entry.getKey();
            if (!path.startsWith(prefix)) {
                problems.add(ModProblem.warning(null, null, "'" + path + "' is outside the package folder and is ignored."));
                continue;
            }

            modPackage.addEntry(ModPackageEntry.fromBytes(path.substring(prefix.length()), entry.getValue()));
        }

        if (this.validator != null)
            problems.addAll(this.validator.validate(manifest, modPackage.getEntries().keySet()));

        verifyFiles(modPackage, problems);
        for (ModProblem problem : problems)
            modPackage.addProblem(problem);

        return modPackage;
    }

    /**
     * Checks the [Files] section against the content files and sets the package's content hash.
     * Without a [Files] section the content hash is calculated from the files and a warning is added.
     * @param modPackage the package to check
     * @param problems the list to add problems to
     */
    public static void verifyFiles(ModPackage modPackage, List<ModProblem> problems) {
        String section = ModFormatConstants.SECTION_FILES;
        ModManifest manifest = modPackage.getManifest();
        List<String> lines = manifest.getFilesLines();
        if (lines == null) {
            problems.add(ModProblem.warning(section, null, "The manifest has no [Files] section, so the contents cannot be verified."));
            modPackage.setContentHash(ModFormatConstants.calculateContentHash(modPackage.createFilesLines()));
            return;
        }

        List<String> canonicalLines = new ArrayList<>(lines.size());
        Set<String> listedPaths = new HashSet<>();
        for (String line : lines) {
            String[] parsed = ModFormatConstants.parseFilesLine(line);
            if (parsed == null) {
                problems.add(ModProblem.error(section, null, "'" + line + "' is not a '<sha1> <size> <path>' line."));
                continue;
            }

            String sha1 = parsed[0];
            long size = Long.parseLong(parsed[1]);
            String path = parsed[2];
            canonicalLines.add(ModFormatConstants.formatFilesLine(sha1, size, path));
            listedPaths.add(path);

            ModPackageEntry entry = modPackage.getEntry(path);
            if (entry == null) {
                problems.add(ModProblem.error(section, null, "'" + path + "' is listed but missing from the package."));
            } else if (entry.getSize() != size) {
                problems.add(ModProblem.error(section, null, "'" + path + "' is " + entry.getSize() + " bytes, but [Files] says " + size + "."));
            } else if (!entry.getSha1().equalsIgnoreCase(sha1)) {
                problems.add(ModProblem.error(section, null, "'" + path + "' does not match its SHA-1 in [Files]."));
            }
        }

        for (String path : modPackage.getEntries().keySet())
            if (!listedPaths.contains(path))
                problems.add(ModProblem.error(section, null, "'" + path + "' is in the package but not listed in [Files]."));

        String calculatedHash = ModFormatConstants.calculateContentHash(canonicalLines);
        String declaredHash = manifest.getContentHash();
        if (declaredHash == null) {
            problems.add(ModProblem.warning(section, ModFormatConstants.KEY_CONTENT_HASH, "[Files] has no contentHash."));
        } else if (!declaredHash.equalsIgnoreCase(calculatedHash)) {
            problems.add(ModProblem.error(section, ModFormatConstants.KEY_CONTENT_HASH, "contentHash " + declaredHash + " does not match the [Files] lines (" + calculatedHash + ")."));
        }

        modPackage.setContentHash(calculatedHash);
    }

    private static String findRootPrefix(Set<String> paths, List<ModProblem> problems) {
        if (paths.contains(ModFormatConstants.MANIFEST_NAME))
            return "";

        String folder = null;
        for (String path : paths) {
            int slash = path.indexOf('/');
            String top = slash >= 0 ? path.substring(0, slash + 1) : "";
            if (folder == null) {
                folder = top;
            } else if (!folder.equals(top)) {
                folder = "";
                break;
            }
        }

        if (folder != null && !folder.isEmpty() && paths.contains(folder + ModFormatConstants.MANIFEST_NAME))
            return folder;

        problems.add(ModProblem.error(null, null, "The package has no mod.cfg at its root or inside a single root folder."));
        return null;
    }

    private static boolean isIgnoredMetadata(String name) {
        return name.startsWith(MACOS_METADATA_FOLDER) || name.equals(MACOS_FOLDER_FILE) || name.endsWith("/" + MACOS_FOLDER_FILE);
    }

    private static byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1)
            outputStream.write(buffer, 0, read);

        return outputStream.toByteArray();
    }
}
