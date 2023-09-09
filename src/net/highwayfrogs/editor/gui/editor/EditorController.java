package net.highwayfrogs.editor.gui.editor;

import javafx.scene.layout.AnchorPane;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameConfig;
import net.highwayfrogs.editor.games.sony.SCGameFile;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.SCGameObject;

/**
 * Represents a base froglord editor controller.
 * Created by Kneesnap on 9/21/2018.
 */
@Getter
public class EditorController<T extends SCGameFile<TInstance>, TInstance extends SCGameInstance, TConfig extends SCGameConfig> extends SCGameObject<TInstance> {
    private T file;


    public EditorController(TInstance instance) {
        super(instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TConfig getConfig() {
        return (TConfig) super.getConfig();
    }

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
}