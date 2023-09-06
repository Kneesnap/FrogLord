package net.highwayfrogs.editor.utils;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.stage.*;
import javafx.stage.Window;
import javafx.stage.FileChooser.ExtensionFilter;
import lombok.Cleanup;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.gui.GUIMain;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

/**
 * Some static utilities.
 * Created by Kneesnap on 8/12/2018.
 */
@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "unused"})
public class Utils {
    private static final ByteBuffer INT_BUFFER = ByteBuffer.allocate(Constants.INTEGER_SIZE);
    private static final ByteBuffer FLOAT_BUFFER = ByteBuffer.allocate(Constants.FLOAT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    private static final CRC32 crc32 = new CRC32();
    private static final File[] EMPTY_FILE_ARRAY = new File[0];
    private static final Map<BufferedImage, TextureCache> imageCacheMap = new HashMap<>();
    private static final Map<Color, Image> colorImageCacheMap = new HashMap<>();
    private static final Map<Color, java.awt.Color> awtColorCacheMap = new HashMap<>();
    private static final long IMAGE_CACHE_EXPIRE = TimeUnit.MINUTES.toMillis(5);
    private static final Map<Integer, List<Integer>> integerLists = new HashMap<>();

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
        int value = 0;
        for (int i = 0; i < readSize; i++)
            value += ((long) data[startIndex + i] & 0xFFL) << (Constants.BITS_PER_BYTE * i);
        return value;
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
        FLOAT_BUFFER.putFloat(value);
        return FLOAT_BUFFER.array();
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
     * Verify a condition is true, otherwise throw an exception.
     * @param condition The condition to verify is true.
     * @param error     The error message if false.
     */
    public static void verify(boolean condition, String error) {
        if (!condition)
            throw new RuntimeException(error);
    }

    /**
     * Verify a condition is true, otherwise throw an exception.
     * @param condition  The condition to verify is true.
     * @param error      The error message if false.
     * @param formatting Formatting to apply to the error message.
     */
    public static void verify(boolean condition, String error, Object... formatting) {
        if (!condition)
            throw new RuntimeException(formatting.length > 0 ? String.format(error, formatting) : error);
    }

    /**
     * Create a new array with less elements than the supplied one. (Cut from the left side)
     * @param array    The array to take elements from.
     * @param cutCount The amount of elements to cut.
     * @return newArray
     */
    public static <T> T[] cutElements(T[] array, int cutCount) {
        return Arrays.copyOfRange(array, cutCount, array.length);
    }

    /**
     * Converts a number to a bit array.
     * Example Input:
     * value = 00101100
     * bitCount = 6
     * Output: 101100 or {1, 0, 1, 1, 0, 0}
     * @param value    The value to get bits from.
     * @param bitCount The amount
     * @return bits
     */
    public static int[] getBits(int value, int bitCount) {
        int[] bits = new int[bitCount];
        for (int i = 0; i < bits.length; i++)
            bits[bits.length - i - 1] = (value >> i) & Constants.BIT_TRUE;
        return bits;
    }

    /**
     * Reverse the order of elements in an array.
     * @param array The array to swap bits of.
     * @return sameArray
     */
    public static <T> T[] reverse(T[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            T temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
        return array;
    }

    /**
     * Flip a bit.
     * @param bit The bit to flip.
     * @return flippedBit
     */
    public static int flipBit(int bit) {
        return bit == Constants.BIT_TRUE ? Constants.BIT_FALSE : Constants.BIT_TRUE;
    }

    /**
     * Get a bit value.
     * @param value The bit to convert.
     * @return bit
     */
    public static int getBit(boolean value) {
        return value ? Constants.BIT_TRUE : Constants.BIT_FALSE;
    }

    /**
     * Get a bit value.
     * @param bit The bit to convert.
     * @return value
     */
    public static boolean getBit(int bit) {
        if (bit == Constants.BIT_TRUE)
            return true;

        if (bit == Constants.BIT_FALSE)
            return false;

        throw new RuntimeException("Invalid bit-value: " + bit);
    }

    /**
     * Turn a list of bytes into a byte array.
     * @param list The list of bytes.
     * @return byteArray
     */
    public static byte[] toArray(List<Byte> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++)
            bytes[i] = list.get(i);
        return bytes;
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
        short small = (short) (floatValue * 0xFF);
        return small > 0x7F ? ((byte) -(0x100 - small)) : (byte) small;
    }

    /**
     * Convert an unsigned short into a float 0-1.
     * @param unsignedShort The short to convert.
     * @return floatValue
     */
    public static float unsignedShortToFloat(short unsignedShort) {
        // [AndyEder] - be careful if you use this function... it might not return the values you expect...
        return (float) unsignedShort / (float) Short.MAX_VALUE;
    }

    /**
     * Convert an unsigned integer into a float 0-1.
     * @param unsignedInt The integer to convert.
     * @return floatValue
     */
    public static float unsignedIntToFloat(long unsignedInt) {
        // [AndyEder] - be careful if you use this function... it might not return the values you expect...
        return (float) unsignedInt / (float) Integer.MAX_VALUE;
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
        return (short) (floatVal * (1 << n));
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
     * Convert an unsigned short back into a unsigned byte.
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
     * @param unsignedShort The short to convert into a int.
     * @return unsignedShort
     */
    public static int shortToUnsignedInt(short unsignedShort) {
        int num = unsignedShort;
        if (num < 0)
            num += 65536;
        return num;
    }

    /**
     * Convert an unsigned int back into a unsigned short.
     * @param unsignedInt the int to turn back into a short.
     * @return byte
     */
    public static short unsignedIntToShort(int unsignedInt) {
        if (unsignedInt < 0 || unsignedInt > 0xFFFF)
            throw new RuntimeException("The provided short value is outside the range of an unsigned byte. [0,65535]. Value: " + unsignedInt);
        return (short) unsignedInt;
    }

    /**
     * Convert an unsigned int into an long, which can be converted back into a long.
     * @param unsignedInt The int to convert into a long.
     * @return unsignedLong
     */
    public static long intToUnsignedLong(int unsignedInt) {
        long num = unsignedInt;
        if (num < 0) {
            num += Integer.MAX_VALUE;
            num++;
        }
        return num;
    }

    /**
     * Convert an unsigned long back into a unsigned int.
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
     * Get a resource in the JAR.
     * @param resourceName The resource name.
     * @return resourceURL
     */
    public static URL getResource(String resourceName) {
        return Utils.class.getClassLoader().getResource(resourceName);
    }

    /**
     * Get a JAR resource as a stream.
     * @param resourceName The name of the resource to load.
     * @return resourceStream
     */
    public static InputStream getResourceStream(String resourceName) {
        return Utils.class.getClassLoader().getResourceAsStream(resourceName);
    }

    /**
     * Read lines of text from an InputStream
     * @param stream The stream to read from.
     * @return lines
     */
    @SneakyThrows
    public static List<String> readLinesFromStream(InputStream stream) {
        @Cleanup InputStreamReader reader = new InputStreamReader(stream);
        @Cleanup BufferedReader bufferedReader = new BufferedReader(reader);
        return bufferedReader.lines().collect(Collectors.toList());
    }

    /**
     * Read lines of text from a file.
     * @param file The file to read from.
     * @return fileText
     */
    public static List<String> readLinesFromFile(File file) {
        try {
            return Files.readAllLines(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Read bytes from an InputStream
     * Stolen from sun.nio.ch
     * @param stream The stream to read from.
     * @return lines
     */
    @SneakyThrows
    public static byte[] readBytesFromStream(InputStream stream) {
        byte[] output = new byte[0];
        int var1 = Integer.MAX_VALUE;

        int var6;
        for (int var4 = 0; var4 < var1; var4 += var6) {
            int var5;
            if (var4 >= output.length) {
                var5 = Math.min(var1 - var4, output.length + 1024);
                if (output.length < var4 + var5) {
                    output = Arrays.copyOf(output, var4 + var5);
                }
            } else {
                var5 = output.length - var4;
            }

            var6 = stream.read(output, var4, var5);
            if (var6 < 0) {
                if (output.length != var4)
                    output = Arrays.copyOf(output, var4);
                break;
            }
        }

        return output;
    }

    /**
     * Read bytes from an InputStream, and writes them to an output stream.
     * @param input  The stream to read from.
     * @param output The stream to write to.
     */
    public static void copyInputStreamData(InputStream input, OutputStream output, boolean closeInput) {
        byte[] buffer = new byte[4096];

        try {
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1)
                output.write(buffer, 0, bytesRead);
        } catch (IOException ex) {
            System.out.println("Failed to copy stream data from the input stream to the output stream!");
            ex.printStackTrace();
        }

        if (closeInput) {
            try {
                input.close();
            } catch (IOException ex) {
                System.out.println("Failed to close the input stream.");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Get the first file with this name that does not exist. Appends stuff like (1).
     * @param file The file to get.
     * @return nonexistantFile.
     */
    public static File getNonExistantFile(File file) {
        if (!file.exists())
            return file;

        int id = 0;

        File result = file;
        while (result.exists())
            result = getFile(file, ++id);

        return result;
    }

    private static File getFile(File file, int id) {
        if (id == 0)
            return file;

        String fileName = file.getName();
        String name = fileName.replaceFirst("[.][^.]+$", ""); // Remove extension.
        name += " (" + id + ")";
        if (fileName.contains("."))
            name += fileName.substring(fileName.lastIndexOf(".") + 1);

        return new File(file.getParentFile(), name);
    }

    /**
     * Resizes an image.
     * @param img    The image to resize.
     * @param width  The new width.
     * @param height The new height.
     * @return resized
     */
    public static BufferedImage resizeImage(BufferedImage img, int width, int height) {
        BufferedImage newImage = new BufferedImage(width, height, img.getType());
        Graphics2D graphics = newImage.createGraphics();
        graphics.drawImage(img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();

        return newImage;
    }

    /**
     * Raise x to the y. Math.pow is pretty intensive, so we do this instead.
     * @param x The base.
     * @param y The exponent.
     * @return value
     */
    public static int power(int x, int y) {
        int result = 1;
        while (y-- > 0)
            result *= x;
        return result;
    }

    /**
     * Get a byte value unsigned, as an integer.
     * @param value The value to turn into an integer.
     * @return unsignedInt
     */
    public static int getUnsignedByte(byte value) {
        return value >= 0 ? value : Byte.MAX_VALUE - value;
    }

    /**
     * Strip the extension from a file name.
     * @param name The file name.
     * @return stripped
     */
    public static String stripExtension(String name) {
        return name.split("\\.")[0];
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
     * Strip the extension and windows 95 from a file name.
     * @param name The file name.
     * @return stripped
     */
    public static String stripExtensionWin95(String name) {
        return stripWin95(stripExtension(name));
    }

    /**
     * Deletes a file.
     * @param file The file to delete.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteFile(File file) {
        if (file.exists())
            file.delete();
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
     * Test if a string is an integer.
     * @param str The string to test.
     * @return isInteger
     */
    public static boolean isInteger(String str) {
        if (str == null || str.isEmpty() || str.equals("-"))
            return false;

        int startIndex = str.startsWith("-") ? 1 : 0;
        for (int i = startIndex; i < str.length(); i++)
            if (Character.digit(str.charAt(i), 10) < 0)
                return false;
        return true;
    }

    /**
     * Test if a string is a hex number.
     * @param str The string to test.
     * @return isHexInteger
     */
    public static boolean isHexInteger(String str) {
        if (!str.startsWith("0x"))
            return false;

        for (int i = 2; i < str.length(); i++) {
            char temp = str.charAt(i);
            boolean isHex = (temp >= '0' && temp <= '9') || (temp >= 'a' && temp <= 'f') || (temp >= 'A' && temp <= 'F');
            if (!isHex)
                return false;
        }

        return true;
    }

    /**
     * Test if a string is a signed short.
     * @param str The string to test.
     * @return isSignedShort
     */
    public static boolean isSignedShort(String str) {
        if (!Utils.isInteger(str))
            return false;

        int intTest = Integer.parseInt(str);
        return intTest >= Short.MIN_VALUE && intTest <= Short.MAX_VALUE;
    }

    /**
     * Test if a string is an unsigned byte.
     * @param str The string to test.
     * @return isUnsignedByte
     */
    public static boolean isUnsignedByte(String str) {
        if (!Utils.isInteger(str))
            return false;

        int intTest = Integer.parseInt(str);
        return intTest >= 0 && intTest <= 0xFF;
    }

    /**
     * Is the input string a valid integer or decimal number?
     * @param input The input to test.
     * @return isNumber
     */
    public static boolean isNumber(String input) {
        boolean hasDecimal = false;
        for (int i = 0; i < input.length(); i++) {
            char test = input.charAt(i);
            if (test == '-' && i == 0)
                continue; // Allow negative indicator.

            if (test == '.') {
                if (!hasDecimal) {
                    hasDecimal = true;
                    continue;
                } else {
                    return false; // Multiple decimal = invalid number.
                }
            }

            if (!Character.isDigit(test))
                return false; // Character isn't a digit, so it can't be a number.
        }

        return true;
    }

    /**
     * Prompt the user to select a file.
     * @param title      The title of the window to display.
     * @param typeInfo   The label to show for the file-type.
     * @param extensions Allowed extensions.
     * @return selectedFile, Can be null.
     */
    public static File promptFileOpenExtensions(String title, String typeInfo, String... extensions) {
        Utils.verify(extensions.length > 0, "");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        List<String> allExtensions = new ArrayList<>();
        for (String ext : extensions) {
            String type = "*." + ext; // Unix is case-sensitive, so we add both lower-case and upper-case.
            String lowerCase = type.toLowerCase();
            String upperCase = type.toUpperCase();

            if (lowerCase.equals(upperCase)) {
                allExtensions.add(type);
            } else {
                allExtensions.add(lowerCase);
                allExtensions.add(upperCase);
            }
        }
        fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, allExtensions));

        fileChooser.setInitialDirectory(getValidFolder(GUIMain.getWorkingDirectory()));

        File selectedFile = fileChooser.showOpenDialog(GUIMain.MAIN_STAGE);
        if (selectedFile != null)
            GUIMain.setWorkingDirectory(selectedFile.getParentFile());

        return selectedFile;
    }

    /**
     * Prompt the user to select a file.
     * @param title     The title of the window to display.
     * @param typeInfo  The label to show for the file-type.
     * @param extension Allowed extension.
     * @return selectedFile, Can be null.
     */
    public static File promptFileOpen(String title, String typeInfo, String extension) {
        return promptFileOpenExtensions(title, typeInfo, extension);
    }

    /**
     * Prompt the user to save a file.
     * @param title       The title of the window to display.
     * @param suggestName The initial name to suggest saving the file as.
     * @param typeInfo    The label to show for the file-type.
     * @param extension   Allowed extension.
     * @return selectedFile, Can be null.
     */
    public static File promptFileSave(String title, String suggestName, String typeInfo, String extension) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        if (extension != null) {
            String type = "*." + extension; // Unix is case-sensitive, so we add both lower-case and upper-case.

            String lowerCase = type.toLowerCase();
            String upperCase = type.toUpperCase();

            if (lowerCase.equals(upperCase)) {
                fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, type));
            } else {
                fileChooser.getExtensionFilters().add(new ExtensionFilter(typeInfo, lowerCase, upperCase));
            }
        }

        fileChooser.setInitialDirectory(getValidFolder(GUIMain.getWorkingDirectory()));
        if (suggestName != null) {
            String initialName = suggestName;
            if (extension != null && !extension.equals("*"))
                initialName += "." + extension;

            fileChooser.setInitialFileName(initialName);
        }

        File selectedFile = fileChooser.showSaveDialog(GUIMain.MAIN_STAGE);
        if (selectedFile != null)
            GUIMain.setWorkingDirectory(selectedFile.getParentFile());

        return selectedFile;
    }

    /**
     * Find the valid folder, for instance maybe the file was deleted.
     * @param folder The folder to get a valid one from.
     * @return validFolder
     */
    public static File getValidFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory())
            return folder;

        return folder != null ? getValidFolder(folder.getParentFile()) : new File("./");
    }

    /**
     * Prompt the user to select a directory.
     * @param title         The title of the window.
     * @param saveDirectory Should this directory be saved as the current directory?
     * @return directoryFile
     */
    public static File promptChooseDirectory(String title, boolean saveDirectory) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(getValidFolder(GUIMain.getWorkingDirectory()));

        File selectedFolder = chooser.showDialog(GUIMain.MAIN_STAGE);
        if (selectedFolder != null && saveDirectory)
            GUIMain.setWorkingDirectory(selectedFolder);

        return selectedFolder;
    }

    /**
     * A null-safe way of reading files from a directory.
     * @param directory The directory to read files from.
     * @return readFiles
     */
    public static File[] listFiles(File directory) {
        verify(directory.isDirectory(), "This is not a directory!");
        File[] files = directory.listFiles();
        return files != null ? files : EMPTY_FILE_ARRAY;
    }

    /**
     * Create the directory.
     * @param directory The directory to create.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void makeDirectory(File directory) {
        if (!directory.exists())
            directory.mkdirs();
    }

    /**
     * Load a FXML template as a new window.
     * WARNING: This method is blocking.
     * @param template   The name of the template to load. Should not be user-controllable, as there is no path sanitization.
     * @param title      The title of the window to show.
     * @param controller Makes the window controller.
     */
    @SneakyThrows
    public static <T> void loadFXMLTemplate(String template, String title, Function<Stage, T> controller) {
        loadFXMLTemplate(template, title, controller, null);
    }

    /**
     * Gets the FXMLLoader by its name.
     * @param template The template name.
     * @return loader
     */
    public static FXMLLoader getFXMLLoader(String template) {
        return new FXMLLoader(getResource("javafx/" + template + ".fxml"));
    }

    /**
     * Load a FXML template as a new window.
     * WARNING: This method is blocking.
     * @param template   The name of the template to load. Should not be user-controllable, as there is no path sanitization.
     * @param title      The title of the window to show.
     * @param controller Makes the window controller.
     */
    @SneakyThrows
    public static <T> void loadFXMLTemplate(String template, String title, Function<Stage, T> controller, BiConsumer<Stage, T> consumer) {
        FXMLLoader loader = getFXMLLoader(template);

        Stage newStage = new Stage();
        newStage.setTitle(title);

        T controllerObject = controller.apply(newStage);
        loader.setController(controllerObject);

        Parent rootNode = loader.load();
        newStage.setScene(new Scene(rootNode));
        newStage.setResizable(false);

        if (consumer != null)
            consumer.accept(newStage, controllerObject);

        newStage.initModality(Modality.WINDOW_MODAL);
        newStage.initOwner(GUIMain.MAIN_STAGE);
        newStage.getIcons().add(GUIMain.NORMAL_ICON);
        newStage.showAndWait();
    }

    /**
     * Make a given stage close when the escape key is pressed.
     * @param stage   The stage to apply.
     * @param onClose Behavior to run when the escape key is pressed.
     */
    public static void closeOnEscapeKey(Stage stage, Runnable onClose) {
        closeOnEscapeKey(stage, onClose, true);
    }

    private static void closeOnEscapeKey(Stage stage, Runnable onClose, boolean firstTime) {
        Scene scene = stage.getScene();
        if (scene == null || !Platform.isFxApplicationThread()) {
            if (firstTime)
                Platform.runLater(() -> closeOnEscapeKey(stage, onClose, false));
            return;
        }

        Utils.verify(scene.getOnKeyPressed() == null, "Scene already has a key-press listener!");
        scene.setOnKeyPressed(evt -> {
            if (evt.getCode() == KeyCode.ESCAPE) {
                if (onClose != null)
                    onClose.run();
                stage.close();
            }
        });
    }

    /**
     * Switch the Stage's scene without changing window size.
     * @param stage    The stage that should own the scene.
     * @param newScene The new scene to use.
     * @return oldScene
     */
    public static Scene setSceneKeepPosition(Stage stage, Scene newScene) {
        Scene oldScene = stage.getScene();

        Window oldWindow = oldScene.getWindow();
        double width = oldWindow.getWidth();
        double height = oldWindow.getHeight();
        double x = oldWindow.getX();
        double y = oldWindow.getY();

        stage.setScene(newScene); // Exit the viewer.

        // Maintain the position the viewer Scene was at when it was closed.
        Window newWindow = newScene.getWindow();
        newWindow.setX(x);
        newWindow.setY(y);
        newWindow.setWidth(width);
        newWindow.setHeight(height);

        return oldScene;
    }

    /**
     * Get the raw file name without an extension.
     * @param fileName The file name to get raw.
     * @return rawFileName
     */
    public static String getRawFileName(String fileName) {
        return stripExtension(fileName).toUpperCase();
    }

    /**
     * Turn an integer into a hex string.
     * 255 -> 0xFF
     * @param value The value to convert.
     * @return hexString
     */
    public static String toHexString(int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    /**
     * Turn a long into a hex string.
     * 255 -> 0xFF
     * @param value The value to convert.
     * @return hexString
     */
    public static String toHexString(long value) {
        return "0x" + Long.toHexString(value).toUpperCase();
    }

    /**
     * Get the alpha value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorByte
     */
    public static byte getAlpha(int rgb) {
        return (byte) ((rgb >> 24) & 0xFF);
    }

    /**
     * Get the red value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorValue
     */
    public static int getRedInt(int rgb) {
        return getRed(rgb) & 0xFF;
    }

    /**
     * Get the green value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorValue
     */
    public static int getGreenInt(int rgb) {
        return getGreen(rgb) & 0xFF;
    }

    /**
     * Get the blue value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorValue
     */
    public static int getBlueInt(int rgb) {
        return getBlue(rgb) & 0xFF;
    }

    /**
     * Get the red value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorByte
     */
    public static byte getRed(int rgb) {
        return (byte) ((rgb >> 16) & 0xFF);
    }

    /**
     * Get the green value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorByte
     */
    public static byte getGreen(int rgb) {
        return (byte) ((rgb >> 8) & 0xFF);
    }

    /**
     * Get the blue value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorByte
     */
    public static byte getBlue(int rgb) {
        return (byte) (rgb & 0xFF);
    }

    /**
     * Get a Color object from an integer.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromRGB(int rgb) {
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Get a Color object from an integer.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromRGB(int rgb, double alpha) {
        return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }

    /**
     * Get a Color object from an integer.
     * @param rgb The integer to get the color from.
     * @return color
     */
    public static Color fromBGR(int rgb) {
        return Color.rgb(rgb & 0xFF, (rgb >> 8) & 0xFF, (rgb >> 16) & 0xFF);
    }

    /**
     * Get a integer from a color object.
     * @param color The color to turn into rgb.
     * @return rgbInt
     */
    public static int toRGB(Color color) {
        int result = (int) (color.getRed() * 0xFF);
        result = (result << 8) + (int) (color.getGreen() * 0xFF);
        result = (result << 8) + (int) (color.getBlue() * 0xFF);
        return result;
    }

    /**
     * Swaps the red/blue value in ARGB or ABGR.
     * @param color The color to turn into rgb.
     * @return rgbInt
     */
    public static int swapRedBlue(int color) {
        int oldBlue = (color & 0xFF);
        int oldRed = ((color >> 16) & 0xFF);

        int result = color & 0xFF00FF00;
        result |= oldRed;
        result |= ((oldBlue << 16) & 0xFF0000);
        return result;
    }

    /**
     * Get a integer from a color object.
     * @param color The color to turn into rgb.
     * @return rgbInt
     */
    public static int toARGB(Color color) {
        return ((int) (color.getOpacity() * 255) << 24) | toRGB(color);
    }

    /**
     * Get a integer from color bytes.
     * @return rgbInt
     */
    public static int toRGB(byte red, byte green, byte blue) {
        int result = byteToUnsignedShort(red);
        result = (result << 8) + byteToUnsignedShort(green);
        result = (result << 8) + byteToUnsignedShort(blue);
        return result;
    }

    /**
     * Get a integer from a color object.
     * @param color The color to turn into bgr.
     * @return rgbInt
     */
    public static int toBGR(Color color) {
        int result = (int) (color.getBlue() * 0xFF);
        result = (result << 8) + (int) (color.getGreen() * 0xFF);
        result = (result << 8) + (int) (color.getRed() * 0xFF);
        return result;
    }

    /**
     * Turn a matrix into a string.
     * @param matrix The matrix to turn into a string.
     * @return matrixStr
     */
    public static String matrixToString(int[][] matrix) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < matrix.length; i++) {
            if (i > 0)
                sb.append(" ");
            sb.append(Arrays.toString(matrix[i]));
        }

        return sb.append("]").toString();
    }

