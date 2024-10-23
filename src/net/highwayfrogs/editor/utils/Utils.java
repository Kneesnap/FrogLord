package net.highwayfrogs.editor.utils;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
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
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Cleanup;
import lombok.SneakyThrows;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.writer.ArrayReceiver;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.GUIMain;
import net.highwayfrogs.editor.gui.GameUIController;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh.DynamicMeshTextureQuality;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Some static utilities.
 * TODO: Split this into multiple classes at some point.
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
    private static final Map<String, FXMLLoader> CACHED_RESOURCE_PATH_FXML_LOADERS = new HashMap<>();
    private static Logger logger;

    /**
     * Gets a logger usable in a static context.
     * This is reported as "Utils", so it's recommended to not use this, but this can be helpful for debugging.
     */
    public static Logger getLogger() {
        if (logger != null)
            return logger;
        
        return logger = Logger.getLogger(Utils.class.getSimpleName());
    }

    /**
     * Creates an integer identifier from a string.
     * @param text The text to convert
     * @return identifier string
     */
    public static int makeIdentifier(String text) {
        if (text == null || text.length() != 4)
            throw new RuntimeException("Cannot make signature from '" + text + "'.");

        return (text.charAt(3) << 24) | (text.charAt(2) << 16) | (text.charAt(1) << 8) | text.charAt(0);
    }

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
     * Gets the integer value interpreted as a magic string.
     * @param value The value to convert
     * @return magicString
     */
    public static String toMagicString(int value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Constants.INTEGER_SIZE; i++)
            writeByteAsText(builder, (byte) (value >> (i * Constants.BITS_PER_BYTE)));

        return builder.toString();
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
     * Create a new array with fewer elements than the supplied one. (Cut from the left side)
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
        short small = (short) Math.round(floatValue * 0xFF);
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
     * Get a resource in the JAR.
     * @param resourcePath The resource path.
     * @param includeSubFolders if true, sub folders will be included.
     * @return resourceURL
     */
    @SneakyThrows
    public static List<URL> getInternalResourceFilesInDirectory(URL resourcePath, boolean includeSubFolders) {
        if (resourcePath.getProtocol() != null && resourcePath.getProtocol().equalsIgnoreCase("jar")) {
            String fullResourcePath = resourcePath.getFile();
            int exclamationPos = fullResourcePath.indexOf('!');
            if (exclamationPos < 0) {
                makePopUp("Couldn't find the JAR-embedded file resource path in the URL '" + resourcePath + "'.", AlertType.ERROR);
                return Collections.emptyList();
            }

            String localResourcePath = fullResourcePath.substring(exclamationPos + 1);
            File frogLordJar = getFileFromURL(Utils.class.getProtectionDomain().getCodeSource().getLocation());
            if (!frogLordJar.exists())
                throw new RuntimeException("Failed to find resource files at '" + localResourcePath + "', we resolved the FrogLord jar file to '" + frogLordJar + "', which did not exist. (" + resourcePath + ")");

            BiPredicate<Path, BasicFileAttributes> pathValidityCheck = (path, attributes) -> path.startsWith(localResourcePath);
            try (FileSystem fs = FileSystems.newFileSystem(frogLordJar.toPath(), Utils.class.getClassLoader())) {
                boolean didAnyPathsMatch = false;
                List<URL> foundResourceUrls = new ArrayList<>();

                // Test the path for resource files.
                for (Path root : fs.getRootDirectories()) {
                    try (Stream<Path> stream = Files.find(root, Integer.MAX_VALUE, pathValidityCheck)) {
                        foundResourceUrls.addAll(getUrlsFromPaths(stream, true));
                    } catch (Throwable th) {
                        handleError(null, th, false, "Failed to test the path '%s' for internal resource files.", root);
                    }
                }

                return foundResourceUrls;
            } catch (Throwable th) {
                handleError(null, th, false, "Failed to get the FileSystem object for the FrogLord jar: '%s'", frogLordJar);
            }

            throw new RuntimeException("Failed to enumerate resource files in '" + resourcePath + "'.");
        }

        // We should only get here when running from an IDE. (Or if there's some other version FrogLord would be run outside a jar?)
        try {
            Path path = Paths.get(resourcePath.toURI());
            try (Stream<Path> stream = Files.walk(path, includeSubFolders ? Integer.MAX_VALUE : 1)) {
                return getUrlsFromPaths(stream, false);
            }
        } catch (URISyntaxException | IOException ex) {
            throw new RuntimeException("Failed to get files in resource directory '" + resourcePath + "'", ex);
        }
    }

    private static List<URL> getUrlsFromPaths(Stream<Path> stream, boolean remakeResources) {
        Stream<URL> urlStream = stream.map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (Throwable th) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(url -> !url.getPath().endsWith("/")); // Remove directories.

        // This part is necessary since the urls we get from the file walker aren't actually valid for to open for some reason.
        // So, we take the file paths we get, and then feed them back into something we know we can get the paths from.
        if (remakeResources) {
            urlStream = urlStream.map(url -> {
                String fullResourcePath = url.getFile();
                int exclamationPos = fullResourcePath.indexOf('!');
                if (exclamationPos < 0)
                    throw new RuntimeException("Couldn't find the JAR-embedded file resource path in the URL '" + url + "'/'" + fullResourcePath + "'.");

                String localResourcePath = fullResourcePath.substring(exclamationPos + (fullResourcePath.charAt(exclamationPos + 1) == '/' ? 2 : 1));
                URL convertedURL = getResourceURL(localResourcePath);
                if (convertedURL == null)
                    throw new RuntimeException("Failed to convert local resource path '" + localResourcePath + "' from URL '" + url + "' into usable resource path URL.");

                return convertedURL;
            });
        }

        return urlStream.collect(Collectors.toList());
    }

    /**
     * Often times we have improperly formatted URLs in java. Unfortunately, this comes with characters like ' ' replaced with %20, which can cause headaches when working with the file system.
     * https://web.archive.org/web/20100327174235/http://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html
     * This is our attempt at fixing the problem.
     * @param url the url to resolve a file for
     * @return validFile
     */
    public static File getFileFromURL(URL url) {
        if (!url.getProtocol().equalsIgnoreCase("file") && !url.getProtocol().equalsIgnoreCase("jar"))
            throw new UnsupportedOperationException("Cannot get file from URL with protocol '" + url.getProtocol() + "'. (" + url + ")");

        try {
            return new File(url.toURI());
        } catch(URISyntaxException e) {
            return new File(url.getPath());
        }
    }

    /**
     * Get a resource in the JAR.
     * @param resourceName The resource name.
     * @return resourceURL
     */
    public static URL getResourceURL(String resourceName) {
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
            handleError(null, e, false, "Failed to read text lines from file '%s'", file);
            return Collections.emptyList();
        }
    }

    /**
     * Read text from a file.
     * @param file The file to read from.
     * @return fileText
     */
    public static String readFileText(File file) {
        return String.join(Constants.NEWLINE, readLinesFromFile(file));
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
            handleError(null, ex, false, "Failed to copy stream data from the input stream to the output stream!");
        }

        if (closeInput) {
            try {
                input.close();
            } catch (IOException ex) {
                handleError(null, ex, false, "Failed to close the input stream.");
            }
        }
    }

    /**
     * Get the first file with this name that does not exist. Appends stuff like (1).
     * @param file The file to get.
     * @return nonexistentFile.
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
     * Resize an image.
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
     * Strip the extension from a file name.
     * @param name The file name.
     * @return stripped
     */
    public static String stripSingleExtension(String name) {
        int lastDotIndex = name.lastIndexOf('.');
        return lastDotIndex >= 0 ? name.substring(0, lastDotIndex) : name;
    }

    /**
     * Get the file name from the url.
     * @param url the url to get the file name from
     * @return fileName
     */
    public static String getFileName(URL url) {
        if (url == null)
            throw new NullPointerException("url");
        String query = url.getQuery();
        if (query != null)
            return query;

        String fullPath = url.getFile();
        int backslashPos = fullPath.lastIndexOf('/');
        return backslashPos >= 0 ? fullPath.substring(backslashPos + 1) : fullPath;
    }

    /**
     * Get the file name from the url.
     * @param url the url to get the file name from
     * @return fileName
     */
    public static String getFileNameWithoutExtension(URL url) {
        return stripSingleExtension(getFileName(url));
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
     * Strip the extension and Windows 95 from a file name.
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
     * Parse a hex integer string into a 32 bit signed integer.
     * @param str The string to parse.
     * @return parsedNumber
     */
    public static int parseHexInteger(String str) {
        return str.startsWith("0x") ? Integer.parseInt(str.substring(2), 16) : Integer.parseInt(str);
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
    public static File promptFileOpenExtensions(GameInstance instance, String title, String typeInfo, String... extensions) {
        Utils.verify(extensions.length > 0, "");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);

        List<String> allExtensions = new ArrayList<>();
        for (String ext : extensions) {
            String type = ext.contains(".") ? ext : "*." + ext; // Unix is case-sensitive, so we add both lower-case and upper-case.
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

        File selectedFile = fileChooser.showOpenDialog(instance != null ? instance.getMainStage() : null);
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
    public static File promptFileOpen(GameInstance instance, String title, String typeInfo, String extension) {
        return promptFileOpenExtensions(instance, title, typeInfo, extension);
    }

    /**
     * Prompt the user to save a file.
     * @param title       The title of the window to display.
     * @param suggestName The initial name to suggest saving the file as.
     * @param typeInfo    The label to show for the file-type.
     * @param extension   Allowed extension.
     * @return selectedFile, Can be null.
     */
    public static File promptFileSave(GameInstance instance, String title, String suggestName, String typeInfo, String extension) {
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
            if (extension != null && !extension.equals("*") && !initialName.endsWith("." + extension))
                initialName += "." + extension;

            fileChooser.setInitialFileName(initialName);
        }

        File selectedFile = fileChooser.showSaveDialog(instance != null ? instance.getMainStage() : null);
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
    public static File promptChooseDirectory(GameInstance instance, String title, boolean saveDirectory) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        chooser.setInitialDirectory(getValidFolder(GUIMain.getWorkingDirectory()));

        File selectedFolder = chooser.showDialog(instance != null ? instance.getMainStage() : null);
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
     * @param template The name of the template to load. Should not be user-controllable, as there is no path sanitization.
     * @param controller the window controller
     * @param title the title of the window to show
     * @param waitUntilClose if true, the thread will be blocked until the window is closed
     */
    @SuppressWarnings("ConstantValue")
    public static <T> boolean createWindowFromFXMLTemplate(String template, GameUIController<?> controller, String title, boolean waitUntilClose) {
        if (controller == null)
            throw new NullPointerException("controller");

        GameInstance instance = controller.getGameInstance();
        FXMLLoader fxmlTemplateLoader = getFXMLTemplateLoader(instance, template);
        if (fxmlTemplateLoader == null) {
            makePopUp("The UI template '" + template + "' was not found.", AlertType.ERROR);
            return false;
        }

        // Load fxml data.
        if (GameUIController.loadController(instance, fxmlTemplateLoader, controller) == null)
            return false;

        // Open a window.
        GameUIController.openWindow(controller, title, waitUntilClose);
        return true;
    }

    /**
     * Gets the fxml template URL by its name.
     * @param template The template name.
     * @return fxmlTemplateUrl
     */
    public static FXMLLoader getFXMLTemplateLoader(GameInstance gameInstance, String template) {
         FXMLLoader fxmlLoader = gameInstance != null ? gameInstance.getFXMLTemplateLoader(template) : null;
         if (fxmlLoader != null)
             return fxmlLoader;

         fxmlLoader = CACHED_RESOURCE_PATH_FXML_LOADERS.get(template);
         if (fxmlLoader == null) {
             String localPath = "fxml/" + template + ".fxml";
             URL url = Utils.getResourceURL(localPath);
             if (url == null)
                 throw new RuntimeException("Could not find resource '" + localPath + "' for " + Utils.getSimpleName(gameInstance) + ".");
             CACHED_RESOURCE_PATH_FXML_LOADERS.put(template, fxmlLoader = new FXMLLoader(url));
         }

         return fxmlLoader;
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
     * Get the alpha value from an int value.
     * @param rgb The int value to get the color from.
     * @return colorValue
     */
    public static int getAlphaInt(int rgb) {
        return getAlpha(rgb) & 0xFF;
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
     * Get an integer from a color object.
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
     * Get an integer from a color object.
     * @param color The color to turn into rgb.
     * @return rgbInt
     */
    public static int toARGB(Color color) {
        return ((int) (color.getOpacity() * 255) << 24) | toRGB(color);
    }

    /**
     * Get an integer from color bytes.
     * @return rgbInt
     */
    public static int toRGB(byte red, byte green, byte blue) {
        return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Get an integer from color bytes.
     * @return rgbInt
     */
    public static int toARGB(byte red, byte green, byte blue, byte alpha) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Get an integer from a color object.
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
     * Get an integer from color bytes.
     * @return rgbInt
     */
    public static int toABGR(byte red, byte green, byte blue, byte alpha) {
        return ((alpha & 0xFF) << 24) | ((blue & 0xFF) << 16) | ((green & 0xFF) << 8) | (red & 0xFF);
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
     * Reads all bytes in a file.
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
        AtomicReference<String> resetTextRef = new AtomicReference<>(field.getText());
        field.setStyle(null);
        field.setOnKeyPressed(evt -> {
            KeyCode code = evt.getCode();
            if (field.getStyle().isEmpty() && (code.isLetterKey() || code.isDigitKey() || code == KeyCode.BACK_SPACE)) {
                field.setStyle("-fx-text-inner-color: darkgreen;");
            } else if (code == KeyCode.ESCAPE) {
                if (field.getParent() != null)
                    field.getParent().requestFocus();
                evt.consume(); // Don't pass further, eg: we don't want to exit the UI we're in.
                field.setText(resetTextRef.get());
                field.setStyle(null);
            } else if (code == KeyCode.ENTER) {
                boolean successfullyHandled = false;
                String newText = field.getText();

                try {
                    successfullyHandled = setter == null || setter.apply(newText);
                } catch (Throwable th) {
                    Utils.makeErrorPopUp("An error occurred applying the text '" + newText + "'.", th, true);
                }

                // Run completion hook. If it doesn't pass, return false. If it errors. warn and set it red.
                if (successfullyHandled) {
                    try {
                        if (onPass != null)
                            onPass.run();
                        field.setStyle(null); // Disable any red / green styling.
                        resetTextRef.set(newText);
                        if (field.getParent() != null) // Remove field focus after success.
                            field.getParent().requestFocus();
                        return;
                    } catch (Throwable th) {
                        Utils.makeErrorPopUp("An error occurred handling the text '" + newText + "'.", th, true);
                    }
                }

                field.setStyle(Constants.FX_STYLE_INVALID_TEXT);
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
        if (data == null || test.length > data.length)
            return false;

        for (int i = 0; i < test.length; i++)
            if (data[startIndex + i] != test[i])
                return false;
        return true;
    }

    /**
     * Creates an PhongMaterial which uses nearest-neighbor texture display and is unaffected by lighting.
     * When the transparency outlines look poorly, the solution is to resize the image to use a higher resolution (while also using nearest neighbor)
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeUnlitSharpMaterial(Image texture) {
        return updateUnlitSharpMaterial(new PhongMaterial(), texture);
    }

    /**
     * Updates a PhongMaterial which uses nearest-neighbor texture display and is unaffected by lighting.
     * When the transparency outlines look poorly, the solution is to resize the image to use a higher resolution (while also using nearest neighbor)
     * @param material The material to update.
     * @param texture The texture to apply to the material.
     * @return phongMaterial
     */
    public static PhongMaterial updateUnlitSharpMaterial(PhongMaterial material, Image texture) {
        // DirectX / Direct3D
        // When the alpha in the diffuse map is zero, "discard" occurs, causing the pixel to be transparent, regardless of what the self-illumination map says.
        // At the end Mtl1PS.hlsl in JavaFX is when the pixel RGB from the self-illumination map is added, but importantly it ignores the self-illumination alpha.
        // Since the RGB value is added and not overridden, the color should be zero (black) so that the self-illumination map can provide the color.
        // To achieve this, we set the diffuse color to be black, to multiply out the RGB values while keeping alpha intact.
        // The alpha is obtained from the previous multiplication between the diffuse texture pixel and the diffuse color.
        // I assume that the reason self-illumination is not blurry but diffuse is blurry probably means the textures are configured differently.

        if (material.getDiffuseColor() != Color.BLACK)
            material.setDiffuseColor(Color.BLACK); // When this is set to the default (WHITE), the coloring looks wrong when combined with a self-illumination map, since it's combining the light from both sources.
        if (material.getDiffuseMap() != texture)
            material.setDiffuseMap(texture); // Setting the diffuse map this seems to enable transparency, where-as it will be the diffuse color if not set.
        if (material.getSelfIlluminationMap() != texture)
            material.setSelfIlluminationMap(texture); // When this is not present, the material becomes fully black, because the diffuse color is off. If the color is changed to white, then the image does display but it's blurry and still has the same transparency problems.
        return material;
    }

    /**
     * Make the phong material with the given quality setting.
     * @param texture the texture to create a material for
     * @param quality the quality of the material
     * @return newMaterial
     */
    public static PhongMaterial makePhongMaterial(Image texture, DynamicMeshTextureQuality quality) {
        switch (quality) {
            case LIT_BLURRY:
                return makeLitBlurryMaterial(texture);
            case UNLIT_SHARP:
                return makeUnlitSharpMaterial(texture);
            default:
                throw new IllegalArgumentException("Unsupported texture quality: " + quality);
        }
    }

    /**
     * Update the phong material with the given quality setting.
     * @param material the material to update
     * @param texture the texture to update the material with
     * @param quality the quality of the material
     * @return material
     */
    public static PhongMaterial updatePhongMaterial(PhongMaterial material, Image texture, DynamicMeshTextureQuality quality) {
        switch (quality) {
            case LIT_BLURRY:
                return updateLitBlurryMaterial(material, texture);
            case UNLIT_SHARP:
                return updateUnlitSharpMaterial(material, texture);
            default:
                throw new IllegalArgumentException("Unsupported texture quality: " + quality);
        }
    }

    /**
     * Creates a lit PhongMaterial which uses some kind of blurring/smoothing for the texture but is impacted by lighting.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeLitBlurryMaterial(Image texture) {
        return updateLitBlurryMaterial(new PhongMaterial(), texture);
    }

    /**
     * Updates a lit PhongMaterial which uses some kind of blurring/smoothing for the texture but is impacted by lighting.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial updateLitBlurryMaterial(PhongMaterial material, Image texture) {
        // The following is what would seem to intuitively work, and it does. But the image is also blurry, which doesn't seem to be the case with self-illumination.
        // However, self-illumination is unaffected by lighting.

        if (material.getDiffuseColor() != Color.WHITE)
            material.setDiffuseColor(Color.WHITE); // The default PhongMaterial diffuseColor is WHITE, which is what we want.
        if (material.getDiffuseMap() != texture)
            material.setDiffuseMap(texture);
        if (material.getSelfIlluminationMap() != null)
            material.setSelfIlluminationMap(null);
        return material;
    }

    /**
     * Creates a PhongMaterial with a texture affected by lighting, and using only diffuse components.
     * @param texture The texture to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeDiffuseMaterial(Image texture) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(texture);
        return material;
    }

    /**
     * Creates a PhongMaterial with a color unaffected by lighting which can be used for highlight overlays.
     * @param color The color to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeHighlightOverlayMaterial(Color color) {
        BufferedImage colorImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = colorImage.createGraphics();
        graphics.setColor(toAWTColor(color, (byte) 0x80));
        graphics.fillRect(0, 0, colorImage.getWidth(), colorImage.getHeight());
        graphics.dispose();

        return makeUnlitSharpMaterial(toFXImage(colorImage, false));
    }

    /**
     * Creates a PhongMaterial with a color unaffected by lighting.
     * @param color The color to create.
     * @return phongMaterial
     */
    public static PhongMaterial makeUnlitSharpMaterial(Color color) {
        return makeUnlitSharpMaterial(makeColorImage(color));
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
     * Calculate interpolated color value based on "t" between source and target colours.
     * @param colorSrc The source color.
     * @param colorTgt The target color.
     * @param t        The desired delta (from 0.0 to 1.0 inclusive).
     * @return Color
     */
    public static int calculateInterpolatedColourARGB(final java.awt.Color colorSrc, final java.awt.Color colorTgt, float t) {
        t = Math.min(t, 1.0f);
        t = Math.max(t, 0.0f);

        short red = (short) Math.min(255, colorSrc.getRed() + (int) ((colorTgt.getRed() - colorSrc.getRed()) * t));
        short green = (short) Math.min(255, colorSrc.getGreen() + (int) ((colorTgt.getGreen() - colorSrc.getGreen()) * t));
        short blue = (short) Math.min(255, colorSrc.getBlue() + (int) ((colorTgt.getBlue() - colorSrc.getBlue()) * t));
        short alpha = (short) Math.min(255, colorSrc.getAlpha() + (int) ((colorTgt.getAlpha() - colorSrc.getAlpha()) * t));

        return toARGB(unsignedShortToByte(red), unsignedShortToByte(green), unsignedShortToByte(blue), unsignedShortToByte(alpha));
    }

    /**
     * Calculate interpolated color value based on "t" between source and target colours.
     * @param colorSrc The source color.
     * @param colorTgt The target color.
     * @param t        The desired delta (from 0.0 to 1.0 inclusive).
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
     * Handle an exception which should be reported to the user.
     * @param logger the logger to write the error to
     * @param th the exception to log
     * @param showWindow if true, a popup window will display the error
     */
    public static void handleError(Logger logger, Throwable th, boolean showWindow) {
        handleError(logger, th, showWindow, 2);
    }

    /**
     * Handle an exception which should be reported to the user.
     * @param logger the logger to write the error to
     * @param th the exception to log
     * @param showWindow if true, a popup window will display the error
     * @param skipCount the number of methods to search back.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void handleError(Logger logger, Throwable th, boolean showWindow, int skipCount) {
        // TODO: Should generalize? Probably?
        // TODO: JAva 9 -> StackWalker.getCallerClass()
        // TODO: return StackWalker.
        //      getInstance().
        //      walk(stream -> stream.skip(1).findFirst().get()).
        //      getMethodName();

        // There
        String callingMethodName = null;
        Class<?> callingClass = null;
        try {
            StackTraceElement element = new Throwable().fillInStackTrace().getStackTrace()[skipCount];
            callingMethodName = element.getMethodName();
            callingClass = element.getClass();
        } catch (Throwable classLookupException) {
            // If this fails, just use null.
        }

        // Use the utils logger if we weren't given one.
        if (logger == null)
            logger = getLogger();

        // Print stage trace.
        if (logger != null) {
            logger.throwing(callingClass != null ? callingClass.getSimpleName() : null, callingMethodName, th);
        } else {
            th.printStackTrace();
        }

        // Create popup window.
        if (showWindow) {
            if (Platform.isFxApplicationThread()) {
                Utils.makeErrorPopUp(null, th, false);
            } else {
                Platform.runLater(() -> Utils.makeErrorPopUp(null, th, false));
            }
        }
    }

    /**
     * Handle an exception which should be reported to the user.
     * @param logger the logger to write the error to
     * @param th the exception to log
     * @param showWindow if true, a popup window will display the error
     * @param message the message to accompany the exception
     * @param arguments format string arguments to the message
     */
    public static void handleError(Logger logger, Throwable th, boolean showWindow, String message, Object... arguments) {
        handleError(logger, th, showWindow, 2, message, arguments);
    }

    /**
     * Handle an exception which should be reported to the user.
     * @param logger the logger to write the error to
     * @param th the exception to log
     * @param showWindow if true, a popup window will display the error
     * @param skipCount the number of methods to search back.
     * @param message the message to accompany the exception
     * @param arguments format string arguments to the message
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void handleError(Logger logger, Throwable th, boolean showWindow, int skipCount, String message, Object... arguments) {
        // TODO: Should generalize? Probably?
        // TODO: JAva 9 -> StackWalker.getCallerClass()
        // TODO: return StackWalker.
        //      getInstance().
        //      walk(stream -> stream.skip(1).findFirst().get()).
        //      getMethodName();

        // There
        String callingMethodName = null;
        Class<?> callingClass = null;
        try {
            StackTraceElement element = new Throwable().fillInStackTrace().getStackTrace()[skipCount];
            callingMethodName = element.getMethodName();
            callingClass = element.getClass();
        } catch (Throwable classLookupException) {
            // If this fails, just use null.
        }

        // Format message.
        String formattedMessage;
        try {
            formattedMessage = message != null && arguments != null && arguments.length > 0 ? String.format(message, arguments) : message;
        } catch (IllegalFormatException exception) {
            formattedMessage = "[String Formatting Failed] " + message;
        }

        // Use the utils logger if we weren't given one.
        if (logger == null)
            logger = getLogger();

        // Print stage trace.
        if (logger != null) {
            if (formattedMessage != null)
                logger.severe(formattedMessage);
            if (th != null)
                logger.throwing(callingClass != null ? callingClass.getSimpleName() : null, callingMethodName, th);
        } else {
            System.err.println(formattedMessage);
            if (th != null)
                th.printStackTrace();
        }

        // Create popup window.
        if (showWindow) {
            if (Platform.isFxApplicationThread()) {
                Utils.makeErrorPopUp(formattedMessage, th, false);
            } else {
                final String finalFormattedMessage = formattedMessage;
                Platform.runLater(() -> Utils.makeErrorPopUp(finalFormattedMessage, th, false));
            }
        }
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
    public static void makeErrorPopUp(String message, Throwable ex, boolean printException) {
        // Get the exception as a string.
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        if (message != null && message.length() > 0) {
            printWriter.append(message);
            printWriter.append(System.lineSeparator());
        }

        if (ex != null)
            ex.printStackTrace(printWriter);

        if (printException) {
            handleError(null, ex, true, stringWriter.toString());
        } else {
            String errorMessage = stringWriter.toString();

            Alert alert = new Alert(AlertType.ERROR, errorMessage, ButtonType.OK);
            if (ex != null) {
                alert.setResizable(true);
                alert.setWidth(1000);
                alert.setHeight(750);
            }

            alert.showAndWait();
        }
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
     * Returns a value as a hex string with leading 0s included.
     * @param value The value to get as a hex string.
     * @return hexString
     */
    public static String to0PrefixedHexString(int value) {
        return padStringLeft(Integer.toHexString(value).toUpperCase(), 8, '0');
    }

    /**
     * Returns a value as a hex string with leading 0s included.
     * @param value The value to get as a hex string.
     * @return hexString
     */
    public static String to0PrefixedHexStringLower(int value) {
        return padStringLeft(Integer.toHexString(value).toLowerCase(), 8, '0');
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
     * Returns the string with all non-alphanumeric characters removed.
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
    public static void takeScreenshot(GameInstance instance, SubScene subScene, Scene scene, String namePrefix, boolean transparentBackground) {
        Paint subSceneColor = subScene.getFill();

        if (transparentBackground)
            subScene.setFill(Color.TRANSPARENT);

        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);

        WritableImage wImage = new WritableImage((int) subScene.getWidth(), (int) subScene.getHeight());
        BufferedImage sceneImage = SwingFXUtils.fromFXImage(subScene.snapshot(snapshotParameters, wImage), null);

        if (transparentBackground)
            subScene.setFill(subSceneColor);

        // Write to file.
        int id = -1;
        while (id++ < 10000) {
            String fileName = (namePrefix != null && namePrefix.length() > 0 ? namePrefix + "-" : "")
                    + Utils.padStringLeft(Integer.toString(id), 4, '0') + ".png";

            File testFile = new File(GUIMain.getWorkingDirectory(), fileName);
            if (!testFile.exists()) {
                try {
                    ImageIO.write(sceneImage, "png", testFile);
                    break;
                } catch (IOException ex) {
                    try {
                        // Let user pick a directory (in case current working directory is not writeable)
                        File targetDirectory = Utils.promptChooseDirectory(instance, "Save Screenshot", true);
                        ImageIO.write(sceneImage, "png", new File(targetDirectory, fileName));
                        break;
                    } catch (IOException ex2) {
                        handleError(instance.getLogger(), ex2, true, "Failed to write screenshot to '%s'. (No permissions to write here?)", fileName);
                        return;
                    }
                }
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
            handleError(null, e, false, "Couldn't find SHA-1 algorithm implementation.");
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

    /**
     * Gets the simple class name for a particular object.
     * @param clazz The class to get the class name from.
     * @return className
     */
    public static String getSimpleName(Class<?> clazz) {
        return clazz != null ? clazz.getSimpleName() : "NULL CLASS";
    }

    /**
     * Gets the simple class name for a particular object.
     * @param obj The object to get the class name for.
     * @return className
     */
    public static String getSimpleName(Object obj) {
        return obj != null ? obj.getClass().getSimpleName() : "NULL OBJECT (Unknown class)";
    }

    /**
     * Test if the input string is null or empty.
     * @param input The string to test.
     * @return True if the string is null or empty.
     */
    public static boolean isNullOrEmpty(String input) {
        return input == null || input.isEmpty();
    }

    /**
     * Test if the input string is null or whitespace.
     * @param input The string to test.
     * @return True if the string is null or empty.
     */
    public static boolean isNullOrWhiteSpace(String input) {
        return input == null || input.isEmpty() || input.trim().isEmpty();
    }

    /**
     * Trim whitespace at the start of the string.
     * @param input the input string to trim
     * @return trimmedString
     */
    public static String trimStart(String input) {
        if (input == null)
            return null;

        int inputLength = input.length();
        int startIndex = 0;
        while ((startIndex < inputLength) && (input.charAt(startIndex) <= ' '))
            startIndex++;

        return (startIndex > 0) ? input.substring(startIndex) : input;
    }

    /**
     * Trim whitespace at the end of the string.
     * @param input the input string to trim
     * @return trimmedString
     */
    public static String trimEnd(String input) {
        if (input == null)
            return null;

        int len = input.length();
        int st = 0;
        while ((len > 0) && (input.charAt(len - 1) <= ' '))
            len--;

        return (len < input.length()) ? input.substring(0, len) : input;
    }

    /**
     * Tests a value for bits outside the supplied mask, warning if found.
     * @param logger The logger to log the warning to.
     * @param value The value to test
     * @param mask The bit mask to test against.
     * @param target A display string representing the data type.
     * @return true iff there are no unsupported bits.
     */
    public static boolean warnAboutInvalidBitFlags(Logger logger, long value, long mask, String target) {
        if ((value & ~mask) == 0)
            return true;

        if (target != null) {
            logger.warning(target + " had bit flag value " + toHexString(value) + ", which contained unhandled bits.");
        } else {
            logger.warning("Bit flag value " + toHexString(value) + " had unexpected bits set!");
        }
        return false;
    }

    /**
     * Performs a binary search on a list to find a value which can be reduced to the provided key.
     * @param list The sorted list to search.
     * @param key The key to search for.
     * @param toKeyFunction The function to convert the list element into a key.
     * @return foundIndex or insertion index
     * @param <TIntSource> the list element
     */
    public static <TIntSource> int binarySearch(List<TIntSource> list, int key, ToIntFunction<TIntSource> toKeyFunction) {
        int left = 0;
        int right = list.size() - 1;

        while (left <= right) {
            int middleIndex = (left + right) >>> 1;
            int middleValue = toKeyFunction.applyAsInt(list.get(middleIndex));

            if (middleValue > key) {
                right = middleIndex - 1;
            } else if (middleValue < key) {
                left = middleIndex + 1;
            } else {
                return middleIndex;
            }
        }

        // Return insertion index.
        return -(left + 1);
    }

    /**
     * Performs a binary search on a list to find a value which can be reduced to the provided key.
     * @param list The sorted list to search.
     * @param key The key to search for.
     * @param param the param value used to get a key from the list elements
     * @param toKeyFunction The function to convert the list element into a key.
     * @return foundIndex or insertion index
     * @param <TIntSource> the list element
     * @param <TParam> the param value type used to get a key from the list elements
     */
    public static <TIntSource, TParam> int binarySearch(List<TIntSource> list, int key, TParam param, ToIntBiFunction<TParam, TIntSource> toKeyFunction) {
        int left = 0;
        int right = list.size() - 1;

        while (left <= right) {
            int middleIndex = (left + right) >>> 1;
            int middleValue = toKeyFunction.applyAsInt(param, list.get(middleIndex));

            if (middleValue > key) {
                right = middleIndex - 1;
            } else if (middleValue < key) {
                left = middleIndex + 1;
            } else {
                return middleIndex;
            }
        }

        // Return insertion index.
        return -(left + 1);
    }

    /**
     * Performs a binary search on a list to find a value which can be reduced to the provided key.
     * @param list The sorted list to search.
     * @param key The key to search for.
     * @param toKeyFunction The function to convert the list element into a key.
     * @return foundIndex or insertion index
     * @param <TLongSource> the list element
     */
    public static <TLongSource> int binarySearch(List<TLongSource> list, long key, ToLongFunction<TLongSource> toKeyFunction) {
        int left = 0;
        int right = list.size() - 1;

        while (left <= right) {
            int middleIndex = (left + right) >>> 1;
            long middleValue = toKeyFunction.applyAsLong(list.get(middleIndex));

            if (middleValue > key) {
                right = middleIndex - 1;
            } else if (middleValue < key) {
                left = middleIndex + 1;
            } else {
                return middleIndex;
            }
        }

        // Return insertion index.
        return -(left + 1);
    }

    /**
     * Performs a binary search on a list to find a value which can be reduced to the provided key.
     * @param list The sorted list to search.
     * @param key The key to search for.
     * @param param the param value used to get a key from the list elements
     * @param toKeyFunction The function to convert the list element into a key.
     * @return foundIndex or insertion index
     * @param <TLongSource> the list element
     * @param <TParam> the param value type used to get a key from the list elements
     */
    public static <TLongSource, TParam> int binarySearch(List<TLongSource> list, long key, TParam param, ToLongBiFunction<TParam, TLongSource> toKeyFunction) {
        int left = 0;
        int right = list.size() - 1;

        while (left <= right) {
            int middleIndex = (left + right) >>> 1;
            long middleValue = toKeyFunction.applyAsLong(param, list.get(middleIndex));

            if (middleValue > key) {
                right = middleIndex - 1;
            } else if (middleValue < key) {
                left = middleIndex + 1;
            } else {
                return middleIndex;
            }
        }

        // Return insertion index.
        return -(left + 1);
    }

    /**
     * Report an error if the action fails.
     * @param action the action to run
     */
    public static void reportErrorIfFails(Runnable action) {
        try {
            action.run();
        } catch (Throwable th) {
            handleError(null, th, true);
        }
    }

    /**
     * Prints the stack trace of the current thread.
     */
    public static void printStackTrace() {
        try {
            throw new RuntimeException("Triggered Exception");
        } catch (Throwable e) {
            handleError(null, e, false);
        }
    }

    /**
     * Tests if a file is located within a folder. Restricts directory escalation.
     * @param targetFile The target file to test.
     * @param holdingFolder The folder which must hold the file.
     * @return Whether the file is held.
     */
    public static boolean isFileWithinParent(File targetFile, File holdingFolder) {
        if (targetFile == null)
            throw new RuntimeException("The folder to test is null.");
        if (holdingFolder == null)
            throw new RuntimeException("The holding folder is null.");
        if (!holdingFolder.isDirectory() || holdingFolder.isFile())
            throw new RuntimeException("The holding folder '" + holdingFolder.getName() + "' is actually not a folder.");

        // Note: Make sure that if holdingFolder == targetFile, that still returns false.
        File testFile;
        try {
            testFile = targetFile.getCanonicalFile().getParentFile(); // Evaluates everything like '..\', drive letters, etc.
            holdingFolder = holdingFolder.getCanonicalFile(); // Necessary to make .equals() work.
        } catch (IOException ex) {
            // Probably not accessible due to permissions, or, it's an invalid file path which by definition can't be evaluated.
            return false;
        }

        // Search parent files.
        while (testFile != null) {
            if (testFile.equals(holdingFolder))
                return true;

            testFile = testFile.getParentFile();
        }

        // Didn't find a match.
        return false;
    }

    /**
     * Gets the file as a path local to the server root. This is mainly for displays.
     * @param rootFolder The folder to treat as the root.
     * @param file The file to get the path of.
     * @return localPathString
     */
    public static String toLocalPath(File rootFolder, File file, boolean allowOutsideRoot) {
        if (file == null)
            return "NULL FILE";
        if (rootFolder == null)
            throw new NullPointerException("rootFolder");
        if (!rootFolder.isDirectory())
            throw new RuntimeException("The provided rootFolder: '" + rootFolder + "' was not a directory!");

        if (!isFileWithinParent(file, rootFolder)) {
            if (allowOutsideRoot)
                return file.getPath();

            throw new RuntimeException("File '" + file + "' was not found within '" + rootFolder + "'.");
        }

        String rootPath;
        try {
            rootPath = rootFolder.getCanonicalPath();
        } catch (IOException ex) {
            getLogger().severe("Failure path: '" + rootFolder + "'");
            throw new RuntimeException("Failed to get canonical path of file.", ex);
        }

        String targetPath;
        try {
            targetPath = file.getCanonicalPath();
        } catch (IOException ex) {
            getLogger().severe("Failure path: '" + rootFolder + "'");
            throw new RuntimeException("Failed to get canonical path of target file.", ex);
        }

        // Strip file path.
        String resultPath = targetPath;
        if (resultPath.startsWith(rootPath))
            resultPath = resultPath.substring(rootPath.length() + 1);

        return resultPath;
    }

    /**
     * Adds a suffix to the file name, before the extension.
     * @param file the file to add a suffix to
     * @param suffix the suffix to add
     * @return the new file with the suffix applied
     */
    public static File addFileNameSuffix(File file, String suffix) {
        if (file == null)
            throw new NullPointerException("file");
        if (suffix == null || suffix.isEmpty())
            return file;

        String inputFileName = file.getName();
        String strippedInputName = Utils.stripExtension(inputFileName);
        return new File(file.getParentFile(), strippedInputName + suffix + inputFileName.substring(strippedInputName.length() + 1));
    }

    /**
     * Ensures input string is using the path separator for the system currently running FrogLord.
     * @param input the string containing potentially incorrect path separators
     * @param removeLeadingSeparator if true, any path separators at the start of the string will be removed.
     * @return processed string
     */
    public static String ensureValidPathSeparator(String input, boolean removeLeadingSeparator) {
        if (input == null || input.length() == 0)
            return input;

        String result = input;
        if (File.separatorChar != '\\')
            result = result.replace('\\', File.separatorChar);
        if (File.separatorChar != '/')
            result = result.replace('/', File.separatorChar);

        if (removeLeadingSeparator)
            while (result.length() > 0 && result.charAt(0) == File.separatorChar)
                result = result.substring(1);

        return result;
    }

    /**
     * Gets the index of the value in the list.
     * If the value is not seen in the list, an index at the end of the list will be provided.
     * This is primarily used for identifying indices when debugging.
     * @param list the list to find the index from
     * @param value the value to lookup
     * @return index
     * @param <TElement> the type of elements in the list.
     */
    public static <TElement> int getLoadingIndex(List<TElement> list, TElement value) {
        if (list == null)
            return -1;

        int index = list.indexOf(value);
        if (index == -1) {
            return list.size();
        } else {
            return index;
        }
    }

    /**
     * Gets the file name extension from the file name.
     * @param fileName the file name to get the extension from
     * @return fileNameExtension, if there is one
     */
    public static String getFileNameExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0)
            return null;

        return fileName.substring(dotIndex + 1);
    }

    /**
     * Gets an audio clip from the raw audio data.
     * @param format the format to create the clip with
     * @param rawAudioData the raw audio data to supply to the clip
     * @return audioClip, if successful
     */
    public static Clip getClipFromRawAudioData(AudioFormat format, byte[] rawAudioData) {
        if (format == null)
            throw new NullPointerException("format");
        if (rawAudioData == null)
            throw new NullPointerException("rawAudioData");

        Clip audioClip = null;
        try {
            audioClip = AudioSystem.getClip();
            audioClip.open(format, rawAudioData, 0, rawAudioData.length);
            return audioClip;
        } catch (LineUnavailableException ex) {
            handleError(null, ex, false, "Failed to create an audio Clip.");
            if (audioClip != null)
                audioClip.close();

            return null;
        }
    }

    /**
     * Gets the AudioFormat from file bytes representing a supported audio file type (Usually .wav)
     * @param wavFileContents the file contents to load.
     * @return rawAudioFormat
     */
    public static AudioFormat getAudioFormatFromWavFile(byte[] wavFileContents) {
        if (wavFileContents == null)
            throw new NullPointerException("wavFileContents");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(wavFileContents);
        AudioInputStream audioInputStream = null;
        AudioInputStream convertedInputStream;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            return audioInputStream.getFormat();
        } catch (UnsupportedAudioFileException | IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Couldn't read the audio data.");
            return null;
        } finally {
            try {
                if (audioInputStream != null)
                    audioInputStream.close();
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "Failed to close AudioInputStream.");
            }
        }
    }

    /**
     * Gets an audio Clip from file bytes representing a supported audio file type (Usually .wav)
     * @param wavFileContents the file contents to load.
     * @return audioClip
     */
    public static Clip getClipFromWavFile(byte[] wavFileContents, boolean showErrorWindow) {
        if (wavFileContents == null)
            throw new NullPointerException("wavFileContents");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(wavFileContents);
        AudioInputStream audioInputStream = null;
        AudioInputStream convertedInputStream;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(inputStream);

            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            return clip;
        } catch (UnsupportedAudioFileException ex) {
            Utils.handleError(getLogger(), ex, showErrorWindow, "The audio file format appears to be unsupported.\nThe file may only be playable using an external audio player such as VLC.");
            return null;
        } catch (IOException ex) {
            Utils.handleError(getLogger(), ex, showErrorWindow, "Problems with IO when reading the audio data.");
            return null;
        } catch (LineUnavailableException ex) {
            Utils.handleError(getLogger(), ex, showErrorWindow, "Couldn't get an audio line.");
            return null;
        } finally {
            try {
                if (audioInputStream != null)
                    audioInputStream.close();
            } catch (Throwable th) {
                Utils.handleError(null, th, false, "Failed to close AudioInputStream.");
            }
        }
    }

    /**
     * Gets raw audio data from file bytes representing a supported audio file type (Usually .wav)
     * @param wavFileContents the file contents to load.
     * @return rawAudioDataInBytes
     */
    public static byte[] getRawAudioDataFromWavFile(byte[] wavFileContents) {
        return getRawAudioDataConvertedFromWavFile(null, wavFileContents);
    }

    /**
     * Gets raw audio data from file bytes representing a supported audio file type (Usually .wav)
     * @param targetFormat the target format to convert the audio to. Null indicates no conversion should occur.
     * @param wavFileContents the file contents to load.
     * @return rawAudioDataInBytes
     */
    public static byte[] getRawAudioDataConvertedFromWavFile(AudioFormat targetFormat, byte[] wavFileContents) {
        if (wavFileContents == null)
            throw new NullPointerException("wavFileContents");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(wavFileContents);
        AudioInputStream audioInputStream;
        AudioInputStream convertedInputStream;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            if (targetFormat == null)
                targetFormat = audioInputStream.getFormat();

            convertedInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            audioInputStream.close();
        } catch (UnsupportedAudioFileException | IOException ex) {
            Utils.handleError(getLogger(), ex, true, "Couldn't read the audio data. The audio will still play, but it will have a pop.");
            return wavFileContents;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) (audioInputStream.getFrameLength() * targetFormat.getFrameSize()));
        Utils.copyInputStreamData(convertedInputStream, byteArrayOutputStream, true);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Saves the raw audio clip provided to the given wav file.
     * @param file the file to save the audio data to
     * @param format the format to get data from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToWavFile(File file, AudioFormat format, byte[] rawAudioData) {
        saveRawAudioDataToFile(file, AudioFileFormat.Type.WAVE, format, rawAudioData);
    }

    /**
     * Saves the raw audio clip provided to the given file.
     * @param file the file to save the audio data to
     * @param fileType the format (usually .wav) to save the file contents as
     * @param format the format to get data from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToFile(File file, AudioFileFormat.Type fileType, AudioFormat format, byte[] rawAudioData) {
        if (file == null)
            throw new NullPointerException("file");
        if (format == null)
            throw new NullPointerException("format");

        Clip audioClip = getClipFromRawAudioData(format, rawAudioData);
        if (audioClip == null)
            return;

        saveRawAudioDataToFile(file, fileType, audioClip, rawAudioData);
        audioClip.close();
    }

    /**
     * Saves the raw audio clip provided to the given wav file.
     * @param file the file to save the audio data to
     * @param audioClip the audio clip to source format and information from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToWavFile(File file, Clip audioClip, byte[] rawAudioData) {
        saveRawAudioDataToFile(file, AudioFileFormat.Type.WAVE, audioClip, rawAudioData);
    }

    /**
     * Saves the raw audio clip provided to the given file.
     * @param file the file to save the audio data to
     * @param fileType the format (usually .wav) to save the file contents as
     * @param audioClip the audio clip to source format and information from
     * @param rawAudioData the raw (header-less) audio data.
     */
    public static void saveRawAudioDataToFile(File file, AudioFileFormat.Type fileType, Clip audioClip, byte[] rawAudioData) {
        if (file == null)
            throw new NullPointerException("file");
        if (fileType == null)
            throw new NullPointerException("fileType");
        if (audioClip == null)
            throw new NullPointerException("audioClip");
        if (rawAudioData == null)
            throw new NullPointerException("rawAudioData");
        if (Utils.testSignature(rawAudioData, "RIFF"))
            throw new IllegalArgumentException("The 'rawAudioData' appears to have a RIFF header, making it already already directly savable as a .wav file!");

        AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(rawAudioData), audioClip.getFormat(), audioClip.getFrameLength());
        try {
            AudioSystem.write(inputStream, fileType, file);
        } catch (IOException ex) {
            handleError(null, ex, false, "Failed to save sound to file '%s'.", file.getName());
        }
    }

    /**
     * Runs the provided task on the FX Application Thread.
     * @param task the task to run
     */
    public static void runOnFXThread(Runnable task) {
        if (task == null)
            throw new NullPointerException("task");

        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /**
     * Creates a wav file with the given data.
     * @param formatHeaderSource a byte array containing ONLY a wave format header
     * @param rawAudioDataSource a byte array containing ONLY the raw audio data
     * @return wavFile
     */
    public static byte[] createWavFile(byte[] formatHeaderSource, byte[] rawAudioDataSource) {
        if (formatHeaderSource == null)
            throw new NullPointerException("formatHeaderSource");
        if (rawAudioDataSource == null)
            throw new NullPointerException("rawAudioDataSource");

        return createWavFile(formatHeaderSource, 0, formatHeaderSource.length, rawAudioDataSource, 0, rawAudioDataSource.length);
    }

    /**
     * Creates a wav file with the given data.
     * @param formatHeaderSource a byte array containing a wave format header
     * @param formatStartIndex the index where the data to the wave data header starts
     * @param formatLength the amount of bytes in the format header
     * @param rawAudioDataSource a byte array containing the raw audio data
     * @param audioDataStartIndex the index where the raw audio data starts
     * @param audioDataLength the amount of bytes to read of raw audio data
     * @return wavFile
     */
    public static byte[] createWavFile(byte[] formatHeaderSource, int formatStartIndex, int formatLength, byte[] rawAudioDataSource, int audioDataStartIndex, int audioDataLength) {
        if (formatHeaderSource == null)
            throw new NullPointerException("formatHeaderSource");
        if (rawAudioDataSource == null)
            throw new NullPointerException("rawAudioDataSource");

        if (formatLength < 16)
            throw new IllegalArgumentException("The size of a wave audio header format must be at least 16 bytes! (Was: " + formatLength + ")");
        if (formatStartIndex < 0)
            throw new IllegalArgumentException("The offset into the wave audio header byte array must be at least 0! (Was: " + formatStartIndex + ")");
        if (formatLength > formatHeaderSource.length - formatStartIndex)
            throw new IllegalArgumentException("The format size (" + formatLength + ") was greater than the total number of bytes available! (" + (formatHeaderSource.length - formatStartIndex) + ")");

        if (audioDataStartIndex < 0)
            throw new IllegalArgumentException("The offset into the raw audio data byte array must be at least 0! (Was: " + audioDataStartIndex + ")");
        if (audioDataLength < 0)
            throw new IllegalArgumentException("The size of the raw audio data must be at least 0 bytes! (Was: " + audioDataLength + ")");
        if (audioDataLength > rawAudioDataSource.length - audioDataStartIndex)
            throw new IllegalArgumentException("The format size (" + formatLength + ") was greater than the total number of bytes available! (" + (rawAudioDataSource.length - audioDataStartIndex) + ")");

        // Read the audio format header.
        byte[] waveFormatEx;
        if (formatStartIndex == 0 && formatHeaderSource.length == formatLength) {
            waveFormatEx = formatHeaderSource;
        } else {
            waveFormatEx = new byte[formatLength];
            System.arraycopy(formatHeaderSource, formatStartIndex, waveFormatEx, 0, formatLength);
        }

        // Read the raw audio data.
        byte[] rawAudioData;
        if (audioDataStartIndex == 0 && rawAudioDataSource.length == audioDataLength) {
            rawAudioData = rawAudioDataSource;
        } else {
            rawAudioData = new byte[audioDataLength];
            System.arraycopy(rawAudioDataSource, audioDataStartIndex, rawAudioData, 0, audioDataLength);
        }

        // Write the WAV file.
        ArrayReceiver receiver = new ArrayReceiver();
        DataWriter writer = new DataWriter(receiver);

        writer.writeStringBytes("RIFF");
        int fileSizeAddress = writer.writeNullPointer();
        writer.writeStringBytes("WAVE");
        writer.writeStringBytes("fmt ");
        writer.writeInt(waveFormatEx.length); // Write chunk 1 size.
        writer.writeBytes(waveFormatEx);
        writer.writeStringBytes("data");
        writer.writeInt(rawAudioData.length);
        writer.writeBytes(rawAudioData);
        writer.writeAddressAt(fileSizeAddress, writer.getIndex() - (fileSizeAddress + Constants.INTEGER_SIZE));

        writer.closeReceiver();
        return receiver.toArray();
    }

    /**
     * Safely writes the given bytes to a file, replacing the file if it already exists.
     * If an exception occurs during the writing of the file, false will be returned, and the error will be handled
     * @param logger the logger to write any error to. If null is provided, the util logger will be used.
     * @param outputFile The file to write the data to
     * @param bytes The bytes to write to the file
     * @param showPopupOnError If true and an error occurs, a popup will be displayed.
     * @return true iff the file was successfully written
     */
    public static boolean writeBytesToFile(Logger logger, File outputFile, byte[] bytes, boolean showPopupOnError) {
        if (outputFile == null)
            throw new NullPointerException("outputFile");
        if (bytes == null)
            throw new NullPointerException("bytes");

        if (outputFile.exists() && !outputFile.isFile())
            throw new IllegalArgumentException("'" + outputFile + "' is not a valid file!");

        File folder = outputFile.getParentFile();
        if (!folder.exists())
            throw new IllegalArgumentException("The path to '" + outputFile + "' did not exist, therefore the file cannot be written.");

        try {
            if (!folder.canWrite()) // We want it to properly create popups based on thread/etc, since this error is one which is likely the user's responsibility.
                throw new IOException("Can't write to the file '" + outputFile.getName() + "'." + Constants.NEWLINE + "Do you have permission to save in this folder?");

            Files.write(outputFile.toPath(), bytes);
            return true;
        } catch (IOException ex) {
            Utils.handleError(logger, ex, showPopupOnError, "Failed to save file '%s'.", outputFile.getName());
            return false;
        }
    }

    /**
     * Format a string safely, not throwing an exception if there is an issue.
     * @param str  The string to format.
     * @param args The arguments to format it with.
     * @return formattedString
     */
    public static String formatStringSafely(String str, Object... args) {
        if (args.length == 0)
            return str;

        try {
            return String.format(str, args);
        } catch (IllegalFormatException ife) {
            handleError(null, ife, false, "There was an issue formatting the string '%s'.", str);
            return str + " (FORMATTING ERROR)";
        }
    }

    /**
     * Gets the clean string of a double value.
     * Examples:
     * - 12.34 -> "12.34"
     * - 12.0  -> "12"
     * @param doubleVal The value to get the string of.
     * @return numString
     */
    public static String doubleToCleanString(double doubleVal) {
        return ((int) doubleVal == doubleVal) ? String.valueOf((int) doubleVal) : String.valueOf(doubleVal);
    }

    /**
     * Gets the number of digits for a number.
     * @param number The number to get digits for.
     * @return digitCount
     */
    public static int getDigitCount(int number) {
        return (int) Math.max(Math.log10(number), 0) + 1;
    }

    /**
     * Appends left-based padding.
     * @return paddedStr
     */
    public static String appendLeftPadding(String str, int goalSize, char add) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (goalSize - str.length()); i++)
            sb.append(add);
        sb.append(str);
        return sb.toString();
    }

    /**
     * Get the error messages as a list of strings.
     * @param throwable The error to get messages from.
     * @return errorMessages
     */
    public static List<String> getErrorMessages(Throwable throwable) {
        List<String> messages = new ArrayList<>();
        while (throwable != null) {
            messages.add(0, throwable.getMessage());
            throwable = throwable.getCause();
        }

        return messages;
    }

    /**
     * Get the error messages as a newline separated string.
     * @param throwable The error to get messages from.
     * @return errorMessages
     */
    public static String getErrorMessagesString(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        List<Throwable> errors = new ArrayList<>();
        while (throwable != null) {
            errors.add(0, throwable);
            throwable = throwable.getCause();
        }

        for (Throwable error : errors) {
            if (builder.length() > 0)
                builder.append("\n");
            String message = error.getMessage();
            if (message == null)
                message = error.getLocalizedMessage();
            builder.append(message);
        }

        return builder.toString();
    }
}