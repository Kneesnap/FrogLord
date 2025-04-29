package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents floating tree data.
 * Created by Kneesnap on 2/7/2023.
 */
@Getter
public class FroggerEntityScriptDataFloatingTree extends FroggerEntityScriptData {
    private int delayBeforeMoving = 30;

    public FroggerEntityScriptDataFloatingTree(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.delayBeforeMoving = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.delayBeforeMoving);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Delay Before Moving (secs)", this.delayBeforeMoving, newDelayBeforeMoving -> this.delayBeforeMoving = newDelayBeforeMoving, 30);
    }
}