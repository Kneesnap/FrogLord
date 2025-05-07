package net.highwayfrogs.editor.utils;

import net.highwayfrogs.editor.Constants;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Contains utilities for managing data.
 * Created by Kneesnap on 10/25/2024.
 */
public class DataUtils {
    private static final CRC32 crc32 = new CRC32();
    private static final ByteBuffer FLOAT_BUFFER = ByteBuffer.allocate(Constants.FLOAT_SIZE).order(ByteOrder.LITTLE_ENDIAN);

    /**
     * Convert a byte array to a number.
     * @param data The data to turn into a number.
     * @return intValue
     */
    public static int readNumberFromBytes(byte[] data) {
        return readNumberFromBytes(data, data.length, 0);
    }

    /**
     * Convert a byte array to a number.
     * @param data The data to turn into a number.
     * @return intValue
     */
    public static int readNumberFromBytes(byte[] data, int readSize, int startIndex) {
        long value = 0;
        for (int i = 0; i < readSize; i++)
            value += ((long) data[startIndex + i] & 0xFFL) << (Constants.BITS_PER_BYTE * i);
        return (int) value;
    }

    /**
     * Convert a byte array to a number.
     * @param data The data to turn into a number.
     * @return intValue
     */
    public static int readIntFromBytes(byte[] data, int startIndex) {
        return readNumberFromBytes(data, Constants.INTEGER_SIZE, startIndex);
    }

