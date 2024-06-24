package net.highwayfrogs.editor.gui.editor;

import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import net.highwayfrogs.editor.games.generic.GameInstance;
import net.highwayfrogs.editor.gui.editor.DisplayList.RenderListManager;
import net.highwayfrogs.editor.gui.editor.MeshViewFrameTimer.MeshViewFixedFrameRateTimer;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;
import net.highwayfrogs.editor.utils.Utils;

import java.util.logging.Logger;

/**
 * A Mesh UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 9/26/2023.
 */
public class MeshUIManager<TMesh extends DynamicMesh> {
    @Getter private final MeshViewController<TMesh> controller;
    private Logger cachedLogger;

    public MeshUIManager(MeshViewController<TMesh> controller) {
        this.controller = controller;
    }

    /**
     * Gets the game instance which this ui manager exists for.
     */
    public GameInstance getGameInstance() {
        return this.controller != null ? this.controller.getGameInstance() : null;
    }

    /**
     * Get the logger for this manager.
     */
    public Logger getLogger() {
        if (this.cachedLogger != null)
            return this.cachedLogger;

        return this.cachedLogger = Logger.getLogger(Utils.getSimpleName(this));
    }

    /**
     * Gets the frame timer for the given frame-rate.
     * @param framesPerSecond the frame-rate to get the timer for
     * @return frameTimer
     */
    public MeshViewFixedFrameRateTimer getFrameTimer(int framesPerSecond) {
        return this.controller != null ? this.controller.getFrameTimer().getOrCreateTimer(framesPerSecond) : null;
    }

    /**
     * Called when the manager is created.
     */
    public void onSetup() {
        // Do nothing, this is for overriding.
    }

    /**
     * Called when the manager is shutdown.
     */
    public void onRemove() {
        // Do nothing, this is for overriding.
    }

    /**
     * Called to add nodes which render last.
     */
    public void setupNodesWhichRenderLast() {
        // Do nothing, this is for overriding.
    }

    /**
     * Sets up and/or refreshes the 2D editor UI.
     */
    public void updateEditor() {

    }

    /**
     * Gets the RenderManager for this manager.
     */
    public RenderListManager getRenderManager() {
        return getController().getRenderManager();
    }

    /**
     * Gets the transparent RenderManager for this manager.
     */
    public RenderListManager getTransparentRenderManager() {
        return getController().getTransparentRenderManager();
    }

    /**
     * Gets the mesh this manages.
     * @return mapMesh
     */
    public TMesh getMesh() {
        return getController().getMesh();
    }

    /**
     * Handle when a key is pressed.
     * @param event The key event fired.
     * @return consumeEvent
     */
    public boolean onKeyPress(KeyEvent event) {
        return false;
    }

    /**
     * Expand the title pane containing the given node.
     * @param node The node to work backwards from.
     */
    protected static void expandTitlePaneFrom(Node node) {
        while (node != null) {
            if (node instanceof TitledPane) {
                ((TitledPane) node).setExpanded(true);
                return;
            }

            node = node.getParent();
        }
    }

    // TODO: Selection can be multiple things. Select a grid square! Select a polygon! Select a beast wars vertex! Select a MOF Part.
    // These things are different, can be selected through different means, and also are not mutually exclusive. Eg: We may want a generalized selection system...

    // TODO: Select polygon.
    // TODO: Select MOF Part
    // TODO: Select Beast Wars Vertex
    // TODO: Select Grid Square
    // TODO: Select path (Managed by Path Manager)
}