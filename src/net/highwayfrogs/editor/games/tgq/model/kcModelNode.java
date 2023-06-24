package net.highwayfrogs.editor.games.tgq.model;

import lombok.Getter;
import net.highwayfrogs.editor.file.GameObject;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;

/**
 * Represents a model node in the kcGameSystem.
 * Created by Kneesnap on 6/22/2023.
 */
@Getter
public class kcModelNode extends GameObject {
    private long nodeId; // uint
    private long primitiveCount; // uint
    // TODO: Pointer to associated prim(s)?

    @Override
    public void load(DataReader reader) {
        this.nodeId = reader.readUnsignedIntAsLong();
        this.primitiveCount = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.nodeId);
        writer.writeUnsignedInt(this.primitiveCount);
    }
}
