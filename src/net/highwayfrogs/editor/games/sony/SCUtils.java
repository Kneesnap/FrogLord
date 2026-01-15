package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.psx.PSXBitstreamImage;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.SCByteTextureUV;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile.WADEntry;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVBFile;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVHFile;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationMinimalSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCWindowsPreReleaseSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCWindowsRetailSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationVabSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCWindowsSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloFile;
import net.highwayfrogs.editor.games.sony.shared.vlo2.VloImage;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;
import net.highwayfrogs.editor.utils.image.ImageUtils;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This file contains functions shared between different Sony Cambridge / Millennium Interactive games which don't adhere well to polymorphism or are shared between different places.
 * Created by Kneesnap on 9/8/2023.
 */
public class SCUtils {
    public static final String IMAGE_C_PREFIX = "im_";
    public static final String UNNAMED_IMAGE_PREFIX = "img";
    public static final String C_UNNAMED_IMAGE_PREFIX = IMAGE_C_PREFIX + UNNAMED_IMAGE_PREFIX;
    public static final String C_IMAGE_TYPE_PREFIX = "MR_TEXTURE  ";

    private static final int MR_TEXTURE_UV_COUNT = 4;
    public static final int MR_TEXTURE_SIZE_IN_BYTES = (3 * Constants.SHORT_SIZE) + (2 * Constants.BYTE_SIZE) + (MR_TEXTURE_UV_COUNT * SCByteTextureUV.BYTE_SIZE);

    /**
     * Creates a game file from formats seen between different games.
     * @param resourceEntry The entry to create a game file from.
     * @param fileData  The file data to create files from. Can be null.
     * @return newGameFile
     */
    public static SCGameFile<?> createSharedGameFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        SCGameInstance instance = resourceEntry.getGameInstance();
        String vloSignature;
        switch (instance.getPlatform()) {
            case WINDOWS:
                vloSignature = VloFile.PC_SIGNATURE;
                break;
            case PLAYSTATION:
                vloSignature = VloFile.PSX_SIGNATURE;
                break;
            default:
                throw new RuntimeException("Unsupported target platform: " + instance.getPlatform());
        }

        // If there's file-data, it's the best indicator so let's use it first and foremost.
        if (fileData != null) {
            if (DataUtils.testSignature(fileData, vloSignature))
                return new VloFile(resourceEntry.getGameInstance());

            if (instance.isPSX() && DataUtils.testSignature(fileData, SCPlayStationVabSoundBankHeader.PSX_SIGNATURE))
                return makeSound(resourceEntry, fileData, SCForcedLoadSoundFileType.HEADER);

            // 3D models.
            if (instance.getGameType().isAtLeast(SCGameType.MOONWARRIOR)) {
                if (DataUtils.testSignature(fileData, PTStaticFile.IDENTIFIER_STRING))
                    return new PTStaticFile(resourceEntry.getGameInstance());
                if (DataUtils.testSignature(fileData, PTSkeletonFile.IDENTIFIER_STRING))
                    return new PTSkeletonFile(resourceEntry.getGameInstance());
                if (DataUtils.testSignature(fileData, PTActionSetFile.IDENTIFIER_STRING))
                    return new PTActionSetFile(resourceEntry.getGameInstance());
            } else {
                if (DataUtils.testSignature(fileData, MRModel.DUMMY_DATA) || DataUtils.testSignature(fileData, MRStaticMof.SIGNATURE) || MRAnimatedMof.testSignature(fileData))
                    return makeModel(resourceEntry);
            }
        }

        if (resourceEntry.hasExtension("bs"))
            return new PSXBitstreamImage(resourceEntry.getGameInstance());
        if (resourceEntry.hasExtension("vlo"))
            return new VloFile(resourceEntry.getGameInstance());
        if (resourceEntry.hasExtension("xmr") || resourceEntry.hasExtension("xar") || resourceEntry.hasExtension("xmu"))
            return makeModel(resourceEntry);
        if (resourceEntry.hasExtension("vh") || resourceEntry.hasExtension("vb"))
            return makeSound(resourceEntry, fileData, null);

