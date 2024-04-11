package net.highwayfrogs.editor.games.konami.greatquest;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.konami.greatquest.loading.kcLoadContext;
import net.highwayfrogs.editor.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A base TGQ file.
 * Created by Kneesnap on 8/17/2019.
 */
@Getter
public abstract class TGQFile extends GameObject {
    private final TGQBinFile mainArchive;
    private byte[] rawData;
    private String fileName;
    private String filePath;
    private int nameHash;
    private boolean collision; // This is true iff there are multiple files that share the hash.
    private boolean compressed;

    public TGQFile(TGQBinFile mainArchive) {
        this.mainArchive = mainArchive;
    }

    @Override
    public void save(DataWriter writer) {
        throw new UnsupportedOperationException("TGQ Files cannot be saved now.");
    }

    /**
     * The first method called after all files have been loaded.
     */
    public void afterLoad1(kcLoadContext context) {
        // Do nothing.
    }

    /**
     * The second method called after all files have been loaded.
     */
    public void afterLoad2(kcLoadContext context) {
        // Do nothing.
    }

    /**
     * Initialize the information about this file.
     * @param realName   This file's raw name. Can be null.
     * @param compressed Whether this file is compressed.
     */
    public void init(String realName, boolean compressed, int hash, byte[] rawBytes, boolean collision) {
        setFilePath(realName);
        this.compressed = compressed;
        this.nameHash = hash;
        this.rawData = rawBytes;
        this.collision = collision;
    }

    /**
     * Sets the raw file name of this file.
     * @param filePath The raw file name. (Full path)
     */
    public void setFilePath(String filePath) {
        // Cut off everything before the "\\Game" folder.
        // Only files which have collision hashes have something before that (eg: "\\Netapp1\PD\.....").
        // I've chosen to start counting game files at the "root folder of game data" instead of "root folder of network drive".
        if (filePath != null) {
            int startPos = TGQUtils.indexOfMultiple(filePath, TGQUtils.GAME_PATH_INDEX_PATTERNS);
            if (startPos > 0)
                filePath = filePath.substring(startPos);
        }

        // If we already have a path, we shouldn't be replacing it, and we should warn if the path differs.
        if (this.filePath != null && !this.filePath.isEmpty() && !this.filePath.equalsIgnoreCase(filePath)) {
            System.out.println("Attempted to replace file name '" + this.filePath + "' with '" + filePath + "'. Not sure how to handle.");
            return;
        }

        // Apply data.
        this.filePath = filePath;
        this.fileName = filePath;
        if (filePath != null && filePath.contains("\\")) // Remove path.
            this.fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
    }

    /**
     * Gets the archive into the main archive.
     * @return archiveIndex
     */
    public int getArchiveIndex() {
        return getMainArchive() != null ? getMainArchive().getFiles().indexOf(this) : -1;
    }

    /**
     * Tests if this file has a file path assigned.
     * @return hasFilePath
     */
    public boolean hasFilePath() {
        return this.filePath != null;
    }

    /**
     * Gets the export name.
     * @return exportName
     */
    public String getExportName() {
        if (hasFilePath() && Utils.isValidFileName(getFileName()))
            return getFileName();

        int index = getArchiveIndex();
        if (index == -1)
            index = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

        return index + "." + getExtension();
    }

    /**
     * Gets the file name shown in debug contexts. Trys to show as much detail as possible.
     */
    public String getDebugName() {
        if (this.filePath != null)
            return this.filePath;
        if (this.fileName != null)
            return this.fileName;
        return getExportName();
    }

    /**
     * Gets the extension for this file.
     */
    public String getExtension() {
        return "dat";
    }

    /**
     * Gets the folder the file gets exported to if the file path is unknown.
     */
    public abstract String getDefaultFolderName();

    /**
     * Gets the folder the file gets exported for any custom export.
     */
    public String getExportFolderName() {
        return "Usable";
    }

    /**
     * Exports the file to a folder.
     * @param baseFolder The base folder that game assets are saved to.
     */
    public void export(File baseFolder) {
        File targetFile = TGQUtils.getExportFile(baseFolder, this);
        File targetFolder = targetFile.getParentFile();
        if (this.rawData != null && (!targetFile.exists() || targetFile.length() != this.rawData.length)) {
            try {
                Files.write(targetFile.toPath(), this.rawData);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to export file '" + targetFile + "'.", ex);
            }
        }

        if (this instanceof IFileExport) {
            File exportFolder = new File(targetFolder, getExportFolderName() + "/");
            Utils.makeDirectory(exportFolder);

            try {
                ((IFileExport) this).exportToFolder(exportFolder);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to export file '" + getDebugName() + "' to usable format.", ex);
            }
        }
    }
}