package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.rushedmap;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataPathInfo;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Holds entity data for the swan in the old build. 'SUBURBIA_SWAN' from suburbia.h
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class FroggerEntityDataSwanOld extends FroggerEntityDataPathInfo {
    private int splineDelay;
    private short swimmingTime;
    private short flapThinkTime;
    private short flappingTime;

    public FroggerEntityDataSwanOld(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.splineDelay = reader.readUnsignedShortAsInt();
        this.swimmingTime = reader.readShort();
        this.flapThinkTime = reader.readShort();
        this.flappingTime = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeUnsignedShort(this.splineDelay);
        writer.writeShort(this.swimmingTime);
        writer.writeShort(this.flapThinkTime);
        writer.writeShort(this.flappingTime);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addUnsignedFixedShort("Spline Delay (secs)", this.splineDelay, newValue -> this.splineDelay = newValue, getGameInstance().getFPS());
        editor.addFixedShort("Swimming Time (secs)", this.swimmingTime, newValue -> this.swimmingTime = newValue, getGameInstance().getFPS());
        editor.addFixedShort("Flap Think Time (secs)", this.flapThinkTime, newValue -> this.flapThinkTime = newValue, getGameInstance().getFPS());
        editor.addFixedShort("Flapping Time (secs)", this.flappingTime, newValue -> this.flappingTime = newValue, getGameInstance().getFPS());
    }
}