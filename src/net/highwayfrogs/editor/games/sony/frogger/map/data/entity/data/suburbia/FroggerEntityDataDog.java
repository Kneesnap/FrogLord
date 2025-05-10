package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements 'SUBURBIA_DOG' entity data from ent_sub.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataDog extends FroggerEntityDataPathInfo {
    private int waitDelay = 30;

    public FroggerEntityDataDog(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.waitDelay = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.waitDelay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedInt("Wait Delay (sec)", this.waitDelay, newWaitDelay -> this.waitDelay = newWaitDelay, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("How long to wait in the dog kennel before running out."));
    }
}