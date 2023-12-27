package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.scene.layout.VBox;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMeshController;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.packet.OldFroggerMapGridHeaderPacket.OldFroggerMapGrid;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.ui.OldFroggerMapUIManager.OldFroggerMapListManager;

import java.util.List;

/**
 * Manages the UI for editing grid map data in old Frogger.
 * TODO: Go over global grid header data
 * TODO: should this manage the polygon highlighting from the grid editors?
 * Created by Kneesnap on 12/24/2023.
 */
public class OldFroggerGridManager extends OldFroggerMapListManager<OldFroggerMapGrid, Void> {
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
    protected Void setupDisplay(OldFroggerMapGrid grid) {
        // TODO: IMPLEMENT.
        return null;
    }

    @Override
    protected void updateEditor(OldFroggerMapGrid grid) {
        grid.setupEditor(this, getEditorGrid());
    }

    @Override
    protected void setVisible(OldFroggerMapGrid oldFroggerMapGrid, Void unused, boolean visible) {
        // TODO: Implement.
    }

    @Override
    protected void onSelectedValueChange(OldFroggerMapGrid oldValue, Void oldDelegate, OldFroggerMapGrid newValue, Void newDelegate) {
        // TODO: Implement.
    }

    @Override
    protected OldFroggerMapGrid createNewValue() {
        return new OldFroggerMapGrid(getMap().getGameInstance());
    }

    @Override
    protected void onDelegateRemoved(OldFroggerMapGrid oldFroggerMapGrid, Void unused) {
        // TODO: Implement.
    }
}
