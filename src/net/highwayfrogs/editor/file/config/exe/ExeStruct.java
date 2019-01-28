package net.highwayfrogs.editor.file.config.exe;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.config.FroggerEXEInfo;
import net.highwayfrogs.editor.gui.GUIMain;

/**
 * A struct found in the frogger exe.
 * Created by Kneesnap on 1/27/2019.
 */
public abstract class ExeStruct extends GameObject {
    /**
     * Get the exe config.
     * @return exeConfig
     */
    public FroggerEXEInfo getConfig() {
        return GUIMain.EXE_CONFIG;
    }
}
