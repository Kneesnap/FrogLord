package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.patch.GamePatch;
import net.highwayfrogs.editor.file.patch.PatchWriteRecorder;
import net.highwayfrogs.editor.file.patch.PatchWriteRecorder.PatchWriteRange;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameType;
import net.highwayfrogs.editor.games.sony.frogger.FroggerConfig;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerDemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerPCDemoTableEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.map.FroggerMapBook;
import net.highwayfrogs.editor.games.sony.frogger.data.theme.FroggerThemeBook;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.animation.FroggerMapAnimation;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.packets.FroggerMapFilePacketAnimation;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference.FroggerBaseSectionDifference;
import net.highwayfrogs.editor.games.sony.frogger.mods.FroggerBaseDifference.FroggerExeBlockChange;
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
import net.highwayfrogs.editor.system.Config;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * Holds the hashes of an unmodified Frogger build, as FrogLord re-serialises it, so that changes made to a game can be found without the original files.
 * Created by jimbolina on 11/6/2026.
 */
@Getter
public class FroggerBaseFingerprint {
    private String buildName;
    private long exeChecksum;
    private int exeLength;
    private int frogLordVersion;
    private String generated;
    private final Map<String, String> files = new LinkedHashMap<>();
    private final Map<String, String> wadChildren = new LinkedHashMap<>();
    private final Map<String, String> images = new LinkedHashMap<>();
    private final Map<String, String> levelSelect = new LinkedHashMap<>();
    private final List<String> arcadeOrder = new ArrayList<>();
    private final Map<String, String> raceSelect = new LinkedHashMap<>();
    private final List<String> raceOrder = new ArrayList<>();
    private final Map<String, String> demos = new LinkedHashMap<>();
    private final Map<String, String> themes = new LinkedHashMap<>();
    private final Map<String, String> remaps = new LinkedHashMap<>();
    private final List<FroggerOwnedRange> ownedRanges = new ArrayList<>();
    private final Map<String, String> exeBlocks = new LinkedHashMap<>();
    private final Map<String, String> patchSiteHashes = new LinkedHashMap<>();
    private final Map<String, List<PatchWriteRange>> patchSiteRanges = new LinkedHashMap<>();

    public static final String FINGERPRINT_FOLDER = "fingerprints";
    public static final String SECTION_FINGERPRINT = "Fingerprint";
    public static final String SECTION_FILES = "Files";
    public static final String SECTION_WAD_CHILDREN = "WadChildren";
    public static final String SECTION_IMAGES = "Images";
    public static final String SECTION_LEVEL_SELECT = "LevelSelect";
    public static final String SECTION_RACE_SELECT = "RaceSelect";
    public static final String SECTION_DEMOS = "Demos";
    public static final String SECTION_THEMES = "Themes";
    public static final String SECTION_REMAPS = "Remaps";
    public static final String SECTION_EXE_OWNED = "ExeOwned";
    public static final String SECTION_EXE_BLOCKS = "ExeBlocks";
    public static final String SECTION_PATCH_SITES = "PatchSites";
    public static final int EXE_BLOCK_SIZE = 1024;
    public static final String UNPATCHED_SUFFIX = "/unpatched";
    public static final String UNCOMPARED_HASH = "-";
    public static final String DUMMY_HASH = FroggerModHasher.sha1(MRModel.DUMMY_DATA);

    private static final String PATCH_FOLDER = "patches";
    private static final String PATCH_EXTENSION = ".patch";
    private static final String KEY_BUILD = "build";
    private static final String KEY_EXE_CHECKSUM = "exeChecksum";
    private static final String KEY_EXE_LENGTH = "exeLength";
    private static final String KEY_FROGLORD_VERSION = "frogLordVersion";
    private static final String KEY_GENERATED = "generated";
    private static final String KEY_ORDER = "order";
    private static final String LABEL_MWI = "MWI";
    private static final String LABEL_REMAINDER = "scripts and music";
    private static final String LABEL_PSX_TRAILER = "FrogLord trailer";
    private static final int IMAGE_FLAG_MASK = VloImage.FLAG_TRANSLUCENT | VloImage.FLAG_BLACK_IS_TRANSPARENT | VloImage.FLAG_2D_SPRITE;

    /**
     * Generates the fingerprint of a loaded game. The game is not changed: maps are serialised with the level-WAD dummying of
     * FroggerMapFilePacketEntity undone straight after, and executable regions are found with a recording writer.
     * Two re-serialisation effects are left out so that a game FrogLord has saved compares equal to the build it came from:
     * VLO files hash as their images (VRAM placement is redone on every save), and maps hash without the unused animation
     * texture ids, which every save moves one animation further on.
     * @param instance the game to fingerprint, normally a freshly loaded retail copy
     * @param logger the logger to report to, or null for the class logger
     * @return fingerprint
     */
    public static FroggerBaseFingerprint generate(@NonNull FroggerGameInstance instance, ILogger logger) {
        if (logger == null)
            logger = ClassNameLogger.getLogger(instance, FroggerBaseFingerprint.class);

        byte[] exeBytes = instance.getExecutableBytes();
        FroggerBaseFingerprint fingerprint = new FroggerBaseFingerprint();
        fingerprint.buildName = instance.getVersionConfig().getInternalName();
        fingerprint.exeLength = getEffectiveExecutableLength(instance, exeBytes);
        fingerprint.exeChecksum = DataUtils.getCRC32(Arrays.copyOf(exeBytes, fingerprint.exeLength));
        fingerprint.frogLordVersion = Constants.UPDATE_VERSION;
        fingerprint.generated = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        fingerprint.ownedRanges.addAll(recordOwnedRanges(instance, fingerprint.exeLength));
        fingerprint.patchSiteRanges.putAll(recordPatchSites(instance, logger));
        fingerprint.populate(instance, logger, exeBytes, fingerprint.exeLength);
        logger.info("Generated the base fingerprint of %s: %d files, %d WAD children, %d images, %d executable blocks, %d patch sites.",
                fingerprint.buildName, fingerprint.files.size(), fingerprint.wadChildren.size(), fingerprint.images.size(),
                fingerprint.exeBlocks.size(), fingerprint.patchSiteHashes.size());
        return fingerprint;
    }

