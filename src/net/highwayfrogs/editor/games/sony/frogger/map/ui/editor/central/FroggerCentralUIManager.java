package net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMesh;
import net.highwayfrogs.editor.games.sony.frogger.map.mesh.FroggerMapMeshController;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * A UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * "Central" is the name of the editor for unbaked levels.
 * Created by Kneesnap on 6/1/2024.
 */
@Getter
public class FroggerCentralUIManager extends MeshUIManager<FroggerMapMesh> {
    public FroggerCentralUIManager(MeshViewController<FroggerMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the mesh represents.
     */
    public FroggerMapFile getMap() {
        return getMesh().getMap();
    }

    @Override
    public FroggerGameInstance getGameInstance() {
        return (FroggerGameInstance) super.getGameInstance();
    }

    @Override
    public FroggerMapMeshController getController() {
        return (FroggerMapMeshController) super.getController();
    }

    /**
     * A UI list manager for Old Frogger map meshes.
     */
    public static abstract class FroggerCentralMapListManager<TValue, T3DDelegate> extends BasicListMeshUIManager<FroggerMapMesh, TValue, T3DDelegate> {
        public FroggerCentralMapListManager(MeshViewController<FroggerMapMesh> controller) {
            super(controller);
        }

        /**
         * Gets the map file the mesh represents.
         */
        public FroggerMapFile getMap() {
            return getMesh().getMap();
        }

        @Override
        public FroggerGameInstance getGameInstance() {
            return (FroggerGameInstance) super.getGameInstance();
        }

        @Override
        public FroggerMapMeshController getController() {
            return (FroggerMapMeshController) super.getController();
        }
    }
}