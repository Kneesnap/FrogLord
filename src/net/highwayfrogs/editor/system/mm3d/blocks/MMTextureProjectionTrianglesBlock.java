package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
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
@Setter
public class MMTextureProjectionTrianglesBlock extends MMDataBlockBody {
    private int index; // Texture projection index to which these triangles are assigned.
    private List<Integer> triangleIndices = new ArrayList<>(); // List of triangle indices that are assigned to projection

    public MMTextureProjectionTrianglesBlock(MisfitModel3DObject parent) {
        super(OffsetType.TEXTURE_PROJECTIONS_TRIANGLES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.index = reader.readInt();
        int triCount = reader.readInt();
        for (int i = 0; i < triCount; i++)
            this.triangleIndices.add(reader.readInt());
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.index);
        writer.writeInt(this.triangleIndices.size());
        this.triangleIndices.forEach(writer::writeInt);
    }
}
