package net.highwayfrogs.editor.games.sony.beastwars.ui;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.beastwars.map.BeastWarsMapFile;
import net.highwayfrogs.editor.games.sony.beastwars.map.mesh.BeastWarsMapMesh;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * A Mesh UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 9/26/2023.
 */
@Getter
public class BeastWarsMapUIManager extends MeshUIManager<BeastWarsMapMesh> {
    public BeastWarsMapUIManager(MeshViewController<BeastWarsMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the mesh represents.
     */
    public BeastWarsMapFile getMap() {
        return getMesh().getMap();
    }

    /**
     * A UI list manager for Beast Wars map meshes.
     */
    public static abstract class BeastWarsMapListManager<TValue, T3DDelegate> extends BasicListMeshUIManager<BeastWarsMapMesh, TValue, T3DDelegate> {
        public BeastWarsMapListManager(MeshViewController<BeastWarsMapMesh> controller) {
            super(controller);
        }

        /**
         * Gets the map file the mesh represents.
         */
        public BeastWarsMapFile getMap() {
            return getMesh().getMap();
        }
    }
}