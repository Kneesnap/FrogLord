package net.highwayfrogs.editor.file.map.entity.script.volcano;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Contains data about homing the frog.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class ScriptHawkData extends EntityScriptData {
    private int speed;
    private int aggroDistance;

    @Override
    public void load(DataReader reader) {
        this.speed = reader.readInt();
        this.aggroDistance = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.speed);
        writer.writeInt(this.aggroDistance);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        editor.addIntegerField("Speed", getSpeed(), this::setSpeed, null);
        editor.addIntegerField("Aggro Range", getAggroDistance(), this::setAggroDistance, null);
    }
}
