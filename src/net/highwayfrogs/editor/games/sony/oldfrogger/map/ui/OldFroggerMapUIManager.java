package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.OldFroggerMapFile;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * A UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 12/12/2023.
 */
@Getter
public class OldFroggerMapUIManager extends MeshUIManager<OldFroggerMapMesh> {
    public OldFroggerMapUIManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the mesh represents.
     */
    public OldFroggerMapFile getMap() {
        return getMesh().getMap();
    }

    @Override
    public OldFroggerMapMeshController getController() {
        return (OldFroggerMapMeshController) super.getController();
    }

    /**
     * A UI list manager for Old Frogger map meshes.
     */
    public static abstract class OldFroggerMapListManager<TValue, T3DDelegate> extends BasicListMeshUIManager<OldFroggerMapMesh, TValue, T3DDelegate> {
        public OldFroggerMapListManager(MeshViewController<OldFroggerMapMesh> controller) {
            super(controller);
        }

        /**
         * Gets the map file the mesh represents.
         */
        public OldFroggerMapFile getMap() {
            return getMesh().getMap();
        }

        @Override
        public OldFroggerMapMeshController getController() {
            return (OldFroggerMapMeshController) super.getController();
        }
    }
}