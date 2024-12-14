package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.model.actionset.PTActionSetFile;
import net.highwayfrogs.editor.games.sony.shared.model.skeleton.PTSkeletonFile;
import net.highwayfrogs.editor.games.sony.shared.model.staticmesh.PTStaticFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.WADFile;
import net.highwayfrogs.editor.games.sony.shared.mwd.mwi.MWIResourceEntry;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVBFile;
import net.highwayfrogs.editor.games.sony.shared.sound.SCSplitVHFile;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationMinimalSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCPlayStationSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCWindowsPreReleaseSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.body.SCWindowsRetailSoundBankBody;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationMinimalSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCPlayStationVabSoundBankHeader;
import net.highwayfrogs.editor.games.sony.shared.sound.header.SCWindowsSoundBankHeader;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.DataUtils;
import net.highwayfrogs.editor.utils.FileUtils;

/**
 * This file contains functions shared between different Sony Cambridge / Millennium Interactive games which don't adhere well to polymorphism or are shared between different places.
 * Created by Kneesnap on 9/8/2023.
 */
public class SCUtils {
    /**
     * Creates a game file from formats seen between different games.
     * @param resourceEntry The entry to create a game file from.
     * @param fileData  The file data to create files from. Can be null.
     * @return newGameFile
     */
    public static SCGameFile<?> createSharedGameFile(MWIResourceEntry resourceEntry, byte[] fileData) {
        SCGameInstance instance = resourceEntry.getGameInstance();
        String vloSignature;
        switch (instance.getPlatform()) {
            case WINDOWS:
                vloSignature = VLOArchive.PC_SIGNATURE;
                break;
            case PLAYSTATION:
                vloSignature = VLOArchive.PSX_SIGNATURE;
                break;
            default:
                throw new RuntimeException("Unsupported target platform: " + instance.getPlatform());
        }

        // If there's file-data, it's the best indicator so let's use it first and foremost.
        if (fileData != null) {
            if (DataUtils.testSignature(fileData, vloSignature))
                return new VLOArchive(resourceEntry.getGameInstance());

            if (instance.isPSX() && DataUtils.testSignature(fileData, SCPlayStationVabSoundBankHeader.PSX_SIGNATURE))
                return makeSound(resourceEntry, fileData, SCForcedLoadSoundFileType.HEADER);

            // 3D models.
            if (instance.getGameType().isAtLeast(SCGameType.MOONWARRIOR)) {
                if (DataUtils.testSignature(fileData, PTStaticFile.IDENTIFIER_STRING))
                    return new PTStaticFile(resourceEntry.getGameInstance());
                if (DataUtils.testSignature(fileData, PTSkeletonFile.IDENTIFIER_STRING))
                    return new PTSkeletonFile(resourceEntry.getGameInstance());
                if (DataUtils.testSignature(fileData, PTActionSetFile.IDENTIFIER_STRING))
                    return new PTActionSetFile(resourceEntry.getGameInstance());
            } else {
                if (DataUtils.testSignature(fileData, MOFHolder.DUMMY_DATA) || DataUtils.testSignature(fileData, MOFFile.SIGNATURE) || MOFAnimation.testSignature(fileData))
                    return makeMofHolder(resourceEntry);
            }
        } else {
            if (resourceEntry.hasExtension("vlo"))
                return new VLOArchive(resourceEntry.getGameInstance());
            if (resourceEntry.hasExtension("xmr") || resourceEntry.hasExtension("xar") || resourceEntry.hasExtension("xmu"))
                return makeMofHolder(resourceEntry);
        }

        if (resourceEntry.hasExtension("vh") || resourceEntry.hasExtension("vb"))
            return makeSound(resourceEntry, fileData, null);

        if (resourceEntry.getTypeId() == WADFile.TYPE_ID)
            return new WADFile(instance); // I think this is consistent across different games.

        // This should be one of the final checks because it is prone to false-positives and being slower than most checks.
        if (PSXTIMFile.isTIMFile(instance, fileData))
            return new PSXTIMFile(instance);

        return null;
    }

