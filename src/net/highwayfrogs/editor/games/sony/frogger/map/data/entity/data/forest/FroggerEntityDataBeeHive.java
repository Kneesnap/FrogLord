package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.forest;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implementation of 'FOREST_HIVE' from ent_for.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataBeeHive extends FroggerEntityDataMatrix {
    private int releaseDistance = 768;
    private int swarmSpeed = 10922; // 10922.5

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
        editor.addFixedInt("Release Distance (grid sq)", this.releaseDistance, newReleaseDistance -> this.releaseDistance = newReleaseDistance, 256, 0, Integer.MAX_VALUE)
                .setTooltip(FXUtils.createTooltip("The maximum number of grid squares away (pythagorean distance) the player must be to activate the bees."));
        // The units of this are unknown.
        editor.addFixedInt("Max Speed (grid/sec)", this.swarmSpeed, newSwarmSpeed -> this.swarmSpeed = newSwarmSpeed, 2184.5, 0, Integer.MAX_VALUE)
                .setTooltip(FXUtils.createTooltip("The maximum speed of the bees.\nThe value seems to roughly correspond to the number of grid squares the bees can travel per-second."));
    }
}