    /**
     * Loads the fingerprint bundled with FrogLord for a build, from games/frogger/fingerprints/&lt;build&gt;.cfg.
     * @param buildInternalName the version internal name, such as pc-retail-v3.0e
     * @return fingerprint, or null when FrogLord has none for that build
     */
    public static FroggerBaseFingerprint loadBundled(String buildInternalName) {
        if (buildInternalName == null || buildInternalName.isEmpty())
            return null;

        URL url = SCGameType.FROGGER.getEmbeddedResourceURL(FINGERPRINT_FOLDER + "/" + buildInternalName + "." + Config.DEFAULT_EXTENSION);
        if (url == null)
            return null;

        return load(Config.loadTextConfigFromURL(url));
    }

    /**
     * Loads a fingerprint from its text config form.
     * @param config the root config of a fingerprint file
     * @return fingerprint
     */
    public static FroggerBaseFingerprint load(@NonNull Config config) {
        Config header = config.getChildConfigByName(SECTION_FINGERPRINT);
        if (header == null)
            throw new IllegalArgumentException("The config has no [" + SECTION_FINGERPRINT + "] section, so it is not a base fingerprint.");

        FroggerBaseFingerprint fingerprint = new FroggerBaseFingerprint();
        fingerprint.buildName = header.getKeyValueNodeOrError(KEY_BUILD).getAsString();
        fingerprint.exeChecksum = Long.parseLong(header.getKeyValueNodeOrError(KEY_EXE_CHECKSUM).getAsString());
        fingerprint.exeLength = header.getKeyValueNodeOrError(KEY_EXE_LENGTH).getAsInteger();
        fingerprint.frogLordVersion = header.getKeyValueNodeOrError(KEY_FROGLORD_VERSION).getAsInteger();
        fingerprint.generated = header.getOrDefaultKeyValueNode(KEY_GENERATED).getAsString();

        readHashSection(config, SECTION_FILES, fingerprint.files);
        readHashSection(config, SECTION_WAD_CHILDREN, fingerprint.wadChildren);
        readHashSection(config, SECTION_IMAGES, fingerprint.images);
        readHashSection(config, SECTION_LEVEL_SELECT, fingerprint.levelSelect);
        readOrder(config, SECTION_LEVEL_SELECT, fingerprint.arcadeOrder);
        readHashSection(config, SECTION_RACE_SELECT, fingerprint.raceSelect);
        readOrder(config, SECTION_RACE_SELECT, fingerprint.raceOrder);
        readHashSection(config, SECTION_DEMOS, fingerprint.demos);
        readHashSection(config, SECTION_THEMES, fingerprint.themes);
        readHashSection(config, SECTION_REMAPS, fingerprint.remaps);
        readHashSection(config, SECTION_EXE_BLOCKS, fingerprint.exeBlocks);

        for (String line : getSectionLines(config, SECTION_EXE_OWNED)) {
            String[] split = line.split(" ", 3);
            if (split.length < 2)
                throw new IllegalArgumentException("Invalid [" + SECTION_EXE_OWNED + "] line: '" + line + "'.");
            fingerprint.ownedRanges.add(new FroggerOwnedRange(parseRange(split[1]), split.length > 2 ? split[2] : "", split[0]));
        }

        for (String line : getSectionLines(config, SECTION_PATCH_SITES)) {
            String[] split = line.split(" ", 3);
            if (split.length < 2)
                throw new IllegalArgumentException("Invalid [" + SECTION_PATCH_SITES + "] line: '" + line + "'.");

            List<PatchWriteRange> ranges = new ArrayList<>();
            if (split.length > 2)
                for (String range : split[2].split(","))
                    if (!range.trim().isEmpty())
                        ranges.add(parseRange(range.trim()));

            fingerprint.patchSiteHashes.put(split[1], split[0]);
            fingerprint.patchSiteRanges.put(split[1], Collections.unmodifiableList(ranges));
        }

        return fingerprint;
    }

