package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
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
import net.highwayfrogs.editor.utils.Utils;

/**
 * This file contains functions shared between different Sony Cambridge / Millennium Interactive games which don't adhere well to polymorphism or are shared between different places.
 * Created by Kneesnap on 9/8/2023.
 */
public class SCUtils {
    /**
     * Creates a game file from formats seen between different games.
     * @param fileEntry The entry to create a game file from.
     * @param fileData  The file data to create files from. Can be null.
     * @return newGameFile
     */
    public static SCGameFile<?> createSharedGameFile(FileEntry fileEntry, byte[] fileData) {
        return createSharedGameFile(fileEntry, fileData, false);
    }

    /**
     * Creates a game file from formats seen between different games.
     * @param fileEntry The entry to create a game file from.
     * @param fileData  The file data to create files from. Can be null.
     * @param forceSoundFile If the file should be forced to be treated as a sound file.
     * @return newGameFile
     */
    public static SCGameFile<?> createSharedGameFile(FileEntry fileEntry, byte[] fileData, boolean forceSoundFile) {
        SCGameInstance instance = fileEntry.getGameInstance();
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
            if (Utils.testSignature(fileData, vloSignature))
                return new VLOArchive(fileEntry.getGameInstance());
            if (instance.getGameType().isBefore(SCGameType.MOONWARRIOR) && (Utils.testSignature(fileData, MOFHolder.DUMMY_DATA) || Utils.testSignature(fileData, MOFFile.SIGNATURE)) || MOFAnimation.testSignature(fileData))
                return makeMofHolder(fileEntry);
            if (instance.isPSX() && Utils.testSignature(fileData, SCPlayStationVabSoundBankHeader.PSX_SIGNATURE))
                return makeSound(fileEntry, fileData);
        } else {
            if (fileEntry.hasExtension("vlo"))
                return new VLOArchive(fileEntry.getGameInstance());
            if (fileEntry.hasExtension("xmr") || fileEntry.hasExtension("xar") || fileEntry.hasExtension("xmu"))
                return makeMofHolder(fileEntry);
        }

        if (fileEntry.hasExtension("vh") || fileEntry.hasExtension("vb") || forceSoundFile)
            return makeSound(fileEntry, fileData);

        if (fileEntry.getTypeId() == WADFile.TYPE_ID)
            return new WADFile(instance); // I think this is consistent across different games.

        // This should be one of the final checks because it is prone to false-positives and being slower than most checks.
        if (PSXTIMFile.isTIMFile(instance, fileData))
            return new PSXTIMFile(instance);

