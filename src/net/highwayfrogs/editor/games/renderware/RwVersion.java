package net.highwayfrogs.editor.games.renderware;

import net.highwayfrogs.editor.utils.NumberUtils;

/**
 * This handles utilities for version-related
 * <a ref="https://www.grandtheftwiki.com/RenderWare"/> This documentation was misleading, but I figured it out.
 * This code is likely only useful for Renderware version 3. (RW2 and RW2 use vastly different file formats supposedly, Source: https://gamicus.gamepedia.com/RenderWare )
 * Some Game Info:
 * Frogger Beyond: Engine is RW 3.3, Files report 3.3.0.2
 * Frogger's Adventures: The Rescue. Engine is RW 3.6, but the files report version in 3.4.0.3
 * 3.0.0.3 - 0x0003FFFF
 * 3.?.?.? - 0x0800FFFF (GTA III)
 * 3.1.0.0 - 0x00000310 (GTA III)
 * 3.3.0.2 - 0x0C02FFFF (GTA VC, Frogger Beyond)
 * 3.4.0.3 - 0x1003FFFF (GTA VC, Frogger's Adventures The Rescue)
 * 3.6.0.3 - 0x1803FFFF (GTA SA, Frogger Ancient Shadow)
 * To test if a version is after another, use normal > math. It's formatted in a way where that actually works, presuming Renderware version 5 doesn't use this format. (I don't know if it exists, but if it does it doesn't use this format, since 4 doesn't either). The reason why 5 wouldn't work is because it would flip the negative bit, inverting the check.
 * Created by Kneesnap on 6/9/2020.
 */
public class RwVersion {
    public static final int VERSION_3302 = 0x0C02FFFF; // Frogger Beyond
    public static final int VERSION_3403 = 0x1003FFFF; // Frogger's Adventures: The Rescue
    public static final int VERSION_3602 = 0x1802FFFF;
    public static final int VERSION_3603 = 0x1803FFFF; // Frogger Ancient Shadow

    /**
     * Tests if the first version provided is at or before the second version provided.
     * @param version1 the first version to test
     * @param version2 the second version to test
     * @return true iff version1 <= version2
     */
    public static boolean isAtOrBefore(int version1, int version2) {
        if (isOldFormat(version1) == isOldFormat(version2))
            return version1 <= version2; // Don't mix the old format with the new format.

        int rwVersion1 = getVersion(version1);
        int rwVersion2 = getVersion(version2);
        if (rwVersion1 != rwVersion2)
            return rwVersion1 < rwVersion2;

        int rwMajorVersion1 = getMajorVersion(version1);
        int rwMajorVersion2 = getMajorVersion(version2);
        if (rwMajorVersion1 != rwMajorVersion2)
            return rwMajorVersion1 < rwMajorVersion2;

        int rwMinorVersion1 = getMinorVersion(version1);
        int rwMinorVersion2 = getMinorVersion(version2);
        if (rwMinorVersion1 != rwMinorVersion2)
            return rwMinorVersion1 < rwMinorVersion2;

        int rwRevision1 = getRevision(version1);
        int rwRevision2 = getRevision(version2);
        if (rwRevision1 != rwRevision2)
            return rwRevision1 < rwRevision2;

        int rwBuild1 = getBinaryVersion(version1);
        int rwBuild2 = getBinaryVersion(version2);
        return (rwBuild1 == (short) 0xFFFF || rwBuild2 == (short) 0xFFFF) || rwBuild1 <= rwBuild2;
    }

    /**
     * Tests if the first version provided is at least the second version provided.
     * @param version1 the first version to test
     * @param version2 the second version to test
     * @return true iff version1 >= version2
     */
    public static boolean isAtLeast(int version1, int version2) {
        return isAtOrBefore(version2, version1);
    }

    /**
     * Makes a library version with the given information that goes into one.
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

        if (version == 3 && ((major == 1 && minor == 0 && revision == 0) || major == 0)) // Write in the old format if we're in that version range.
            return (version << 8) | (major << 4) | minor;

        return (((version - 3) & 0b11) << 30)
                | ((major & 0x0F) << 26)
                | ((minor & 0x0F) << 22)
                | ((revision & 0x3F) << 16)
                | (binaryVer & 0xFFFF); // I think this was FFFF for all non-internal builds of RenderWare. Complete guess on my part, but doing so would allow the version to be tested with a simple > or < check.
    }


    /**
     * Check if a version ID appears valid.
     * @param version the version to test
     * @return true iff the version appears valid
     */
    public static boolean doesVersionAppearValid(int version) {
        // According to https://gtamods.com/wiki/RenderWare_binary_stream_file,
        // RenderWare 4.x dropped support for v3's format, so we should never see anything beyond v3.
        // Also, RenderWare 2.x used a different format.

        // According to the 2007 archive.org backup of renderware.com, RW 3.7 was the last version of RenderWare Graphics.
        // I've never seen a minor version which isn't zero, but I'll leave some wiggle room.
        return getVersion(version) == 3 && getMajorVersion(version) <= 7 && getMinorVersion(version) <= 4
                && (getBinaryVersion(version) == -1 || (getBinaryVersion(version) >= 0 && getBinaryVersion(version) <= 10));
    }

