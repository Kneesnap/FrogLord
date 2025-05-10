package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Holds script data for nuclear barrels.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataNuclearBarrel extends FroggerEntityScriptData {
    private int jumpDistance = 3;
    private int jumpTime = 15; // Passed directly to JumpFrog()

    public FroggerEntityScriptDataNuclearBarrel(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.jumpDistance = reader.readInt();
        this.jumpTime = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.jumpDistance);
        writer.writeInt(this.jumpTime);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addSignedIntegerField("Jump Distance (grid)", this.jumpDistance, newDistance -> this.jumpDistance = newDistance)
                .setTooltip(FXUtils.createTooltip("Controls how many grid squares the player will be moved."));
        editor.addFixedInt("Jump Time (sec)", this.jumpTime, newTime -> this.jumpTime = newTime, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("Controls how long (in seconds) the jump will take."));
    }
}