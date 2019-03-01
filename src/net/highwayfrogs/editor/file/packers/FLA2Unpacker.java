package net.highwayfrogs.editor.file.packers;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.Utils;
import net.highwayfrogs.editor.system.ByteArrayWrapper;

/**
 * Supports FLA2 unpacking, used in Frogger 2.
 * Created by Kneesnap on 2/26/2019.
 */
public class FLA2Unpacker {
    public static final String MARKER = "FLA2";
    public static final byte[] MARKER_BYTES = MARKER.getBytes();
    private static final int HISTORY_SIZE = 4096;
    private static final int MASK_HISTORY = HISTORY_SIZE - 1;
    private static final int MASK_UPPER = 0xF0;
    private static final int MASK_LOWER = 0x0F;
    private static final int SHIFT_UPPER = 16;
    private static final int FLAG_FINISHED = 0x80;
    private static final int UNPACK_START_INDEX = MARKER_BYTES.length + Constants.INTEGER_SIZE;

    /**
     * Is a given byte array PP20 compressed data?
     * @param a The bytes to test.
     * @return isCompressed
     */
    public static boolean isCompressed(byte[] a) {
        return a.length > UNPACK_START_INDEX && Utils.testSignature(a, MARKER_BYTES);
    }

    /**
     * Unpacks FLA2 compressed data.
     * @param data The data to unpack.
     * @return unpackedData
     */
    public static byte[] unpackData(byte[] data) {
        Utils.verify(isCompressed(data), "Data is not compressed!");

        int readIndex = UNPACK_START_INDEX;
        byte[] lzHistory = new byte[HISTORY_SIZE]; // New clear history.
        int lzHistoryOff = 0;

        int resultSize = Utils.readIntFromBytes(data, MARKER_BYTES.length);
        ByteArrayWrapper outputWrapper = new ByteArrayWrapper(resultSize);

        while (true) {
            int tag = Utils.byteToUnsignedShort(data[readIndex++]);
            for (int loop = 0; loop < Constants.BITS_PER_BYTE; loop++) {
                if ((tag & FLAG_FINISHED) > 0) {
                    short count = Utils.byteToUnsignedShort(data[readIndex++]);

                    if (count == 0) {
                        Utils.verify(outputWrapper.size() == resultSize, "Invalid result size for decompression!");
                        return outputWrapper.toNewArray(); // Finished decompressing.
                    } else { // Copy from earlier.
                        int offset = HISTORY_SIZE - (((MASK_UPPER & count) * SHIFT_UPPER) + Utils.byteToUnsignedShort(data[readIndex++]));

                        count &= MASK_LOWER;
                        count += Constants.SHORT_SIZE;
                        while (count-- > 0) {
                            byte toAdd = data[Utils.byteToUnsignedShort(lzHistory[(lzHistoryOff + offset) & MASK_HISTORY])];
                            outputWrapper.add(toAdd);
                            lzHistory[lzHistoryOff] = toAdd;
                            lzHistoryOff = (lzHistoryOff + 1) & MASK_HISTORY;
                        }
                    }
                } else {
                    byte toAdd = data[readIndex++];
                    outputWrapper.add(toAdd);
                    lzHistory[lzHistoryOff] = toAdd;
                    lzHistoryOff = (lzHistoryOff + 1) & MASK_HISTORY;
                }
                tag += tag;
            }
        }
    }
}
