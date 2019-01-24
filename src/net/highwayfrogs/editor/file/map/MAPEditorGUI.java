package net.highwayfrogs.editor.file.map;

import javafx.scene.layout.GridPane;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

/**
 * A custom GUI creator for maps, to handle changes.
 * Created by Kneesnap on 1/21/2019.
 */
public class MAPEditorGUI extends GUIEditorGrid {
    private MapUIController uiController;

    public MAPEditorGUI(GridPane pane, MapUIController uiController) {
        super(pane);
        this.uiController = uiController;
    }

    @Override
    protected void onChange() {
        super.onChange();
        uiController.getController().resetEntities();
    }
}
