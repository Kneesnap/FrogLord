package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.volcano;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implementation of the 'VOL_TRIGGERED_PLATFORM' entity data definition in ent_vol.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataTriggeredPlatform extends FroggerEntityDataPathInfo {
    private boolean movingByDefault;

    public FroggerEntityDataTriggeredPlatform(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        int initialMovement = reader.readInt();
        this.movingByDefault = (initialMovement != 1);
        if (initialMovement != 0 && initialMovement != 1)
            getLogger().warning("The initial movement state was %d, but only 0 or 1 was expected.", initialMovement);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.movingByDefault ? 0 : 1);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addCheckBox("Moving By Default?", this.movingByDefault, newMovingByDefault -> this.movingByDefault = newMovingByDefault)
                .setTooltip(FXUtils.createTooltip("Controls whether the platform is moving when the level begins."));
    }
}