    /**
     * Read a float from a byte array.
     * @param data The data to read the float from.
     * @return floatValue
     */
    public static float readFloatFromBytes(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Turn a float into a byte array, and return it.
     * @param value The float to convert.
     * @return byteArray
     */
    public static byte[] writeFloatToBytes(float value) {
        FLOAT_BUFFER.clear();
        return Arrays.copyOf(FLOAT_BUFFER.order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array(), Constants.FLOAT_SIZE);
    }

    /**
     * Turn an integer into a byte array.
     * @param value The integer to turn into a byte array.
     * @return byteArray
     */
    public static byte[] toByteArray(int value) {
        byte[] bytes = new byte[Constants.INTEGER_SIZE];
        bytes[0] = (byte) value;
        bytes[1] = (byte) ((value >> 8) & 0xFF);
        bytes[2] = (byte) ((value >> 16) & 0xFF);
        bytes[3] = (byte) ((value >> 24) & 0xFF);
        return bytes;
    }

    /**
     * Turn a byte array into a readable byte string.
     * @param array The array to convert into a string.
     * @return byteString
     */
    public static String toByteString(byte[] array) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < array.length; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(toByteString(array[i]));
        }
        return sb.append("}").toString();
    }

    /**
     * Turn a single byte into a hex string.
     * @param value The byte to convert.
     * @return byteStr
     */
    public static String toByteString(byte value) {
        String str = String.format("%x", value);
        if (str.length() == 1)
            str = "0" + str;
        return str;
    }

    /**
     * Writes a byte as a text value to the string builder.
     * @param builder The builder to write to.
     * @param value   The value to write.
     */
    public static void writeByteAsText(StringBuilder builder, byte value) {
        if (value == 0x0A) {
            builder.append("\\n");
        } else if (value == 0x0D) {
            builder.append("\\r");
        } else if (value == 0x5C) {
            builder.append("\\\\"); // Write an escaped backslash to avoid confusion.
        } else if (value >= 0x20 && value <= 0x7E) {
            builder.append((char) value);
        } else {
            builder.append('\\').append(toByteString(value));
        }
    }

    /**
     * Convert an unsigned byte into a float 0-1.
     * @param unsignedByte The byte to convert to a float. 0x00 = 0.00. 0xFF = 1.000
     * @return floatValue
     */
    public static float unsignedByteToFloat(byte unsignedByte) {
        float num = unsignedByte;
        if (num < 0)
            num += 256;
        return num / 0xFF;
    }

    /**
     * Convert a float 0-1 to a byte.
     * @param floatValue The float to convert to a byte.
     * @return byteValue
     */
    public static byte floatToByte(float floatValue) {
        short small = (short) Math.round(floatValue * 0xFF);
        return small > 0x7F ? ((byte) -(0x100 - small)) : (byte) small;
    }

    /**
     * Convert a short value (fixed point, n fractional bits) into a float.
     * @param shortVal The short to convert.
     * @return floatValue
     */
    public static float fixedPointShortToFloat4Bit(short shortVal) {
        return fixedPointShortToFloatNBits(shortVal, 4);
    }

    /**
     * Convert a short value (fixed point, n fractional bits) into a float.
     * @param shortVal The short to convert.
     * @return floatValue
     */
    public static float fixedPointShortToFloat12Bit(short shortVal) {
        return fixedPointShortToFloatNBits(shortVal, 12);
    }

    /**
     * Convert a short value (fixed point, n fractional bits) into a float.
     * The inverse of the returned value is guaranteed to be calculable back into shortVal.
     * @param shortVal The short to convert.
     * @param n        The number of fractional bits.
     * @return floatValue
     */
    public static float fixedPointShortToFloatNBits(short shortVal, long n) {
        return ((float) shortVal / (1 << n));
    }

    /**
     * Convert a float value into a short (fixed point, n fractional bits).
     * @param floatVal The float to convert.
     * @param n        The number of fractional bits.
     * @return shortValue
     */
    public static short floatToFixedPointShort(float floatVal, int n) {
        if (!Float.isFinite(floatVal))
            throw new IllegalArgumentException("Cannot represent " + floatVal + " as a fixed-point value!");

        int fixedPtValue = (int) (floatVal * (1 << n));
        if (fixedPtValue < Short.MIN_VALUE || fixedPtValue > Short.MAX_VALUE) {
            final int wholeNumberBitCount = (Constants.SHORT_SIZE * Constants.BITS_PER_BYTE) - n - 1;
            throw new IllegalArgumentException("Cannot represent " + floatVal + " as a 1." + wholeNumberBitCount + "." + n + " fixed-point value!");
        }

        return (short) fixedPtValue;
    }

    /**
     * Convert a float value into a short (fixed point, n fractional bits).
     * @param floatVal The float to convert.
     * @return shortValue
     */
    public static short floatToFixedPointShort4Bit(float floatVal) {
        return floatToFixedPointShort(floatVal, 4);
    }

    /**
     * Convert a float value into a short (fixed point, n fractional bits).
     * @param floatVal The float to convert.
     * @return shortValue
     */
    public static short floatToFixedPointShort12Bit(float floatVal) {
        return floatToFixedPointShort(floatVal, 12);
    }

    /**
     * Convert an int value (fixed point, n fractional bits) into a float.
     * @param intVal The integer to convert.
     * @param n      The number of fractional bits.
     * @return floatValue
     */
    public static float fixedPointIntToFloatNBits(int intVal, long n) {
        return ((float) intVal / (float) (1 << n));
    }

    /**
     * Convert an int value (fixed point, n fractional bits) into a float.
     * @param intVal The integer to convert.
     * @param n      The number of fractional bits.
     * @return floatValue
     */
    public static double fixedPointIntToFloatNBits(long intVal, long n) {
        return ((double) intVal / (double) (1L << n));
    }

    /**
     * Convert an int value (fixed point, 4 fractional bits) into a float.
     * @param intVal The integer to convert.
     * @return floatValue
     */
    public static float fixedPointIntToFloat4Bit(int intVal) {
        return fixedPointIntToFloatNBits(intVal, 4);
    }

    /**
     * Convert a float value into a int (fixed point, n fractional bits).
     * @param floatVal The float to convert.
     * @param n        The number of fractional bits.
     * @return intValue
     */
    public static int floatToFixedPointInt(float floatVal, int n) {
        if (!Float.isFinite(floatVal))
            throw new IllegalArgumentException("Cannot represent " + floatVal + " as a fixed-point value!");
        final int wholeNumberBitCount = (Constants.INTEGER_SIZE * Constants.BITS_PER_BYTE) - n - 1;
        if (floatVal >= (1 << wholeNumberBitCount) || floatVal < -(1 << wholeNumberBitCount))
            throw new IllegalArgumentException("Cannot represent " + floatVal + " as a 1." + wholeNumberBitCount + "." + n + " fixed-point value!");

        return (int) (floatVal * (float) (1 << n));
    }

    /**
     * Convert a float value into a int (fixed point, 4 fractional bits).
     * @param floatVal The float to convert.
     * @return intValue
     */
    public static int floatToFixedPointInt4Bit(float floatVal) {
        return floatToFixedPointInt(floatVal, 4);
    }

    /**
     * Convert an unsigned byte into a short, which can be converted back into a byte.
     * @param unsignedByte The byte to convert into a short.
     * @return unsignedShort
     */
    public static short byteToUnsignedShort(byte unsignedByte) {
        short num = unsignedByte;
        if (num < 0)
            num += 256;
        return num;
    }

    /**
     * Convert an unsigned short back into an unsigned byte.
     * @param unsignedShort the short to turn back into a byte.
     * @return byte
     */
    public static byte unsignedShortToByte(short unsignedShort) {
        if (unsignedShort < 0 || unsignedShort > 0xFF)
            throw new RuntimeException("The provided short value is outside the range of an unsigned byte. [0,255]. Value: " + unsignedShort);
        return (byte) unsignedShort;
    }

    /**
     * Convert an unsigned short into an int, which can be converted back into a short.
     * @param unsignedShort The short to convert into an int.
     * @return unsignedShort
     */
    public static int shortToUnsignedInt(short unsignedShort) {
        int num = unsignedShort;
        if (num < 0)
            num += 65536;
        return num;
    }

    /**
     * Convert an unsigned int back into an unsigned short.
     * @param unsignedInt the int to turn back into a short.
     * @return byte
     */
    public static short unsignedIntToShort(int unsignedInt) {
        if (unsignedInt < 0 || unsignedInt > 0xFFFF)
            throw new RuntimeException("The provided short value is outside the range of an unsigned byte. [0,65535]. Value: " + unsignedInt);
        return (short) unsignedInt;
    }

    /**
     * Convert an unsigned int into a long, which can be converted back into a long.
     * @param unsignedInt The int to convert into a long.
     * @return unsignedLong
     */
    public static long intToUnsignedLong(int unsignedInt) {
        long num = unsignedInt;
        if (num < 0) {
            num += 0xFFFFFFFFL;
            num++;
        }
        return num;
    }

    /**
     * Convert an unsigned long back into an unsigned int.
     * @param unsignedLong the long to turn back into an int.
     * @return int
     */
    public static int unsignedLongToInt(long unsignedLong) {
        if (unsignedLong < 0 || unsignedLong > 0xFFFFFFFFL)
            throw new RuntimeException("The provided short value is outside the range of an unsigned byte. [0,0xFFFFFFFF]. Value: " + unsignedLong);
        return (int) unsignedLong;
    }

    /**
     * Reverse a byte array.
     * @param array The array to reverse
     * @return array The input array.
     */
    public static byte[] reverseByteArray(byte[] array) {
        for (int i = 0; i < array.length / 2; i++) { // Reverse the byte order.
            byte temp = array[i];
            array[i] = array[array.length - 1 - i];
            array[array.length - 1 - i] = temp;
        }
        return array;
    }

    /**
     * Get the CRC32 hash of a file.
     * @param file The file to get the hash of.
     * @return crc32Hash
     */
    public static long getCRC32(File file) throws IOException {
        return getCRC32(Files.readAllBytes(file.toPath()));
    }

    /**
     * Get the CRC32 hash of a byte array.
     * @param bytes The bytes to get the hash of.
     * @return crc32Hash
     */
    public static long getCRC32(byte[] bytes) {
        crc32.reset();
        crc32.update(bytes);
        return crc32.getValue();
    }

    /**
     * Test if a string is present at a given index in a byte array.
     * @param data The array to check.
     * @param test The string to test against.
     * @return hasSignature
     */
    public static boolean testSignature(byte[] data, String test) {
        return testSignature(data, 0, test.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Test if a string is present at a given index in a byte array.
     * @param data       The array to check.
     * @param startIndex The index into that array.
     * @param test       The string to test against.
     * @return hasSignature
     */
    public static boolean testSignature(byte[] data, int startIndex, String test) {
        return testSignature(data, startIndex, test.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Test if a file signature matches bytes.
     * @param data The data to test.
     * @param test The signature to test against.
     * @return hasSignature
     */
    public static boolean testSignature(byte[] data, byte[] test) {
        return testSignature(data, 0, test);
    }

    /**
     * Test if a file signature matches bytes.
     * @param data       The data to test.
     * @param startIndex The index to start testing at.
     * @param test       The signature to test against.
     * @return hasSignature
     */
    public static boolean testSignature(byte[] data, int startIndex, byte[] test) {
        if (data == null || test.length > data.length)
            return false;

        for (int i = 0; i < test.length; i++)
            if (data[startIndex + i] != test[i])
                return false;
        return true;
    }

    /**
     * Swaps the byte-order (endian) of the given value.
     * @param value the value to swap the byte-order for
     * @return valueWithSwappedByteOrder
     */
    public static short swapShortByteOrder(short value) {
        int lowByte = (value & 0xFF);
        int highByte = (value & 0xFF00) >> 8;
        return (short) ((lowByte << 8) | highByte);
    }
}
