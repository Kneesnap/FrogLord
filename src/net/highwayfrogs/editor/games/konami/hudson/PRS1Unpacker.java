package net.highwayfrogs.editor.games.konami.hudson;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Contains PRS1 compression utilities.
 * Created by Kneesnap on 6/7/2020.
 */
public class PRS1Unpacker {
    private static final String MAGIC = "PRS1";

    /**
     * Decompresses data compressed with PRS1 compression.
     * @param compressedData The compressed data.
     * @return uncompressedData
     */
    public static byte[] decompressPRS1(byte[] compressedData) {
        if (compressedData == null)
            throw new RuntimeException("Cannot decompress null PRS1 data.");
        if (!isCompressedPRS1(compressedData))
            throw new RuntimeException("Cannot decompress data which is not in the PRS1 format!");

        DataReader reader = new DataReader(new ArraySource(compressedData));
        reader.verifyString(MAGIC);
        int uncompressedSize = reader.readInt();
        int compressedSize = reader.readInt();

        return decompressData(reader, compressedSize, uncompressedSize);
    }

    /**
     * Decompresses raw PRS1 data.
     * Reverse engineered from "Frogger's Adventures: The Rescue" PC at 0x00540358.
     * This algorithm was also found in the Mario Party 4 decomp and likely exists in more games too.
     * @param reader         The reader to read data from.
     * @param compressedSize The size of the compressed data.
     * @return decompressedData
     */
    public static byte[] decompressData(DataReader reader, int compressedSize, int uncompressedSize) {
        byte[] outputBuffer = new byte[uncompressedSize];
        int outPos = 0;

        byte[] compressionBuffer = new byte[4096]; // The original buffer is most likely 4096 bytes large.
        int bufferPos = 0xFEE;
        int temp = 0;

        while (true) {
            temp >>= 1;

            if ((temp & 0x100) == 0) {
                if (compressedSize == 0)
                    return outputBuffer;

                compressedSize--;
                temp = 0xFF00 | (reader.hasMore() ? (reader.readByte() & 0xFF) : 0);
            }

            if (compressedSize == 0)
                return outputBuffer;

            byte currentByte = (reader.hasMore() ? reader.readByte() : Constants.NULL_BYTE);
            if ((temp & 1) == 0) {
                if (compressedSize == 1)
                    return outputBuffer;

                compressedSize -= 2;
                short readOffset = reader.hasMore() ? reader.readUnsignedByteAsShort() : 0;

                for (int i = 0; i <= (readOffset & 0x0F) + 2; i++) {
                    byte copy = compressionBuffer[(i + ((currentByte & 0xFF) | ((readOffset & 0xF0) << 4))) & 0xFFF];
                    if (outPos < uncompressedSize)
                        outputBuffer[outPos] = copy;
                    outPos++;
                    compressionBuffer[bufferPos] = copy;
                    bufferPos = (bufferPos + 1) & 0xFFF;
                }
            } else {
                if (outPos < uncompressedSize)
                    outputBuffer[outPos] = currentByte;

                outPos++;
                compressionBuffer[bufferPos] = currentByte;
                bufferPos = (bufferPos + 1) & 0xFFF;
                compressedSize--;
            }
        }
    }

    /**
     * Test if given data is compressed PRS1 data.
     * @param data The data to test.
     * @return isCompressedData.
     */
    public static boolean isCompressedPRS1(byte[] data) {
        return data != null && data.length > 12 && Utils.testSignature(data, MAGIC);
    }
}