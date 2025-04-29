package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.suburbia;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Represents the "SUBURBIA_TURTLE" struct from ent_sub.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataTurtle extends FroggerEntityDataPathInfo {
    private int diveDelay = 60;
    private int riseDelay = 60;
    private boolean divingEnabled;

    private static final int TYPE_DIVING = 0;
    private static final int TYPE_NOT_DIVING = 1;

    public FroggerEntityDataTurtle(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.diveDelay = reader.readInt();
        this.riseDelay = reader.readInt();
        int turtleType = reader.readInt();
        this.divingEnabled = (turtleType == TYPE_DIVING);
        if (turtleType != TYPE_DIVING && turtleType != TYPE_NOT_DIVING)
            getLogger().warning("st_turtle_type was expected to be either 0 or 1, but was actually " + turtleType);
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.diveDelay);
        writer.writeInt(this.riseDelay);
        writer.writeInt(isDivingEnabled() ? TYPE_DIVING : TYPE_NOT_DIVING);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedInt("Dive Delay (secs)", this.diveDelay, newDiveDelay -> this.diveDelay = newDiveDelay, 30);
        editor.addFixedInt("Rise Delay (secs)", this.riseDelay, newRiseDelay -> this.riseDelay = newRiseDelay, 30);
        editor.addCheckBox("Diving Allowed", this.divingEnabled, newDivingEnabled -> this.divingEnabled = newDivingEnabled);
    }
}