        if (resourceEntry.getTypeId() == WADFile.TYPE_ID)
            return new WADFile(instance); // I think this is consistent across different games.

        // This should be one of the final checks because it is prone to false-positives and being slower than most checks.
        if (PSXTIMFile.isTIMFile(instance, fileData))
            return new PSXTIMFile(instance);

        return null;
    }

    /**
     * Creates a new mof holder for the file entry and game instance combo.
     * @param resourceEntry The file entry to create the file from.
     * @return model
     */
    public static MRModel makeModel(MWIResourceEntry resourceEntry) {
        SCGameInstance instance = resourceEntry.getGameInstance();
        String fileName = resourceEntry.getDisplayName();
        MRModel completeModel = null;

        // Override lookup.
        String otherMofFile = instance.getVersionConfig().getMofParentOverrides().get(fileName);
        if (otherMofFile != null) {
            MWIResourceEntry replaceEntry = instance.getResourceEntryByName(otherMofFile);
            if (replaceEntry != null)
                completeModel = instance.getGameFile(replaceEntry.getResourceId());
            if (completeModel == null)
                resourceEntry.getLogger().warning("MOF Parent Override for '%s' was not found. Entry: %s", otherMofFile, replaceEntry);
        } else {
            MRModel lastCompleteMOF = null;
            for (int i = resourceEntry.getResourceId() - 1; i >= 0; i--) {
                SCGameFile<?> testModel = instance.getGameFile(i);
                if (testModel instanceof MRModel) {
                    MRModel newModel = (MRModel) testModel;
                    if (!newModel.isIncomplete()) {
                        lastCompleteMOF = newModel;
                        break;
                    }
                }
            }

            completeModel = lastCompleteMOF;
        }

        return new MRModel(resourceEntry.getGameInstance(), completeModel);
    }

    /**
     * Creates a new sound file for the file entry and game instance combo.
     * @param resourceEntry The file entry to create the file from.
     * @param fileData  The contents of the file to test.
     * @return gameFile
     */
    public static SCGameFile<?> makeSound(MWIResourceEntry resourceEntry, byte[] fileData, SCForcedLoadSoundFileType forcedType) {
        SCGameInstance instance = resourceEntry.getGameInstance();
        SCGameFile<?> lastFile = instance.getGameFile(resourceEntry.getResourceId() - 1);
        SCGameFile<?> nextFile = instance.getGameFile(resourceEntry.getResourceId() + 1);
        MWIResourceEntry lastEntry = lastFile != null ? lastFile.getIndexEntry() : null;
        MWIResourceEntry nextEntry = nextFile != null ? nextFile.getIndexEntry() : null;
        boolean lastFileNameMatches = (lastEntry != null) && ((!lastEntry.hasFullFilePath() || !resourceEntry.hasFullFilePath())
                || FileUtils.stripExtension(resourceEntry.getDisplayName()).equalsIgnoreCase(FileUtils.stripExtension(lastEntry.getDisplayName())));
        boolean nextFileNamesMatches = (nextEntry != null) && ((!nextEntry.hasFullFilePath() || !resourceEntry.hasFullFilePath())
                || FileUtils.stripExtension(resourceEntry.getDisplayName()).equalsIgnoreCase(FileUtils.stripExtension(nextEntry.getDisplayName())));

        SCSplitVHFile lastSoundHeader = lastFile instanceof SCSplitVHFile ? (SCSplitVHFile) lastFile : null;
        SCSplitVBFile lastSoundBody = lastFile instanceof SCSplitVBFile ? (SCSplitVBFile) lastFile : null;

        // Ensure we find lastVH if we didn't find it before.
        if (lastSoundHeader == null && resourceEntry.hasExtension("vb")) {
            MWIResourceEntry vhEntry = resourceEntry.getGameInstance().getResourceEntryByName(FileUtils.stripExtension(resourceEntry.getDisplayName()) + ".vh");
            if (vhEntry != null) {
                SCGameFile<?> vhFile = resourceEntry.getGameInstance().getGameFile(vhEntry);
                if (vhFile instanceof SCSplitVHFile)
                    lastSoundHeader = (SCSplitVHFile) vhFile;
            }
        }

        // Ensure we find lastVB if we didn't find it before.
        if (lastSoundBody == null && resourceEntry.hasExtension("vh")) {
            MWIResourceEntry vbEntry = resourceEntry.getGameInstance().getResourceEntryByName(FileUtils.stripExtension(resourceEntry.getDisplayName()) + ".vb");
            if (vbEntry != null) {
                SCGameFile<?> vbFile = resourceEntry.getGameInstance().getGameFile(vbEntry);
                if (vbFile instanceof SCSplitVBFile)
                    lastSoundBody = (SCSplitVBFile) vbFile;
            }
        }

        // Create new object.
        if (lastSoundBody != null || resourceEntry.hasExtension("vh") || forcedType == SCForcedLoadSoundFileType.HEADER || (instance.isPSX() && DataUtils.testSignature(fileData, SCPlayStationVabSoundBankHeader.PSX_SIGNATURE))) {
            SCSplitSoundBankHeader<?, ?> newHeader = createSoundHeaderBody(instance);
            SCSplitVHFile newHeaderFile = new SCSplitVHFile(resourceEntry.getGameInstance(), newHeader);
            if ((lastFileNameMatches || !nextFileNamesMatches) && lastSoundBody != null && lastSoundBody.getSoundBank() == null)
                newHeaderFile.createSoundBank(lastSoundBody);

            return newHeaderFile;
        } else if (lastSoundHeader != null || resourceEntry.hasExtension("vb") || forcedType == SCForcedLoadSoundFileType.BODY) {
            SCSplitSoundBankBody<?, ?> newBody = createSoundBankBody(instance, resourceEntry);
            SCSplitVBFile newBodyFile = new SCSplitVBFile(resourceEntry.getGameInstance(), newBody);
            if ((lastFileNameMatches || !nextFileNamesMatches) && lastSoundHeader != null && lastSoundHeader.getSoundBank() == null)
                newBodyFile.createSoundBank(lastSoundHeader);

            return newBodyFile;
        }

        return null;
    }

    public enum SCForcedLoadSoundFileType {
        HEADER, BODY
    }

    private static SCSplitSoundBankHeader<?, ?> createSoundHeaderBody(SCGameInstance instance) {
        if (instance.isPSX() && instance.getGameType().isAtLeast(SCGameType.MEDIEVIL2)) {
            return new SCPlayStationMinimalSoundBankHeader(instance);
        } else if (instance.isPSX()) {
            return new SCPlayStationVabSoundBankHeader(instance);
        } else {
            return new SCWindowsSoundBankHeader<>(instance);
        }
    }

    private static SCSplitSoundBankBody<?, ?> createSoundBankBody(SCGameInstance instance, MWIResourceEntry resourceEntry) {
        String fileName = resourceEntry.getDisplayName();
        if (instance.isPSX() && instance.getGameType().isAtLeast(SCGameType.MEDIEVIL2)) {
            return new SCPlayStationMinimalSoundBankBody(instance, fileName);
        } else if (instance.isPSX()) {
            return new SCPlayStationSoundBankBody(instance, fileName);
        } else if (instance.getGameType().isAtOrBefore(SCGameType.OLD_FROGGER) || (instance.isFrogger() && !((FroggerGameInstance) instance).getVersionConfig().isAtLeastRetailWindows())) {
            return new SCWindowsPreReleaseSoundBankBody(instance, fileName);
        } else {
            return new SCWindowsRetailSoundBankBody(instance, fileName);
        }
    }

    /**
     * Add textures to the atlas used by model files, texture remaps, etc.
     * @param atlas      The texture atlas to add these textures to.
     * @param vloArchive The VLO to add textures from.
     */
    @SuppressWarnings("unused")
    public static void addAtlasTextures(TextureAtlas atlas, VloFile vloArchive) {
        if (vloArchive == null)
            return;

        for (int i = 0; i < vloArchive.getImages().size(); i++)
            atlas.addTexture(vloArchive.getImages().get(i));
    }

    /**
     * Calculates the checksum of the provided byte array.
     * This checksum is used in MediEvil II and C-12 Final Resistance, which this function has been validated against.
     * @param rawData the data to calculate the checksum from
     * @return checksum
     */
    public static int calculateChecksum(byte[] rawData) {
        long checksum = 0;
        for (int i = 0; i < rawData.length; i += Constants.INTEGER_SIZE) {
            int byteCount = Math.min(Constants.INTEGER_SIZE, rawData.length - i);
            int tempValue = DataUtils.readNumberFromBytes(rawData, byteCount, i);
            checksum += (tempValue & 0xFFFFFFFFL);
            if ((tempValue & 0xFFFFFFFFL) > (checksum & 0xFFFFFFFFL))
                checksum++;
        }

        // Zero indicates no checksum, so don't return zero.
        int checksum32 = (int) (checksum & 0xFFFFFFFFL);
        return (checksum32 != 0) ? checksum32 : -1;
    }

    /**
     * Strip win95 from the name of a file.
     * @param name The name to strip win95 from.
     * @return strippedName
     */
    public static String stripWin95(String name) {
        return name.contains("_WIN95") ? name.replace("_WIN95", "") : name;
    }

    /**
     * Strip the extension and Windows 95 from a file name.
     * @param name The file name.
     * @return stripped
     */
    public static String stripExtensionWin95(String name) {
        return stripWin95(FileUtils.stripExtension(name));
    }

    /**
     * Gets images in the order which they are seen in the .bss section.
     * Supports both the PSX and PC platforms.
     * @param instance the game instance to get images from
     * @return bssOrderImages
     */
    public static List<VloImage> getImagesInBssOrder(SCGameInstance instance) {
        if (instance == null)
            throw new NullPointerException("instance");

        List<VloImage> allImages = new ArrayList<>();
        for (VloFile archive : instance.getMainArchive().getAllFiles(VloFile.class))
            allImages.addAll(archive.getImages());

        // Remove images which aren't in the bmp texture pointer list.
        Iterator<VloImage> iterator = allImages.iterator();
        while (iterator.hasNext()) {
            VloImage image = iterator.next();
            int textureId = image.getTextureId() & 0xFFFF;
            Long bmpTexturePointer = instance.getBmpTexturePointers().size() > textureId ? instance.getBmpTexturePointers().get(textureId) : null;
            if (bmpTexturePointer == null || bmpTexturePointer == 0)
                iterator.remove();
        }

        // Sort images to
        allImages.sort(Comparator.comparingLong(image -> instance.getBmpTexturePointers().get(image.getTextureId() & 0xFFFF)));

        // Remove images with duplicate image IDs.
        short lastTextureId = -1;
        for (int i = 0; i < allImages.size(); i++) {
            VloImage image = allImages.get(i);
            if (image.getTextureId() == lastTextureId) {
                allImages.remove(i--);
                continue;
            }

            lastTextureId = image.getTextureId();
        }

        return allImages;
    }

    /**
     * Reads the symbols in a .MAP file.
     * @param file the file to read
     * @return symbolMapping
     */
    @SuppressWarnings("unused")
    public static Map<Integer, String> readSymbolMap(File file) {
        Pattern pattern = Pattern.compile("\\s*([a-fA-F0-9]{8})\\s*([a-zA-Z_][a-zA-Z0-9_]+)");

        Map<Integer, String> map = new HashMap<>();
        for (String line : FileUtils.readLinesFromFile(file)) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches())
                map.put((int) Long.parseLong(matcher.group(1), 16), matcher.group(2));
        }

        return map;
    }

    /**
     * Gets all instances of SC games except the provided game instance.
     * @param instance the instance to search from
     * @return gameInstances, if there are any.
     */
    public static List<SCGameInstance> getAllInstancesExcept(GameInstance instance) {
        List<SCGameInstance> instances = Collections.emptyList();
        for (GameInstance testInstance : FrogLordApplication.getActiveGameInstances()) {
            if (!(testInstance instanceof SCGameInstance) || testInstance == instance)
                continue;

            if (instances.isEmpty())
                instances = new ArrayList<>();
            instances.add((SCGameInstance) testInstance);
        }

        return instances;
    }

    /**
     * Find images shared between the two versions which are a perfect match, and generate an image naming config based on it.
     * @param nameSourceInst the game instance containing the image names
     * @param copyDestInst the game instance to transfer the image names to
     * @param includeMissingTexturesAsComments if true, missing textures will be included as comments
     * @return sharedImageConfig
     */
    public static String generateImageNameConfigForMatchingTextures(SCGameInstance nameSourceInst, SCGameInstance copyDestInst, boolean includeMissingTexturesAsComments) {
        if (nameSourceInst == null)
            throw new NullPointerException("nameSourceInst");
        if (copyDestInst == null)
            throw new NullPointerException("copyDestInst");

        Map<Integer, List<VloImage>> imageMappings = new HashMap<>();
        for (VloFile vloFile : nameSourceInst.getMainArchive().getAllFiles(VloFile.class)) {
            for (VloImage image : vloFile.getImages()) {
                int hash = Arrays.hashCode(image.getPixelBuffer());
                List<VloImage> list = imageMappings.computeIfAbsent(hash, key -> new ArrayList<>());
                if (list.stream().noneMatch(testImage -> testImage.getTextureId() == image.getTextureId()))
                    list.add(image);
            }
        }

        // Map images.
        Map<Short, List<VloImage>> resultingMappings = new HashMap<>();
        for (VloFile vloFile : copyDestInst.getMainArchive().getAllFiles(VloFile.class)) {
            for (VloImage image : vloFile.getImages()) {
                if (image.getName() != null) { // Keep previous name.
                    resultingMappings.putIfAbsent(image.getTextureId(), new ArrayList<>(Arrays.asList(image, image)));
                    continue;
                }

                List<VloImage> matchingImages = resultingMappings.get(image.getTextureId());
                if (matchingImages == null) {
                    resultingMappings.putIfAbsent(image.getTextureId(), matchingImages = new ArrayList<>());
                    matchingImages.add(image);
                }

                int hash = Arrays.hashCode(image.getPixelBuffer());
                List<VloImage> list = imageMappings.get(hash);
                if (list == null)
                    continue;

                for (VloImage testImage : list)
                    if (!matchingImages.contains(testImage) && ImageUtils.doImagesMatch(image.toBufferedImage(), testImage.toBufferedImage()))
                        matchingImages.add(testImage);
            }
        }

        // Generate config.
        int generatedNames = 0, knownNames = 0;
        List<Entry<Short, List<VloImage>>> entryList = new ArrayList<>(resultingMappings.entrySet());
        entryList.sort(Comparator.comparingInt(Entry::getKey));
        StringBuilder builder = new StringBuilder();
        for (Entry<Short, List<VloImage>> entry : entryList) {
            List<VloImage> images = entry.getValue();
            if (images == null || images.size() <= 1) {
                if (includeMissingTexturesAsComments)
                    builder.append("#").append(entry.getKey()).append("=?").append(Constants.NEWLINE);
                continue;
            }

            // If possible, pick it from the known VLO.
            VloImage destImage = images.remove(0);
            if (images.stream().anyMatch(testImage -> testImage.getParent().getFileDisplayName().equals(destImage.getParent().getFileDisplayName())))
                images.removeIf(testImage -> !testImage.getParent().getFileDisplayName().equals(destImage.getParent().getFileDisplayName()));

            VloImage firstImage = images.stream().filter(testImage -> testImage.getName() != null).findFirst().orElse(images.get(0));
            String name = firstImage.getName();
            if (name == null) {
                if (!includeMissingTexturesAsComments && images.stream().allMatch(image -> image.getName() == null))
                    continue;

                name = SCUtils.C_UNNAMED_IMAGE_PREFIX + firstImage.getTextureId();
                generatedNames++;
                builder.append("#");
            } else {
                knownNames++;
            }

            builder.append(entry.getKey()).append("=").append(name);
            if (images.remove(firstImage) && images.size() > 0) {
                builder.append(" # ");
                for (int i = 0; i < images.size(); i++) {
                    VloImage image = images.get(i);
                    String tempName = image.getName();
                    if (i > 0)
                        builder.append(", ");
                    builder.append(tempName != null ? tempName : SCUtils.C_UNNAMED_IMAGE_PREFIX + firstImage.getTextureId());
                }
            }

            builder.append(Constants.NEWLINE);
        }

        builder.append("# ").append(knownNames).append(" texture names have been mapped. (").append(generatedNames).append(" textures were linked, but not named)")
                .append(Constants.NEWLINE).append("# Names and IDs mapped from version: '").append(nameSourceInst.getVersionConfig().getInternalName()).append("'.");
        return builder.toString();
    }

    /**
     * Loads all .BS images in a given WAD file with the provided width/height.
     * @param instance the game instance to load the files from
     * @param wadFileName the name of the wad file to search
     * @param width the width of the images
     * @param height the height of the images
     * @param warnIfNotFound if true and the wad file is not found, display a warning
     */
    public static void loadBsImagesByName(SCGameInstance instance, String wadFileName, int width, int height, boolean warnIfNotFound) {
        if (instance == null)
            throw new NullPointerException("instance");
        if (StringUtils.isNullOrWhiteSpace(wadFileName))
            throw new NullPointerException("wadFileName");

        WADFile wadFile = instance.getMainArchive().getFileByName(wadFileName);
        if (wadFile == null) {
            if (warnIfNotFound)
                instance.getLogger().warning("Could not find file named '%s', skipping .BS image resolution.", wadFileName);
            return;
        }

        loadBsImages(wadFile, width, height);
    }

    /**
     * Loads all .BS images found within the given wad file.
     * @param wadFile the wad file to load files from
     * @param width the width of the images
     * @param height the height of the images.
     */
    public static void loadBsImages(WADFile wadFile, int width, int height) {
        if (wadFile == null)
            throw new NullPointerException("wadFile");
        if (width <= 0)
            throw new IllegalArgumentException("Invalid width: " + width);
        if (height <= 0)
            throw new IllegalArgumentException("Invalid height: " + height);

        for (WADEntry wadEntry : wadFile.getFiles()) {
            SCGameFile<?> gameFile = wadEntry.getFile();
            if (gameFile instanceof PSXBitstreamImage)
                ((PSXBitstreamImage) gameFile).getImage(width, height);
        }
    }

    /**
     * Copies a WAD file entry from one wad file to another.
     * @param source The source .WAD file to copy wad entries from
     * @param target the target .WAD file to set up (the per-level wad file)
     * @param wadEntryIndex the index of the wad file entry to copy.
     */
    @SuppressWarnings("unused") // Used by Noodle scripts.
    public static void copyWadEntry(WADFile source, WADFile target, int wadEntryIndex) {
        if (source == null)
            throw new NullPointerException("source");
        if (target == null)
            throw new NullPointerException("target");
        if (source.getFiles().size() != target.getFiles().size())
            throw new IllegalArgumentException("File '" + source.getFileDisplayName() + "' has " + source.getFiles().size() + " entries, while" + target.getFileDisplayName() + " has " + target.getFiles().size() + " entries. (They are not compatible with each other.)");
        if (wadEntryIndex < 0 || wadEntryIndex >= source.getFiles().size())
            throw new IllegalArgumentException("The wadEntryIndex: " + wadEntryIndex + " is not valid for " + source.getFileDisplayName() + "! (" + source.getFiles().size() + " entries)");

        WADEntry srcEntry = source.getFiles().get(wadEntryIndex);
        WADEntry dstEntry = target.getFiles().get(wadEntryIndex);
        byte[] rawData = srcEntry.getFile().writeDataToByteArray();
        SCGameFile<?> newFile = source.getArchive().replaceFile(srcEntry.getDisplayName(), rawData, dstEntry.getFileEntry(), dstEntry.getFile(), false);
    }
}