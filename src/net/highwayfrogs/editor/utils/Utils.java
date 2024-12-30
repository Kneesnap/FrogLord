package net.highwayfrogs.editor.utils;

import javafx.application.Platform;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.logging.ILogger;
import net.highwayfrogs.editor.utils.logging.InstanceLogger.WrappedLogger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.*;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.logging.Logger;

/**
 * Contains miscellaneous static utility functions.
 * Created by Kneesnap on 8/12/2018.
 */
public class Utils {
    private static final Map<Integer, List<Integer>> integerLists = new HashMap<>();
    private static Logger logger;
    private static ILogger instanceLogger;

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
     * Gets a logger usable in a static context.
     * This is reported as "Utils", so it's recommended to not use this, but this can be helpful for debugging.
     */
    public static ILogger getInstanceLogger() {
        if (instanceLogger != null)
            return instanceLogger;

        return instanceLogger = new WrappedLogger(getLogger());
    }

    /**
     * Creates an integer identifier from a string.
     * @param text The text to convert
     * @return identifier string
     */
    public static int makeIdentifierInteger(String text) {
        if (text == null || text.length() != 4)
            throw new RuntimeException("Cannot make signature from '" + text + "'.");

        return (text.charAt(3) << 24) | (text.charAt(2) << 16) | (text.charAt(1) << 8) | text.charAt(0);
    }

    /**
     * Gets the integer value interpreted as an identifier string.
     * @param value The value to convert
     * @return magicString
     */
    public static String toIdentifierString(int value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Constants.INTEGER_SIZE; i++)
            DataUtils.writeByteAsText(builder, (byte) (value >> (i * Constants.BITS_PER_BYTE)));

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
     * Handle an exception which should be reported to the user.
     * @param logger the logger to write the error to
     * @param th the exception to log
     * @param showWindow if true, a popup window will display the error
     */
    public static void handleError(ILogger logger, Throwable th, boolean showWindow) {
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
    public static void handleError(ILogger logger, Throwable th, boolean showWindow, int skipCount) {
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
            logger = getInstanceLogger();

        // Print stage trace.
        if (logger != null) {
            logger.throwing(callingClass != null ? callingClass.getSimpleName() : null, callingMethodName, th);
        } else {
            th.printStackTrace();
        }

        // Create popup window.
        if (showWindow) {
            if (Platform.isFxApplicationThread()) {
                FXUtils.makeErrorPopUp(null, th, false);
            } else {
                Platform.runLater(() -> FXUtils.makeErrorPopUp(null, th, false));
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
    public static void handleError(ILogger logger, Throwable th, boolean showWindow, String message, Object... arguments) {
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
    public static void handleError(ILogger logger, Throwable th, boolean showWindow, int skipCount, String message, Object... arguments) {
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
            logger = getInstanceLogger();

        // JavaFX has an error which was fixed beyond Java 8, but it throws an exception in earlier versions with using null in ComboBoxes.
        // We do NOT want to open a popup for this error, as it will for sure confuse the user into thinking something went wrong.
        // And even if it doesn't, it's highly annoying.
        if (th instanceof IndexOutOfBoundsException && "handleFxThreadError".equalsIgnoreCase(callingMethodName)) {
            // Get the exception as a string.
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            th.printStackTrace(printWriter);
            String stackTraceStr = stringWriter.toString();

            if (stackTraceStr.contains("ReadOnlyUnbackedObservableList.subList") && stackTraceStr.contains("clearAndSelect") && stackTraceStr.contains("Scene$MouseHandler.process")) {
                if (logger != null) {
                    logger.warning("Skipping an internal JavaFX error which can safely be ignored.");
                } else {
                    System.err.println("Skipping an internal JavaFX error which can safely be ignored.");
                }

                return;
            }
        }

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
                FXUtils.makeErrorPopUp(formattedMessage, th, false);
            } else {
                final String finalFormattedMessage = formattedMessage;
                Platform.runLater(() -> FXUtils.makeErrorPopUp(finalFormattedMessage, th, false));
            }
        }
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
            logger.warning(target + " had bit flag value " + NumberUtils.toHexString(value) + ", which contained unhandled bits.");
        } else {
            logger.warning("Bit flag value " + NumberUtils.toHexString(value) + " had unexpected bits set!");
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