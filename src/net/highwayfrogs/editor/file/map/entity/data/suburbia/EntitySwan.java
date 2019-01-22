package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.EntityData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
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
    public void addData(GUIEditorGrid editor) {
        editor.addIntegerField("Spline Delay", getSplineDelay(), this::setSplineDelay, null);
        editor.addShortField("Swimming Time", getSwimmingTime(), this::setSwimmingTime, null);
        editor.addShortField("Flap Think Time", getFlapThinkTime(), this::setFlapThinkTime, null);
        editor.addShortField("Flapping Time", getFlappingTime(), this::setFlappingTime, null);
    }
}
