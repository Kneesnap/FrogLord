package net.highwayfrogs.editor.games.sony.frogger.mods;

import lombok.Getter;
import lombok.NonNull;
import net.highwayfrogs.editor.file.patch.GamePatch;
import net.highwayfrogs.editor.file.patch.PatchArgument;
import net.highwayfrogs.editor.file.patch.PatchArgumentType;
import net.highwayfrogs.editor.file.patch.PatchRuntime;
import net.highwayfrogs.editor.file.patch.PatchValue;
import net.highwayfrogs.editor.games.psx.image.PsxImageBitDepth;
import net.highwayfrogs.editor.games.shared.mods.ModFormatConstants;
import net.highwayfrogs.editor.games.shared.mods.ModInstallResult;
import net.highwayfrogs.editor.games.shared.mods.ModManifest;
import net.highwayfrogs.editor.games.shared.mods.ModManifest.ModTarget;
import net.highwayfrogs.editor.games.shared.mods.ModPackage;
import net.highwayfrogs.editor.games.shared.mods.ModProblem;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectEntry;
import net.highwayfrogs.editor.games.sony.frogger.data.FroggerLevelSelectWorldID;
import net.highwayfrogs.editor.games.sony.frogger.data.demo.FroggerDemoFile;
import net.highwayfrogs.editor.games.sony.frogger.data.map.FroggerMapBook;
import net.highwayfrogs.editor.games.sony.frogger.data.theme.FroggerThemeBook;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapLevelID;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapTheme;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.FroggerMapEntity;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry;
import net.highwayfrogs.editor.games.sony.frogger.map.data.form.FroggerFormEntry.FroggerFormLibFlag;
import net.highwayfrogs.editor.games.sony.frogger.utils.FroggerUtils;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.Utils.ProblemResponse;
import net.highwayfrogs.editor.utils.logging.ClassNameLogger;
import net.highwayfrogs.editor.utils.logging.ILogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Installs a Frogger mod package onto the open game in memory: target selection, the base check, name resolution as a dry run, then the changes in a fixed order.
 * Nothing is saved; the caller saves the game with FrogLord's normal save afterwards. Messages go to the logger and into the returned result.
 * Created by jimbolina on 11/6/2026.
 */
public class FroggerModInstaller {
    @Getter private final FroggerGameInstance gameInstance;
    @Getter private final ModPackage modPackage;
    private final ILogger logger;
    private final FroggerModNameResolver resolver;
    private ModInstallResult result;
    private FroggerModManifest manifest;
    private String targetName;
    private final List<ImagePlan> imagePlans = new ArrayList<>();
    private final List<MapPlan> mapPlans = new ArrayList<>();
    private final List<TimeLimitPlan> timeLimitPlans = new ArrayList<>();
    private final List<ThemePlan> themePlans = new ArrayList<>();
    private final List<LevelPlan> levelPlans = new ArrayList<>();
    private final List<DemoFilePlan> demoFilePlans = new ArrayList<>();
    private final List<DemoRowPlan> demoRowPlans = new ArrayList<>();
    private final List<PatchPlan> patchPlans = new ArrayList<>();
    private List<FroggerLevelSelectEntry> arcadePlan;
    private List<FroggerLevelSelectEntry> racePlan;

    private static final float LEVEL_NAME_FONT_SIZE = 16F;
    private static final int MAX_TIME_LIMIT = 0xFFFF;

    public FroggerModInstaller(@NonNull FroggerGameInstance gameInstance, @NonNull ModPackage modPackage, ILogger logger) {
        this.gameInstance = gameInstance;
        this.modPackage = modPackage;
        this.logger = logger != null ? logger : ClassNameLogger.getLogger(gameInstance, FroggerModInstaller.class);
        this.resolver = new FroggerModNameResolver(gameInstance);
    }

    /**
     * Installs the package onto the open game. The base build is not checked against a fingerprint, which the result records as a warning.
     * @param allowModifiedBase whether to continue when the open game is known to differ from the base build
     * @return the install result
     */
    public ModInstallResult install(boolean allowModifiedBase) {
        return install(allowModifiedBase, null);
    }

    /**
     * Installs the package onto the open game.
     * @param allowModifiedBase whether to continue when the open game differs from the base build
     * @param baseDifferences the differences between the open game and the base build which the caller found by comparing against a
     *                        base fingerprint, empty when it matched, or null when no fingerprint was checked
     * @return the install result
     */
    public ModInstallResult install(boolean allowModifiedBase, List<String> baseDifferences) {
        this.result = new ModInstallResult(this.modPackage.getManifest().getName());
        clearPlans();

        if (!selectTarget() || !checkBase(allowModifiedBase, baseDifferences))
            return finish();

        resolveImages();
        resolveMaps();
        resolveModels();
        resolveThemes();
        resolveLevelSelect();
        resolveDemos();
        resolvePatches();
        if (!this.result.isSuccess()) {
            error(null, null, "Name resolution failed, so nothing was changed.");
            return finish();
        }

        try {
            applyImages();
            checkTexturePlacement();
            applyMaps();
            applyLevelWads();
            applyThemes();
            applyLevelSelect();
            applyDemos();
            applyPatches();
        } catch (Throwable th) {
            error(null, null, "The install stopped: " + describeThrowable(th));
        }

        return finish();
    }

    private ModInstallResult finish() {
        if (this.result.isSuccess()) {
            this.logger.info("Installed '%s' for %s with %d operation(s) and %d warning(s). Save to finish: FrogLord writes the modded_ pair.",
                    this.result.getPackageName(), this.targetName, this.result.getAppliedOperations().size(), this.result.getWarnings().size());
        } else {
            this.logger.severe("Failed to install '%s' (%d error(s)).", this.result.getPackageName(), this.result.getErrors().size());
        }

        return this.result;
    }

    private void clearPlans() {
        this.manifest = null;
        this.targetName = null;
        this.imagePlans.clear();
        this.mapPlans.clear();
        this.timeLimitPlans.clear();
        this.themePlans.clear();
        this.levelPlans.clear();
        this.demoFilePlans.clear();
        this.demoRowPlans.clear();
        this.patchPlans.clear();
        this.arcadePlan = null;
        this.racePlan = null;
    }

