package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implementation of 'FOREST_HIVE' from ent_for.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataBeeHive extends FroggerEntityDataMatrix {
    private int releaseDistance = 768;
    private int swarmSpeed = 10920;

    public FroggerEntityDataBeeHive(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.releaseDistance = reader.readInt();
        this.swarmSpeed = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.releaseDistance);
        writer.writeInt(this.swarmSpeed);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedInt("Release Distance (grid)", this.releaseDistance, newReleaseDistance -> this.releaseDistance = newReleaseDistance, 256, 0, Integer.MAX_VALUE);
        // The units of this are unknown.
        editor.addFixedInt("Max Swarm Speed (???)", this.swarmSpeed, newSwarmSpeed -> this.swarmSpeed = newSwarmSpeed, 2184, 0, Integer.MAX_VALUE);
    }
}