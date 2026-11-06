package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.patch.GamePatch;
import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.file.patch.PatchWriteRecorder.PatchWriteRange;
import net.highwayfrogs.editor.games.generic.GameConfig;
import net.highwayfrogs.editor.games.shared.mods.ModFormatConstants;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModManifest.ModTarget;
import net.highwayfrogs.editor.games.shared.mods.ModPackage;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectWorldID;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerDemoFile;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerDemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerPCDemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.theme.FroggerThemeBook;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference.FroggerBaseSectionDifference;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference.FroggerExeBlockChange;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseFingerprint.FroggerOwnedRange;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerModGameplay.FroggerModGameplaySettings;
import net.highwayfrogs.editor.games.sony.shared.LinkedTextureRemap;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Builds a mod package holding what differs between an open Frogger game and the base build it was made from.
 * The open game is never changed. Changes the format cannot carry are reported as problems and listed in the export log.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModExporter {
    private final FroggerGameInstance gameInstance;
    private final ILogger logger;

    public static final String EXPORT_LOG_NAME = "export.log";
    public static final String RAW_WRITES_PATCH_ID = "raw-writes";
    public static final String RAW_WRITES_PATCH_PATH = ModFormatConstants.FOLDER_PATCHES + RAW_WRITES_PATCH_ID + ".patch";
    public static final String SECTION_EXE_BLOCKS = "ExeBlocks";

    private static final String SECTION_FILES = "Files";
    private static final String SECTION_WAD_CHILDREN = "WadChildren";
    private static final String SECTION_IMAGES = "Images";
    private static final String SECTION_LEVEL_SELECT = "LevelSelect";
    private static final String SECTION_RACE_SELECT = "RaceSelect";
    private static final String SECTION_DEMOS = "Demos";
    private static final String SECTION_THEMES = "Themes";
    private static final String SECTION_REMAPS = "Remaps";
    private static final String SECTION_PATCH_SITES = "PatchSites";
    private static final String SECTION_EXE_OWNED = "ExeOwned";
    private static final String PNG_EXTENSION = ".png";
    private static final String REMAP_EXTENSION = ".remap";
    private static final String DEATH_HEIGHT_KEY = ";deathHeight=";
    private static final int SEARCH_INT_LIMIT = 32;
    private static final int THEME_SEARCH_LIMIT = 1 << 16;
    private static final Map<String, List<String>> TARGET_ALIASES = new HashMap<>();

    private static final int FIELD_LEVEL = 0;
    private static final int FIELD_WORLD = 1;
    private static final int FIELD_STACK_POSITION = 2;
    private static final int FIELD_THEME = 3;
    private static final int FIELD_LOCAL_LEVEL_ID = 4;
    private static final int FIELD_LEVELS_IN_WORLD = 5;
    private static final int FIELD_WORLD_IMAGE_SELECTABLE = 6;
    private static final int FIELD_WORLD_IMAGE_VISITED = 7;
    private static final int FIELD_WORLD_IMAGE_NOT_TRIED = 8;
    private static final int FIELD_LEVEL_TEXTURE = 9;
    private static final int FIELD_LEVEL_NAME_TEXTURE = 10;
    private static final int FIELD_LEVEL_NAME_TEXTURE_IN_GAME = 11;
    private static final String[] ROW_FIELD_LABELS = {"level", "world", "stackPosition", "theme", "localLevelId", "levelsInWorld",
            "worldImageSelectable", "worldImageVisited", "worldImageNotTried", "levelTexture", "levelNameTexture", "levelNameTextureInGame"};

    static {
        TARGET_ALIASES.put("pc-retail-v3.0e", Collections.singletonList("pc-retail-v3.0e-vogons"));
    }

    public FroggerModExporter(@NonNull FroggerGameInstance gameInstance, ILogger logger) {
        this.gameInstance = gameInstance;
        this.logger = logger != null ? logger : ClassNameLogger.getLogger(gameInstance, FroggerModExporter.class);
    }

    /**
     * Exports the changes of the open game against a base fingerprint, such as the one FrogLord ships for the build.
     * Raw executable writes cannot be packaged this way, because the fingerprint holds no bytes.
     * @param base the fingerprint of the unmodified build
     * @param draft the manifest holding the author's [Mod] section; it is not changed
     * @param options the author's choices, or null for the defaults
     * @return exportResult
     */
    public ModExportResult export(@NonNull FroggerBaseFingerprint base, @NonNull ModManifest draft, FroggerModExportOptions options) {
        return new ExportRun(base, null, draft, options).run();
    }

    /**
     * Exports the changes of the open game against an unmodified copy of its build, opened as a second game instance.
     * A fingerprint is generated from the copy and compared as for a shipped one; the copy's own data also gives the exact
     * base values, so raw executable writes can be packaged when the options ask for them.
     * @param baseInstance the unmodified copy of the build
     * @param draft the manifest holding the author's [Mod] section; it is not changed
     * @param options the author's choices, or null for the defaults
     * @return exportResult
     */
    public ModExportResult export(@NonNull FroggerGameInstance baseInstance, @NonNull ModManifest draft, FroggerModExportOptions options) {
        if (baseInstance == this.gameInstance)
            throw new IllegalArgumentException("The base copy must be a second game instance, not the open game.");

        FroggerBaseFingerprint base = FroggerBaseFingerprint.generate(baseInstance, this.logger);
        return new ExportRun(base, baseInstance, draft, options).run();
    }

    private class ExportRun {
        private final FroggerBaseFingerprint base;
        private final FroggerGameInstance baseInstance;
        private final ModManifest draft;
        private final FroggerModExportOptions options;
        private final FroggerModNameResolver resolver = new FroggerModNameResolver(gameInstance);
        private final FroggerModNameResolver baseResolver;
        private final FroggerModManifest frogger = new FroggerModManifest();
        private final Map<String, byte[]> contents = new LinkedHashMap<>();
        private final List<ModProblem> problems = new ArrayList<>();
        private final List<String> exported = new ArrayList<>();
        private final List<String> leftOut = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private FroggerBaseDifference difference;
        private Map<String, SCGameFile<?>> filesByKey;
        private final Set<String> handledFileKeys = new HashSet<>();

        private ExportRun(FroggerBaseFingerprint base, FroggerGameInstance baseInstance, ModManifest draft, FroggerModExportOptions options) {
            this.base = base;
            this.baseInstance = baseInstance;
            this.baseResolver = baseInstance != null ? new FroggerModNameResolver(baseInstance) : null;
            this.draft = draft;
            this.options = options != null ? options : new FroggerModExportOptions();
        }

        private ModExportResult run() {
            this.difference = this.base.compare(gameInstance, logger);
            if (this.difference.isBuildMismatch())
                problem(true, null, null, "The base is '" + this.difference.getBaseBuild() + "', but the open game is '"
                        + this.difference.getCurrentBuild() + "'; the package would not describe this game.");

            this.filesByKey = collectFiles();
            ModManifest manifest = createManifest();
            exportLevels();
            exportImages();
            exportLevelSelect(true);
            exportLevelSelect(false);
            exportDemos();
            exportThemes();
            exportPatchSites();
            exportExecutable();
            reportOwnedRegions();
            reportOtherFiles();

            this.frogger.applyTo(manifest);
            ModPackage modPackage = new ModPackage(manifest);
            for (Map.Entry<String, byte[]> entry : this.contents.entrySet())
                modPackage.addEntry(entry.getKey(), entry.getValue());

            String exportLog = buildLog(manifest);
            logger.info("Exported %d content files with %d problems against the base of %s.", this.contents.size(), this.problems.size(), this.base.getBuildName());
            return new ModExportResult(modPackage, this.difference, this.problems, exportLog);
        }

        private ModManifest createManifest() {
            ModManifest manifest = this.draft.copy();
            manifest.removeSection(ModFormatConstants.SECTION_TARGETS);
            manifest.removeSection(ModFormatConstants.SECTION_FILES);
            if (manifest.getFormatVersion() == null)
                manifest.setFormatVersion(ModFormatConstants.FORMAT_VERSION);
            manifest.setGame(FroggerModManifest.GAME_IDENTIFIER);
            manifest.setExportedBy("FrogLord " + Constants.VERSION);

            String buildName = this.base.getBuildName();
            List<Long> checksums = new ArrayList<>();
            SCGameConfig buildConfig = this.baseInstance != null ? this.baseInstance.getVersionConfig() : findVersionConfig(buildName);
            if (buildConfig != null) {
                for (long checksum : buildConfig.getExecutableChecksums())
                    checksums.add(checksum);
            } else {
                problem(false, ModFormatConstants.SECTION_TARGETS, buildName, "FrogLord has no version config named '" + buildName + "', so exeChecksum is left out.");
            }

            List<String> aliases = TARGET_ALIASES.get(buildName);
            manifest.setTargets(Collections.singletonList(new ModTarget(buildName, checksums, aliases != null ? aliases : Collections.<String>emptyList())));
            return manifest;
        }

        private void exportLevels() {
            Map<String, FroggerModLevelMaps> levels = new LinkedHashMap<>();
            Map<WADFile, FroggerModLevelMaps> levelsByWad = new IdentityHashMap<>();
            Map<TextureRemapArray, String> remapKeys = collectRemapKeys();
            Set<String> ownedRemapKeys = new HashSet<>();
            Set<String> changedFiles = new HashSet<>(this.difference.getFiles().getChanged());

            for (Map.Entry<String, SCGameFile<?>> entry : this.filesByKey.entrySet()) {
                if (!(entry.getValue() instanceof FroggerMapFile))
                    continue;

                FroggerMapFile map = (FroggerMapFile) entry.getValue();
                String fileName = FileUtils.stripExtension(displayNameOf(map));
                boolean lowPoly = fileName.endsWith(FroggerModNameResolver.LOW_POLY_MAP_SUFFIX);
                String code = lowPoly ? fileName.substring(0, fileName.length() - FroggerModNameResolver.LOW_POLY_MAP_SUFFIX.length()) : fileName;
                FroggerModLevelMaps level = levels.computeIfAbsent(code, FroggerModLevelMaps::new);
                String remapKey = map.getTextureRemap() != null ? remapKeys.get(map.getTextureRemap()) : null;
                boolean remapChanged = remapKey != null && this.difference.getRemaps().getChanged().contains(remapKey);
                if (remapKey != null)
                    ownedRemapKeys.add(remapKey);

                boolean mapChanged = changedFiles.contains(entry.getKey());
                if (mapChanged)
                    this.handledFileKeys.add(entry.getKey());
                if (mapChanged && isExcluded(SECTION_FILES, entry.getKey()))
                    mapChanged = false;
                if (remapChanged && isExcluded(SECTION_REMAPS, remapKey))
                    remapChanged = false;

                if (lowPoly) {
                    level.lowPolyMap = map;
                    level.lowPolyMapChanged = mapChanged;
                    if (remapChanged)
                        problem(false, FroggerModManifest.SECTION_MAPS, code, "The remap of " + displayNameOf(map)
                                + " changed, but format version 1 has no remap key for the low-poly twin; it is left out.");
                } else {
                    level.map = map;
                    level.mapChanged = mapChanged;
                    level.remapChanged = remapChanged;
                    level.levelWad = resolver.findLevelWad(map);
                    if (level.levelWad != null)
                        levelsByWad.put(level.levelWad, level);
                }
            }

            for (String key : this.difference.getRemaps().getAllKeys())
                if (!ownedRemapKeys.contains(key) && !isExcluded(SECTION_REMAPS, key))
                    problem(false, SECTION_REMAPS, key, "The texture remap '" + key + "' changed, but it belongs to no map, so format version 1 cannot carry it.");

            readWadChildren(levelsByWad);
            for (FroggerModLevelMaps level : levels.values())
                exportLevel(level);
        }

        private void readWadChildren(Map<WADFile, FroggerModLevelMaps> levelsByWad) {
            Map<String, Set<String>> baseHashesByName = collectBaseModelHashes();
            Map<String, List<Integer>> slotsByWad = new LinkedHashMap<>();
            FroggerBaseSectionDifference section = this.difference.getWadChildren();
            for (String key : section.getRemoved())
                problem(false, SECTION_WAD_CHILDREN, key, "The WAD slot " + key + " no longer exists; WAD slot counts cannot change.");

            List<String> keys = new ArrayList<>(section.getChanged());
            keys.addAll(section.getAdded());
            for (String key : keys) {
                if (isExcluded(SECTION_WAD_CHILDREN, key))
                    continue;

                int split = key.lastIndexOf(':');
                Integer index = split > 0 ? ModManifest.parseInteger(key.substring(split + 1)) : null;
                if (index == null) {
                    problem(false, SECTION_WAD_CHILDREN, key, "The WAD child " + key + " could not be located.");
                    continue;
                }

                slotsByWad.computeIfAbsent(key.substring(0, split), name -> new ArrayList<>()).add(index);
            }

            for (Map.Entry<String, List<Integer>> entry : slotsByWad.entrySet()) {
                WADFile wad = resolver.findWad(entry.getKey());
                FroggerModLevelMaps level = wad != null ? levelsByWad.get(wad) : null;
                if (level == null) {
                    problem(false, SECTION_WAD_CHILDREN, entry.getKey(), "Models changed in " + entry.getKey() + " (slots " + entry.getValue()
                            + "), which is not the level WAD of a map; format version 1 places models only through [Maps] models=.");
                    continue;
                }

                level.wadChanged = true;
                for (int index : entry.getValue()) {
                    if (index < 0 || index >= wad.getFiles().size())
                        continue;

                    WADEntry child = wad.getFiles().get(index);
                    if (child.isDummy() || !(child.getFile() instanceof MRModel))
                        continue;

                    String slotName = entry.getKey() + ":" + index;
                    String modelName = resolver.modelNameOf(child);
                    if (modelName == null) {
                        problem(true, FroggerModManifest.SECTION_MODELS, slotName, "Slot " + index + " of " + entry.getKey()
                                + " holds a model under the placeholder id whose bytes match no named model; format version 1 cannot add model names.");
                        continue;
                    }

                    byte[] modelBytes = child.getFile().writeDataToByteArray();
                    Set<String> baseHashes = baseHashesByName.get(modelName);
                    if (baseHashes != null && baseHashes.contains(FroggerModHasher.sha1(modelBytes)))
                        continue;

                    addModel(modelName, modelBytes, slotName);
                }
            }
        }

        private void addModel(String modelName, byte[] modelBytes, String slotName) {
            String path = ModFormatConstants.FOLDER_MODELS + modelName;
            byte[] existing = this.contents.get(path);
            if (existing != null) {
                if (!Arrays.equals(existing, modelBytes))
                    problem(true, FroggerModManifest.SECTION_MODELS, modelName, "Two changed copies of " + modelName + " differ (one is in "
                            + slotName + "); a package holds one model per name, so only the first is packaged.");
                return;
            }

            FroggerModModelEntry modelEntry = new FroggerModModelEntry(modelName);
            modelEntry.setFile(path);
            this.frogger.getModelEntries().add(modelEntry);
            addContent(path, modelBytes, "model " + modelName + " (from " + slotName + ")");
        }

        private Map<String, Set<String>> collectBaseModelHashes() {
            Map<String, Set<String>> results = new HashMap<>();
            for (WADFile wad : gameInstance.getMainArchive().getAllFiles(WADFile.class)) {
                String wadName = resolver.wadNameOf(wad);
                if (wadName == null)
                    continue;

                List<WADEntry> children = wad.getFiles();
                for (int i = 0; i < children.size(); i++) {
                    WADEntry child = children.get(i);
                    if (resolver.hasPlaceholderId(child))
                        continue;

                    String baseHash = this.base.getWadChildren().get(wadName + ":" + i);
                    if (baseHash != null)
                        results.computeIfAbsent(child.getFileEntry().getDisplayName(), name -> new HashSet<>()).add(baseHash);
                }
            }

            return results;
        }

        private void exportLevel(FroggerModLevelMaps level) {
            boolean anyChange = level.mapChanged || level.lowPolyMapChanged || level.remapChanged || level.wadChanged;
            if (!anyChange)
                return;

            FroggerModMapEntry mapEntry = new FroggerModMapEntry(level.code);
            if (level.mapChanged && level.map != null) {
                String path = ModFormatConstants.FOLDER_MAPS + level.code + FroggerModNameResolver.MAP_EXTENSION;
                mapEntry.setFile(path);
                addContent(path, serializeMap(level.map), "map " + displayNameOf(level.map));
            }

            if (level.lowPolyMapChanged && level.lowPolyMap != null) {
                String path = ModFormatConstants.FOLDER_MAPS + level.code + FroggerModNameResolver.LOW_POLY_MAP_SUFFIX + FroggerModNameResolver.MAP_EXTENSION;
                mapEntry.setLowPolyFile(path);
                addContent(path, serializeMap(level.lowPolyMap), "low-poly map " + displayNameOf(level.lowPolyMap));
            }

            if (level.remapChanged && level.map != null) {
                try {
                    List<String> names = resolver.remapNamesOf(level.map);
                    String path = ModFormatConstants.FOLDER_MAPS + level.code + REMAP_EXTENSION;
                    StringBuilder builder = new StringBuilder();
                    for (String name : names)
                        builder.append(name).append('\n');
                    mapEntry.setRemap(path);
                    addContent(path, builder.toString().getBytes(StandardCharsets.UTF_8), "remap of " + level.code + " (" + names.size() + " names)");
                } catch (IllegalArgumentException ex) {
                    problem(true, FroggerModManifest.SECTION_MAPS, level.code, ex.getMessage());
                }
            }

            if (level.levelWad != null) {
                mapEntry.setModels(resolver.listModelNames(level.levelWad));
            } else if (level.wadChanged || level.mapChanged) {
                problem(false, FroggerModManifest.SECTION_MAPS, level.code, "The map book of " + level.code + " names no level WAD, so models= is left out.");
            }

            if (mapEntry.getFile() == null && mapEntry.getLowPolyFile() == null && mapEntry.getRemap() == null && mapEntry.getModels() == null)
                return;

            this.frogger.getMapEntries().add(mapEntry);
        }

        private byte[] serializeMap(FroggerMapFile map) {
            List<FroggerModWadSlotSnapshot> snapshots = new ArrayList<>();
            for (WADFile wad : gameInstance.getMainArchive().getAllFiles(WADFile.class))
                for (WADEntry entry : wad.getFiles())
                    snapshots.add(new FroggerModWadSlotSnapshot(entry));

            try {
                return map.writeDataToByteArray();
            } finally {
                for (FroggerModWadSlotSnapshot snapshot : snapshots)
                    snapshot.restore();
            }
        }

        private void exportImages() {
            FroggerBaseSectionDifference section = this.difference.getImages();
            Map<String, FroggerModImageRef> images = collectImages();
            Set<String> renamedBaseKeys = new HashSet<>();
            for (String key : section.getAdded()) {
                FroggerModImageRef ref = images.get(key);
                String originalName = ref != null ? ref.image.getOriginalName() : null;
                String originalKey = ref != null && originalName != null ? ref.bankName + ":" + originalName : null;
                if (originalKey != null && section.getRemoved().contains(originalKey) && !isExcluded(SECTION_IMAGES, key)) {
                    renamedBaseKeys.add(originalKey);
                    this.notes.add("Image " + key + " carries a custom name; it is exported under its base name " + originalKey + ".");
                    exportImage(ref, originalName, key);
                } else if (!isExcluded(SECTION_IMAGES, key)) {
                    problem(false, SECTION_IMAGES, key, "The image " + key + " is new; format version 1 can only replace images the base bank holds.");
                }
            }

            for (String key : section.getRemoved())
                if (!renamedBaseKeys.contains(key) && !isExcluded(SECTION_IMAGES, key))
                    problem(false, SECTION_IMAGES, key, "The image " + key + " no longer exists; format version 1 cannot remove images.");

            for (String key : section.getChanged()) {
                if (isExcluded(SECTION_IMAGES, key))
                    continue;

                FroggerModImageRef ref = images.get(key);
                String name = ref != null ? resolver.imageNameOf(ref.image) : null;
                if (ref == null || name == null || !key.equals(ref.bankName + ":" + name)) {
                    problem(true, SECTION_IMAGES, key, "The changed image " + key + " has no unique name in its bank; name it with FrogLord's custom-name feature.");
                    continue;
                }

                exportImage(ref, name, key);
            }
        }

        private void exportImage(FroggerModImageRef ref, String name, String key) {
            VloImage image = ref.image;
            byte[] pngBytes;
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(image.toBufferedImage(VloImage.DEFAULT_IMAGE_EXPORT_SETTINGS), "png", output))
                    throw new IOException("No PNG writer is available.");
                pngBytes = output.toByteArray();
            } catch (IOException ex) {
                problem(true, SECTION_IMAGES, key, "The image " + key + " could not be written as PNG: " + ex.getMessage());
                return;
            }

            String path = ModFormatConstants.FOLDER_IMAGES + ref.bankName + "/" + name + PNG_EXTENSION;
            FroggerModImageEntry imageEntry = new FroggerModImageEntry(ref.bankName, name);
            imageEntry.setFile(path);
            imageEntry.setBlackIsTransparent(image.testFlag(VloImage.FLAG_BLACK_IS_TRANSPARENT));
            imageEntry.setTranslucent(image.testFlag(VloImage.FLAG_TRANSLUCENT));

            imageEntry.setInGameWidth(image.getInternalUnpaddedWidth());
            imageEntry.setInGameHeight(image.getInternalUnpaddedHeight());

            VloImage baseImage = this.baseResolver != null ? this.baseResolver.findImage(ref.bankName, image.getOriginalName() != null ? image.getOriginalName() : name) : null;
            if (baseImage != null) {
                if (baseImage.getBitDepth() != image.getBitDepth())
                    imageEntry.setBitDepth(image.getBitDepth().getBitsPerPixel());
                if (baseImage.testFlag(VloImage.FLAG_2D_SPRITE) != image.testFlag(VloImage.FLAG_2D_SPRITE))
                    problem(false, SECTION_IMAGES, key, "The 2D-sprite flag of " + key + " changed; format version 1 does not carry it.");
            } else if (this.baseInstance == null && gameInstance.isPSX()) {
                this.notes.add("The bit depth of " + key + " is not compared, because the fingerprint holds no image fields; bitDepth is left out.");
            }

            this.frogger.getImageEntries().add(imageEntry);
            addContent(path, pngBytes, "image " + key + " (" + image.getPaddedWidth() + "x" + image.getPaddedHeight() + ")");
        }

        private void exportLevelSelect(boolean arcade) {
            String sectionName = arcade ? SECTION_LEVEL_SELECT : SECTION_RACE_SELECT;
            FroggerBaseSectionDifference section = arcade ? this.difference.getLevelSelect() : this.difference.getRaceSelect();
            boolean orderChanged = arcade ? this.difference.isArcadeOrderChanged() : this.difference.isRaceOrderChanged();
            List<FroggerLevelSelectEntry> entries = arcade ? gameInstance.getArcadeLevelSelectEntries() : gameInstance.getRaceLevelSelectEntries();
            Map<String, FroggerLevelSelectEntry> rows = collectRows(entries);
            List<String> pointerNames = null;

            boolean anyChange = orderChanged;
            for (String key : section.getAllKeys())
                if (!isExcluded(sectionName, key))
                    anyChange = true;

            if (anyChange) {
                List<String> order = new ArrayList<>(rows.keySet());
                for (String key : order)
                    if (key.indexOf('@') >= 0)
                        problem(true, sectionName, key, "The level appears in more than one row; " + (arcade ? "arcade=" : "race=") + " cannot name the row.");

                if (arcade) {
                    this.frogger.setArcadeOrder(order);
                } else {
                    this.frogger.setRaceOrder(order);
                }

                this.exported.add((arcade ? "arcade" : "race") + " order " + String.join(",", order) + " (" + section.getRemoved().size() + " rows removed)");
            }

            List<String> rowKeys = new ArrayList<>(section.getChanged());
            rowKeys.addAll(section.getAdded());
            Set<String> written = new HashSet<>();
            for (String key : rowKeys) {
                if (isExcluded(sectionName, key) || !rows.containsKey(key))
                    continue;

                FroggerLevelSelectEntry entry = rows.get(key);
                String[] current = rowFieldsOf(resolver, entry);
                String[] baseFields = null;
                if (section.getChanged().contains(key)) {
                    if (this.baseInstance != null) {
                        FroggerLevelSelectEntry baseEntry = collectRows(arcade ? this.baseInstance.getArcadeLevelSelectEntries() : this.baseInstance.getRaceLevelSelectEntries()).get(key);
                        baseFields = baseEntry != null ? rowFieldsOf(this.baseResolver, baseEntry) : null;
                    } else {
                        if (pointerNames == null)
                            pointerNames = collectPointerNames();
                        String baseHash = (arcade ? this.base.getLevelSelect() : this.base.getRaceSelect()).get(key);
                        baseFields = baseHash != null ? searchRowFields(current, baseHash, pointerNames) : null;
                    }

                    if (baseFields == null)
                        this.notes.add(sectionName + " " + key + ": the base values could not be recovered, so every field of the row is written.");
                }

                FroggerModLevelEntry levelEntry = createLevelEntry(sectionName, key, current, baseFields);
                if (arcade)
                    levelEntry.setName(this.options.getLevelName(key));
                this.frogger.getLevelEntries().add(levelEntry);
                written.add(key);
                this.exported.add((arcade ? "arcade" : "race") + " row " + key + " " + levelEntry.toConfig().toString().replace("\r\n", " ").replace("\n", " ").trim());
            }

            if (arcade) {
                for (String key : rows.keySet()) {
                    String name = this.options.getLevelName(key);
                    if (name == null || written.contains(key))
                        continue;

                    FroggerModLevelEntry levelEntry = new FroggerModLevelEntry(key);
                    levelEntry.setName(name);
                    this.frogger.getLevelEntries().add(levelEntry);
                    this.exported.add("arcade row " + key + " name only");
                }
            }
        }

        private FroggerModLevelEntry createLevelEntry(String sectionName, String key, String[] current, String[] baseFields) {
            FroggerModLevelEntry levelEntry = new FroggerModLevelEntry(key);
            if (differs(current, baseFields, FIELD_WORLD))
                levelEntry.setWorld(current[FIELD_WORLD]);
            if (differs(current, baseFields, FIELD_STACK_POSITION))
                levelEntry.setStackPosition(Integer.parseInt(current[FIELD_STACK_POSITION]));
            if (differs(current, baseFields, FIELD_LOCAL_LEVEL_ID))
                levelEntry.setLocalLevelId(Integer.parseInt(current[FIELD_LOCAL_LEVEL_ID]));
            if (differs(current, baseFields, FIELD_LEVELS_IN_WORLD))
                levelEntry.setLevelsInWorld(Integer.parseInt(current[FIELD_LEVELS_IN_WORLD]));

            if (differs(current, baseFields, FIELD_LEVEL_NAME_TEXTURE) || differs(current, baseFields, FIELD_LEVEL_NAME_TEXTURE_IN_GAME)) {
                String nameImage = current[FIELD_LEVEL_NAME_TEXTURE];
                if (!nameImage.equals(current[FIELD_LEVEL_NAME_TEXTURE_IN_GAME]) || isPointerText(nameImage)) {
                    problem(false, sectionName, key, "The level-select and in-game name images of " + key + " are " + nameImage + " and "
                            + current[FIELD_LEVEL_NAME_TEXTURE_IN_GAME] + "; nameImage= sets both to one named image, so it is left out.");
                } else {
                    levelEntry.setNameImage(nameImage);
                }
            }

            if (differs(current, baseFields, FIELD_LEVEL_TEXTURE)) {
                if (isPointerText(current[FIELD_LEVEL_TEXTURE])) {
                    problem(false, sectionName, key, "The picture of " + key + " points at " + current[FIELD_LEVEL_TEXTURE] + ", which has no image name; picture= is left out.");
                } else {
                    levelEntry.setPicture(current[FIELD_LEVEL_TEXTURE]);
                }
            }

            if (baseFields != null) {
                for (int field : new int[] {FIELD_THEME, FIELD_WORLD_IMAGE_SELECTABLE, FIELD_WORLD_IMAGE_VISITED, FIELD_WORLD_IMAGE_NOT_TRIED})
                    if (differs(current, baseFields, field))
                        problem(false, sectionName, key, "The row field " + ROW_FIELD_LABELS[field] + " of " + key + " changed from " + baseFields[field]
                                + " to " + current[field] + "; format version 1 does not carry it.");
            }

            return levelEntry;
        }

        private void exportDemos() {
            FroggerBaseSectionDifference section = this.difference.getDemos();
            List<FroggerDemoTableEntry> entries = gameInstance.getDemoTable().getEntries();
            for (String key : section.getRemoved())
                problem(false, SECTION_DEMOS, key, "Demo table row " + key + " no longer exists; the table length is fixed.");

            List<String> keys = new ArrayList<>(section.getChanged());
            keys.addAll(section.getAdded());
            for (String key : keys) {
                Integer index = ModManifest.parseInteger(key);
                if (isExcluded(SECTION_DEMOS, key) || index == null || index < 0 || index >= entries.size())
                    continue;

                FroggerDemoTableEntry entry = entries.get(index);
                FroggerModDemoRow row;
                if (entry.isSkipped()) {
                    row = FroggerModDemoRow.createSkipped(index);
                } else {
                    String demoName = resourceName(entry.getDemoResourceId());
                    String levelCode = resolver.levelCodeOf(entry.getLevel());
                    String unlockCode = resolver.levelCodeOf(entry.getUnlockLevel());
                    if (demoName == null || levelCode == null || unlockCode == null) {
                        problem(true, SECTION_DEMOS, key, "Demo table row " + key + " names an unknown level or demo file.");
                        continue;
                    }

                    row = FroggerModDemoRow.create(index, levelCode, demoName, unlockCode);
                    if (entry instanceof FroggerPCDemoTableEntry) {
                        int lowPolyId = ((FroggerPCDemoTableEntry) entry).getLowPolyDemoResourceId();
                        if (lowPolyId >= 0 && lowPolyId != entry.getDemoResourceId())
                            problem(false, SECTION_DEMOS, key, "Demo table row " + key + " plays " + resourceName(lowPolyId)
                                    + " in low-poly mode; format version 1 has one demo per row, so the installer uses " + demoName + " for both.");
                    }
                }

                this.frogger.getDemoRows().add(row);
            }

            if (!this.frogger.getDemoRows().isEmpty())
                this.exported.add("demo table: " + this.frogger.getDemoRows().size() + " rows");

            List<String> demoFiles = new ArrayList<>();
            for (String key : this.difference.getFiles().getChanged()) {
                SCGameFile<?> file = this.filesByKey.get(key);
                if (!(file instanceof FroggerDemoFile))
                    continue;

                this.handledFileKeys.add(key);
                if (isExcluded(SECTION_FILES, key))
                    continue;

                String name = displayNameOf(file);
                demoFiles.add(name);
                addContent(ModFormatConstants.FOLDER_DEMOS + name, file.writeDataToByteArray(), "demo " + name);
            }

            if (!demoFiles.isEmpty())
                this.frogger.setDemoFiles(demoFiles);
        }

        private void exportThemes() {
            for (String key : this.difference.getThemes().getAllKeys()) {
                if (isExcluded(SECTION_THEMES, key))
                    continue;

                FroggerMapTheme theme = resolver.findTheme(key);
                FroggerThemeBook book = theme != null ? gameInstance.getThemeBook(theme) : null;
                if (book == null) {
                    problem(false, SECTION_THEMES, key, "The theme book " + key + " changed, but the open game has no such book.");
                    continue;
                }

                int deathHeight = book.execute(pc -> pc.getDeathHeight(), psx -> psx.getDeathHeight());
                Integer baseDeathHeight = findBaseDeathHeight(theme, book);
                if (baseDeathHeight == null) {
                    problem(false, SECTION_THEMES, key, "The theme book " + key + " changed in fields other than deathHeight (form library or WAD/VLO ids); format version 1 does not carry them.");
                    if (this.baseInstance == null)
                        this.notes.add("Theme " + key + ": no base deathHeight reproduces the fingerprint hash, so deathHeight is written as it is now.");
                    baseDeathHeight = Integer.MIN_VALUE;
                }

                if (baseDeathHeight == deathHeight)
                    continue;

                FroggerModThemeEntry themeEntry = new FroggerModThemeEntry(theme.name());
                themeEntry.setDeathHeight((double) DataUtils.fixedPointShortToFloat4Bit((short) deathHeight));
                this.frogger.getThemeEntries().add(themeEntry);
                this.exported.add("theme " + theme.name() + " deathHeight " + (baseDeathHeight != Integer.MIN_VALUE ? baseDeathHeight + " -> " : "") + deathHeight
                        + " (" + themeEntry.getDeathHeight() + ")");
            }
        }

        private Integer findBaseDeathHeight(FroggerMapTheme theme, FroggerThemeBook book) {
            String fields = themeFieldsOf(book);
            int split = fields.lastIndexOf(DEATH_HEIGHT_KEY);
            if (this.baseInstance != null) {
                FroggerThemeBook baseBook = this.baseInstance.getThemeBook(theme);
                if (baseBook == null)
                    return null;

                String baseFields = themeFieldsOf(baseBook);
                int baseSplit = baseFields.lastIndexOf(DEATH_HEIGHT_KEY);
                if (split < 0 || baseSplit < 0 || !fields.substring(0, split).equals(baseFields.substring(0, baseSplit)) || !formBookBytes(book).equals(formBookBytes(baseBook)))
                    return null;
                return baseBook.execute(pc -> pc.getDeathHeight(), psx -> psx.getDeathHeight());
            }

            String baseHash = this.base.getThemes().get(theme.name());
            if (baseHash == null || split < 0)
                return null;

            String prefix = fields.substring(0, split) + DEATH_HEIGHT_KEY;
            List<byte[]> forms = new ArrayList<>();
            for (FroggerFormEntry formEntry : book.getFormBook())
                forms.add(formEntry.writeDataToByteArray());

            for (int candidate = -THEME_SEARCH_LIMIT; candidate <= THEME_SEARCH_LIMIT; candidate++) {
                FroggerModHasher hasher = new FroggerModHasher();
                hasher.putString(prefix + candidate);
                hasher.putInt(forms.size());
                for (byte[] form : forms)
                    hasher.putBytes(form);
                if (baseHash.equals(hasher.finish()))
                    return candidate;
            }

            return null;
        }

        private void exportPatchSites() {
            FroggerModGameplaySettings settings = new FroggerModGameplaySettings();
            boolean gameplay = false;
            for (String id : this.difference.getPatchSites().getAllKeys()) {
                if (isExcluded(SECTION_PATCH_SITES, id))
                    continue;

                GamePatch patch = FroggerPatchLibrary.getPatchById(id);
                if (patch == null) {
                    problem(false, SECTION_PATCH_SITES, id, "The sites of the bundled patch '" + id + "' changed, but this FrogLord does not bundle it.");
                    continue;
                }

                Map<String, String> values;
                try {
                    values = readPatchVariables(patch);
                } catch (RuntimeException ex) {
                    problem(true, SECTION_PATCH_SITES, id, "The arguments of '" + id + "' could not be read back: " + ex.getMessage());
                    continue;
                }

                if (FroggerModGameplay.PATCH_FPS.equals(id) || FroggerModGameplay.PATCH_JUNGLE_SWAP.equals(id) || FroggerModGameplay.PATCH_INITIAL_LEVELS.equals(id)) {
                    FroggerModGameplay.fromPatchArguments(id, values, settings);
                    gameplay = true;
                    this.notes.add("Patch " + id + " read back: " + describeArguments(patch, values));
                    continue;
                }

                FroggerModPatchRef patchRef = new FroggerModPatchRef(patch.getId());
                for (PatchArgument argument : patch.getArguments())
                    if (values.get(argument.getName()) != null)
                        patchRef.getArguments().put(argument.getName(), values.get(argument.getName()));
                if (!patch.getArguments().isEmpty() && patch.getArgsCode().isEmpty())
                    problem(false, FroggerModManifest.SECTION_PATCHES, id, "The patch '" + id + "' has no [ArgsCode], so its arguments cannot be read back; the defaults are written.");

                this.frogger.getPatches().add(patchRef);
                this.exported.add("patch " + id + " " + patchRef.getArguments());
            }

            if (!gameplay)
                return;

            for (String message : settings.getProblems())
                problem(false, FroggerModManifest.SECTION_GAMEPLAY, null, message);

            if (settings.getFrameRate() != null) {
                this.frogger.setFrameRate(settings.getFrameRate());
                this.notes.add("frameRate is the value fps.patch reads back (1000 / delay / 2 with integer division, so a written 30 reads back as 31).");
            }

            if (Boolean.FALSE.equals(settings.getJungleSwap())) {
                this.frogger.setJungleSwap(Boolean.FALSE);
            } else if (Boolean.TRUE.equals(settings.getJungleSwap())) {
                problem(false, FroggerModManifest.SECTION_GAMEPLAY, FroggerModGameplay.KEY_JUNGLE_SWAP,
                        "The jungle-swap sites changed, but not to the patched bytes at all three sites; jungleSwap is left out.");
            }

            if (settings.getInitialLevels() != null) {
                this.frogger.setInitialLevels(settings.getInitialLevels());
                this.frogger.setVisibleLevels(settings.getVisibleLevels() != null && !settings.getVisibleLevels().isEmpty() ? settings.getVisibleLevels() : null);
            }

            this.exported.add("gameplay initialLevels=" + this.frogger.getInitialLevels() + " visibleLevels=" + this.frogger.getVisibleLevels()
                    + " jungleSwap=" + this.frogger.getJungleSwap() + " frameRate=" + this.frogger.getFrameRate());
        }

        private Map<String, String> readPatchVariables(GamePatch patch) {
            PatchRuntime runtime = new PatchRuntime(gameInstance, patch, null, ProblemResponse.THROW_EXCEPTION);
            runtime.runSetup();
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, PatchValue> entry : runtime.getVariables().entrySet())
                values.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
            return values;
        }

        private String describeArguments(GamePatch patch, Map<String, String> values) {
            List<String> parts = new ArrayList<>();
            if (FroggerModGameplay.PATCH_JUNGLE_SWAP.equals(patch.getId()))
                parts.add(FroggerModGameplay.VARIABLE_APPLIED + "=" + values.get(FroggerModGameplay.VARIABLE_APPLIED));
            for (PatchArgument argument : patch.getArguments())
                parts.add(argument.getName() + "=" + values.get(argument.getName()));
            return String.join(" ", parts);
        }

        private void exportExecutable() {
            if (this.difference.getBaseExecutableLength() != this.difference.getCurrentExecutableLength())
                problem(false, SECTION_EXE_BLOCKS, null, "The executable is " + this.difference.getCurrentExecutableLength() + " bytes, the base is "
                        + this.difference.getBaseExecutableLength() + "; format version 1 cannot change its length.");

            List<FroggerExeBlockChange> unexplained = new ArrayList<>();
            for (FroggerExeBlockChange change : this.difference.getUnexplainedExeBlocks()) {
                if (isExcluded(SECTION_EXE_BLOCKS, FroggerModHasher.formatRange(change.getBlock()))) {
                    continue;
                }

                unexplained.add(change);
                problem(false, SECTION_EXE_BLOCKS, FroggerModHasher.formatRange(change.getBlock()), "Unexplained executable change: " + change
                        + (this.options.isIncludeRawExecutableWrites() && this.baseInstance != null ? "; packaged as raw writes." : "; it is not packaged."));
            }

            if (unexplained.isEmpty() || !this.options.isIncludeRawExecutableWrites())
                return;

            if (this.baseInstance == null) {
                problem(false, SECTION_EXE_BLOCKS, null, "Raw executable writes need an unmodified copy of the build to compare with; none was given, so they are not packaged.");
                return;
            }

            byte[] baseBytes = this.baseInstance.getExecutableBytes();
            byte[] currentBytes = gameInstance.getExecutableBytes();
            int length = Math.min(this.difference.getBaseExecutableLength(), this.difference.getCurrentExecutableLength());
            boolean[] skipped = new boolean[length];
            for (FroggerOwnedRange owned : this.base.getOwnedRanges())
                FroggerModHasher.markRange(skipped, owned.getRange());
            for (List<PatchWriteRange> ranges : this.base.getPatchSiteRanges().values())
                for (PatchWriteRange range : ranges)
                    FroggerModHasher.markRange(skipped, range);

            StringBuilder code = new StringBuilder();
            int byteCount = 0;
            for (FroggerExeBlockChange change : unexplained) {
                PatchWriteRange block = change.getBlock();
                for (int address = Math.max(0, block.getAddress()); address < Math.min(length, block.getEndAddress()); address++) {
                    if (skipped[address] || baseBytes[address] == currentBytes[address])
                        continue;

                    code.append(String.format("writeb 0x%02X 0x%08X%n", currentBytes[address] & 0xFF, address).replace("\r\n", "\n"));
                    byteCount++;
                }
            }

            if (byteCount == 0)
                return;

            String text = "Name=Raw executable writes\n"
                    + "Description=Generated by FrogLord " + Constants.VERSION + " from the " + byteCount + " executable bytes that differ from "
                    + this.base.getBuildName() + " outside FrogLord-owned regions and bundled patch sites. Programs other than FrogLord ignore it.\n"
                    + "Author=" + (this.draft.getAuthor() != null ? this.draft.getAuthor().replace('#', ' ') : "FrogLord") + "\n"
                    + "Versions=" + this.base.getBuildName() + "\n\n[Code]\n" + code;
            FroggerModPatchRef patchRef = new FroggerModPatchRef(RAW_WRITES_PATCH_ID);
            patchRef.setFile(RAW_WRITES_PATCH_PATH);
            patchRef.setRaw(Boolean.TRUE);
            this.frogger.getPatches().add(patchRef);
            addContent(RAW_WRITES_PATCH_PATH, text.getBytes(StandardCharsets.UTF_8), "raw executable writes (" + byteCount + " bytes)");
        }

        private void reportOwnedRegions() {
            for (String key : this.difference.getOwnedRegions().getChanged()) {
                String label = key.indexOf(' ') > 0 ? key.substring(key.indexOf(' ') + 1) : key;
                boolean explained;
                switch (label) {
                    case "texture remaps":
                        explained = !this.difference.getRemaps().isEmpty();
                        break;
                    case "theme and form library":
                        explained = !this.difference.getThemes().isEmpty();
                        break;
                    case "level select":
                        explained = !this.difference.getLevelSelect().isEmpty() || !this.difference.getRaceSelect().isEmpty()
                                || this.difference.isArcadeOrderChanged() || this.difference.isRaceOrderChanged();
                        break;
                    case "demo tables":
                        explained = !this.difference.getDemos().isEmpty();
                        break;
                    default:
                        explained = false;
                        break;
                }

                if (!explained && !isExcluded(SECTION_EXE_OWNED, key))
                    problem(false, SECTION_EXE_OWNED, key, "The executable region " + key + " changed; format version 1 does not carry "
                            + label + " edits.");
            }
        }

        private void reportOtherFiles() {
            FroggerBaseSectionDifference section = this.difference.getFiles();
            for (String key : section.getRemoved())
                problem(false, SECTION_FILES, key, "The file " + key + " no longer exists; format version 1 cannot remove files.");
            for (String key : section.getAdded())
                problem(false, SECTION_FILES, key, "The file " + key + " is new; format version 1 cannot add files.");

            for (String key : section.getChanged()) {
                SCGameFile<?> file = this.filesByKey.get(key);
                if (this.handledFileKeys.contains(key) || file instanceof WADFile || file instanceof VloFile || isExcluded(SECTION_FILES, key))
                    continue;

                problem(false, SECTION_FILES, key, "The file " + key + " (" + (file != null ? file.getClass().getSimpleName() : "unknown")
                        + ") changed; format version 1 does not carry it.");
            }
        }

        private Map<String, SCGameFile<?>> collectFiles() {
            Map<String, SCGameFile<?>> results = new LinkedHashMap<>();
            for (SCGameFile<?> file : gameInstance.getMainArchive().getFiles()) {
                if (file == null)
                    continue;

                MWIResourceEntry entry = file.getIndexEntry();
                String name = entry != null ? entry.getDisplayName() : file.getFileDisplayName();
                results.put(FroggerModHasher.makeUniqueKey(results.keySet(), name, entry != null ? "@" + entry.getResourceId() : "@"), file);
            }

            return results;
        }

        private Map<String, FroggerModImageRef> collectImages() {
            Map<String, FroggerModImageRef> results = new LinkedHashMap<>();
            for (VloFile bank : gameInstance.getMainArchive().getAllFiles(VloFile.class)) {
                String bankName = resolver.bankNameOf(bank);
                if (bankName == null)
                    bankName = bank.getFileDisplayName();

                List<VloImage> images = bank.getImages();
                for (int i = 0; i < images.size(); i++) {
                    VloImage image = images.get(i);
                    String imageName = resolver.imageNameOf(image);
                    String key = FroggerModHasher.makeUniqueKey(results.keySet(), bankName + ":" + (imageName != null ? imageName : "id" + image.getTextureId()), "@" + i);
                    results.put(key, new FroggerModImageRef(bankName, image));
                }
            }

            return results;
        }

        private Map<TextureRemapArray, String> collectRemapKeys() {
            Map<TextureRemapArray, String> results = new IdentityHashMap<>();
            Set<String> used = new HashSet<>();
            for (TextureRemapArray remap : gameInstance.getTextureRemaps()) {
                String name = null;
                if (remap instanceof LinkedTextureRemap && ((LinkedTextureRemap<?>) remap).getResourceEntry() != null)
                    name = FileUtils.stripExtension(((LinkedTextureRemap<?>) remap).getResourceEntry().getDisplayName());
                if (name == null || name.isEmpty())
                    name = remap.getName();
                if (name == null || name.isEmpty())
                    name = String.format("remap@0x%08X", remap.getReaderIndex());

                String key = FroggerModHasher.makeUniqueKey(used, name, String.format("@0x%08X", remap.getReaderIndex()));
                used.add(key);
                results.put(remap, key);
            }

            return results;
        }

        private Map<String, FroggerLevelSelectEntry> collectRows(List<FroggerLevelSelectEntry> entries) {
            Map<String, FroggerLevelSelectEntry> results = new LinkedHashMap<>();
            for (FroggerLevelSelectEntry entry : entries) {
                if (entry.isTerminator())
                    continue;

                FroggerMapLevelID level = entry.getLevel();
                results.put(FroggerModHasher.makeUniqueKey(results.keySet(), level != null ? level.getInternalName() : "level", "@" + results.size()), entry);
            }

            return results;
        }

        private List<String> collectPointerNames() {
            Set<String> names = new LinkedHashSet<>();
            for (Long pointer : gameInstance.getBmpTexturePointers())
                if (pointer != null)
                    names.add(FroggerModHasher.pointerName(resolver, pointer));
            names.add(FroggerModHasher.pointerName(resolver, 0));
            return new ArrayList<>(names);
        }

        private String resourceName(int resourceId) {
            MWIResourceEntry entry = resourceId >= 0 ? gameInstance.getResourceEntryByID(resourceId) : null;
            return entry != null ? entry.getDisplayName() : null;
        }

        private boolean isExcluded(String section, String key) {
            if (key == null || !this.options.isExcluded(section, key))
                return false;

            String text = section + " " + key;
            if (!this.leftOut.contains(text))
                this.leftOut.add(text);
            return true;
        }

        private void addContent(String path, byte[] data, String description) {
            this.contents.put(path, data);
            this.exported.add(path + " (" + data.length + " bytes): " + description);
        }

        private void problem(boolean error, String section, String key, String message) {
            ModProblem problem = error ? ModProblem.error(section, key, message) : ModProblem.warning(section, key, message);
            this.problems.add(problem);
            if (error) {
                logger.severe("%s", problem);
            } else {
                logger.warning("%s", problem);
            }
        }

        private String buildLog(ModManifest manifest) {
            List<String> lines = new ArrayList<>();
            lines.add("FrogLord " + Constants.VERSION + " mod package export");
            lines.add("Mod: " + manifest.getName() + " " + manifest.getVersion() + " (" + manifest.getId() + ")");
            lines.add("Open game: " + gameInstance.getVersionConfig().getInternalName());
            lines.add("Base: " + (this.baseInstance != null ? "unmodified copy of " : "fingerprint of ") + this.base.getBuildName()
                    + (this.base.getGenerated() != null ? " (fingerprint generated " + this.base.getGenerated() + ")" : ""));
            lines.add("");
            lines.add("Differences found:");
            for (String line : this.difference.describe())
                lines.add("  " + line);
            lines.add("");
            lines.add("Exported (" + this.contents.size() + " content files):");
            for (String line : this.exported)
                lines.add("  " + line);
            if (!this.leftOut.isEmpty()) {
                lines.add("");
                lines.add("Left out by the author:");
                for (String line : this.leftOut)
                    lines.add("  " + line);
            }
            if (!this.notes.isEmpty()) {
                lines.add("");
                lines.add("Notes:");
                for (String line : this.notes)
                    lines.add("  " + line);
            }
            lines.add("");
            lines.add("Problems (" + this.problems.size() + "):");
            for (ModProblem problem : this.problems)
                lines.add("  " + problem);
            return String.join("\n", lines) + "\n";
        }
    }

    private static String[] rowFieldsOf(FroggerModNameResolver resolver, FroggerLevelSelectEntry entry) {
        String theme;
        try {
            theme = entry.getTheme() != null ? entry.getTheme().name() : "null";
        } catch (RuntimeException ex) {
            theme = "invalid";
        }

        FroggerMapLevelID level = entry.getLevel();
        String[] fields = new String[ROW_FIELD_LABELS.length];
        fields[FIELD_LEVEL] = level != null ? level.name() : "null";
        fields[FIELD_WORLD] = entry.getWorld() != null ? entry.getWorld().name() : "null";
        fields[FIELD_STACK_POSITION] = String.valueOf(entry.getStackPosition());
        fields[FIELD_THEME] = theme;
        fields[FIELD_LOCAL_LEVEL_ID] = String.valueOf(entry.getLocalLevelId());
        fields[FIELD_LEVELS_IN_WORLD] = String.valueOf(entry.getLevelsInWorld());
        fields[FIELD_WORLD_IMAGE_SELECTABLE] = FroggerModHasher.pointerName(resolver, entry.getWorldImageSelectablePointer());
        fields[FIELD_WORLD_IMAGE_VISITED] = FroggerModHasher.pointerName(resolver, entry.getWorldImageVisitedPointer());
        fields[FIELD_WORLD_IMAGE_NOT_TRIED] = FroggerModHasher.pointerName(resolver, entry.getWorldImageNotTriedPointer());
        fields[FIELD_LEVEL_TEXTURE] = FroggerModHasher.pointerName(resolver, entry.getLevelTexturePointer());
        fields[FIELD_LEVEL_NAME_TEXTURE] = FroggerModHasher.pointerName(resolver, entry.getLevelNameTexturePointer());
        fields[FIELD_LEVEL_NAME_TEXTURE_IN_GAME] = FroggerModHasher.pointerName(resolver, entry.getLevelNameTextureInGamePointer());
        return fields;
    }

    private static String rowHashOf(String[] fields) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0)
                builder.append(';');
            builder.append(ROW_FIELD_LABELS[i]).append('=').append(fields[i]);
        }

        return FroggerModHasher.sha1(builder.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String[] searchRowFields(String[] current, String baseHash, List<String> pointerNames) {
        List<FroggerModRowVariant> scalarVariants = new ArrayList<>();
        for (FroggerLevelSelectWorldID world : FroggerLevelSelectWorldID.values())
            scalarVariants.add(new FroggerModRowVariant(new int[] {FIELD_WORLD}, world.name()));
        scalarVariants.add(new FroggerModRowVariant(new int[] {FIELD_WORLD}, "null"));
        for (FroggerMapTheme theme : FroggerMapTheme.values())
            scalarVariants.add(new FroggerModRowVariant(new int[] {FIELD_THEME}, theme.name()));
        scalarVariants.add(new FroggerModRowVariant(new int[] {FIELD_THEME}, "null"));
        scalarVariants.add(new FroggerModRowVariant(new int[] {FIELD_THEME}, "invalid"));
        for (int field : new int[] {FIELD_STACK_POSITION, FIELD_LOCAL_LEVEL_ID, FIELD_LEVELS_IN_WORLD})
            for (int value = 0; value < SEARCH_INT_LIMIT; value++)
                scalarVariants.add(new FroggerModRowVariant(new int[] {field}, String.valueOf(value)));

        List<FroggerModRowVariant> pointerVariants = new ArrayList<>();
        int[][] pointerGroups = {{FIELD_LEVEL_NAME_TEXTURE, FIELD_LEVEL_NAME_TEXTURE_IN_GAME}, {FIELD_LEVEL_TEXTURE},
                {FIELD_WORLD_IMAGE_SELECTABLE, FIELD_WORLD_IMAGE_VISITED, FIELD_WORLD_IMAGE_NOT_TRIED}, {FIELD_LEVEL_NAME_TEXTURE},
                {FIELD_LEVEL_NAME_TEXTURE_IN_GAME}, {FIELD_WORLD_IMAGE_SELECTABLE}, {FIELD_WORLD_IMAGE_VISITED}, {FIELD_WORLD_IMAGE_NOT_TRIED}};
        for (int[] group : pointerGroups)
            for (String name : pointerNames)
                pointerVariants.add(new FroggerModRowVariant(group, name));

        scalarVariants.removeIf(variant -> variant.isNoChange(current));
        pointerVariants.removeIf(variant -> variant.isNoChange(current));

        List<FroggerModRowVariant> singles = new ArrayList<>(scalarVariants);
        singles.addAll(pointerVariants);
        for (FroggerModRowVariant variant : singles) {
            String[] candidate = variant.apply(current);
            if (baseHash.equals(rowHashOf(candidate)))
                return candidate;
        }

        for (int i = 0; i < scalarVariants.size(); i++) {
            for (int j = i + 1; j < scalarVariants.size(); j++) {
                FroggerModRowVariant first = scalarVariants.get(i);
                FroggerModRowVariant second = scalarVariants.get(j);
                if (first.overlaps(second))
                    continue;

                String[] candidate = second.apply(first.apply(current));
                if (baseHash.equals(rowHashOf(candidate)))
                    return candidate;
            }
        }

        for (FroggerModRowVariant scalar : scalarVariants) {
            String[] partial = scalar.apply(current);
            for (FroggerModRowVariant pointer : pointerVariants) {
                String[] candidate = pointer.apply(partial);
                if (baseHash.equals(rowHashOf(candidate)))
                    return candidate;
            }
        }

        return null;
    }

    private static boolean differs(String[] current, String[] baseFields, int field) {
        return baseFields == null || !current[field].equals(baseFields[field]);
    }

    private static boolean isPointerText(String pointerName) {
        return pointerName == null || pointerName.startsWith("0x");
    }

    private static String themeFieldsOf(FroggerThemeBook book) {
        return book.execute(
                pc -> "highWadId=" + pc.getHighWadId() + ";highVloId=" + pc.getHighVloId() + ";lowWadId=" + pc.getLowWadId() + ";lowVloId=" + pc.getLowVloId()
                        + ";highMultiplayerWadId=" + pc.getHighMultiplayerWadId() + ";highMultiplayerVloId=" + pc.getHighMultiplayerVloId()
                        + ";lowMultiplayerWadId=" + pc.getLowMultiplayerWadId() + ";lowMultiplayerVloId=" + pc.getLowMultiplayerVloId()
                        + ";formLibraryPointer=" + pc.getFormLibraryPointer() + DEATH_HEIGHT_KEY + pc.getDeathHeight(),
                psx -> "wadId=" + psx.getWadId() + ";vloId=" + psx.getVloId() + ";multiplayerWadId=" + psx.getMultiplayerWadId()
                        + ";multiplayerVloId=" + psx.getMultiplayerVloId() + ";formLibraryPointer=" + psx.getFormLibraryPointer()
                        + DEATH_HEIGHT_KEY + psx.getDeathHeight());
    }

    private static String formBookBytes(FroggerThemeBook book) {
        FroggerModHasher hasher = new FroggerModHasher();
        for (FroggerFormEntry formEntry : book.getFormBook())
            hasher.putBytes(formEntry.writeDataToByteArray());
        return hasher.finish();
    }

    private static SCGameConfig findVersionConfig(String buildName) {
        for (GameConfig config : SCGameType.FROGGER.getVersionConfigs())
            if (config instanceof SCGameConfig && config.getInternalName().equals(buildName))
                return (SCGameConfig) config;
        return null;
    }

    private static String displayNameOf(SCGameFile<?> file) {
        MWIResourceEntry entry = file.getIndexEntry();
        return entry != null ? entry.getDisplayName() : file.getFileDisplayName();
    }

    private static class FroggerModLevelMaps {
        private final String code;
        private FroggerMapFile map;
        private FroggerMapFile lowPolyMap;
        private WADFile levelWad;
        private boolean mapChanged;
        private boolean lowPolyMapChanged;
        private boolean remapChanged;
        private boolean wadChanged;

        private FroggerModLevelMaps(String code) {
            this.code = code;
        }
    }

    private static class FroggerModImageRef {
        private final String bankName;
        private final VloImage image;

        private FroggerModImageRef(String bankName, VloImage image) {
            this.bankName = bankName;
            this.image = image;
        }
    }

    private static class FroggerModRowVariant {
        private final int[] fields;
        private final String value;

        private FroggerModRowVariant(int[] fields, String value) {
            this.fields = fields;
            this.value = value;
        }

        private boolean isNoChange(String[] current) {
            for (int field : this.fields)
                if (!current[field].equals(this.value))
                    return false;
            return true;
        }

        private boolean overlaps(FroggerModRowVariant other) {
            for (int field : this.fields)
                for (int otherField : other.fields)
                    if (field == otherField)
                        return true;
            return false;
        }

        private String[] apply(String[] fields) {
            String[] result = fields.clone();
            for (int field : this.fields)
                result[field] = this.value;
            return result;
        }
    }

    private static class FroggerModWadSlotSnapshot {
        private final WADEntry entry;
        private final SCGameFile<?> file;
        private final int resourceId;
        private final MRStaticMof staticMof;
        private final MRAnimatedMof animatedMof;
        private final boolean incomplete;

        private FroggerModWadSlotSnapshot(WADEntry entry) {
            this.entry = entry;
            this.file = entry.getFile();
            this.resourceId = entry.getResourceId();
            MRModel model = this.file instanceof MRModel ? (MRModel) this.file : null;
            this.staticMof = model != null ? model.getStaticMof() : null;
            this.animatedMof = model != null ? model.getAnimatedMof() : null;
            this.incomplete = model != null && model.isIncomplete();
        }

        private void restore() {
            if (this.entry.getFile() != this.file || this.entry.getResourceId() != this.resourceId) {
                this.entry.setResourceId(this.resourceId);
                this.entry.setFile(this.file);
            }

            if (!(this.file instanceof MRModel))
                return;

            MRModel model = (MRModel) this.file;
            if (model.getStaticMof() == this.staticMof && model.getAnimatedMof() == this.animatedMof && model.isIncomplete() == this.incomplete)
                return;

            if (this.staticMof != null)
                model.setStaticMof(this.staticMof);
            if (this.animatedMof != null)
                model.setAnimatedMof(this.animatedMof);
            model.setIncomplete(this.incomplete);
        }
    }

}
