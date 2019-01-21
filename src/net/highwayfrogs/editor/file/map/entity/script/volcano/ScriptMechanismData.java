package net.highwayfrogs.editor.file.map.entity.script.volcano;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.script.EntityScriptData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents the data loaded by SCRIPT_VOL_MECHANISM.
 * Created by Kneesnap on 11/27/2018.
 */
@Getter
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
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Return Delay", String.valueOf(returnTripDelay)));
        table.getItems().add(new NameValuePair("Delta", String.valueOf(destination)));
        table.getItems().add(new NameValuePair("Direction Change Delay", String.valueOf(directionChangeDelay)));
        table.getItems().add(new NameValuePair("Return Target", String.valueOf(returnTripDestination)));
        table.getItems().add(new NameValuePair("Destination", String.valueOf(destination)));
        table.getItems().add(new NameValuePair("Initial Delay", String.valueOf(initialDelay)));
    }
}
