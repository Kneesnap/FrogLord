package net.highwayfrogs.editor.games.sony.oldfrogger.map.ui;

import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import net.highwayfrogs.editor.games.sony.oldfrogger.map.mesh.OldFroggerMapMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshViewController;

/**
 * Allows editing general map level data.
 * Created by Kneesnap on 12/22/2023.
 */
public class OldFroggerGeneralDataManager extends OldFroggerMapUIManager {
    private GUIEditorGrid editorGrid;

    public OldFroggerGeneralDataManager(MeshViewController<OldFroggerMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();

        // UI Pane
        VBox editorBox = this.getController().makeAccordionMenu("Level Settings");

        // Separator, and grid setup.
        editorBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        this.editorGrid = this.getController().makeEditorGrid(editorBox);
    }

    @Override
    public void updateEditor() {
        super.updateEditor();

        // Setup basics.
        this.editorGrid.clearEditor();
        getMap().getLevelSpecificDataPacket().setupEditor(getController(), this.editorGrid);
        this.editorGrid.addSeparator();

        if (getMap().getStandardPacket() != null) {
            this.editorGrid.addBoldLabel("\"Standard\" Map Data:");
            getMap().getStandardPacket().setupEditor(this, this.editorGrid);
        }
    }
}
