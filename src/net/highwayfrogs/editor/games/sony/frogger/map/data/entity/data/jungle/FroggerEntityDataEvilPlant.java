package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.jungle;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Implements JUN_PLANT entity data from ent_jun.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataEvilPlant extends FroggerEntityDataMatrix {
    private short snapTime = 6;
    private short snapDelay = 30;

    public FroggerEntityDataEvilPlant(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.snapTime = reader.readShort();
        this.snapDelay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.snapTime);
        writer.writeShort(this.snapDelay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedShort("Snap Time (sec)", this.snapTime, newSnapTime -> this.snapTime = newSnapTime, 30);
        editor.addFixedShort("Snap Delay (sec)", this.snapDelay, newSnapDelay -> this.snapDelay = newSnapDelay, 30);
    }
}