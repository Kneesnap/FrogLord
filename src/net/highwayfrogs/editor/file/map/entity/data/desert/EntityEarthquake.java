package net.highwayfrogs.editor.file.map.entity.data.desert;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * A cut entity. This class is not used because in the retail game, this data structure is not enabled.
 * Represents "DESERT_EARTHQUAKE".
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class EntityEarthquake extends EntityData {
    private short timeFlag;
    private int[] pauseList = new int[MAX_ENTITY_UNPAUSED_BY_QUAKE];

    private static final int MAX_ENTITY_UNPAUSED_BY_QUAKE = 10;

    @Override
    public void load(DataReader reader) {
        this.timeFlag = reader.readShort();
        reader.readShort();
        for (int i = 0; i < pauseList.length; i++)
            pauseList[i] = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.timeFlag);
        writer.writeUnsignedShort(0);
        for (int pause : pauseList)
            writer.writeUnsignedShort(pause);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Time Flag", String.valueOf(getTimeFlag())));
        for (int i = 0; i < pauseList.length; i++)
            table.getItems().add(new NameValuePair("Pause #" + i, String.valueOf(pauseList[i])));
    }
}
