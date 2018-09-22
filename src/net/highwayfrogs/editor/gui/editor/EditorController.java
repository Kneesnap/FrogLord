package net.highwayfrogs.editor.gui.editor;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameFile;

/**
 * Created by Kneesnap on 9/21/2018.
 */
@Getter
public class EditorController<T extends GameFile> {
    private T file;

    /**
     * Setup this window, by loading a GameFile to edit.
     * @param file The file to load and edit.
     */
    public void loadFile(T file) {
        this.file = file;
    }
}
