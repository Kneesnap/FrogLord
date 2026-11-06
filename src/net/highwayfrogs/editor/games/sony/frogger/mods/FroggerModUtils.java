package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.NonNull;
import net.highwayfrogs.editor.games.shared.mods.*;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.system.Config.ConfigValueNode;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Static entry points for exporting and installing Frogger mod packages, shared by Noodle scripts, the dialogs and a command line.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModUtils {
    private static final String MODDED_PREFIX = "modded_";
    private static final String UNKNOWN_CONTENT_HASH = "0";
    private static final String DEFAULT_MOD_NAME = "Untitled mod";
    private static final String DEFAULT_MOD_VERSION = "1.0.0";
    private static final String DEFAULT_MOD_AUTHOR = "Unknown";
    private static final String[] CONTENT_SECTIONS = {FroggerBaseFingerprint.SECTION_FILES, FroggerBaseFingerprint.SECTION_WAD_CHILDREN,
            FroggerBaseFingerprint.SECTION_IMAGES, FroggerBaseFingerprint.SECTION_LEVEL_SELECT, FroggerBaseFingerprint.SECTION_RACE_SELECT,
            FroggerBaseFingerprint.SECTION_DEMOS, FroggerBaseFingerprint.SECTION_THEMES, FroggerBaseFingerprint.SECTION_REMAPS};

    /**
     * Exports the changes of a game as a mod package and, when an output file is given, writes it with export.log beside it.
     * @param game the modded game to export
     * @param baseExeOrNull an unmodified executable of the same build, with its MWD beside it, or null to compare against the
     *                      fingerprint FrogLord ships for the build (all content is exported when there is none)
     * @param output the .mod file to write, or null to only build the package
     * @param manifestOverrides the author's [Mod] fields, either as a manifest root holding a [Mod] section or as the section itself;
     *                          null for defaults
     * @return exportResult
     */
    public static ModExportResult exportModPackage(@NonNull FroggerGameInstance game, File baseExeOrNull, File output, Config manifestOverrides) {
        ILogger logger = ClassNameLogger.getLogger(game, FroggerModUtils.class);
        ModManifest draft = createDraftManifest(manifestOverrides);
        FroggerModExporter exporter = new FroggerModExporter(game, logger);
        FroggerModExportOptions options = new FroggerModExportOptions();

        ModExportResult result;
        if (baseExeOrNull != null) {
            FroggerGameInstance baseInstance = loadBaseCopy(game, baseExeOrNull);
            result = exporter.export(baseInstance, draft, options);
        } else {
            FroggerBaseFingerprint fingerprint = FroggerBaseFingerprint.loadBundled(game.getVersionConfig().getInternalName());
            if (fingerprint == null) {
                logger.warning("FrogLord has no base fingerprint for %s, so all content is exported.", game.getVersionConfig().getInternalName());
                fingerprint = createAllContentBase(game, logger);
            }

            result = exporter.export(fingerprint, draft, options);
        }

        if (output != null)
            writeModPackage(result, output);
        return result;
    }

    /**
     * Writes an exported package as a zip, and its export log as export.log in the same folder.
     * @param result the export result to write
     * @param output the .mod file to write
     * @return the export log file written
     */
    public static File writeModPackage(@NonNull ModExportResult result, @NonNull File output) {
        try {
            new ModPackageWriter().write(result.getModPackage(), output);
            File logFile = getExportLogFile(output);
            Files.write(logFile.toPath(), result.getExportLog().getBytes(StandardCharsets.UTF_8));
            return logFile;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write the mod package '" + output + "'.", ex);
        }
    }

    /**
     * Validates the manifest of an exported package as an installer will read it.
     * @param result the export result to check
     * @return the validation problems, empty when the manifest is valid
     */
    public static List<ModProblem> validateExport(@NonNull ModExportResult result) {
        ModPackage modPackage = result.getModPackage();
        modPackage.updateFiles();
        return FroggerModManifest.createValidator().validate(modPackage.getManifest(), new HashSet<>(modPackage.getEntries().keySet()));
    }

    /**
     * Gets the file the export log of a package is written to: export.log in the package's folder.
     * @param output the .mod file
     * @return exportLogFile
     */
    public static File getExportLogFile(@NonNull File output) {
        return new File(output.getAbsoluteFile().getParentFile(), FroggerModExporter.EXPORT_LOG_NAME);
    }

    /**
     * Installs a mod package onto a game, after checking the game against the fingerprint FrogLord ships for its build.
     * Nothing is saved; the caller saves the game afterwards.
     * @param game the game to install onto
     * @param packageFile the .mod file to install
     * @param allowModifiedBase whether to install onto a game which already differs from its base build
     * @return installResult
     */
    public static ModInstallResult installModPackage(@NonNull FroggerGameInstance game, @NonNull File packageFile, boolean allowModifiedBase) {
        List<ModProblem> problems = new ArrayList<>();
        ModPackage modPackage;
        try {
            modPackage = new ModPackageReader(FroggerModManifest.createValidator()).read(packageFile, problems);
        } catch (IOException ex) {
            problems.add(ModProblem.error(null, null, "Could not read '" + packageFile.getName() + "': " + ex.getMessage()));
            modPackage = null;
        }

        if (modPackage == null || modPackage.hasErrors()) {
            ModInstallResult result = new ModInstallResult(modPackage != null ? modPackage.getManifest().getName() : packageFile.getName());
            for (ModProblem problem : problems)
                result.addProblem(problem);
            if (!result.isSuccess() || modPackage == null)
                result.addProblem(ModProblem.error(null, null, "The package cannot be installed."));
            return result;
        }

        FroggerBaseDifference difference = compareWithBaseFingerprint(game);
        List<String> baseDifferences = difference != null ? (difference.isEmpty() ? new ArrayList<String>() : difference.getSummaryLines()) : null;
        return new FroggerModInstaller(game, modPackage, null).install(allowModifiedBase, baseDifferences);
    }

    /**
     * Generates the base fingerprint of a game and saves it.
     * @param game the game to fingerprint, normally a freshly loaded retail copy
     * @param output the .cfg file to write, or null to only generate it
     * @return fingerprint
     */
    public static FroggerBaseFingerprint generateBaseFingerprint(@NonNull FroggerGameInstance game, File output) {
        FroggerBaseFingerprint fingerprint = FroggerBaseFingerprint.generate(game, null);
        if (output != null)
            fingerprint.save(output);
        return fingerprint;
    }

    /**
     * Compares a game against the fingerprint FrogLord ships for its build.
     * @param game the game to compare
     * @return difference, or null when FrogLord has no fingerprint for the build
     */
    public static FroggerBaseDifference compareWithBaseFingerprint(@NonNull FroggerGameInstance game) {
        FroggerBaseFingerprint fingerprint = FroggerBaseFingerprint.loadBundled(game.getVersionConfig().getInternalName());
        return fingerprint != null ? fingerprint.compare(game, null) : null;
    }

    /**
     * Creates a base for exporting a game when no unmodified copy and no fingerprint exist: every map, model, image, row, demo,
     * theme and remap counts as changed, so all content is exported, while the executable compares equal to itself.
     * @param game the game to export
     * @param logger the logger to report to, or null for the class logger
     * @return fingerprint
     */
    public static FroggerBaseFingerprint createAllContentBase(@NonNull FroggerGameInstance game, ILogger logger) {
        Config config = FroggerBaseFingerprint.generate(game, logger).toConfig();
        for (String sectionName : CONTENT_SECTIONS) {
            Config section = config.getChildConfigByName(sectionName);
            if (section == null)
                continue;

            List<ConfigValueNode> lines = section.getInternalText();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).getAsString();
                int split = line != null ? line.trim().indexOf(' ') : -1;
                if (split > 0)
                    lines.set(i, new ConfigValueNode(UNKNOWN_CONTENT_HASH + line.trim().substring(split)));
            }
        }

        return FroggerBaseFingerprint.load(config);
    }

    /**
     * Finds the MWD of an unmodified copy: beside its executable, named as the open game's MWD, or as that name without the
     * "modded_" prefix FrogLord gives saved files.
     * @param game the open game
     * @param baseExe the executable of the unmodified copy
     * @return mwdFile, or null when none is found
     */
    public static File findBaseMwd(@NonNull SCGameInstance game, @NonNull File baseExe) {
        File folder = baseExe.getAbsoluteFile().getParentFile();
        File loadedMwd = game.getMwdFile();
        if (folder == null || loadedMwd == null)
            return null;

        Set<String> names = new LinkedHashSet<>();
        String loadedName = loadedMwd.getName();
        names.add(loadedName);
        if (loadedName.toLowerCase(Locale.ROOT).startsWith(MODDED_PREFIX))
            names.add(loadedName.substring(MODDED_PREFIX.length()));

        for (String name : names) {
            File candidate = new File(folder, name);
            if (candidate.isFile() && !candidate.getAbsoluteFile().equals(loadedMwd.getAbsoluteFile()))
                return candidate;
        }

        return null;
    }

    /**
     * Opens an unmodified copy of the open game's build as a second game instance, without a window.
     * @param game the open game
     * @param baseExe the executable of the unmodified copy; its MWD must sit beside it (see findBaseMwd)
     * @return baseInstance
     */
    public static FroggerGameInstance loadBaseCopy(@NonNull FroggerGameInstance game, @NonNull File baseExe) {
        if (!baseExe.isFile())
            throw new IllegalArgumentException("The executable '" + baseExe + "' does not exist.");
        if (game.getExeFile() != null && baseExe.getAbsoluteFile().equals(game.getExeFile().getAbsoluteFile()))
            throw new IllegalArgumentException("The original executable must be another file than the open game's.");

        File baseMwd = findBaseMwd(game, baseExe);
        if (baseMwd == null)
            throw new IllegalArgumentException("No MWD named '" + game.getMwdFile().getName() + "' (or without its '" + MODDED_PREFIX
                    + "' prefix) sits beside '" + baseExe.getName() + "'.");

        long checksum;
        try {
            checksum = DataUtils.getCRC32(baseExe);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read '" + baseExe + "'.", ex);
        }

        SCGameConfig baseConfig = SCGameType.FROGGER.getConfigByExecutableChecksum(checksum);
        if (baseConfig == null)
            throw new IllegalArgumentException("'" + baseExe.getName() + "' is not an executable FrogLord recognises as an unmodified Frogger build.");
        if (!baseConfig.getInternalName().equals(game.getVersionConfig().getInternalName()))
            throw new IllegalArgumentException("'" + baseExe.getName() + "' is " + baseConfig.getInternalName() + ", but the open game is "
                    + game.getVersionConfig().getInternalName() + ".");

        SCGameInstance instance = SCGameType.FROGGER.createGameInstance();
        instance.loadGame(baseConfig, new Config("modExportBase"), baseMwd, baseExe, null);
        return (FroggerGameInstance) instance;
    }

    /**
     * Creates the draft manifest of an export from the author's [Mod] fields, filling a random id, a name, a version and an author
     * when missing, so that the exported manifest validates.
     * @param manifestOverrides a manifest root holding a [Mod] section, the [Mod] section itself, or null
     * @return draft
     */
    public static ModManifest createDraftManifest(Config manifestOverrides) {
        ModManifest draft;
        if (manifestOverrides == null) {
            draft = new ModManifest();
        } else if (manifestOverrides.getChildConfigByName(ModFormatConstants.SECTION_MOD) != null) {
            draft = new ModManifest(ModManifest.copyConfig(manifestOverrides));
        } else {
            draft = new ModManifest();
            Config section = ModManifest.copyConfig(manifestOverrides);
            section.setSectionName(ModFormatConstants.SECTION_MOD);
            draft.getConfig().addChildConfig(section);
        }

        if (isBlank(draft.getId()))
            draft.setId(UUID.randomUUID().toString());
        if (isBlank(draft.getName()))
            draft.setName(DEFAULT_MOD_NAME);
        if (isBlank(draft.getVersion()))
            draft.setVersion(DEFAULT_MOD_VERSION);
        if (isBlank(draft.getAuthor()))
            draft.setAuthor(DEFAULT_MOD_AUTHOR);
        return draft;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
