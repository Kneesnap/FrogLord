package net.highwayfrogs.editor.games.sony.shared.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.standard.MRTexture;
import net.highwayfrogs.editor.file.vlo.GameImage;
import net.highwayfrogs.editor.games.sony.SCGameConfig.SCBssSymbol;
import net.highwayfrogs.editor.games.sony.SCGameConfig.SCBssSymbolType;
import net.highwayfrogs.editor.games.sony.SCGameConfig.SCImageList;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.gui.extra.hash.FroggerHashUtil;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.StringUtils;

import java.util.*;

/**
 * Created by Kneesnap on 12/07/2025.
 */
public class SCImageTableGenerator {
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

        List<GameImage> bssOrderedImages = SCUtils.getImagesInBssOrder(instance);

        // Determine the longest name.
        int maxNameLength = 0;
        for (int i = 0; i < bssOrderedImages.size(); i++) {
            GameImage image = bssOrderedImages.get(i);
            String imageName = image.getOriginalName();

            int tempNameLength;
            if (StringUtils.isNullOrEmpty(imageName)) {
                tempNameLength = SCUtils.C_UNNAMED_IMAGE_PREFIX.length() + NumberUtils.getDigitCount(image.getTextureId() & 0xFFFF);
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

        List<String> linesAtEnd = new ArrayList<>();
        StringBuilder lineBuilder = new StringBuilder();

        long lastAddress = -1;
        int lastHash = -1;
        boolean lastWasImageSymbol = true;
        int[] hashes = getHashesPerImage(instance, bssOrderedImages);
        int hashDigitCount = instance.isPC()
                ? NumberUtils.getDigitCount(FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE)
                : NumberUtils.getDigitCount(FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE);
        int targetCommentPosition = SCUtils.C_IMAGE_TYPE_PREFIX.length() + maxNameLength + 1;
        List<OutputSymbol> symbols = new ArrayList<>();
        for (int i = 0; i < bssOrderedImages.size(); i++) {
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
                int hash = -1;
                if (symbol != null) {
                    hash = calculateHash(instance, symbolName);
                    if (lastHash > hash)
                        instance.getLogger().warning("Non-Image Symbol '%s' (%d) appears to be improperly ordered due to improper hash ordering. (Last Hash: %d)", symbolName, hash, lastHash);

                    writeHashComment(lineBuilder, targetCommentPosition, hashDigitCount, hash);
                    if (symbol.getType() == SCBssSymbolType.PSYQ || symbol.getType() == SCBssSymbolType.GAME_LIB) {
                        lineBuilder.append(" (END)");
                    } else if (symbol.getType() == SCBssSymbolType.GAME) {
                        lineBuilder.append(" (START)");
                    } else {
                        lineBuilder.append(" (START/END)");
                    }
                    lastHash = hash;
                    lastWasImageSymbol = false;
                } else {
                    linesAtEnd.add(String.format("0x%08X,%s,%d", gapStartAddress, symbolMap.getOrDefault(gapStartAddress, "?"), gapSize));
                }

                lineBuilder.append(" [Size: ").append(gapSize).append(']');

                // Prepare for the next one.
                symbols.add(new OutputSymbol(null, symbol, symbolName, lineBuilder.toString(), hash));
                lastAddress += gapSize;
                i--;
                continue;
            }

            lineBuilder.setLength(0);
            lineBuilder.append(SCUtils.C_IMAGE_TYPE_PREFIX);

            // Write texture name.
            int hash = hashes[image.getTextureId() & 0xFFFF];
            String imageName = image.getOriginalName();
            if (imageName == null)
                imageName = SCUtils.C_UNNAMED_IMAGE_PREFIX + image.getTextureId();

            lineBuilder.append(imageName).append(';');
            if (hash >= 0) {
                if (lastHash > hash && !lastWasImageSymbol)
                    instance.getLogger().warning("Image Symbol '%s' (%d) appears to be improperly ordered due to improper hash ordering. (Last Hash: %d)", imageName, hash, lastHash);

                writeHashComment(lineBuilder, targetCommentPosition, hashDigitCount, hash);
                lastHash = hash;
                lastWasImageSymbol = true;
            }

            symbols.add(new OutputSymbol(image, null, imageName, lineBuilder.toString(), hash));

            // Prepare for next iteration.
            lastAddress = currAddress + MRTexture.SIZE_IN_BYTES;
        }

        // Write results.
        List<String> results = new ArrayList<>();

        OutputSymbol lastSymbolWithHash = null;
        OutputSymbol nextSymbolWithHash = null;
        int lastSymbolWithHashIndex = -1;
        int nextSymbolWithHashIndex = -1;
        for (int i = 0; i < symbols.size(); i++) {
            OutputSymbol symbol = symbols.get(i);
            if (symbol.hash != -1) {
                results.add(symbol.getTextLine());
                lastSymbolWithHash = symbol;
                lastSymbolWithHashIndex = i;
                continue;
            }

            if (i >= nextSymbolWithHashIndex) {
                nextSymbolWithHash = null;
                for (nextSymbolWithHashIndex = i + 1; nextSymbolWithHashIndex < symbols.size(); nextSymbolWithHashIndex++) {
                    OutputSymbol testSymbol = symbols.get(nextSymbolWithHashIndex);
                    if (testSymbol.hash != -1) {
                        nextSymbolWithHash = testSymbol;
                        break;
                    }
                }
            }

            int minHash = 0;
            if (lastSymbolWithHash != null) {
                minHash = lastSymbolWithHash.hash;
                OutputSymbol testSymbol = symbols.get(lastSymbolWithHashIndex + 1);
                if (lastSymbolWithHash.shouldIncreaseHash(instance, testSymbol))
                    minHash++;
            }

            int maxHash = instance.isPC() ? FroggerHashUtil.MSVC_SYMBOL_HASH_TABLE_SIZE - 1 : FroggerHashUtil.PSYQ_LINKER_HASH_TABLE_SIZE - 1;
            if (nextSymbolWithHash != null) {
                maxHash = nextSymbolWithHash.hash;
                OutputSymbol testSymbol = symbols.get(nextSymbolWithHashIndex - 1);
                if (testSymbol.shouldIncreaseHash(instance, nextSymbolWithHash))
                    maxHash--;
            }

            if (minHash > maxHash)
                instance.getLogger().warning("Calculated an invalid minHash/maxHash pair of %d-%d for symbol %s!", minHash, maxHash, symbol.getName());

            lineBuilder.setLength(0);
            lineBuilder.append(symbol.getTextLine());
            if (minHash == maxHash) {
                writeHashComment(lineBuilder, targetCommentPosition, hashDigitCount, minHash);
            } else {
                writeHashComment(lineBuilder, targetCommentPosition, NumberUtils.padNumberString(minHash, hashDigitCount) + "-" + NumberUtils.padNumberString(maxHash, hashDigitCount));
            }

            results.add(lineBuilder.toString());
        }

        if (linesAtEnd.size() > 0) {
            results.add("");
            results.addAll(linesAtEnd);
        }

        SCImageList imageList = instance.getVersionConfig().getImageList();
        instance.getLogger().info("Export complete, %d/%d textures had names configured. (%d left)",
                imageList.getImageNamesById().size(), bssOrderedImages.size(), bssOrderedImages.size() - imageList.getImageNamesById().size());
        return results;
    }

