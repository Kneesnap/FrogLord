package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.FileUtils;

/**
 * Represents math capabilities used by Sony Cambridge games.
 * Created by Kneesnap on 9/7/2023.
 */
public class SCMath {
    private static short[] COS_ENTRIES;
    private static short[] SIN_ENTRIES;
    private static final int ACOSTABLE_ENTRIES = 4096;
    public static final int FIXED_POINT_ONE = 4096;

    // So... why is PI funky?
    // When reverse engineering the proper math for Frogger path calculations, I was just slightly off.
    // I was confident I had the right algorithm but the values I was getting were slightly off. Instead of 0.5 for the path arc angle I'd get 0.499984......
    // Eventually, I tried solving for PI. What I got was consistent across different path arcs, and makes the angles look accurate.
    // This made no sense UNTIL I searched GitHub. This number is the most accurate representation of PI for a 16 bit floating point number.
    // Soooo, I think this suggests mappy may have been using 16 bit floating point numbers for certain operations?? If so that... actually makes sense.
    // Some paths still don't calculate cleanly, but the fact that this is a real recognized stand-in for Pi and works across a good chunk of paths makes me think it's correct.
    // Though it's possible the ones which don't calculate cleanly were changed via GUI slider, much like the slider seen in FrogLord UI.
    // I think this is the case since many of the non-even paths don't look right if you put the clean number in.
    public static final float MAPPY_PI_HALF16 = 3.140625F;

    private static void readACosTable() {
        if (COS_ENTRIES != null && SIN_ENTRIES != null)
            return;

        // Make tables.
        COS_ENTRIES = new short[ACOSTABLE_ENTRIES];
        SIN_ENTRIES = new short[ACOSTABLE_ENTRIES];

        // Read data.
        DataReader reader = new DataReader(new ArraySource(FileUtils.readBytesFromStream(FileUtils.getResourceStream("games/sony/ACOSTABLE"))));
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