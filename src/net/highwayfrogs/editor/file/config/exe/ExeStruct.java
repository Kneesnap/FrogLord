package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;

/**
 * A struct found in the frogger exe.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class ExeStruct extends GameObject {
    /**
     * Handle a manual correction.
     * @param args The arguments supplied.
     */
    public void handleCorrection(String[] args) {
        throw new UnsupportedOperationException("This struct does not currently support manual correction! (" + getClass().getSimpleName() + ")");
    }

    /**
     * Handle a manual correction.
     * @param str The string containing the arguments.
     */
    public void handleCorrection(String str) {
        handleCorrection(str.split(","));
    }

    /**
     * Test if a file entry is held by this struct.
     * @param test The entry to test.
     * @return isEntry
     */
    public abstract boolean isEntry(FileEntry test);
}
