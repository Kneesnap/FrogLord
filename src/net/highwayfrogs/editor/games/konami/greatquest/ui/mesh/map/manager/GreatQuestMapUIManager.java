package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import net.highwayfrogs.editor.games.konami.greatquest.file.GreatQuestChunkedFile;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMeshController;
import net.highwayfrogs.editor.gui.editor.BasicListMeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshUIManager;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * A UI manager is our way of isolating different editor features shown in a 3D environment.
 * This helps isolate UI code, 3D management code, etc.
 * Created by Kneesnap on 4/14/2024.
 */
public class GreatQuestMapUIManager extends MeshUIManager<GreatQuestMapMesh> {
    public GreatQuestMapUIManager(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
    }

    /**
     * Gets the map file the mesh represents.
     */
    public GreatQuestChunkedFile getMap() {
        return getMesh().getMap();
    }

    @Override
    public GreatQuestMapMeshController getController() {
        return (GreatQuestMapMeshController) super.getController();
    }

    /**
     * A UI list manager for Great Quest map meshes.
     */
    public static abstract class GreatQuestMapListManager<TValue, T3DDelegate> extends BasicListMeshUIManager<GreatQuestMapMesh, TValue, T3DDelegate> {
        public GreatQuestMapListManager(MeshViewController<GreatQuestMapMesh> controller) {
            super(controller);
        }

        /**
         * Gets the map file the mesh represents.
         */
        public GreatQuestChunkedFile getMap() {
            return getMesh().getMap();
        }

        @Override
        public GreatQuestMapMeshController getController() {
            return (GreatQuestMapMeshController) super.getController();
        }
    }
}