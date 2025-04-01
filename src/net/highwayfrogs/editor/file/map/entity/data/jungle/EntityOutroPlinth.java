package net.highwayfrogs.editor.file.map.entity.data.jungle;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.map.entity.data.MatrixData;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.games.sony.frogger.FroggerGameInstance;

/**
 * Represents "JUN_OUTRO_FROGPLINTH_DATA".
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
@Setter
public class EntityOutroPlinth extends MatrixData {
    private int id;

    public EntityOutroPlinth(FroggerGameInstance instance) {
        super(instance);
    }

    @Override
    public void load(DataReader reader) {
        super.load(reader);
        this.id = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        super.save(writer);
        writer.writeInt(this.id);
    }
}