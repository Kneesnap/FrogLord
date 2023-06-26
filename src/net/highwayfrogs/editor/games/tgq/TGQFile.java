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
    private String cleanName;
    private String rawName;
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
     * Initialize the information about this file.
     * @param realName   This file's raw name. Can be null.
     * @param compressed Whether this file is compressed.
     */
    public void init(String realName, boolean compressed, int hash) {
        setRawName(realName);
        this.compressed = compressed;
        this.nameHash = hash;
    }

    /**
     * Sets the raw file name of this file.
     * @param rawName The raw file name. (Full path)
     */
    public void setRawName(String rawName) {
        this.rawName = rawName;
        this.cleanName = rawName;
        if (rawName != null && rawName.contains("\\")) // Remove path.
            this.cleanName = rawName.substring(rawName.lastIndexOf("\\") + 1);
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
        return this.rawName != null;
    }

    /**
     * Gets the export name.
     * @return exportName
     */
    public String getExportName() {
        if (hasName() && Utils.isValidFileName(getCleanName())) {
            if (this instanceof TGQDummyFile)
                return getArchiveIndex() + "-" + getCleanName();

            return getCleanName();
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
