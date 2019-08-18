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
    private TGQBinFile mainArchive;
    private String cleanName;
    private String rawName;
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
     * @param compressed Whether or not this file is compressed.
     */
    public void init(String realName, boolean compressed) {
        if (realName != null) {
            this.rawName = realName;
            this.cleanName = realName;
            if (realName.contains("\\")) // Remove path.
                this.cleanName = realName.substring(realName.lastIndexOf("\\") + 1);
        }

        this.compressed = compressed;
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
        if (hasName() && Utils.isValidFileName(getCleanName()))
            return getCleanName();

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
