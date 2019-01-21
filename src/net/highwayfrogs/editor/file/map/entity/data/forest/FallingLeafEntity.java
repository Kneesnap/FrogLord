package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class FallingLeafEntity extends MatrixData {
    private int fallSpeed;
    private int swayDuration;
    private int swayAngle;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.fallSpeed = reader.readUnsignedShortAsInt();
        this.swayDuration = reader.readUnsignedShortAsInt();
        this.swayAngle = reader.readUnsignedShortAsInt();
        reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.fallSpeed);
        writer.writeUnsignedShort(this.swayDuration);
        writer.writeUnsignedShort(this.swayAngle);
        writer.writeUnsignedShort(0);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Fall Speed", getFallSpeed(), this::setFallSpeed, null);
        editor.addIntegerField("Sway Duration", getSwayDuration(), this::setSwayDuration, null);
        editor.addIntegerField("Sway Angle", getSwayAngle(), this::setSwayAngle, null);
    }
}
