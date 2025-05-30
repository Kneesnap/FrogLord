package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.standard.MRTexture;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.SCGameConfig.SCBssSymbol;
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
import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;

import java.io.File;
import java.util.*;
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
                resourceEntry.getLogger().warning("MOF Parent Override for '" + otherMofFile + "' was not found. Entry: " + replaceEntry);
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
     * Saves an image ordering table for hash testing
     * @param instance the game instance to export the image ordering table from
     * @return orderingTableStrings
     */
    public static List<String> saveImageOrderingTable(SCGameInstance instance, Map<Integer, String> symbolMap) {
        if (instance == null)
            throw new NullPointerException("instance");
        if (symbolMap == null)
            symbolMap = Collections.emptyMap();

        List<GameImage> bssOrderedImages = getImagesInBssOrder(instance);

        // Determine the longest name.
        int maxNameLength = 0;
        for (int i = 0; i < bssOrderedImages.size(); i++) {
            GameImage image = bssOrderedImages.get(i);
            String imageName = image.getOriginalName();

            int tempNameLength;
            if (StringUtils.isNullOrEmpty(imageName)) {
                tempNameLength = C_UNNAMED_IMAGE_PREFIX.length() + NumberUtils.getDigitCount(image.getTextureId() & 0xFFFF);
            } else {
                tempNameLength = imageName.length();
            }

            if (tempNameLength > maxNameLength)
                maxNameLength = tempNameLength;
        }

        // Go through bss symbol names too for max name length.
        for (SCBssSymbol symbol : instance.getVersionConfig().getBssSymbols().values()) {
            String symbolName = symbol.getName();
            if (!StringUtils.isNullOrWhiteSpace(symbolName) && symbolName.length() > maxNameLength)
                maxNameLength = symbolName.length();
        }

        List<String> results = new ArrayList<>();
        List<String> linesAtEnd = new ArrayList<>();
        StringBuilder lineBuilder = new StringBuilder();

        long lastAddress = -1;
        int lastHash = -1;
        boolean lastWasImageSymbol = true;
        int[] hashes = getHashesPerImage(instance, bssOrderedImages);
        int hashDigitCount = instance.isPC()
                ? NumberUtils.getDigitCount(FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE)
                : NumberUtils.getDigitCount(FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE);
        int targetCommentPosition = C_IMAGE_TYPE_PREFIX.length() + maxNameLength + 1;
        for (int i = 0; i < instance.getBmpTexturePointers().size(); i++) {
            GameImage image = bssOrderedImages.get(i);
            long currAddress = instance.getBmpTexturePointers().get(image.getTextureId());

            if (i > 0 && (int) lastAddress != (int) currAddress) { // There's a gap.
                int gapStartAddress = (int) lastAddress;
                int gapSize = (int) (currAddress - lastAddress);

                // Try to resolve name.
                String symbolName;
                SCBssSymbol symbol = instance.getVersionConfig().getBssSymbols().get(gapStartAddress);
                if (symbol != null) {
                    gapSize = symbol.getSize();
                    symbolName = symbol.getName();
                } else {
                    symbolName = String.format("DAT_%08x", gapStartAddress);
                }

                lineBuilder.setLength(0);
                lineBuilder.append("// ").append(symbolName);
                if (symbol != null) {
                    int hash = calculateHash(instance, symbolName);
                    if (lastHash > hash)
                        instance.getLogger().warning("Non-Image Symbol '%s' (%d) appears to be improperly ordered due to improper hash ordering. (Last Hash: %d)", symbolName, hash, lastHash);

                    writeHashComment(lineBuilder, targetCommentPosition, hashDigitCount, hash);
                    lineBuilder.append(" (START/END)");
                    lastHash = hash;
                    lastWasImageSymbol = false;
                } else {
                    linesAtEnd.add(String.format("0x%08X,%s,%d", gapStartAddress, symbolMap.getOrDefault(gapStartAddress, "?"), gapSize));
                }

                lineBuilder.append(" [Size: ").append(gapSize).append(']');

                // Prepare for the next one.
                results.add(lineBuilder.toString());
                lastAddress += gapSize;
                i--;
                continue;
            }

            lineBuilder.setLength(0);
            lineBuilder.append(C_IMAGE_TYPE_PREFIX);

            // Write texture name.
            int hash = hashes[image.getTextureId() & 0xFFFF];
            String imageName = image.getOriginalName();
            if (imageName == null)
                imageName = C_UNNAMED_IMAGE_PREFIX + image.getTextureId();

            lineBuilder.append(imageName).append(';');
            if (hash >= 0) {
                if (lastHash > hash && !lastWasImageSymbol)
                    instance.getLogger().warning("Image Symbol '%s' (%d) appears to be improperly ordered due to improper hash ordering. (Last Hash: %d)", imageName, hash, lastHash);

                writeHashComment(lineBuilder, targetCommentPosition, hashDigitCount, hash);
                lastHash = hash;
                lastWasImageSymbol = true;
            }

            results.add(lineBuilder.toString());

            // Prepare for next iteration.
            lastAddress = currAddress + MRTexture.SIZE_IN_BYTES;
        }

        if (linesAtEnd.size() > 0) {
            results.add("");
            results.addAll(linesAtEnd);
        }

        return results;
    }

    private static int calculateHash(SCGameInstance instance, String symbolName) {
        if (instance == null)
            throw new NullPointerException("instance");

        switch (instance.getPlatform()) {
            case WINDOWS:
                return FroggerHashUtil.getMsvcCompilerC1HashTableKey(symbolName);
            case PLAYSTATION:
                return FroggerHashUtil.getPsyQLinkerHash(symbolName);
            default:
                throw new UnsupportedOperationException("Cannot calculate hash for the " + instance.getPlatform() + " platform.");
        }
    }

    private static int[] getHashesPerImage(SCGameInstance instance, List<GameImage> orderedImages) {
        int maxTextureId = -1;
        for (int i = 0; i < orderedImages.size(); i++) {
            int textureId = orderedImages.get(i).getTextureId() & 0xFFFF;
            if (textureId > maxTextureId)
                maxTextureId = textureId;
        }

        // Create results, and apply per-image.
        String lastName = null;
        GameImage lastImage = null;
        int lastHash = -1;
        int[] results = new int[maxTextureId + 1];
        Arrays.fill(results, -1);
        for (int i = 0; i < orderedImages.size(); i++) {
            GameImage image = orderedImages.get(i);
            String originalName = image.getOriginalName();
            if (originalName == null)
                continue;

            int hash = calculateHash(instance, originalName);
            results[image.getTextureId() & 0xFFFF] = hash;
            if (lastHash > hash && lastHash != -1) {
                instance.getLogger().warning("Symbols '%s' (%d) and '%s' (%d) appear to be improperly ordered due to detecting improper hash ordering.", lastName, lastHash, originalName, hash);
            } else if (i > 0 && hash == lastHash && lastImage != null) {
                if (instance.isPSX()) {
                    int currAssemblerHash = FroggerHashUtil.getPsyQAssemblerHash(originalName);
                    int lastAssemblerHash = FroggerHashUtil.getPsyQAssemblerHash(lastName);
                    if (currAssemblerHash > lastAssemblerHash) {
                        instance.getLogger().warning("Symbols '%s' (%d) and '%s' (%d) appear to be ordered improperly due to string length/assembler hash.", lastName, lastAssemblerHash, originalName, currAssemblerHash);
                    } else if (currAssemblerHash == lastAssemblerHash && lastImage.getTextureId() <= image.getTextureId()) {
                        instance.getLogger().warning("Symbols '%s' (%d) and '%s' (%d) appear to be ordered improperly due to texture ID ordering.", lastName, lastImage.getTextureId(), originalName, image.getTextureId());
                    }
                } else if (instance.isPC() && lastImage.getTextureId() <= image.getTextureId()) {
                    instance.getLogger().warning("Symbols '%s' (%d) and '%s' (%d) appear to be ordered improperly due to texture ID ordering.", lastName, lastImage.getTextureId(), originalName, image.getTextureId());
                }
            }

            lastHash = hash;
            lastImage = image;
            lastName = originalName;
        }

        // Copy hashes between multiple confirmed numbers.
        lastHash = -1;
        int lastIndex = -1;
        for (int i = 0; i < orderedImages.size(); i++) {
            GameImage image = orderedImages.get(i);
            String name = image.getOriginalName();
            int hash = results[image.getTextureId() & 0xFFFF];
            if (name == null || hash < 0)
                continue;

            // Apply hash to all entries in-between.
            if (lastHash == hash)
                for (int j = lastIndex; j < i; j++)
                    results[orderedImages.get(j).getTextureId() & 0xFFFF] = hash;

            lastHash = hash;
            lastIndex = i;
        }

        // TODO: Test assembler hash.

        return results;
    }

    private static void writeHashComment(StringBuilder builder, int targetCommentPosition, int hashDigitCount, int hash) {
        // Pad to a consistent width.
        while (targetCommentPosition > builder.length())
            builder.append(' ');
        builder.append(" // ");

        builder.append(NumberUtils.padNumberString(hash, hashDigitCount));
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
}