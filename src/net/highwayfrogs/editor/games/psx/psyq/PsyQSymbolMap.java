package net.highwayfrogs.editor.games.psx.psyq;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.utils.FileUtils;
import net.highwayfrogs.editor.utils.NumberUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a PsyQ .MAP file created by psylink.
 * Created by Kneesnap on 10/15/2026.
 */
public class PsyQSymbolMap {
    @Getter private final String fileName;
    private final Map<Integer, List<PsyQMapSymbol>> symbolsByAddress;
    private final Map<String, PsyQMapSymbol> symbolsByName;
    @Getter private final List<PsyQMapSymbol> orderedSymbols;
    @Getter private final List<PsyQMapSection> sections;

    private static final Pattern SYMBOL_DEFINITION = Pattern.compile("\\s*([a-fA-F0-9]{8})\\s*([a-zA-Z_][a-zA-Z0-9_]+)");
    private static final Pattern SECTION_DEFINITION = Pattern.compile(" ([0-9A-Fa-f]{8}) ([0-9A-Fa-f]{8}) ([0-9A-Fa-f]{8}) ([0-9A-Fa-f]{8}) ([a-zA-Z_]+)\\s+([a-zA-Z_.]+)");

    private PsyQSymbolMap(String fileName, Map<Integer, List<PsyQMapSymbol>> symbolsByAddress, Map<String, PsyQMapSymbol> symbolsByName, List<PsyQMapSymbol> orderedSymbols, List<PsyQMapSection> sections) {
        this.fileName = fileName;
        this.symbolsByAddress = symbolsByAddress;
        this.symbolsByName = symbolsByName;
        this.orderedSymbols = Collections.unmodifiableList(orderedSymbols);
        this.sections = Collections.unmodifiableList(sections);
    }

    /**
     * Looks up the executable section by its name. If no such section exists, null will be returned.
     * @param sectionName the section to lookup
     * @return section
     */
    public PsyQMapSection getSectionByName(String sectionName) {
        for (int i = 0; i < this.sections.size(); i++) {
            PsyQMapSection section = this.sections.get(i);
            if (section.sectionName.equals(sectionName))
                return section;
        }

        return null;
    }

    /**
     * Looks up the executable section by its name. If no such section exists, an Exception will be thrown.
     * @param sectionName the section to lookup
     * @return section
     */
    public PsyQMapSection getSectionByNameOrError(String sectionName) {
        PsyQMapSection section = getSectionByName(sectionName);
        if (section == null)
            throw new RuntimeException("No section named '" + sectionName + "' was found in '" + this.fileName + "'.");

        return section;
    }

    /**
     * Gets the address of the symbol with the provided name, throws an exception if it could not be found.
     * @param symbolName the name of the symbol to resolve
     * @return symbolAddress
     */
    public int getSymbolAddressOrError(String symbolName) {
        PsyQMapSymbol symbol = this.symbolsByName.get(symbolName);
        if (symbol == null)
            throw new RuntimeException("No symbol named '" + symbolName + "' was found in '" + this.fileName + "'.");

        return symbol.address;
    }

    /**
     * Gets the address of the symbol with the provided name, or returns -1 if no symbol exists.
     * @param symbolName the name of the symbol to resolve
     * @return symbolAddress, or -1
     */
    public int getSymbolAddressSafe(String symbolName) {
        PsyQMapSymbol symbol = this.symbolsByName.get(symbolName);
        return symbol != null ? symbol.address : -1;
    }

    /**
     * Gets the padded size of the symbol with the provided name, throws an exception if it could not be found.
     * @param symbolName the name of the symbol to resolve
     * @return paddedSymbolSize
     */
    public int getPaddedSymbolSizeInBytesOrError(String symbolName) {
        PsyQMapSymbol symbol = this.symbolsByName.get(symbolName);
        if (symbol == null)
            throw new RuntimeException("No symbol named '" + symbolName + "' was found in '" + this.fileName + "'.");

        return symbol.getPaddedSizeInBytes();
    }

    /**
     * Gets the unpadded size of the symbol with the provided name, throws an exception if it could not be found.
     * @param symbolName the name of the symbol to resolve
     * @param byteMultiple the byte-multiple which the symbol is known to be of.
     * @return unpaddedSymbolSize
     */
    public int getUnpaddedSymbolSizeInBytesOrError(String symbolName, int byteMultiple) {
        PsyQMapSymbol symbol = this.symbolsByName.get(symbolName);
        if (symbol == null)
            throw new RuntimeException("No symbol named '" + symbolName + "' was found in '" + this.fileName + "'.");

        return symbol.getUnpaddedSizeInBytes(byteMultiple);
    }

