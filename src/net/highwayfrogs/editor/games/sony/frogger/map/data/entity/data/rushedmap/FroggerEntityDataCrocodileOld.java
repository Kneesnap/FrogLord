package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.rushedmap;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the old crocodile entity data 'ORIGINAL_CROCODILE' in suburbia.h
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class FroggerEntityDataCrocodileOld extends FroggerEntityDataPathInfo {
    private int openMouthDelay;

    public FroggerEntityDataCrocodileOld(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.openMouthDelay = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.openMouthDelay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Mouth Open Delay (secs)", this.openMouthDelay, newValue -> this.openMouthDelay = newValue, getGameInstance().getFPS());
    }
}