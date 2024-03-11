package net.highwayfrogs.editor.games.sony.medievil.map.ui;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.medievil.map.MediEvilMapFile;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMesh;
import net.highwayfrogs.editor.games.sony.medievil.map.mesh.MediEvilMapMeshController;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * A UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 12/12/2023. Repurposed for MediEvil by RampantSpirit on 3/11/2024.
 */
@Getter
public class MediEvilMapUIManager extends MeshUIManager<MediEvilMapMesh> {
    public MediEvilMapUIManager(MeshViewController<MediEvilMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the mesh represents.
     */
    public MediEvilMapFile getMap() {
        return getMesh().getMap();
    }

    @Override
    public MediEvilMapMeshController getController() {
        return (MediEvilMapMeshController) super.getController();
    }

    /**
     * A UI list manager for Old Frogger map meshes.
     */
    public static abstract class MediEvilMapListManager<TValue, T3DDelegate> extends BasicListMeshUIManager<MediEvilMapMesh, TValue, T3DDelegate> {
        public MediEvilMapListManager(MeshViewController<MediEvilMapMesh> controller) {
            super(controller);
        }

        /**
         * Gets the map file the mesh represents.
         */
        public MediEvilMapFile getMap() {
            return getMesh().getMap();
        }

        @Override
        public MediEvilMapMeshController getController() {
            return (MediEvilMapMeshController) super.getController();
        }
    }
}