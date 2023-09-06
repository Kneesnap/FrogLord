package net.highwayfrogs.editor.utils;

import lombok.Getter;

import java.util.Arrays;

/**
 * Tracks any number of bit flags, and whether or not they are set.
 * TODO: Make this a standalone class bundled in MTF.
 * Created by Kneesnap on 2/25/2022.
 */
public class FlagTracker {
    private final byte[] bytes;
    @Getter private int minFlag = -1;
    @Getter private int maxFlag = -1;
    @Getter private int activeFlagCount = 0;
    @Getter private final int flagSlots;

    private static int[] CACHE_FLAG_ARRAY = new int[128];

    public FlagTracker(int numOfFlags) {
        // Expand cache array so it can hold it.
        if (numOfFlags > CACHE_FLAG_ARRAY.length) {
            int newSize = CACHE_FLAG_ARRAY.length * 2;
            while (numOfFlags > newSize)
                newSize *= 2;
            CACHE_FLAG_ARRAY = new int[newSize];
        }

        this.flagSlots = numOfFlags;
        this.bytes = new byte[(numOfFlags / 8) + (numOfFlags % 8 > 0 ? 1 : 0)];
    }

    /**
     * Updates whether or not a particular flag is set.
     * @param flag     The flag to set the state for.
     * @param newState The new state to apply.
     */
    public void set(int flag, boolean newState) {
        if (flag < 0 || flag >= this.flagSlots)
            throw new RuntimeException("The flag " + flag + " is outside the range of tracked flags.");

        boolean oldState = get(flag);
        if (oldState == newState)
            return;

        int index = flag / 8;
        int bit = flag % 8;
        if (newState) {
            this.bytes[index] |= (1 << bit);
            this.activeFlagCount++;

            if (flag > this.maxFlag/* || this.maxFlag == -1*/)
                this.maxFlag = flag;
            if (flag < this.minFlag || this.minFlag == -1)
                this.minFlag = flag;
        } else {
            this.bytes[index] &= ~(1 << bit);
            this.activeFlagCount--;

            if (flag == this.maxFlag) {
                this.maxFlag = -1;

                // Reduce to the highest value, if one exists.
                if (this.activeFlagCount > 0)
                    for (int i = flag; i >= this.minFlag && this.maxFlag == -1; i--)
                        if (this.get(i))
                            this.maxFlag = i;
            }

            if (flag == this.minFlag) {
                this.minFlag = -1;

                // Reduce to the highest value, if one exists.
                if (this.activeFlagCount > 0)
                    for (int i = flag; i <= this.maxFlag && this.minFlag == -1; i++)
                        if (this.get(i))
                            this.minFlag = i;
            }
        }
    }

    /**
     * Tests whether or not a flag is set.
     * @param flag The flag to test.
     * @return Whether or not the flag is set.
     */
    public boolean get(int flag) {
        if (flag < 0 || flag >= this.flagSlots)
            return false;

        int index = flag / 8;
        int bit = flag % 8;
        return (this.bytes[index] & (1 << bit)) == (1 << bit);
    }

    /**
     * Stores all currently active flags into the target array sorted from lowest to highest.
     * @param array The array to store the flags in.
     * @return The number of flags which have been stored in the array.
     */
    public int getFlags(int[] array) {
        Arrays.fill(array, -1);
        if (this.activeFlagCount == 0)
            return 0;

        int count = 0;
        for (int i = this.minFlag; i <= this.maxFlag; i++)
            if (this.get(i))
                array[count++] = i;

        return count;
    }

    /**
     * Gets an array containing all of the flags currently set sorted from lowest to highest.
     */
    public int[] getFlags() {
        int count = this.getFlags(CACHE_FLAG_ARRAY);
        int[] newArray = new int[count];
        System.arraycopy(CACHE_FLAG_ARRAY, 0, newArray, 0, count);
        return newArray;
    }
}