    /**
     * Finds the symbol which contains the given address.
     * @param address the address to get the symbol for
     * @return symbol
     */
    public PsyQMapSymbol getSymbolContaining(int address) {
        int left = 0, right = this.orderedSymbols.size() - 1;

        long tempAddress = address & 0xFFFFFFFFL;
        while (left <= right) {
            int middle = (left + right) >> 1;
            PsyQMapSymbol symbol = this.orderedSymbols.get(middle);
            int nextSymbolAddress = middle + 1 >= this.orderedSymbols.size() ? 0xFFFFFFFF : this.orderedSymbols.get(middle + 1).getAddress();

            if (tempAddress >= (nextSymbolAddress & 0xFFFFFFFFL)) {
                left = middle + 1;
            } else if (tempAddress < (symbol.getAddress() & 0xFFFFFFFFL)) {
                right = middle - 1;
            } else { // Found the symbol.
                return symbol.getUserSymbol();
            }
        }

        return null;
    }

    /**
     * Reads the symbols in a .MAP file.
     * @param file the file to read
     * @return symbolMapping
     */
    @SuppressWarnings("IfStatementMissingBreakInLoop")
    public static PsyQSymbolMap readSymbolMap(File file) {
        List<PsyQMapSection> sections = new ArrayList<>();
        Map<String, PsyQMapSymbol> symbolsByName = new HashMap<>();
        Map<Integer, List<PsyQMapSymbol>> symbolsByAddress = new HashMap<>();
        for (String line : FileUtils.readLinesFromFile(file)) {
            Matcher sectionMatcher = SECTION_DEFINITION.matcher(line);
            if (sectionMatcher.matches()) {
                int startAddress = (int) Long.parseLong(sectionMatcher.group(1), 16);
                int endAddress = (int) Long.parseLong(sectionMatcher.group(2), 16);
                int sectionLength = (int) Long.parseLong(sectionMatcher.group(3), 16);
                int objAddress = (int) Long.parseLong(sectionMatcher.group(4), 16);
                String groupName = sectionMatcher.group(5);
                String sectionName = sectionMatcher.group(6);
                sections.add(new PsyQMapSection(startAddress, endAddress, sectionLength, objAddress, groupName, sectionName));
                continue;
            }


            Matcher symbolMatcher = SYMBOL_DEFINITION.matcher(line);
            if (!symbolMatcher.matches()) {

                // This right here is a bit of a hack, but its purpose is to prefer using the actual order of the symbols/labels, instead of the alphabetical ordering, which occurs earlier in the file.
                if (line.endsWith("Names in address order")) {
                    symbolsByName.clear();
                    symbolsByAddress.clear();
                }

                continue;
            }

            int address = (int) Long.parseLong(symbolMatcher.group(1), 16);
            String symbolName = symbolMatcher.group(2);
            PsyQMapSymbol newSymbol = new PsyQMapSymbol(symbolName, address);

            // Warn if there are multiple symbols with the same name at a different address.
            PsyQMapSymbol otherSymbolWithSameName = symbolsByName.get(symbolName);
            if (otherSymbolWithSameName != null && otherSymbolWithSameName.address != address) {
                throw new RuntimeException("A symbol named '" + symbolName + "' labelled multiple addresses. (0x"
                        + NumberUtils.to0PrefixedHexString(address) + " and 0x"
                        + NumberUtils.to0PrefixedHexString(otherSymbolWithSameName.address) + ")");
            } else if (otherSymbolWithSameName == null) {
                symbolsByName.put(symbolName, newSymbol);
            }

            // Only register the symbol if it isn't already present.
            boolean symbolFound = false;
            List<PsyQMapSymbol> symbolsAtAddress = symbolsByAddress.computeIfAbsent(address, key -> new ArrayList<>());
            for (int i = 0; i < symbolsAtAddress.size() && !symbolFound; i++)
                if (symbolsAtAddress.get(i).name.equals(symbolName))
                    symbolFound = true;

            if (!symbolFound)
                symbolsAtAddress.add(newSymbol);
        }

        // Create the ordered list of symbols, AND setup the symbol linked list.
        List<Map.Entry<Integer, List<PsyQMapSymbol>>> orderedSymbolEntries = new ArrayList<>(symbolsByAddress.entrySet());
        orderedSymbolEntries.sort(Comparator.comparingLong(entry -> entry.getKey() & 0xFFFFFFFFL));

        PsyQMapSymbol lastSymbol = null;
        List<PsyQMapSymbol> orderedSymbols = new ArrayList<>();
        for (int i = 0; i < orderedSymbolEntries.size(); i++) {
            List<PsyQMapSymbol> currSymbols = orderedSymbolEntries.get(i).getValue();
            for (int j = 0; j < currSymbols.size(); j++) {
                PsyQMapSymbol symbol = currSymbols.get(j);
                if (lastSymbol != null)
                    lastSymbol.nextSymbol = symbol;

                orderedSymbols.add(symbol);
                symbol.previousSymbol = lastSymbol;
                lastSymbol = symbol;
            }
        }

        return new PsyQSymbolMap(file.getName(), symbolsByAddress, symbolsByName, orderedSymbols, sections);
    }

