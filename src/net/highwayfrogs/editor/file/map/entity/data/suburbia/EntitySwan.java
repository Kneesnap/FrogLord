package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import javafx.scene.control.TableView;
import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.NameValuePair;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class EntitySwan extends EntityData {
    private int splineDelay;
    private short swimmingTime;
    private short flapThinkTime;
    private short flappingTime;

    @Override
    public void load(DataReader reader) {
        this.splineDelay = reader.readUnsignedShortAsInt();
        this.swimmingTime = reader.readShort();
        this.flapThinkTime = reader.readShort();
        this.flappingTime = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.splineDelay);
        writer.writeShort(this.swimmingTime);
        writer.writeShort(this.flapThinkTime);
        writer.writeShort(this.flappingTime);
    }

    @Override
    public void addData(TableView<NameValuePair> table) {
        table.getItems().add(new NameValuePair("Spline Delay", String.valueOf(getSplineDelay())));
        table.getItems().add(new NameValuePair("Swimming Time", String.valueOf(getSwimmingTime())));
        table.getItems().add(new NameValuePair("Flap Think Time", String.valueOf(getFlapThinkTime())));
        table.getItems().add(new NameValuePair("Flapping Time", String.valueOf(getFlappingTime())));
    }
}
