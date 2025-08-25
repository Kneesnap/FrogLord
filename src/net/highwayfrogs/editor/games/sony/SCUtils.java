package net.highwayfrogs.editor.games.sony;

import lombok.NonNull;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.FrogLordApplication;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.ImageWorkHorse;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.mof2.MRModel;
import net.highwayfrogs.editor.games.sony.shared.mof2.animation.MRAnimatedMof;
import net.highwayfrogs.editor.games.sony.shared.mof2.mesh.MRStaticMof;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
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
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.StringUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
                vloSignature = VLOArchive.PC_SIGNATURE;
                break;
            case PLAYSTATION:
                vloSignature = VLOArchive.PSX_SIGNATURE;
                break;
            default:
                throw new RuntimeException("Unsupported target platform: " + instance.getPlatform());
        }

        // If there's file-data, it's the best indicator so let's use it first and foremost.
        if (fileData != null) {
            if (DataUtils.testSignature(fileData, vloSignature))
                return new VLOArchive(resourceEntry.getGameInstance());

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
        } else {
            if (resourceEntry.hasExtension("vlo"))
                return new VLOArchive(resourceEntry.getGameInstance());
            if (resourceEntry.hasExtension("xmr") || resourceEntry.hasExtension("xar") || resourceEntry.hasExtension("xmu"))
                return makeModel(resourceEntry);
        }

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
    public static void addAtlasTextures(TextureAtlas atlas, VLOArchive vloArchive) {
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
    public static List<GameImage> getImagesInBssOrder(SCGameInstance instance) {
        if (instance == null)
            throw new NullPointerException("instance");

        List<GameImage> allImages = new ArrayList<>();
        for (VLOArchive archive : instance.getMainArchive().getAllFiles(VLOArchive.class))
            allImages.addAll(archive.getImages());

        // Remove images which aren't in the bmp texture pointer list.
        Iterator<GameImage> iterator = allImages.iterator();
        while (iterator.hasNext()) {
            GameImage image = iterator.next();
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
            GameImage image = allImages.get(i);
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
     * @return sharedImageConfig
     */
    public static String generateImageNameConfigForMatchingTextures(SCGameInstance nameSourceInst, SCGameInstance copyDestInst) {
        if (nameSourceInst == null)
            throw new NullPointerException("nameSourceInst");
        if (copyDestInst == null)
            throw new NullPointerException("copyDestInst");

        Map<Integer, List<GameImage>> imageMappings = new HashMap<>();
        for (VLOArchive vloFile : nameSourceInst.getMainArchive().getAllFiles(VLOArchive.class)) {
            for (GameImage image : vloFile.getImages()) {
                int hash = Arrays.hashCode(image.getImageBytes());
                List<GameImage> list = imageMappings.computeIfAbsent(hash, key -> new ArrayList<>());
                if (list.stream().noneMatch(testImage -> testImage.getTextureId() == image.getTextureId()))
                    list.add(image);
            }
        }

        // Map images.
        Map<Short, List<GameImage>> resultingMappings = new HashMap<>();
        for (VLOArchive vloFile : copyDestInst.getMainArchive().getAllFiles(VLOArchive.class)) {
            for (GameImage image : vloFile.getImages()) {
                if (image.getOriginalName() != null) { // Keep previous name.
                    resultingMappings.putIfAbsent(image.getTextureId(), new ArrayList<>(Arrays.asList(image, image)));
                    continue;
                }

                List<GameImage> matchingImages = resultingMappings.get(image.getTextureId());
                if (matchingImages == null) {
                    resultingMappings.putIfAbsent(image.getTextureId(), matchingImages = new ArrayList<>());
                    matchingImages.add(image);
                }

                int hash = Arrays.hashCode(image.getImageBytes());
                List<GameImage> list = imageMappings.get(hash);
                if (list == null)
                    continue;

                for (GameImage testImage : list)
                    if (ImageWorkHorse.doImagesMatch(image.toBufferedImage(), testImage.toBufferedImage()) && !matchingImages.contains(testImage))
                        matchingImages.add(testImage);
            }
        }

        // Generate config.
        int generatedNames = 0, knownNames = 0;
        List<Entry<Short, List<GameImage>>> entryList = new ArrayList<>(resultingMappings.entrySet());
        entryList.sort(Comparator.comparingInt(Entry::getKey));
        StringBuilder builder = new StringBuilder();
        for (Entry<Short, List<GameImage>> entry : entryList) {
            List<GameImage> images = entry.getValue();
            if (images == null || images.size() <= 1) {
                builder.append("#").append(entry.getKey()).append("=?").append(Constants.NEWLINE);
                continue;
            }

            // If possible, pick it from the known VLO.
            GameImage destImage = images.remove(0);
            if (images.stream().anyMatch(testImage -> testImage.getParent().getFileDisplayName().equals(destImage.getParent().getFileDisplayName())))
                images.removeIf(testImage -> !testImage.getParent().getFileDisplayName().equals(destImage.getParent().getFileDisplayName()));

            GameImage firstImage = images.stream().filter(testImage -> testImage.getOriginalName() != null).findFirst().orElse(images.get(0));
            String name = firstImage.getOriginalName();
            if (name == null) {
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
                    GameImage image = images.get(i);
                    String tempName = image.getOriginalName();
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
     * Generates the .H file used in conjuntion with the .MWI/.MWD.
     * @param instance the instance to generate the header for
     * @param resourceDirectory the resource directory
     * @param file the file to save the mwd header as
     * @param fileTypePrefix the file type prefix
     * @param fileTypes the file types to define
     */
    public static void generateMwdCHeader(@NonNull SCGameInstance instance, @NonNull String resourceDirectory, @NonNull File file, @NonNull String fileTypePrefix, String... fileTypes) {
        String fileName = file.getName().toUpperCase();
        String ifdefName = "__" + fileName.replace('.', '_');

        DateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d yyyy 'at' kk:mm:ss");

        StringBuilder builder = new StringBuilder();
        builder.append("//").append(Constants.NEWLINE)
                .append("//\t").append(fileName).append(Constants.NEWLINE)
                .append("//").append(Constants.NEWLINE)
                .append("//\t").append("Generated by FrogLord ").append(Constants.VERSION).append(" on ").append(dateFormat.format(Calendar.getInstance().getTime())).append(Constants.NEWLINE)
                .append("//").append(Constants.NEWLINE)
                .append(Constants.NEWLINE)
                .append("#ifndef\t").append(ifdefName).append(Constants.NEWLINE)
                .append("#define\t").append(ifdefName).append(Constants.NEWLINE)
                .append(Constants.NEWLINE)
                .append("#define\tRES_").append(FileUtils.stripExtension(fileName)).append("_DIRECTORY\t\"").append(resourceDirectory).append("\"").append(Constants.NEWLINE)
                .append(Constants.NEWLINE)
                .append(Constants.NEWLINE);

        // Determine longest file names.
        final String resourcePrefix = "RES_";
        int maxLength = "RES_NUMBER_OF_RESOURCES".length();
        for (MWIResourceEntry entry : instance.getArchiveIndex().getEntries()) {
            int testLength = resourcePrefix.length() + entry.getDisplayName().length();
            if (testLength > maxLength)
                maxLength = testLength;
        }

        // Write file types.
        for (int i = 0; i < fileTypes.length; i++) {
            String suffix = fileTypes[i];
            if (!StringUtils.isNullOrEmpty(suffix))
                writeDefineSymbol(builder, fileTypePrefix + "_FTYPE_" + suffix, maxLength, "(" + i + ")");
        }

        builder.append(Constants.NEWLINE)
                .append(Constants.NEWLINE);

        // Write file names.
        for (MWIResourceEntry entry : instance.getArchiveIndex().getEntries()) {
            String entryName = resourcePrefix + entry.getDisplayName().replace(".", "_");
            writeDefineSymbol(builder, entryName, maxLength, "(" + entry.getResourceId() + ")");
        }

        builder.append(Constants.NEWLINE);
        writeDefineSymbol(builder, "RES_NUMBER_OF_RESOURCES", maxLength, "(" + instance.getArchiveIndex().getEntries().size() + ")");
        builder.append(Constants.NEWLINE)
                .append(Constants.NEWLINE)
                .append("#endif\t//").append(ifdefName).append(Constants.NEWLINE);

        // Write to file.
        FileUtils.writeStringToFile(instance.getLogger(), file, builder.toString(), true);
    }

    private static void writeDefineSymbol(StringBuilder builder, String symbolName, int maxLength, String value) {
        builder.append("#define\t").append(symbolName);

        int paddedChars = maxLength - symbolName.length();
        for (int i = 0; i < paddedChars; i++)
            builder.append(' ');

        builder.append('\t').append(value).append(Constants.NEWLINE);
    }
}