package net.highwayfrogs.editor.file.map.entity.data.general;

import lombok.Getter;
import net.highwayfrogs.editor.Constants;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.map.entity.data.MatrixEntity;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.standard.psx.PSXMatrix;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a trigger entity.
 * Created by Kneesnap on 11/26/2018.
 */
@Getter
public class TriggerEntity extends GameObject implements MatrixEntity {
    private PSXMatrix matrix = new PSXMatrix();
    private int type;
    private short[] uniqueIds = new short[ENTITY_TYPE_TRIGGER_MAX_IDS];

    public static final int ENTITY_TYPE_TRIGGER_MAX_IDS = 10;
    public static final int BYTE_SIZE = PSXMatrix.BYTE_SIZE + (10 * Constants.SHORT_SIZE) + (2 * Constants.INTEGER_SIZE);

    @Override
    public void load(DataReader reader) {
        matrix.load(reader);
        this.type = reader.readInt();

        for (int i = 0; i < uniqueIds.length; i++)
            this.uniqueIds[i] = reader.readShort();
    }

    @Override
    public void save(DataWriter writer) {
        matrix.save(writer);
        writer.writeInt(this.type);
        for (short id : uniqueIds)
            writer.writeShort(id);
    }
}
