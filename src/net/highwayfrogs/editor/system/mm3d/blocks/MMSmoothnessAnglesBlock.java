package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Holds smoothness angles.
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMSmoothnessAnglesBlock extends MMDataBlockBody {
    private long groupIndex; // Index into group array.
    private short angle; // Maximum angle (in degrees) to use in smoothing normals. (0 - 180)

    public MMSmoothnessAnglesBlock(MisfitModel3DObject parent) {
        super(OffsetType.SMOOTHNESS_ANGLES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.groupIndex = reader.readUnsignedIntAsLong();
        this.angle = reader.readUnsignedByteAsShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedInt(this.groupIndex);
        writer.writeUnsignedByte(this.angle);
    }
}
