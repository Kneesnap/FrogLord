package net.highwayfrogs.editor;

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
    public static final int POINTER_SIZE = INTEGER_SIZE;
    public static final byte NULL_BYTE = (byte) 0;

    public static final int BIT_TRUE = 1;
    public static final int BIT_FALSE = 0;

    public static final int BITS_PER_BYTE = 8;

    public static final int MAP_VIEW_SCALE = 10000;
    public static final double MAP_VIEW_FAR_CLIP = 15000.0;

    public static final String DEV_ISLAND_NAME = "ISLAND.MAP";
    public static final String DEV_QB_NAME = "QB.MAP";
    public static final String SKY_LAND_PREFIX = "SKY_LAND";

    public static final int BIT_FLAG_0 = 1;
    public static final int BIT_FLAG_1 = 1 << 1;
    public static final int BIT_FLAG_2 = 1 << 2;
    public static final int BIT_FLAG_3 = 1 << 3;
    public static final int BIT_FLAG_4 = 1 << 4;
    public static final int BIT_FLAG_5 = 1 << 5;
    public static final int BIT_FLAG_6 = 1 << 6;
    public static final int BIT_FLAG_7 = 1 << 7;
    public static final int BIT_FLAG_8 = 1 << 8;

    public static final boolean ENABLE_WAD_FORMATS = true;
    public static final boolean COPY_STATIC_MOF = false;
}
