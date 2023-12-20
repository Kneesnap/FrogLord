package net.highwayfrogs.editor.file.map.entity.data.rushedmap;

import lombok.Getter;
import net.highwayfrogs.editor.file.map.entity.data.PathData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Holds entity data for the swan in the old build.
 * Created by Kneesnap on 2/1/2023.
 */
@Getter
public class EntitySwanOld extends PathData {
    private int splineDelay;
    private short swimmingTime;
    private short flapThinkTime;
    private short flappingTime;

    public EntitySwanOld(FroggerGameInstance instance) {
        super(instance);
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
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addIntegerField("Spline Delay", this.splineDelay, newValue -> this.splineDelay = newValue, null);
        editor.addShortField("Swimming Time", this.swimmingTime, newValue -> this.swimmingTime = newValue, null);
        editor.addShortField("Flap Think Time", this.flapThinkTime, newValue -> this.flapThinkTime = newValue, null);
        editor.addShortField("Flapping Time", this.flappingTime, newValue -> this.flappingTime = newValue, null);
    }
}