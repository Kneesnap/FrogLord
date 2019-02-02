package net.highwayfrogs.editor.file.map.entity.data;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.gui.editor.MapUIController;

/**
 * Represents game-data.
 * Created by Kneesnap on 1/20/2019.
 */
public abstract class EntityData extends GameObject {

    /**
     * Add entity data to a table.
     * @param editor The editor to build on.
     */
    public abstract void addData(GUIEditorGrid editor);

    /**
     * Add entity data to the editor.
     * @param controller The controller to apply to.
     * @param editor     The editor to apply to.
     */
    public void addData(MapUIController controller, GUIEditorGrid editor) {
        this.addData(editor);
    }
}
