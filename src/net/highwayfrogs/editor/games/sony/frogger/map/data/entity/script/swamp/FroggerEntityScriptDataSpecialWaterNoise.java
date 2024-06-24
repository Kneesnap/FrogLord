package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * A special (PSX Demo only) script data set.
 * Pulled by looking at script_swp_water_noise.
 * Created by Kneesnap on 7/30/2019.
 */
@Getter
public class FroggerEntityScriptDataSpecialWaterNoise extends FroggerEntityScriptData {
    private int distance = 768;

    public FroggerEntityScriptDataSpecialWaterNoise(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.distance = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.distance);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Distance (grid)", this.distance, newDistance -> this.distance = newDistance, 256);
    }
}