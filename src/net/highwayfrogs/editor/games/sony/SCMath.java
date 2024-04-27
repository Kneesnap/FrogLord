package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.file.reader.ArraySource;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.utils.Utils;

/**
 * Represents math capabilities used by Sony Cambridge games.
 * Created by Kneesnap on 9/7/2023.
 */
public class SCMath {
    private static short[] COS_ENTRIES;
    private static short[] SIN_ENTRIES;
    private static final int ACOSTABLE_ENTRIES = 4096;

    private static void readACosTable() {
        if (COS_ENTRIES != null && SIN_ENTRIES != null)
            return;

        // Make tables.
        COS_ENTRIES = new short[ACOSTABLE_ENTRIES];
        SIN_ENTRIES = new short[ACOSTABLE_ENTRIES];

        // Read data.
        DataReader reader = new DataReader(new ArraySource(Utils.readBytesFromStream(Utils.getResourceStream("games/sony/ACOSTABLE"))));
        for (int i = 0; i < ACOSTABLE_ENTRIES; i++) {
            SIN_ENTRIES[i] = reader.readShort();
            COS_ENTRIES[i] = reader.readShort();
        }
    }

    /**
     * Perform rCos on an angle.
     * @param angle The angle to perform rCos on.
     * @return rCos
     */
    public static int rcos(int angle) {
        readACosTable();
        return COS_ENTRIES[angle & 0xFFF]; // Angle is a fixed point number between where 4096 is the integer 1. The mask removes the integer part.
    }

    /**
     * Perform rSin on an angle.
     * @param angle The angle to perform rSin on.
     * @return rSin
     */
    public static int rsin(int angle) {
        readACosTable();
        return SIN_ENTRIES[angle & 0xFFF]; // & 0xFFF cuts off the decimal point.
    }
}