    /**
     * Writes this fingerprint in its text config form.
     * @return rootConfig
     */
    public Config toConfig() {
        Config root = new Config(this.buildName);
        Config header = new Config(SECTION_FINGERPRINT);
        header.getOrCreateKeyValueNode(KEY_BUILD).setAsString(this.buildName);
        header.getOrCreateKeyValueNode(KEY_EXE_CHECKSUM).setAsString(String.valueOf(this.exeChecksum));
        header.getOrCreateKeyValueNode(KEY_EXE_LENGTH).setAsInteger(this.exeLength);
        header.getOrCreateKeyValueNode(KEY_FROGLORD_VERSION).setAsInteger(this.frogLordVersion);
        header.getOrCreateKeyValueNode(KEY_GENERATED).setAsString(this.generated);
        root.addChildConfig(header);

        root.addChildConfig(writeHashSection(SECTION_FILES, this.files));
        root.addChildConfig(writeHashSection(SECTION_WAD_CHILDREN, this.wadChildren));
        root.addChildConfig(writeHashSection(SECTION_IMAGES, this.images));
        Config levelSelectConfig = writeHashSection(SECTION_LEVEL_SELECT, this.levelSelect);
        levelSelectConfig.getOrCreateKeyValueNode(KEY_ORDER).setAsString(String.join(",", this.arcadeOrder));
        root.addChildConfig(levelSelectConfig);
        Config raceSelectConfig = writeHashSection(SECTION_RACE_SELECT, this.raceSelect);
        raceSelectConfig.getOrCreateKeyValueNode(KEY_ORDER).setAsString(String.join(",", this.raceOrder));
        root.addChildConfig(raceSelectConfig);
        root.addChildConfig(writeHashSection(SECTION_DEMOS, this.demos));
        root.addChildConfig(writeHashSection(SECTION_THEMES, this.themes));
        root.addChildConfig(writeHashSection(SECTION_REMAPS, this.remaps));

        Config ownedConfig = new Config(SECTION_EXE_OWNED);
        for (FroggerOwnedRange owned : this.ownedRanges)
            ownedConfig.getInternalText().add(new Config.ConfigValueNode(owned.getHash() + " " + FroggerModHasher.formatRange(owned.getRange()) + " " + owned.getLabel()));
        root.addChildConfig(ownedConfig);

        root.addChildConfig(writeHashSection(SECTION_EXE_BLOCKS, this.exeBlocks));

        Config patchConfig = new Config(SECTION_PATCH_SITES);
        for (Map.Entry<String, String> entry : this.patchSiteHashes.entrySet()) {
            List<String> ranges = new ArrayList<>();
            for (PatchWriteRange range : this.patchSiteRanges.get(entry.getKey()))
                ranges.add(FroggerModHasher.formatRange(range));
            patchConfig.getInternalText().add(new Config.ConfigValueNode(entry.getValue() + " " + entry.getKey() + " " + String.join(",", ranges)));
        }
        root.addChildConfig(patchConfig);
        return root;
    }

    /**
     * Saves this fingerprint as a text config file.
     * @param outputFile the file to write
     */
    public void save(@NonNull File outputFile) {
        File parent = outputFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists())
            FileUtils.makeDirectory(parent);

        try {
            Files.write(outputFile.toPath(), toConfig().toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to write the base fingerprint to '" + outputFile + "'.", ex);
        }
    }

    /**
     * Compares a game against this fingerprint. The same hashes are derived from the game, over the executable regions
     * recorded in this fingerprint, and every difference is classified. The game is not changed.
     * @param instance the game to compare
     * @param logger the logger to report to, or null for the class logger
     * @return difference
     */
    public FroggerBaseDifference compare(@NonNull FroggerGameInstance instance, ILogger logger) {
        if (logger == null)
            logger = ClassNameLogger.getLogger(instance, FroggerBaseFingerprint.class);

        byte[] exeBytes = instance.getExecutableBytes();
        FroggerBaseFingerprint current = new FroggerBaseFingerprint();
        current.buildName = instance.getVersionConfig().getInternalName();
        current.exeLength = getEffectiveExecutableLength(instance, exeBytes);
        current.ownedRanges.addAll(this.ownedRanges);
        current.patchSiteRanges.putAll(this.patchSiteRanges);
        current.populate(instance, logger, exeBytes, this.exeLength);

        FroggerBaseDifference difference = new FroggerBaseDifference(this.buildName, current.buildName);
        difference.setBaseExecutableLength(this.exeLength);
        difference.setCurrentExecutableLength(current.exeLength);
        compareSection(this.files, current.files, difference.getFiles());
        compareWadChildren(current, difference);
        compareSection(this.images, current.images, difference.getImages());
        compareSection(this.levelSelect, current.levelSelect, difference.getLevelSelect());
        difference.setArcadeOrderChanged(isOrderChanged(this.arcadeOrder, current.arcadeOrder));
        compareSection(this.raceSelect, current.raceSelect, difference.getRaceSelect());
        difference.setRaceOrderChanged(isOrderChanged(this.raceOrder, current.raceOrder));
        compareSection(this.demos, current.demos, difference.getDemos());
        compareSection(this.themes, current.themes, difference.getThemes());
        compareSection(this.remaps, current.remaps, difference.getRemaps());
        compareSection(this.patchSiteHashes, current.patchSiteHashes, difference.getPatchSites());

        for (int i = 0; i < this.ownedRanges.size(); i++) {
            FroggerOwnedRange baseRange = this.ownedRanges.get(i);
            FroggerOwnedRange currentRange = current.ownedRanges.get(i);
            if (!UNCOMPARED_HASH.equals(baseRange.getHash()) && !baseRange.getHash().equals(currentRange.getHash()))
                difference.getOwnedRegions().getChanged().add(FroggerModHasher.formatRange(baseRange.getRange()) + " " + baseRange.getLabel());
        }

        compareExeBlocks(current, difference);
        logger.info("Compared %s with the base fingerprint of %s: %s.", current.buildName, this.buildName, difference.isEmpty() ? "no differences" : "differences found");
        return difference;
    }

