package net.highwayfrogs.editor.gui.editor;

import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import net.highwayfrogs.editor.gui.editor.DisplayList.RenderListManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMesh;

/**
 * A Mesh UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public class MeshUIManager<TMesh extends DynamicMesh> {
    private final MeshViewController<TMesh> controller;

    public MeshUIManager(MeshViewController<TMesh> controller) {
        this.controller = controller;
    }

    /**
     * Called when the manager is created.
     */
    public void onSetup() {
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
     * Gets the MapMesh this manages.
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