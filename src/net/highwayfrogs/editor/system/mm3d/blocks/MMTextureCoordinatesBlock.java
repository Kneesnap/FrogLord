package net.highwayfrogs.editor.system.mm3d.blocks;

import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;

/**
 * Holds texture coordinate information.
 * Created by Kneesnap on 2/28/2019.
 */
public class MMTextureCoordinatesBlock extends MMDataBlockBody {
    private int flags;
    private long triangle; // Triangle for this texture coordinate set.
    private float[] xCoordinates = new float[3]; // Indexed by v1, v2, v3.
    private float[] yCoordinates = new float[3]; // Indexed by v1, v2, v3.

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.triangle = reader.readUnsignedIntAsLong();
        readFloatArray(reader, this.xCoordinates);
        readFloatArray(reader, this.yCoordinates);
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedInt(this.triangle);
        writeFloatArray(writer, this.xCoordinates);
        writeFloatArray(writer, this.yCoordinates);
    }
}
