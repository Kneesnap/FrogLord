package net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.swamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.highwayfrogs.editor.games.sony.frogger.map.FroggerMapFile;
import net.highwayfrogs.editor.games.sony.frogger.map.data.entity.data.FroggerEntityDataMatrix;
import net.highwayfrogs.editor.gui.GUIEditorGrid;
import net.highwayfrogs.editor.utils.FXUtils;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Implements the 'SWAMP_CRUSHER' entity data definition in ent_swp.h
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class FroggerEntityDataCrusher extends FroggerEntityDataMatrix {
    private short speed = 4 * 2184 + 2;
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
        editor.addFixedShort("Speed (World Units/sec)", this.speed, newSpeed -> this.speed = newSpeed, 2184.5)
                .setTooltip(FXUtils.createTooltip("Controls how fast the crusher moves.\nMost likely these are in world units."));
        editor.addFixedShort("Distance (grid)", this.distance, newDistance -> this.distance = newDistance, 256)
                .setTooltip(FXUtils.createTooltip("Controls many grid squares does the crusher has moved when it is fully extended."));
        editor.addEnumSelector("Movement Direction", this.direction, FroggerEntityDataCrusherDirection.values(), false, newDirection -> this.direction = newDirection)
                .setTooltip(FXUtils.createTooltip("Controls which direction the crusher moves, from its starting position."));
        editor.addFixedShort("Delay (secs)", this.delay, newDelay -> this.delay = newDelay, getGameInstance().getFPS())
                .setTooltip(FXUtils.createTooltip("Controls how long to wait until after the level loads to start moving."));
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