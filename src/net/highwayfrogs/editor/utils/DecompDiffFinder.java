package net.highwayfrogs.editor.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.games.sony.SCUtils;
import net.highwayfrogs.editor.utils.objects.IndexBitArray;

import java.io.File;
import java.util.*;

/**
 * Determines what symbol the next mismatch is located under when doing a partial source recomp.
 * TODO: Skip mismatched (and matched bytes) of 0x00, since they're the default byte.
 * TODO: If the same bytes are found nearby, respond.
 * Created by Kneesnap on 9/15/2025.
 */
public class DecompDiffFinder {
    private static final File DECOMP_FOLDER = new File(System.getProperty("user.home") + "\\Desktop\\medievil\\");
    private static final File ORIGINAL_EXECUTABLE = new File(DECOMP_FOLDER, "build" + File.separator + "NTSC-U (USA)" + File.separator + "files" + File.separator + "MEDIEVIL.EXE");
    private static final File COMPILED_EXECUTABLE = new File(DECOMP_FOLDER, "build" + File.separator + "MEDIEVIL.EXE");
    private static final File SYMBOL_MAP_FILE = new File(DECOMP_FOLDER, "build" + File.separator + "MEDIEVIL.MAP");
    private static final int FILE_TO_ADDRESS_OFFSET = 0x800214A4; // For MediEvil retail NTSC Overlays: 0x80010000, main executable: 0x800214A4

    private static final int CONSECUTIVE_BYTE_MISMATCH_DISPLAY = 16;
    private static final int CONSECUTIVE_BYTE_MATCH_TO_RESET = 2048;

    public static void main(String[] args) {
        List<PsyqMapSymbol> orderedSymbols = getOrderedSymbols();

        byte[] originalFileBytes = FileUtils.readBytesFromFile(ORIGINAL_EXECUTABLE);
        if (originalFileBytes == null || originalFileBytes.length == 0) {
            System.err.println("Failed to read file '" + ORIGINAL_EXECUTABLE + "'.");
            System.exit(1);
            return;
        }

        byte[] compiledFileBytes = FileUtils.readBytesFromFile(COMPILED_EXECUTABLE);
        if (compiledFileBytes == null || compiledFileBytes.length == 0) {
            System.err.println("Failed to read file '" + COMPILED_EXECUTABLE + "'.");
            System.exit(1);
            return;
        }

        // Find bytes which don't match.
        IndexBitArray nonMatchingBits = new IndexBitArray(compiledFileBytes.length);
        for (int i = 0; i < compiledFileBytes.length; i++) {
            if (i >= originalFileBytes.length || originalFileBytes[i] != compiledFileBytes[i])
                nonMatchingBits.setBit(i, true);
        }

        System.out.println(nonMatchingBits.getBitCount() + " bytes do not match, " + (compiledFileBytes.length - nonMatchingBits.getBitCount()) + " do match.");

        // Find mismatches.
        int nextIndex;
        int currentIndex = nonMatchingBits.getFirstBitIndex();
        int consecutiveBytesMismatched = 1;
        boolean isMismatched = false;
        while ((nextIndex = nonMatchingBits.getNextBitIndex(currentIndex)) >= 0) {
            int mismatchDistance = nextIndex - currentIndex;
            if (mismatchDistance == 1) { // Next byte is also mismatched.
                if (++consecutiveBytesMismatched >= CONSECUTIVE_BYTE_MISMATCH_DISPLAY && !isMismatched) {
                    isMismatched = true;

                    int mismatchAddress = currentIndex - consecutiveBytesMismatched + 1;
                    int mismatchRamAddress = mismatchAddress + FILE_TO_ADDRESS_OFFSET;
                    System.out.println("Mismatch Address: " + NumberUtils.toHexString(mismatchAddress) + "/" + NumberUtils.to0PrefixedHexString(mismatchRamAddress)
                            + ", Symbol: " + getSymbolContaining(orderedSymbols, mismatchRamAddress));
                    // TODO: Print mismatch and info about the start position.
                }
            } else {
                consecutiveBytesMismatched = 0;
                if (mismatchDistance >= CONSECUTIVE_BYTE_MATCH_TO_RESET)
                    isMismatched = false;
            }

            currentIndex = nextIndex;
        }


        // Allow user input for symbols.
        System.out.println();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("What is the address to find the symbol of? ");

            String userInputStr = scanner.nextLine();

            int address;
            if (NumberUtils.isHexInteger(userInputStr)) {
                address = NumberUtils.parseHexInteger(userInputStr);
            } else if (NumberUtils.isHexInteger("0x" + userInputStr)) {
                address = NumberUtils.parseHexInteger("0x" + userInputStr);
            } else {
                System.out.println("Invalid address.");
                continue;
            }

            PsyqMapSymbol symbol = getSymbolContaining(orderedSymbols, address);
            if (symbol == null)
                symbol = getSymbolContaining(orderedSymbols, address + FILE_TO_ADDRESS_OFFSET);

            System.out.println("Symbol: " + symbol);
            System.out.println();
        }
    }

    private static List<PsyqMapSymbol> getOrderedSymbols() {
        Map<Integer, String> symbolMap = SCUtils.readSymbolMap(SYMBOL_MAP_FILE);

        // Get ordered list of symbols.
        List<PsyqMapSymbol> mapSymbols = new ArrayList<>();
        for (Map.Entry<Integer, String> symbolEntry : symbolMap.entrySet())
            mapSymbols.add(new PsyqMapSymbol(symbolEntry.getValue(), symbolEntry.getKey()));

        mapSymbols.sort(Comparator.comparingInt(PsyqMapSymbol::getAddress));
        return mapSymbols;
    }

    private static PsyqMapSymbol getSymbolContaining(List<PsyqMapSymbol> symbols, int address) {
        int left = 0, right = symbols.size() - 1;

        long tempAddress = address & 0xFFFFFFFFL;
        while (left <= right) {
            int middle = (left + right) >> 1;
            PsyqMapSymbol symbol = symbols.get(middle);
            int nextSymbolAddress = middle + 1 >= symbols.size() ? 0xFFFFFFFF : symbols.get(middle + 1).getAddress();

            if (tempAddress >= (nextSymbolAddress & 0xFFFFFFFFL)) {
                left = middle + 1;
            } else if (tempAddress < (symbol.getAddress() & 0xFFFFFFFFL)) {
                right = middle - 1;
            } else { // Found the symbol.
                return symbol;
            }
        }

        return null;
    }

    @Getter
    @RequiredArgsConstructor
    private static class PsyqMapSymbol {
        private final String name;
        private final int address;

        @Override
        public String toString() {
            return this.name + "[" + NumberUtils.to0PrefixedHexString(this.address) + "]";
        }
    }
}