    /**
     * Gets a list containing all symbols found within the provided section
     * @param section the provided section to find symbols within
     * @return symbols found within the section
     */
    public List<PsyQMapSymbol> getSymbolsWithin(PsyQMapSection section) {
        if (section == null)
            throw new NullPointerException("section");

        return getSymbolsWithin(section.startAddress, section.endAddress);
    }

    /**
     * Gets a list containing all symbols found within the provided address bounds
     * @param startAddress the start address
     * @param exclusiveEndAddress the end address
     * @return symbols found within the address bounds
     */
    public List<PsyQMapSymbol> getSymbolsWithin(int startAddress, int exclusiveEndAddress) {
        PsyQMapSymbol temp = getSymbolContaining(startAddress);

        List<PsyQMapSymbol> symbols = new ArrayList<>();
        while (temp != null && exclusiveEndAddress > temp.getAddress()) {
            symbols.add(temp);
            temp = temp.getNextSymbol();
        }

        return symbols;
    }


    @Getter
    @RequiredArgsConstructor
    public static class PsyQMapSymbol {
        private final String name;
        private final int address;
        private PsyQMapSymbol previousSymbol;
        private PsyQMapSymbol nextSymbol;

        /**
         * Sometimes multiple symbols exist at the same address.
         * This function does its best to find the symbol which is part of the user's program, and not compiler/linker generated, such as _text_org or __sbss_objend.
         * It is not foolproof, and could fail under edge-case scenarios. But at the time of writing it works in all real use-cases.
         */
        private PsyQMapSymbol getUserSymbol() {

            // Search previous symbols.
            PsyQMapSymbol tempPrevSymbol = this.previousSymbol;
            while (tempPrevSymbol != null && tempPrevSymbol.address == this.address) {
                if (tempPrevSymbol.name.charAt(0) != '_')
                    return tempPrevSymbol;

                tempPrevSymbol = tempPrevSymbol.previousSymbol;
            }

            // Search next symbols.
            PsyQMapSymbol tempNextSymbol = this.nextSymbol;
            while (tempNextSymbol != null && tempNextSymbol.address == this.address) {
                if (tempNextSymbol.name.charAt(0) != '_')
                    return tempNextSymbol;

                tempNextSymbol = tempNextSymbol.nextSymbol;
            }

            //
            return this;
        }

        /**
         * Gets the next symbol which is not at the same address as this symbol.
         */
        public PsyQMapSymbol getNextSymbolAtNewAddress() {
            PsyQMapSymbol tempNext = this.nextSymbol;
            while (tempNext != null && this.address == tempNext.address)
                tempNext = tempNext.nextSymbol;

            return tempNext;
        }

        /**
         * Gets the size of the psyq symbol, calculated by the amount of space between this and the next symbol.
         * @return sizeInBytes
         */
        public int getPaddedSizeInBytes() {
            PsyQMapSymbol nextSymbol = getNextSymbolAtNewAddress(); // Skip all symbols which occupy the same spot, as they all have the same size.
            if (nextSymbol == null)
                return -1; // No size is known for the last symbol, since there's no next symbol to get the size from.

            return nextSymbol.address - this.address;
        }

        /**
         * Gets the unpadded size of the symbol.
         * @param byteMultiple the byte-multiple which the symbol is known to be of.
         * @return unpaddedSymbolSize
         */
        public int getUnpaddedSizeInBytes(int byteMultiple) {
            if (byteMultiple <= 0)
                throw new IllegalArgumentException("The provided byteMultiple was: " + byteMultiple + ", but it must be a positive non-zero number.");

            int size = getPaddedSizeInBytes();
            if (size <= 0)
                return size;

            return size - (size % byteMultiple);
        }

        @Override
        public String toString() {
            return this.name + "[" + NumberUtils.to0PrefixedHexString(this.address) + "]";
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class PsyQMapSection {
        private final int startAddress;
        private final int endAddress;
        private final int length;
        private final int objAddress; // Seems to be the same as startAddress.
        private final String group; // Seems to be specified in the linker script, seems arbitrary.
        private final String sectionName; // Begins with a dot in Frogger. Eg: ".text", ".bss", etc.
    }
}
