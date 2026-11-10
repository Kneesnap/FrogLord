package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.sky;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Contains data about homing the frog.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataHawk extends FroggerEntityScriptData {
    private int speed = 3276;
    private int aggroDistance = 1024;

    public FroggerEntityScriptDataHawk(FroggerMapFile mapFile) {
        super(mapFile);
    }

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
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Speed (grid tiles/sec)", this.speed, newSpeed -> this.speed = newSpeed, 2184.5).setTooltip(FXUtils.createTooltip("How fast the hawk chases its target."));
        editor.addFixedInt("Aggro Range (grid)", this.aggroDistance, newAggroDistance -> this.aggroDistance = newAggroDistance, 256).setTooltip(FXUtils.createTooltip("The detection range for the hawk. This radius follows the hawk while it moves.\nThe hawk will instantly freeze in place once it stops detecting the player."));
    }
}