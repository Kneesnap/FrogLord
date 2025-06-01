package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.retro;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'ORG_BEAVER_DATA' entity data definition from ent_org.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataBeaver extends FroggerEntityDataPathInfo {
    private short delay = 30;

    public FroggerEntityDataBeaver(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.delay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.delay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedShort("Delay (secs)", this.delay, newDelay -> this.delay = newDelay, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("The beaver will only start swimming from an idle state after it spends this amount of time without colliding with any entity."));
    }
}