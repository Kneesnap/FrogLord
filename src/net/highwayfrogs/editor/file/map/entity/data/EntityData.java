package net.highwayfrogs.editor.file.map.entity.data;

import javafx.scene.control.TableView;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents game-data.
 * Created by Kneesnap on 1/20/2019.
 */
public abstract class EntityData extends GameObject {

    /**
     * Add entity data to a table.
     * @param table The table to add data to.
     */
    public abstract void addData(TableView<NameValuePair> table);
}