    @Getter
    @AllArgsConstructor
    private static class OutputSymbol {
        private final GameImage image;
        private final SCBssSymbol symbol;
        private final String name;
        private final String textLine;
        private int hash; // the hash known for the symbol, if there is one.

        public boolean shouldIncreaseHash(SCGameInstance instance, OutputSymbol nextSymbol) {
            GameImage nextImage = nextSymbol.getImage();
            if (instance.isPSX()) {
                return (this.symbol != null && (this.symbol.getType() == SCBssSymbolType.PSYQ || this.symbol.getType() == SCBssSymbolType.GAME_LIB))
                        || (nextSymbol.getSymbol() != null && nextSymbol.getSymbol().getType() == SCBssSymbolType.GAME);
            } else if (instance.isPC()) {
                if (this.image == null || nextImage == null)
                    throw new IllegalStateException("The PC versions were not expected to have non-image symbols in the image bss chunk!");

                return nextImage.getTextureId() > this.image.getTextureId();
            } else {
                throw new UnsupportedOperationException("The " + instance.getPlatform() + " platform is not currently supported.");
            }
        }
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
                        if (!"im_gatso".equals(originalName)) // im_gatso is special because of its first use being in main.obj, the only game .obj to be linked before sprdata.obj. Thus, its ordering is not consistent with the others.
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
        writeHashComment(builder, targetCommentPosition, NumberUtils.padNumberString(hash, hashDigitCount));
    }

    private static void writeHashComment(StringBuilder builder, int targetCommentPosition, String hashComment) {
        // Pad to a consistent width.
        while (targetCommentPosition > builder.length())
            builder.append(' ');
        builder.append(" // ");

        builder.append(hashComment);
    }

}
