package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.rushedmap;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Implements the turtle data 'SUBURBIA_TURTLE' entity data from suburbia.h
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class FroggerEntityDataTurtleOld extends FroggerEntityDataPathInfo {
    private short diveDelay;
    private short riseDelay;
    private short diveSpeed;
    private short riseSpeed;

    public FroggerEntityDataTurtleOld(FroggerMapFile mapFile) {
        super(mapFile);
    }

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
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedShort("Dive Delay (secs)", this.diveDelay, newValue -> this.diveDelay = newValue, 30);
        editor.addFixedShort("Dive Speed (secs)", this.diveSpeed, newValue -> this.diveSpeed = newValue, 30);
        editor.addSignedShortField("Rise Delay (???)", this.riseDelay, newValue -> this.riseDelay = newValue);
        editor.addFixedShort("Rise Speed (???)", this.riseSpeed, newValue -> this.riseSpeed = newValue, 2184);
    }
}