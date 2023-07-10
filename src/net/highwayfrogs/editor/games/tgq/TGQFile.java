package net.highwayfrogs.editor.games.tgq;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.writer.DataWriter;
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
    public void afterLoad1() {
        // Do nothing.
    }

    /**
     * The second method called after all files have been loaded.
     */
    public void afterLoad2() {
        // Do nothing.
    }

    /**
     * Initialize the information about this file.
     * @param realName   This file's raw name. Can be null.
     * @param compressed Whether this file is compressed.
     */
    public void init(String realName, boolean compressed, int hash, byte[] rawBytes) {
        setFilePath(realName);
        this.compressed = compressed;
        this.nameHash = hash;
        this.rawData = rawBytes;
    }

    /**
     * Sets the raw file name of this file.
     * @param filePath The raw file name. (Full path)
     */
    public void setFilePath(String filePath) {
        if (this.filePath != null && !this.filePath.isEmpty() && !this.filePath.equalsIgnoreCase(filePath)) {
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
     * Gets the extension for this file.
     */
    public String getExtension() {
        return "dat";
    }
}
