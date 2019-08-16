package net.highwayfrogs.editor.gui.editor.map.manager;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.gui.editor.MapUIController;
import net.highwayfrogs.editor.gui.editor.RenderManager;

/**
 * A base Map UI manager, which can control 3D and 2D editor parts.
 * Created by Kneesnap on 8/16/2019.
 */
@Getter
public class MapManager {
    private MapUIController controller;

    private boolean promptActive;
    private Runnable promptCancel;

    public MapManager(MapUIController controller) {
        this.controller = controller;
    }

    /**
     * Activate a user-input prompt.
     * @param onCancel The cancel behavior.
     */
    protected void activatePrompt(Runnable onCancel) {
        this.promptActive = true;
        this.promptCancel = onCancel;
    }

    /**
     * Should be overridden to clean prompt data from the sub-class.
     */
    protected void cleanChildPrompt() {

    }

    /**
     * Cleans up the prompt.
     */
    public void onPromptFinish() {
        if (!isPromptActive())
            return;

        this.promptActive = false;
        this.promptCancel = null;
        cleanChildPrompt();
    }

    /**
     * Cancel the user prompt.
     */
    public void cancelPrompt() {
        if (!isPromptActive())
            return;

        this.promptActive = false;
        if (this.promptCancel != null) {
            this.promptCancel.run();
            this.promptCancel = null;
        }
        cleanChildPrompt();
    }

    /**
     * Gets the RenderManager for this manager.
     */
    public RenderManager getRenderManager() {
        return getController().getController().getRenderManager();
    }

    /**
     * Gets the map file this belongs to.
     * @return mapFile
     */
    public MAPFile getMap() {
        return getController().getMap();
    }

    /**
     * Sets up the 2D editor for whatever this manages.
     * May do nothing.
     */
    public void setupEditor() {

    }
}
