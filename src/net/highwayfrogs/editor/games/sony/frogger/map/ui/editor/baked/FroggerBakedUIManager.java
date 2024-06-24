package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.baked;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * A UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 5/28/2024.
 */
@Getter
public class FroggerBakedUIManager extends MeshUIManager<FroggerMapMesh> {
    public FroggerBakedUIManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the mesh represents.
     */
    public FroggerMapFile getMap() {
        return getMesh().getMap();
    }

    @Override
    public FroggerMapMeshController getController() {
        return (FroggerMapMeshController) super.getController();
    }

    /**
     * A UI list manager for Old Frogger map meshes.
     */
    public static abstract class FroggerBakedMapListManager<TValue, T3DDelegate> extends BasicListMeshUIManager<FroggerMapMesh, TValue, T3DDelegate> {
        public FroggerBakedMapListManager(MeshViewController<FroggerMapMesh> controller) {
            super(controller);
        }

        /**
         * Gets the map file the mesh represents.
         */
        public FroggerMapFile getMap() {
            return getMesh().getMap();
        }

        @Override
        public FroggerMapMeshController getController() {
            return (FroggerMapMeshController) super.getController();
        }
    }
}