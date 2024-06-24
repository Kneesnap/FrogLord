package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.desert;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityData;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Seems to be fully unused.
 * Represents DESERT_THERMAL in ent_des.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataThermal extends FroggerEntityData {
    private int rotateTime;

    public FroggerEntityDataThermal(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.rotateTime = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.rotateTime);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        editor.addUnsignedFixedShort("Rotation% (per frame)", 4096 - this.rotateTime, newRotateTime -> this.rotateTime = 4096 - newRotateTime, 4096, 0, 4096);
    }
}