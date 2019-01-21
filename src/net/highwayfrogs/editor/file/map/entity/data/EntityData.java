package net.highwayfrogs.editor.file.map.entity.data;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

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
}
