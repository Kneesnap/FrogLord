package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp;

import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'SWAMP_PRESS' entity data definition from ent_swp.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataPress extends FroggerEntityDataMatrix {
    private short speed = 4369;
    private short distance = 1024;
    private FroggerEntityDataPressDirection direction = FroggerEntityDataPressDirection.MOVING_DOWN;
    private short delay = 30;

    public FroggerEntityDataPress(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readShort();
        this.distance = reader.readShort();
        this.direction = FroggerEntityDataPressDirection.values()[reader.readShort()];
        this.delay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.speed);
        writer.writeShort(this.distance);
        writer.writeShort((short) (this.direction != null ? this.direction : FroggerEntityDataPressDirection.MOVING_DOWN).ordinal());
        writer.writeShort(this.delay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedShort("Speed (???)", this.speed, newSpeed -> this.speed = newSpeed, 2184);
        editor.addFixedShort("Distance (grid)", this.distance, newDistance -> this.distance = newDistance, 256);
        editor.addEnumSelector("Movement Direction", this.direction, FroggerEntityDataPressDirection.values(), false, newDirection -> this.direction = newDirection);
        editor.addFixedShort("Delay (secs)", this.delay, newDelay -> this.delay = newDelay, 30);
    }

    public enum FroggerEntityDataPressDirection {
        MOVING_UP, MOVING_DOWN
    }
}