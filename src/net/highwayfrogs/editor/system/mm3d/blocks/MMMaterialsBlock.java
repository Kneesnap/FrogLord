package net.highwayfrogs.editor.system.mm3d.blocks;

import lombok.Getter;
import net.highwayfrogs.editor.file.reader.DataReader;
import net.highwayfrogs.editor.file.writer.DataWriter;
import net.highwayfrogs.editor.system.mm3d.MMDataBlockBody;

/**
 * Created by Kneesnap on 2/28/2019.
 */
@Getter
public class MMMaterialsBlock extends MMDataBlockBody {
    private int flags;
    private long texture;
    private String name;
    private float[] ambient = new float[4];
    private float[] diffuse = new float[4];
    private float[] specular = new float[4];
    private float[] emissive = new float[4];
    private float shininess;

    @Override
    public void load(DataReader reader) {
        this.flags = reader.readUnsignedShortAsInt();
        this.texture = reader.readUnsignedIntAsLong();
        this.name = reader.readNullTerminatedString();
        readFloatArray(reader, this.ambient);
        readFloatArray(reader, this.diffuse);
        readFloatArray(reader, this.specular);
        readFloatArray(reader, this.emissive);
        this.shininess = reader.readFloat();
    }

    @Override
    public void save(DataWriter writer) {
        writer.writeUnsignedShort(this.flags);
        writer.writeUnsignedInt(this.texture);
        writer.writeTerminatorString(this.name);
        writeFloatArray(writer, this.ambient);
        writeFloatArray(writer, this.diffuse);
        writeFloatArray(writer, this.specular);
        writeFloatArray(writer, this.emissive);
        writer.writeFloat(this.shininess);
    }
}
