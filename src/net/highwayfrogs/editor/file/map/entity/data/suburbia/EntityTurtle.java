package net.highwayfrogs.editor.file.map.entity.data.suburbia;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Represents the "SUBURBIA_TURTLE" struct.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityTurtle extends PathData {
    private int diveDelay;
    private int riseDelay;
    private boolean divingEnabled;

    private static final int TYPE_DIVING = 0;
    private static final int TYPE_NOT_DIVING = 1;

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.diveDelay = reader.readInt();
        this.riseDelay = reader.readInt();
        this.divingEnabled = (reader.readInt() == TYPE_DIVING);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.diveDelay);
        writer.writeInt(this.riseDelay);
        writer.writeInt(isDivingEnabled() ? TYPE_DIVING : TYPE_NOT_DIVING);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Dive Delay", getDiveDelay(), this::setDiveDelay, null);
        editor.addIntegerField("Rise Delay", getRiseDelay(), this::setRiseDelay, null);
        editor.addCheckBox("Diving Allowed", isDivingEnabled(), this::setDivingEnabled);
    }
}
