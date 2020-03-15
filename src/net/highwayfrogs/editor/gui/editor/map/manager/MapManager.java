package net.highwayfrogs.editor.gui.editor.map.manager;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.MAPFile;
import net.highwayfrogs.editor.file.map.poly.polygon.MAPPolygon;
import net.highwayfrogs.editor.file.map.view.MapMesh;
import net.highwayfrogs.editor.gui.editor.MAPController;
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
     * Called when the manager is created.
     */
    public void onSetup() {

    }

    /**
     * Handles a map click.
     * @param event          The MouseEvent.
     * @param clickedPolygon The polygon clicked on.
     * @return consumeClick (Whether or not the click should be consumed and not handled by anything else)
     */
    public boolean handleClick(MouseEvent event, MAPPolygon clickedPolygon) {
        return false;
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
        return getController().getRenderManager();
    }

    /**
     * Gets the map file this belongs to.
     * @return mapFile
     */
    public MAPFile getMap() {
        return getController().getMap();
    }

    /**
     * Gets the MapMesh this manages.
     * @return mapMesh
     */
    public MapMesh getMesh() {
        return getController().getMapMesh();
    }

    /**
     * Gets the base ui controller. (The menu which controls the 2d interface, so the remap editor, island fixer, etc)
     * @return baseController
     */
    public MAPController getBaseController() {
        return getController().getController();
    }

    /**
     * Sets up the 2D editor for whatever this manages.
     * May do nothing.
     */
    public void setupEditor() {

    }

    /**
     * Handle when a key is pressed.
     * @param event The key event fired.
     * @return consumeEvent
     */
    public boolean onKeyPress(KeyEvent event) {
        if (isPromptActive() && event.getCode() == KeyCode.ESCAPE) { // Cancel active prompt.
            cancelPrompt();
            return true;
        }

        return false;
    }
}
