package net.highwayfrogs.editor.games.shared.mods;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.*;

/**
 * An in-memory mod package: the manifest, the content files in order, the content hash and the problems found when it was read.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class ModPackage {
    @NonNull private final ModManifest manifest;
    private final Map<String, ModPackageEntry> entries = new LinkedHashMap<>();
    private final List<ModProblem> problems = new ArrayList<>();
    @Setter private String contentHash;

    public ModPackage(@NonNull ModManifest manifest) {
        this.manifest = manifest;
    }

    /**
     * Gets the content files, in order. The manifest is not included.
     * @return entries
     */
    public Collection<ModPackageEntry> getEntryList() {
        return Collections.unmodifiableCollection(this.entries.values());
    }

    /**
     * Gets the entries map, keyed by zip path.
     * @return entries
     */
    public Map<String, ModPackageEntry> getEntries() {
        return Collections.unmodifiableMap(this.entries);
    }

    /**
     * Gets a content file by its zip path.
     * @param path the zip path
     * @return entry, or null
     */
    public ModPackageEntry getEntry(String path) {
        return this.entries.get(path);
    }

    /**
     * Gets the bytes of a content file.
     * @param path the zip path
     * @return fileBytes, or null if the package does not hold the path
     */
    public byte[] getEntryData(String path) {
        ModPackageEntry entry = this.entries.get(path);
        return entry != null ? entry.getData() : null;
    }

    /**
     * Adds or replaces a content file.
     * @param entry the entry to add
     */
    public void addEntry(@NonNull ModPackageEntry entry) {
        if (ModFormatConstants.MANIFEST_NAME.equals(entry.getPath()))
            throw new IllegalArgumentException("The manifest is not a content file.");

        this.entries.put(entry.getPath(), entry);
    }

    /**
     * Adds or replaces a content file from bytes.
     * @param path the zip path
     * @param data the file contents
     * @return newEntry
     */
    public ModPackageEntry addEntry(@NonNull String path, @NonNull byte[] data) {
        ModPackageEntry entry = ModPackageEntry.fromBytes(path, data);
        addEntry(entry);
        return entry;
    }

    /**
     * Removes a content file.
     * @param path the zip path
     * @return the removed entry, or null
     */
    public ModPackageEntry removeEntry(String path) {
        return this.entries.remove(path);
    }

    /**
     * Gets the canonical [Files] lines for the current content files, sorted by path.
     * @return filesLines
     */
    public List<String> createFilesLines() {
        List<String> paths = new ArrayList<>(this.entries.keySet());
        Collections.sort(paths);

        List<String> lines = new ArrayList<>(paths.size());
        for (String path : paths)
            lines.add(this.entries.get(path).toFilesLine());

        return lines;
    }

    /**
     * Rewrites the manifest's [Files] section and the content hash from the current content files.
     */
    public void updateFiles() {
        List<String> lines = createFilesLines();
        this.contentHash = ModFormatConstants.calculateContentHash(lines);
        this.manifest.setFiles(this.contentHash, lines);
    }

    /**
     * Returns true if any problem found on read is an error.
     */
    public boolean hasErrors() {
        return ModProblem.hasErrors(this.problems);
    }

    /**
     * Records a problem.
     * @param problem the problem to record
     */
    public void addProblem(@NonNull ModProblem problem) {
        this.problems.add(problem);
    }
}
