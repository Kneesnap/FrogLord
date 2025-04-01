package net.highwayfrogs.editor.file.map.entity.data.cave;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityFrogLight extends MatrixData {
    private int minRadius;
    private int maxRadius;
    private int dieSpeed;
    private int count;
    private int setup;

    public EntityFrogLight(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.minRadius = reader.readInt();
        this.maxRadius = reader.readInt();
        this.dieSpeed = reader.readInt();
        this.count = reader.readInt();
        this.setup = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.minRadius);
        writer.writeInt(this.maxRadius);
        writer.writeInt(this.dieSpeed);
        writer.writeInt(this.count);
        writer.writeInt(this.setup);
    }
}