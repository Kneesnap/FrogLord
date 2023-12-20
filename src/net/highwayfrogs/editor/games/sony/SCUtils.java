package net.highwayfrogs.editor.games.sony;

import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.file.WADFile;
import net.highwayfrogs.editor.file.mof.MOFFile;
import net.highwayfrogs.editor.file.mof.MOFHolder;
import net.highwayfrogs.editor.file.mof.animation.MOFAnimation;
import net.highwayfrogs.editor.file.sound.VBAudioBody;
import net.highwayfrogs.editor.file.sound.VHAudioHeader;
import net.highwayfrogs.editor.file.sound.VHFile;
import net.highwayfrogs.editor.file.sound.prototype.PrototypeVBFile;
import net.highwayfrogs.editor.file.sound.psx.PSXVBFile;
import net.highwayfrogs.editor.file.sound.psx.PSXVHFile;
import net.highwayfrogs.editor.file.sound.retail.RetailPCVBFile;
import net.highwayfrogs.editor.file.vlo.VLOArchive;
import net.highwayfrogs.editor.games.psx.PSXTIMFile;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.shared.TextureRemapArray;
import net.highwayfrogs.editor.gui.texture.atlas.TextureAtlas;
import net.highwayfrogs.editor.utils.Utils;

/**
 * This file contains functions shared between different Sony Cambridge / Millennium Interactive games which don't adhere well to polymorphism or are shared between different places.
 * Created by Kneesnap on 9/8/2023.
 */
public class SCUtils {
    /**
     * Test if a value looks like a valid pointer.
     * Built for Frogger 1997, but will likely be used in most games.
     * @param platform The platform value to test.
     * @param pointer  The pointer value to test.
     * @return looks like a pointer.
     */
    public static boolean isValidLookingPointer(SCGamePlatform platform, long pointer) {
        if (platform == SCGamePlatform.PLAYSTATION) {
            // Tests if the value is within address space of KSEG0, since that's Main RAM.
            // The first 64K (0x10000 bytes) are skipped because it's reserved for the BIOS.
            // There is 2 MB of RAM in this area, so the data must be within such a range.
            return (pointer >= 0x80010000L && pointer < 0x80200000L) || (pointer >= 0x10000L && pointer < 0x200000L);
        } else if (platform == SCGamePlatform.WINDOWS) {
            return (pointer & 0xFFF00000) == 0x00400000;
        } else {
            throw new RuntimeException("Unsupported platform '" + platform + "'.");
        }
    }

    /**
     * Creates a game file from formats seen between different games.
     * @param fileEntry The entry to create a game file from.
     * @param fileData  The file data to create files from. Can be null.
     * @return newGameFile
     */
    public static SCGameFile<?> createSharedGameFile(FileEntry fileEntry, byte[] fileData) {
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
            if (instance.isPSX() && Utils.testSignature(fileData, PSXVHFile.PSX_SIGNATURE))
                return makeSound(fileEntry, fileData);
        } else {
            if (fileEntry.hasExtension("vlo"))
                return new VLOArchive(fileEntry.getGameInstance());
            if (fileEntry.hasExtension("xmr") || fileEntry.hasExtension("xar") || fileEntry.hasExtension("xmu"))
                return makeMofHolder(fileEntry);
        }

        if (fileEntry.hasExtension("vh") || fileEntry.hasExtension("vb"))
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
                System.out.println("MOF Parent Override for '" + otherMofFile + "' was not found. Entry: " + replaceFileEntry);
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

        VHAudioHeader lastVH = lastFile instanceof VHAudioHeader ? (VHAudioHeader) lastFile : null;
        VBAudioBody<?> lastVB = lastFile instanceof VBAudioBody<?> ? (VBAudioBody<?>) lastFile : null;

        // Ensure we find lastVH if we didn't find it before.
        if (lastVH == null && fileEntry.hasExtension("vb")) {
            FileEntry vhEntry = fileEntry.getGameInstance().getResourceEntryByName(Utils.stripExtension(fileEntry.getDisplayName()) + ".vh");
            if (vhEntry != null) {
                SCGameFile<?> vhFile = fileEntry.getGameInstance().getGameFile(vhEntry);
                if (vhFile instanceof VHAudioHeader)
                    lastVH = (VHAudioHeader) vhFile;
            }
        }

        // Ensure we find lastVB if we didn't find it before.
        if (lastVB == null && fileEntry.hasExtension("vh")) {
            FileEntry vbEntry = fileEntry.getGameInstance().getResourceEntryByName(Utils.stripExtension(fileEntry.getDisplayName()) + ".vb");
            if (vbEntry != null) {
                SCGameFile<?> vbFile = fileEntry.getGameInstance().getGameFile(vbEntry);
                if (vbFile instanceof VBAudioBody<?>)
                    lastVB = (VBAudioBody<?>) vbFile;
            }
        }

        // Create new object.
        if (lastVB != null || fileEntry.hasExtension("vh") || (instance.isPSX() && Utils.testSignature(fileData, PSXVHFile.PSX_SIGNATURE))) {
            VHAudioHeader newHeader = instance.isPSX() ? new PSXVHFile(fileEntry.getGameInstance()) : new VHFile(fileEntry.getGameInstance());
            if (lastVB != null && doFileNamesMatch)
                newHeader.setVbFile(lastVB);

            return newHeader;
        } else if (lastVH != null || fileEntry.hasExtension("vb")) {
            VBAudioBody<?> newBody;
            if (instance.isPSX()) {
                newBody = new PSXVBFile(fileEntry.getGameInstance());
            } else if (instance.isFrogger() && !((FroggerGameInstance) instance).getConfig().isAtLeastRetailWindows()) {
                newBody = new PrototypeVBFile(fileEntry.getGameInstance());
            } else {
                newBody = new RetailPCVBFile(fileEntry.getGameInstance());
            }

            if (lastVH != null && doFileNamesMatch)
                newBody.setHeader(lastVH);

            return newBody;
        }

        return null;
    }

    /**
     * Add textures to the atlas used by model files, texture remaps, etc.
     * @param atlas      The texture atlas to add these textures to.
     * @param vloArchive The VLO to add textures from.
     */
    public static void addAtlasTextures(TextureAtlas atlas, VLOArchive vloArchive) {
        if (vloArchive == null)
            return;

        for (int i = 0; i < vloArchive.getImages().size(); i++)
            atlas.addTexture(vloArchive.getImages().get(i));
    }

    /**
     * Add textures to the atlas used by model files, texture remaps, etc.
     * @param atlas        The texture atlas to add these textures to.
     * @param wadFile      An optional WAD file to scan for models to add VLOs from.
     * @param textureRemap An optional texture remap to add VLOs from.
     */
    public static void addAtlasTextures(TextureAtlas atlas, VLOArchive mainVlo, WADFile wadFile, TextureRemapArray textureRemap) {
        if (mainVlo != null)
            addAtlasTextures(atlas, mainVlo);

        // TODO: Finish this once we've got stuff figured out.
    }
}