package net.highwayfrogs.editor.games.sony.shared.map.ui;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.SCGameInstance;
import net.highwayfrogs.editor.games.sony.shared.map.SCMapFile;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMesh;
import net.highwayfrogs.editor.games.sony.shared.map.mesh.SCMapMeshController;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;

/**
 * A UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 5/14/2024.
 */
@Getter
public class SCMapUIManager<TMapMesh extends SCMapMesh> extends MeshUIManager<TMapMesh> {
    public SCMapUIManager(SCMapMeshController<TMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the mesh represents.
     */
    public SCMapFile<? extends SCGameInstance> getMap() {
        return getMesh().getMap();
    }

    @Override
    public SCMapMeshController<TMapMesh> getController() {
        return (SCMapMeshController<TMapMesh>) super.getController();
    }

    /**
     * A UI list manager for SCMapFile map meshes.
     */
    public static abstract class SCMapListManager<TMapMesh extends SCMapMesh, TValue, T3DDelegate> extends BasicListMeshUIManager<TMapMesh, TValue, T3DDelegate> {
        public SCMapListManager(SCMapMeshController<TMapMesh> controller) {
            super(controller);
        }

        /**
         * Gets the map file the mesh represents.
         */
        public SCMapFile<? extends SCGameInstance> getMap() {
            return getMesh().getMap();
        }

        @Override
        public SCMapMeshController<TMapMesh> getController() {
            return (SCMapMeshController<TMapMesh>) super.getController();
        }
    }
}