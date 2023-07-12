package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.tgq.loading.kcLoadContext;
import net.highwayfrogs.editor.utils.Utils;

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
        if (this.filePath != null && !this.filePath.isEmpty() && !this.filePath.equalsIgnoreCase(filePath) && !(this.filePath.startsWith("\\\\") && !filePath.startsWith("\\\\"))) {
            System.out.println("Attempted to replace file name '" + this.filePath + "' with '" + filePath + "'. Not sure how to handle.");
            return;
        }

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
     * Tests if this file has a filename assigned.
     * @return hasFileName
     */
    public boolean hasName() {
        return this.filePath != null;
    }

    /**
     * Gets the export name.
     * @return exportName
     */
    public String getExportName() {
        if (hasName() && Utils.isValidFileName(getFileName())) {
            if (this instanceof TGQDummyFile)
                return getArchiveIndex() + "-" + getFileName();

            return getFileName();
        }

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
}
