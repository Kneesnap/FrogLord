package net.highwayfrogs.editor.file.map.entity.data.swamp;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;
import net.highwayfrogs.editor.gui.GUIEditorGrid;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityCrusher extends MatrixData {
    private short speed;
    private short distance;
    private short direction;
    private short delay;

    public EntityCrusher(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.speed = reader.readShort();
        this.distance = reader.readShort();
        this.direction = reader.readShort();
        this.delay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.speed);
        writer.writeShort(this.distance);
        writer.writeShort(this.direction);
        writer.writeShort(this.delay);
    }

    @Override
    public void addData(GUIEditorGrid editor) {
        super.addData(editor);
        editor.addShortField("Speed", getSpeed(), this::setSpeed, null);
        editor.addShortField("Distance", getDistance(), this::setDistance, null);
        editor.addShortField("Direction", getDirection(), this::setDirection, null);
        editor.addShortField("Delay", getDelay(), this::setDelay, null);
    }
}