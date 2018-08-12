package net.highwayfrogs.editor;

/**
 * Holds constant variables which may come in handy.
 * Created by Kneesnap on 8/10/2018.
 */
public class Constants {

    public static final int CD_SECTOR_SIZE = 0x800;

    public static final int INTEGER_SIZE = 4;
    public static final byte NULL_BYTE = (byte) 0;

    public static final int BITS_PER_BYTE = 8;

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
}
