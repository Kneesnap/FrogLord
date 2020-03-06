package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Represents the Joint Vertices block.
 * Deprecated as of 1.6, but this is still present in the format, and is here for compatibility.
 * Use Weighted Influences instead.
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
@Setter
public class MMJointVerticesBlock extends MMDataBlockBody {
    private long vertexIndex; // Index into the vertex array.
    private long jointIndex; // Index into te joint array.

    public MMJointVerticesBlock(MisfitModel3DObject parent) {
        super(OffsetType.JOINT_VERTICES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.vertexIndex = reader.readUnsignedIntAsLong();
        this.jointIndex = reader.readUnsignedIntAsLong();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.vertexIndex);
        writer.writeUnsignedInt(this.jointIndex);
    }
}