    /**
     * Creates a new mof holder for the file entry and game instance combo.
     * @param resourceEntry The file entry to create the file from.
     * @return mofHolder
     */
    public static MOFHolder makeMofHolder(MWIResourceEntry resourceEntry) {
        SCGameInstance instance = resourceEntry.getGameInstance();
        String fileName = resourceEntry.getDisplayName();
        MOFHolder completeMof = null;

        // Override lookup.
        String otherMofFile = instance.getVersionConfig().getMofParentOverrides().get(fileName);
        if (otherMofFile != null) {
            MWIResourceEntry replaceEntry = instance.getResourceEntryByName(otherMofFile);
            if (replaceEntry != null)
                completeMof = instance.getGameFile(replaceEntry.getResourceId());
            if (completeMof == null)
                resourceEntry.getLogger().warning("MOF Parent Override for '" + otherMofFile + "' was not found. Entry: " + replaceEntry);
        } else {
            MOFHolder lastCompleteMOF = null;
            for (int i = resourceEntry.getResourceId() - 1; i >= 0; i--) {
                SCGameFile<?> testMof = instance.getGameFile(i);
                if (testMof instanceof MOFHolder) {
                    MOFHolder newHolder = (MOFHolder) testMof;
                    if (!newHolder.isIncomplete()) {
                        lastCompleteMOF = newHolder;
                        break;
                    }
                }
            }

            completeMof = lastCompleteMOF;
        }

        return new MOFHolder(resourceEntry.getGameInstance(), null, completeMof);
    }

    /**
     * Creates a new sound file for the file entry and game instance combo.
     * @param resourceEntry The file entry to create the file from.
     * @param fileData  The contents of the file to test.
     * @return mofHolder
     */
    public static SCGameFile<?> makeSound(MWIResourceEntry resourceEntry, byte[] fileData, SCForcedLoadSoundFileType forcedType) {
        SCGameInstance instance = resourceEntry.getGameInstance();
        SCGameFile<?> lastFile = instance.getGameFile(resourceEntry.getResourceId() - 1);
        SCGameFile<?> nextFile = instance.getGameFile(resourceEntry.getResourceId() + 1);
        MWIResourceEntry lastEntry = lastFile != null ? lastFile.getIndexEntry() : null;
        MWIResourceEntry nextEntry = nextFile != null ? nextFile.getIndexEntry() : null;
        boolean lastFileNameMatches = (lastEntry != null) && ((!lastEntry.hasFullFilePath() || !resourceEntry.hasFullFilePath())
                || FileUtils.stripExtension(resourceEntry.getDisplayName()).equalsIgnoreCase(FileUtils.stripExtension(lastEntry.getDisplayName())));
        boolean nextFileNamesMatches = (nextEntry != null) && ((!nextEntry.hasFullFilePath() || !resourceEntry.hasFullFilePath())
                || FileUtils.stripExtension(resourceEntry.getDisplayName()).equalsIgnoreCase(FileUtils.stripExtension(nextEntry.getDisplayName())));

        SCSplitVHFile lastSoundHeader = lastFile instanceof SCSplitVHFile ? (SCSplitVHFile) lastFile : null;
        SCSplitVBFile lastSoundBody = lastFile instanceof SCSplitVBFile ? (SCSplitVBFile) lastFile : null;

        // Ensure we find lastVH if we didn't find it before.
        if (lastSoundHeader == null && resourceEntry.hasExtension("vb")) {
            MWIResourceEntry vhEntry = resourceEntry.getGameInstance().getResourceEntryByName(FileUtils.stripExtension(resourceEntry.getDisplayName()) + ".vh");
            if (vhEntry != null) {
                SCGameFile<?> vhFile = resourceEntry.getGameInstance().getGameFile(vhEntry);
                if (vhFile instanceof SCSplitVHFile)
                    lastSoundHeader = (SCSplitVHFile) vhFile;
            }
        }

        // Ensure we find lastVB if we didn't find it before.
        if (lastSoundBody == null && resourceEntry.hasExtension("vh")) {
            MWIResourceEntry vbEntry = resourceEntry.getGameInstance().getResourceEntryByName(FileUtils.stripExtension(resourceEntry.getDisplayName()) + ".vb");
            if (vbEntry != null) {
                SCGameFile<?> vbFile = resourceEntry.getGameInstance().getGameFile(vbEntry);
                if (vbFile instanceof SCSplitVBFile)
                    lastSoundBody = (SCSplitVBFile) vbFile;
            }
        }

        // Create new object.
        if (lastSoundBody != null || resourceEntry.hasExtension("vh") || forcedType == SCForcedLoadSoundFileType.HEADER || (instance.isPSX() && DataUtils.testSignature(fileData, SCPlayStationVabSoundBankHeader.PSX_SIGNATURE))) {
            SCSplitSoundBankHeader<?, ?> newHeader = createSoundHeaderBody(instance);
            SCSplitVHFile newHeaderFile = new SCSplitVHFile(resourceEntry.getGameInstance(), newHeader);
            if ((lastFileNameMatches || !nextFileNamesMatches) && lastSoundBody != null && lastSoundBody.getSoundBank() == null)
                newHeaderFile.createSoundBank(lastSoundBody);

            return newHeaderFile;
        } else if (lastSoundHeader != null || resourceEntry.hasExtension("vb") || forcedType == SCForcedLoadSoundFileType.BODY) {
            SCSplitSoundBankBody<?, ?> newBody = createSoundBankBody(instance, resourceEntry);
            SCSplitVBFile newBodyFile = new SCSplitVBFile(resourceEntry.getGameInstance(), newBody);
            if ((lastFileNameMatches || !nextFileNamesMatches) && lastSoundHeader != null && lastSoundHeader.getSoundBank() == null)
                newBodyFile.createSoundBank(lastSoundHeader);

            return newBodyFile;
        }

        return null;
    }

