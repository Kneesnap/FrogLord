package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.layout.VBox;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapPolygon;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode;
import net.highwayfrogs.editor.gui.mesh.DynamicMeshOverlayNode.OverlayTarget;

import java.util.List;

/**
 * Manages the UI for editing grid map data in old Frogger.
 * Created by Kneesnap on 12/24/2023.
 */
public class OldFroggerGridManager extends OldFroggerMapListManager<OldFroggerMapGrid, DynamicMeshOverlayNode> {
    public OldFroggerGridManager(OldFroggerMapMeshController controller) {
        super(controller);
    }

    @Override
    public String getTitle() {
        return "Grids";
    }

    @Override
    public String getValueName() {
        return "Grid";
    }

    @Override
    public List<OldFroggerMapGrid> getValues() {
        return getMap().getGridPacket().getGrids();
    }

    @Override
    protected void setupMainGridEditor(VBox editorBox) {
        getMap().getGridPacket().setupEditor(this, getMainGrid());
        getMainGrid().addSeparator();

        super.setupMainGridEditor(editorBox);
    }

    @Override
    protected DynamicMeshOverlayNode setupDisplay(OldFroggerMapGrid grid) {
        DynamicMeshOverlayNode overlay = new DynamicMeshOverlayNode(getMesh());
        getMesh().getNodes().add(overlay);
        return overlay;
    }

    @Override
    protected void updateEditor(OldFroggerMapGrid grid) {
        grid.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setVisible(OldFroggerMapGrid grid, DynamicMeshOverlayNode overlay, boolean visible) {
        boolean wasVisible = overlay.getDataEntries().size() > 0;
        if (wasVisible == visible)
            return; // Already correct state.

        if (visible) {
            for (int i = 0; i < grid.getPolygons().size(); i++) {
                OldFroggerMapPolygon polygon = grid.getPolygons().get(i);
                overlay.add(new OverlayTarget(getMesh().getMainNode().getDataEntry(polygon), OldFroggerMapMesh.YELLOW_COLOR));
            }

            // Without this, the grids will not show.
            getMesh().updateMeshArrays();
        } else {
            overlay.clear();
        }
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapGrid oldValue, DynamicMeshOverlayNode oldOverlay, OldFroggerMapGrid newValue, DynamicMeshOverlayNode newOverlay) {
        // Do nothing.
    }

    @Override
    protected OldFroggerMapGrid createNewValue() {
        return new OldFroggerMapGrid(getMap());
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapGrid oldFroggerMapGrid, DynamicMeshOverlayNode overlay) {
        if (overlay != null)
            getMesh().removeNode(overlay);
    }
}
