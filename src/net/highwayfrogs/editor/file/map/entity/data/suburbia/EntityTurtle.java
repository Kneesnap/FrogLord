package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Represents the "SUBURBIA_TURTLE" struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntityTurtle extends PathData {
    private int diveDelay;
    private int riseDelay;
    private int turtleType;

    public static final int TYPE_DIVING = 0;
    public static final int TYPE_NOT_DIVING = 1;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.diveDelay = reader.readInt();
        this.riseDelay = reader.readInt();
        this.turtleType = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.diveDelay);
        writer.writeInt(this.riseDelay);
        writer.writeInt(this.turtleType);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        super.addData(table);
        table.getItems().add(new NameValuePair("Dive Delay", String.valueOf(getDiveDelay())));
        table.getItems().add(new NameValuePair("Rise Delay", String.valueOf(getRiseDelay())));
        table.getItems().add(new NameValuePair("Turtle Type", String.valueOf(getTurtleType())));
    }
}
