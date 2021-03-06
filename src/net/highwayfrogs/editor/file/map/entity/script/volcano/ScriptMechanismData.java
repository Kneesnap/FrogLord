package net.highwayfrogs.editor.file.map.entity.script.volcano;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents the data loaded by SCRIPT_VOL_MECHANISM.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
@Setter
public class ScriptMechanismData extends EntityScriptData {
    private int returnTripDelay;
    private int delta;
    private int directionChangeDelay;
    private int returnTripDestination;
    private int destination;
    private int initialDelay;

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
    public void addData(GUIEditorGrid editor) {
        editor.addIntegerField("Return Delay", getReturnTripDelay(), this::setReturnTripDelay, null);
        editor.addIntegerField("Delta", getDelta(), this::setDelta, null);
        editor.addIntegerField("Direction Change Delay", getDirectionChangeDelay(), this::setDirectionChangeDelay, null);
        editor.addIntegerField("Return Target", getReturnTripDestination(), this::setReturnTripDestination, null);
        editor.addIntegerField("Destination", getDestination(), this::setDestination, null);
        editor.addIntegerField("Initial Delay", getInitialDelay(), this::setInitialDelay, null);
    }
}
