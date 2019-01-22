package net.highwayfrogs.editor.file.map.entity.data.forest;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityHedgehog extends PathData {
    private int runTime;
    private int rollTime;
    private int runSpeed;
    private int rollSpeed;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.runTime = reader.readUnsignedShortAsInt();
        this.rollTime = reader.readUnsignedShortAsInt();
        this.runSpeed = reader.readUnsignedShortAsInt();
        this.rollSpeed = reader.readUnsignedShortAsInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.runTime);
        writer.writeUnsignedShort(this.rollTime);
        writer.writeUnsignedShort(this.runSpeed);
        writer.writeUnsignedShort(this.rollSpeed);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Run Time", getRunTime(), this::setRunTime, null);
        editor.addIntegerField("Roll Time", getRollTime(), this::setRollTime, null);
        editor.addIntegerField("Run Speed", getRunSpeed(), this::setRunSpeed, null);
        editor.addIntegerField("Roll Speed", getRollSpeed(), this::setRollSpeed, null);
    }
}
