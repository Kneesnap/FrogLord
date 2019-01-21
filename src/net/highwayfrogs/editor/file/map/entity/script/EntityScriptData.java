package net.highwayfrogs.editor.file.map.entity.script;

import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Listens to entity script data.
 * Created by Kneesnap on 11/27/2018.
 */
public abstract class EntityScriptData extends GameObject {

    /**
     * Add script data.
     * @param editor The editor to build on top of.
     */
    public abstract void addData(GUIEditorGrid editor);
}
