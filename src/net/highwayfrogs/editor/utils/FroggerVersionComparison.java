package net.highwayfrogs.editor.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.config.Config;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;

import java.io.File;
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

    /**
     * Generates a report of the differences.
     */
    public static void generateReport() {
        Map<String, FroggerFileTracker> trackerMap = new HashMap<>();
        List<FroggerFileTracker> fileTrackers = new ArrayList<>();

        // TODO: Show build that file was removed from.

        for (FroggerGameBuild build : gameBuilds) {
            for (FroggerGameFileEntry entry : build.getFiles()) {
                if (entry.getFullPath().endsWith(".WAD"))
                    continue; // Skip .WAD files, since we only care about the files inside the WAD, not the WAD itself.

                FroggerFileTracker tracker = trackerMap.get(entry.getFullPath());
                if (tracker == null) {
                    trackerMap.put(entry.getFullPath(), tracker = new FroggerFileTracker(entry.getFullPath()));
                    fileTrackers.add(tracker);
                }

                tracker.getFiles().add(entry);
            }
        }

        StringBuilder builder = new StringBuilder();
        Set<String> seenHashes = new HashSet<>();
        Map<FroggerGameFileEntry, FroggerGameFileEntry> filesWorthLookingAt = new HashMap<>();
        for (FroggerFileTracker tracker : fileTrackers) {
            builder.append(tracker.getFileIdentifier()).append(":").append(Constants.NEWLINE);

            FroggerGameFileEntry lastFileEntry = null;
            for (FroggerGameFileEntry entry : tracker.getFiles()) {

                boolean alreadySeen = !seenHashes.add(entry.getSha1Hash());
                if (lastFileEntry != null) {
                    boolean didChange = (lastFileEntry.getFileSize() != entry.getFileSize()) || !lastFileEntry.getSha1Hash().equals(entry.getSha1Hash());
                    if (didChange) {
                        builder.append(" - CHANGE:  ").append(entry.getBuild().getBuildName()).append(", Size: ").append(entry.getFileSize()).append(", SHA1: ").append(entry.getSha1Hash());
                        if (alreadySeen)
                            builder.append(" **ALREADY SEEN**");
                        builder.append(Constants.NEWLINE);
                        filesWorthLookingAt.put(entry, lastFileEntry);
                    }
                } else {
                    builder.append(" - INITIAL: ").append(entry.getBuild().getBuildName()).append(", Size: ").append(entry.getFileSize()).append(", SHA1: ").append(entry.getSha1Hash()).append(Constants.NEWLINE);
                    filesWorthLookingAt.put(entry, entry);
                }

                lastFileEntry = entry;
            }

            builder.append(Constants.NEWLINE);
        }

        builder.append(Constants.NEWLINE);
        builder.append(Constants.NEWLINE);
        builder.append("CHANGES PER BUILD:");
        builder.append(Constants.NEWLINE);
        builder.append(Constants.NEWLINE);

        for (FroggerGameBuild build : gameBuilds) {
            builder.append(build.getBuildName()).append(":").append(Constants.NEWLINE);

            for (FroggerGameFileEntry entry : build.getFiles()) {
                FroggerGameFileEntry changedFrom = filesWorthLookingAt.get(entry);
                if (changedFrom == null)
                    continue;

                builder.append(" - ").append(entry.getFileName());

                if (changedFrom == entry) {
                    builder.append(" was seen for the first time.").append(Constants.NEWLINE);
                } else {
                    builder.append(" changed from ").append(changedFrom.getBuild().getBuildName()).append(".").append(Constants.NEWLINE);
                }
            }

            builder.append(Constants.NEWLINE);
        }

        System.out.println(builder);
    }

    /**
     * Adds a new version of the game to the version config.
     * @param versionToAdd The version of the game to add.
     */
    public static void addNewVersionToConfig(FroggerEXEInfo versionToAdd) {
        if (!isEnabled())
            return;

        if (gameBuildsByName.containsKey(versionToAdd.getInternalName())) {
            System.out.println("This build is already tracked in the version cache.");
            return;
        }

        FroggerGameBuild newBuild = new FroggerGameBuild(versionToAdd.getInternalName());
        for (FileEntry entry : versionToAdd.getMWI().getEntries())
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
        Config loadConfig = new Config(Utils.readLinesFromFile(versionConfigFile));

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
