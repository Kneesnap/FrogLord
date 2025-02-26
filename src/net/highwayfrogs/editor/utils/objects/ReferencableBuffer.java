package net.highwayfrogs.editor.utils.objects;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.utils.NumberUtils;
import net.highwayfrogs.editor.utils.logging.ILogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents a buffer whose portions can be referenced if already written.
 * The main application is for use in the Sony Cambridge MOF model format, but theoretically this could be used in other games too, as it's not that complicated of a system.
 * Created by Kneesnap on 2/24/2025.
 */
@RequiredArgsConstructor
public abstract class ReferencableBuffer<TElement> {
    private final boolean allowPartialBufferReuse;

    // Both saving & loading.
    private final List<TElement> buffer = new ArrayList<>();
    private int bufferStartIndex = -1;

    // Load only:
    private final IndexBitArray usedElements = new IndexBitArray();

    // Save only:
    private final Map<TElement, List<Integer>> elementSaveOccurrences = new HashMap<>(); // A map of an elements to all places it can be found.

    /**
     * Reads an element into the buffer.
     * @param reader the reader to read the element from
     * @return the newly read element
     */
    public TElement readElement(DataReader reader) {
        if (this.bufferStartIndex < 0)
            this.bufferStartIndex = reader.getIndex();

        TElement newElement = loadElement(reader);
        this.buffer.add(newElement);
        return newElement;
    }

    /**
     * Loads an element from the reader.
     * @param reader the reader to load the buffer element from
     * @return newElement
     */
    protected abstract TElement loadElement(DataReader reader);

    /**
     * Saves an element to the writer.
     * @param writer the writer to write the element to
     * @param element the element to save
     */
    protected abstract void saveElement(DataWriter writer, TElement element);

    /**
     * Gets the number of bytes a single element takes up.
     */
    public abstract int getElementFixedByteSize();

    /**
     * Load the elements from the buffer into an output list.
     * @param output the output list to save
     * @param address the address to read the elements from
     * @param amount the number of elements to read
     */
    public void copyElementsFromBuffer(List<TElement> output, int address, int amount) {
        if (output == null)
            throw new NullPointerException("output");
        if (amount < 0)
            throw new IllegalArgumentException("amount must be at least zero! (was: " + amount + ")");
        if (this.bufferStartIndex < 0)
            throw new RuntimeException("bufferStartIndex has not been set, so elements cannot be loaded!");
        if (address < this.bufferStartIndex)
            throw new IllegalArgumentException("Cannot read elements from " + NumberUtils.toHexString(address) + ", that is before the start of the buffer at " + NumberUtils.toHexString(this.bufferStartIndex) + "!");

        int elementFixedByteSize = getElementFixedByteSize();
        if (elementFixedByteSize <= 0)
            throw new RuntimeException("Invalid fixed byte size of " + elementFixedByteSize + "!");

        int diff = (address - this.bufferStartIndex);
        int index = diff / elementFixedByteSize;
        if (index > this.buffer.size())
            throw new IllegalArgumentException("Cannot read elements from " + NumberUtils.toHexString(address) + ", as it is beyond the end of the element buffer. (" + NumberUtils.toHexString(this.bufferStartIndex + (this.buffer.size() * elementFixedByteSize)) + ")");

        if (index + amount > this.buffer.size())
            throw new IllegalArgumentException("Cannot read " + amount + " elements from " + NumberUtils.toHexString(address) + ", as there are only " + (this.buffer.size() - amount) + " buffers available from there.");

        if ((diff % elementFixedByteSize) != 0)
            throw new IllegalArgumentException("Cannot read elements from " + NumberUtils.toHexString(address) + " because it is misaligned by " + (diff % elementFixedByteSize) + " byte(s) from " + NumberUtils.toHexString(this.bufferStartIndex) + "!");

        // Load the elements.
        for (int i = 0; i < amount; i++) {
            this.usedElements.setBit(index + i, true);
            output.add(this.buffer.get(index + i));
        }
    }

