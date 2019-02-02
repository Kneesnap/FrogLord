package net.highwayfrogs.editor.file.map.entity.data.desert;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * A cut entity. This class is not used because in the retail game, this data structure is not enabled.
 * Represents "DESERT_EARTHQUAKE".
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class EntityEarthquake extends EntityData {
    private short timeFlag;
    private int[] pauseList = new int[MAX_ENTITY_UNPAUSED_BY_QUAKE];

    private static final int MAX_ENTITY_UNPAUSED_BY_QUAKE = 10;

    @Override
    public void load(DataReader reader) {
        this.timeFlag = reader.readShort();
        reader.skipShort();
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
    public void addData(GUIEditorGrid editor) {
        editor.addShortField("Time Flag", getTimeFlag(), this::setTimeFlag, null);
        for (int i = 0; i < getPauseList().length; i++) {
            final int tempIndex = i;
            editor.addIntegerField("Pause #" + (i + 1), getPauseList()[i], newVal -> getPauseList()[tempIndex] = newVal, null);
        }
    }
}
