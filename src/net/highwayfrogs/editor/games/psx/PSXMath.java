package net.highwayfrogs.editor.games.psx;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.utils.data.reader.ArraySource;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.FileUtils;

import java.io.InputStream;

/**
 * Contains math utilities shared across all PS1 games.
 * Some of this file has been ported from PsyCross under the MIT License:
 * MIT License
 * Copyright (c) 2020 REDRIVER2 Project
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * Created by Kneesnap on 3/6/2025.
 */
public class PSXMath {
    public static final short FIXED_PT12_ONE = Constants.BIT_FLAG_12; // (1 << 12), 4096, 0x1000.

    // 'ratan_tbl[1025]' is the symbol used in the original ratan2 function.
    private static final short[] RATAN_TBL = readShortTable("ratan_tbl", 1025);

    // LUT for cos and sin
    // 8192 entries, even entry = Sin, odd entry = Cos
    // take TR angle (between 0 and 65535), shift R by 3 (to get between 0 and 8191), and with 8190 (to get between 0 and 8190)
    // if you want Cos, add 1 to the index
    // you then get value between -4096 and 4096, which you can divide by 4096f to get between -1 and 1
    // Sometimes this table is accessed as an integer array, seen in RotMatrix()
    public static final short[] RCOSSIN_TBL = readShortTable("rcossin_tbl", 8192);

    /**
     * Creates a rotation matrix based on the given rotation angles.
     * @param rotationAngles the rotation angles (4.12 fixed point) to create the matrix from, a length of at least 3 is expected.
     * @return newRotationMatrix
     */
    public static short[][] createRotationMatrix(short[] rotationAngles) {
        return RotMatrix(rotationAngles, new short[3][3]);
    }

    /**
     * Initialize the matrix as a rotation matrix given the rotation angles.
     * Based on the original PSX RotMatrix function.
     * Copied from Psy-Cross.
     * @param r the rotation angles (4.12 fixed point) to create the matrix from, a length of at least 3 is expected.
     * @param m output storage for the matrix, expected at least 3 rows & 3 columns
     * @return matrix
     */
    public static short[][] RotMatrix(short[] r, short[][] m) {
        int t7 = r[0]; // x
        int t9 = t7 & 0xFFF;
        int t8 = 0;
        int t3 = 0;
        int t0 = 0;
        int t6 = 0;
        int t1 = 0;
        int t4 = 0;
        int t5 = 0;
        int t2 = 0;

        if (t7 < 0) {
            t7 = -t7;
            t7 &= 0xFFF;
            t9 = getRCosSinTableInt(t7);
            t8 = (t9 << 16) >> 16;
            t3 = -t8;
        } else {
            //loc_244
            t9 = getRCosSinTableInt(t9);
            t3 = (t9 << 16) >> 16;
        }
        t0 = t9 >> 16;

        //loc_264
        t7 = r[1]; // ry
        t9 = t7 & 0xFFF;

        if (t7 < 0) {
            t7 = -t7;
            t7 &= 0xFFF;
            t9 = getRCosSinTableInt(t7);
            t4 = (t9 << 16) >> 16;
            t6 = -t4;
        } else {
            //loc_2A8
            t9 = getRCosSinTableInt(t9);
            t6 = (t9 << 16) >> 16;
            t4 = -t6;
        }
        t1 = t9 >> 16;

        //loc_2CC
        t8 = t1 * t3;
        t7 = r[2]; // rz
        m[0][2] = (short) t6;
        t9 = -t8;
        t6 = t9 >> 12;
        t8 = t1 * t0;
        m[1][2] = (short) t6;
        t9 = t7 & 0xFFF;

        if (t7 < 0) {
            t6 = t8 >> 12;
            m[2][2] = (short) t6;
            t7 = -t7;
            t7 &= 0xFFF;
            t9 = getRCosSinTableInt(t7);
            t8 = t9 & 0xFFFF;
            t5 = -t8;
        } else {
            //loc_334
            t7 = t8;
            t6 = t7 >> 12;
            m[2][2] = (short) t6;
            t9 = getRCosSinTableInt(t9);
            t5 = t9 & 0xFFFF;
        }
        t2 = t9 >> 16;

        //loc_360
        t7 = t2 * t1;
        t6 = t7 >> 12;
        m[0][0] = (short) t6;

        t7 = t5 * t1;
        t6 = -t7;
        t7 = t6 >> 12;
        m[0][1] = (short) t7;

        t7 = t2 * t4;
        t8 = t7 >> 12;
        t7 = t8 * t3;
        t6 = t7 >> 12;
        t7 = t5 * t0;
        t9 = t7 >> 12;
        t7 = t9 - t6;
        m[1][0] = (short) t7;

        t6 = t8 * t0;
        t7 = t6 >> 12;
        t6 = t5 * t3;
        t9 = t6 >> 12;
        t6 = t9 + t7;
        m[2][0] = (short) t6;

        t7 = t5 * t4;
        t8 = t7 >> 12;
        t7 = t8 * t3;
        t6 = t7 >> 12;
        t7 = t2 * t0;
        t9 = t7 >> 12;
        t7 = t9 + t6;
        m[1][1] = (short) t7;

        t6 = t8 * t0;
        t7 = t6 >> 12;
        t6 = t2 * t3;
        t9 = t6 >> 12;
        t6 = t9 - t7;
        m[2][1] = (short) t6;

        return m;
    }

    /**
     * Reverse engineered implementation of ratan2, based on PsyQ Libraries 4.0.
     * @param y
     * @param x
     * @return angle
     */
    public static short ratan2(int y, int x) {
        boolean negativeX = (x < 0);
        if (negativeX)
            x = -x;

        boolean negativeY = (y < 0);
        if (negativeY)
            y = -y;

        if ((x == 0) && (y == 0))
            return 0;

        short angle;
        int tableIndex;
        if (y < x) {
            if ((y & 0x7FE00000) == 0) {
                tableIndex = (y << 10) / x;
            } else {
                tableIndex = y / (x >> 10);
            }
            angle = RATAN_TBL[tableIndex];
        } else {
            if ((x & 0x7FE00000) == 0) {
                tableIndex = (x << 10) / y;
            } else {
                tableIndex = x / (y >> 10);
            }
            angle = (short) (0x400 - RATAN_TBL[tableIndex]);
        }

        if (negativeX)
            angle = (short) (0x800 - angle);
        if (negativeY)
            angle = (short) -angle;

        return angle;
    }

    private static int getRCosSinTableInt(int index) {
        int shiftedIndex = index << 1;
        return (RCOSSIN_TBL[shiftedIndex + 1] << 16) | (RCOSSIN_TBL[shiftedIndex] & 0xFFFF);
    }

    private static short[] readShortTable(String fileName, int length) {
        return readShortTable(FileUtils.getResourceStream("platforms/psx/" + fileName), length);
    }

    /**
     * Read an embedded table of short values.
     * @param inputStream the input stream to read the data from
     * @param length      the expected amount of entries
     * @return shortArray
     */
    public static short[] readShortTable(InputStream inputStream, int length) {
        DataReader reader = new DataReader(new ArraySource(FileUtils.readBytesFromStream(inputStream)));
        if (reader.getRemaining() != Constants.SHORT_SIZE * length)
            throw new RuntimeException("The expected entry count was " + length + ", but we found " + (reader.getRemaining() / Constants.SHORT_SIZE) + " entries instead.");

        short[] newTable = new short[length];
        for (int i = 0; i < newTable.length; i++)
            newTable[i] = reader.readShort();

        return newTable;
    }
}
