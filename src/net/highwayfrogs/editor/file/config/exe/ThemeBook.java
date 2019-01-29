package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.vlo.VLOArchive;

/**
 * Represents a platform-independent ThemeBook.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class ThemeBook extends ExeStruct {

    /**
     * Get the VLO of this book.
     * @param file The map file to get the vlo from.
     * @return vloArchive
     */
    public abstract VLOArchive getVLO(MAPFile file);

    /**
     * Tests if this is a valid theme.
     * @return isValid
     */
    public abstract boolean isValid();
}