        return null;
    }

    /**
     * Creates a new mof holder for the file entry and game instance combo.
     * @param fileEntry The file entry to create the file from.
     * @return mofHolder
     */
    public static MOFHolder makeMofHolder(FileEntry fileEntry) {
        SCGameInstance instance = fileEntry.getGameInstance();
        String fileName = fileEntry.getDisplayName();
        MOFHolder completeMof = null;

        // Override lookup.
        String otherMofFile = instance.getConfig().getMofParentOverrides().get(fileName);
        if (otherMofFile != null) {
            FileEntry replaceFileEntry = instance.getResourceEntryByName(otherMofFile);
            if (replaceFileEntry != null)
                completeMof = instance.getGameFile(replaceFileEntry.getResourceId());
            if (completeMof == null)
                fileEntry.getLogger().warning("MOF Parent Override for '" + otherMofFile + "' was not found. Entry: " + replaceFileEntry);
        } else {
            MOFHolder lastCompleteMOF = null;
            for (int i = fileEntry.getResourceId() - 1; i >= 0; i--) {
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

        return new MOFHolder(fileEntry.getGameInstance(), null, completeMof);
    }

    /**
     * Creates a new sound file for the file entry and game instance combo.
     * @param fileEntry The file entry to create the file from.
     * @param fileData  The contents of the file to test.
     * @return mofHolder
     */
    public static SCGameFile<?> makeSound(FileEntry fileEntry, byte[] fileData) {
        SCGameInstance instance = fileEntry.getGameInstance();
        SCGameFile<?> lastFile = instance.getGameFile(fileEntry.getResourceId() - 1);
        FileEntry lastEntry = lastFile != null ? lastFile.getIndexEntry() : null;
        boolean doFileNamesMatch = (lastFile != null) && ((!lastEntry.hasFullFilePath() || !fileEntry.hasFullFilePath())
                || Utils.stripExtension(fileEntry.getDisplayName()).equalsIgnoreCase(Utils.stripExtension(lastEntry.getDisplayName())));

        SCSplitVHFile lastSoundHeader = lastFile instanceof SCSplitVHFile ? (SCSplitVHFile) lastFile : null;
        SCSplitVBFile lastSoundBody = lastFile instanceof SCSplitVBFile ? (SCSplitVBFile) lastFile : null;

        // Ensure we find lastVH if we didn't find it before.
        if (lastSoundHeader == null && fileEntry.hasExtension("vb")) {
            FileEntry vhEntry = fileEntry.getGameInstance().getResourceEntryByName(Utils.stripExtension(fileEntry.getDisplayName()) + ".vh");
            if (vhEntry != null) {
                SCGameFile<?> vhFile = fileEntry.getGameInstance().getGameFile(vhEntry);
                if (vhFile instanceof SCSplitVHFile)
                    lastSoundHeader = (SCSplitVHFile) vhFile;
            }
        }

        // Ensure we find lastVB if we didn't find it before.
        if (lastSoundBody == null && fileEntry.hasExtension("vh")) {
            FileEntry vbEntry = fileEntry.getGameInstance().getResourceEntryByName(Utils.stripExtension(fileEntry.getDisplayName()) + ".vb");
            if (vbEntry != null) {
                SCGameFile<?> vbFile = fileEntry.getGameInstance().getGameFile(vbEntry);
                if (vbFile instanceof SCSplitVBFile)
                    lastSoundBody = (SCSplitVBFile) vbFile;
            }
        }

        // Create new object.
        if (lastSoundBody != null || fileEntry.hasExtension("vh") || (instance.isPSX() && Utils.testSignature(fileData, SCPlayStationVabSoundBankHeader.PSX_SIGNATURE))) {
            SCSplitSoundBankHeader<?, ?> newHeader = createSoundHeaderBody(instance);
            SCSplitVHFile newHeaderFile = new SCSplitVHFile(fileEntry.getGameInstance(), newHeader);
            if (doFileNamesMatch && lastSoundBody != null && lastSoundBody.getSoundBank() == null)
                newHeaderFile.createSoundBank(lastSoundBody);

            return newHeaderFile;
        } else if (lastSoundHeader != null || fileEntry.hasExtension("vb")) {
            SCSplitSoundBankBody<?, ?> newBody = createSoundBankBody(instance, fileEntry);
            SCSplitVBFile newBodyFile = new SCSplitVBFile(fileEntry.getGameInstance(), newBody);
            if (doFileNamesMatch && lastSoundHeader != null && lastSoundHeader.getSoundBank() == null)
                newBodyFile.createSoundBank(lastSoundHeader);

            return newBodyFile;
        }

        return null;
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

    private static SCSplitSoundBankBody<?, ?> createSoundBankBody(SCGameInstance instance, FileEntry fileEntry) {
        String fileName = fileEntry.getDisplayName();
        if (instance.isPSX() && instance.getGameType().isAtLeast(SCGameType.MEDIEVIL2)) {
            return new SCPlayStationMinimalSoundBankBody(instance, fileName);
        } else if (instance.isPSX()) {
            return new SCPlayStationSoundBankBody(instance, fileName);
        } else if (instance.isFrogger() && !((FroggerGameInstance) instance).getConfig().isAtLeastRetailWindows()) {
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
}