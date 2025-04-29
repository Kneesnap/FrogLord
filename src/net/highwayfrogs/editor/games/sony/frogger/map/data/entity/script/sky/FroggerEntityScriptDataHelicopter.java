package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Holds helicopter script data.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataHelicopter extends FroggerEntityScriptData {
    private int destination = 768;
    private int delta = 5461;

    public FroggerEntityScriptDataHelicopter(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.destination = reader.readInt();
        this.delta = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.destination);
        writer.writeInt(this.delta);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Fly Height (grid)", this.destination, newDestination -> this.destination = newDestination, 256);
        editor.addFixedInt("Delta (???)", this.delta, newDelta -> this.delta = newDelta, 2184);
    }
}