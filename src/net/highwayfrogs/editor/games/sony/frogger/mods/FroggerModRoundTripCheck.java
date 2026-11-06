package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.games.shared.mods.ModFormatConstants;
import net.highwayfrogs.editor.games.shared.mods.ModInstallResult;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModPackage;
import net.highwayfrogs.editor.games.shared.mods.ModPackageEntry;
import net.highwayfrogs.editor.games.shared.mods.ModPackageReader;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Checks that a Frogger mod package survives a round trip: install it onto an unmodified copy of its target, export the result again, and compare the two packages.
 * The manifests are compared section by section for the target, ignoring [Mod] key order, exportedBy and [Files]; content files are compared by SHA-1. Nothing is saved.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModRoundTripCheck {
    private final ILogger logger;

    public static final String SECTION_CONTENT = "Content";
    private static final Set<String> IGNORED_SECTIONS = new HashSet<>(Arrays.asList(ModFormatConstants.SECTION_FILES, ModFormatConstants.SECTION_OVERRIDES));
    private static final Set<String> IGNORED_MOD_KEYS = new HashSet<>(Collections.singletonList(ModFormatConstants.KEY_EXPORTED_BY));
    private static final String TEXT_KEY = "(text)";
    private static final int MAX_VALUE_LENGTH = 96;

    public FroggerModRoundTripCheck(ILogger logger) {
        this.logger = logger;
    }

    /**
     * Reads a package file, opens an unmodified copy of its target from an executable and MWD as a second game instance, and runs the round trip.
     * @param packageFile the .mod file
     * @param exeFile the unmodified executable
     * @param mwdFile the MWD beside it
     * @return result
     */
    public FroggerModRoundTripResult check(@NonNull File packageFile, @NonNull File exeFile, @NonNull File mwdFile) {
        FroggerModRoundTripResult result = new FroggerModRoundTripResult(packageFile.getName());
        ModPackage modPackage = readPackage(packageFile, result);
        if (modPackage == null)
            return result;

        return check(modPackage, exeFile, mwdFile, result);
    }

    /**
     * Opens an unmodified copy of the package's target from an executable and MWD as a second game instance, and runs the round trip.
     * @param modPackage the package to check
     * @param exeFile the unmodified executable
     * @param mwdFile the MWD beside it
     * @return result
     */
    public FroggerModRoundTripResult check(@NonNull ModPackage modPackage, @NonNull File exeFile, @NonNull File mwdFile) {
        return check(modPackage, exeFile, mwdFile, new FroggerModRoundTripResult(modPackage.getManifest().getName()));
    }

    private FroggerModRoundTripResult check(ModPackage modPackage, File exeFile, File mwdFile, FroggerModRoundTripResult result) {
        FroggerGameInstance retailCopy;
        try {
            retailCopy = loadGameCopy(exeFile, mwdFile);
        } catch (Throwable th) {
            result.fail("Could not open the unmodified copy '" + exeFile.getName() + "': " + describeThrowable(th));
            return result;
        }

        result.addStep("Opened " + exeFile.getName() + " and " + mwdFile.getName() + " as " + retailCopy.getVersionConfig().getInternalName() + ".");
        return check(modPackage, retailCopy, result);
    }

    /**
     * Runs the round trip on a game instance which holds an unmodified copy of the package's target. The instance is changed by the install and should be discarded afterwards.
     * @param modPackage the package to check
     * @param retailCopy the loaded unmodified copy, not the game the user is editing
     * @return result
     */
    public FroggerModRoundTripResult check(@NonNull ModPackage modPackage, @NonNull FroggerGameInstance retailCopy) {
        return check(modPackage, retailCopy, new FroggerModRoundTripResult(modPackage.getManifest().getName()));
    }

    private FroggerModRoundTripResult check(ModPackage modPackage, FroggerGameInstance retailCopy, FroggerModRoundTripResult result) {
        ILogger log = this.logger != null ? this.logger : ClassNameLogger.getLogger(retailCopy, FroggerModRoundTripCheck.class);
        String buildName = retailCopy.getVersionConfig().getInternalName();
        result.setTargetName(buildName);

        FroggerBaseFingerprint fingerprint = FroggerBaseFingerprint.loadBundled(buildName);
        if (fingerprint == null) {
            result.fail("FrogLord has no bundled base fingerprint for '" + buildName + "', so the copy cannot be checked and the result cannot be exported.");
            return result;
        }

        FroggerBaseDifference baseDifference = fingerprint.compare(retailCopy, log);
        if (!baseDifference.isEmpty()) {
            result.getBaseDifferences().addAll(baseDifference.getSummaryLines());
            result.fail("The copy is not the unmodified " + buildName + " build, so the round trip was refused.");
            return result;
        }

        result.addStep("The copy matches the bundled base fingerprint of " + buildName + ".");

        ModInstallResult installResult;
        try {
            installResult = new FroggerModInstaller(retailCopy, modPackage, log).install(false, new ArrayList<String>());
        } catch (Throwable th) {
            result.fail("The install stopped unexpectedly: " + describeThrowable(th));
            return result;
        }

        result.setInstallResult(installResult);
        if (!installResult.isSuccess()) {
            result.fail("The package could not be installed onto the copy.");
            return result;
        }

        result.addStep("Installed the package: " + installResult.getAppliedOperations().size() + " operation(s), " + installResult.getWarnings().size() + " warning(s).");

        ModExportResult exportResult;
        try {
            exportResult = new FroggerModExporter(retailCopy, log).export(fingerprint, createDraft(modPackage), createExportOptions(modPackage, buildName));
        } catch (Throwable th) {
            result.fail("The export of the installed copy stopped unexpectedly: " + describeThrowable(th));
            return result;
        }

        result.setExportResult(exportResult);
        if (exportResult.hasErrors()) {
            result.fail("The installed copy could not be exported.");
            return result;
        }

        result.addStep("Exported the installed copy against the base fingerprint: " + exportResult.getModPackage().getEntries().size() + " content file(s), "
                + exportResult.getProblems().size() + " problem(s).");
        result.getSections().addAll(compare(modPackage, exportResult.getModPackage(), buildName));
        log.info("Round trip of '%s' on %s: %s.", result.getPackageName(), buildName, result.isPassed() ? "the packages match" : "the packages differ");
        return result;
    }

    /**
     * Opens a Frogger executable and MWD as a new game instance without showing it. The build is detected from the executable checksum.
     * @param exeFile the executable
     * @param mwdFile the MWD
     * @return gameInstance
     * @throws IOException if the executable cannot be read
     */
    public static FroggerGameInstance loadGameCopy(@NonNull File exeFile, @NonNull File mwdFile) throws IOException {
        if (!exeFile.isFile())
            throw new IllegalArgumentException("The executable '" + exeFile + "' does not exist.");
        if (!mwdFile.isFile())
            throw new IllegalArgumentException("The MWD '" + mwdFile + "' does not exist.");

        long checksum = DataUtils.getCRC32(exeFile);
        SCGameConfig versionConfig = SCGameType.FROGGER.getConfigByExecutableChecksum(checksum);
        if (versionConfig == null)
            throw new IllegalArgumentException(String.format("The executable checksum %08X is not a known Frogger build.", checksum));

        SCGameInstance instance = SCGameType.FROGGER.createGameInstance();
        instance.loadGame(versionConfig, new Config("roundTrip"), mwdFile.getAbsoluteFile(), exeFile.getAbsoluteFile(), null);
        return (FroggerGameInstance) instance;
    }

    /**
     * Compares two packages for one target: each manifest section of the effective configuration, then the content files by SHA-1.
     * [Files] is not compared; in [Mod], key order and exportedBy are ignored.
     * @param expected the package that was installed
     * @param actual the package exported after the install
     * @param target the target name or alias
     * @return sectionComparisons, in the order of the expected manifest, with content last
     */
    public static List<FroggerModSectionComparison> compare(@NonNull ModPackage expected, @NonNull ModPackage actual, String target) {
        Config expectedRoot = expected.getManifest().getEffectiveConfig(target);
        Config actualRoot = actual.getManifest().getEffectiveConfig(target);

        Set<String> sectionNames = new LinkedHashSet<>();
        for (Config section : expectedRoot.getChildConfigNodes())
            sectionNames.add(section.getSectionName());
        for (Config section : actualRoot.getChildConfigNodes())
            sectionNames.add(section.getSectionName());
        sectionNames.removeAll(IGNORED_SECTIONS);

        List<FroggerModSectionComparison> comparisons = new ArrayList<>();
        for (String sectionName : sectionNames) {
            Map<String, String> expectedValues = flattenSections(expectedRoot, sectionName);
            Map<String, String> actualValues = flattenSections(actualRoot, sectionName);
            if (ModFormatConstants.SECTION_MOD.equals(sectionName)) {
                expectedValues.keySet().removeAll(IGNORED_MOD_KEYS);
                actualValues.keySet().removeAll(IGNORED_MOD_KEYS);
            }

            comparisons.add(compareValues(sectionName, expectedValues, actualValues, expectedRoot.getAllChildConfigsByName(sectionName).isEmpty(),
                    actualRoot.getAllChildConfigsByName(sectionName).isEmpty()));
        }

        Map<String, String> expectedHashes = new TreeMap<>();
        for (ModPackageEntry entry : expected.getEntryList())
            expectedHashes.put(entry.getPath(), entry.getSha1());
        Map<String, String> actualHashes = new TreeMap<>();
        for (ModPackageEntry entry : actual.getEntryList())
            actualHashes.put(entry.getPath(), entry.getSha1());
        comparisons.add(compareValues(SECTION_CONTENT, expectedHashes, actualHashes, false, false));
        return comparisons;
    }

    private static FroggerModSectionComparison compareValues(String sectionName, Map<String, String> expectedValues, Map<String, String> actualValues,
                                                             boolean missingInExpected, boolean missingInActual) {
        FroggerModSectionComparison comparison = new FroggerModSectionComparison(sectionName);
        if (missingInExpected)
            comparison.getDifferences().add("the section is only in the exported package");
        if (missingInActual)
            comparison.getDifferences().add("the section is only in the installed package");

        Set<String> keys = new TreeSet<>(expectedValues.keySet());
        keys.addAll(actualValues.keySet());
        for (String key : keys) {
            String expectedValue = expectedValues.get(key);
            String actualValue = actualValues.get(key);
            if (Objects.equals(expectedValue, actualValue))
                continue;

            if (actualValue == null) {
                comparison.getDifferences().add(key + ": only in the installed package (" + shorten(expectedValue) + ")");
            } else if (expectedValue == null) {
                comparison.getDifferences().add(key + ": only in the exported package (" + shorten(actualValue) + ")");
            } else {
                comparison.getDifferences().add(key + ": installed '" + shorten(expectedValue) + "', exported '" + shorten(actualValue) + "'");
            }
        }

        comparison.setComparedValueCount(keys.size());
        return comparison;
    }

    private static Map<String, String> flattenSections(Config root, String sectionName) {
        Map<String, String> values = new TreeMap<>();
        List<Config> sections = root.getAllChildConfigsByName(sectionName);
        for (int i = 0; i < sections.size(); i++)
            flatten(sections.get(i), i > 0 ? "#" + i + "/" : "", values);

        return values;
    }

    private static void flatten(Config section, String prefix, Map<String, String> values) {
        for (String key : ModManifest.getKeys(section))
            values.put(prefix + key, ModManifest.getString(section, key));

        List<String> textLines = new ArrayList<>();
        for (String line : ModManifest.getTextLines(section))
            if (line != null && !line.trim().isEmpty())
                textLines.add(line.trim());
        if (!textLines.isEmpty())
            values.put(prefix + TEXT_KEY, String.join("\n", textLines));

        Map<String, Integer> childCounts = new HashMap<>();
        for (Config child : section.getChildConfigNodes()) {
            String childName = child.getSectionName();
            int index = childCounts.containsKey(childName) ? childCounts.get(childName) : 0;
            childCounts.put(childName, index + 1);
            flatten(child, prefix + childName + (index > 0 ? "#" + index : "") + "/", values);
        }
    }

    private static ModManifest createDraft(ModPackage modPackage) {
        ModManifest draft = new ModManifest();
        Config modSection = modPackage.getManifest().getSection(ModFormatConstants.SECTION_MOD);
        if (modSection != null)
            draft.getConfig().addChildConfig(ModManifest.copyConfig(modSection));

        return draft;
    }

    private static FroggerModExportOptions createExportOptions(ModPackage modPackage, String target) {
        FroggerModExportOptions options = new FroggerModExportOptions();
        FroggerModManifest manifest = new FroggerModManifest(modPackage.getManifest(), target);
        for (FroggerModLevelEntry entry : manifest.getLevelEntries())
            if (entry.getName() != null)
                options.setLevelName(entry.getLevelCode(), entry.getName());

        return options;
    }

    private static ModPackage readPackage(File packageFile, FroggerModRoundTripResult result) {
        List<ModProblem> problems = new ArrayList<>();
        ModPackage modPackage;
        try {
            modPackage = new ModPackageReader(FroggerModManifest.createValidator()).read(packageFile, problems);
        } catch (IOException ex) {
            result.fail("Could not read '" + packageFile.getName() + "': " + describeThrowable(ex));
            return null;
        }

        if (modPackage == null || ModProblem.hasErrors(problems)) {
            for (ModProblem problem : problems)
                result.addStep("Package problem: " + problem);
            result.fail("The package '" + packageFile.getName() + "' is not valid.");
            return null;
        }

        if (modPackage.getManifest().getName() != null)
            result.setPackageName(modPackage.getManifest().getName());
        result.addStep("Read " + packageFile.getName() + ": " + modPackage.getEntries().size() + " content file(s), " + problems.size() + " problem(s).");
        return modPackage;
    }

    private static String shorten(String value) {
        if (value == null)
            return "";

        String flat = value.replace("\n", "\\n");
        return flat.length() > MAX_VALUE_LENGTH ? flat.substring(0, MAX_VALUE_LENGTH) + "..." : flat;
    }

    private static String describeThrowable(Throwable th) {
        String message = th.getMessage();
        return th.getClass().getSimpleName() + (message != null ? ": " + message : "");
    }

    /**
     * The comparison of one manifest section, or of the content files, between the installed and the exported package.
     * Created by jimbolina on 11/6/2026.
     */
    @Getter
    public static class FroggerModSectionComparison {
        @NonNull private final String sectionName;
        private final List<String> differences = new ArrayList<>();
        private int comparedValueCount;

        public FroggerModSectionComparison(@NonNull String sectionName) {
            this.sectionName = sectionName;
        }

        private void setComparedValueCount(int comparedValueCount) {
            this.comparedValueCount = comparedValueCount;
        }

        /**
         * Returns true if the section holds the same values in both packages.
         */
        public boolean isEqual() {
            return this.differences.isEmpty();
        }

        @Override
        public String toString() {
            return "[" + this.sectionName + "] " + (isEqual() ? "equal (" + this.comparedValueCount + " value(s))" : this.differences.size() + " difference(s)");
        }
    }

    /**
     * The outcome of a round-trip check: the steps taken, the install and export results, the section comparisons and the verdict.
     * Created by jimbolina on 11/6/2026.
     */
    @Getter
    public static class FroggerModRoundTripResult {
        private String packageName;
        private String targetName;
        private String failure;
        private ModInstallResult installResult;
        private ModExportResult exportResult;
        private final List<String> steps = new ArrayList<>();
        private final List<String> baseDifferences = new ArrayList<>();
        private final List<FroggerModSectionComparison> sections = new ArrayList<>();

        public FroggerModRoundTripResult(String packageName) {
            this.packageName = packageName;
        }

        private void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        private void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        private void setInstallResult(ModInstallResult installResult) {
            this.installResult = installResult;
        }

        private void setExportResult(ModExportResult exportResult) {
            this.exportResult = exportResult;
        }

        private void addStep(String step) {
            this.steps.add(step);
        }

        private void fail(String failure) {
            if (this.failure == null)
                this.failure = failure;
        }

        /**
         * Returns true if the round trip ran to the end and every section and content file matched.
         */
        public boolean isPassed() {
            if (this.failure != null || this.sections.isEmpty())
                return false;

            for (FroggerModSectionComparison section : this.sections)
                if (!section.isEqual())
                    return false;

            return true;
        }

        /**
         * Gets the verdict and one line per compared section.
         * @return summaryLines
         */
        public List<String> getSummaryLines() {
            List<String> lines = new ArrayList<>();
            lines.add(getVerdict());
            for (FroggerModSectionComparison section : this.sections)
                lines.add(section.toString());

            return lines;
        }

        /**
         * Gets the one-line verdict.
         * @return verdict
         */
        public String getVerdict() {
            String name = "'" + (this.packageName != null ? this.packageName : "mod package") + "'" + (this.targetName != null ? " on " + this.targetName : "");
            if (this.failure != null)
                return "Round trip of " + name + " did not complete: " + this.failure;

            int differing = 0;
            for (FroggerModSectionComparison section : this.sections)
                if (!section.isEqual())
                    differing++;

            return differing == 0 ? "Round trip of " + name + " passed: every section and content file matches."
                    : "Round trip of " + name + " failed: " + differing + " of " + this.sections.size() + " section(s) differ.";
        }

        /**
         * Describes the result as text: the verdict, the steps, every section with its differing keys, and the install and export problems.
         * @return description
         */
        public String describe() {
            String newLine = System.lineSeparator();
            StringBuilder builder = new StringBuilder(getVerdict()).append(newLine);
            appendList(builder, "Steps", this.steps);
            appendList(builder, "Differences from the base build", this.baseDifferences);

            if (!this.sections.isEmpty()) {
                builder.append("Sections:").append(newLine);
                for (FroggerModSectionComparison section : this.sections) {
                    builder.append(" - ").append(section).append(newLine);
                    for (String difference : section.getDifferences())
                        builder.append("     ").append(difference).append(newLine);
                }
            }

            if (this.installResult != null) {
                appendProblems(builder, "Install warnings", this.installResult.getWarnings());
                appendProblems(builder, "Install errors", this.installResult.getErrors());
            }

            if (this.exportResult != null)
                appendProblems(builder, "Export problems", this.exportResult.getProblems());

            builder.append("Nothing was saved; the copy holds the installed package and should be discarded.").append(newLine);
            return builder.toString();
        }

        private static void appendList(StringBuilder builder, String title, List<String> lines) {
            if (lines.isEmpty())
                return;

            builder.append(title).append(':').append(System.lineSeparator());
            for (String line : lines)
                builder.append(" - ").append(line).append(System.lineSeparator());
        }

        private static void appendProblems(StringBuilder builder, String title, List<ModProblem> problems) {
            if (problems.isEmpty())
                return;

            builder.append(title).append(':').append(System.lineSeparator());
            for (ModProblem problem : problems)
                builder.append(" - ").append(problem).append(System.lineSeparator());
        }
    }
}
