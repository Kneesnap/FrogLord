package net.highwayfrogs.editor.gui;

import javafx.scene.layout.GridPane;

/**
 * Has behavior upon a change to the grid.
 * Created by Kneesnap on 8/20/2019.
 */
public class AbstractGUIEditorGrid extends GUIEditorGrid {
    private Runnable onChange;

    public AbstractGUIEditorGrid(GridPane pane, Runnable onChange) {
        super(pane);
        this.onChange = onChange;
    }

    @Override
    public void onChange() {
        super.onChange();
        this.onChange.run();
    }
}
