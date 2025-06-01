package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;

/**
 * Holds texture coordinate information.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMTextureCoordinatesBlock extends MMDataBlockBody {
    private short flags;
    private int triangleIndex; // Triangle for this texture coordinate set.
    private final float[] xCoordinates = new float[COORDINATES_COUNT]; // Indexed by v1, v2, v3.
    private final float[] yCoordinates = new float[COORDINATES_COUNT]; // Indexed by v1, v2, v3.

    public static final int COORDINATES_COUNT = 3;

    public MMTextureCoordinatesBlock(MisfitModel3DObject parent) {
        super(OffsetType.TEXTURE_COORDINATES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readShort();
        this.triangleIndex = reader.readInt();
        readFloatArray(reader, this.xCoordinates);
        readFloatArray(reader, this.yCoordinates);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeShort(this.flags);
        writer.writeInt(this.triangleIndex);
        writeFloatArray(writer, this.xCoordinates);
        writeFloatArray(writer, this.yCoordinates);
    }
}
