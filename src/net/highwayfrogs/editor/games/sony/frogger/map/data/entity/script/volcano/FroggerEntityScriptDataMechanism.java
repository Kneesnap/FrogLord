package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.volcano;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.script.FroggerEntityScriptData;
import net.highwayfrogs.editor.games.sony.frogger.map.ui.editor.central.FroggerUIMapEntityManager;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the data loaded by SCRIPT_VOL_MECHANISM.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
public class FroggerEntityScriptDataMechanism extends FroggerEntityScriptData {
    private int returnTripDelay = 30;
    private int delta = 2184;
    private int directionChangeDelay = 30;
    private int returnTripDestination = 2184;
    private int destination = 384;
    private int initialDelay = 0;

    public FroggerEntityScriptDataMechanism(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        this.returnTripDelay = reader.readInt();
        this.delta = reader.readInt();
        this.directionChangeDelay = reader.readInt();
        this.returnTripDestination = reader.readInt();
        this.destination = reader.readInt();
        this.initialDelay = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.returnTripDelay);
        writer.writeInt(this.delta);
        writer.writeInt(this.directionChangeDelay);
        writer.writeInt(this.returnTripDestination);
        writer.writeInt(this.destination);
        writer.writeInt(this.initialDelay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor, FroggerUIMapEntityManager manager) {
        editor.addFixedInt("Return Delay (secs)", this.returnTripDelay, newReturnTripDelay -> this.returnTripDelay = newReturnTripDelay, getGameInstance().getFPS());
        editor.addFixedInt("Delta (???)", this.delta, newDelta -> this.delta = newDelta, 2184.5);
        editor.addFixedInt("Direction Change Delay (secs)", this.directionChangeDelay, newDirectionChangeDelay -> this.directionChangeDelay = newDirectionChangeDelay, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("Controls how long to wait when reversing direction."));
        editor.addFixedInt("Return Target (???)", this.returnTripDestination, newReturnTripDestination -> this.returnTripDestination = newReturnTripDestination, 2184.5);
        editor.addFixedInt("Destination (grid)", this.destination, newDestination -> this.destination = newDestination, 256)
                .setTooltip(FXUtils.createTooltip("Controls how far the mechanism will move down before reversing direction. Measured in grid squares."));
        editor.addFixedInt("Initial Delay (secs)", this.initialDelay, newInitialDelay -> this.initialDelay = newInitialDelay, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("Controls how long to wait after the map loads to start moving."));
    }
}