    private void compareWadChildren(FroggerBaseFingerprint current, FroggerBaseDifference difference) {
        FroggerBaseSectionDifference section = difference.getWadChildren();
        for (Map.Entry<String, String> entry : this.wadChildren.entrySet()) {
            String currentHash = current.wadChildren.get(entry.getKey());
            if (currentHash == null) {
                section.getRemoved().add(entry.getKey());
            } else if (!currentHash.equals(entry.getValue())) {
                if (DUMMY_HASH.equals(currentHash) && !DUMMY_HASH.equals(entry.getValue())) {
                    difference.getDummiedWadChildren().add(entry.getKey());
                } else {
                    section.getChanged().add(entry.getKey());
                }
            }
        }

        for (String key : current.wadChildren.keySet())
            if (!this.wadChildren.containsKey(key))
                section.getAdded().add(key);
    }

    private void compareExeBlocks(FroggerBaseFingerprint current, FroggerBaseDifference difference) {
        for (Map.Entry<String, String> entry : this.exeBlocks.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(UNPATCHED_SUFFIX) || entry.getValue().equals(current.exeBlocks.get(key)))
                continue;

            PatchWriteRange block = parseRange(key);
            String unpatchedKey = key + UNPATCHED_SUFFIX;
            String baseUnpatched = this.exeBlocks.get(unpatchedKey);
            boolean explained = baseUnpatched != null && baseUnpatched.equals(current.exeBlocks.get(unpatchedKey));

            List<String> patchIds = new ArrayList<>();
            for (Map.Entry<String, List<PatchWriteRange>> patchEntry : this.patchSiteRanges.entrySet())
                for (PatchWriteRange range : patchEntry.getValue())
                    if (range.getAddress() < block.getEndAddress() && range.getEndAddress() > block.getAddress() && !patchIds.contains(patchEntry.getKey()))
                        patchIds.add(patchEntry.getKey());

            difference.getExeBlocks().add(new FroggerExeBlockChange(block, explained, patchIds));
        }
    }

    private void populate(FroggerGameInstance instance, ILogger logger, byte[] exeBytes, int blockLength) {
        FroggerModNameResolver resolver = new FroggerModNameResolver(instance);
        Map<VloFile, String> bankHashes = hashImages(instance, resolver);
        hashFiles(instance, resolver, logger, bankHashes);
        hashLevelSelect(resolver, instance.getArcadeLevelSelectEntries(), this.levelSelect, this.arcadeOrder);
        hashLevelSelect(resolver, instance.getRaceLevelSelectEntries(), this.raceSelect, this.raceOrder);
        hashDemos(instance, resolver);
        hashThemes(instance);
        hashRemaps(instance);
        hashExecutable(exeBytes, blockLength);
    }

    private void hashFiles(FroggerGameInstance instance, FroggerModNameResolver resolver, ILogger logger, Map<VloFile, String> bankHashes) {
        for (SCGameFile<?> file : instance.getMainArchive().getFiles()) {
            if (file == null)
                continue;

            MWIResourceEntry entry = file.getIndexEntry();
            String name = entry != null ? entry.getDisplayName() : file.getFileDisplayName();
            String key = FroggerModHasher.makeUniqueKey(this.files.keySet(), name, entry != null ? "@" + entry.getResourceId() : "@");
            if (file instanceof WADFile) {
                WADFile wad = (WADFile) file;
                String wadName = resolver.wadNameOf(wad);
                FroggerModHasher containerHasher = new FroggerModHasher();
                List<WADEntry> children = wad.getFiles();
                containerHasher.putInt(children.size());
                for (int i = 0; i < children.size(); i++) {
                    WADEntry child = children.get(i);
                    String childHash = hashWadChild(instance, child, logger);
                    this.wadChildren.put((wadName != null ? wadName : key) + ":" + i, childHash);
                    MWIResourceEntry childEntry = child.getFileEntry();
                    containerHasher.putInt(child.getResourceId());
                    containerHasher.putInt(childEntry != null ? childEntry.getTypeId() : Integer.MIN_VALUE);
                    containerHasher.putBoolean(child.isCompressed());
                    containerHasher.putString(childHash);
                }

                this.files.put(key, containerHasher.finish());
            } else if (file instanceof VloFile && bankHashes.containsKey(file)) {
                this.files.put(key, bankHashes.get(file));
            } else {
                this.files.put(key, FroggerModHasher.sha1(serializeWithoutSideEffects(instance, file, logger)));
            }
        }
    }

    private static String hashWadChild(FroggerGameInstance instance, WADEntry child, ILogger logger) {
        if (child.isDummy())
            return DUMMY_HASH;

        return FroggerModHasher.sha1(serializeWithoutSideEffects(instance, child.getFile(), logger));
    }

    private static byte[] serializeWithoutSideEffects(FroggerGameInstance instance, SCGameFile<?> file, ILogger logger) {
        if (!(file instanceof FroggerMapFile))
            return serialize(file, logger);

        List<FroggerWadSlotState> states = new ArrayList<>();
        for (WADFile wad : instance.getMainArchive().getAllFiles(WADFile.class))
            for (WADEntry entry : wad.getFiles())
                states.add(new FroggerWadSlotState(entry));

        FroggerMapFilePacketAnimation animationPacket = ((FroggerMapFile) file).getAnimationPacket();
        List<List<Short>> unusedTextureIdLists = new ArrayList<>();
        unusedTextureIdLists.add(animationPacket.getUnusedTextureIds());
        for (FroggerMapAnimation animation : animationPacket.getAnimations())
            unusedTextureIdLists.add(animation.getUnusedTextureIds());

        List<List<Short>> savedUnusedTextureIds = new ArrayList<>();
        for (List<Short> list : unusedTextureIdLists) {
            savedUnusedTextureIds.add(new ArrayList<>(list));
            list.clear();
        }

        try {
            return serialize(file, logger);
        } finally {
            for (int i = 0; i < unusedTextureIdLists.size(); i++) {
                unusedTextureIdLists.get(i).clear();
                unusedTextureIdLists.get(i).addAll(savedUnusedTextureIds.get(i));
            }

            for (FroggerWadSlotState state : states)
                state.restore(logger, file);
        }
    }

    private static byte[] serialize(SCGameFile<?> file, ILogger logger) {
        try {
            return file.writeDataToByteArray();
        } catch (Throwable th) {
            logger.severe("Failed to serialise '%s' for the fingerprint: %s", file.getFileDisplayName(), th);
            return ("SERIALISATION-FAILED " + th.getClass().getName()).getBytes(StandardCharsets.UTF_8);
        }
    }

    private Map<VloFile, String> hashImages(FroggerGameInstance instance, FroggerModNameResolver resolver) {
        Map<VloFile, String> bankHashes = new IdentityHashMap<>();
        for (VloFile bank : instance.getMainArchive().getAllFiles(VloFile.class)) {
            FroggerModHasher bankHasher = new FroggerModHasher();
            String bankName = resolver.bankNameOf(bank);
            if (bankName == null)
                bankName = bank.getFileDisplayName();

            List<VloImage> bankImages = bank.getImages();
            bankHasher.putInt(bankImages.size());
            for (int i = 0; i < bankImages.size(); i++) {
                VloImage image = bankImages.get(i);
                String imageName = resolver.imageNameOf(image);
                String key = bankName + ":" + (imageName != null ? imageName : "id" + image.getTextureId());
                key = FroggerModHasher.makeUniqueKey(this.images.keySet(), key, "@" + i);

                FroggerModHasher hasher = new FroggerModHasher();
                hasher.putInt(image.getPaddedWidth());
                hasher.putInt(image.getPaddedHeight());
                hasher.putInt(image.getInternalUnpaddedWidth());
                hasher.putInt(image.getInternalUnpaddedHeight());
                hasher.putInt(image.getFlags() & IMAGE_FLAG_MASK);
                hasher.putString(image.getBitDepth().name());
                int[] pixels = image.getPixelBuffer();
                hasher.putInt(pixels != null ? pixels.length : -1);
                if (pixels != null)
                    for (int pixel : pixels)
                        hasher.putInt(pixel);

                String imageHash = hasher.finish();
                this.images.put(key, imageHash);
                bankHasher.putInt(image.getTextureId());
                bankHasher.putString(key);
                bankHasher.putString(imageHash);
            }

            bankHashes.put(bank, bankHasher.finish());
        }

        return bankHashes;
    }

    private static void hashLevelSelect(FroggerModNameResolver resolver, List<FroggerLevelSelectEntry> entries, Map<String, String> output, List<String> order) {
        for (FroggerLevelSelectEntry entry : entries) {
            if (entry.isTerminator())
                continue;

            FroggerMapLevelID level = entry.getLevel();
            String key = FroggerModHasher.makeUniqueKey(output.keySet(), level != null ? resolver.levelCodeOf(level) : "level", "@" + output.size());
            order.add(key);

            String theme;
            try {
                theme = entry.getTheme() != null ? entry.getTheme().name() : "null";
            } catch (RuntimeException ex) {
                theme = "invalid";
            }

            StringBuilder builder = new StringBuilder();
            builder.append("level=").append(level != null ? level.name() : "null")
                    .append(";world=").append(entry.getWorld() != null ? entry.getWorld().name() : "null")
                    .append(";stackPosition=").append(entry.getStackPosition())
                    .append(";theme=").append(theme)
                    .append(";localLevelId=").append(entry.getLocalLevelId())
                    .append(";levelsInWorld=").append(entry.getLevelsInWorld())
                    .append(";worldImageSelectable=").append(FroggerModHasher.pointerName(resolver, entry.getWorldImageSelectablePointer()))
                    .append(";worldImageVisited=").append(FroggerModHasher.pointerName(resolver, entry.getWorldImageVisitedPointer()))
                    .append(";worldImageNotTried=").append(FroggerModHasher.pointerName(resolver, entry.getWorldImageNotTriedPointer()))
                    .append(";levelTexture=").append(FroggerModHasher.pointerName(resolver, entry.getLevelTexturePointer()))
                    .append(";levelNameTexture=").append(FroggerModHasher.pointerName(resolver, entry.getLevelNameTexturePointer()))
                    .append(";levelNameTextureInGame=").append(FroggerModHasher.pointerName(resolver, entry.getLevelNameTextureInGamePointer()));
            output.put(key, FroggerModHasher.sha1(builder.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void hashDemos(FroggerGameInstance instance, FroggerModNameResolver resolver) {
        List<FroggerDemoTableEntry> entries = instance.getDemoTable().getEntries();
        for (int i = 0; i < entries.size(); i++) {
            FroggerDemoTableEntry entry = entries.get(i);
            String text;
            if (entry.isSkipped()) {
                text = "skip";
            } else {
                text = "level=" + resolver.levelCodeOf(entry.getLevel()) + ";demo=" + resourceName(instance, entry.getDemoResourceId())
                        + ";unlock=" + resolver.levelCodeOf(entry.getUnlockLevel());
                if (entry instanceof FroggerPCDemoTableEntry) {
                    int lowPolyId = ((FroggerPCDemoTableEntry) entry).getLowPolyDemoResourceId();
                    text += ";lowPolyDemo=" + resourceName(instance, lowPolyId >= 0 ? lowPolyId : entry.getDemoResourceId());
                }
            }

            this.demos.put(String.valueOf(i), FroggerModHasher.sha1(text.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private static String resourceName(FroggerGameInstance instance, int resourceId) {
        MWIResourceEntry entry = resourceId >= 0 ? instance.getResourceEntryByID(resourceId) : null;
        return entry != null ? entry.getDisplayName() : "id" + resourceId;
    }

    private void hashThemes(FroggerGameInstance instance) {
        for (FroggerMapTheme theme : FroggerMapTheme.values()) {
            FroggerThemeBook book = instance.getThemeBook(theme);
            if (book == null)
                continue;

            String fields = book.execute(
                    pc -> "highWadId=" + pc.getHighWadId() + ";highVloId=" + pc.getHighVloId() + ";lowWadId=" + pc.getLowWadId() + ";lowVloId=" + pc.getLowVloId()
                            + ";highMultiplayerWadId=" + pc.getHighMultiplayerWadId() + ";highMultiplayerVloId=" + pc.getHighMultiplayerVloId()
                            + ";lowMultiplayerWadId=" + pc.getLowMultiplayerWadId() + ";lowMultiplayerVloId=" + pc.getLowMultiplayerVloId()
                            + ";formLibraryPointer=" + pc.getFormLibraryPointer() + ";deathHeight=" + pc.getDeathHeight(),
                    psx -> "wadId=" + psx.getWadId() + ";vloId=" + psx.getVloId() + ";multiplayerWadId=" + psx.getMultiplayerWadId()
                            + ";multiplayerVloId=" + psx.getMultiplayerVloId() + ";formLibraryPointer=" + psx.getFormLibraryPointer()
                            + ";deathHeight=" + psx.getDeathHeight());

            FroggerModHasher hasher = new FroggerModHasher();
            hasher.putString(fields);
            List<FroggerFormEntry> formBook = book.getFormBook();
            hasher.putInt(formBook.size());
            for (FroggerFormEntry formEntry : formBook)
                hasher.putBytes(formEntry.writeDataToByteArray());

            this.themes.put(theme.name(), hasher.finish());
        }
    }

    private void hashRemaps(FroggerGameInstance instance) {
        for (TextureRemapArray remap : instance.getTextureRemaps()) {
            String name = null;
            if (remap instanceof LinkedTextureRemap && ((LinkedTextureRemap<?>) remap).getResourceEntry() != null)
                name = FileUtils.stripExtension(((LinkedTextureRemap<?>) remap).getResourceEntry().getDisplayName());
            if (name == null || name.isEmpty())
                name = remap.getName();
            if (name == null || name.isEmpty())
                name = String.format("remap@0x%08X", remap.getReaderIndex());

            FroggerModHasher hasher = new FroggerModHasher();
            List<Short> textureIds = remap.getTextureIds();
            hasher.putInt(textureIds.size());
            for (Short textureId : textureIds)
                hasher.putInt(textureId != null ? textureId : Integer.MIN_VALUE);

            this.remaps.put(FroggerModHasher.makeUniqueKey(this.remaps.keySet(), name, String.format("@0x%08X", remap.getReaderIndex())), hasher.finish());
        }
    }

    private void hashExecutable(byte[] exeBytes, int blockLength) {
        int currentLength = this.exeLength;
        boolean[] owned = new boolean[blockLength];
        for (FroggerOwnedRange range : this.ownedRanges)
            FroggerModHasher.markRange(owned, range.getRange());

        boolean[] patched = new boolean[blockLength];
        for (List<PatchWriteRange> ranges : this.patchSiteRanges.values())
            for (PatchWriteRange range : ranges)
                FroggerModHasher.markRange(patched, range);

        for (int i = 0; i < this.ownedRanges.size(); i++) {
            FroggerOwnedRange range = this.ownedRanges.get(i);
            String hash = LABEL_MWI.equals(range.getLabel()) ? UNCOMPARED_HASH : hashBytes(exeBytes, currentLength, range.getRange());
            this.ownedRanges.set(i, new FroggerOwnedRange(range.getRange(), range.getLabel(), hash));
        }

        for (int blockStart = 0; blockStart < blockLength; blockStart += EXE_BLOCK_SIZE) {
            int blockEnd = Math.min(blockStart + EXE_BLOCK_SIZE, blockLength);
            FroggerModHasher fullHasher = new FroggerModHasher();
            FroggerModHasher unpatchedHasher = new FroggerModHasher();
            int ownedBytes = 0;
            boolean anyPatched = false;
            for (int i = blockStart; i < blockEnd; i++) {
                if (owned[i]) {
                    ownedBytes++;
                    continue;
                }

                int value = i < currentLength ? (exeBytes[i] & 0xFF) : -1;
                fullHasher.putInt(value);
                if (patched[i]) {
                    anyPatched = true;
                } else {
                    unpatchedHasher.putInt(value);
                }
            }

            if (ownedBytes == blockEnd - blockStart)
                continue;

            String key = FroggerModHasher.formatRange(new PatchWriteRange(blockStart, blockEnd - blockStart));
            this.exeBlocks.put(key, fullHasher.finish());
            if (anyPatched)
                this.exeBlocks.put(key + UNPATCHED_SUFFIX, unpatchedHasher.finish());
        }

        for (Map.Entry<String, List<PatchWriteRange>> entry : this.patchSiteRanges.entrySet()) {
            FroggerModHasher hasher = new FroggerModHasher();
            for (PatchWriteRange range : entry.getValue())
                for (int i = range.getAddress(); i < range.getEndAddress(); i++)
                    hasher.putInt(i >= 0 && i < currentLength ? (exeBytes[i] & 0xFF) : -1);
            this.patchSiteHashes.put(entry.getKey(), hasher.finish());
        }
    }

    private static String hashBytes(byte[] exeBytes, int currentLength, PatchWriteRange range) {
        FroggerModHasher hasher = new FroggerModHasher();
        for (int i = range.getAddress(); i < range.getEndAddress(); i++)
            hasher.putInt(i >= 0 && i < currentLength ? (exeBytes[i] & 0xFF) : -1);
        return hasher.finish();
    }

    private static int getEffectiveExecutableLength(FroggerGameInstance instance, byte[] exeBytes) {
        byte[] signature = FileUtils.FROGLORD_EXECUTABLE_SIGNATURE_BYTES;
        int signatureStart = exeBytes.length - signature.length;
        if (instance.isPC() && signatureStart >= Constants.INTEGER_SIZE && DataUtils.testSignature(exeBytes, signatureStart, signature)) {
            int originalLength = DataUtils.readIntFromBytes(exeBytes, signatureStart - Constants.INTEGER_SIZE);
            if (originalLength > 0 && originalLength <= exeBytes.length)
                return originalLength;
        }

        return exeBytes.length;
    }

    private static List<FroggerOwnedRange> recordOwnedRanges(FroggerGameInstance instance, int exeLength) {
        FroggerConfig config = instance.getVersionConfig();
        Map<String, List<PatchWriteRange>> parts = new LinkedHashMap<>();
        parts.put(LABEL_MWI, recordWrites(writer -> instance.writeMWI(writer, instance.getArchiveIndex())));
        parts.put("bmp_pointers", recordWrites(writer -> {
            if (config.getBmpPointerAddress() <= 0 || instance.getBmpTexturePointers().isEmpty())
                return;
            writer.setIndex(config.getBmpPointerAddress());
            instance.getBmpTexturePointers().forEach(writer::writeUnsignedInt);
        }));
        parts.put("texture remaps", recordWrites(instance::writeTextureRemaps));
        parts.put("theme and form library", recordWrites(writer -> {
            if (config.getThemeBookAddress() <= 0)
                return;
            writer.setIndex(config.getThemeBookAddress());
            for (FroggerThemeBook book : instance.getThemeLibrary())
                book.save(writer);
        }));
        parts.put("map library", recordWrites(writer -> {
            if (config.getMapBookAddress() <= 0)
                return;
            writer.setIndex(config.getMapBookAddress());
            for (FroggerMapBook book : instance.getMapLibrary())
                book.save(writer);
        }));
        parts.put("demo tables", recordWrites(writer -> instance.getDemoTable().save(writer)));
        parts.put("level select", recordWrites(writer -> {
            if (config.getArcadeLevelAddress() <= 0)
                return;
            writer.setIndex(config.getArcadeLevelAddress());
            instance.getArcadeLevelSelectEntries().forEach(entry -> entry.save(writer));
            instance.getRaceLevelSelectEntries().forEach(entry -> entry.save(writer));
        }));

        List<FroggerOwnedRange> results = new ArrayList<>();
        boolean[] labelled = new boolean[exeLength];
        for (Map.Entry<String, List<PatchWriteRange>> entry : parts.entrySet()) {
            for (PatchWriteRange range : entry.getValue()) {
                results.add(new FroggerOwnedRange(range, entry.getKey(), UNCOMPARED_HASH));
                FroggerModHasher.markRange(labelled, range);
            }
        }

        for (PatchWriteRange range : recordWrites(writer -> instance.writeExecutableData(writer, instance.getArchiveIndex()))) {
            int start = -1;
            for (int i = Math.max(0, range.getAddress()); i <= Math.min(exeLength, range.getEndAddress()); i++) {
                boolean free = i < Math.min(exeLength, range.getEndAddress()) && !labelled[i];
                if (free && start < 0) {
                    start = i;
                } else if (!free && start >= 0) {
                    results.add(new FroggerOwnedRange(new PatchWriteRange(start, i - start), LABEL_REMAINDER, UNCOMPARED_HASH));
                    start = -1;
                }
            }
        }

        if (instance.isPSX())
            results.add(new FroggerOwnedRange(new PatchWriteRange(Constants.PSX_WRITABLE_START_OFFSET, Constants.PSX_WRITABLE_AREA_SIZE), LABEL_PSX_TRAILER, UNCOMPARED_HASH));

        results.sort(Comparator.comparingInt(owned -> owned.getRange().getAddress()));
        return results;
    }

    private static List<PatchWriteRange> recordWrites(Consumer<DataWriter> writeAction) {
        PatchWriteRecorder recorder = new PatchWriteRecorder();
        writeAction.accept(new DataWriter(recorder));
        return recorder.getRanges();
    }

    private static Map<String, List<PatchWriteRange>> recordPatchSites(FroggerGameInstance instance, ILogger logger) {
        Map<String, List<PatchWriteRange>> results = new LinkedHashMap<>();
        for (GamePatch patch : FroggerPatchLibrary.getPatchesForVersion(instance.getVersionConfig().getInternalName())) {
            try {
                results.put(patch.getId(), PatchWriteRecorder.recordPatchWrites(instance, loadFreshPatch(patch)));
            } catch (Throwable th) {
                logger.warning("Could not record the sites of the bundled patch '%s': %s", patch.getId(), th);
            }
        }

        return results;
    }

    private static GamePatch loadFreshPatch(GamePatch patch) throws IOException {
        URL url = SCGameType.FROGGER.getEmbeddedResourceURL(PATCH_FOLDER + "/" + patch.getId() + PATCH_EXTENSION);
        if (url == null)
            return patch;

        try (InputStream inputStream = url.openStream()) {
            GamePatch freshPatch = new GamePatch();
            freshPatch.loadPatchFromConfig(patch.getId(), new net.highwayfrogs.editor.file.config.Config(inputStream));
            return freshPatch;
        }
    }

    private static boolean isOrderChanged(List<String> baseOrder, List<String> currentOrder) {
        List<String> baseCommon = new ArrayList<>(baseOrder);
        baseCommon.retainAll(currentOrder);
        List<String> currentCommon = new ArrayList<>(currentOrder);
        currentCommon.retainAll(baseOrder);
        return !baseCommon.equals(currentCommon);
    }

    private static void compareSection(Map<String, String> base, Map<String, String> current, FroggerBaseSectionDifference output) {
        for (Map.Entry<String, String> entry : base.entrySet()) {
            String currentHash = current.get(entry.getKey());
            if (currentHash == null) {
                output.getRemoved().add(entry.getKey());
            } else if (!currentHash.equals(entry.getValue())) {
                output.getChanged().add(entry.getKey());
            }
        }

        for (String key : current.keySet())
            if (!base.containsKey(key))
                output.getAdded().add(key);
    }

    private static Config writeHashSection(String sectionName, Map<String, String> hashes) {
        Config section = new Config(sectionName);
        for (Map.Entry<String, String> entry : hashes.entrySet())
            section.getInternalText().add(new Config.ConfigValueNode(entry.getValue() + " " + entry.getKey()));
        return section;
    }

    private static List<String> getSectionLines(Config config, String sectionName) {
        Config section = config.getChildConfigByName(sectionName);
        return section != null ? section.getTextWithoutComments() : Collections.<String>emptyList();
    }

    private static void readHashSection(Config config, String sectionName, Map<String, String> output) {
        for (String line : getSectionLines(config, sectionName)) {
            String trimmed = line.trim();
            int split = trimmed.indexOf(' ');
            if (split <= 0)
                throw new IllegalArgumentException("Invalid [" + sectionName + "] line: '" + line + "'.");
            output.put(trimmed.substring(split + 1).trim(), trimmed.substring(0, split));
        }
    }

    private static void readOrder(Config config, String sectionName, List<String> output) {
        Config section = config.getChildConfigByName(sectionName);
        if (section == null || !section.hasKeyValueNode(KEY_ORDER))
            return;

        String order = section.getKeyValueNodeOrError(KEY_ORDER).getAsString();
        if (order != null)
            for (String code : order.split(","))
                if (!code.trim().isEmpty())
                    output.add(code.trim());
    }

    private static PatchWriteRange parseRange(String text) {
        String trimmed = text.trim();
        int suffix = trimmed.indexOf('/');
        if (suffix >= 0)
            trimmed = trimmed.substring(0, suffix);

        int plus = trimmed.indexOf('+');
        if (plus <= 0 || !trimmed.startsWith("0x"))
            throw new IllegalArgumentException("Invalid executable range: '" + text + "'.");
        return new PatchWriteRange(Integer.parseInt(trimmed.substring(2, plus), 16), Integer.parseInt(trimmed.substring(plus + 1)));
    }

    /**
     * An executable region FrogLord writes on save, with what writes it and the hash of its bytes ("-" when not compared).
     */
    @Getter
    public static class FroggerOwnedRange {
        private final PatchWriteRange range;
        private final String label;
        private final String hash;

        public FroggerOwnedRange(@NonNull PatchWriteRange range, @NonNull String label, @NonNull String hash) {
            this.range = range;
            this.label = label;
            this.hash = hash;
        }
    }

    private static class FroggerWadSlotState {
        private final WADEntry entry;
        private final SCGameFile<?> file;
        private final int resourceId;
        private final MRStaticMof staticMof;
        private final MRAnimatedMof animatedMof;
        private final boolean incomplete;

        private FroggerWadSlotState(WADEntry entry) {
            this.entry = entry;
            this.file = entry.getFile();
            this.resourceId = entry.getResourceId();
            MRModel model = this.file instanceof MRModel ? (MRModel) this.file : null;
            this.staticMof = model != null ? model.getStaticMof() : null;
            this.animatedMof = model != null ? model.getAnimatedMof() : null;
            this.incomplete = model != null && model.isIncomplete();
        }

        private void restore(ILogger logger, SCGameFile<?> savedFile) {
            if (this.entry.getFile() != this.file || this.entry.getResourceId() != this.resourceId) {
                logger.warning("Serialising '%s' replaced a model in '%s'; the slot was put back.", savedFile.getFileDisplayName(), this.entry.getWadFile().getFileDisplayName());
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
