package net.highwayfrogs.editor.utils.objects;

import lombok.Getter;
import net.highwayfrogs.editor.games.generic.data.IBinarySerializable;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

import java.util.Arrays;
import java.util.List;

/**
 * An array of indices, stored using bits.
 * Created by Kneesnap on 12/26/2023.
 */
public class IndexBitArray implements IBinarySerializable {
    private int[] array;
    @Getter private int bitCount; // The number of bits set.
    @Getter private int lastBitIndex; // The ID of the highest bit set.

    private static final int[] EMPTY_ARRAY = new int[0];
    private static final int ELEMENT_BIT_SHIFT = 5;
    private static final int BITS_PER_ELEMENT = 1 << ELEMENT_BIT_SHIFT; // 32

    public IndexBitArray() {
        this.array = EMPTY_ARRAY;
        clear();
    }

    public IndexBitArray(int expectedBitCount) {
        this.array = new int[getArraySize(expectedBitCount)];
        clear();
    }

    @Override
    public void load(DataReader reader) {
        this.bitCount = 0;
        this.lastBitIndex = 0;
        int arrayLength = reader.readInt();
        if (this.array == null || arrayLength != this.array.length)
            this.array = arrayLength > 0 ? new int[arrayLength] : EMPTY_ARRAY;

        for (int i = 0; i < arrayLength; i++) {
            int value = reader.readInt();
            this.array[i] = value;
            if (value == 0)
                continue;

            // Calculate bitCount and lastBitIndex.
            for (int bit = 0; bit < BITS_PER_ELEMENT; bit++) {
                int bitMask = 1 << bit;
                if ((value & bitMask) != bitMask)
                    continue;

                this.bitCount++;
                this.lastBitIndex = (i << ELEMENT_BIT_SHIFT) | bit;
            }
        }
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.array.length);
        for (int i = 0; i < this.array.length; i++)
            writer.writeInt(this.array[i]);
    }

    /**
     * Clear all the bits in the array.
     */
    public void clear() {
        Arrays.fill(this.array, 0, getArraySize(Math.max(0, this.lastBitIndex)), 0);
        this.bitCount = 0;
        this.lastBitIndex = -1;
    }

    /**
     * Writes the index bit array to a StringBuilder.
     * @param builder the string builder to write to.
     */
    public void toArrayString(StringBuilder builder) {
        int startLength = builder.length();
        int lastBitIndex = getFirstBitIndex();
        while (lastBitIndex != -1) {
            if (builder.length() > startLength)
                builder.append(", ");
            builder.append(lastBitIndex);
            lastBitIndex = getNextBitIndex(lastBitIndex);
        }
    }

    /**
     * Writes the index bit array to a String.
     */
    public String toArrayString() {
        StringBuilder builder = new StringBuilder("[");
        toArrayString(builder);
        return builder.append(']').toString();
    }

    /**
     * Gets the first bit set in the array.
     * @return firstBit, or -1 if there are no bits set.
     */
    public int getFirstBitIndex() {
        return getNextBitIndex(-1);
    }

    /**
     * Gets the next bit set after the given bit.
     * @param lastBitIndex The bit to start searching after.
     * @return nextBit, or -1 if there is no such next bit.
     */
    public int getNextBitIndex(int lastBitIndex) {
        if (this.lastBitIndex == -1 || lastBitIndex >= this.lastBitIndex)
            return -1;

        // Ensure any index less than zero will return the first bit found.
        if (lastBitIndex < 0)
            lastBitIndex = -1;

        int startElementIndex = (lastBitIndex + 1) >> ELEMENT_BIT_SHIFT;
        int startLocalBitIndex = (lastBitIndex + 1) % BITS_PER_ELEMENT;
        int endElementIndex = (this.lastBitIndex) >> ELEMENT_BIT_SHIFT;
        int endLocalBitIndex = (this.lastBitIndex) % BITS_PER_ELEMENT;

        for (int elementIndex = startElementIndex; elementIndex <= endElementIndex; elementIndex++) {
            int element = this.array[elementIndex];
            if (element == 0)
                continue; // Quick skip.

            // Calculate bit range.
            int startBit = (elementIndex == startElementIndex) ? startLocalBitIndex : 0;
            int endBit = (elementIndex == endElementIndex) ? endLocalBitIndex : (BITS_PER_ELEMENT - 1);

            // Test each bit
            for (int localBit = startBit; localBit <= endBit; localBit++) {
                int bitMask = (1 << localBit);
                if ((element & bitMask) == bitMask)
                    return (elementIndex << ELEMENT_BIT_SHIFT) + localBit;
            }
        }

        return -1;
    }

    /**
     * Gets the previous bit set after the given bit.
     * @param lastBitIndex The bit to start searching before.
     * @return nextBit, or -1 if there is no such previous bit.
     */
    public int getPreviousBitIndex(int lastBitIndex) {
        if (lastBitIndex <= 0)
            return -1;
        if (lastBitIndex > this.lastBitIndex)
            return this.lastBitIndex;

        int startElementIndex = (lastBitIndex - 1) >> ELEMENT_BIT_SHIFT;
        int startLocalBitIndex = (lastBitIndex - 1) % BITS_PER_ELEMENT;

        for (int elementIndex = startElementIndex; elementIndex >= 0; elementIndex--) {
            int element = this.array[elementIndex];
            if (element == 0)
                continue; // Quick skip.

            // Check each bit.
            int startBit = (elementIndex == startElementIndex) ? startLocalBitIndex : (BITS_PER_ELEMENT - 1);
            for (int localBit = startBit; localBit >= 0; localBit--) {
                int bitMask = (1 << localBit);
                if ((element & bitMask) == bitMask)
                    return (elementIndex << ELEMENT_BIT_SHIFT) + localBit;
            }
        }

        return -1;
    }


    /**
     * Get the state of the bit corresponding to the provided bit index.
     * @param bit The bit to test.
     * @return whether the provided bit is set
     */
    public boolean getBit(int bit) {
        if (bit < 0)
            throw new ArrayIndexOutOfBoundsException("Cannot access negative bits. (" + bit + ")");

        int elementIndex = (bit >> ELEMENT_BIT_SHIFT);
        int bitMask = (1 << (bit % BITS_PER_ELEMENT));
        return this.array.length > elementIndex && (this.array[elementIndex] & bitMask) == bitMask;
    }

    /**
     * Set the state of the bits corresponding to the provided bit index range.
     * @param startIndex bit index to start setting values from.
     * @param newState   the state to apply to the bits.
     */
    public void setBits(int startIndex, int amount, boolean newState) {
        if (amount < 0) {
            throw new ArrayIndexOutOfBoundsException("Cannot set a negative number of bits. (" + amount + ")");
        } else if (amount == 0) {
            return; // Nothing getting updated.
        } else if (amount == 1) {
            setBit(startIndex, newState);
            return;
        }

        // Ensure there's capacity for the bits.
        ensureCapacity(startIndex + amount);
        ensureCapacity(startIndex);

        // I tried an optimizing option, which would calculate the masks to apply to each element.
        // However, this doesn't have any benefit once we track the number of bits set. Eg: it would still require the same amount of work, but be more complicated.
        for (int i = 0; i < amount; i++)
            setBit(startIndex + i, newState);
    }

    /**
     * Set the state of the bit corresponding to the provided bit index.
     * @param bitIndex The bit index to set.
     * @param newState Whether the bit should be set.
     * @return true iff the bit changed
     */
    public boolean setBit(int bitIndex, boolean newState) {
        ensureCapacity(bitIndex);

        int elementIndex = (bitIndex >> ELEMENT_BIT_SHIFT);
        int bitMask = (1 << (bitIndex % BITS_PER_ELEMENT));
        int oldValue = this.array[elementIndex];

        // Verify there is a difference between the old and the new.
        boolean oldState = (oldValue & bitMask) == bitMask;
        if (oldState == newState)
            return false;

        // Update new state.
        if (newState) {
            this.array[elementIndex] |= bitMask;
            this.bitCount++;

            // Update the highest bit if this is the highest one.
            if (bitIndex > this.lastBitIndex)
                this.lastBitIndex = bitIndex;
        } else {
            this.array[elementIndex] &= ~bitMask;
            this.bitCount--;

            // Reduce the highest bit if this was the highest bit.
            if (this.lastBitIndex == bitIndex)
                this.lastBitIndex = getPreviousBitIndex(this.lastBitIndex);
        }

        return true;
    }

    private void ensureCapacity(int bit) {
        if (bit < 0)
            throw new ArrayIndexOutOfBoundsException("Cannot set negative bits (" + bit + ")");

        long newArraySize = this.array.length;
        int elementIndex = (bit >> ELEMENT_BIT_SHIFT);
        while (elementIndex >= newArraySize) {
            if (newArraySize == 0) {
                newArraySize = 1;
            } else {
                // Double the array.
                newArraySize <<= 1;
            }
        }

        if (newArraySize != this.array.length)
            this.array = Arrays.copyOf(this.array, (int) newArraySize);
    }

    /**
     * Gets the size of the array required to store the given number of bits.
     * @param bitCount the bit count kept by the array
     * @return requiredArrayLength
     */
    public static int getArraySize(int bitCount) {
        return ((bitCount + BITS_PER_ELEMENT - 1) >> ELEMENT_BIT_SHIFT);
    }

    /**
     * Calculates the amount of elements to remove for any given index.
     */
    public int[] calculateRemovalAmountLookupBuffer(int minimumSize) {
        int[] results = new int[Math.max(minimumSize, getLastBitIndex() + 1)];

        // Remove Elements.
        int lastIndex = -1, tempIndex = -1;
        int removeCount = 0;
        while ((tempIndex = getNextBitIndex(tempIndex)) >= 0) {
            Arrays.fill(results, lastIndex + 1, tempIndex + 1, removeCount++);
            lastIndex = tempIndex;
        }

        Arrays.fill(results, lastIndex + 1, results.length, removeCount);
        return results;
    }

    /**
     * Removes the values at the given indices from the list.
     * @param list the list to remove the values from
     */
    public <T> void removeValuesFromList(List<T> list) {
        if (list == null)
            throw new NullPointerException("list");

        // Remove Elements.
        int totalIndexCount = getBitCount();
        int removedGroups = 0;
        int currentIndex = getFirstBitIndex();
        for (int i = 0; i < totalIndexCount; i++) {
            int nextIndex = ((totalIndexCount > i + 1) ? getNextBitIndex(currentIndex) : list.size() - 1);
            int copyLength = (nextIndex - currentIndex);

            // If copy length is 0, that means there is a duplicate index (impossible), and we can just safely skip it.
            if (copyLength > 0) {
                int removeIndex = currentIndex - removedGroups; // This works because TestRemovals is sorted.
                removedGroups++;

                // System.arraycopy(src: list, srcPos: removeIndex + removedGroups, dst: list, dstPos: removeIndex, copyLength);
                for (int j = 0; j < copyLength; j++)
                    list.set(removeIndex + j, list.get(removeIndex + removedGroups + j));
            } else if (i == totalIndexCount - 1) { // Edge-case, if we're at the end of the array, copyLength can be zero because there would be nothing to copy. This should still be counted as a removal.
                removedGroups++;
            }

            currentIndex = nextIndex;
        }

        // Remove invalid elements from the end.
        while (totalIndexCount-- > 0)
            list.remove(list.size() - 1);
    }
}