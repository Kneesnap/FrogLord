package net.highwayfrogs.editor.games.sony.frogger.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Utilities for handling differences between Frogger versions.
 * Created by Kneesnap on 2/8/2023.
 */
public class FroggerVersionComparison {
    private static final Map<String, FroggerGameBuild> gameBuildsByName = new HashMap<>();
    private static final List<FroggerGameBuild> gameBuilds = new ArrayList<>();
    private static File versionConfigFile;

    private static final Set<String> SKIPPED_FILE_LINKS = new HashSet<>(Arrays.asList("GEN_FROG.XMR", "GEN_FROG.XAR"));

    /**
     * Generates a report of the differences.
     */
    public static void generateReport() {
        Map<String, String> linkedFiles = new HashMap<>();

        // 1) Link files by name.
        for (FroggerGameBuild build : gameBuilds) {
            for (FroggerGameFileEntry entry : build.getFiles()) {
                if (linkedFiles.containsKey(entry.getFullPath()) || SKIPPED_FILE_LINKS.contains(entry.getFileName()))
                    continue; // Already been linked.

                if (entry.getFullPath().endsWith(".XMR"))
                    linkedFiles.put(entry.getFullPath().replace(".XMR", ".XAR"), entry.getFullPath());
                if (entry.getFullPath().endsWith(".XAR"))
                    linkedFiles.put(entry.getFullPath().replace(".XAR", ".XMR"), entry.getFullPath());
            }
        }

        // 2) Link files from different builds to objects which represent the singular file.
        Map<String, FroggerFileTracker> trackerMap = new HashMap<>();
        List<FroggerFileTracker> fileTrackers = new ArrayList<>();

        for (FroggerGameBuild build : gameBuilds) {
            for (FroggerGameFileEntry entry : build.getFiles()) {
                if (entry.getFullPath().endsWith(".WAD"))
                    continue; // Skip .WAD files, since we only care about the files inside the WAD, not the WAD itself.

                String linkedPath = linkedFiles.getOrDefault(entry.getFullPath(), entry.getFullPath());

                FroggerFileTracker tracker = trackerMap.get(linkedPath);
                if (tracker == null) {
                    trackerMap.put(entry.getFullPath(), tracker = new FroggerFileTracker(entry.getFullPath()));
                    fileTrackers.add(tracker);
                }

                tracker.getFiles().add(entry);
            }
        }

        StringBuilder buildByFile = new StringBuilder();
        Set<String> seenHashes = new HashSet<>();
        for (FroggerFileTracker tracker : fileTrackers) {
            buildByFile.append(tracker.getFileIdentifier()).append(":").append(Constants.NEWLINE);

            FroggerGameFileEntry lastFileEntry = null;
            for (FroggerGameFileEntry entry : tracker.getFiles()) {

                if (lastFileEntry != null && !lastFileEntry.getFullPath().equals(entry.getFullPath()))
                    buildByFile.append(" - RENAMED: ").append(entry.getBuild().getBuildName()).append(", ").append(entry.getFullPath()).append(Constants.NEWLINE);

                if (lastFileEntry != null) {
                    boolean didChange = (lastFileEntry.getFileSize() != entry.getFileSize()) || !lastFileEntry.getSha1Hash().equals(entry.getSha1Hash());
                    if (didChange) {
                        buildByFile.append(" - CHANGE:  ").append(entry.getBuild().getBuildName()).append(", Size: ").append(entry.getFileSize()).append(", SHA1: ").append(entry.getSha1Hash());
                        tracker.getUniqueFiles().add(entry);
                    } else {
                        lastFileEntry = entry;
                        continue;
                    }
                } else {
                    buildByFile.append(" - INITIAL: ").append(entry.getBuild().getBuildName()).append(", Size: ").append(entry.getFileSize()).append(",SHA1: ").append(entry.getSha1Hash());
                    tracker.getUniqueFiles().add(entry);
                }

                if (!seenHashes.add(entry.getSha1Hash()))
                    buildByFile.append(" **FILE ALREADY SEEN**");
                buildByFile.append(Constants.NEWLINE);

                lastFileEntry = entry;
            }

            FroggerGameFileEntry lastFile = tracker.getFiles().size() > 0 ? tracker.getFiles().get(tracker.getFiles().size() - 1) : null;
            if (lastFile != null && !lastFile.getBuild().getBuildName().contains("retail"))
                buildByFile.append(" - REMOVED: The last version this file was seen was ").append(lastFile.getBuild().getBuildName()).append(".").append(Constants.NEWLINE);

            buildByFile.append(Constants.NEWLINE);
        }

        FroggerGameBuild lastBuild = null;
        StringBuilder buildByVersion = new StringBuilder();
        for (FroggerGameBuild build : gameBuilds) {
            buildByVersion.append(build.getBuildName()).append(":").append(Constants.NEWLINE);

            // Find changed files.
            int changeCount = 0;
            for (FroggerGameFileEntry entry : build.getFiles()) {
                // Find tracker.
                String linkedPath = linkedFiles.getOrDefault(entry.getFullPath(), entry.getFullPath());
                FroggerFileTracker tracker = trackerMap.get(linkedPath);
                if (tracker == null)
                    continue; // Probably wad file.
                if (!tracker.getUniqueFiles().contains(entry))
                    continue; // Duplicate of another file.

                changeCount++;
                buildByVersion.append(" - ").append(entry.getFileName());

                int fullTrackerIndex = tracker.getFiles().indexOf(entry);
                FroggerGameFileEntry changedFrom = fullTrackerIndex > 0 ? tracker.getFiles().get(fullTrackerIndex - 1) : null;
                boolean wasSeenInLastBuild = (changedFrom != null) && (lastBuild == changedFrom.getBuild());

                if (changedFrom == null) {
                    buildByVersion.append(" was seen for the first time.").append(Constants.NEWLINE);
                } else if (wasSeenInLastBuild) {
                    buildByVersion.append(" changed from ").append(changedFrom.getBuild().getBuildName()).append(".").append(Constants.NEWLINE);
                } else {
                    buildByVersion.append(" was re-added.").append(Constants.NEWLINE);
                }
            }

            // Find removed files.
            if (lastBuild != null) {
                for (FroggerGameFileEntry entry : lastBuild.getFiles()) {
                    String linkedPath = linkedFiles.getOrDefault(entry.getFullPath(), entry.getFullPath());
                    FroggerFileTracker tracker = trackerMap.get(linkedPath);
                    if (tracker == null)
                        continue; // Probably wad file.

                    int index = tracker.getFiles().indexOf(entry);
                    FroggerGameFileEntry nextEntry = index >= 0 && tracker.getFiles().size() > index + 1 ? tracker.getFiles().get(index + 1) : null;

                    if (nextEntry == null || nextEntry.getBuild() != build)
                        buildByVersion.append(" - ").append(entry.getFileName()).append(" was deleted.").append(Constants.NEWLINE);
                }
            }

            buildByVersion.append("Results: ").append(changeCount).append("/").append(build.getFiles().size())
                    .append(" files should be looked at.").append(Constants.NEWLINE).append(Constants.NEWLINE);

            // Next build!
            lastBuild = build;
        }

        try {
            Files.write(new File(versionConfigFile.getParentFile(), "report-by-file.txt").toPath(), buildByFile.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(new File(versionConfigFile.getParentFile(), "report-by-version.txt").toPath(), buildByVersion.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Failed to write report to file.");
        }

        System.out.println("Reports saved to text files.");
    }

    /**
     * Adds a new version of the game to the version config.
     * @param versionToAdd The version of the game to add.
     */
    public static void addNewVersionToConfig(FroggerGameInstance versionToAdd) {
        if (!isEnabled())
            return;

        if (gameBuildsByName.containsKey(versionToAdd.getConfig().getInternalName())) {
            System.out.println("This build is already tracked in the version cache.");
            return;
        }

        FroggerGameBuild newBuild = new FroggerGameBuild(versionToAdd.getConfig().getInternalName());
        for (MWIResourceEntry entry : versionToAdd.getArchiveIndex().getEntries())
            if (entry != null && entry.getFullFilePath() != null)
                newBuild.getFiles().add(new FroggerGameFileEntry(newBuild, entry.getFullFilePath().replace('/', '\\'), entry.getUnpackedSize(), entry.getSha1Hash()));

        // Store.
        gameBuilds.add(newBuild);
        gameBuildsByName.put(newBuild.getBuildName(), newBuild);

        // Save config.
        saveToConfig();
        System.out.println("Added new version to config.");
    }

    /**
     * Represents a file tracked over time.
     */
    @Getter
    private static class FroggerFileTracker {
        private final String fileIdentifier;
        private final List<FroggerGameFileEntry> files = new ArrayList<>();
        private final List<FroggerGameFileEntry> uniqueFiles = new ArrayList<>();

        public FroggerFileTracker(String fileIdentifier) {
            this.fileIdentifier = fileIdentifier;
        }
    }

    @Getter
    private static class FroggerGameBuild {
        private final String buildName;
        private final List<FroggerGameFileEntry> files = new ArrayList<>();

        public FroggerGameBuild(String buildName) {
            this.buildName = buildName;
        }

        /**
         * Reads a game build from a config entry.
         * @param config The config entry to read from.
         * @return parsedGameBuild
         */
        public static FroggerGameBuild readGameBuild(Config config) {
            FroggerGameBuild build = new FroggerGameBuild(config.getName());
            for (String textLine : config.getText())
                build.getFiles().add(FroggerGameFileEntry.readEntryFromLine(build, textLine));
            return build;
        }
    }

    @Getter
    @AllArgsConstructor
    private static class FroggerGameFileEntry {
        private final FroggerGameBuild build;
        private final String fullPath;
        private final int fileSize;
        private final String sha1Hash;

        /**
         * Gets the file name on its own.
         */
        public String getFileName() {
            String fileName = this.fullPath;

            int tempIndex;
            if ((tempIndex = fileName.lastIndexOf('/')) != -1)
                fileName = fileName.substring(tempIndex + 1);
            if ((tempIndex = fileName.lastIndexOf('\\')) != -1)
                fileName = fileName.substring(tempIndex + 1);

            return fileName;
        }

        /**
         * Writes the version information to the builder.
         * @param builder The builder to write to.
         */
        public void write(StringBuilder builder) {
            builder.append(this.fullPath)
                    .append(",")
                    .append(this.fileSize)
                    .append(",")
                    .append(this.sha1Hash);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            this.write(builder);
            return builder.toString();
        }

        /**
         * Reads a file entry from a text line.
         * @param build The build to create the entry from.
         * @param line  The line to read the entry from.
         */
        public static FroggerGameFileEntry readEntryFromLine(FroggerGameBuild build, String line) {
            String[] split = line.split(",");
            String fullPath = split[0];
            int fileSize = Integer.parseInt(split[1]);
            String sha1Hash = split[2];
            return new FroggerGameFileEntry(build, fullPath, fileSize, sha1Hash);
        }
    }

    /**
     * Sets up the version configuration.
     * @param mainFolder The folder to load the configuration file from.
     */
    public static void setup(File mainFolder) {
        if (isEnabled())
            return;

        versionConfigFile = new File(mainFolder, "versions.cfg");
        if (!versionConfigFile.exists()) {
            versionConfigFile = null; // Keep disabled.
            return;
        }

        loadFromConfig();
    }

    private static void loadFromConfig() {
        Config loadConfig = new Config(FileUtils.readLinesFromFile(versionConfigFile));

        gameBuilds.clear();
        gameBuildsByName.clear();
        for (Config childConfig : loadConfig.getOrderedChildren()) {
            FroggerGameBuild newBuild = FroggerGameBuild.readGameBuild(childConfig);
            gameBuilds.add(newBuild);
            gameBuildsByName.put(newBuild.getBuildName(), newBuild);
        }
    }

    private static void saveToConfig() {
        if (!isEnabled())
            return;

        StringBuilder builder = new StringBuilder();
        List<String> results = new ArrayList<>();

        // Write game builds.
        for (FroggerGameBuild build : gameBuilds) {
            results.add("[" + build.getBuildName() + "]");
            for (FroggerGameFileEntry fileEntry : build.getFiles()) {
                fileEntry.write(builder);
                results.add(builder.toString());
                builder.setLength(0);
            }
            results.add("");
        }

        try {
            Files.write(versionConfigFile.toPath(), results);
        } catch (Throwable th) {
            th.printStackTrace();
            System.out.println("Failed to save '" + versionConfigFile.getName() + "'.");
        }
    }

    /**
     * Tests whether the version comparison tool is enabled.
     */
    public static boolean isEnabled() {
        return versionConfigFile != null;
    }
}