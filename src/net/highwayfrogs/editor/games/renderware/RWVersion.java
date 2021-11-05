package net.highwayfrogs.editor.games.renderware;

/**
 * This handles utilities for version-related
 * https://www.grandtheftwiki.com/RenderWare This documentation was misleading, but I figured it out.
 * This code is likely only useful for Renderware version 3. (RW2 and RW2 use vastly different file formats supposedly, Source: https://gamicus.gamepedia.com/RenderWare )
 * Some Game Info:
 * Frogger Beyond: Engine is RW 3.3, Files: 3.3.0.2
 * Frogger's Adventures: The Rescue. Engine is RW 3.6, Files appear to have been created in 3.4.0.3
 * GTA SA - RW 3.6
 * To test if a version is after another, use normal > math. It's formatted in a way where that actually works, presuming Renderware version 5 doesn't use this format. (I don't know if it exists, but if it does it doesn't use this format, since 4 doesn't either). The reason why 5 wouldn't work is because it would flip the negative bit, inverting the check.
 * Created by Kneesnap on 6/9/2020.
 */
public class RWVersion {
    /**
     * Makes a library version with all of the given information that goes into one.
     * @param version   The RenderWare version.
     * @param major     The major version.
     * @param minor     The minor version.
     * @param revision  The revision.
     * @param binaryVer The binary version. (-1 in Rescue)
     * @return libraryId
     */
    public static int makeLibraryVersion(int version, int major, int minor, int revision, short binaryVer) {
        if (version < 3 || version > 6)
            throw new RuntimeException("Version was out of the valid range [3,6]. (Was " + version + ")");
        if (major < 0 || major > 0b1111)
            throw new RuntimeException("Major Version was out of the valid range [0,15]. (Was " + major + ")");
        if (minor < 0 || minor > 0b1111)
            throw new RuntimeException("Minor Version was out of the valid range [0,15]. (Was " + minor + ")");
        if (revision < 0 || revision > 0x3F)
            throw new RuntimeException("Build Revision was out of the valid range [0,63]. (Was " + revision + ")");

        return (((version - 3) & 0b11) << 30)
                | ((major & 0x0F) << 26)
                | ((minor & 0x0F) << 22)
                | ((revision & 0x3F) << 16)
                | (binaryVer & 0xFFFF);
    }

    /**
     * Gets a version id from a number.
     * @param libraryId The library id to read from.
     * @return readVersion
     */
    public static String convertVersionToString(int libraryId) {
        return getVersion(libraryId) + "." + getMajorVersion(libraryId) + "." + getMinorVersion(libraryId) + "." + getRevision(libraryId);
    }

    /**
     * Gets the renderware version from the library id.
     * @param libraryId The library id to read from.
     * @return readVersion
     */
    public static int getVersion(int libraryId) {
        return 3 + ((libraryId >> 30) & 0b11);
    }

    /**
     * Gets the renderware major version from the library id.
     * @param libraryId The library id to read from.
     * @return majorVersion
     */
    public static int getMajorVersion(int libraryId) {
        return ((libraryId >> 26) & 0x0F);
    }

    /**
     * Gets the renderware minor version from the library id.
     * @param libraryId The library id to read from.
     * @return minorVersion
     */
    public static int getMinorVersion(int libraryId) {
        return ((libraryId >> 22) & 0x0F);
    }

    /**
     * Gets the renderware build revision from the library id.
     * @param libraryId The library id to read from.
     * @return revision
     */
    public static int getRevision(int libraryId) {
        return ((libraryId >> 16) & 0x3F);
    }

    /**
     * Gets the renderware binary version from the library id.
     * @param libraryId The library id to read from.
     * @return binaryVersion
     */
    public static short getBinaryVersion(int libraryId) {
        return (short) (libraryId & 0xFFFF);
    }

    /**
     * Runs tests to make sure this works.
     */
    @SuppressWarnings("unused")
    public static void runTests() {
        // A) getVersionInformation tests.
        System.out.println("Running Renderware version tests...");
        boolean passA1 = RWVersion.convertVersionToString(0x1003FFFF).equals("3.4.0.3");
        boolean passA2 = RWVersion.convertVersionToString(0x0401FFFF).equals("3.1.0.1") && RWVersion.getBinaryVersion(0x0401FFFF) == (short) -1;
        boolean passA3 = RWVersion.convertVersionToString(0x1C02002D).equals("3.7.0.2") && RWVersion.getBinaryVersion(0x1C02002D) == (short) 45;
        boolean passA = (passA1 && passA2 && passA3);
        if (!passA) {
            System.out.println("Test A1: " + (passA1 ? "PASSED" : "FAILED"));
            System.out.println("Test A2: " + (passA2 ? "PASSED" : "FAILED"));
            System.out.println("Test A3: " + (passA3 ? "PASSED" : "FAILED"));
            System.out.println();
        }

        // B) Test creating library ids.
        boolean passB1 = RWVersion.makeLibraryVersion(3, 4, 0, 3, (short) -1) == 0x1003FFFF;
        boolean passB2 = RWVersion.makeLibraryVersion(3, 1, 0, 1, (short) -1) == 0x0401FFFF;
        boolean passB3 = RWVersion.makeLibraryVersion(3, 7, 0, 2, (short) 45) == 0x1C02002D;
        boolean passB = (passB1 && passB2 && passB3);
        if (!passB) {
            System.out.println("Test B1: " + (passB1 ? "PASSED" : "FAILED"));
            System.out.println("Test B2: " + (passB2 ? "PASSED" : "FAILED"));
            System.out.println("Test B3: " + (passB3 ? "PASSED" : "FAILED"));
            System.out.println();
        }


        boolean passedAll = (passA && passB);
        System.out.println("Renderware Tests Complete: " + (passedAll ? "PASSED" : "FAILED"));
        System.out.println();
    }
}
