package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Holds the normals for each face of the model.
 * Created by Kneesnap on 2/28/2019.
 */
public class MMTriangleNormalsBlock extends MMDataBlockBody {
    private int unusedFlags;
    @Getter @Setter private int index; // Triangle index (0 based)
    @Getter private final float[] v1Normals = new float[3];
    @Getter private final float[] v2Normals = new float[3];
    @Getter private final float[] v3Normals = new float[3];

    public MMTriangleNormalsBlock(MisfitModel3DObject parent) {
        super(OffsetType.TRIANGLE_NORMALS, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.unusedFlags = reader.readUnsignedShortAsInt();
        this.index = reader.readInt();
        readFloatArray(reader, this.v1Normals);
        readFloatArray(reader, this.v2Normals);
        readFloatArray(reader, this.v3Normals);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.unusedFlags);
        writer.writeInt(this.index);
        writeFloatArray(writer, this.v1Normals);
        writeFloatArray(writer, this.v2Normals);
        writeFloatArray(writer, this.v3Normals);
    }
}
