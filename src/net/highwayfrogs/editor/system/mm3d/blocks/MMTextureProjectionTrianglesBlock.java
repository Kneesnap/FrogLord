package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

import java.util.ArrayList;
import java.util.List;

/**
 * The texture projection triangles section is a list of which triangles are using a specified texture projection to set their texture coordinates.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMTextureProjectionTrianglesBlock extends MMDataBlockBody {
    private long index; // Texture projection index to which these triangles are assigned.
    private List<Long> triangleIndices = new ArrayList<>(); // List of triangle indices that are assigned to projection

    public MMTextureProjectionTrianglesBlock(MisfitModel3DObject parent) {
        super(OffsetType.TEXTURE_PROJECTIONS_TRIANGLES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.index = reader.readUnsignedIntAsLong();
        int triCount = reader.readInt();
        for (int i = 0; i < triCount; i++)
            this.triangleIndices.add(reader.readUnsignedIntAsLong());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.index);
        writer.writeInt(this.triangleIndices.size());
        this.triangleIndices.forEach(writer::writeUnsignedInt);
    }
}
