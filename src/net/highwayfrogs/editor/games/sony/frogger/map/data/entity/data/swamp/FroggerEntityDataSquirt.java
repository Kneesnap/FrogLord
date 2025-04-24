package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.file.standard.SVector;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'SWAMP_SQUIRT' entity data definition in ent_swp.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataSquirt extends FroggerEntityDataMatrix {
    private short timeDelay = 15;
    private short dropTime = 45;
    private final SVector target = new SVector();

    public FroggerEntityDataSquirt(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.timeDelay = reader.readShort();
        this.dropTime = reader.readShort();
        this.target.loadWithPadding(reader);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.timeDelay);
        writer.writeShort(this.dropTime);
        this.target.saveWithPadding(writer);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedShort("Time Delay (secs)", this.timeDelay, newTimeDelay -> this.timeDelay = newTimeDelay, 30);
        editor.addFixedShort("Drop Time (secs)", this.dropTime, newDropTime -> this.dropTime = newDropTime, 30);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        super.setupEditor(editor, manager);
        editor.addFloatSVector("Target", this.target, manager.getController());
    }
}