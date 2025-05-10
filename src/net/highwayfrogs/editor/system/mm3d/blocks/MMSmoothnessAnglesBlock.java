package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import lombok.Setter;
import net.highwayfrogs.editor.utils.data.reader.DataReader;
import net.highwayfrogs.editor.utils.data.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;
import net.highwayfrogs.editor.system.mm3d.MisfitModel3DObject;
import net.highwayfrogs.editor.system.mm3d.OffsetType;

/**
 * Holds smoothness angles.
 * Version: 1.4+
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
@Setter
public class MMSmoothnessAnglesBlock extends MMDataBlockBody {
    private int groupIndex; // Index into group array.
    private short angle; // Maximum angle (in degrees) to use in smoothing normals. (0 - 180)

    public MMSmoothnessAnglesBlock(MisfitModel3DObject parent) {
        super(OffsetType.SMOOTHNESS_ANGLES, parent);
    }

    @Override
    public void load(DataReader reader) {
        this.groupIndex = reader.readInt();
        this.angle = reader.readUnsignedByteAsShort();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeInt(this.groupIndex);
        writer.writeUnsignedByte(this.angle);
    }
}