    /**
     * Prints warnings after the data is loaded.
     */
    public void printLoadWarningsForUnusedData(ILogger logger) {
        // Sanity test on elements.
        // The MOF format tries to reuse memory as much as possible, so it would be surprising if there were any unused elements.
        // It could potentially point to a bug in FrogLord.
        int totalUnusedElementCount = 0;
        int currentStreakStartIndex = -1;
        int currentStreak = 0;
        int elementFixedByteSize = getElementFixedByteSize();
        for (int i = 0; i < this.buffer.size(); i++) {
            if (this.usedElements.getBit(i)) {
                if (currentStreak > 0) {
                    totalUnusedElementCount += currentStreak;
                    int dataPosition = this.bufferStartIndex + (currentStreakStartIndex * elementFixedByteSize);
                    logger.warning("There %s %d unused buffer element%s found at %08X.",
                            (currentStreak > 1 ? "were" : "was"), currentStreak, (currentStreak > 1 ? "s" : ""), dataPosition);

                    currentStreak = 0;
                }
            } else if (currentStreak++ == 0) {
                currentStreakStartIndex = i;
            }
        }

        if (totalUnusedElementCount > 0)
            logger.warning("A total of %d unused buffer element(s) were seen.", totalUnusedElementCount);
    }

    /**
     * Save the provided element buffer to the writer. If the element buffer is already written, a reference to the existing buffer will be used.
     * If the element buffer has not been written yet, it will be written, with a reference to the newly written values returned.
     * @param writer The writer to write the element buffer data to
     * @param buffer the elements to write. Should not contain any null values.
     * @return elementBufferPointer
     */
    public int saveElementBuffer(DataWriter writer, List<TElement> buffer) {
        if (this.bufferStartIndex < 0)
            this.bufferStartIndex = writer.getIndex();

        List<Integer> spotsToCheck = this.elementSaveOccurrences.get(buffer.get(0));

        // Check if the full buffer is already written.
        int elementWriteStartIndex = 0;
        int elementBufferPointer = writer.getIndex();
        if (spotsToCheck != null) {
            for (int i = 0; i < spotsToCheck.size(); i++) {
                int index = spotsToCheck.get(i);

                boolean fullBufferAvailable = true;
                if (index + buffer.size() > this.buffer.size()) {
                    if (!this.allowPartialBufferReuse)
                        break; // We've reached part of the buffer which isn't large enough to contain the buffer.

                    fullBufferAvailable = false;
                }

                // Test if the full buffer matches.
                boolean allMatch = true;
                for (int j = 1; j < buffer.size() && allMatch && index + j < this.buffer.size(); j++)
                    if (!this.buffer.get(index + j).equals(buffer.get(j)))
                        allMatch = false;

                if (allMatch) { // The buffer has been found already, so get the index of it.
                    elementBufferPointer = this.bufferStartIndex + (index * getElementFixedByteSize());
                    if (fullBufferAvailable) {
                        // We found the full buffer already written, so we'll use it.
                        return elementBufferPointer;
                    } else {
                        // We've found the start of our new data at the end of the existing buffer, so we're going to try and continue it to finish our new data.
                        elementWriteStartIndex = (this.buffer.size() - index);
                        break;
                    }
                }
            }
        }

        // Full (or partial) Buffer not found, write it.
        for (int i = elementWriteStartIndex; i < buffer.size(); i++) {
            TElement element = buffer.get(i);
            this.elementSaveOccurrences.computeIfAbsent(element, key -> new ArrayList<>()).add(this.buffer.size());
            this.buffer.add(element);
            saveElement(writer, element);
        }

        return elementBufferPointer + elementWriteStartIndex;
    }

    /**
     * A referencable buffer taking read/write behavior as constructor params.
     */
    public static class LazyReferencableBuffer<TElement> extends ReferencableBuffer<TElement> {
        @Getter private final int elementFixedByteSize;
        @NonNull private final Function<DataReader, TElement> reader;
        @NonNull private final BiConsumer<DataWriter, TElement> writer;

        public LazyReferencableBuffer(int elementFixedByteSize, Function<DataReader, TElement> reader, BiConsumer<DataWriter, TElement> writer, boolean allowPartialBufferReuse) {
            super(allowPartialBufferReuse);
            if (elementFixedByteSize <= 0)
                throw new IllegalArgumentException("Invalid fixed byte size: " + elementFixedByteSize);

            this.elementFixedByteSize = elementFixedByteSize;
            this.reader = reader;
            this.writer = writer;
        }


        @Override
        protected TElement loadElement(DataReader reader) {
            return this.reader.apply(reader);
        }

        @Override
        protected void saveElement(DataWriter writer, TElement element) {
            this.writer.accept(writer, element);
        }
    }
}
