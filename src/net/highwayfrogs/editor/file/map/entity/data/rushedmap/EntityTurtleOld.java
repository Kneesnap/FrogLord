package net.highwayfrogs.editor.file.map.entity.data.rushedmap;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Implements the turtle data.
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class EntityTurtleOld extends PathData {
    private short diveDelay;
    private short riseDelay;
    private short diveSpeed;
    private short riseSpeed;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.diveDelay = reader.readShort();
        this.riseDelay = reader.readShort();
        this.diveSpeed = reader.readShort();
        this.riseSpeed = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.diveDelay);
        writer.writeShort(this.riseDelay);
        writer.writeShort(this.diveSpeed);
        writer.writeShort(this.riseSpeed);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addShortField("Dive Delay", this.diveDelay, newValue -> this.diveDelay = newValue, null);
        editor.addShortField("Dive Speed", this.diveSpeed, newValue -> this.diveSpeed = newValue, null);
        editor.addShortField("Rise Delay", this.riseDelay, newValue -> this.riseDelay = newValue, null);
        editor.addShortField("Rise Speed", this.riseSpeed, newValue -> this.riseSpeed = newValue, null);
    }
}