    public enum SCForcedLoadSoundFileType {
        HEADER, BODY
    }

    private static SCSplitSoundBankHeader<?, ?> createSoundHeaderBody(SCGameInstance instance) {
        if (instance.isPSX() && instance.getGameType().isAtLeast(SCGameType.MEDIEVIL2)) {
            return new SCPlayStationMinimalSoundBankHeader(instance);
        } else if (instance.isPSX()) {
            return new SCPlayStationVabSoundBankHeader(instance);
        } else {
            return new SCWindowsSoundBankHeader<>(instance);
        }
    }

    private static SCSplitSoundBankBody<?, ?> createSoundBankBody(SCGameInstance instance, MWIResourceEntry resourceEntry) {
        String fileName = resourceEntry.getDisplayName();
        if (instance.isPSX() && instance.getGameType().isAtLeast(SCGameType.MEDIEVIL2)) {
            return new SCPlayStationMinimalSoundBankBody(instance, fileName);
        } else if (instance.isPSX()) {
            return new SCPlayStationSoundBankBody(instance, fileName);
        } else if (instance.getGameType().isAtOrBefore(SCGameType.OLD_FROGGER) || (instance.isFrogger() && !((FroggerGameInstance) instance).getVersionConfig().isAtLeastRetailWindows())) {
            return new SCWindowsPreReleaseSoundBankBody(instance, fileName);
        } else {
            return new SCWindowsRetailSoundBankBody(instance, fileName);
        }
    }

    /**
     * Add textures to the atlas used by model files, texture remaps, etc.
     * @param atlas      The texture atlas to add these textures to.
     * @param vloArchive The VLO to add textures from.
     */
    @SuppressWarnings("unused")
    public static void addAtlasTextures(TextureAtlas atlas, VLOArchive vloArchive) {
        if (vloArchive == null)
            return;

        for (int i = 0; i < vloArchive.getImages().size(); i++)
            atlas.addTexture(vloArchive.getImages().get(i));
    }

    /**
     * Calculates the checksum of the provided byte array.
     * This checksum is used in MediEvil II and C-12 Final Resistance, which this function has been validated against.
     * @param rawData the data to calculate the checksum from
     * @return checksum
     */
    public static int calculateChecksum(byte[] rawData) {
        long checksum = 0;
        for (int i = 0; i < rawData.length; i += Constants.INTEGER_SIZE) {
            int byteCount = Math.min(Constants.INTEGER_SIZE, rawData.length - i);
            int tempValue = DataUtils.readNumberFromBytes(rawData, byteCount, i);
            checksum += (tempValue & 0xFFFFFFFFL);
            if ((tempValue & 0xFFFFFFFFL) > (checksum & 0xFFFFFFFFL))
                checksum++;
        }

        // Zero indicates no checksum, so don't return zero.
        int checksum32 = (int) (checksum & 0xFFFFFFFFL);
        return (checksum32 != 0) ? checksum32 : -1;
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
        return stripWin95(FileUtils.stripExtension(name));
    }
}