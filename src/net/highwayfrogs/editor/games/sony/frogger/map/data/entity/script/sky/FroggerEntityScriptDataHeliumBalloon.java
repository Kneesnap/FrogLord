package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * It is unknown what these values are for.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataHeliumBalloon extends FroggerEntityScriptData {
    private int unknown1Speed = 2184;
    private int unknown2Distance = 1280;

    public FroggerEntityScriptDataHeliumBalloon(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.unknown1Speed = reader.readInt();
        this.unknown2Distance = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.unknown1Speed);
        writer.writeInt(this.unknown2Distance);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Unknown 1 (Speed?)", this.unknown1Speed, newUnknown1 -> this.unknown1Speed = newUnknown1, 2184);
        editor.addFixedInt("Unknown 2 (Distance?)", this.unknown2Distance, newUnknown1 -> this.unknown2Distance = newUnknown1, 256);
    }
}