    /**
     * Gets a version id as a debug string from the version number.
     * @param version The version id to get the string from.
     * @return debug string
     */
    public static String getDebugString(int version) {
        return convertVersionToString(version) + "/" + NumberUtils.toHexString(version);
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
     * RenderWare 3 had two different versioning formats.
     * @param versionId the version ID to test
     * @return true iff the version provided appears to be in the old format.
     */
    public static boolean isOldFormat(int versionId) {
        // Taken from https://gtamods.com/wiki/RenderWare,
        // 3.1.0.0 and before had no binary version and the library ID stamp was just 0x00000VJN (no 0x30000 subtracted).
        // Version 3.1.0.0 for instance would be encoded as 0x00000310.
        // To find out what version a file has when reading, RW checks the upper 16 bits and assumes the old format when they're zero.
        // Version 3.1.0.1 (used in some GTA III files, build FFFF) on the other hand is encoded as 0x0401FFFF.
        return (versionId & 0xFFFF0000) == 0;
    }

    /**
     * Gets the renderware version from the library id.
     * @param libraryId The library id to read from.
     * @return readVersion
     */
    public static int getVersion(int libraryId) {
        if (isOldFormat(libraryId)) {
            return (libraryId >> 8) & 0b1111;
        } else {
            return 3 + ((libraryId >> 30) & 0b11);
        }
    }

    /**
     * Gets the renderware major version from the library id.
     * @param libraryId The library id to read from.
     * @return majorVersion
     */
    public static int getMajorVersion(int libraryId) {
        if (isOldFormat(libraryId)) {
            return (libraryId >> 4) & 0b1111;
        } else {
            return ((libraryId >> 26) & 0x0F);
        }
    }

    /**
     * Gets the renderware minor version from the library id.
     * @param libraryId The library id to read from.
     * @return minorVersion
     */
    public static int getMinorVersion(int libraryId) {
        if (isOldFormat(libraryId)) {
            return libraryId & 0b1111;
        } else {
            return ((libraryId >> 22) & 0x0F);
        }
    }

    /**
     * Gets the renderware build revision from the library id.
     * @param libraryId The library id to read from.
     * @return revision
     */
    public static int getRevision(int libraryId) {
        if (isOldFormat(libraryId)) {
            return 0; // Wasn't set in the old format.
        } else {
            return ((libraryId >> 16) & 0x3F);
        }
    }

    /**
     * Gets the renderware binary version from the library id.
     * @param libraryId The library id to read from.
     * @return binaryVersion
     */
    public static short getBinaryVersion(int libraryId) {
        if (isOldFormat(libraryId)) {
            return (short) 0; // Wasn't set in the old format.
        } else {
            return (short) (libraryId & 0xFFFF);
        }
    }

    /**
     * Runs tests to make sure this works.
     * TODO: At some point we should probably import unit tests from MTF, and this should be moved to a unit test package.
     */
    @SuppressWarnings("unused")
    public static void runTests() {
        // A) getVersionInformation tests.
        System.out.println("Running Renderware version tests...");
        boolean passA1 = RwVersion.convertVersionToString(0x1003FFFF).equals("3.4.0.3");
        boolean passA2 = RwVersion.convertVersionToString(0x0401FFFF).equals("3.1.0.1") && RwVersion.getBinaryVersion(0x0401FFFF) == (short) -1;
        boolean passA3 = RwVersion.convertVersionToString(0x1C02002D).equals("3.7.0.2") && RwVersion.getBinaryVersion(0x1C02002D) == (short) 45;
        boolean passA = (passA1 && passA2 && passA3);
        if (!passA) {
            System.out.println("Test A1: " + (passA1 ? "PASSED" : "FAILED"));
            System.out.println("Test A2: " + (passA2 ? "PASSED" : "FAILED"));
            System.out.println("Test A3: " + (passA3 ? "PASSED" : "FAILED"));
            System.out.println();
        }

        // B) Test creating library ids.
        boolean passB1 = RwVersion.makeLibraryVersion(3, 4, 0, 3, (short) -1) == 0x1003FFFF;
        boolean passB2 = RwVersion.makeLibraryVersion(3, 1, 0, 1, (short) -1) == 0x0401FFFF;
        boolean passB3 = RwVersion.makeLibraryVersion(3, 7, 0, 2, (short) 45) == 0x1C02002D;
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