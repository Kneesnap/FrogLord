package net.highwayfrogs.editor.gui.editor;

import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.file.GameFile;
import net.highwayfrogs.editor.file.MWIFile.FileEntry;
import net.highwayfrogs.editor.gui.MainController;

/**
 * Represents a base froglord editor controller.
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

    /**
     * Called when this editor is loaded.
     * @param editorRoot The editor root pane.
     */
    public void onInit(AnchorPane editorRoot) {

    }

    /**
     * Called when this editor is unloaded.
     * @param editorRoot The editor root pane.
     */
    public void onClose(AnchorPane editorRoot) {

    }

    /**
     * Get the MWI entry for the this file.
     * @return mwiEntry
     */
    public FileEntry getMWIEntry() {
        return MainController.MAIN_WINDOW.getMwdFile().getEntryMap().get(getFile());
    }
}