    private boolean selectTarget() {
        for (ModProblem problem : this.modPackage.getProblems())
            record(problem);
        if (this.modPackage.hasErrors()) {
            error(null, null, "The package has errors, so it cannot be installed.");
            return false;
        }

        ModManifest packageManifest = this.modPackage.getManifest();
        if (!FroggerModManifest.GAME_IDENTIFIER.equals(packageManifest.getGame())) {
            error(ModFormatConstants.SECTION_MOD, ModFormatConstants.KEY_GAME, "The package is for '" + packageManifest.getGame() + "', not '" + FroggerModManifest.GAME_IDENTIFIER + "'.");
            return false;
        }

        Integer formatVersion = packageManifest.getFormatVersion();
        if (formatVersion == null || formatVersion > ModFormatConstants.FORMAT_VERSION) {
            error(ModFormatConstants.SECTION_MOD, ModFormatConstants.KEY_FORMAT_VERSION, "Format version " + formatVersion + " is not supported (at most " + ModFormatConstants.FORMAT_VERSION + ").");
            return false;
        }

        String buildName = this.gameInstance.getVersionConfig().getInternalName();
        ModTarget target = packageManifest.findTarget(buildName);
        if (target == null) {
            List<String> targetNames = new ArrayList<>();
            for (ModTarget packageTarget : packageManifest.getTargets())
                targetNames.add(packageTarget.getName());

            StringBuilder message = new StringBuilder("This package targets ").append(targetNames.isEmpty() ? "no build" : String.join(", ", targetNames))
                    .append(", not the open build '").append(buildName).append("'.");
            ModTarget checksumTarget = packageManifest.findTargetByChecksum(DataUtils.getCRC32(this.gameInstance.getExecutableBytes()));
            if (checksumTarget != null)
                message.append(" The executable's checksum matches target '").append(checksumTarget.getName()).append("'; check the detected version.");
            error(ModFormatConstants.SECTION_TARGETS, null, message.toString());
            return false;
        }

        this.targetName = buildName;
        this.result.setTargetName(buildName);
        this.manifest = new FroggerModManifest(packageManifest, buildName);
        for (ModProblem problem : this.manifest.getProblems())
            record(problem);
        if (ModProblem.hasErrors(this.manifest.getProblems()))
            return false;

        applied("Selected target " + target.getName() + (target.getName().equals(buildName) ? "" : " through its alias " + buildName) + ".");
        return true;
    }

    private boolean checkBase(boolean allowModifiedBase, List<String> baseDifferences) {
        if (baseDifferences == null) {
            warning(null, null, "Base not checked: no base fingerprint was supplied, so changes already present in the open game are not detected.");
        } else if (!baseDifferences.isEmpty()) {
            String message = "The open game already differs from the base build in: " + String.join("; ", baseDifferences) + ".";
            if (!allowModifiedBase) {
                error(null, null, message + " Install onto an unmodified copy, or allow a modified base.");
                return false;
            }

            warning(null, null, message + " The result is the union of both, and there is no undo.");
        }

        if (this.gameInstance.isPreviouslySavedByFrogLord())
            warning(null, null, "The open executable was saved by FrogLord before, so it may already hold other changes.");

        return true;
    }