    /**
     * Capitalize every letter after a space.
     * @param sentence The sentence to capitalize.
     * @return capitalized
     */
    public static String capitalize(String sentence) {
        String[] split = sentence.replaceAll("_", " ").split(" ");
        List<String> out = new ArrayList<>();
        for (String s : split)
            out.add(s.length() > 0 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : "");
        return String.join(" ", out);
    }

    /**
     * Turn a BufferedImage into an FX Image.
     * @param image The image to convert.
     * @return convertedImage
     */
    public static Image toFXImage(BufferedImage image, boolean useCache) {
        imageCacheMap.entrySet().removeIf(entry -> entry.getValue().hasExpired());
        if (!useCache)
            return SwingFXUtils.toFXImage(image, null);

        return imageCacheMap.computeIfAbsent(image, bufferedImage -> new TextureCache(SwingFXUtils.toFXImage(bufferedImage, null))).getImage();
    }

    /**
     * Reads all of the bytes in a file.
     * @param file The file to read bytes from.
     * @return fileBytes
     */
    @SneakyThrows
    public static byte[] readFileBytes(File file) {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Get the square root of a fixed-point integer.
     * @param i The integer to get the square root of.
     * @return squareRoot
     */
    public static int fixedSqrt(int i) {
        return (int) Math.sqrt(i);
    }

    private static class TextureCache {
        private long lastUpdate;
        private final Image fxImage;

        public TextureCache(Image fxImage) {
            this.fxImage = fxImage;
        }

        /**
         * Gets the image.
         */
        public Image getImage() {
            this.lastUpdate = System.currentTimeMillis();
            return fxImage;
        }

        /**
         * Has this image expired?
         * @return hasExpired
         */
        public boolean hasExpired() {
            return (System.currentTimeMillis() - lastUpdate) > IMAGE_CACHE_EXPIRE;
        }
    }

    /**
     * Get an integer list with incrementing values.
     * @param size The size of the list
     * @return integerList
     */
    public static List<Integer> getIntegerList(int size) {
        return integerLists.computeIfAbsent(size, createSize -> {
            List<Integer> newList = new ArrayList<>(createSize);
            for (int i = 0; i < createSize; i++)
                newList.add(i);
            return newList;
        });
    }

    /**
     * Make a combo box scroll to the value it has selected.
     * @param comboBox The box to scroll.
     */
    @SuppressWarnings("unchecked")
    public static <T> void comboBoxScrollToValue(ComboBox<T> comboBox) {
        if (comboBox.getSkin() != null)
            ((ComboBoxListViewSkin<T>) comboBox.getSkin()).getListView().scrollTo(comboBox.getValue());
    }

    /**
     * Set TextField key-press handling.
     * @param field  The TextField to apply to.
     * @param setter Handles text.
     * @param onPass Called if not null and the setter passed.
     */
    public static void setHandleKeyPress(TextField field, Function<String, Boolean> setter, Runnable onPass) {
        field.setOnKeyPressed(evt -> {
            KeyCode code = evt.getCode();
            if (field.getStyle().isEmpty() && (code.isLetterKey() || code.isDigitKey() || code == KeyCode.BACK_SPACE)) {
                field.setStyle("-fx-text-inner-color: darkgreen;");
            } else if (code == KeyCode.ENTER) {
                boolean pass = setter.apply(field.getText());
                if (pass && onPass != null)
                    onPass.run();

                field.setStyle(pass ? null : "-fx-text-inner-color: red;");
            }
        });
    }

    /**
     * Set TextField key-press handling.
     * @param field  The TextField to apply to.
     * @param tester Handles text.
     * @param onPass Called if not null and the setter passed.
     */
    public static void setHandleTestKeyPress(TextField field, Function<String, Boolean> tester, Consumer<String> onPass) {
        setHandleKeyPress(field, value -> {
            if (!tester.apply(value))
                return false;

            onPass.accept(value);
            return true;
        }, null);
    }

    /**
     * Test if a string is present at a given index in a byte array.
     * @param data The array to check.
     * @param test The string to test against.
     * @return hasSignature
     */
    public static boolean testSignature(byte[] data, String test) {
        return testSignature(data, 0, test.getBytes());
    }

    /**
     * Test if a string is present at a given index in a byte array.
     * @param data       The array to check.
     * @param startIndex The index into that array.
     * @param test       The string to test against.
     * @return hasSignature
     */
    public static boolean testSignature(byte[] data, int startIndex, String test) {
        return testSignature(data, startIndex, test.getBytes());
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
        if (test.length > data.length)
            return false;

        for (int i = 0; i < test.length; i++)
            if (data[startIndex + i] != test[i])
                return false;
        return true;
    }

    /**
     * Creates a PhongMaterial with a texture unaffected by lighting.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeSpecialMaterial(Image texture) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.BLACK);
        material.setSpecularColor(Color.BLACK);
        material.setDiffuseMap(texture);
        material.setSelfIlluminationMap(texture);
        return material;
    }

    /**
     * Creates a PhongMaterial with a texture affected by lighting, and using only diffuse components.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeDiffuseMaterial(Image texture) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        material.setSpecularColor(Color.BLACK);
        material.setSpecularPower(0.0);
        material.setDiffuseMap(texture);
        return material;
    }

    /**
     * Creates a PhongMaterial with a color unaffected by lighting.
     * @param color The color to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeSpecialMaterial(Color color) {
        return makeSpecialMaterial(makeColorImage(color));
    }

    /**
     * Creates an image of a solid color.
     * @param color The color to make the image of.
     * @return colorImage
     */
    public static Image makeColorImage(Color color) {
        return colorImageCacheMap.computeIfAbsent(color, key -> {
            BufferedImage colorImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = colorImage.createGraphics();
            graphics.setColor(toAWTColor(key));
            graphics.fillRect(0, 0, colorImage.getWidth(), colorImage.getHeight());
            graphics.dispose();
            return toFXImage(colorImage, true);
        });
    }

    /**
     * Creates an image of a solid color.
     * @param color The color to make the image of.
     * @return colorImage
     */
    public static Image makeColorImageNoCache(Color color, int width, int height) {
        BufferedImage colorImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = colorImage.createGraphics();
        graphics.setColor(toAWTColor(color));
        graphics.fillRect(0, 0, colorImage.getWidth(), colorImage.getHeight());
        graphics.dispose();
        return toFXImage(colorImage, false);
    }

    /**
     * Convert a JavaFX color to an AWT color.
     * @param fxColor The fx color to convert.
     * @return awtColor
     */
    public static java.awt.Color toAWTColor(Color fxColor) {
        return toAWTColor(fxColor, (byte) (int) (fxColor.getOpacity() * 255));
    }

    /**
     * Convert a JavaFX color to an AWT color.
     * @param fxColor The fx color to convert.
     * @return awtColor
     */
    public static java.awt.Color toAWTColor(Color fxColor, byte alpha) {
        return new java.awt.Color((toRGB(fxColor) & 0xFFFFFF) | ((alpha & 0xFF) << 24), true);
    }

    /**
     * Calculate interpolated color value based on 't' between source and target colours.
     * @param colorSrc The source color.
     * @param colorTgt The target color.
     * @param t The desired delta (from 0.0 to 1.0 inclusive).
     * @return Color
     */
    public static Color calculateInterpolatedColour(final Color colorSrc, final Color colorTgt, float t) {
        t = Math.min(t, 1.0f);
        t = Math.max(t, 0.0f);

        final double rSrc = colorSrc.getRed();
        final double gSrc = colorSrc.getGreen();
        final double bSrc = colorSrc.getBlue();
        final double aSrc = colorSrc.getOpacity();

        final double rTgt = colorTgt.getRed();
        final double gTgt = colorTgt.getGreen();
        final double bTgt = colorTgt.getBlue();
        final double aTgt = colorTgt.getOpacity();

        final double rDelta = rTgt - rSrc;
        final double gDelta = gTgt - gSrc;
        final double bDelta = bTgt - bSrc;
        final double aDelta = aTgt - aSrc;

        double rNew = rSrc + (rDelta * t);
        rNew = Math.min(rNew, 1.0f);
        rNew = Math.max(rNew, 0.0f);

        double gNew = gSrc + (gDelta * t);
        gNew = Math.min(gNew, 1.0f);
        gNew = Math.max(gNew, 0.0f);

        double bNew = bSrc + (bDelta * t);
        bNew = Math.min(bNew, 1.0f);
        bNew = Math.max(bNew, 0.0f);

        double aNew = aSrc + (aDelta * t);
        aNew = Math.min(aNew, 1.0f);
        aNew = Math.max(aNew, 0.0f);

        return new Color(rNew, gNew, bNew, aNew);
    }

    /**
     * Calculate bilinear interpolated color value.
     * @param color0 The first color.
     * @param color1 The second color.
     * @param color2 The third color.
     * @param color3 The fourth color.
     * @param tx The desired delta in x (from 0.0 to 1.0 inclusive).
     * @param ty The desired delta in y (from 0.0 to 1.0 inclusive).
     * @return Color
     */
    public static Color calculateBilinearInterpolatedColour(final Color color0, final Color color1, final Color color2, final Color color3, float tx, float ty) {
        final Color colX0 = calculateInterpolatedColour(color0, color1, tx);
        final Color colX1 = calculateInterpolatedColour(color2, color3, tx);
        return calculateInterpolatedColour(colX0, colX1, ty);
    }

    /**
     * Make a popup show up from an exception.
     * @param message The message to display.
     * @param ex      The exception which caused the error.
     */
    public static void makeErrorPopUp(String message, Exception ex, boolean printException) {
        if (printException)
            ex.printStackTrace();
        new Alert(AlertType.ERROR, (message != null && message.length() > 0 ? message + Constants.NEWLINE : "") + "Error: " + ex.getMessage(), ButtonType.OK).showAndWait();
    }

    /**
     * Make a popup show up.
     * @param message The message to display.
     */
    public static void makePopUp(String message, AlertType type) {
        new Alert(type, message, ButtonType.OK).showAndWait();
    }

    /**
     * Make a yes or no popup prompt show up.
     * @param message The message to display.
     */
    public static boolean makePopUpYesNo(String message) {
        return new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO).showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Removes duplicate spaces from a given string.
     * @param toRemove The string to remove spaces from.
     * @return cleanStr
     */
    public static String removeDuplicateSpaces(String toRemove) {
        return replaceDouble(toRemove, ' ').replaceAll("(\\s+)$", ""); // Removes trailing space.
    }

    /**
     * Replace instances of duplicate characters with one of that character.
     * @param str       The string to modify.
     * @param character The character to replace.
     * @return cleanString
     */
    public static String replaceDouble(String str, char character) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char temp = str.charAt(i);
            output.append(temp);
            if (temp == character) // Skip the double characters.
                while (str.length() > i + 1 && str.charAt(i + 1) == character)
                    i++;
        }

