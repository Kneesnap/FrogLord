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
 * Holds data for the SCRIPT_SWP_BOBBING_WASTE_BARREL script.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataBobbingWaste extends FroggerEntityScriptData {
    private int delay;

    public FroggerEntityScriptDataBobbingWaste(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.delay = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.delay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Delay (secs)", this.delay, newDelay -> this.delay = newDelay, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("Controls how long to wait after the level loads to start bobbing."));
    }
}