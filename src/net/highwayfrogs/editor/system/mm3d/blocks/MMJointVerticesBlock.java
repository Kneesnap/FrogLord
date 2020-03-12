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
 * Deprecated as of 1.6 in favor of Weighted Influences, but this should still be included in files.
 * Created by Kneesnap on 3/6/2020.
 */
@Getter
@Setter
public class MMJointVerticesBlock extends MMDataBlockBody {
    private int vertexIndex; // Index into the vertex array.
    private int jointIndex; // Index into te joint array.

    public MMJointVerticesBlock(MisfitModel3DObject parent) {
        super(OffsetType.JOINT_VERTICES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.vertexIndex = reader.readInt();
        this.jointIndex = reader.readInt();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.vertexIndex);
        writer.writeInt(this.jointIndex);
    }
}
