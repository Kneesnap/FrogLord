package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * It is unknown what these values are for.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataBalloon extends FroggerEntityScriptData {
    private int unknown1Speed = 2184;
    private int unknown2Speed = 2184;
    private int unknown3Distance = 256;

    public FroggerEntityScriptDataBalloon(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.unknown1Speed = reader.readInt();
        this.unknown2Speed = reader.readInt();
        this.unknown3Distance = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.unknown1Speed);
        writer.writeInt(this.unknown2Speed);
        writer.writeInt(this.unknown3Distance);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Unknown 1 (Speed?)", this.unknown1Speed, newUnknown1 -> this.unknown1Speed = newUnknown1, 2184.5);
        editor.addFixedInt("Unknown 2 (Speed?)", this.unknown2Speed, newUnknown2 -> this.unknown2Speed = newUnknown2, 2184.5);
        editor.addFixedInt("Unknown 3 (Distance?)", this.unknown3Distance, newUnknown3 -> this.unknown3Distance = newUnknown3, 256);
    }
}