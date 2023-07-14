package net.highwayfrogs.editor;

import javafx.scene.text.Font;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Holds constant variables which may come in handy.
 * Created by Kneesnap on 8/10/2018.
 */
public class Constants {
    public static final String NEWLINE = System.lineSeparator();

    public static final int CD_SECTOR_SIZE = 0x800;

    public static final int BYTE_SIZE = 1;
    public static final int SHORT_SIZE = 2;
    public static final int INTEGER_SIZE = 4;
    public static final int FLOAT_SIZE = 4;
    public static final int POINTER_SIZE = INTEGER_SIZE;
    public static final byte NULL_BYTE = (byte) 0;

    public static final int BIT_TRUE = 1;
    public static final int BIT_FALSE = 0;

    public static final int BITS_PER_BYTE = 8;

    public static final String SKY_LAND_PREFIX = "SKY_LAND";

    public static final String VERSION = "v0.6.0";
    public static final int UPDATE_VERSION = 1; // Update this with every release.

    public static final int BIT_FLAG_0 = 1;
    public static final int BIT_FLAG_1 = 1 << 1;
    public static final int BIT_FLAG_2 = 1 << 2;
    public static final int BIT_FLAG_3 = 1 << 3;
    public static final int BIT_FLAG_4 = 1 << 4;
    public static final int BIT_FLAG_5 = 1 << 5;
    public static final int BIT_FLAG_6 = 1 << 6;
    public static final int BIT_FLAG_7 = 1 << 7;
    public static final int BIT_FLAG_8 = 1 << 8;
    public static final int BIT_FLAG_9 = 1 << 9;
    public static final int BIT_FLAG_10 = 1 << 10;
    public static final int BIT_FLAG_11 = 1 << 11;
    public static final int BIT_FLAG_12 = 1 << 12;
    public static final int BIT_FLAG_13 = 1 << 13;
    public static final int BIT_FLAG_14 = 1 << 14;
    public static final int BIT_FLAG_15 = 1 << 15;
    public static final int BIT_FLAG_16 = 1 << 16;
    public static final int BIT_FLAG_17 = 1 << 17;
    public static final int BIT_FLAG_18 = 1 << 18;
    public static final int BIT_FLAG_19 = 1 << 19;
    public static final int BIT_FLAG_20 = 1 << 20;

    public static final Font SYSTEM_BOLD_FONT = new Font("System Bold", 12);
    public static final String DUMMY_FILE_NAME = "NULL";

    public static final boolean ENABLE_WAD_FORMATS = true;
    public static final boolean LOG_EXE_INFO = false;

    public static final List<Integer> PC_ISLAND_REMAP = Arrays.asList(221, 862, 860, 859, 688, 863, 857, 694, 722, 854, 729, 857, 854, 853, 850, 3, 863);
    public static final List<Integer> PSX_ISLAND_REMAP = Arrays.asList(363, 1191, 1189, 1188, 996, 1192, 1186, 1002, 1033, 1183, 1040, 1186, 1183, 1182, 1179, 4, 1192);

    public static final Color COLOR_TURQUOISE = new Color(0, 128, 128);
    public static final Color COLOR_DEEP_GREEN = new Color(0, 128, 0);
    public static final Color COLOR_DARK_YELLOW = new Color(128, 128, 0);
    public static final Color COLOR_TAN = new Color(240, 240, 240);

    /**
     * Log exe info if the option is enabled.
     * @param obj The object to log.
     */
    public static void logExeInfo(Object obj) {
        if (LOG_EXE_INFO)
            System.out.println(obj);
    }
}