    private void resolveImages() {
        for (FroggerModImageEntry entry : this.manifest.getImageEntries()) {
            String key = imageKey(entry.getBank(), entry.getName());
            try {
                this.imagePlans.add(planImage(key, entry));
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_IMAGES, null, ex.getMessage());
            }
        }
    }

    private ImagePlan planImage(String key, FroggerModImageEntry entry) {
        VloImage image = this.resolver.resolveImage(key, entry.getBank(), entry.getName());
        ImagePlan plan = new ImagePlan(entry, image);
        plan.translucent = entry.getTranslucent() != null ? entry.getTranslucent() : image.testFlag(VloImage.FLAG_TRANSLUCENT);
        plan.bitDepth = image.getBitDepth();
        if (entry.getBitDepth() != null)
            plan.bitDepth = getBitDepth(key, entry.getBitDepth());

        if (entry.getFile() == null) {
            if (entry.getInGameWidth() != null || entry.getInGameHeight() != null || entry.getBitDepth() != null)
                throw fail(key, "inGameWidth, inGameHeight and bitDepth need a file");
            return plan;
        }

        BufferedImage packaged = readImage(key, entry.getFile());
        int fullWidth = packaged.getWidth();
        int fullHeight = packaged.getHeight();
        if (fullWidth > VloImage.MAX_IMAGE_DIMENSION || fullHeight > VloImage.MAX_IMAGE_DIMENSION)
            throw fail(key, "%s is %dx%d, larger than %dx%d", entry.getFile(), fullWidth, fullHeight, VloImage.MAX_IMAGE_DIMENSION, VloImage.MAX_IMAGE_DIMENSION);

        int currentPadding = image.getHeight() - image.getInternalUnpaddedHeight();
        int inGameWidth = entry.getInGameWidth() != null ? entry.getInGameWidth() : fullWidth - currentPadding;
        int inGameHeight = entry.getInGameHeight() != null ? entry.getInGameHeight() : fullHeight - currentPadding;
        if (inGameWidth < 1 || inGameHeight < 1 || inGameWidth > fullWidth || inGameHeight > fullHeight)
            throw fail(key, "the in-game size %dx%d does not fit the %dx%d image in %s", inGameWidth, inGameHeight, fullWidth, fullHeight, entry.getFile());

        int padding = fullHeight - inGameHeight;
        int leftPadding;
        if (this.gameInstance.isPSX()) {
            leftPadding = (fullHeight != inGameHeight && fullHeight != inGameHeight + 1) ? 1 : 0;
        } else {
            if (fullWidth - inGameWidth != padding)
                throw fail(key, "PC images pad both axes alike, but %s is %dx%d with an in-game size of %dx%d", entry.getFile(), fullWidth, fullHeight, inGameWidth, inGameHeight);
            leftPadding = padding / 2;
        }

        if (leftPadding + inGameWidth > fullWidth)
            throw fail(key, "the in-game size %dx%d does not fit the %dx%d image in %s", inGameWidth, inGameHeight, fullWidth, fullHeight, entry.getFile());

        int[] pixels = packaged.getRGB(leftPadding, padding / 2, inGameWidth, inGameHeight, null, 0, inGameWidth);
        BufferedImage inGameImage = new BufferedImage(inGameWidth, inGameHeight, BufferedImage.TYPE_INT_ARGB);
        inGameImage.setRGB(0, 0, inGameWidth, inGameHeight, pixels, 0, inGameWidth);
        plan.inGameImage = inGameImage;
        plan.padding = padding;
        plan.fullWidth = fullWidth;
        plan.fullHeight = fullHeight;
        return plan;
    }

    private void resolveMaps() {
        for (FroggerModMapEntry entry : this.manifest.getMapEntries()) {
            String key = mapKey(entry.getLevelCode());
            try {
                this.mapPlans.add(planMap(key, entry));
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_MAPS, null, ex.getMessage());
            }
        }
    }

    private MapPlan planMap(String key, FroggerModMapEntry entry) {
        FroggerMapLevelID level = this.resolver.resolveLevel(key, entry.getLevelCode());
        FroggerMapFile map = this.resolver.resolveMapFile(key, level);
        MapPlan plan = new MapPlan(entry, map);
        if (entry.getFile() != null)
            plan.mapBytes = getPackagedFile(key + " file", entry.getFile());

        if (entry.getRemap() != null) {
            List<String> names = parseRemapFile(getPackagedFile(key + " remap", entry.getRemap()));
            plan.remapIds = this.resolver.resolveRemapNames(key + " remap", map, names);
        }

        if (entry.getLowPolyFile() != null) {
            plan.lowPolyMap = this.resolver.resolveLowPolyMapFile(key + " lowPolyFile", level);
            plan.lowPolyBytes = getPackagedFile(key + " lowPolyFile", entry.getLowPolyFile());
        }

        if (entry.getModels() == null)
            return plan;

        FroggerMapBook mapBook = this.resolver.getMapBook(level);
        if (mapBook == null || !mapBook.hasPerLevelWadFiles()) {
            warning(FroggerModManifest.SECTION_MAPS, null, key + " models: " + this.targetName + " has no per-level WAD files, so the list is ignored.");
            return plan;
        }

        plan.levelWad = this.resolver.resolveLevelWad(key, map);
        Set<String> seenNames = new HashSet<>();
        for (String modelName : entry.getModels()) {
            String modelKey = key + " models=" + modelName;
            if (!seenNames.add(modelName.toUpperCase(Locale.ROOT)))
                throw fail(modelKey, "the model is listed twice");

            FroggerModModelEntry modelEntry = this.manifest.getModelEntry(modelName);
            boolean packaged = modelEntry != null && modelEntry.getFile() != null;
            byte[] packagedBytes = packaged ? getPackagedFile(modelKey(modelName) + " file", modelEntry.getFile()) : null;

            List<FroggerModWadSlot> heldSlots = new ArrayList<>();
            for (FroggerModWadSlot slot : this.resolver.findModelSlots(modelName))
                if (slot.getWad() == plan.levelWad)
                    heldSlots.add(slot);

            ModelPlan modelPlan;
            if (!heldSlots.isEmpty()) {
                modelPlan = new ModelPlan(modelName, heldSlots, packagedBytes, packaged, true);
            } else {
                FroggerModWadSlot source = this.resolver.resolveSourceSlotForModel(modelKey, plan.levelWad, modelName);
                byte[] modelBytes = packaged ? packagedBytes : source.getEntry().getFile().writeDataToByteArray();
                modelPlan = new ModelPlan(modelName, Collections.singletonList(source), modelBytes, packaged, false);
            }

            for (FroggerModWadSlot slot : modelPlan.slots)
                for (ModelPlan other : plan.models)
                    for (FroggerModWadSlot otherSlot : other.slots)
                        if (otherSlot.getIndex() == slot.getIndex())
                            throw fail(modelKey, "slot %d of %s is already taken by %s", slot.getIndex(), this.resolver.wadNameOf(plan.levelWad), other.modelName);

            plan.models.add(modelPlan);
        }

        return plan;
    }

    private void resolveModels() {
        for (FroggerModModelEntry entry : this.manifest.getModelEntries()) {
            String key = modelKey(entry.getModelName());
            try {
                this.resolver.resolveModelEntry(key, entry.getModelName());
                if (entry.getFile() == null)
                    throw fail(key, "file is required");
                getPackagedFile(key + " file", entry.getFile());
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_MODELS, null, ex.getMessage());
                continue;
            }

            boolean used = false;
            for (MapPlan mapPlan : this.mapPlans)
                for (ModelPlan modelPlan : mapPlan.models)
                    if (modelPlan.modelName.equalsIgnoreCase(entry.getModelName()))
                        used = true;

            if (!used)
                warning(FroggerModManifest.SECTION_MODELS, null, key + ": no [Maps] models= list names this model, so its file is not used.");
        }
    }

    private void resolveThemes() {
        for (FroggerModThemeEntry entry : this.manifest.getThemeEntries()) {
            String key = "[Themes] [[" + entry.getTheme() + "]]";
            try {
                FroggerMapTheme theme = this.resolver.resolveTheme(key, entry.getTheme());
                FroggerThemeBook themeBook = this.gameInstance.getThemeBook(theme);
                if (themeBook == null)
                    throw fail(key, "%s has no theme book for %s", this.targetName, theme.name());

                if (entry.getDeathHeight() == null)
                    continue;

                double fixedPoint = entry.getDeathHeight() * 16D;
                if (Double.isNaN(fixedPoint) || fixedPoint < Short.MIN_VALUE || fixedPoint > Short.MAX_VALUE)
                    throw fail(key + " deathHeight", "%s does not fit the 4-bit fixed-point short", entry.getDeathHeight());

                this.themePlans.add(new ThemePlan(theme, themeBook, DataUtils.floatToFixedPointShort4Bit(entry.getDeathHeight().floatValue())));
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_THEMES, null, ex.getMessage());
            }
        }
    }

    private void resolveLevelSelect() {
        if (this.manifest.getArcadeOrder() != null)
            this.arcadePlan = planLevelOrder(FroggerModManifest.KEY_ARCADE, this.manifest.getArcadeOrder(), this.gameInstance.getArcadeLevelSelectEntries());
        if (this.manifest.getRaceOrder() != null)
            this.racePlan = planLevelOrder(FroggerModManifest.KEY_RACE, this.manifest.getRaceOrder(), this.gameInstance.getRaceLevelSelectEntries());

        for (FroggerModLevelEntry entry : this.manifest.getLevelEntries()) {
            String key = levelKey(entry.getLevelCode());
            try {
                planLevel(key, entry);
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_LEVEL_SELECT, null, ex.getMessage());
            }
        }
    }

    private List<FroggerLevelSelectEntry> planLevelOrder(String listKey, List<String> levelCodes, List<FroggerLevelSelectEntry> baseRows) {
        String key = "[LevelSelect] " + listKey + "=";
        FroggerLevelSelectEntry terminator = null;
        for (FroggerLevelSelectEntry row : baseRows)
            if (row.isTerminator())
                terminator = row;

        if (terminator == null) {
            error(FroggerModManifest.SECTION_LEVEL_SELECT, null, key + ": the " + listKey + " table of " + this.targetName + " has no terminator row");
            return null;
        }

        List<FroggerLevelSelectEntry> rows = new ArrayList<>();
        boolean failed = false;
        for (String levelCode : levelCodes) {
            try {
                FroggerMapLevelID level = this.resolver.resolveLevel(key + levelCode, levelCode);
                FroggerLevelSelectEntry row = findRow(baseRows, level);
                if (row == null)
                    throw fail(key + levelCode, "the %s table of %s has no row for %s, and a package cannot add rows", listKey, this.targetName, levelCode);
                if (rows.contains(row))
                    throw fail(key + levelCode, "the level is listed twice");
                rows.add(row);
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_LEVEL_SELECT, null, ex.getMessage());
                failed = true;
            }
        }

        if (failed)
            return null;

        rows.add(terminator);
        return rows;
    }

    private void planLevel(String key, FroggerModLevelEntry entry) {
        FroggerMapLevelID level = this.resolver.resolveLevel(key, entry.getLevelCode());
        FroggerLevelSelectEntry row = findRow(this.gameInstance.getArcadeLevelSelectEntries(), level);
        if (row == null)
            row = findRow(this.gameInstance.getRaceLevelSelectEntries(), level);
        if (row == null)
            throw fail(key, "%s has no level-select row for %s, and a package cannot add rows", this.targetName, entry.getLevelCode());

        LevelPlan plan = new LevelPlan(entry, row);
        if (entry.getWorld() != null)
            plan.world = this.resolver.resolveWorld(key + " world", entry.getWorld());
        if (entry.getNameImage() != null)
            plan.namePointer = this.resolver.resolvePicturePointer(key + " nameImage", entry.getNameImage());
        if (entry.getPicture() != null)
            plan.picturePointer = this.resolver.resolvePicturePointer(key + " picture", entry.getPicture());

        if (entry.getTimeLimit() != null) {
            if (entry.getTimeLimit() < 0 || entry.getTimeLimit() > MAX_TIME_LIMIT)
                throw fail(key + " timeLimit", "%d is outside 0..%d", entry.getTimeLimit(), MAX_TIME_LIMIT);
            this.resolver.resolveMapFile(key + " timeLimit", level);
            this.timeLimitPlans.add(new TimeLimitPlan(level, entry.getTimeLimit()));
        }

        if (entry.getName() != null) {
            String nameImage = entry.getNameImage() != null ? entry.getNameImage() : this.resolver.imageNameForPointer(row.getLevelNameTexturePointer());
            if (nameImage == null)
                throw fail(key + " name", "the row's name image has no name, so the text cannot be rendered into it; give nameImage");

            if (!isImagePackaged(nameImage)) {
                short textureId = this.resolver.resolveTextureId(key + " name", nameImage);
                List<VloImage> images = this.gameInstance.getMainArchive().getImagesByTextureId(textureId);
                if (images == null || images.isEmpty())
                    throw fail(key + " name", "no bank holds the name image '%s'", nameImage);

                try {
                    FroggerUtils.getFroggerFont(ProblemResponse.THROW_EXCEPTION);
                } catch (RuntimeException ex) {
                    throw fail(key + " name", "the level-name font cannot be loaded (%s)", describeThrowable(ex));
                }

                plan.renderedNameImage = nameImage;
                plan.renderTargets = new ArrayList<>(images);
            }
        }

        this.levelPlans.add(plan);
    }

    private void resolveDemos() {
        List<String> demoFiles = this.manifest.getDemoFiles();
        if (demoFiles != null) {
            for (String demoName : demoFiles) {
                String key = "[Demos] files=" + demoName;
                try {
                    FroggerDemoFile demoFile = this.resolver.resolveDemoFile(key, demoName);
                    byte[] demoBytes = getPackagedFile(key, ModFormatConstants.FOLDER_DEMOS + demoName);
                    this.demoFilePlans.add(new DemoFilePlan(demoName, demoFile, demoBytes));
                } catch (IllegalArgumentException ex) {
                    error(FroggerModManifest.SECTION_DEMOS, null, ex.getMessage());
                }
            }
        }

        int tableSize = this.gameInstance.getDemoTable().size();
        for (FroggerModDemoRow row : this.manifest.getDemoRows()) {
            String key = "[Demos] [[Table]] " + row.getIndex();
            try {
                if (row.getIndex() >= tableSize)
                    throw fail(key, "the demo table of %s has %d rows, and its length is fixed", this.targetName, tableSize);

                DemoRowPlan plan = new DemoRowPlan(row);
                if (!row.isSkipped()) {
                    plan.level = this.resolver.resolveLevel(key + " level", row.getLevelCode());
                    this.resolver.resolveDemoFile(key + " demo", row.getDemoName());
                    plan.unlockLevel = this.resolver.resolveLevel(key + " unlock", row.getUnlockCode());
                }

                this.demoRowPlans.add(plan);
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_DEMOS, null, ex.getMessage());
            }
        }
    }

    private void resolvePatches() {
        List<String> gameplayProblems = FroggerModGameplay.validate(this.manifest.getInitialLevels(), this.manifest.getVisibleLevels(),
                this.manifest.getJungleSwap(), this.manifest.getFrameRate());
        for (String problem : gameplayProblems)
            error(FroggerModManifest.SECTION_GAMEPLAY, null, "[Gameplay]: " + problem);

        if (gameplayProblems.isEmpty()) {
            Map<String, Map<String, String>> gameplayPatches = FroggerModGameplay.toPatchArguments(this.manifest.getInitialLevels(),
                    this.manifest.getVisibleLevels(), this.manifest.getJungleSwap(), this.manifest.getFrameRate());
            for (Map.Entry<String, Map<String, String>> gameplayPatch : gameplayPatches.entrySet()) {
                String key = "[Gameplay] " + getGameplayKey(gameplayPatch.getKey());
                try {
                    GamePatch patch = FroggerPatchLibrary.getPatchById(gameplayPatch.getKey());
                    if (patch == null)
                        throw fail(key, "FrogLord has no bundled patch '%s'", gameplayPatch.getKey());
                    this.patchPlans.add(planPatch(key, patch, gameplayPatch.getValue()));
                } catch (IllegalArgumentException ex) {
                    error(FroggerModManifest.SECTION_GAMEPLAY, null, ex.getMessage());
                }
            }
        }

        for (FroggerModPatchRef patchRef : this.manifest.getPatches()) {
            String key = "[Patches] [[" + patchRef.getId() + "]]";
            try {
                GamePatch patch;
                if (patchRef.isPackaged()) {
                    patch = loadPackagedPatch(key, patchRef);
                } else {
                    patch = FroggerPatchLibrary.getPatchById(patchRef.getId());
                    if (patch == null)
                        throw fail(key, "FrogLord has no bundled patch '%s'; ship it with file=", patchRef.getId());
                }

                for (PatchPlan other : this.patchPlans)
                    if (other.patch == patch)
                        warning(FroggerModManifest.SECTION_PATCHES, null, key + ": the patch also runs for [Gameplay]; it runs twice, [Patches] last.");

                this.patchPlans.add(planPatch(key, patch, patchRef.getArguments()));
            } catch (IllegalArgumentException ex) {
                error(FroggerModManifest.SECTION_PATCHES, null, ex.getMessage());
            }
        }
    }

    private GamePatch loadPackagedPatch(String key, FroggerModPatchRef patchRef) {
        byte[] patchBytes = getPackagedFile(key + " file", patchRef.getFile());
        List<String> lines = new ArrayList<>(Arrays.asList(new String(patchBytes, StandardCharsets.UTF_8).replace("\r\n", "\n").split("\n", -1)));
        try {
            GamePatch patch = new GamePatch();
            patch.loadPatchFromConfig(patchRef.getId(), new net.highwayfrogs.editor.file.config.Config(lines));
            return patch;
        } catch (RuntimeException ex) {
            throw fail(key + " file", "%s is not a valid patch (%s)", patchRef.getFile(), describeThrowable(ex));
        }
    }

    private PatchPlan planPatch(String key, GamePatch patch, Map<String, String> argumentTexts) {
        if (!patch.isCompatibleWithVersion(this.targetName))
            throw fail(key, "the patch '%s' does not list %s in its Versions", patch.getId(), this.targetName);
        if (!patch.getVersionSpecificVariables().isEmpty() && !patch.getVersionSpecificVariables().containsKey(this.targetName))
            throw fail(key, "the patch '%s' has no [%s] section with that build's addresses", patch.getId(), this.targetName);

        PatchPlan plan = new PatchPlan(key, patch);
        for (Map.Entry<String, String> argumentText : argumentTexts.entrySet()) {
            String argumentKey = key + " " + argumentText.getKey();
            PatchArgument argument = null;
            for (PatchArgument testArgument : patch.getArguments())
                if (testArgument.getName().equals(argumentText.getKey()))
                    argument = testArgument;

            if (argument == null)
                throw fail(argumentKey, "the patch '%s' has no argument named '%s'", patch.getId(), argumentText.getKey());

            String text = argumentText.getValue() != null ? argumentText.getValue().trim() : "";
            if (!argument.getType().getBehavior().isValidString(text))
                throw fail(argumentKey, "'%s' is not a valid %s value", text, argument.getType().name().toLowerCase(Locale.ROOT));

            Object value = argument.getType().getBehavior().parseString(text);
            if (!argument.getType().getBehavior().isValidValue(value, argument))
                throw fail(argumentKey, "%s is outside the argument's range %s..%s", text, argument.getMinimumValue(), argument.getMaximumValue());

            plan.argumentTypes.put(argument.getName(), argument.getType());
            plan.argumentValues.put(argument.getName(), value);
        }

        return plan;
    }

    private void applyImages() {
        for (ImagePlan plan : this.imagePlans) {
            VloImage image = plan.image;
            Boolean blackIsTransparent = plan.entry.getBlackIsTransparent();
            if (plan.inGameImage != null && !this.gameInstance.isPSX() && Boolean.FALSE.equals(blackIsTransparent) && image.isPaddingTransparent())
                image.setPaddingTransparent(false);

            if (plan.inGameImage != null) {
                image.replaceImage(plan.inGameImage, plan.bitDepth, plan.padding, plan.translucent, ProblemResponse.THROW_EXCEPTION);
            } else {
                image.setFlag(VloImage.FLAG_TRANSLUCENT, plan.translucent);
            }
            this.result.setGameModified(true);

            if (blackIsTransparent != null) {
                if (this.gameInstance.isPSX()) {
                    if (image.testFlag(VloImage.FLAG_BLACK_IS_TRANSPARENT) != blackIsTransparent)
                        warning(FroggerModManifest.SECTION_IMAGES, null, imageKey(plan.entry.getBank(), plan.entry.getName())
                                + " blackIsTransparent: the PSX flag follows the pixels, so it stays " + !blackIsTransparent + ".");
                } else {
                    image.setFlag(VloImage.FLAG_BLACK_IS_TRANSPARENT, blackIsTransparent);
                }
            }

            if (plan.inGameImage != null && (image.getWidth() != plan.fullWidth || image.getHeight() != plan.fullHeight))
                warning(FroggerModManifest.SECTION_IMAGES, null, imageKey(plan.entry.getBank(), plan.entry.getName()) + ": the image is stored as "
                        + image.getWidth() + "x" + image.getHeight() + ", not " + plan.fullWidth + "x" + plan.fullHeight + " as packaged.");

            applied(String.format("Replaced image %s/%s (texture id %d): %dx%d, in-game %dx%d, flags 0x%04X.", plan.entry.getBank(), plan.entry.getName(),
                    image.getTextureId(), image.getWidth(), image.getHeight(), image.getInternalUnpaddedWidth(), image.getInternalUnpaddedHeight(), image.getFlags()));
        }
    }

    private void checkTexturePlacement() {
        if (this.imagePlans.isEmpty() || this.gameInstance.getVloTree() == null)
            return;

        try {
            this.gameInstance.getVloTree().rebuildRecursive(null);
            applied("Checked that the textures still fit VRAM (FrogLord places them again when saving).");
        } catch (Throwable th) {
            this.result.setSaveExpectedToFail(true);
            warning(FroggerModManifest.SECTION_IMAGES, null, "The replaced images do not fit VRAM with FrogLord's texture placement, so saving will fail: "
                    + describeThrowable(th) + ". On PC, images without blackIsTransparent need room on pages without transparent textures.");
        }
    }

    private void applyMaps() {
        for (MapPlan plan : this.mapPlans) {
            if (plan.mapBytes != null) {
                plan.map = replaceMap(plan.map, plan.entry.getFile(), plan.mapBytes);
                applied("Replaced map " + plan.map.getFileDisplayName() + " with " + plan.entry.getFile() + " (" + plan.mapBytes.length + " bytes).");
            }

            if (plan.lowPolyBytes != null) {
                plan.lowPolyMap = replaceMap(plan.lowPolyMap, plan.entry.getLowPolyFile(), plan.lowPolyBytes);
                applied("Replaced map " + plan.lowPolyMap.getFileDisplayName() + " with " + plan.entry.getLowPolyFile() + " (" + plan.lowPolyBytes.length + " bytes).");
            }

            if (plan.remapIds != null) {
                TextureRemapArray remap = plan.map.getTextureRemap();
                int changedSlots = 0;
                for (int i = 0; i < plan.remapIds.length; i++) {
                    Short oldId = remap.getRemappedTextureId(i);
                    if (oldId == null || oldId != plan.remapIds[i])
                        changedSlots++;
                    remap.setRemappedTextureId(i, plan.remapIds[i]);
                }

                applied("Set the " + plan.remapIds.length + "-slot texture remap of " + plan.map.getFileDisplayName() + " from " + plan.entry.getRemap()
                        + " (" + changedSlots + " slot(s) changed).");
            }
        }

        for (TimeLimitPlan plan : this.timeLimitPlans) {
            FroggerMapFile map = this.resolver.resolveMapFile(levelKey(plan.level.getInternalName()) + " timeLimit", plan.level);
            int oldTimeLimit = map.getGeneralPacket().getStartingTimeLimit();
            map.getGeneralPacket().setStartingTimeLimit(plan.timeLimit);
            applied("Set the time limit of " + map.getFileDisplayName() + " from " + oldTimeLimit + " to " + plan.timeLimit + ".");
        }
    }

    private FroggerMapFile replaceMap(FroggerMapFile oldMap, String packagePath, byte[] mapBytes) {
        this.result.setGameModified(true);
        SCGameFile<?> newFile = this.gameInstance.getMainArchive().replaceFile(packagePath, mapBytes, oldMap.getIndexEntry(), oldMap, false);
        if (!(newFile instanceof FroggerMapFile))
            throw new IllegalStateException(packagePath + " could not be loaded as " + oldMap.getFileDisplayName() + "; the archive kept the old map.");

        return (FroggerMapFile) newFile;
    }

    private void applyLevelWads() {
        for (MapPlan plan : this.mapPlans) {
            if (plan.levelWad != null) {
                for (ModelPlan modelPlan : plan.models)
                    placeModel(plan.levelWad, modelPlan);
            }

            if (plan.mapBytes != null || plan.levelWad != null)
                validateLevelWad(plan.map);
            if (plan.lowPolyBytes != null)
                validateLevelWad(plan.lowPolyMap);
        }
    }

    private void placeModel(WADFile levelWad, ModelPlan plan) {
        String wadName = this.resolver.wadNameOf(levelWad);
        if (plan.alreadyHeld) {
            List<Integer> keptSlots = new ArrayList<>();
            for (FroggerModWadSlot slot : plan.slots) {
                WADEntry targetEntry = levelWad.getFiles().get(slot.getIndex());
                if (plan.bytes != null && !Arrays.equals(getFileBytes(targetEntry.getFile()), plan.bytes)) {
                    replaceModel(levelWad, slot.getIndex(), plan.modelName, plan.bytes, slot.getFileEntry(), "the package");
                } else {
                    keptSlots.add(slot.getIndex());
                }
            }

            if (!keptSlots.isEmpty())
                applied("Kept " + plan.modelName + " in " + wadName + " slot(s) " + keptSlots + ", which already hold it.");
            return;
        }

        FroggerModWadSlot source = plan.slots.get(0);
        int slot = source.getIndex();
        WADEntry targetEntry = levelWad.getFiles().get(slot);
        if (!targetEntry.isDummy() && !this.resolver.hasPlaceholderId(targetEntry))
            warning(FroggerModManifest.SECTION_MAPS, null, mapKey(this.resolver.levelCodeOf(findMapOfWad(levelWad))) + " models=" + plan.modelName
                    + ": slot " + slot + " of " + wadName + " held " + targetEntry.getFileEntry().getDisplayName() + ", which is replaced.");

        replaceModel(levelWad, slot, plan.modelName, plan.bytes, source.getFileEntry(), plan.packaged ? "the package" : source.getWadName());
    }

    private void replaceModel(WADFile levelWad, int slot, String modelName, byte[] modelBytes, MWIResourceEntry modelEntry, String origin) {
        String wadName = this.resolver.wadNameOf(levelWad);
        this.result.setGameModified(true);
        SCGameFile<?> newFile = this.gameInstance.getMainArchive().replaceFile(modelName, modelBytes, modelEntry, levelWad.getFiles().get(slot).getFile(), false);
        if (newFile == null)
            throw new IllegalStateException(modelName + " could not be loaded into " + wadName + " slot " + slot + "; the archive kept the old file.");

        applied("Placed " + modelName + " (resource id " + modelEntry.getResourceId() + ") into " + wadName + " slot " + slot + " from " + origin + ".");
    }

    private FroggerMapFile findMapOfWad(WADFile levelWad) {
        for (MapPlan plan : this.mapPlans)
            if (plan.levelWad == levelWad)
                return plan.map;
        return null;
    }

    private void validateLevelWad(FroggerMapFile map) {
        FroggerMapBook mapBook = this.resolver.getMapBook(map.getMapLevelID());
        if (mapBook == null || !mapBook.hasPerLevelWadFiles())
            return;

        WADFile levelWad = this.resolver.findLevelWad(map);
        if (levelWad == null) {
            warning(FroggerModManifest.SECTION_MAPS, null, mapKey(this.resolver.levelCodeOf(map)) + ": the map book of " + map.getFileDisplayName() + " names no level WAD, so its models were not checked.");
            return;
        }

        List<WADEntry> entries = levelWad.getFiles();
        boolean[] dummyBefore = new boolean[entries.size()];
        String[] namesBefore = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            dummyBefore[i] = entries.get(i).isDummy();
            namesBefore[i] = this.resolver.modelNameOf(entries.get(i));
        }

        map.getEntityPacket().ensureValidLevelWadFile(false);
        this.result.setGameModified(true);

        String wadName = this.resolver.wadNameOf(levelWad);
        for (int i = 0; i < entries.size(); i++)
            if (!dummyBefore[i] && entries.get(i).isDummy())
                applied("Cleared " + wadName + " slot " + i + " (" + (namesBefore[i] != null ? namesBefore[i] : entries.get(i).getDisplayName()) + "), which "
                        + map.getFileDisplayName() + " does not use.");

        String key = mapKey(this.resolver.levelCodeOf(map));
        Set<String> reported = new HashSet<>();
        for (FroggerMapEntity entity : map.getEntityPacket().getEntities()) {
            if (!(entity.getFormEntry() instanceof FroggerFormEntry)) {
                if (reported.add("form:" + entity.getLoggerInfo()))
                    warning(FroggerModManifest.SECTION_MAPS, null, key + ": " + entity.getLoggerInfo() + " has an unsupported form entry.");
                continue;
            }

            FroggerFormEntry formEntry = (FroggerFormEntry) entity.getFormEntry();
            if (!formEntry.testFlag(FroggerFormLibFlag.NO_MODEL) && formEntry.getModel(map, false) == null
                    && reported.add("model:" + formEntry.getWadIndex() + ":" + formEntry.getId()))
                warning(FroggerModManifest.SECTION_MAPS, null, key + ": " + entity.getLoggerInfo() + " uses a model missing from " + wadName
                        + " slot " + formEntry.getWadIndex() + "; the game will crash when it loads the level.");
        }

        for (int i = 0; i < entries.size(); i++)
            if (FroggerUtils.isHardcodedInGameCode(entries.get(i)) && entries.get(i).isDummy())
                warning(FroggerModManifest.SECTION_MAPS, null, key + ": " + wadName + " slot " + i + " (" + entries.get(i).getDisplayName()
                        + ") is used by the game code but holds no model.");
    }

    private void applyThemes() {
        for (ThemePlan plan : this.themePlans) {
            final int deathHeight = plan.deathHeight;
            int oldDeathHeight = plan.themeBook.execute(pc -> {
                int old = pc.getDeathHeight();
                pc.setDeathHeight(deathHeight);
                return old;
            }, psx -> {
                int old = psx.getDeathHeight();
                psx.setDeathHeight(deathHeight);
                return old;
            });

            this.result.setGameModified(true);
            applied("Set the " + plan.theme.name() + " theme book's death height from " + oldDeathHeight + " to " + deathHeight + ".");
        }
    }

    private void applyLevelSelect() {
        if (this.arcadePlan != null) {
            replaceRows(this.gameInstance.getArcadeLevelSelectEntries(), this.arcadePlan);
            applied("Rebuilt the arcade level select with " + (this.arcadePlan.size() - 1) + " row(s): " + String.join(",", this.manifest.getArcadeOrder()) + ".");
        }

        if (this.racePlan != null) {
            replaceRows(this.gameInstance.getRaceLevelSelectEntries(), this.racePlan);
            applied("Rebuilt the race level select with " + (this.racePlan.size() - 1) + " row(s): " + String.join(",", this.manifest.getRaceOrder()) + ".");
        }

        for (LevelPlan plan : this.levelPlans) {
            FroggerModLevelEntry entry = plan.entry;
            FroggerLevelSelectEntry row = plan.row;
            List<String> changes = new ArrayList<>();
            if (plan.world != null) {
                row.setWorld(plan.world);
                changes.add("world " + plan.world.name());
            }
            if (entry.getStackPosition() != null) {
                row.setStackPosition(entry.getStackPosition());
                changes.add("stackPosition " + entry.getStackPosition());
            }
            if (entry.getLocalLevelId() != null) {
                row.setLocalLevelId(entry.getLocalLevelId());
                changes.add("localLevelId " + entry.getLocalLevelId());
            }
            if (entry.getLevelsInWorld() != null) {
                row.setLevelsInWorld(entry.getLevelsInWorld());
                changes.add("levelsInWorld " + entry.getLevelsInWorld());
            }
            if (plan.namePointer != null && plan.namePointer != row.getLevelNameTexturePointer()) {
                if (row.getLevelNameTextureInGamePointer() == row.getLevelNameTexturePointer()) {
                    row.setLevelNameTextureInGamePointer(plan.namePointer);
                    changes.add("in-game name image " + entry.getNameImage());
                }

                row.setLevelNameTexturePointer(plan.namePointer);
                changes.add(String.format("name image %s (0x%08X)", entry.getNameImage(), plan.namePointer));
            }
            if (plan.picturePointer != null) {
                row.setLevelTexturePointer(plan.picturePointer);
                changes.add(String.format("picture %s (0x%08X)", entry.getPicture(), plan.picturePointer));
            }

            this.result.setGameModified(true);
            if (!changes.isEmpty())
                applied("Set the level-select row of " + entry.getLevelCode() + ": " + String.join(", ", changes) + ".");

            if (plan.renderTargets != null) {
                for (VloImage image : plan.renderTargets) {
                    BufferedImage text = FroggerUtils.createFroggerLevelNameTexture(this.gameInstance, image.getInternalUnpaddedWidth(),
                            image.getInternalUnpaddedHeight(), entry.getName(), LEVEL_NAME_FONT_SIZE);
                    image.replaceImage(text, null, -1, image.testFlag(VloImage.FLAG_TRANSLUCENT), ProblemResponse.THROW_EXCEPTION);
                    applied("Rendered the name '" + entry.getName() + "' of " + entry.getLevelCode() + " into " + this.resolver.bankNameOf(image.getParent())
                            + "/" + plan.renderedNameImage + " (" + image.getInternalUnpaddedWidth() + "x" + image.getInternalUnpaddedHeight() + ").");
                }
            }
        }
    }

    private void replaceRows(List<FroggerLevelSelectEntry> rows, List<FroggerLevelSelectEntry> newRows) {
        rows.clear();
        rows.addAll(newRows);
        this.result.setGameModified(true);
    }

    private void applyDemos() {
        for (DemoFilePlan plan : this.demoFilePlans) {
            this.result.setGameModified(true);
            String packagePath = ModFormatConstants.FOLDER_DEMOS + plan.demoName;
            SCGameFile<?> newFile = this.gameInstance.getMainArchive().replaceFile(packagePath, plan.bytes, plan.demoFile.getIndexEntry(), plan.demoFile, false);
            if (!(newFile instanceof FroggerDemoFile))
                throw new IllegalStateException(packagePath + " could not be loaded as " + plan.demoName + "; the archive kept the old file.");
            applied("Replaced demo " + plan.demoName + " with " + packagePath + " (" + plan.bytes.length + " bytes).");
        }

        int skippedRows = 0;
        int setRows = 0;
        for (DemoRowPlan plan : this.demoRowPlans) {
            this.result.setGameModified(true);
            if (plan.row.isSkipped()) {
                this.gameInstance.getDemoTable().get(plan.row.getIndex()).setSkipped();
                skippedRows++;
            } else {
                FroggerDemoFile demoFile = this.resolver.resolveDemoFile("[Demos] [[Table]] " + plan.row.getIndex() + " demo", plan.row.getDemoName());
                this.gameInstance.getDemoTable().get(plan.row.getIndex()).setup(plan.level, demoFile, plan.unlockLevel);
                setRows++;
            }
        }

        if (!this.demoRowPlans.isEmpty())
            applied("Set " + this.demoRowPlans.size() + " demo table row(s) of " + this.gameInstance.getDemoTable().size() + ": " + setRows + " demo(s), " + skippedRows + " skipped.");
    }

    private void applyPatches() {
        for (PatchPlan plan : this.patchPlans) {
            PatchRuntime runtime = new PatchRuntime(this.gameInstance, plan.patch, null, ProblemResponse.THROW_EXCEPTION);
            runtime.runSetup();
            String before = describeAppliedVariable(runtime);
            List<String> argumentTexts = new ArrayList<>();
            for (Map.Entry<String, Object> argument : plan.argumentValues.entrySet()) {
                PatchArgumentType type = plan.argumentTypes.get(argument.getKey());
                argumentTexts.add(argument.getKey() + "=" + type.getBehavior().safeValueToString(argument.getValue()));
                runtime.getVariables().put(argument.getKey(), new PatchValue(type, argument.getValue()));
            }

            this.result.setGameModified(true);
            runtime.run();
            applied("Applied patch '" + plan.patch.getId() + "' for " + plan.key + (argumentTexts.isEmpty() ? "" : " with " + String.join(", ", argumentTexts))
                    + (before != null ? " (" + before + ")" : "") + ".");
        }
    }

    private static String describeAppliedVariable(PatchRuntime runtime) {
        PatchValue applied = runtime.getVariable(FroggerModGameplay.VARIABLE_APPLIED, true);
        if (applied == null)
            return null;
        return applied.isBoolean() && applied.getAsBoolean() ? "the executable already held it" : "the executable did not hold it before";
    }

    private boolean isImagePackaged(String imageName) {
        for (FroggerModImageEntry entry : this.manifest.getImageEntries())
            if (entry.getName().equals(imageName) && entry.getFile() != null)
                return true;
        return false;
    }

    private static FroggerLevelSelectEntry findRow(List<FroggerLevelSelectEntry> rows, FroggerMapLevelID level) {
        for (FroggerLevelSelectEntry row : rows)
            if (!row.isTerminator() && row.getLevel() == level)
                return row;
        return null;
    }

    private byte[] getPackagedFile(String key, String path) {
        byte[] data = path != null ? this.modPackage.getEntryData(path) : null;
        if (data == null)
            throw fail(key, "the package holds no file '%s'", path);
        return data;
    }

    private BufferedImage readImage(String key, String path) {
        byte[] data = getPackagedFile(key + " file", path);
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null)
                throw fail(key + " file", "%s is not an image FrogLord can read", path);
            return image;
        } catch (IOException ex) {
            throw fail(key + " file", "%s could not be read (%s)", path, describeThrowable(ex));
        }
    }

    private static List<String> parseRemapFile(byte[] remapBytes) {
        List<String> names = new ArrayList<>();
        for (String line : new String(remapBytes, StandardCharsets.UTF_8).replace("\r\n", "\n").split("\n", -1)) {
            int commentAt = line.indexOf('#');
            String name = (commentAt >= 0 ? line.substring(0, commentAt) : line).trim();
            if (!name.isEmpty())
                names.add(name);
        }

        return names;
    }

    private static PsxImageBitDepth getBitDepth(String key, int bitsPerPixel) {
        for (PsxImageBitDepth bitDepth : PsxImageBitDepth.values())
            if (bitDepth.getBitsPerPixel() == bitsPerPixel)
                return bitDepth;
        throw fail(key + " bitDepth", "%d is not 4, 8 or 16", bitsPerPixel);
    }

    private static byte[] getFileBytes(SCGameFile<?> file) {
        if (file == null)
            return null;
        byte[] rawData = file.getRawFileData();
        return rawData != null ? rawData : file.writeDataToByteArray();
    }

    private static String getGameplayKey(String patchId) {
        if (FroggerModGameplay.PATCH_FPS.equals(patchId))
            return FroggerModGameplay.KEY_FRAME_RATE;
        if (FroggerModGameplay.PATCH_JUNGLE_SWAP.equals(patchId))
            return FroggerModGameplay.KEY_JUNGLE_SWAP;
        if (FroggerModGameplay.PATCH_INITIAL_LEVELS.equals(patchId))
            return FroggerModGameplay.KEY_INITIAL_LEVELS + "/" + FroggerModGameplay.KEY_VISIBLE_LEVELS;
        return patchId;
    }

    private static String imageKey(String bank, String name) {
        return "[Images] [[" + bank + "]] [[[" + name + "]]]";
    }

    private static String mapKey(String levelCode) {
        return "[Maps] [[" + levelCode + "]]";
    }

    private static String modelKey(String modelName) {
        return "[Models] [[" + modelName + "]]";
    }

    private static String levelKey(String levelCode) {
        return "[LevelSelect] [[" + levelCode + "]]";
    }

    private static String describeThrowable(Throwable th) {
        StringBuilder builder = new StringBuilder();
        Throwable cause = th;
        while (cause != null) {
            if (builder.length() > 0)
                builder.append(" <- ");
            builder.append(cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
            cause = cause.getCause() != cause ? cause.getCause() : null;
        }

        return builder.toString();
    }

    private static IllegalArgumentException fail(String key, String message, Object... arguments) {
        return new IllegalArgumentException(key + ": " + String.format(message, arguments));
    }

    private void applied(String operation) {
        this.logger.info(operation);
        this.result.addAppliedOperation(operation);
    }

    private void warning(String section, String key, String message) {
        record(ModProblem.warning(section, key, message));
    }

    private void error(String section, String key, String message) {
        record(ModProblem.error(section, key, message));
    }

    private void record(ModProblem problem) {
        String message = (problem.getSection() != null ? "[" + problem.getSection() + "] " : "") + problem.getMessage();
        if (problem.isError()) {
            this.logger.severe(message);
        } else {
            this.logger.warning(message);
        }

        this.result.addProblem(problem);
    }

    private static class ImagePlan {
        private final FroggerModImageEntry entry;
        private final VloImage image;
        private BufferedImage inGameImage;
        private int padding;
        private int fullWidth;
        private int fullHeight;
        private boolean translucent;
        private PsxImageBitDepth bitDepth;

        private ImagePlan(FroggerModImageEntry entry, VloImage image) {
            this.entry = entry;
            this.image = image;
        }
    }

    private static class MapPlan {
        private final FroggerModMapEntry entry;
        private FroggerMapFile map;
        private byte[] mapBytes;
        private short[] remapIds;
        private FroggerMapFile lowPolyMap;
        private byte[] lowPolyBytes;
        private WADFile levelWad;
        private final List<ModelPlan> models = new ArrayList<>();

        private MapPlan(FroggerModMapEntry entry, FroggerMapFile map) {
            this.entry = entry;
            this.map = map;
        }
    }

    private static class ModelPlan {
        private final String modelName;
        private final List<FroggerModWadSlot> slots;
        private final byte[] bytes;
        private final boolean packaged;
        private final boolean alreadyHeld;

        private ModelPlan(String modelName, List<FroggerModWadSlot> slots, byte[] bytes, boolean packaged, boolean alreadyHeld) {
            this.modelName = modelName;
            this.slots = slots;
            this.bytes = bytes;
            this.packaged = packaged;
            this.alreadyHeld = alreadyHeld;
        }
    }

    private static class TimeLimitPlan {
        private final FroggerMapLevelID level;
        private final int timeLimit;

        private TimeLimitPlan(FroggerMapLevelID level, int timeLimit) {
            this.level = level;
            this.timeLimit = timeLimit;
        }
    }

    private static class ThemePlan {
        private final FroggerMapTheme theme;
        private final FroggerThemeBook themeBook;
        private final int deathHeight;

        private ThemePlan(FroggerMapTheme theme, FroggerThemeBook themeBook, int deathHeight) {
            this.theme = theme;
            this.themeBook = themeBook;
            this.deathHeight = deathHeight;
        }
    }

    private static class LevelPlan {
        private final FroggerModLevelEntry entry;
        private final FroggerLevelSelectEntry row;
        private FroggerLevelSelectWorldID world;
        private Long namePointer;
        private Long picturePointer;
        private String renderedNameImage;
        private List<VloImage> renderTargets;

        private LevelPlan(FroggerModLevelEntry entry, FroggerLevelSelectEntry row) {
            this.entry = entry;
            this.row = row;
        }
    }

    private static class DemoFilePlan {
        private final String demoName;
        private final FroggerDemoFile demoFile;
        private final byte[] bytes;

        private DemoFilePlan(String demoName, FroggerDemoFile demoFile, byte[] bytes) {
            this.demoName = demoName;
            this.demoFile = demoFile;
            this.bytes = bytes;
        }
    }

    private static class DemoRowPlan {
        private final FroggerModDemoRow row;
        private FroggerMapLevelID level;
        private FroggerMapLevelID unlockLevel;

        private DemoRowPlan(FroggerModDemoRow row) {
            this.row = row;
        }
    }

    private static class PatchPlan {
        private final String key;
        private final GamePatch patch;
        private final Map<String, PatchArgumentType> argumentTypes = new LinkedHashMap<>();
        private final Map<String, Object> argumentValues = new LinkedHashMap<>();

        private PatchPlan(String key, GamePatch patch) {
            this.key = key;
            this.patch = patch;
        }
    }
}
