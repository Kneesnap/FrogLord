package net.highwayfrogs.editor.file.map.entity.data.jungle;

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
public class EntityEvilPlant extends MatrixData {
    private short snapTime;
    private short snapDelay;

    public EntityEvilPlant(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.snapTime = reader.readShort();
        this.snapDelay = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeShort(this.snapTime);
        writer.writeShort(this.snapDelay);
    }
}