        return output.toString();
    }

    /**
     * Tests if an array contains a value. Creates no objects.
     * @param array The array to search.
     * @param find  The value to look for. Can be null.
     * @return contains
     */
    public static <T> boolean contains(T[] array, T find) {
        return indexOf(array, find) >= 0;
    }

    /**
     * Find the index that a sequence of bytes is found in another sequence of bytes.
     * @param searchArray The array to search.
     * @param findArray   The bytes to look for.
     * @return indexOf
     */
    public static int indexOf(byte[] searchArray, byte[] findArray) {
        for (int i = 0; i < searchArray.length - findArray.length + 1; i++) {
            boolean match = true;
            for (int j = 0; j < findArray.length; j++) {
                if (searchArray[i + j] != findArray[j]) {
                    match = false;
                    break;
                }
            }

            if (match)
                return i;
        }
        return -1;
    }

    /**
     * Tests if an array contains a value. Creates no objects.
     * @param array The array to search.
     * @param find  The value to look for. Can be null.
     * @return contains
     */
    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    public static <T> int indexOf(T[] array, T find) {
        for (int i = 0; i < array.length; i++)
            if (Objects.equals(find, array[i]))
                return i;
        return -1;
    }

    /**
     * Tests if an array contains a value. Creates no objects.
     * @param array The array to search.
     * @param find  The value to look for. Can be null.
     * @return contains
     */
    @SuppressWarnings({"ForLoopReplaceableByForEach"})
    public static int indexOf(int[] array, int find) {
        for (int i = 0; i < array.length; i++)
            if (find == array[i])
                return i;
        return -1;
    }

    /**
     * Tests if a string is alphanumeric or not.
     * @param testString The string to test.
     * @return isAlphanumeric
     */
    public static boolean isAlphanumeric(String testString) {
        for (int i = 0; i < testString.length(); i++)
            if (!Character.isLetterOrDigit(testString.charAt(i)))
                return false;
        return true;
    }

    /**
     * Tests if a string is alphanumeric or not.
     * @param testString The string to test.
     * @return isAlphanumeric
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isValidFileName(String testString) {
        File f = new File(testString);
        try {
            f.getCanonicalPath();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Pads the number with 0s as a string.
     * @param number The number to pad.
     * @param digits The number of digits to include.
     * @return paddedString
     */
    public static String padNumberString(int number, int digits) {
        int usedDigits = (number == 0) ? 1 : (int) Math.log10(number) + 1;

        StringBuilder prependStr = new StringBuilder();
        for (int i = 0; i < (digits - usedDigits); i++)
            prependStr.append("0");

        return prependStr.toString() + number;
    }

    /**
     * Pads the number with 0s as a string.
     * @param baseStr The string to pad.
     * @param targetLength The target string size.
     * @return paddedString
     */
    public static String padStringLeft(String baseStr, int targetLength, char toAdd) {
        StringBuilder prependStr = new StringBuilder();
        while (targetLength > prependStr.length() + baseStr.length())
            prependStr.append(toAdd);

        return prependStr + baseStr;
    }

    /**
     * A clamp method that clamps down on negative numbers, and up on positive numbers.
     * @param value The value to clamp.
     * @return clampedCeil
     */
    public static int ceilNegative(double value) {
        if ((int) value == value)
            return (int) value;

        return (value > 0) ? ((int) value + 1) : ((int) value - 1);
    }

    /**
     * Returns the string with all non alpha-numeric characters removed.
     * @param input The input to strip.
     * @return strippedStr
     */
    public static String stripAlphanumeric(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char temp = input.charAt(i);
            if (Character.isLetterOrDigit(temp))
                output.append(temp);
        }
        return output.toString();
    }

    /**
     * Takes a screenshot of a given SubScene.
     * @param subScene   The subScene to take a screenshot of.
     * @param namePrefix The file name prefix to save the image as.
     */
    public static void takeScreenshot(SubScene subScene, Scene scene, String namePrefix) {
        WritableImage image = scene.snapshot(null);
        BufferedImage sceneImage = SwingFXUtils.fromFXImage(image, null);
        BufferedImage croppedImage = cropImage(sceneImage, (int) subScene.getLayoutX(), (int) subScene.getLayoutY(), (int) subScene.getWidth(), (int) subScene.getHeight());

        // Write to file.
        int id = -1;
        while (id++ < 1000) {
            File testFile = new File(GUIMain.getWorkingDirectory(), (namePrefix != null && namePrefix.length() > 0 ? namePrefix + "-" : "") + Utils.padStringLeft(Integer.toString(id), 4, '0') + ".png");
            if (!testFile.exists()) {
                try {
                    ImageIO.write(croppedImage, "png", testFile);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                break;
            }
        }
    }

    /**
     * Crop an image.
     * @param image The image to trim.
     * @return croppedImage
     */
    public static BufferedImage cropImage(BufferedImage image, int x, int y, int width, int height) {
        BufferedImage croppedImage = new BufferedImage(width, height, image.getType());
        Graphics2D graphics = croppedImage.createGraphics();
        graphics.drawImage(image, -x, -y, image.getWidth(), image.getHeight(), null);
        graphics.dispose();
        return croppedImage;
    }

    /**
     * Calculate the SHA1 hash of the bytes.
     * @param data The data to calculate the SHA1 hash of.
     * @return sha1Hash
     */
    public static String calculateSHA1Hash(byte[] data) {
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(data);
            return byteToHex(crypt.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash)
            formatter.format("%02x", b);

        String result = formatter.toString();
        formatter.close();
        return result;
    }
}