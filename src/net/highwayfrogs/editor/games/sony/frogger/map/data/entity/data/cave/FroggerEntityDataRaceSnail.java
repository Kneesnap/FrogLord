package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.cave;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Includes data for the race snail entity.
 * Represents 'CAVES_RACE_SNAIL' in ent_cav.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataRaceSnail extends FroggerEntityDataPathInfo {
    private int forwardDistance;
    private int backwardDistance;

    public FroggerEntityDataRaceSnail(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.forwardDistance = reader.readUnsignedShortAsInt();
        this.backwardDistance = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.forwardDistance);
        writer.writeUnsignedShort(this.backwardDistance);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Forward Distance", this.forwardDistance, newForwardDistance -> this.forwardDistance = newForwardDistance, 256);
        editor.addUnsignedFixedShort("Backward Distance", this.backwardDistance, newBackwardDistance -> this.backwardDistance = newBackwardDistance, 256);
    }
}