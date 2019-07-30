package net.highwayfrogs.editor.file.map.entity.script.swamp;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * A special (PSX Demo only) script data set.
 * Pulled by looking at script_swp_water_noise.
 * Created by Kneesnap on 7/30/2019.
 */
@Getter
@Setter
public class ScriptSpecialWaterNoiseData extends EntityScriptData {
    private int distance;

    @Override
    public void load(DataReader reader) {
        this.distance = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.distance);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        editor.addIntegerField("Distance", getDistance(), this::setDistance, null);
    }
}
