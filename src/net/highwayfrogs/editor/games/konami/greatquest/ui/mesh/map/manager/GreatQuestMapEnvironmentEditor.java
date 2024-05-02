package net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.manager;

import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import net.highwayfrogs.editor.games.konami.greatquest.map.kcEnvironment;
import net.highwayfrogs.editor.games.konami.greatquest.ui.mesh.map.GreatQuestMapMesh;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MeshViewController;
import net.highwayfrogs.editor.gui.editor.UISidePanel;

/**
 * Manages the map environment
 * TODO: Support displaying the lights in the scene when we upgrade to new version.
 * Created by Kneesnap on 4/19/2024.
 */
public class GreatQuestMapEnvironmentEditor extends GreatQuestMapUIManager {
    private GUIEditorGrid editorGrid;
    public GreatQuestMapEnvironmentEditor(MeshViewController<GreatQuestMapMesh> controller) {
        super(controller);
    }

    @Override
    public void onSetup() {
        super.onSetup();

        // UI Pane
        UISidePanel sidePanel = getController().createSidePanel("Environment Settings");

        // Separator, and grid setup.
        sidePanel.add(new Separator(Orientation.HORIZONTAL));
        this.editorGrid = sidePanel.makeEditorGrid();

        kcEnvironment environment = getMap().getEnvironment();
        if (environment != null && environment.isFogEnabled())
            getController().getColorPickerLevelBackground().setValue(environment.getFog().getColor().toFxColor());
    }

    @Override
    public void updateEditor() {
        super.updateEditor();

        // Setup basics.
        this.editorGrid.clearEditor();
        getMap().getEnvironment().setupEditor(this.editorGrid, getController());
        this.editorGrid.addSeparator();
    }
}