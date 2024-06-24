package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Implements the 'SWAMP_CRUSHER' entity data definition in ent_swp.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataCrusher extends FroggerEntityDataMatrix {
    private short speed = 4 * 2184;
    private short distance = 512;
    private FroggerEntityDataCrusherDirection direction = FroggerEntityDataCrusherDirection.values()[0];
    private short delay = 30;

    public FroggerEntityDataCrusher(FroggerMapFile mapFile) {
        super(mapFile);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readShort();
        this.distance = reader.readShort();
        this.direction = FroggerEntityDataCrusherDirection.values()[reader.readShort()];
        this.delay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.speed);
        writer.writeShort(this.distance);
        writer.writeShort((short) (this.direction != null ? this.direction.ordinal() : 0));
        writer.writeShort(this.delay);
    }

    @Override
    public void setupEditor(GUIEditorGrid editor) {
        super.setupEditor(editor);
        editor.addFixedShort("Speed (???)", this.speed, newSpeed -> this.speed = newSpeed, 2184);
        editor.addFixedShort("Distance (grid)", this.distance, newDistance -> this.distance = newDistance, 256);
        editor.addEnumSelector("Movement Direction", this.direction, FroggerEntityDataCrusherDirection.values(), false, newDirection -> this.direction = newDirection);
        editor.addFixedShort("Delay (secs)", this.delay, newDelay -> this.delay = newDelay, 30);
    }

    @Getter
    @AllArgsConstructor
    public enum FroggerEntityDataCrusherDirection {
        POSITIVE_Z("Z+"),
        POSITIVE_X("X+"),
        NEGATIVE_Z("Z-"),
        NEGATIVE_X("X-");

        private final String displayString;
    }
}