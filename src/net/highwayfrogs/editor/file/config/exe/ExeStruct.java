package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.GameObject;

/**
 * A struct found in the frogger exe.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class ExeStruct extends GameObject {
    /**
     * Handle a manual correction.
     * @param args The arguments supplied.
     */
    public abstract void handleCorrection(String[] args);

    /**
     * Handle a manual correction.
     * @param str The string containing the arguments.
     */
    public void handleCorrection(String str) {
        handleCorrection(str.split(","));
    }
}
